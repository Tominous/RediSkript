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
import org.cryptomator.siv.SivMode;
import org.cryptomator.siv.UnauthenticCiphertextException;
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
    private boolean encryptionEnabled;
    private String encryptionKey;
    private String macKey;
    private final SivMode AES_SIV = new SivMode();

    @Override
    public void onEnable(){
        instance = this;
        this.saveDefaultConfig();
        FileConfiguration config = this.getConfig();
        encryptionEnabled = config.getBoolean("Redis.EncryptMessages");
        if (encryptionEnabled) {
            // AES-128 encryption
            encryptionKey = config.getString("Redis.EncryptionKey");
            macKey = config.getString("Redis.MacKey");
        }
        
        addon = Skript.registerAddon(this);
        try { addon.loadClasses("net.limework.skLimework", "elements");
            Skript.registerEvent("redis message", EvtRedis.class, onRedisMessage.class, "redis message");
            Skript.registerExpression(ExprChannel.class, String.class, ExpressionType.SIMPLE, "redis channel");
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

    public String decrypt(byte[] message) throws UnauthenticCiphertextException, IllegalBlockSizeException {
        return new String(AES_SIV.decrypt(encryptionKey.getBytes(), macKey.getBytes(), message), StandardCharsets.UTF_8);
    }

    public byte[] encrypt(String message) {
        return AES_SIV.encrypt(encryptionKey.getBytes(), macKey.getBytes(), message.getBytes());
    }
}
