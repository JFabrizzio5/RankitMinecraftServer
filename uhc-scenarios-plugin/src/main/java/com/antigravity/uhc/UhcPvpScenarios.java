package com.antigravity.uhc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class UhcPvpScenarios extends JavaPlugin implements CommandExecutor, Listener {

    private boolean cutcleanEnabled = false;
    private boolean nocleanEnabled = false;
    private boolean timebombEnabled = false;
    private boolean uhcActive = false;
    
    // Map to track active invincibilities: Player UUID -> Expiration time in ms
    private final Map<UUID, Long> noCleanPlayers = new HashMap<>();

    // Combat Logger trackers
    private final Map<UUID, Villager> combatLogVillagers = new HashMap<>();
    private final Map<UUID, ItemStack[]> combatLogInventories = new HashMap<>();
    private final Map<UUID, ItemStack[]> combatLogArmor = new HashMap<>();
    private final Set<UUID> killedOfflinePlayers = new HashSet<>();

    // Scoreboard parameters
    private String serverIp = "play.rankit.net";
    private String eventName = "UHC Evento";
    private String gameState = "Espera";
    private int elapsedSeconds = 0;
    private int currentBorderSize = 2000;
    private int aliveCount = 0;

    // Track player kills
    private final Map<UUID, Integer> playerKills = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("uhcscenario").setExecutor(this);
        
        // Repeating task to keep players inside the border safely
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!uhcActive) return;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    teleInsideBorderIfNeeded(p);
                }
            }
        }.runTaskTimer(this, 40L, 40L); // Check every 2 seconds

        // Repeating task to update scoreboards for all players every 1 second
        new BukkitRunnable() {
            @Override
            public void run() {
                updateAllScoreboards();
            }
        }.runTaskTimer(this, 20L, 20L); // Every 1 second
        
        getLogger().info("UhcPvpScenarios v1.4 cargado con éxito.");
    }

    @Override
    public void onDisable() {
        noCleanPlayers.clear();
        // Remove any left-over combat log villagers on plugin disable to prevent persistence issues
        for (Villager villager : combatLogVillagers.values()) {
            if (villager != null && villager.isValid()) {
                villager.remove();
            }
        }
        combatLogVillagers.clear();
        combatLogInventories.clear();
        combatLogArmor.clear();
        playerKills.clear();
        getLogger().info("UhcPvpScenarios v1.4 desactivado.");
    }

    private void updateAllScoreboards() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                updatePlayerScoreboard(p);
            } catch (Exception e) {
                // Prevent crashes from scoreboard conflicts
            }
        }
    }

    private void updatePlayerScoreboard(Player p) {
        org.bukkit.scoreboard.ScoreboardManager sm = Bukkit.getScoreboardManager();
        org.bukkit.scoreboard.Scoreboard sb = p.getScoreboard();
        if (sb == sm.getMainScoreboard()) {
            sb = sm.getNewScoreboard();
            p.setScoreboard(sb);
        }
        
        org.bukkit.scoreboard.Objective obj = sb.getObjective("uhcsb");
        if (obj != null) {
            obj.unregister();
        }
        
        obj = sb.registerNewObjective("uhcsb", "dummy");
        obj.setDisplaySlot(org.bukkit.scoreboard.DisplaySlot.SIDEBAR);
        obj.setDisplayName(ChatColor.GOLD + ChatColor.BOLD.toString() + eventName.toUpperCase());
        
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.GRAY + "----------------------");
        
        if (uhcActive) {
            lines.add(ChatColor.YELLOW + "Fase: " + ChatColor.WHITE + gameState);
            
            // Format elapsed seconds as MM:SS or HH:MM:SS
            int h = elapsedSeconds / 3600;
            int m = (elapsedSeconds % 3600) / 60;
            int s = elapsedSeconds % 60;
            String timeStr = (h > 0 ? String.format("%02d:%02d:%02d", h, m, s) : String.format("%02d:%02d", m, s));
            lines.add(ChatColor.YELLOW + "Tiempo: " + ChatColor.GREEN + timeStr);
            lines.add(""); // Empty line
            lines.add(ChatColor.YELLOW + "Borde: " + ChatColor.RED + "±" + currentBorderSize);
            lines.add(ChatColor.YELLOW + "Vivos: " + ChatColor.GREEN + aliveCount);
            lines.add(""); // Empty line
            int kills = playerKills.getOrDefault(p.getUniqueId(), 0);
            lines.add(ChatColor.YELLOW + "Kills: " + ChatColor.RED + kills);
        } else {
            lines.add(ChatColor.YELLOW + "Estado: " + ChatColor.WHITE + "Esperando...");
            lines.add(ChatColor.YELLOW + "Conectados: " + ChatColor.GREEN + Bukkit.getOnlinePlayers().size());
            lines.add(""); // Empty line
            lines.add(ChatColor.YELLOW + "Seed: " + ChatColor.AQUA + "1746271928");
        }
        
        lines.add(ChatColor.GRAY + "---------------------");
        lines.add(ChatColor.AQUA + serverIp);
        
        // Set scores descending
        int score = lines.size();
        for (String line : lines) {
            if (line.isEmpty()) {
                line = getUniqueEmptyLine(score);
            }
            if (line.length() > 40) {
                line = line.substring(0, 40);
            }
            obj.getScore(line).setScore(score);
            score--;
        }
    }

    private String getUniqueEmptyLine(int score) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < score; i++) {
            sb.append("§r");
        }
        return sb.toString();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("uhcscenario")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Uso: /uhcscenario <cutclean|noclean|timebomb|active|scoreboard> <true|false|valores...>");
                return true;
            }
            
            String scenario = args[0].toLowerCase();
            String valStr = args[1].toLowerCase();
            boolean value = valStr.equals("true") || valStr.equals("on") || valStr.equals("1");
            
            if (scenario.equals("cutclean")) {
                cutcleanEnabled = value;
                sender.sendMessage(ChatColor.GREEN + "CutClean configurado en: " + value);
                Bukkit.broadcastMessage(ChatColor.GOLD + "[UHC] Escenario CutClean " + 
                        (value ? ChatColor.GREEN + "ACTIVADO" : ChatColor.RED + "DESACTIVADO"));
                return true;
            } else if (scenario.equals("noclean")) {
                nocleanEnabled = value;
                if (!value) {
                    noCleanPlayers.clear();
                }
                sender.sendMessage(ChatColor.GREEN + "NoClean configurado en: " + value);
                Bukkit.broadcastMessage(ChatColor.GOLD + "[UHC] Escenario NoClean " + 
                        (value ? ChatColor.GREEN + "ACTIVADO" : ChatColor.RED + "DESACTIVADO"));
                return true;
            } else if (scenario.equals("timebomb")) {
                timebombEnabled = value;
                sender.sendMessage(ChatColor.GREEN + "TimeBomb configurado en: " + value);
                Bukkit.broadcastMessage(ChatColor.GOLD + "[UHC] Escenario TimeBomb " + 
                        (value ? ChatColor.GREEN + "ACTIVADO" : ChatColor.RED + "DESACTIVADO"));
                return true;
            } else if (scenario.equals("scoreboard")) {
                if (args.length >= 7) {
                    serverIp = args[1];
                    eventName = args[2].replace("_", " ");
                    gameState = args[3].replace("_", " ");
                    try {
                        elapsedSeconds = Integer.parseInt(args[4]);
                        currentBorderSize = Integer.parseInt(args[5]);
                        aliveCount = Integer.parseInt(args[6]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Error parseando números.");
                    }
                    updateAllScoreboards();
                }
                return true;
            } else if (scenario.equals("active")) {
                uhcActive = value;
                if (!value) {
                    // Reset combat log state
                    for (Villager villager : combatLogVillagers.values()) {
                        if (villager != null && villager.isValid()) {
                            villager.remove();
                        }
                    }
                    combatLogVillagers.clear();
                    combatLogInventories.clear();
                    combatLogArmor.clear();
                    killedOfflinePlayers.clear();
                    
                    // Reset scoreboard variables and kills
                    playerKills.clear();
                    elapsedSeconds = 0;
                    gameState = "Espera";
                }
                sender.sendMessage(ChatColor.GREEN + "UHC activo configurado en: " + value);
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "Escenario no reconocido. Opciones: cutclean, noclean, timebomb, scoreboard, active");
                return true;
            }
        }
        return false;
    }

    // ==========================================
    // CUTCLEAN EVENTS
    // ==========================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!cutcleanEnabled) return;
        
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        
        Block block = event.getBlock();
        Material type = block.getType();
        
        if (type == Material.IRON_ORE) {
            event.setCancelled(true);
            block.setType(Material.AIR);
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.IRON_INGOT, 1));
            block.getWorld().spawn(block.getLocation(), ExperienceOrb.class).setExperience(2);
        } else if (type == Material.GOLD_ORE) {
            event.setCancelled(true);
            block.setType(Material.AIR);
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.GOLD_INGOT, 1));
            block.getWorld().spawn(block.getLocation(), ExperienceOrb.class).setExperience(3);
        } else if (type == Material.GRAVEL) {
            event.setCancelled(true);
            block.setType(Material.AIR);
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.FLINT, 1));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        if (cutcleanEnabled) {
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
        
        // Handle Combat Logger Villager Death
        if (event.getEntity() instanceof Villager) {
            Villager villager = (Villager) event.getEntity();
            UUID ownerUuid = null;
            
            for (Map.Entry<UUID, Villager> entry : combatLogVillagers.entrySet()) {
                if (entry.getValue().getEntityId() == villager.getEntityId()) {
                    ownerUuid = entry.getKey();
                    break;
                }
            }
            
            if (ownerUuid != null) {
                combatLogVillagers.remove(ownerUuid);
                killedOfflinePlayers.add(ownerUuid);
                
                ItemStack[] contents = combatLogInventories.remove(ownerUuid);
                ItemStack[] armor = combatLogArmor.remove(ownerUuid);
                
                Location loc = villager.getLocation();
                event.getDrops().clear(); // Prevent default emerald drops
                
                List<ItemStack> itemsToDrop = new ArrayList<>();
                if (contents != null) {
                    for (ItemStack item : contents) {
                        if (item != null && item.getType() != Material.AIR) {
                            itemsToDrop.add(item);
                        }
                    }
                }
                if (armor != null) {
                    for (ItemStack item : armor) {
                        if (item != null && item.getType() != Material.AIR) {
                            itemsToDrop.add(item);
                        }
                    }
                }
                
                String victimName = Bukkit.getOfflinePlayer(ownerUuid).getName();
                
                // Write offline death to bridge file for Node.js controller
                writeOfflineDeathBridge(victimName);
                
                if (timebombEnabled) {
                    triggerTimeBomb(loc, itemsToDrop, victimName);
                } else {
                    for (ItemStack item : itemsToDrop) {
                        loc.getWorld().dropItemNaturally(loc, item);
                    }
                }
                
                Bukkit.broadcastMessage(ChatColor.RED + "¡El Aldeano de desconexión de " + victimName + " ha sido asesinado!");
            }
        }
    }

    // ==========================================
    // NOCLEAN EVENTS
    // ==========================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        
        // Write the death to the bridge file immediately for 100% reliable Node.js winner/top detection
        writeOfflineDeathBridge(victim.getName());
        
        // If TimeBomb is active, steal drops and put them inside a ticking chest
        if (timebombEnabled) {
            List<ItemStack> drops = new ArrayList<>(event.getDrops());
            event.getDrops().clear();
            triggerTimeBomb(victim.getLocation(), drops, victim.getName());
        }
        
        // Increment kill count for the killer
        Player killer = victim.getKiller();
        if (killer != null) {
            UUID killerUuid = killer.getUniqueId();
            playerKills.put(killerUuid, playerKills.getOrDefault(killerUuid, 0) + 1);
        }
        
        if (!nocleanEnabled) return;
        
        if (killer != null && killer.isOnline()) {
            final UUID killerUuid = killer.getUniqueId();
            long durationMs = 20000;
            noCleanPlayers.put(killerUuid, System.currentTimeMillis() + durationMs);
            
            killer.sendMessage(ChatColor.GREEN + "¡Has eliminado a " + victim.getName() + "! Recibes 20 segundos de invencibilidad por NoClean.");
            
            Bukkit.getScheduler().runTaskLater(this, new Runnable() {
                @Override
                public void run() {
                    if (noCleanPlayers.containsKey(killerUuid)) {
                        long expire = noCleanPlayers.get(killerUuid);
                        if (System.currentTimeMillis() >= expire) {
                            noCleanPlayers.remove(killerUuid);
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
            if (!uhcActive) {
                event.setCancelled(true);
                return;
            }
        }
        
        if (!nocleanEnabled) return;
        
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            UUID uuid = player.getUniqueId();
            
            if (noCleanPlayers.containsKey(uuid)) {
                long expire = noCleanPlayers.get(uuid);
                if (System.currentTimeMillis() < expire) {
                    event.setCancelled(true);
                } else {
                    noCleanPlayers.remove(uuid);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!nocleanEnabled) return;
        
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            UUID uuid = attacker.getUniqueId();
            
            if (noCleanPlayers.containsKey(uuid)) {
                noCleanPlayers.remove(uuid);
                attacker.sendMessage(ChatColor.RED + "¡Tu invencibilidad de NoClean ha sido cancelada por atacar!");
            }
        }
    }

    // ==========================================
    // COMBAT LOGGER (DISCONNECT VILLAGER) & RECONNECTION
    // ==========================================

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!uhcActive) return;
        
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL) return;
        
        UUID uuid = player.getUniqueId();
        
        // Spawn custom combat villager
        Villager villager = player.getWorld().spawn(player.getLocation(), Villager.class);
        villager.setCustomName(ChatColor.RED + "[Offline] " + player.getName());
        villager.setCustomNameVisible(true);
        villager.setProfession(Villager.Profession.PRIEST); // Sleek purple robes
        
        // Store player items
        combatLogInventories.put(uuid, player.getInventory().getContents());
        combatLogArmor.put(uuid, player.getInventory().getArmorContents());
        combatLogVillagers.put(uuid, villager);
        
        getLogger().info("Aldeano de desconexión invocado para " + player.getName() + " en " + player.getLocation().toString());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Check if killed offline
        if (killedOfflinePlayers.contains(uuid)) {
            killedOfflinePlayers.remove(uuid);
            
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            player.setGameMode(GameMode.SPECTATOR);
            player.teleport(player.getWorld().getSpawnLocation());
            
            player.sendMessage(ChatColor.RED + "¡Fuiste eliminado porque mataron a tu Aldeano de desconexión mientras estabas offline!");
            return;
        }
        
        // Check and remove villager
        if (combatLogVillagers.containsKey(uuid)) {
            Villager villager = combatLogVillagers.remove(uuid);
            if (villager != null && villager.isValid()) {
                player.teleport(villager.getLocation());
                villager.remove();
            }
            combatLogInventories.remove(uuid);
            combatLogArmor.remove(uuid);
            player.sendMessage(ChatColor.GREEN + "¡Te has reconectado exitosamente! Tu Aldeano de desconexión fue removido.");
        }
        
        // Teleport inside border if outside safely
        if (uhcActive) {
            Bukkit.getScheduler().runTaskLater(this, new Runnable() {
                @Override
                public void run() {
                    teleInsideBorderIfNeeded(player);
                }
            }, 5L);
        }
    }

    // ==========================================
    // TIMEBOMB CHEST GENERATOR
    // ==========================================

    private void triggerTimeBomb(final Location loc, final List<ItemStack> items, final String playerName) {
        final Block blockLeft = loc.getBlock();
        final Block blockRight = blockLeft.getRelative(org.bukkit.block.BlockFace.EAST);
        
        blockLeft.setType(Material.CHEST);
        blockRight.setType(Material.CHEST);
        
        Chest chestLeft = (Chest) blockLeft.getState();
        Chest chestRight = (Chest) blockRight.getState();
        
        int leftSize = chestLeft.getInventory().getSize();
        int index = 0;
        for (ItemStack item : items) {
            if (item == null || item.getType() == Material.AIR) continue;
            if (index < leftSize) {
                chestLeft.getInventory().setItem(index, item);
            } else if (index < leftSize * 2) {
                chestRight.getInventory().setItem(index - leftSize, item);
            }
            index++;
        }
        
        Bukkit.broadcastMessage(ChatColor.GOLD + "[TimeBomb] " + ChatColor.YELLOW + "La tumba de " + playerName + " explotará en 30 segundos.");
        
        // Hologram armorstand
        final ArmorStand hologram = loc.getWorld().spawn(loc.clone().add(0.5, -0.5, 0.5), ArmorStand.class);
        hologram.setGravity(false);
        hologram.setCanPickupItems(false);
        hologram.setVisible(false);
        hologram.setCustomName(ChatColor.RED + "TimeBomb: 30s");
        hologram.setCustomNameVisible(true);
        
        new BukkitRunnable() {
            int secondsLeft = 30;
            
            @Override
            public void run() {
                if (secondsLeft <= 0) {
                    hologram.remove();
                    blockLeft.setType(Material.AIR);
                    blockRight.setType(Material.AIR);
                    
                    loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), 4.0F, false, true);
                    Bukkit.broadcastMessage(ChatColor.RED + "¡La TimeBomb de " + playerName + " ha explotado!");
                    
                    cancel();
                    return;
                }
                
                secondsLeft--;
                hologram.setCustomName(ChatColor.RED + "TimeBomb: " + secondsLeft + "s");
            }
        }.runTaskTimer(this, 20L, 20L);
    }

    private void writeOfflineDeathBridge(String playerName) {
        try {
            // Write to mc-data/offline-deaths.txt
            File mcDataDir = getDataFolder().getParentFile().getParentFile();
            File bridgeFile = new File(mcDataDir, "offline-deaths.txt");
            
            FileWriter writer = new FileWriter(bridgeFile, true);
            writer.write(playerName + "\n");
            writer.close();
            
            getLogger().info("Muerte offline registrada en puente txt para: " + playerName);
        } catch (IOException e) {
            getLogger().severe("Error al escribir en offline-deaths.txt: " + e.getMessage());
        }
    }

    // ==========================================
    // SPECTATOR COMPASS & SAFE BORDER TELEPORT
    // ==========================================

    @EventHandler
    public void onPlayerRespawn(org.bukkit.event.player.PlayerRespawnEvent event) {
        if (!uhcActive) return;
        final Player player = event.getPlayer();
        // Set spectator and give compass
        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
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
    public void onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        if (!uhcActive) return;
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
        
        org.bukkit.inventory.Inventory inv = Bukkit.createInventory(null, size, ChatColor.DARK_GRAY + "Navegador de Espectador");
        
        for (int i = 0; i < alivePlayers.size() && i < 54; i++) {
            Player target = alivePlayers.get(i);
            ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
            org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
            meta.setOwner(target.getName());
            meta.setDisplayName(ChatColor.GREEN + target.getName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "Click Izquierdo: Teletransportarse");
            lore.add(ChatColor.GOLD + "Click Derecho: Ver Inventario");
            head.setItemMeta(meta);
            inv.setItem(i, head);
        }
        
        spectator.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!uhcActive) return;
        if (event.getInventory() == null) return;
        if (!event.getInventory().getName().equals(ChatColor.DARK_GRAY + "Navegador de Espectador")) return;
        
        event.setCancelled(true);
        
        Player spectator = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.SKULL_ITEM) return;
        
        org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) clicked.getItemMeta();
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onSuffocationDamage(EntityDamageEvent event) {
        if (!uhcActive) return;
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

    private void teleInsideBorderIfNeeded(Player player) {
        if (player.getGameMode() == GameMode.SPECTATOR) return;
        org.bukkit.WorldBorder border = player.getWorld().getWorldBorder();
        double size = border.getSize() / 2.0;
        double centerX = border.getCenter().getX();
        double centerZ = border.getCenter().getZ();
        
        double px = player.getLocation().getX();
        double pz = player.getLocation().getZ();
        
        if (Math.abs(px - centerX) > (size - 2.0) || Math.abs(pz - centerZ) > (size - 2.0)) {
            Location safeLoc = getRandomSafeLocationInside(player.getWorld(), centerX, centerZ, size - 5.0);
            player.teleport(safeLoc);
            player.sendMessage(ChatColor.RED + "¡Fuiste teletransportado adentro porque te encontrabas fuera del borde!");
        }
    }

    private Location getRandomSafeLocationInside(org.bukkit.World world, double centerX, double centerZ, double maxRadius) {
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
