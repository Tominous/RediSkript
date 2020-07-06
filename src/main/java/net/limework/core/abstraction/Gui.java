package net.limework.core.abstraction;

import ch.njol.skript.Skript;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

import static org.bukkit.ChatColor.translateAlternateColorCodes;

public abstract class Gui {
    protected Inventory gui;
    private int rows;
    private boolean loaded = false;
    protected String name;


    protected void setup(String name, int rows) {
        if (loaded) {
            return;
        }
        loaded = true;
        this.rows = 9 * rows;
        this.name = ChatColor.translateAlternateColorCodes('&', name);
        gui = Bukkit.createInventory(null, this.rows, this.name);
    }


    protected void makeItem(Material material, int howMany, int slot,  String name) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta item_meta = item.getItemMeta();
        item_meta.setDisplayName(translateAlternateColorCodes('&', name));
        item.setItemMeta(item_meta);
        item.setAmount(howMany);
        this.gui.clear(slot);
        this.gui.setItem(slot, item);
    }

    protected void makeItem(Material material, int howMany, int slot,  String name, String... Lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta item_meta = item.getItemMeta();
        item_meta.setDisplayName(translateAlternateColorCodes('&', name));
        List<String> lore = new ArrayList<>();
        for (String s : Lore) {
            lore.add(translateAlternateColorCodes('&', s));
        }
        item_meta.setLore(lore);
        item.setItemMeta(item_meta);
        item.setAmount(howMany);
        this.gui.clear(slot);
        this.gui.setItem(slot, item);
    }

    protected void fillGUI(Material material) {
        int slot = -1;
        while (slot <= (this.rows - 2)) {
            slot++;
            makeItem(material, 1, slot , "&1.", "");
        }

    }


}
