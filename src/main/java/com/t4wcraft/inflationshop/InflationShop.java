package com.t4wcraft.inflationshop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class InflationShop extends JavaPlugin implements Listener, CommandExecutor {
    private static Economy econ = null;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!setupEconomy()) {
            getLogger().severe("¡Vault no encontrado, desactivando plugin!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("ishop") != null) {
            getCommand("ishop").setExecutor(this);
        }
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        return rsp != null && (econ = rsp.getProvider()) != null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        
        String title = ChatColor.translateAlternateColorCodes('&', getConfig().getString("menu.title", "&8» &0&lTIENDA"));
        int size = getConfig().getInt("menu.size", 27);
        
        Inventory inv = Bukkit.createInventory(null, size, title);
        ItemStack item = new ItemStack(Material.DIAMOND);
        inv.setItem(13, item);
        ((Player) sender).openInventory(inv);
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = ChatColor.translateAlternateColorCodes('&', getConfig().getString("menu.title", "&8» &0&lTIENDA"));
        if (!event.getView().getTitle().equals(title)) return;
        
        event.setCancelled(true);
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() != Material.DIAMOND) return;

        Player player = (Player) event.getWhoClicked();
        double price = getConfig().getDouble("precio_diamante", 100.0);
        double tasa = getConfig().getDouble("tasa_inflacion", 1.05);

        if (econ.getBalance(player) >= price) {
            econ.withdrawPlayer(player, price);
            player.getInventory().addItem(new ItemStack(Material.DIAMOND));
            player.sendMessage(ChatColor.GREEN + "Compraste diamante por $" + String.format("%.2f", price));
            
            getConfig().set("precio_diamante", price * tasa);
            saveConfig();
        } else {
            player.sendMessage(ChatColor.RED + "No tenés suficiente dinero. Precio actual: $" + String.format("%.2f", price));
        }
    }
}
