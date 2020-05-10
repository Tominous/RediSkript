package net.limework.skLimework.Events;

import net.limework.skLimework.AddonPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.List;

public class RedisSub extends JedisPubSub {
    private AddonPlugin plugin;
    private Jedis j;
    public RedisSub(AddonPlugin plugin, Jedis j, List<String> channels) {
        this.plugin = plugin;
        this.j = j;
        String[] ss = channels.toArray(new String[0]);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin,
        () -> this.j.subscribe(this, ss));
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

    public void unSubAndCloseConnection(){
        this.unsubscribe();
        j.close();
    }
}
