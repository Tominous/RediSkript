package net.limework.rediskript.managers;

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

import javax.crypto.IllegalBlockSizeException;
import java.nio.charset.StandardCharsets;
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
                //System.out.println("Message got from channel: "+channel +" and the Message: " +json.toString());
                JSONArray messages = j.getJSONArray("Messages");
                RedisMessageEvent event;
                for (int i = 0 ; i < messages.length(); i++) {

                    System.out.println(messages.get(i).toString());
                    event = new RedisMessageEvent(channelString, messages.get(i).toString(), j.getLong("Date"));
                    if (plugin.isEnabled()) {
                        RedisMessageEvent finalEvent = event;
                        Bukkit.getScheduler().runTask(plugin, () -> plugin.getServer().getPluginManager().callEvent(finalEvent));
                    }
                }

                //if plugin is disabling, don't call events anymore
            }
        } catch (Exception e) {
            e.printStackTrace();
            Bukkit.getLogger().warning(ChatColor.translateAlternateColorCodes('&', "&cI got a message that was empty from channel " + channelString + " please check your code that you used to send the message. Message content:"));
            Bukkit.getLogger().warning(receivedMessage);
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
