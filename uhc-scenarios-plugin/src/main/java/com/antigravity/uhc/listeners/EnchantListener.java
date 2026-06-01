package com.antigravity.uhc.listeners;

import com.antigravity.uhc.UhcPvpScenarios;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class EnchantListener implements Listener {
    private final UhcPvpScenarios plugin;

    public EnchantListener(UhcPvpScenarios plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!plugin.isEnchant17Enabled()) return;
        if (event.getInventory().getType() == InventoryType.ENCHANTING) {
            // Set 64 lapis in slot 1 (lapis slot)
            event.getInventory().setItem(1, new ItemStack(Material.INK_SACK, 64, (short) 4));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!plugin.isEnchant17Enabled()) return;
        if (event.getInventory().getType() == InventoryType.ENCHANTING) {
            // Clear slot 1 to prevent getting/dropping the lapis
            event.getInventory().setItem(1, null);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClickEnchant(InventoryClickEvent event) {
        if (!plugin.isEnchant17Enabled()) return;
        if (event.getInventory().getType() == InventoryType.ENCHANTING) {
            if (event.getRawSlot() == 1) {
                event.setCancelled(true);
                return;
            }
            if (event.isShiftClick()) {
                ItemStack current = event.getCurrentItem();
                if (current != null && current.getType() == Material.INK_SACK && current.getDurability() == 4) {
                    event.setCancelled(true);
                    return;
                }
            }
            if (event.getAction() == InventoryAction.HOTBAR_SWAP || event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) {
                if (event.getRawSlot() == 1) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDragEnchant(InventoryDragEvent event) {
        if (!plugin.isEnchant17Enabled()) return;
        if (event.getInventory().getType() == InventoryType.ENCHANTING) {
            if (event.getRawSlots().contains(1)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEnchantItem(EnchantItemEvent event) {
        if (!plugin.isEnchant17Enabled()) return;
        Player player = event.getEnchanter();
        int totalCost = event.getExpLevelCost(); 
        int vanillaCost = event.whichButton() + 1; 
        int extraCost = totalCost - vanillaCost; 
        if (extraCost > 0) {
            int currentLevel = player.getLevel();
            player.setLevel(Math.max(0, currentLevel - extraCost));
        }
        
        final Inventory inv = event.getInventory();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (inv != null && inv.getType() == InventoryType.ENCHANTING) {
                    inv.setItem(1, new ItemStack(Material.INK_SACK, 64, (short) 4));
                }
            }
        }.runTaskLater(plugin, 1L);
    }
}
