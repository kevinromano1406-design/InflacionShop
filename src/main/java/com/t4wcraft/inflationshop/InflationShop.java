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
    private final Map<Material, Double> bloques = new HashMap<>();
    private final Map<Material, Double> minerales = new HashMap<>();
    private final Map<Material, Double> armaduras = new HashMap<>();

    @Override
    public void onEnable() {
        setupEconomy();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("ishop").setExecutor(this);
        loadData();
    }

    private void loadData() {
        bloques.clear(); minerales.clear(); armaduras.clear();
        // --- BLOQUES ---
        bloques.put(Material.STONE, 5.0); bloques.put(Material.GRANITE, 5.0); bloques.put(Material.DIORITE, 5.0);
        bloques.put(Material.ANDESITE, 5.0); bloques.put(Material.COBBLESTONE, 2.0); bloques.put(Material.OBSIDIAN, 500.0);
        bloques.put(Material.DIRT, 1.0); bloques.put(Material.SAND, 1.0); bloques.put(Material.GRAVEL, 1.0);
        bloques.put(Material.OAK_PLANKS, 5.0); bloques.put(Material.SPRUCE_PLANKS, 5.0); bloques.put(Material.SPAWNER, 5000.0);
        
        // --- MINERALES ---
        minerales.put(Material.IRON_INGOT, 50.0); minerales.put(Material.IRON_BLOCK, 450.0);
        minerales.put(Material.GOLD_INGOT, 100.0); minerales.put(Material.GOLD_BLOCK, 900.0);
        minerales.put(Material.DIAMOND, 200.0); minerales.put(Material.DIAMOND_BLOCK, 1800.0);
        minerales.put(Material.EMERALD, 150.0); minerales.put(Material.NETHERITE_INGOT, 2000.0);

        // --- ARMADURAS ---
        armaduras.put(Material.IRON_CHESTPLATE, 200.0); armaduras.put(Material.DIAMOND_CHESTPLATE, 1000.0);
        armaduras.put(Material.NETHERITE_CHESTPLATE, 5000.0); armaduras.put(Material.DIAMOND_HELMET, 500.0);
    }

    private void openMenu(Player p, String cat) {
        Inventory inv = Bukkit.createInventory(null, 54, "Tienda: " + cat);
        Map<Material, Double> source = cat.equals("Bloques") ? bloques : (cat.equals("Minerales") ? minerales : armaduras);
        
        source.forEach((mat, price) -> {
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + mat.name().replace("_", " "));
            meta.setLore(Arrays.asList(ChatColor.YELLOW + "Compra: $" + price.intValue(), ChatColor.RED + "Venta: $" + (price.intValue() / 2), ChatColor.GRAY + "Izq: Comprar | Der: Vender"));
            item.setItemMeta(meta);
            inv.addItem(item);
        });
        p.openInventory(inv);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) { loadData(); sender.sendMessage("§aTienda recargada."); return true; }
        if (sender instanceof Player) openMenu((Player) sender, "Bloques");
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().startsWith("Tienda: ")) return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;
        Player p = (Player) e.getWhoClicked();
        Material mat = e.getCurrentItem().getType();
        
        Double price = bloques.getOrDefault(mat, minerales.getOrDefault(mat, armaduras.get(mat)));
        if (price != null) {
            if (e.isLeftClick() && econ.getBalance(p) >= price) {
                econ.withdrawPlayer(p, price);
                p.getInventory().addItem(new ItemStack(mat));
            } else if (e.isRightClick() && p.getInventory().contains(mat)) {
                p.getInventory().removeItem(new ItemStack(mat, 1));
                econ.depositPlayer(p, price / 2);
            }
        }
    }

    private void setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) econ = rsp.getProvider();
    }
}
