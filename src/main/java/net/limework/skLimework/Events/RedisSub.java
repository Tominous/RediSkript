package net.limework.skLimework.Events;

import net.limework.skLimework.AddonPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.cryptomator.siv.UnauthenticCiphertextException;
import org.json.JSONObject;
import redis.clients.jedis.BinaryJedis;
import redis.clients.jedis.BinaryJedisPubSub;

import javax.crypto.IllegalBlockSizeException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


public class RedisSub extends BinaryJedisPubSub implements Runnable{

    private AddonPlugin plugin;
    private BinaryJedis j;
    private List<String> channels;
    private AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    private AtomicBoolean isRedisOnline = new AtomicBoolean();


    public RedisSub(AddonPlugin plugin, BinaryJedis j, List<String> channels) {
        this.plugin = plugin;
        this.j = j;
        this.channels = channels;
    }

    @Override
    public void run(){
        while (!isShuttingDown.get()){
            try {
                message("&e[Jedis] &cConnecting to redis...........");
                if (!this.j.isConnected()) this.j = plugin.getJedisPool().getResource();
                isRedisOnline.set(true);
                message("&e[Jedis] &aRedis Connected");
                this.j.subscribe(this,
                        channels.get(0).getBytes(),
                        channels.get(1).getBytes(),
                        channels.get(2).getBytes(),
                        channels.get(3).getBytes(),
                        channels.get(4).getBytes());
            } catch (Exception e){
                message("&e[Jedis] &cConnection to redis has failed! &ereconnecting...");
                this.j.close();
                isRedisOnline.set(false);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void message(String message){
        plugin.getLogger().info(ChatColor.translateAlternateColorCodes('&', message));
    }

    @Override
    public void onMessage(byte[] channel, byte[] message) {
        AddonPlugin plugin = (AddonPlugin) Bukkit.getPluginManager().getPlugin("SKLimework");
        String channelString = new String(channel);
        try {
            String decrypted = null;
            try {
                assert plugin != null;
                decrypted = plugin.decrypt(message);
            } catch (UnauthenticCiphertextException | IllegalBlockSizeException e) {
                e.printStackTrace();
            }
            assert decrypted != null;
            JSONObject j = new JSONObject(decrypted);
            //System.out.println("Message got from channel: "+channel +" and the Message: " +json.toString());
            plugin.getServer().getPluginManager().callEvent(new onRedisMessage(channelString, j.getString("Message")));
        } catch (Exception e) {
            e.printStackTrace();
            Bukkit.getLogger().warning(ChatColor.translateAlternateColorCodes('&', "&2[&aGBot&a] &cI Got a Message that Was empty from channel "+ channel +" Please check your code that you used to send the message. ^ ignore the error."));
        }

    }

    public void shutdown(){
        this.isShuttingDown.set(true);
        this.unsubscribe();
        j.close();
    }

    public boolean IsRedisOnline() {
        return isRedisOnline.get();
    }

}