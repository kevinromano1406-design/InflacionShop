package com.t4wcraft.inflationshop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class InflationShop extends JavaPlugin implements Listener, CommandExecutor {
    private static Economy econ = null;
    private final Map<Material, ShopItem> shopItems = new HashMap<>();
    private String menuTitle;
    private int menuSize;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();
        if (!setupEconomy()) {
            getLogger().severe("¡Vault no encontrado, desactivando plugin!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("ishop")).setExecutor(this);
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        return rsp != null && (econ = rsp.getProvider()) != null;
    }

    private void loadConfigValues() {
        FileConfiguration config = getConfig();
        menuTitle = ChatColor.translateAlternateColorCodes('&', config.getString("menu.title", "&8» &0&lTIENDA"));
        menuSize = config.getInt("menu.size", 27);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Inventory inv = Bukkit.createInventory(null, menuSize, menuTitle);
            ((Player) sender).openInventory(inv);
        }
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(menuTitle)) event.setCancelled(true);
    }

    // Clase interna para evitar errores de compilación
    private static class ShopItem {
        public ShopItem() {}
    }
}
