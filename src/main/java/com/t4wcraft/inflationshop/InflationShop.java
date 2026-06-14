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
        // Guardar la configuración por defecto si no existe
        saveDefaultConfig();
        loadConfigValues();

        // Registrar e inicializar Vault
        if (!setupEconomy()) {
            getLogger().severe(String.format("[%s] - Desactivado por falta de Vault o un plugin de Economía compatible!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Registrar eventos de inventario y el comando /ishop
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("ishop")).setExecutor(this);

        getLogger().info("¡InflationShop activado con éxito! Listo para la versión 1.20.4.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    private void loadConfigValues() {
        FileConfiguration config = getConfig();
        menuTitle = ChatColor.translateAlternateColorCodes('&', config.getString("menu.title", "&8» &0&lTIENDA CON INFLACIÓN"));
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
                    getLogger().warning("¡El material " + key + " no es válido en la configuración!");
                }
            }
        } else {
            // Valores de prueba por si se arranca sin archivo config
            shopItems.put(Material.DIAMOND, new ShopItem(Material.DIAMOND, 500.0, 250.0, 0.005, 4.0, 0.2));
            shopItems.put(Material.IRON_INGOT, new ShopItem(Material.IRON_INGOT, 50.0, 25.0, 0.002, 5.0, 0.1));
            shopItems.put(Material.GOLD_INGOT, new ShopItem(Material.GOLD_INGOT, 150.0, 75.0, 0.003, 4.0, 0.15));
        }
    }

    public void openShop(Player player) {
        Inventory inv = Bukkit.createInventory(null, menuSize, menuTitle);

        // Decoración de fondo con paneles grises
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);
        }
        for (int i = 0; i < menuSize; i++) {
            inv.setItem(i, glass);
        }

        // Colocar los ítems de venta
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
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede ser usado por jugadores.");
            return true;
        }

        Player player = (Player) sender;
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("inflationshop.admin")) {
                player.sendMessage(ChatColor.RED + "No tienes permiso para recargar la tienda.");
                return true;
            }
            reloadConfig();
            loadConfigValues();
            player.sendMessage(ChatColor.GREEN + "¡Configuración de InflationShop recargada!");
            return true;
        }

        openShop(player);
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(menuTitle)) return;
        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        Player player = (Player) event.getWhoClicked();
        Material clickedMaterial = event.getCurrentItem().getType();

        if (!shopItems.containsKey(clickedMaterial)) return;
        ShopItem shopItem = shopItems.get(clickedMaterial);

        ClickType click = event.getClick();
        boolean isBuy = click.isLeftClick();
        boolean isBulk = click.isShiftClick();
        int amount = isBulk ? 64 : 1;

        if (isBuy) {
            // Lógica de compra con cálculo incremental por inflación
            double totalCost = shopItem.calculateTotalBuyCost(amount);
            if (!econ.has(player, totalCost)) {
                player.sendMessage(ChatColor.RED + "No tienes suficiente dinero. Necesitas " + econ.format(totalCost));
                return;
            }

            if (getInventoryFreeSpace(player, clickedMaterial) < amount) {
                player.sendMessage(ChatColor.RED + "No tienes suficiente espacio en tu inventario.");
                return;
            }

            econ.withdrawPlayer(player, totalCost);
            player.getInventory().addItem(new ItemStack(clickedMaterial, amount));
            shopItem.registerTransactions(amount);

            player.sendMessage(ChatColor.GREEN + "Compraste x" + amount + " " + clickedMaterial.name() + " por " + econ.format(totalCost));
        } else {
            // Lógica de venta con caída de precios progresiva por oferta
            if (!player.getInventory().containsAtLeast(new ItemStack(clickedMaterial), amount)) {
                player.sendMessage(ChatColor.RED + "No tienes x" + amount + " " + clickedMaterial.name() + " para vender.");
                return;
            }

            double totalPayout = shopItem.calculateTotalSellPayout(amount);
            player.getInventory().removeItem(new ItemStack(clickedMaterial, amount));
            econ.depositPlayer(player, totalPayout);
            shopItem.registerTransactions(-amount);

            player.sendMessage(ChatColor.GREEN + "Vendiste x" + amount + " " + clickedMaterial.name() + " por " + econ.format(totalPayout));
        }

        openShop(player);
    }

    private int getInventoryFreeSpace(Player player, Material material) {
        int space = 0;
        for (ItemStack is : player.getInventory().getStorageContents()) {
            if (is == null || is.getType() == Material.AIR) {
                space += material.getMaxStackSize();
            } else if (is.getType() == material) {
                space += (material.getMaxStackSize() - is.getAmount());
            }
        }
        return space;
    }

    private class ShopItem {
        private final Material material;
        private final double baseBuyPrice;
        private final double baseSellPrice;
        private final double changeRate;
        private final double maxMultiplier;
        private final double minMultiplier;
        private int transactionOffset = 0;

        public ShopItem(Material material, double baseBuyPrice, double baseSellPrice, double changeRate, double maxMultiplier, double minMultiplier) {
            this.material = material;
            this.baseBuyPrice = baseBuyPrice;
            this.baseSellPrice = baseSellPrice;
            this.changeRate = changeRate;
            this.maxMultiplier = maxMultiplier;
            this.minMultiplier = minMultiplier;
        }

        private double getMultiplier(int offsetAdjustment) {
            double currentMult = 1.0 + ((transactionOffset + offsetAdjustment) * changeRate);
            return Math.max(minMultiplier, Math.min(maxMultiplier, currentMult));
        }

        public double getUnitPriceBuy() {
            return baseBuyPrice * getMultiplier(0);
        }

        public double getUnitPriceSell() {
            return baseSellPrice * getMultiplier(0);
        }

        public double calculateTotalBuyCost(int amount) {
            double total = 0;
            for (int i = 0; i < amount; i++) {
                total += baseBuyPrice * getMultiplier(i);
            }
            return total;
        }

        public double calculateTotalSellPayout(int amount) {
            double total = 0;
            for (int i = 0; i < amount; i++) {
                total += baseSellPrice * getMultiplier(-i);
            }
            return total;
        }

        public void registerTransactions(int amount) {
            this.transactionOffset += amount;
        }

        public ItemStack getGuiItem() {
            ItemStack is = new ItemStack(material);
            ItemMeta meta = is.getItemMeta();
            if (meta == null) return is;

            meta.setDisplayName(ChatColor.GOLD + "➤ " + ChatColor.YELLOW + ChatColor.BOLD + material.name());

            List<String> lore = new ArrayList<>();
            double currentMult = getMultiplier(0);
            double currentPct = (currentMult - 1.0) * 100.0;

            lore.add(ChatColor.GRAY + "-----------------------------");
            lore.add(ChatColor.WHITE + "Precio Compra (u): " + ChatColor.GREEN + "$" + String.format("%.2f", getUnitPriceBuy()));
            lore.add(ChatColor.WHITE + "Precio Venta (u): " + ChatColor.RED + "$" + String.format("%.2f", getUnitPriceSell()));
            lore.add("");

            if (currentPct > 0.1) {
                lore.add(ChatColor.GOLD + "▲ Inflación: " + ChatColor.RED + "+" + String.format("%.1f", currentPct) + "% " + ChatColor.GRAY + "(Mucha Demanda)");
            } else if (currentPct < -0.1) {
                lore.add(ChatColor.BLUE + "▼ Deflación: " + ChatColor.GREEN + String.format("%.1f", currentPct) + "% " + ChatColor.GRAY + "(Mucha Oferta)");
            } else {
                lore.add(ChatColor.DARK_GRAY + "■ Estable: " + ChatColor.GRAY + "0.0% (Precio Base)");
            }

            lore.add(ChatColor.GRAY + "-----------------------------");
            lore.add(ChatColor.YELLOW + "● Click Izquierdo: " + ChatColor.WHITE + "Comprar 1 ud.");
            lore.add(ChatColor.YELLOW + "● Click Derecho: " + ChatColor.WHITE + "Vender 1 ud.");
            lore.add(ChatColor.GOLD + "● Shift + Click Izq: " + ChatColor.WHITE + "Comprar 64 uds.");
            lore.add(ChatColor.GOLD + "● Shift + Click Der: " + ChatColor.WHITE + "Vender 64 uds.");
            lore.add(ChatColor.GRAY + "-----------------------------");

            meta.setLore(lore);
            is.setItemMeta(meta);
            return is;
        }
    }
}
```
eof

### ¿Cómo seguimos?
Una vez que pegues este código en el editor de GitHub, andá arriba a la derecha, dale a **Commit changes...** (el botón verde) y confirmalo para guardarlo.

Cuando termines este paso, avisame y te paso el código de registro (`plugin.yml`). ¡Falta re poco!
