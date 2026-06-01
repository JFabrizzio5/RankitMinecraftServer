package com.antigravity.uhc.manager;

import com.antigravity.uhc.UhcPvpScenarios;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class LobbyManager {
    private final UhcPvpScenarios plugin;
    private int activeLobby = 1; // 1 = Quartz Dome, 2 = Emerald/Obsidian Dome

    public LobbyManager(UhcPvpScenarios plugin) {
        this.plugin = plugin;
    }

    public int getActiveLobby() {
        return activeLobby;
    }

    public void setActiveLobby(int activeLobby) {
        this.activeLobby = activeLobby;
    }

    public void buildLobby(World world) {
        if (world == null) return;
        plugin.getLogger().info("Construyendo Lobby 1 (Cúpula de Cuarzo) en el mundo 'lobby'...");
        
        // ------------------ BUILD LOBBY 1 (Quartz at X=0, Y=100, Z=0) ------------------
        int radius = 12;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x*x + z*z <= radius*radius) {
                    world.getBlockAt(x, 100, z).setType(Material.QUARTZ_BLOCK);
                    if (x*x + z*z >= (radius-1)*(radius-1)) {
                        world.getBlockAt(x, 100, z).setType(Material.GLOWSTONE);
                    }
                }
            }
        }
        
        int domeHeight = 8;
        for (int y = 101; y <= 101 + domeHeight; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = Math.sqrt(x*x + (y-101)*(y-101) + z*z);
                    if (distance >= radius - 0.5 && distance <= radius + 0.5) {
                        if (y == 101 + domeHeight) {
                            world.getBlockAt(x, y, z).setType(Material.GLOWSTONE);
                        } else {
                            world.getBlockAt(x, y, z).setType(Material.GLASS);
                        }
                    }
                    if (distance < radius - 0.5) {
                        world.getBlockAt(x, y, z).setType(Material.AIR);
                    }
                }
            }
        }

        // ------------------ BUILD LOBBY 2 (Emerald & Obsidian at X=1000, Y=100, Z=1000) ------------------
        plugin.getLogger().info("Construyendo Lobby 2 (Cúpula de Esmeralda y Obsidiana) en el mundo 'lobby'...");
        for (int x = 1000 - radius; x <= 1000 + radius; x++) {
            for (int z = 1000 - radius; z <= 1000 + radius; z++) {
                int dx = x - 1000;
                int dz = z - 1000;
                if (dx*dx + dz*dz <= radius*radius) {
                    if (dx*dx + dz*dz >= (radius-1)*(radius-1)) {
                        world.getBlockAt(x, 100, z).setType(Material.OBSIDIAN);
                    } else {
                        world.getBlockAt(x, 100, z).setType(Material.EMERALD_BLOCK);
                    }
                }
            }
        }

        for (int y = 101; y <= 101 + domeHeight; y++) {
            for (int x = 1000 - radius; x <= 1000 + radius; x++) {
                for (int z = 1000 - radius; z <= 1000 + radius; z++) {
                    int dx = x - 1000;
                    int dz = z - 1000;
                    double distance = Math.sqrt(dx*dx + (y-101)*(y-101) + dz*dz);
                    if (distance >= radius - 0.5 && distance <= radius + 0.5) {
                        if (y == 101 + domeHeight) {
                            world.getBlockAt(x, y, z).setType(Material.GLOWSTONE);
                        } else {
                            // Green Stained Glass (Data value 5 is lime green stained glass in 1.8.8)
                            world.getBlockAt(x, y, z).setType(Material.STAINED_GLASS);
                            world.getBlockAt(x, y, z).setData((byte) 5);
                        }
                    }
                    if (distance < radius - 0.5) {
                        world.getBlockAt(x, y, z).setType(Material.AIR);
                    }
                }
            }
        }
        
        world.setSpawnLocation(0, 101, 0);
        plugin.getLogger().info("Ambos Lobbies construídos exitosamente.");
    }

    public void teleToLobby(Player player) {
        World lobbyWorld = Bukkit.getWorld("lobby");
        if (lobbyWorld != null) {
            if (activeLobby == 1) {
                player.teleport(new Location(lobbyWorld, 0.5, 101.5, 0.5, 0f, 0f));
            } else {
                player.teleport(new Location(lobbyWorld, 1000.5, 101.5, 1000.5, 0f, 0f));
            }
        } else {
            player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        }
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setExp(0);
        player.setLevel(0);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        // Welcome message
        player.sendMessage(ChatColor.GOLD + "=============================================");
        player.sendMessage(ChatColor.YELLOW + "  ¡Bienvenido a " + ChatColor.AQUA + ChatColor.BOLD.toString() + "RANKIT PvP NETWORK" + ChatColor.YELLOW + "!");
        player.sendMessage(ChatColor.GRAY + "  Usa tus ítems para combatir en Practice, desafiar");
        player.sendMessage(ChatColor.GRAY + "  a otros jugadores, o personalizar tus cosméticos.");
        player.sendMessage(ChatColor.GOLD + "=============================================");

        // Equip lobby items
        // 1. Espada de Hierro: Practice FFA (Slot 0)
        ItemStack practiceFfa = new ItemStack(Material.IRON_SWORD);
        ItemMeta pfMeta = practiceFfa.getItemMeta();
        pfMeta.setDisplayName(ChatColor.GREEN + ChatColor.BOLD.toString() + "Practice FFA " + ChatColor.GRAY + "(Click Derecho)");
        java.util.List<String> pfLore = new java.util.ArrayList<>();
        pfLore.add(ChatColor.YELLOW + "Click Derecho para entrar a la arena.");
        pfMeta.setLore(pfLore);
        practiceFfa.setItemMeta(pfMeta);
        player.getInventory().setItem(0, practiceFfa);

        // 2. Espada de Oro: Retar a Duelo 1v1 (Slot 1)
        ItemStack challengeDuel = new ItemStack(Material.GOLD_SWORD);
        ItemMeta cdMeta = challengeDuel.getItemMeta();
        cdMeta.setDisplayName(ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "Duelos 1v1 " + ChatColor.GRAY + "(Click Derecho)");
        java.util.List<String> cdLore = new java.util.ArrayList<>();
        cdLore.add(ChatColor.YELLOW + "Click Derecho para seleccionar un oponente.");
        cdMeta.setLore(cdLore);
        challengeDuel.setItemMeta(cdMeta);
        player.getInventory().setItem(1, challengeDuel);

        // 3. Redstone Comparator: Ajustes y Cosméticos (Slot 4)
        ItemStack cosmetics = new ItemStack(Material.REDSTONE_COMPARATOR);
        ItemMeta cosMeta = cosmetics.getItemMeta();
        cosMeta.setDisplayName(ChatColor.YELLOW + ChatColor.BOLD.toString() + "Ajustes y Cosméticos " + ChatColor.GRAY + "(Click Derecho)");
        java.util.List<String> cosLore = new java.util.ArrayList<>();
        cosLore.add(ChatColor.YELLOW + "Click Derecho para ocultar jugadores,");
        cosLore.add(ChatColor.YELLOW + "seleccionar partículas y efectos de bajas.");
        cosMeta.setLore(cosLore);
        cosmetics.setItemMeta(cosMeta);
        player.getInventory().setItem(4, cosmetics);

        // 4. Brújula: Ver Partida UHC (Slot 2) - Only if UHC is active
        if (plugin.isUhcActive()) {
            ItemStack spectateUhc = new ItemStack(Material.COMPASS);
            ItemMeta suMeta = spectateUhc.getItemMeta();
            suMeta.setDisplayName(ChatColor.AQUA + ChatColor.BOLD.toString() + "Ver Partida UHC " + ChatColor.GRAY + "(Click Derecho)");
            java.util.List<String> suLore = new java.util.ArrayList<>();
            suLore.add(ChatColor.YELLOW + "Click Derecho para teletransportarse a la partida.");
            suMeta.setLore(suLore);
            spectateUhc.setItemMeta(suMeta);
            player.getInventory().setItem(2, spectateUhc);
        }

        // 5. Reloj: Selector de Lobby (Slot 7)
        ItemStack lobbySelector = new ItemStack(Material.WATCH);
        ItemMeta lsMeta = lobbySelector.getItemMeta();
        lsMeta.setDisplayName(ChatColor.YELLOW + ChatColor.BOLD.toString() + "Selector de Lobby " + ChatColor.GRAY + "(Click Derecho)");
        java.util.List<String> lsLore = new java.util.ArrayList<>();
        lsLore.add(ChatColor.YELLOW + "Click Derecho para elegir entre");
        lsLore.add(ChatColor.YELLOW + "la Planta A (Cuarzo) y Planta B (Esmeralda).");
        lsMeta.setLore(lsLore);
        lobbySelector.setItemMeta(lsMeta);
        player.getInventory().setItem(7, lobbySelector);

        // 6. Estrella del Nether: Panel de Control (Slot 8) - Only if OP
        if (player.isOp()) {
            ItemStack controlPanel = new ItemStack(Material.NETHER_STAR);
            ItemMeta cpMeta = controlPanel.getItemMeta();
            cpMeta.setDisplayName(ChatColor.GOLD + ChatColor.BOLD.toString() + "Panel de Administrador " + ChatColor.GRAY + "(Click Derecho)");
            java.util.List<String> cpLore = new java.util.ArrayList<>();
            cpLore.add(ChatColor.YELLOW + "Click Derecho para abrir el menú de UHC.");
            cpMeta.setLore(cpLore);
            controlPanel.setItemMeta(cpMeta);
            player.getInventory().setItem(8, controlPanel);
        }

        player.updateInventory();
    }
}
