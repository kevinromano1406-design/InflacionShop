package com.t4wcraft.inflationshop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class InflationShop extends JavaPlugin implements Listener, CommandExecutor {
    private static Economy econ = null;
    private final Map<Material, Double> prices = new HashMap<>();

    @Override
    public void onEnable() {
        setupEconomy();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("ishop").setExecutor(this);
        loadItems();
    }

    private void loadItems() {
        prices.clear();
        // MINERALES
        prices.put(Material.DIAMOND, 150.0);
        prices.put(Material.GOLD_INGOT, 80.0);
        prices.put(Material.IRON_INGOT, 40.0);
        prices.put(Material.COAL, 10.0);
        // BLOQUES
        prices.put(Material.STONE, 5.0);
        prices.put(Material.OAK_LOG, 15.0);
        prices.put(Material.OBSIDIAN, 200.0);
        // ARMADURAS
        prices.put(Material.DIAMOND_CHESTPLATE, 1200.0);
        prices.put(Material.IRON_CHESTPLATE, 400.0);
        // FARMING
        prices.put(Material.WHEAT, 5.0);
        prices.put(Material.CARROT, 5.0);
        // SPAWNERS
        prices.put(Material.SPAWNER, 10000.0);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            loadItems();
            sender.sendMessage(ChatColor.GREEN + "¡Tienda recargada con todos los precios!");
            return true;
        }
        if (sender instanceof Player) openShop((Player) sender);
        return true;
    }

    private void openShop(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_RED + "Tienda Oficial T4WCraft");
        for (Map.Entry<Material, Double> entry : prices.entrySet()) {
            ItemStack item = new ItemStack(entry.getKey());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.AQUA + entry.getKey().name().replace("_", " "));
            meta.setLore(Arrays.asList(
                ChatColor.YELLOW + "Precio Compra: $" + String.format("%.2f", entry.getValue()),
                ChatColor.RED + "Precio Venta: $" + String.format("%.2f", entry.getValue() * 0.5),
                ChatColor.GRAY + "Izq: Comprar | Der: Vender"
            ));
            item.setItemMeta(meta);
            inv.addItem(item);
        }
        p.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().contains("Tienda")) return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;
        
        Player p = (Player) e.getWhoClicked();
        Material mat = e.getCurrentItem().getType();
        if (!prices.containsKey(mat)) return;

        double price = prices.get(mat);
        if (e.isLeftClick()) {
            if (econ.getBalance(p) >= price) {
                econ.withdrawPlayer(p, price);
                p.getInventory().addItem(new ItemStack(mat));
                prices.put(mat, price * 1.05); // Inflación
                p.sendMessage(ChatColor.GREEN + "Compraste " + mat.name() + " por $" + price);
            }
        } else if (e.isRightClick()) {
            p.getInventory().removeItem(new ItemStack(mat, 1));
            econ.depositPlayer(p, price * 0.5);
            p.sendMessage(ChatColor.RED + "Vendiste " + mat.name() + " por $" + (price * 0.5));
        }
        openShop(p);
    }

    private void setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) econ = rsp.getProvider();
    }
}
