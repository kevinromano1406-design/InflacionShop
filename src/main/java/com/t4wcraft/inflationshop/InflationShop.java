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
        prices.put(Material.DIAMOND, 100.0);
        prices.put(Material.IRON_INGOT, 50.0);
        prices.put(Material.DIAMOND_HELMET, 500.0);
        prices.put(Material.WHEAT, 10.0);
        prices.put(Material.SPAWNER, 5000.0);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            loadItems();
            sender.sendMessage(ChatColor.GREEN + "¡Tienda recargada!");
            return true;
        }
        if (sender instanceof Player) openShop((Player) sender);
        return true;
    }

    private void openShop(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_BLUE + "Tienda T4WCraft");
        prices.forEach((mat, price) -> {
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + mat.name().replace("_", " "));
            List<String> lore = Arrays.asList(ChatColor.YELLOW + "Precio: $" + price, ChatColor.GRAY + "Izq: Comprar | Der: Vender");
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.addItem(item);
        });
        p.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().contains("Tienda")) return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null) return;
        
        Player p = (Player) e.getWhoClicked();
        Material mat = e.getCurrentItem().getType();
        if (!prices.containsKey(mat)) return;

        double price = prices.get(mat);
        if (e.isLeftClick()) { // COMPRA
            if (econ.getBalance(p) >= price) {
                econ.withdrawPlayer(p, price);
                p.getInventory().addItem(new ItemStack(mat));
                prices.put(mat, price * 1.05); // Inflación 5%
                p.sendMessage(ChatColor.GREEN + "Compraste " + mat.name() + " por $" + price);
            }
        } else if (e.isRightClick()) { // VENTA
            if (p.getInventory().contains(mat)) {
                p.getInventory().removeItem(new ItemStack(mat, 1));
                econ.depositPlayer(p, price * 0.5);
                p.sendMessage(ChatColor.RED + "Vendiste " + mat.name() + " por $" + (price * 0.5));
            }
        }
        openShop(p);
    }

    private void setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) econ = rsp.getProvider();
    }
}
