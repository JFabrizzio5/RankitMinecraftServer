package com.antigravity.uhc.listeners;

import com.antigravity.uhc.UhcPvpScenarios;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PracticeListener implements Listener {
    private final UhcPvpScenarios plugin;

    public PracticeListener(UhcPvpScenarios plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPracticeDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();

        // Check if player is in practice
        if (!plugin.getPracticeManager().getPracticePlayers().contains(victim.getUniqueId())) return;

        // Cancel fall damage in lobby/practice arena to keep it smooth
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
            return;
        }

        // Intercept death (Fake Death system)
        double finalDamage = event.getFinalDamage();
        if (victim.getHealth() - finalDamage <= 0) {
            event.setCancelled(true);
            
            // Trigger fake death defeat
            try {
                plugin.getPracticeManager().handlePracticeDefeat(victim);
            } catch (Exception e) {
                plugin.getLogger().severe("Error handling practice defeat for player " + victim.getName() + ": " + e.getMessage());
                // Fallback: heal manually to prevent true death if something crashes
                victim.setHealth(victim.getMaxHealth());
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        if (plugin.getPracticeManager().getPracticePlayers().contains(p.getUniqueId())) {
            plugin.getPracticeManager().leavePractice(p);
        }
    }
}
