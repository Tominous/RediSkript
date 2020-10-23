package net.limework.core.commands;

import net.limework.core.RediSkript;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReloadRedis implements CommandExecutor {
    private RediSkript plugin;
    public ReloadRedis(RediSkript plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            sender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&'
                    , "&cYou cannot execute this command.")));
            return true;
        }
        plugin.getRm().reloadRedis();
        sender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&'
                , "&eRediSkript has been reloaded!")));
        return false;
    }
}
