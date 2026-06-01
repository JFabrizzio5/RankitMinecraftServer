package com.antigravity.uhc.manager;

import com.antigravity.uhc.UhcPvpScenarios;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class PracticeManager {
    private final UhcPvpScenarios plugin;
    private boolean practiceActive = false;
    private final Set<UUID> practicePlayers = new HashSet<>();
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Integer> deaths = new HashMap<>();
    private final Map<UUID, UUID> activeDuels = new HashMap<>(); // p1 -> p2
    private final List<UUID> duelQueue = new ArrayList<>(); // Matchmaking Queue

    public PracticeManager(UhcPvpScenarios plugin) {
        this.plugin = plugin;
    }

    public List<UUID> getDuelQueue() {
        return duelQueue;
    }

    public void toggleQueue(Player p) {
        if (!practiceActive) {
            p.sendMessage(ChatColor.RED + "El modo Practice no está activado en este momento.");
            return;
        }

        UUID uuid = p.getUniqueId();
        if (duelQueue.contains(uuid)) {
            duelQueue.remove(uuid);
            p.sendMessage(ChatColor.RED + "[Cola] Has salido de la cola de búsqueda 1v1.");
        } else {
            // Remove from practice FFA active play if they want to duel
            duelQueue.add(uuid);
            p.sendMessage(ChatColor.GREEN + "[Cola] Entraste a la cola de búsqueda 1v1. Buscando oponente...");
            
            // Check if match is found
            if (duelQueue.size() >= 2) {
                Player p1 = Bukkit.getPlayer(duelQueue.remove(0));
                Player p2 = Bukkit.getPlayer(duelQueue.remove(0));
                
                if (p1 != null && p1.isOnline() && p2 != null && p2.isOnline()) {
                    p1.sendMessage(ChatColor.GREEN + "[Cola] ¡Partida encontrada!");
                    p2.sendMessage(ChatColor.GREEN + "[Cola] ¡Partida encontrada!");
                    startDuel(p1, p2);
                } else {
                    // Cleanup offline players from queue
                    if (p1 != null && p1.isOnline()) duelQueue.add(p1.getUniqueId());
                    if (p2 != null && p2.isOnline()) duelQueue.add(p2.getUniqueId());
                }
            }
        }
    }

    public boolean isPracticeActive() {
        return practiceActive;
    }

    public void setPracticeActive(boolean active) {
        this.practiceActive = active;
        if (active) {
            World lobbyWorld = Bukkit.getWorld("lobby");
            if (lobbyWorld != null) {
                buildPracticeArena(lobbyWorld);
            }
            Bukkit.broadcastMessage(ChatColor.GOLD + "[Practice] " + ChatColor.GREEN + "¡Modo Practice ACTIVADO! Escribe /uhcscenario practice join para unirte.");
        } else {
            // Kick everyone back to lobby
            for (UUID uuid : new ArrayList<>(practicePlayers)) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    leavePractice(p);
                }
            }
            practicePlayers.clear();
            activeDuels.clear();
            Bukkit.broadcastMessage(ChatColor.GOLD + "[Practice] " + ChatColor.RED + "Modo Practice DESACTIVADO.");
        }
    }

    public Set<UUID> getPracticePlayers() {
        return practicePlayers;
    }

    public Map<UUID, Integer> getKills() {
        return kills;
    }

    public Map<UUID, Integer> getDeaths() {
        return deaths;
    }

    public Map<UUID, UUID> getActiveDuels() {
        return activeDuels;
    }

    public void buildPracticeArena(World world) {
        if (world == null) return;
        plugin.getLogger().info("Generando la arena de combate Practice a la coordenada X=200, Y=100, Z=200...");

        int radius = 22;
        // Build flat Sandstone platform
        for (int x = 200 - radius; x <= 200 + radius; x++) {
            for (int z = 200 - radius; z <= 200 + radius; z++) {
                double distanceSq = Math.pow(x - 200, 2) + Math.pow(z - 200, 2);
                if (distanceSq <= radius * radius) {
                    world.getBlockAt(x, 100, z).setType(Material.SANDSTONE);
                    // Add decorative ring
                    if (distanceSq >= (radius - 1) * (radius - 1)) {
                        world.getBlockAt(x, 100, z).setType(Material.SMOOTH_BRICK);
                    }
                }
            }
        }

        // Walls around the platform to prevent fallouts
        for (int y = 101; y <= 105; y++) {
            for (int x = 200 - radius; x <= 200 + radius; x++) {
                for (int z = 200 - radius; z <= 200 + radius; z++) {
                    double distance = Math.sqrt(Math.pow(x - 200, 2) + Math.pow(z - 200, 2));
                    if (distance >= radius - 0.8 && distance <= radius + 0.8) {
                        if (y == 105) {
                            world.getBlockAt(x, y, z).setType(Material.GLOWSTONE);
                        } else {
                            world.getBlockAt(x, y, z).setType(Material.GLASS);
                        }
                    }
                }
            }
        }
        plugin.getLogger().info("Arena de combate Practice generada exitosamente.");
    }

    public void joinPractice(Player p) {
        if (!practiceActive) {
            p.sendMessage(ChatColor.RED + "El modo Practice no está activado en este momento.");
            return;
        }

        practicePlayers.add(p.getUniqueId());
        kills.putIfAbsent(p.getUniqueId(), 0);
        deaths.putIfAbsent(p.getUniqueId(), 0);

        p.sendMessage(ChatColor.GOLD + "=============================================");
        p.sendMessage(ChatColor.YELLOW + "¡Te has unido a la " + ChatColor.AQUA + "Arena de Práctica (Practice FFA)" + ChatColor.YELLOW + "!");
        p.sendMessage(ChatColor.GRAY + "Prepárate para combatir. Las muertes aquí no te eliminarán del servidor.");
        p.sendMessage(ChatColor.GOLD + "=============================================");

        resetPlayerForPractice(p);
    }

    public void leavePractice(Player p) {
        practicePlayers.remove(p.getUniqueId());
        activeDuels.remove(p.getUniqueId());
        duelQueue.remove(p.getUniqueId()); // Remove from duel queue if in it
        // Remove from values of activeDuels too
        UUID opponent = null;
        for (Map.Entry<UUID, UUID> entry : activeDuels.entrySet()) {
            if (entry.getValue().equals(p.getUniqueId())) {
                opponent = entry.getKey();
                break;
            }
        }
        if (opponent != null) {
            activeDuels.remove(opponent);
            Player oppPlayer = Bukkit.getPlayer(opponent);
            if (oppPlayer != null) {
                oppPlayer.sendMessage(ChatColor.RED + "Tu oponente se desconectó de Practice. Regresando al Lobby...");
                resetPlayerForPractice(oppPlayer);
            }
        }

        plugin.getLobbyManager().teleToLobby(p);
        p.sendMessage(ChatColor.GREEN + "Has salido de Practice y has regresado al Lobby Spawn.");
    }

    public void resetPlayerForPractice(Player p) {
        p.setGameMode(GameMode.SURVIVAL);
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
        p.setHealth(p.getMaxHealth());
        p.setFoodLevel(20);
        p.setExp(0);
        p.setLevel(0);
        for (PotionEffect effect : p.getActivePotionEffects()) {
            p.removePotionEffect(effect.getType());
        }

        // Teleport to practice area center
        World lobbyWorld = Bukkit.getWorld("lobby");
        if (lobbyWorld != null) {
            p.teleport(new Location(lobbyWorld, 200.5, 101.5, 200.5, 0f, 0f));
        } else {
            p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        }

        givePracticeKit(p);
    }

    public void givePracticeKit(Player p) {
        p.getInventory().clear();

        // 1. Weapons & Tools
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addEnchantment(Enchantment.DAMAGE_ALL, 1);
        ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.setDisplayName(ChatColor.AQUA + "Espada de Combate");
        sword.setItemMeta(swordMeta);
        p.getInventory().setItem(0, sword);

        ItemStack bow = new ItemStack(Material.BOW);
        bow.addEnchantment(Enchantment.ARROW_DAMAGE, 1);
        p.getInventory().setItem(1, bow);

        // 2. Consumables
        p.getInventory().setItem(2, new ItemStack(Material.GOLDEN_APPLE, 4)); // Gapples
        p.getInventory().setItem(3, new ItemStack(Material.GOLDEN_APPLE, 1, (short) 1)); // Notch Apple
        p.getInventory().setItem(4, new ItemStack(Material.COOKED_BEEF, 16));

        // 3. Potions (Speed II, Fire Resistance, Healing Splash)
        p.getInventory().setItem(5, new ItemStack(Material.POTION, 2, (short) 8226)); // Speed II Drinkable (1:30)
        p.getInventory().setItem(6, new ItemStack(Material.POTION, 1, (short) 8259)); // Fire Resistance (8:00)
        
        // Healing II splash fill remaining
        for (int i = 7; i <= 8; i++) {
            p.getInventory().setItem(i, new ItemStack(Material.POTION, 1, (short) 16421)); // Splash Healing II
        }
        for (int i = 9; i <= 27; i++) {
            p.getInventory().setItem(i, new ItemStack(Material.POTION, 1, (short) 16421));
        }

        p.getInventory().setItem(28, new ItemStack(Material.ARROW, 64));

        // 4. Armor
        ItemStack helmet = new ItemStack(Material.IRON_HELMET);
        helmet.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1);
        ItemStack chest = new ItemStack(Material.IRON_CHESTPLATE);
        chest.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1);
        ItemStack legs = new ItemStack(Material.IRON_LEGGINGS);
        legs.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1);
        ItemStack boots = new ItemStack(Material.IRON_BOOTS);
        boots.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1);

        p.getInventory().setHelmet(helmet);
        p.getInventory().setChestplate(chest);
        p.getInventory().setLeggings(legs);
        p.getInventory().setBoots(boots);

        p.updateInventory();
    }

    public void startDuel(final Player p1, final Player p2) {
        if (!practiceActive) {
            p1.sendMessage(ChatColor.RED + "El modo Practice no está activado.");
            p2.sendMessage(ChatColor.RED + "El modo Practice no está activado.");
            return;
        }

        practicePlayers.add(p1.getUniqueId());
        practicePlayers.add(p2.getUniqueId());
        kills.putIfAbsent(p1.getUniqueId(), 0);
        deaths.putIfAbsent(p1.getUniqueId(), 0);
        kills.putIfAbsent(p2.getUniqueId(), 0);
        deaths.putIfAbsent(p2.getUniqueId(), 0);

        activeDuels.put(p1.getUniqueId(), p2.getUniqueId());
        activeDuels.put(p2.getUniqueId(), p1.getUniqueId());

        // Prepare inventories & stats
        p1.setGameMode(GameMode.SURVIVAL);
        p2.setGameMode(GameMode.SURVIVAL);
        p1.getInventory().clear();
        p2.getInventory().clear();
        p1.setHealth(p1.getMaxHealth());
        p2.setHealth(p2.getMaxHealth());
        p1.setFoodLevel(20);
        p2.setFoodLevel(20);

        for (PotionEffect effect : p1.getActivePotionEffects()) p1.removePotionEffect(effect.getType());
        for (PotionEffect effect : p2.getActivePotionEffects()) p2.removePotionEffect(effect.getType());

        // Teleport to opposite sides of Sandstone Arena
        World lobbyWorld = Bukkit.getWorld("lobby");
        if (lobbyWorld == null) {
            lobbyWorld = Bukkit.getWorlds().get(0);
        }
        
        final Location loc1 = new Location(lobbyWorld, 186.5, 101.5, 200.5, -90f, 0f);
        final Location loc2 = new Location(lobbyWorld, 213.5, 101.5, 200.5, 90f, 0f);

        p1.teleport(loc1);
        p2.teleport(loc2);

        givePracticeKit(p1);
        givePracticeKit(p2);

        // Apply Slowness X and Blindness for 3s countdown
        p1.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 70, 9));
        p1.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 70, 9));
        p2.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 70, 9));
        p2.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 70, 9));

        // Broad duel message
        Bukkit.broadcastMessage(ChatColor.GOLD + "[Practice] " + ChatColor.YELLOW + "¡DUELO INICIADO! " + 
                ChatColor.AQUA + p1.getName() + ChatColor.WHITE + " vs " + ChatColor.LIGHT_PURPLE + p2.getName());

        new BukkitRunnable() {
            int count = 3;

            @Override
            public void run() {
                if (!practiceActive || !practicePlayers.contains(p1.getUniqueId()) || !practicePlayers.contains(p2.getUniqueId())) {
                    cancel();
                    return;
                }

                if (count > 0) {
                    p1.sendMessage(ChatColor.YELLOW + "El duelo comienza en: " + ChatColor.RED + count);
                    p2.sendMessage(ChatColor.YELLOW + "El duelo comienza en: " + ChatColor.RED + count);
                    
                    // Simple play effect if possible (sound)
                    p1.playSound(p1.getLocation(), org.bukkit.Sound.NOTE_PLING, 1f, 1f);
                    p2.playSound(p2.getLocation(), org.bukkit.Sound.NOTE_PLING, 1f, 1f);
                    
                    count--;
                } else {
                    p1.sendMessage(ChatColor.GREEN + "¡LUCHEN!");
                    p2.sendMessage(ChatColor.GREEN + "¡LUCHEN!");
                    
                    p1.playSound(p1.getLocation(), org.bukkit.Sound.NOTE_PLING, 1f, 2f);
                    p2.playSound(p2.getLocation(), org.bukkit.Sound.NOTE_PLING, 1f, 2f);
                    
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 10L, 20L); // delay 0.5s then tick every 1s
    }

    public void handlePracticeDefeat(Player victim) {
        UUID killerUuid = activeDuels.get(victim.getUniqueId());
        
        // Handle statistics
        deaths.put(victim.getUniqueId(), deaths.getOrDefault(victim.getUniqueId(), 0) + 1);

        Player killer = null;
        if (killerUuid != null) {
            killer = Bukkit.getPlayer(killerUuid);
            if (killer != null) {
                kills.put(killerUuid, kills.getOrDefault(killerUuid, 0) + 1);
            }
        } else {
            killer = victim.getKiller();
            if (killer != null) {
                kills.put(killer.getUniqueId(), kills.getOrDefault(killer.getUniqueId(), 0) + 1);
            }
        }

        // Play killer's chosen cosmetic kill sound
        if (killer != null) {
            String soundType = com.antigravity.uhc.listeners.LobbyListener.killSounds.get(killer.getUniqueId());
            if (soundType != null) {
                if (soundType.equals("thunder")) {
                    victim.getWorld().playSound(victim.getLocation(), Sound.AMBIENCE_THUNDER, 1.0f, 1.0f);
                    victim.getWorld().strikeLightningEffect(victim.getLocation());
                } else if (soundType.equals("wolf")) {
                    victim.getWorld().playSound(victim.getLocation(), Sound.WOLF_HOWL, 1.0f, 1.0f);
                } else if (soundType.equals("villager")) {
                    victim.getWorld().playSound(victim.getLocation(), Sound.VILLAGER_YES, 1.0f, 1.0f);
                }
            } else {
                victim.getWorld().playSound(victim.getLocation(), Sound.ORB_PICKUP, 1.0f, 1.0f);
            }
        } else {
            victim.getWorld().playSound(victim.getLocation(), Sound.HURT_FLESH, 1.0f, 1.0f);
        }

        // Broadcast defeat
        String killerName = killer != null ? killer.getName() : "el entorno";
        String broadcastMsg = ChatColor.GOLD + "[Practice] " + ChatColor.AQUA + victim.getName() + 
                ChatColor.YELLOW + " ha sido derrotado por " + ChatColor.LIGHT_PURPLE + killerName + ChatColor.YELLOW + "!";
        Bukkit.broadcastMessage(broadcastMsg);

        // Terminate duel association
        activeDuels.remove(victim.getUniqueId());
        if (killerUuid != null) activeDuels.remove(killerUuid);

        // Reset players
        resetPlayerForPractice(victim);
        if (killer != null && killerUuid != null) { // Only reset killer if it was a duel
            resetPlayerForPractice(killer);
            killer.sendMessage(ChatColor.GREEN + "¡Victoria! Has ganado el duelo.");
        }
        
        victim.sendMessage(ChatColor.RED + "Has perdido el duelo. ¡Suerte en la próxima!");
    }
}
