package com.antigravity.uhc.listeners;

import com.antigravity.uhc.UhcPvpScenarios;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

public class NoCleanListener implements Listener {
    private final UhcPvpScenarios plugin;

    public NoCleanListener(UhcPvpScenarios plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        
        // Write the death to the bridge file immediately for 100% reliable Node.js winner/top detection
        plugin.writeOfflineDeathBridge(victim.getName());

        // Generate player head item
        org.bukkit.inventory.ItemStack head = new org.bukkit.inventory.ItemStack(org.bukkit.Material.SKULL_ITEM, 1, (short) 3);
        org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
        skullMeta.setOwner(victim.getName());
        skullMeta.setDisplayName(org.bukkit.ChatColor.GOLD + "Cabeza de " + victim.getName());
        head.setItemMeta(skullMeta);
        
        // If TimeBomb is active, steal drops and put them inside a ticking chest
        if (plugin.isTimebombEnabled()) {
            java.util.List<org.bukkit.inventory.ItemStack> drops = new java.util.ArrayList<>(event.getDrops());
            drops.add(head); // Add head to the TimeBomb chest!
            event.getDrops().clear();
            plugin.triggerTimeBomb(victim.getLocation(), drops, victim.getName());
        } else {
            // Drop head normally on the ground
            event.getDrops().add(head);

            // Spawn UHC "Palo con cabeza" (Head on Fence)
            try {
                org.bukkit.Location loc = victim.getLocation();
                org.bukkit.block.Block fenceBlock = loc.getBlock();
                fenceBlock.setType(org.bukkit.Material.FENCE);
                
                org.bukkit.block.Block skullBlock = loc.clone().add(0, 1, 0).getBlock();
                skullBlock.setType(org.bukkit.Material.SKULL);
                org.bukkit.block.Skull skullState = (org.bukkit.block.Skull) skullBlock.getState();
                skullState.setSkullType(org.bukkit.SkullType.PLAYER);
                skullState.setOwner(victim.getName());
                skullState.update();
            } catch (Exception e) {
                plugin.getLogger().warning("No se pudo generar el poste de cabeza en la muerte de " + victim.getName() + ": " + e.getMessage());
            }
        }
        
        // Increment kill count for the killer
        Player killer = victim.getKiller();
        if (killer != null) {
            UUID killerUuid = killer.getUniqueId();
            plugin.getPlayerKills().put(killerUuid, plugin.getPlayerKills().getOrDefault(killerUuid, 0) + 1);
        }
        
        if (!plugin.isNocleanEnabled()) return;
        
        if (killer != null && killer.isOnline()) {
            final UUID killerUuid = killer.getUniqueId();
            long durationMs = 20000;
            plugin.getNoCleanPlayers().put(killerUuid, System.currentTimeMillis() + durationMs);
            
            killer.sendMessage(ChatColor.GREEN + "¡Has eliminado a " + victim.getName() + "! Recibes 20 segundos de invencibilidad por NoClean.");
            
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    if (plugin.getNoCleanPlayers().containsKey(killerUuid)) {
                        long expire = plugin.getNoCleanPlayers().get(killerUuid);
                        if (System.currentTimeMillis() >= expire) {
                            plugin.getNoCleanPlayers().remove(killerUuid);
                            Player p = Bukkit.getPlayer(killerUuid);
                            if (p != null && p.isOnline()) {
                                p.sendMessage(ChatColor.RED + "¡Tu invencibilidad de NoClean ha expirado!");
                            }
                        }
                    }
                }
            }, 20 * 20L);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            if (!plugin.isUhcActive()) {
                event.setCancelled(true);
                return;
            }
        }
        
        if (!plugin.isNocleanEnabled()) return;
        
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            UUID uuid = player.getUniqueId();
            
            if (plugin.getNoCleanPlayers().containsKey(uuid)) {
                long expire = plugin.getNoCleanPlayers().get(uuid);
                if (System.currentTimeMillis() < expire) {
                    event.setCancelled(true);
                } else {
                    plugin.getNoCleanPlayers().remove(uuid);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!plugin.isNocleanEnabled()) return;
        
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            UUID uuid = attacker.getUniqueId();
            
            if (plugin.getNoCleanPlayers().containsKey(uuid)) {
                plugin.getNoCleanPlayers().remove(uuid);
                attacker.sendMessage(ChatColor.RED + "¡Tu invencibilidad de NoClean ha sido cancelada por atacar!");
            }
        }
    }

    @EventHandler
    public void onPlayerConsume(org.bukkit.event.player.PlayerItemConsumeEvent event) {
        org.bukkit.inventory.ItemStack item = event.getItem();
        if (item == null || item.getType() != org.bukkit.Material.GOLDEN_APPLE) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        
        if (item.getItemMeta().getDisplayName().equals(org.bukkit.ChatColor.GOLD + "Golden Head")) {
            Player player = event.getPlayer();
            // Golden Head effects:
            // 1. Heal 4 hearts (8 HP)
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 8.0));
            // 2. Regen II for 10s (200 ticks)
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 200, 1), true);
            // 3. Absorption I for 2m (2400 ticks)
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.ABSORPTION, 2400, 0), true);
            
            player.sendMessage(org.bukkit.ChatColor.GOLD + "¡Has consumido una Golden Head! " + org.bukkit.ChatColor.YELLOW + "(+4 corazones)");
        }
    }
}
