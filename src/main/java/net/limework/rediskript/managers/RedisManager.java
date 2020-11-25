package net.limework.rediskript.managers;

import ch.njol.skript.registrations.Classes;
import ch.njol.skript.variables.Variables;
import net.limework.rediskript.RediSkript;
import net.limework.rediskript.events.RedisMessageEvent;
import net.limework.rediskript.data.Encryption;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.Configuration;
import org.cryptomator.siv.UnauthenticCiphertextException;
import org.json.JSONArray;
import org.json.JSONObject;
import redis.clients.jedis.BinaryJedis;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

import javax.crypto.IllegalBlockSizeException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class RedisManager extends BinaryJedisPubSub implements Runnable {

    private ExecutorService RedisReconnector;
    private RediSkript plugin;

    private JedisPool jedisPool;
    private ExecutorService RedisService;


    //sub
    private BinaryJedis subscribeJedis;
    private List<String> channels;
    private AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    private Encryption encryption;


    public RedisManager(RediSkript plugin) {
        this.plugin = plugin;
        Configuration config = this.plugin.getConfig();
        JedisPoolConfig JConfig = new JedisPoolConfig();
        int maxConnections = config.getInt("Redis.MaxConnections");
        if (maxConnections < 2) { maxConnections = 2; }

        JConfig.setMaxTotal(maxConnections);
        JConfig.setMaxIdle(maxConnections);
        JConfig.setMinIdle(1);
        JConfig.setBlockWhenExhausted(true);
        this.jedisPool = new JedisPool(JConfig,
                config.getString("Redis.Host"),
                config.getInt("Redis.Port"),
                config.getInt("Redis.TimeOut"),
                config.getString("Redis.Password"),
                config.getBoolean("Redis.useTLS"));
        RedisReconnector = Executors.newSingleThreadExecutor();
        RedisService = Executors.newSingleThreadExecutor();
        try {
            this.subscribeJedis = this.jedisPool.getResource();
        } catch (Exception ignored) {
        }
        this.channels = config.getStringList("Channels");
        encryption = new Encryption(config);

    }

    public void start() {
        this.RedisReconnector.execute(this);
    }

    @Override
    public void run() {
        while (!isShuttingDown.get() && plugin.isEnabled()) {
            try {
                plugin.getLogger().info(ChatColor.translateAlternateColorCodes('&', "&cConnecting to redis..."));
                if (!this.subscribeJedis.isConnected()) this.subscribeJedis = this.jedisPool.getResource();
                plugin.getLogger().info(ChatColor.translateAlternateColorCodes('&', "&aRedis connected!"));
                int byteArr2dSize = 1;
                byte[][] channelsInByte = new byte[channels.size()][byteArr2dSize];
                boolean reInitializeByteArray;
                // Loop that reInitialize array IF array size is not enough
                do {
                    reInitializeByteArray = false;
                    try {
                        /* Data Initialization for channelsInByte array from List<String> channels */
                        for (int x = 0; x < channels.size(); x++) {
                            channelsInByte[x] = channels.get(x).getBytes(StandardCharsets.UTF_8);
                        }
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        reInitializeByteArray = true;
                        /* Increase the current 2d array size to increase 1 and reinitialize the array*/
                        byteArr2dSize += 1;
                        channelsInByte = new byte[channels.size()][byteArr2dSize];
                    }
                } while (reInitializeByteArray);
                this.subscribeJedis.subscribe(this, channelsInByte);


            } catch (Exception e) {
                if (isShuttingDown.get() || !plugin.isEnabled()) {
                    return;
                }
                plugin.getLogger().warning(ChatColor.translateAlternateColorCodes('&', "&cConnection to redis has failed! &cReconnecting..."));
                if (this.subscribeJedis != null) {
                    this.subscribeJedis.close();
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onMessage(byte[] channel, byte[] message) {
        String channelString = new String(channel, StandardCharsets.UTF_8);
        String receivedMessage = null;
        try {
            //if encryption is enabled, decrypt the message, else just convert binary to string
            if (this.encryption.isEncryptionEnabled()) {
                try {
                    receivedMessage = encryption.decrypt(message);
                } catch (UnauthenticCiphertextException | IllegalBlockSizeException e) {
                    e.printStackTrace();
                }

            } else {
                //encryption is disabled, so let's just get the string
                receivedMessage = new String(message, StandardCharsets.UTF_8);
            }
            if (receivedMessage != null) {
                JSONObject j = new JSONObject(receivedMessage);
                if (j.get("Type").equals("Skript")) {
                    JSONArray messages = j.getJSONArray("Messages");
                    RedisMessageEvent event;
                    for (int i = 0; i < messages.length(); i++) {
                        event = new RedisMessageEvent(channelString, messages.get(i).toString(), j.getLong("Date"));
                        //if plugin is disabling, don't call events anymore
                        if (plugin.isEnabled()) {
                            RedisMessageEvent finalEvent = event;
                            Bukkit.getScheduler().runTask(plugin, () -> plugin.getServer().getPluginManager().callEvent(finalEvent));
                        }
                    }
                } else if (j.get("Type").equals("SkriptVariables")) {
                    JSONArray variableNames = j.getJSONArray("Names");
                    boolean delete = false;
                    Object inputValue = null;
                    if (j.isNull("Value")) {
                        delete = true;
                    } else {
                        String input = j.getString("Value");
                        String [] inputs = input.split("\\^", 2);
                        inputValue = Classes.deserialize(inputs[0], Base64.getDecoder().decode(inputs[1]));


                    }
                    for (int i = 0; i < variableNames.length(); i++) {
                        if (delete) {
                            Variables.setVariable(variableNames.get(i).toString(), null, null, false);
                        } else {
                            Variables.setVariable(variableNames.get(i).toString(), inputValue, null, false);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Bukkit.getLogger().warning(ChatColor.translateAlternateColorCodes('&', "&cI got a message that was empty from channel " + channelString + " please check your code that you used to send the message. Message content:"));
            Bukkit.getLogger().warning(receivedMessage);
        }

    }
    public void sendMessage(String[] message, String channel) {
        JSONObject json = new JSONObject();
        json.put("Messages", new JSONArray(message));
        json.put("Type", "Skript");
        json.put("Date", System.currentTimeMillis()); //for unique string every time & PING calculations
        finishSendMessage(json, channel);
    }
    public void sendVariables(String[] variableNames, String variableValue, String channel) {
        JSONObject json = new JSONObject();
        json.put("Names", new JSONArray(variableNames));
        json.put("Value", variableValue);
        json.put("Type", "SkriptVariables");
        json.put("Date", System.currentTimeMillis()); //for unique string every time & PING calculations
        finishSendMessage(json, channel);
    }

    public void finishSendMessage(JSONObject json, String channel) {
        try {
            byte[] message;
            if (this.getEncryption().isEncryptionEnabled()) {
                message = this.getEncryption().encrypt(json.toString());
            } else {
                message = json.toString().getBytes(StandardCharsets.UTF_8);
            }
            //execute sending of redis message on the main thread if plugin is disabling
            //so it can still process the sending

            //sending a redis message blocks main thread if there's no more connections available
            //so to avoid issues, it's best to do it always on separate thread
            if (plugin.isEnabled()) {
                this.getRedisService().execute(() -> {
                    BinaryJedis j = this.getJedisPool().getResource();
                    j.publish(channel.getBytes(StandardCharsets.UTF_8), message);
                    j.close();
                });
            } else {
                BinaryJedis j = this.getJedisPool().getResource();
                j.publish(channel.getBytes(StandardCharsets.UTF_8), message);
                j.close();
            }
        } catch (JedisConnectionException exception) {
            exception.printStackTrace();
        }
    }

    public void shutdown() {
        this.isShuttingDown.set(true);
        if (this.subscribeJedis != null) {
            this.unsubscribe();
            this.subscribeJedis.close();
            this.subscribeJedis.getClient().close();
            this.jedisPool.getResource().close();
        }
        isShuttingDown.set(true);
        this.RedisReconnector.shutdown();
        this.RedisService.shutdown();
        this.RedisService = null;
        this.RedisReconnector = null;

    }
    public void reload() {
        this.shutdown();
        plugin.startRedis(true);
    }

    public JedisPool getJedisPool() {
        return jedisPool;
    }

    public Encryption getEncryption() {
        return encryption;
    }

    public ExecutorService getRedisService() { return RedisService; }
}