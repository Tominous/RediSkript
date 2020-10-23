package net.limework.core;
import net.limework.core.commands.ReloadRedis;
import net.limework.core.hooks.SkriptHook;
import net.limework.core.managers.RedisManager;
import org.bukkit.plugin.java.JavaPlugin;

public class RediSkript extends JavaPlugin {

    //Redis manager
    private RedisManager rm;

    public void startRedis(boolean reloadConfig) {
        if (reloadConfig) {
            reloadConfig();
        }
        rm = new RedisManager(this);
        rm.start();
    }
    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (getServer().getPluginManager().getPlugin("Skript") != null) {
            startRedis(false);
            getServer().getPluginCommand("reloadredis").setExecutor(new ReloadRedis(this));
            new SkriptHook(this);
        } else {
            getLogger().info("Skript wasn't found.");
        }
    }

    @Override
    public void onDisable() {
        if (rm != null) {
            rm.shutdown();
        }
    }


    public RedisManager getRm() {
        return rm;
    }
}