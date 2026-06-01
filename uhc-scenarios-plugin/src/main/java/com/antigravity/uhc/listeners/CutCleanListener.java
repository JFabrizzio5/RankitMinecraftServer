package com.antigravity.uhc.listeners;

import com.antigravity.uhc.UhcPvpScenarios;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class CutCleanListener implements Listener {
    private final UhcPvpScenarios plugin;
    private final Random random = new Random();

    public CutCleanListener(UhcPvpScenarios plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        
        Block block = event.getBlock();
        Material type = block.getType();
        
        if (plugin.isCutcleanEnabled()) {
            if (type == Material.IRON_ORE) {
                event.setCancelled(true);
                block.setType(Material.AIR);
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.IRON_INGOT, 1));
                block.getWorld().spawn(block.getLocation(), ExperienceOrb.class).setExperience(2);
                return;
            } else if (type == Material.GOLD_ORE) {
                event.setCancelled(true);
                block.setType(Material.AIR);
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.GOLD_INGOT, 1));
                block.getWorld().spawn(block.getLocation(), ExperienceOrb.class).setExperience(3);
                return;
            }
        }
        
        if (type == Material.GRAVEL) {
            if (plugin.isCutcleanEnabled() || plugin.isVanillaPlusEnabled()) {
                event.setCancelled(true);
                block.setType(Material.AIR);
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.FLINT, 1));
            }
        } else if (type == Material.LEAVES || type == Material.LEAVES_2) {
            if (plugin.isVanillaPlusEnabled()) {
                ItemStack hand = player.getItemInHand();
                if (hand != null && hand.getType() == Material.SHEARS) {
                    return; // Normal shear behavior
                }
                
                byte data = block.getData();
                boolean isOak = (type == Material.LEAVES && (data & 3) == 0);
                boolean isDarkOak = (type == Material.LEAVES_2 && (data & 3) == 1);
                
                if (isOak || isDarkOak) {
                    event.setCancelled(true);
                    block.setType(Material.AIR);
                    if (random.nextFloat() < 0.10f) {
                        block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.APPLE, 1));
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        if (plugin.isCutcleanEnabled()) {
            for (ItemStack drop : event.getDrops()) {
                if (drop == null) continue;
                Material type = drop.getType();
                if (type == Material.RAW_BEEF) {
                    drop.setType(Material.COOKED_BEEF);
                } else if (type == Material.PORK) {
                    drop.setType(Material.GRILLED_PORK);
                } else if (type == Material.MUTTON) {
                    drop.setType(Material.COOKED_MUTTON);
                } else if (type == Material.RAW_CHICKEN) {
                    drop.setType(Material.COOKED_CHICKEN);
                } else if (type == Material.RABBIT) {
                    drop.setType(Material.COOKED_RABBIT);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (!plugin.isVanillaPlusEnabled()) return;
        Block block = event.getBlock();
        Material type = block.getType();
        byte data = block.getData();
        boolean isOak = (type == Material.LEAVES && (data & 3) == 0);
        boolean isDarkOak = (type == Material.LEAVES_2 && (data & 3) == 1);
        
        if (isOak || isDarkOak) {
            event.setCancelled(true);
            block.setType(Material.AIR);
            if (random.nextFloat() < 0.10f) {
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.APPLE, 1));
            }
        }
    }
}
