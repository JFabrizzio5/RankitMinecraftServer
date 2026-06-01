package com.antigravity.uhc.tasks;

import com.antigravity.uhc.UhcPvpScenarios;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

public class ChunkPregenerator extends BukkitRunnable {
    private final UhcPvpScenarios plugin;
    private final World world;
    private final int radiusChunks;
    private final CommandSender sender;
    
    private int currentX;
    private int currentZ;
    private final int totalChunks;
    private int loadedChunks = 0;
    
    public ChunkPregenerator(UhcPvpScenarios plugin, World world, int radiusBlocks, CommandSender sender) {
        this.plugin = plugin;
        this.world = world;
        this.radiusChunks = (radiusBlocks / 16) + 2; 
        this.sender = sender;
        this.currentX = -radiusChunks;
        this.currentZ = -radiusChunks;
        this.totalChunks = (2 * radiusChunks + 1) * (2 * radiusChunks + 1);
        sender.sendMessage(ChatColor.GREEN + "Iniciando precarga progresiva de chunks para un radio de " + radiusBlocks + " bloques (" + totalChunks + " chunks)...");
    }
    
    @Override
    public void run() {
        int batchSize = 100;
        for (int i = 0; i < batchSize; i++) {
            if (currentX > radiusChunks) {
                sender.sendMessage(ChatColor.GOLD + "[Pregen] ¡Precarga COMPLETA! " + loadedChunks + "/" + totalChunks + " chunks cargados.");
                Bukkit.broadcastMessage(ChatColor.GREEN + "¡Precarga del mapa completada al 100%!");
                plugin.setPregeneratorTask(null);
                cancel();
                return;
            }
            
            world.getChunkAt(currentX, currentZ);
            loadedChunks++;
            
            currentZ++;
            if (currentZ > radiusChunks) {
                currentZ = -radiusChunks;
                currentX++;
            }
        }
        
        double pct = ((double) loadedChunks / totalChunks) * 100.0;
        sender.sendMessage(String.format(ChatColor.YELLOW + "[Pregen] Progreso: %.1f%% (%d/%d chunks)", pct, loadedChunks, totalChunks));
    }
}
