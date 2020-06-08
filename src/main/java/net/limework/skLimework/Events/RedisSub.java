package net.limework.skLimework.Events;

import net.limework.skLimework.AddonPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.cryptomator.siv.UnauthenticCiphertextException;
import org.json.JSONObject;
import redis.clients.jedis.BinaryJedis;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.exceptions.JedisConnectionException;

import javax.crypto.IllegalBlockSizeException;
import java.util.Collections;
import java.util.List;


public class RedisSub extends BinaryJedisPubSub implements Runnable{

    private AddonPlugin plugin;
    private BinaryJedis j;
    private Boolean isShuttingDown = false;
    private List<String> channels;

    public RedisSub(AddonPlugin plugin, BinaryJedis j, List<String> channels) {
        this.plugin = plugin;
        this.j = j;
        this.channels = channels;
    }

    @Override
    public void run(){
        try{
            this.j.subscribe(this, channels.get(0).getBytes(), channels.get(1).getBytes(), channels.get(2).getBytes(), channels.get(3).getBytes(), channels.get(4).getBytes());

        } catch (Exception je){
            plugin.getLogger().warning("Lost connection to redis!");
            newJedis();
        }
    }


    private void newJedis() {
        //this.unsubscribe();
        this.j.close();
        while (!isShuttingDown){
            try {
                plugin.getLogger().info("reconnecting to Redis!");
                this.j = plugin.getJedisPool().getResource();
                plugin.getLogger().info("Connected!");
                break;
            }catch (Exception e){
                plugin.getLogger().warning("reconnecting to Redis has Failed! retrying in 4 seconds!");
                try { Thread.sleep(4000);}catch (InterruptedException ignored){}
            }
        }
        if (isShuttingDown) return;
        plugin.getJedisExecutionService().execute(this);
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
        isShuttingDown = true;
        this.unsubscribe();
        j.close();
    }

}