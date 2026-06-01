package com.antigravity.uhc.listeners;

import com.antigravity.uhc.UhcPvpScenarios;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class BorderListener implements Listener {
    private final UhcPvpScenarios plugin;

    public BorderListener(UhcPvpScenarios plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSuffocationDamage(EntityDamageEvent event) {
        if (!plugin.isUhcActive()) return;
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (player.getGameMode() == GameMode.SPECTATOR) return;
            
            if (event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION) {
                Location loc = player.getLocation();
                Block block = loc.getBlock();
                if (block.getType() == Material.BEDROCK || block.getRelative(org.bukkit.block.BlockFace.UP).getType() == Material.BEDROCK) {
                    event.setCancelled(true);
                    int safeY = player.getWorld().getHighestBlockYAt(loc.getBlockX(), loc.getBlockZ());
                    if (safeY < 50) safeY = 100;
                    Location safeLoc = new Location(player.getWorld(), loc.getX(), safeY + 1.5, loc.getZ());
                    player.teleport(safeLoc);
                    player.sendMessage(ChatColor.GREEN + "¡Fuiste liberado de los bloques de Bedrock de forma segura!");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBorderInvincibilityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (plugin.getBorderInvinciblePlayers().containsKey(player.getUniqueId())) {
                long expire = plugin.getBorderInvinciblePlayers().get(player.getUniqueId());
                if (System.currentTimeMillis() < expire) {
                    event.setCancelled(true);
                } else {
                    plugin.getBorderInvinciblePlayers().remove(player.getUniqueId());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamageByEntityBorder(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            if (plugin.getBorderInvinciblePlayers().containsKey(attacker.getUniqueId())) {
                plugin.getBorderInvinciblePlayers().remove(attacker.getUniqueId());
                attacker.sendMessage(ChatColor.RED + "¡Tu invulnerabilidad del borde ha sido cancelada por atacar!");
            }
        }
    }

    public void teleInsideBorderIfNeeded(Player player) {
        if (player.getGameMode() == GameMode.SPECTATOR) return;
        WorldBorder border = player.getWorld().getWorldBorder();
        double size = border.getSize() / 2.0;
        double centerX = border.getCenter().getX();
        double centerZ = border.getCenter().getZ();
        
        double px = player.getLocation().getX();
        double pz = player.getLocation().getZ();
        
        // Use tolerance size - 0.5 to avoid teleporting valid players standing next to the wall
        if (Math.abs(px - centerX) > (size - 0.5) || Math.abs(pz - centerZ) > (size - 0.5)) {
            Location safeLoc = getRandomSafeLocationInside(player.getWorld(), centerX, centerZ, size - 5.0);
            player.teleport(safeLoc);
            player.sendMessage(ChatColor.RED + "¡Fuiste teletransportado adentro porque te encontrabas fuera del borde!");
            
            // Apply 10s border protection & countdown title
            final UUID uuid = player.getUniqueId();
            plugin.getBorderInvinciblePlayers().put(uuid, System.currentTimeMillis() + 10000L);
            
            new BukkitRunnable() {
                int sec = 10;
                @Override
                public void run() {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null || !p.isOnline() || !plugin.isUhcActive()) {
                        cancel();
                        return;
                    }
                    if (sec <= 0) {
                        plugin.getBorderInvinciblePlayers().remove(uuid);
                        p.sendTitle(ChatColor.RED + "¡PROTECCIÓN FINALIZADA!", ChatColor.YELLOW + "Ya puedes recibir daño");
                        cancel();
                        return;
                    }
                    p.sendTitle(ChatColor.GREEN + "PROTECCIÓN DE BORDE", ChatColor.YELLOW + "Segundos restantes: " + ChatColor.RED + sec);
                    sec--;
                }
            }.runTaskTimer(plugin, 0L, 20L);
        }
    }

    private Location getRandomSafeLocationInside(World world, double centerX, double centerZ, double maxRadius) {
        java.util.Random rand = new java.util.Random();
        double angle = rand.nextDouble() * 2.0 * Math.PI;
        double radius = rand.nextDouble() * maxRadius;
        double rx = centerX + radius * Math.cos(angle);
        double rz = centerZ + radius * Math.sin(angle);
        
        int y = world.getHighestBlockYAt((int) rx, (int) rz);
        if (y < 50) y = 80;
        return new Location(world, rx, y + 1.5, rz);
    }
}
