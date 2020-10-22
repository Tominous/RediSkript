package net.limework.core.hooks;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Getter;
import net.limework.core.RediSkript;
import net.limework.core.events.RedisMessageEvent;
import net.limework.core.skript.elements.EvtRedis;
import net.limework.core.skript.elements.ExprChannel;
import net.limework.core.skript.elements.ExprMessage;

import java.io.IOException;

public class SkriptHook {

    private SkriptAddon addon;
    public SkriptHook(RediSkript plugin) {
        addon = Skript.registerAddon(plugin);
        try {
            addon.loadClasses("net.limework.core.skript", "elements");
            Skript.registerEvent("redis message", EvtRedis.class, RedisMessageEvent.class, "redis message");
            Skript.registerExpression(ExprChannel.class, String.class, ExpressionType.SIMPLE, "redis channel");
            EventValues.registerEventValue(RedisMessageEvent.class, String.class, new Getter<String, RedisMessageEvent>() {
                @Override
                public String get(RedisMessageEvent e) {
                    return e.getChannelName();
                }
            }, 0);
            Skript.registerExpression(ExprMessage.class, String.class, ExpressionType.SIMPLE, "redis message");
            EventValues.registerEventValue(RedisMessageEvent.class, String.class, new Getter<String, RedisMessageEvent>() {
                @Override
                public String get(RedisMessageEvent e) {
                    return e.getMessage();
                }
            }, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SkriptAddon getAddon() {
        return addon;
    }
}