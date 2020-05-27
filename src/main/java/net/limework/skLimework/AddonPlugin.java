package net.limework.skLimework;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Getter;
import net.limework.skLimework.Events.RedisSub;
import net.limework.skLimework.Events.onRedisMessage;
import net.limework.skLimework.elements.EvtRedis;
import net.limework.skLimework.elements.ExprChannel;
import net.limework.skLimework.elements.ExprMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.beans.Expression;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AddonPlugin extends JavaPlugin {
    private AddonPlugin instance;
    private SkriptAddon addon;
    private JedisPool jedisPool;
    private RedisSub redisSub;
    private ExecutorService service;
    private Cipher cipher;
    private boolean encryptionEnabled;

    @Override
    public void onEnable(){
        instance = this;
        this.saveDefaultConfig();
        FileConfiguration config = this.getConfig();
        encryptionEnabled = config.getBoolean("Redis.EncryptMessages");
        if (encryptionEnabled) {
            // AES-128 encryption
            String configKey = config.getString("Redis.EncryptionKey");
            byte[] key = null;
            assert configKey != null;
            key = configKey.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = null;
            try {
                sha = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            key = Arrays.copyOf(key, 16);
            SecretKeySpec encryptionKey = new SecretKeySpec(key, "AES");

            cipher = null;
            try {
                cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
                e.printStackTrace();
            }
            try {
                cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            }
        }
        
        addon = Skript.registerAddon(this);
        try { addon.loadClasses("net.limework.skLimework", "elements");
            Skript.registerEvent("redis message", EvtRedis.class, onRedisMessage.class, "redis message");
            Skript.registerExpression(ExprChannel.class, String.class, ExpressionType.SIMPLE, "channel");
            EventValues.registerEventValue(onRedisMessage.class, String.class, new Getter<String, onRedisMessage>() {
                @Override
                public String get(onRedisMessage e) {
                    return e.getChannelName();
                }
            }, 0);
            Skript.registerExpression(ExprMessage.class, String.class, ExpressionType.SIMPLE, "redis message");
            EventValues.registerEventValue(onRedisMessage.class, String.class, new Getter<String, onRedisMessage>() {
                @Override
                public String get(onRedisMessage e) {
                    return e.getMessage();
                }
            }, 0);

            

        } catch (IOException e) { e.printStackTrace(); }
        JedisPoolConfig jconfig = new JedisPoolConfig();
        jconfig.setMaxTotal(this.getConfig().getInt("Redis.MaxConnections"));
        jconfig.setMaxIdle(this.getConfig().getInt("Redis.MaxConnections"));
        jconfig.setMinIdle(1);
        jedisPool = new JedisPool(jconfig,
                this.getConfig().getString("Redis.Host"),
                this.getConfig().getInt("Redis.Port") ,
                this.getConfig().getInt("Redis.TimeOut"),
                this.getConfig().getString("Redis.Password"),
                this.getConfig().getBoolean("Redis.useSSL"));
        redisSub = new RedisSub(this, jedisPool.getResource(), this.getConfig().getStringList("Channels"));
        service = Executors.newFixedThreadPool(this.getConfig().getInt("Redis.Threads"));
        service.execute(redisSub);

        Bukkit.getLogger().info("[Govindas limework Addon] was enabled!");
    }


    @Override
    public void onDisable(){
       redisSub.shutdown();
       service.shutdown();
       try { service.awaitTermination(3, TimeUnit.SECONDS); } catch (InterruptedException e) { e.printStackTrace(); }
       jedisPool.close();
    }

    public AddonPlugin getInstance() {
        return instance;
    }

    public SkriptAddon getAddonInstance() {
        return addon;
    }

    public ExecutorService getJedisExecutionService() {
        return service;
    }

    public JedisPool getJedisPool() { return jedisPool; }

    public boolean isEncryptionEnabled() { return encryptionEnabled; }

    public String encrypt(String message) {
        String encrypted = null;
        try {
            encrypted = Base64.getEncoder().encodeToString(cipher.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        return encrypted;
    }

    public String decrypt(String message) {
        String decrypted = null;
        try {
            decrypted = new String(cipher.doFinal(Base64.getDecoder().decode(message)), StandardCharsets.UTF_8);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        return decrypted;
    }
}
