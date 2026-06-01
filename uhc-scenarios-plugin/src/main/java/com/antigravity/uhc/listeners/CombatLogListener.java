package com.antigravity.uhc.listeners;

import com.antigravity.uhc.UhcPvpScenarios;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CombatLogListener implements Listener {
    private final UhcPvpScenarios plugin;

    public CombatLogListener(UhcPvpScenarios plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!plugin.isUhcActive()) return;
        
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL) return;
        
        UUID uuid = player.getUniqueId();
        plugin.getCombatLogNames().put(uuid, player.getName());
        
        // Spawn custom combat villager
        Villager villager = player.getWorld().spawn(player.getLocation(), Villager.class);
        villager.setCustomName(ChatColor.RED + "[Offline] " + player.getName());
        villager.setCustomNameVisible(true);
        villager.setProfession(Villager.Profession.PRIEST); // Sleek purple robes
        
        // Store player items
        plugin.getCombatLogInventories().put(uuid, player.getInventory().getContents());
        plugin.getCombatLogArmor().put(uuid, player.getInventory().getArmorContents());
        plugin.getCombatLogVillagers().put(uuid, villager);
        
        plugin.getLogger().info("Aldeano de desconexión invocado para " + player.getName() + " en " + player.getLocation().toString());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // 1. If UHC is not active, always send to lobby!
        if (!plugin.isUhcActive()) {
            plugin.getLobbyManager().teleToLobby(player);
            return;
        }
        
        // 2. If UHC is active, check if they are in the participants list!
        String pName = player.getName().toLowerCase();
        if (!plugin.getParticipants().contains(pName)) {
            plugin.getLobbyManager().teleToLobby(player);
            player.sendMessage(ChatColor.YELLOW + "El torneo UHC está en curso. Has entrado al Lobby como Espectador.");
            return;
        }
        
        // Check if killed offline
        if (plugin.getKilledOfflinePlayers().contains(uuid)) {
            plugin.getKilledOfflinePlayers().remove(uuid);
            
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            player.setGameMode(GameMode.SPECTATOR);
            player.teleport(player.getWorld().getSpawnLocation());
            
            player.sendMessage(ChatColor.RED + "¡Fuiste eliminado porque mataron a tu Aldeano de desconexión mientras estabas offline!");
            return;
        }
        
        // Check and remove villager
        if (plugin.getCombatLogVillagers().containsKey(uuid)) {
            Villager villager = plugin.getCombatLogVillagers().remove(uuid);
            if (villager != null && villager.isValid()) {
                player.teleport(villager.getLocation());
                villager.remove();
            }
            plugin.getCombatLogInventories().remove(uuid);
            plugin.getCombatLogArmor().remove(uuid);
            player.sendMessage(ChatColor.GREEN + "¡Te has reconectado exitosamente! Tu Aldeano de desconexión fue removido.");
        }
        
        // Teleport inside border if outside safely
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                plugin.getBorderListener().teleInsideBorderIfNeeded(player);
            }
        }, 5L);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!plugin.isUhcActive()) return;
        final Player player = event.getPlayer();
        // Set spectator and give compass
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                player.setGameMode(GameMode.SPECTATOR);
                player.getInventory().clear();
                player.getInventory().addItem(new ItemStack(Material.COMPASS));
                player.sendMessage(ChatColor.GOLD + "[UHC] ¡Has sido eliminado! Ahora estás en modo espectador. Usa tu brújula para navegar.");
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.isUhcActive()) return;
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SPECTATOR) return;
        
        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.COMPASS) {
            openSpectatorGUI(player);
            event.setCancelled(true);
        }
    }

    private void openSpectatorGUI(Player spectator) {
        List<Player> alivePlayers = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() != GameMode.SPECTATOR) {
                alivePlayers.add(p);
            }
        }
        
        int size = 9;
        while (size < alivePlayers.size() && size < 54) {
            size += 9;
        }
        
        Inventory inv = Bukkit.createInventory(null, size, ChatColor.DARK_GRAY + "Navegador de Espectador");
        
        for (int i = 0; i < alivePlayers.size() && i < 54; i++) {
            Player target = alivePlayers.get(i);
            ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwner(target.getName());
            meta.setDisplayName(ChatColor.GREEN + target.getName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "Click Izquierdo: Teletransportarse");
            lore.add(ChatColor.GOLD + "Click Derecho: Ver Inventario");
            meta.setLore(lore);
            head.setItemMeta(meta);
            inv.setItem(i, head);
        }
        
        spectator.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!plugin.isUhcActive()) return;
        if (event.getInventory() == null) return;
        if (!event.getInventory().getName().equals(ChatColor.DARK_GRAY + "Navegador de Espectador")) return;
        
        event.setCancelled(true);
        
        Player spectator = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.SKULL_ITEM) return;
        
        SkullMeta meta = (SkullMeta) clicked.getItemMeta();
        if (meta == null || meta.getOwner() == null) return;
        
        Player target = Bukkit.getPlayer(meta.getOwner());
        if (target == null || !target.isOnline()) {
            spectator.sendMessage(ChatColor.RED + "El jugador ya no está online.");
            spectator.closeInventory();
            return;
        }
        
        spectator.closeInventory();
        
        if (event.isLeftClick()) {
            spectator.teleport(target.getLocation());
            spectator.sendMessage(ChatColor.GREEN + "Teletransportado a " + target.getName() + ".");
        } else if (event.isRightClick()) {
            spectator.openInventory(target.getInventory());
            spectator.sendMessage(ChatColor.GREEN + "Viendo el inventario de " + target.getName() + ".");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (!plugin.isUhcActive()) return;
        Player player = event.getPlayer();
        if (event.getNewGameMode() == GameMode.SPECTATOR) {
            plugin.writeOfflineDeathBridge(player.getName());
        }
    }
}
