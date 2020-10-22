package net.limework.core.skript.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import net.limework.core.RediSkript;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.Event;
import org.json.JSONObject;
import redis.clients.jedis.BinaryJedis;

import java.nio.charset.StandardCharsets;

public class EffSendMessage extends Effect {
    static {
        Skript.registerEffect(EffSendMessage.class, "send redis message to channel %string% with message %string%");
    }


    private Expression<String> channel;
    private Expression<String> message;


    @Override
    protected void execute(Event event) {
        RediSkript plugin = (RediSkript) Bukkit.getPluginManager().getPlugin("RediSkript");
        String message = this.message.getSingle(event);
        String channel = this.channel.getSingle(event);
        if (message == null) {//checks if message equals null if true does not execute.
            Bukkit.getLogger().warning(ChatColor.translateAlternateColorCodes('&', "&2[&aGBot&a] &cMessage Was empty Please check your code."));
            return;
        }
        assert plugin != null;
        plugin.getRm().getRedisService().execute(() -> {
            BinaryJedis j = plugin.getRm().getJedisPool().getResource();
            JSONObject json = new JSONObject();
            json.put("Message", message);
            json.put("Type", "Skript");
            json.put("Date", System.currentTimeMillis()); //for unique string every time & PING calculations
            byte[] msg;
            if (plugin.getRm().getEncryption().isEncryptionEnabled()) {
                msg = plugin.getRm().getEncryption().encrypt(json.toString());
            } else {
                msg = json.toString().getBytes(StandardCharsets.UTF_8);
            }
            j.publish(channel.getBytes(StandardCharsets.UTF_8), msg);
            j.close();
        });

    }

    @Override
    public String toString(Event event, boolean b) {
        return "send redis message to channel " + channel.getSingle(event) + " with message " + message.getSingle(event);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parser) {
        this.channel = (Expression<String>) expressions[0];
        this.message = (Expression<String>) expressions[1];
        return true;
    }

}