package net.limework.core;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Getter;
import net.limework.core.Managers.RedisManager;
import net.limework.core.Skript.elements.EvtRedis;
import net.limework.core.Skript.elements.ExprChannel;
import net.limework.core.events.RedisMessageEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public class LimeworkSpigotCore extends JavaPlugin {

    //Redis manager
    private RedisManager rm;

    //Skript
    private SkriptAddon addon;


    @Override
    public void onEnable() {
        saveDefaultConfig();
        rm = new RedisManager(this);


        loadSkript();
        rm.start();
    }

    @Override
    public void onDisable() {

    }

    private void loadSkript() {
        addon = Skript.registerAddon(this);
        try {
            addon.loadClasses("net.limework.core.Skript", "elements");
            Skript.registerEvent("redis message", EvtRedis.class, RedisMessageEvent.class, "redis message");
            Skript.registerExpression(ExprChannel.class, String.class, ExpressionType.SIMPLE, "redis channel");
            EventValues.registerEventValue(RedisMessageEvent.class, String.class, new Getter<String, RedisMessageEvent>() {
                @Override
                public String get(RedisMessageEvent e) {
                    return e.getChannelName();
                }
            }, 0);
            Skript.registerExpression(net.limework.skLimework.elements.ExprMessage.class, String.class, ExpressionType.SIMPLE, "redis message");
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
    
    public RedisManager getRm() {
        return rm;
    }

    public SkriptAddon getAddon() {
        return addon;
    }
}
