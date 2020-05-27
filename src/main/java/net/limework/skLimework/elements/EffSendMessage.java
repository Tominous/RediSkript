package net.limework.skLimework.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import net.limework.skLimework.AddonPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.Event;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class EffSendMessage extends Effect {
//"hi"
    static {
        Skript.registerEffect(EffSendMessage.class, "send redis message to channel %string% with message %string%");
    }


    private Expression<String> channel;
    private Expression<String> message;


    @Override
    protected void execute(Event event) {
        AddonPlugin plugin = (AddonPlugin) Bukkit.getPluginManager().getPlugin("SKLimework");
        String message = this.message.getSingle(event);
        String channel = this.channel.getSingle(event);
        if (message == null){//checks if message equals null if true does not execute.
            Bukkit.getLogger().warning(ChatColor.translateAlternateColorCodes('&', "&2[&aGBot&a] &cMessage Was empty Please check your code."));
            return;
        }
        assert plugin != null;
        plugin.getJedisExecutionService().execute(() -> {
            Jedis j = plugin.getJedisPool().getResource();
            JSONObject json = new JSONObject();
            try {
                if (plugin.isEncryptionEnabled()) {
                    json.put("Message", plugin.encrypt(message));
                } else {
                    json.put("Message", message);
                }
                json.put("Type", "Skript");
                j.publish(channel, json.toString());
                //System.out.println("SkriptSide sent MESSAGE: ["+ message + "] to channel: " + channel + " and json: \n" + json.toString());
            }catch (Exception e){e.printStackTrace();}
            j.close();
        });

    }

    @Override
    public String toString(Event event, boolean b) {
        return null;
    }
    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        this.channel = (Expression<String>) expressions[0];
        this.message = (Expression<String>) expressions[1];
        return true;
    }

}