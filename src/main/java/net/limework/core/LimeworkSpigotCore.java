package net.limework.core;

import net.limework.core.guis.ControlGui;
import net.limework.core.hooks.SkriptHook;
import net.limework.core.managers.DataSourcesManager;
import net.limework.core.managers.RedisManager;
import org.bukkit.plugin.java.JavaPlugin;

public class LimeworkSpigotCore extends JavaPlugin {

    //Redis manager
    private RedisManager rm;

    //data
    private DataSourcesManager dsm;


    @Override
    public void onEnable() {
        saveDefaultConfig();
        rm = new RedisManager(this);
        if (getServer().getPluginManager().getPlugin("Skript") != null) {
            new SkriptHook(this);
        } else {
            getLogger().info("Skript wasn't found.");
        }
        rm.start();
        try {
            ControlGui controlGui = new ControlGui(this);
            this.getServer().getPluginManager().registerEvents(controlGui, this);
            this.getCommand("control").setExecutor(controlGui);
        } catch (NoSuchFieldError e) {
            getLogger().info("SOMETHING WENT WRONG WHEN LOADING control gui.");
            e.printStackTrace();
        }
        dsm = new DataSourcesManager(this);




    }

    @Override
    public void onDisable() {
        rm.shutdown();
        dsm.shutdown();
    }


    public RedisManager getRm() {
        return rm;
    }

    public DataSourcesManager getDsm() {
        return dsm;
    }
}