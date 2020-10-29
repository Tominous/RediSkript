package net.limework.core;
import net.limework.core.commands.ReloadRedis;
import net.limework.core.hooks.SkriptHook;
import net.limework.core.managers.RedisManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.java.JavaPlugin;

public class RediSkript extends JavaPlugin {

    //Redis manager
    private RedisManager rm;

    public void startRedis(boolean reload) {
        if (reload) { reloadConfig(); }
        rm = new RedisManager(this);
        rm.start();
    }
    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (getServer().getPluginManager().getPlugin("Skript") != null) {
            startRedis(false);

            PluginCommand command = getServer().getPluginCommand("reloadredis");
            assert command != null;
            command.setExecutor(new ReloadRedis(this));

            new SkriptHook(this);
        } else {
            getLogger().info("Skript wasn't found.");
        }
    }

    @Override
    //using HIGHEST event priority so it shuts down last and code can still execute well in "on script unload" and "on skript unload" events
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDisable() {
        if (rm != null) {
            rm.shutdown();
        }
    }


    public RedisManager getRm() {
        return rm;
    }
}