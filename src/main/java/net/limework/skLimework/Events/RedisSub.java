package net.limework.skLimework.Events;

import net.limework.skLimework.AddonPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.List;


public class RedisSub extends JedisPubSub implements Runnable{

    private AddonPlugin plugin;
    private Jedis j;
    private String[] channels;

    public RedisSub(AddonPlugin plugin, Jedis j, List<String> channels) {
        this.plugin = plugin;
        this.j = j;
        this.channels = channels.toArray(new String[0]);
    }

    @Override
    public void run(){
        try{
            this.j.subscribe(this, channels);
        } catch (JedisConnectionException je){
            plugin.getLogger().warning("Lost connection to redis!");
            newJedis();
        }
    }


    private void newJedis() {
        //this.unsubscribe();
        this.j.close();
        while (true){
            try {
                plugin.getLogger().info("reconnecting to Redis!");
                this.j = plugin.getJedisPool().getResource();
                plugin.getLogger().info("Connected!");
                break;
            }catch (JedisConnectionException e){
                plugin.getLogger().warning("reconnecting to Redis has Failed! retrying in 4 seconds!");
                try { Thread.sleep(4000);}catch (InterruptedException ignored){}
            }
        }
        plugin.getJedisExecutionService().execute(this);
    }



    @Override
    public void onMessage(String channel, String message) {
        try {
            JSONObject json = new JSONObject(message);
            //System.out.println("Message got from channel: "+channel +" and the Message: " +json.toString());
            plugin.getServer().getPluginManager().callEvent(new onRedisMessage(channel, json.getString("Message")));
        } catch (Exception e) {
            e.printStackTrace();
            Bukkit.getLogger().warning(ChatColor.translateAlternateColorCodes('&', "&2[&aGBot&a] &cI Got a Message that Was empty from channel "+ channel +" Please check your code that you used to send the message. ^ ignore the error."));
        }

    }

    public void shutdown(){
        this.unsubscribe();
        j.close();
    }

}