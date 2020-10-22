package net.limework.core;
import net.limework.core.hooks.SkriptHook;
import net.limework.core.managers.RedisManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class RediSkript extends JavaPlugin {

    //Redis manager
    private RedisManager rm;


    @Override
    public void onEnable() {
        saveDefaultConfig();
        rm = new RedisManager(this);
        Objects.requireNonNull(getServer().getPluginCommand("reloadredis")).setExecutor(rm);
        if (getServer().getPluginManager().getPlugin("Skript") != null) {
            new SkriptHook(this);
        } else {
            getLogger().info("Skript wasn't found.");
        }
        rm.start();




    }

    @Override
    public void onDisable() {
        rm.shutdown();
    }


    public RedisManager getRm() {
        return rm;
    }
}