package net.limework.core.usefulstuff;

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
        if (loaded){
            return;
        }
        loaded = true;
        this.rows = 9 * rows;
        this.name = ChatColor.translateAlternateColorCodes('&', name);
        gui = Bukkit.createInventory(null, this.rows, this.name);
    }


    protected void makeItem(int slot, Material material, String name, String... prelore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta item_meta = item.getItemMeta();
        item_meta.setDisplayName(translateAlternateColorCodes('&', name));
        List<String> lore = new ArrayList<String>();
        for (String s : prelore) {

            lore.add(translateAlternateColorCodes('&', s));
        }
        item_meta.setLore(lore);
        item.setItemMeta(item_meta);
        this.gui.clear(slot);
        this.gui.setItem(slot, item);
    }

    protected void fillGUI(Material material) {
        int x = -1;
        while (x <= (this.rows - 2)) {
            x++;
            makeItem(x, material, "&1.", "");
        }

    }


}
