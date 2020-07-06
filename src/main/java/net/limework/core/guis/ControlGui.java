package net.limework.core.guis;

import net.limework.core.LimeworkSpigotCore;
import net.limework.core.abstraction.Gui;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public class ControlGui extends Gui implements Listener, CommandExecutor {
    private LimeworkSpigotCore plugin;

    public ControlGui(LimeworkSpigotCore plugin) {
        this.plugin = plugin;
        setup("&cServer Control", 6);
        //fillGUI(Material.LIME_STAINED_GLASS_PANE);
        makeItem(Material.CRAFTING_TABLE, 1, 10, "&eCreative mode", "&cClick this for creative mode.");
        makeItem(Material.DIAMOND_SWORD, 1, 12, "&eSurvival Mode", "&bClick this for Survival mode.");
        makeItem(Material.DIAMOND, 1, 14, "&eSpectator Mode", "&eClick this for Spectator mode.");
        makeItem(Material.PUFFERFISH, 1, 16, "&eAdventure Mode", "&aClick this for Adventure mode.");
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            makeItem(Material.OAK_SIGN, 1, 37, "&eRam: " + ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000) + "MB/" + (Runtime.getRuntime().totalMemory() / 1000000) + "MB");
            makeItem(Material.PLAYER_HEAD, 1, 40, "&eOnline Players: " + plugin.getServer().getOnlinePlayers().size() + "/" + plugin.getServer().getMaxPlayers());
        }, 0, 20);
        makeItem(Material.RED_WOOL, 1, 53, "&c&lShutdown the server");
    }


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player p = ((Player) sender);
            p.openInventory(gui);
        }
        return false;
    }

    @EventHandler
    public void onInventory(InventoryClickEvent e) {
        if (e.getInventory() == gui) {
            e.setCancelled(true);
            Player p = (Player) e.getWhoClicked();
            switch (e.getSlot()) {
                case 10: {
                    p.setGameMode(GameMode.CREATIVE);
                    p.closeInventory();
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
                    break;
                }
                case 12: {
                    p.setGameMode(GameMode.SURVIVAL);
                    p.closeInventory();
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
                    break;
                }
                case 14: {
                    p.setGameMode(GameMode.SPECTATOR);
                    p.closeInventory();
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
                    break;
                }
                case 16: {
                    p.setGameMode(GameMode.ADVENTURE);
                    p.closeInventory();
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
                    break;
                }
                case 53: {
                    p.closeInventory();
                    plugin.getServer().shutdown();
                    break;
                }
                default: {
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.0f);
                    break;
                }
            }


        }

    }


}
