package com.antigravity.uhc;

import com.antigravity.uhc.commands.ScenarioCommand;
import com.antigravity.uhc.generator.VoidGenerator;
import com.antigravity.uhc.listeners.*;
import com.antigravity.uhc.manager.LobbyManager;
import com.antigravity.uhc.manager.ScoreboardManager;
import com.antigravity.uhc.manager.PracticeManager;
import com.antigravity.uhc.tasks.ChunkPregenerator;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class UhcPvpScenarios extends JavaPlugin {

    private boolean cutcleanEnabled = false;
    private boolean nocleanEnabled = false;
    private boolean timebombEnabled = false;
    private boolean vanillaPlusEnabled = false;
    private boolean enchant17Enabled = false;
    private boolean uhcActive = false;
    
    // Trackers
    private final Map<UUID, Long> noCleanPlayers = new HashMap<>();
    private final Map<UUID, Long> borderInvinciblePlayers = new HashMap<>();
    private final Map<UUID, Villager> combatLogVillagers = new HashMap<>();
    private final Map<UUID, ItemStack[]> combatLogInventories = new HashMap<>();
    private final Map<UUID, ItemStack[]> combatLogArmor = new HashMap<>();
    private final Map<UUID, String> combatLogNames = new HashMap<>();
    private final Set<UUID> killedOfflinePlayers = new HashSet<>();
    private final Set<String> participants = new HashSet<>();
    private final Map<UUID, Integer> playerKills = new HashMap<>();

    // Scoreboard parameters
    private String serverIp = "play.rankit.net";
    private String eventName = "UHC Evento";
    private String gameState = "Espera";
    private int elapsedSeconds = 0;
    private int currentBorderSize = 2000;
    private int aliveCount = 0;

    private ChunkPregenerator pregeneratorTask = null;

    // Managers & Listeners (modularized)
    private LobbyManager lobbyManager;
    private ScoreboardManager scoreboardManager;
    private BorderListener borderListener;
    private PracticeManager practiceManager;

    @Override
    public void onEnable() {
        // Initialize managers
        this.lobbyManager = new LobbyManager(this);
        this.scoreboardManager = new ScoreboardManager(this);
        this.practiceManager = new PracticeManager(this);
        
        // Initialize listeners
        this.borderListener = new BorderListener(this);
        
        // Register events
        Bukkit.getPluginManager().registerEvents(new CutCleanListener(this), this);
        Bukkit.getPluginManager().registerEvents(new NoCleanListener(this), this);
        Bukkit.getPluginManager().registerEvents(new EnchantListener(this), this);
        Bukkit.getPluginManager().registerEvents(this.borderListener, this);
        Bukkit.getPluginManager().registerEvents(new CombatLogListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PracticeListener(this), this);
        Bukkit.getPluginManager().registerEvents(new LobbyListener(this), this);
        
        // Register Command
        getCommand("uhcscenario").setExecutor(new ScenarioCommand(this));
        
        // Register Golden Head recipe
        registerGoldenHeadRecipe();
        
        // Initialize lobby world
        try {
            WorldCreator creator = new WorldCreator("lobby");
            creator.generator(new VoidGenerator());
            org.bukkit.World lobbyWorld = Bukkit.createWorld(creator);
            lobbyManager.buildLobby(lobbyWorld);
        } catch (Exception e) {
            getLogger().severe("Error cargando/creando el mundo 'lobby': " + e.getMessage());
        }

        // Repeating task to keep players inside the border safely
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!uhcActive) return;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    borderListener.teleInsideBorderIfNeeded(p);
                }
            }
        }.runTaskTimer(this, 20L, 20L); // Check every 1 second

        // Repeating task to update scoreboards for all players every 1 second
        new BukkitRunnable() {
            @Override
            public void run() {
                scoreboardManager.updateAllScoreboards();
            }
        }.runTaskTimer(this, 20L, 20L); // Every 1 second
        
        getLogger().info("UhcPvpScenarios v1.5 modularizado cargado con éxito.");
    }

    private void registerGoldenHeadRecipe() {
        ItemStack goldenHead = new ItemStack(Material.GOLDEN_APPLE, 1);
        org.bukkit.inventory.meta.ItemMeta meta = goldenHead.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Golden Head");
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add(ChatColor.YELLOW + "Poderosa cabeza dorada.");
        lore.add(ChatColor.AQUA + "Cura 4 corazones de vida.");
        meta.setLore(lore);
        goldenHead.setItemMeta(meta);

        org.bukkit.inventory.ShapedRecipe recipe = new org.bukkit.inventory.ShapedRecipe(goldenHead);
        recipe.shape("GGG", "GHG", "GGG");
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('H', new org.bukkit.material.MaterialData(Material.SKULL_ITEM, (byte) 3));
        Bukkit.addRecipe(recipe);
    }

    @Override
    public void onDisable() {
        if (practiceManager != null) {
            practiceManager.setPracticeActive(false);
        }
        noCleanPlayers.clear();
        borderInvinciblePlayers.clear();
        for (Villager villager : combatLogVillagers.values()) {
            if (villager != null && villager.isValid()) {
                villager.remove();
            }
        }
        combatLogVillagers.clear();
        combatLogInventories.clear();
        combatLogArmor.clear();
        combatLogNames.clear();
        playerKills.clear();
        participants.clear();
        if (pregeneratorTask != null) {
            pregeneratorTask.cancel();
        }
        getLogger().info("UhcPvpScenarios v1.5 modularizado desactivado.");
    }

    // Shared utility: Write offline death to bridge file for Node.js controller
    public void writeOfflineDeathBridge(String playerName) {
        try {
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

    // Shared utility: Spawn ticking TimeBomb chest
    public void triggerTimeBomb(final Location loc, final List<ItemStack> items, final String playerName) {
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

    // Getters & Setters
    public boolean isCutcleanEnabled() { return cutcleanEnabled; }
    public void setCutcleanEnabled(boolean enabled) { this.cutcleanEnabled = enabled; }

    public boolean isNocleanEnabled() { return nocleanEnabled; }
    public void setNocleanEnabled(boolean enabled) { this.nocleanEnabled = enabled; }

    public boolean isTimebombEnabled() { return timebombEnabled; }
    public void setTimebombEnabled(boolean enabled) { this.timebombEnabled = enabled; }

    public boolean isVanillaPlusEnabled() { return vanillaPlusEnabled; }
    public void setVanillaPlusEnabled(boolean enabled) { this.vanillaPlusEnabled = enabled; }

    public boolean isEnchant17Enabled() { return enchant17Enabled; }
    public void setEnchant17Enabled(boolean enabled) { this.enchant17Enabled = enabled; }

    public boolean isUhcActive() { return uhcActive; }
    public void setUhcActive(boolean active) { this.uhcActive = active; }

    public Map<UUID, Long> getNoCleanPlayers() { return noCleanPlayers; }
    public Map<UUID, Long> getBorderInvinciblePlayers() { return borderInvinciblePlayers; }
    
    public Map<UUID, Villager> getCombatLogVillagers() { return combatLogVillagers; }
    public Map<UUID, ItemStack[]> getCombatLogInventories() { return combatLogInventories; }
    public Map<UUID, ItemStack[]> getCombatLogArmor() { return combatLogArmor; }
    public Map<UUID, String> getCombatLogNames() { return combatLogNames; }
    public Set<UUID> getKilledOfflinePlayers() { return killedOfflinePlayers; }
    public Set<String> getParticipants() { return participants; }
    public Map<UUID, Integer> getPlayerKills() { return playerKills; }

    public String getServerIp() { return serverIp; }
    public void setServerIp(String serverIp) { this.serverIp = serverIp; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public String getGameState() { return gameState; }
    public void setGameState(String gameState) { this.gameState = gameState; }

    public int getElapsedSeconds() { return elapsedSeconds; }
    public void setElapsedSeconds(int elapsedSeconds) { this.elapsedSeconds = elapsedSeconds; }

    public int getCurrentBorderSize() { return currentBorderSize; }
    public void setCurrentBorderSize(int currentBorderSize) { this.currentBorderSize = currentBorderSize; }

    public int getAliveCount() { return aliveCount; }
    public void setAliveCount(int aliveCount) { this.aliveCount = aliveCount; }

    public ChunkPregenerator getPregeneratorTask() { return pregeneratorTask; }
    public void setPregeneratorTask(ChunkPregenerator task) { this.pregeneratorTask = task; }

    public LobbyManager getLobbyManager() { return lobbyManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public BorderListener getBorderListener() { return borderListener; }
    public PracticeManager getPracticeManager() { return practiceManager; }
}
