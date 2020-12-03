package net.limework.rediskript;

import net.limework.rediskript.commands.CommandReloadRedis;
import net.limework.rediskript.managers.RedisManager;
import net.limework.rediskript.skript.SkriptHook;
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
        startRedis(false);

        PluginCommand command = getServer().getPluginCommand("reloadredis");
        assert command != null;
        command.setExecutor(new CommandReloadRedis(this));

        new SkriptHook(this);
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