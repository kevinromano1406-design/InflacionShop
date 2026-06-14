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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
            getLogger().severe(String.format("[%s] - Desactivado por falta de Vault!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("ishop")).setExecutor(this);
        getLogger().info("¡InflationShop activado con éxito!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    private void loadConfigValues() {
        FileConfiguration config = getConfig();
        menuTitle = ChatColor.translateAlternateColorCodes('&', config.getString("menu.title", "&8» &0&lTIENDA"));
        menuSize = config.getInt("menu.size", 27);

        shopItems.clear();
        if (config.getConfigurationSection("items") != null) {
            for (String key : config.getConfigurationSection("items").getKeys(false)) {
                try {
                    Material mat = Material.valueOf(key.toUpperCase());
                    double baseBuy = config.getDouble("items." + key + ".base-buy-price");
                    double baseSell = config.getDouble("items." + key + ".base-sell-price");
                    double rate = config.getDouble("items." + key + ".change-rate", 0.002);
                    double maxMult = config.getDouble("items." + key + ".max-multiplier", 5.0);
                    double minMult = config.getDouble("items." + key + ".min-multiplier", 0.1);
                    shopItems.put(mat, new ShopItem(mat, baseBuy, baseSell, rate, maxMult, minMult));
                } catch (IllegalArgumentException e) {
                    getLogger().warning("Material inválido: " + key);
                }
            }
        }
    }

    public void openShop(Player player) {
        Inventory inv = Bukkit.createInventory(null, menuSize, menuTitle);
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); glass.setItemMeta(meta); }
        for (int i = 0; i < menuSize; i++) inv.setItem(i, glass);

        int slot = 10;
        for (ShopItem item : shopItems.values()) {
            if (slot >= menuSize - 1) break;
            if (slot % 9 == 0 || slot % 9 == 8) slot++;
            inv.setItem(slot, item.getGuiItem());
            slot++;
        }
        player.openInventory(inv);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        openShop(player);
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(menuTitle)) return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        
        Player player = (Player) event.getWhoClicked();
        ShopItem shopItem = shopItems.get(event.getCurrentItem().getType());
        if (shopItem == null) return;

        int amount = event.getClick().isShiftClick() ? 64 : 1;
        if (event.getClick().isLeftClick()) {
            double cost = shopItem.calculateTotalBuyCost(amount);
            if (econ.has(player, cost)) {
                econ.withdrawPlayer(player, cost);
                player.getInventory().addItem(new ItemStack(event.getCurrentItem().getType(), amount));
                shopItem.registerTransactions(amount);
            }
        } else {
            if (player.getInventory().containsAtLeast(new ItemStack(event.getCurrentItem().getType()), amount)) {
                player.getInventory().removeItem(new ItemStack(event.getCurrentItem().getType(), amount));
                econ.depositPlayer(player, shopItem.calculateTotalSellPayout(amount));
                shopItem.registerTransactions(-amount);
            }
        }
        openShop(player);
    }

    private class ShopItem {
        private final Material material;
        private final double baseBuyPrice, baseSellPrice, changeRate, maxMultiplier, minMultiplier;
        private int transactionOffset = 0;

        public ShopItem(Material mat, double bB, double bS, double r, double maxM, double minM) {
            this.material = mat; this.baseBuyPrice = bB; this.baseSellPrice = bS;
            this.changeRate = r; this.maxMultiplier = maxM; this.minMultiplier = minM;
        }

        private double getMultiplier(int adj) {
            return Math.max(minMultiplier, Math.min(maxMultiplier, 1.0 + ((transactionOffset + adj) * changeRate)));
        }

        public double calculateTotalBuyCost(int amount) {
            double total = 0;
            for (int i = 0; i < amount; i++) total += baseBuyPrice * getMultiplier(i);
            return total;
        }

        public double calculateTotalSellPayout(int amount) {
            double total = 0;
            for (int i = 0; i < amount; i++) total += baseSellPrice * getMultiplier(-i);
            return total;
        }

        public void registerTransactions(int amount) { this.transactionOffset += amount; }

        public ItemStack getGuiItem() {
            ItemStack is = new ItemStack(material);
            ItemMeta meta = is.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + material.name());
            is.setItemMeta(meta);
            return is;
        }
    }
}
