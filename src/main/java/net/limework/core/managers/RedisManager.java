package net.limework.core.managers;

import net.limework.Data.Encryption;
import net.limework.core.LimeworkSpigotCore;
import net.limework.core.events.RedisMessageEvent;
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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class RedisManager extends BinaryJedisPubSub implements Runnable{

    private LimeworkSpigotCore plugin;

    private JedisPool jedisPool;
    private ExecutorService RedisService;



    //sub
    private BinaryJedis subscribeJedis;
    private List<String> channels;
    private AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    private AtomicBoolean isRedisOnline = new AtomicBoolean();
    private Encryption encryption;


    public RedisManager(LimeworkSpigotCore plugin) {
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
        try{this.subscribeJedis = this.jedisPool.getResource(); }catch (Exception ignored){}
        this.channels = config.getStringList("Channels");
        encryption = new Encryption(config);

    }

    public void start(){
        this.RedisService.execute(this);
    }

    @Override
    public void run() {
        while (!isShuttingDown.get()) {
            try {
                message("&e[Jedis] &cConnecting to redis...........");
                if (!this.subscribeJedis.isConnected()) this.subscribeJedis = this.jedisPool.getResource();
                isRedisOnline.set(true);
                message("&e[Jedis] &aRedis Connected");
                int byteArr2dSize = 1;
                byte[][] channelsInByte = new byte[channels.size()][byteArr2dSize];
                boolean reInitializeByteArray;

                // Loop that reInitialize array IF array size is not enough
                do {
                    reInitializeByteArray = false;
                    try {
                        /* Data Initialization for channelsInByte array from List<String> channels */
                        for (int x = 0; x < channels.size(); x++) {
                            channelsInByte[x] = channels.get(x).getBytes();
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
                message("&e[Jedis] &cConnection to redis has failed! &ereconnecting...");
                if (this.subscribeJedis != null){this.subscribeJedis.close();}
                isRedisOnline.set(false);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void message(String message) {
        plugin.getLogger().info(ChatColor.translateAlternateColorCodes('&', message));
    }

    @Override
    public void onMessage(byte[] channel, byte[] message) {
        String channelString = new String(channel);
        try {
            String decrypted = null;
            try {
                decrypted = encryption.decrypt(message);
            } catch (UnauthenticCiphertextException | IllegalBlockSizeException e) {
                e.printStackTrace();
            }
            assert decrypted != null;
            JSONObject j = new JSONObject(decrypted);
            //System.out.println("Message got from channel: "+channel +" and the Message: " +json.toString());
            plugin.getServer().getPluginManager().callEvent(new RedisMessageEvent(channelString, j.getString("Message")));
        } catch (Exception e) {
            e.printStackTrace();
            Bukkit.getLogger().warning(ChatColor.translateAlternateColorCodes('&', "&2[&aGBot&a] &cI Got a Message that Was empty from channel " + channel + " Please check your code that you used to send the message. ^ ignore the error."));
        }

    }

    public void shutdown() {
        this.isShuttingDown.set(true);
        if (this.subscribeJedis != null){
            this.unsubscribe();
            this.subscribeJedis.close();
        }
        this.RedisService.shutdown();

    }

    public boolean IsRedisOnline() {
        return isRedisOnline.get();
    }

    public JedisPool getJedisPool() {
        return jedisPool;
    }

    public ExecutorService getRedisService() {
        return RedisService;
    }

    public AtomicBoolean getIsShuttingDown() {
        return isShuttingDown;
    }

    public AtomicBoolean getIsRedisOnline() {
        return isRedisOnline;
    }

    public Encryption getEncryption() {
        return encryption;
    }
}
