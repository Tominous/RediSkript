package net.limework.core.managers;

import net.limework.core.RediSkript;
import net.limework.core.events.RedisMessageEvent;
import net.limework.data.Encryption;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.Configuration;
import org.cryptomator.siv.UnauthenticCiphertextException;
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

    private RediSkript plugin;

    private JedisPool jedisPool;
    private ExecutorService RedisService;

    private AtomicBoolean isKilled = new AtomicBoolean();


    //sub
    private BinaryJedis subscribeJedis;
    private List<String> channels;
    private AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    private AtomicBoolean isOnline = new AtomicBoolean();
    private Encryption encryption;


    public RedisManager(RediSkript plugin) {
        this.plugin = plugin;
        Configuration config = this.plugin.getConfig();
        JedisPoolConfig JConfig = new JedisPoolConfig();
        JConfig.setMaxTotal(config.getInt("Redis.MaxConnections"));
        JConfig.setMaxIdle(config.getInt("Redis.MaxConnections"));
        JConfig.setMinIdle(1);
        JConfig.setBlockWhenExhausted(true);
        this.jedisPool = new JedisPool(JConfig,
                config.getString("Redis.Host"),
                config.getInt("Redis.Port"),
                config.getInt("Redis.TimeOut"),
                config.getString("Redis.Password"),
                config.getBoolean("Redis.useSSL"));
        RedisService = Executors.newFixedThreadPool(config.getInt("Redis.Threads"));
        try {
            this.subscribeJedis = this.jedisPool.getResource();
        } catch (Exception ignored) {
        }
        this.channels = config.getStringList("Channels");
        encryption = new Encryption(config);

    }

    public void start() {
        this.RedisService.execute(this);
    }

    @Override
    public void run() {
        while (!isShuttingDown.get()) {
            isKilled.set(false);
            try {
                message("&2[&aRediSkript&a] &cConnecting to redis...");
                if (!this.subscribeJedis.isConnected()) this.subscribeJedis = this.jedisPool.getResource();
                message("&2[&aRediSkript&a] &aRedis connected!");
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
                message("&2[&aRediSkript&a] &cConnection to redis has failed! &ereconnecting...");
                if (this.subscribeJedis != null) {
                    this.subscribeJedis.close();
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (isKilled.get()) break;
        }
    }

    private void message(String message) {
        plugin.getLogger().info(ChatColor.translateAlternateColorCodes('&', message));
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
                plugin.getServer().getPluginManager().callEvent(new RedisMessageEvent(channelString, j.getString("Message")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Bukkit.getLogger().warning(ChatColor.translateAlternateColorCodes('&', "&2[&aRediSkript&a] &cI got a message that was empty from channel " + channelString + " please check your code that you used to send the message. Message content:"));
            Bukkit.getLogger().warning(receivedMessage);
        }

    }

    public void shutdown() {
        this.isShuttingDown.set(true);
        if (this.subscribeJedis != null) {
            this.unsubscribe();
            this.subscribeJedis.close();
        }
        this.RedisService.shutdown();

    }

    public JedisPool getJedisPool() {
        return jedisPool;
    }

    public ExecutorService getRedisService() {
        return RedisService;
    }

    public AtomicBoolean isShuttingDown() {
        return isShuttingDown;
    }

    public void reloadRedis() {
        this.isKilled.set(true);
        try {
            if (this.subscribeJedis != null) {
                this.unsubscribe();
                this.subscribeJedis.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.shutdown();
        plugin.startRedis(true);
    }



    public Encryption getEncryption() {
        return encryption;
    }
}
