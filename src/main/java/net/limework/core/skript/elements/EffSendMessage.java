package net.limework.core.skript.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import net.limework.core.RediSkript;
import net.limework.core.managers.RedisManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.Event;
import org.json.JSONObject;
import redis.clients.jedis.BinaryJedis;

import java.nio.charset.StandardCharsets;

public class EffSendMessage extends Effect {
    static {
        Skript.registerEffect(EffSendMessage.class, "send redis message to channel %string% with [message] %string%", "send redis message %string% to [channel] %string%");
    }


    private Expression<String> channel;
    private Expression<String> message;


    @Override
    protected void execute(Event event) {

        RediSkript plugin = (RediSkript) Bukkit.getPluginManager().getPlugin("RediSkript");

        String message = this.message.getSingle(event);
        String channel = this.channel.getSingle(event);

        if (message == null) {
            Bukkit.getLogger().warning(ChatColor.translateAlternateColorCodes('&', "&2[&aRediSkript&a] &cRedis message was empty. Please check your code."));
            return;
        }
        if (channel == null) {
            Bukkit.getLogger().warning(ChatColor.translateAlternateColorCodes('&', "&2[&aRediSkript&a] &cChannel was empty. Please check your code."));
            return;
        }
        assert plugin != null;
        RedisManager manager = plugin.getRm();
        manager.getRedisService().execute(() -> {
            BinaryJedis j = manager.getJedisPool().getResource();
            JSONObject json = new JSONObject();
            json.put("Message", message);
            json.put("Type", "Skript");
            json.put("Date", System.currentTimeMillis()); //for unique string every time & PING calculations
            byte[] msg;
            if (manager.getEncryption().isEncryptionEnabled()) {
                msg = manager.getEncryption().encrypt(json.toString());
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
        if (matchedPattern == 0) {
            this.channel = (Expression<String>) expressions[0];
            this.message = (Expression<String>) expressions[1];
        } else {
            this.channel = (Expression<String>) expressions[1];
            this.message = (Expression<String>) expressions[0];
        }
        return true;
    }

}