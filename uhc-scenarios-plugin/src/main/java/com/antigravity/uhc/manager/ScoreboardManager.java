package com.antigravity.uhc.manager;

import com.antigravity.uhc.UhcPvpScenarios;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ScoreboardManager {
    private final UhcPvpScenarios plugin;

    public ScoreboardManager(UhcPvpScenarios plugin) {
        this.plugin = plugin;
    }

    public void updateAllScoreboards() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                updatePlayerScoreboard(p);
            } catch (Exception e) {
                // Prevent crashes from scoreboard conflicts
            }
        }
    }

    public void updatePlayerScoreboard(Player p) {
        org.bukkit.scoreboard.ScoreboardManager sm = Bukkit.getScoreboardManager();
        Scoreboard sb = p.getScoreboard();
        if (sb == sm.getMainScoreboard()) {
            sb = sm.getNewScoreboard();
            p.setScoreboard(sb);
        }
        
        Objective obj = sb.getObjective("uhcsb");
        if (obj == null) {
            obj = sb.registerNewObjective("uhcsb", "dummy");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.GRAY + "----------------------");

        if (plugin.getPracticeManager().getPracticePlayers().contains(p.getUniqueId())) {
            // Practice Scoreboard
            String title = ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "PRACTICE FFA";
            if (!obj.getDisplayName().equals(title)) {
                obj.setDisplayName(title);
            }
            
            lines.add(ChatColor.YELLOW + "Arena: " + ChatColor.WHITE + "FFA Combat");
            lines.add(""); // Empty line
            
            int kills = plugin.getPracticeManager().getKills().getOrDefault(p.getUniqueId(), 0);
            int deaths = plugin.getPracticeManager().getDeaths().getOrDefault(p.getUniqueId(), 0);
            lines.add(ChatColor.YELLOW + "Kills: " + ChatColor.GREEN + kills);
            lines.add(ChatColor.YELLOW + "Deaths: " + ChatColor.RED + deaths);
            lines.add(""); // Empty line
            
            // Check if player is in a duel
            if (plugin.getPracticeManager().getActiveDuels().containsKey(p.getUniqueId())) {
                org.bukkit.entity.Player opp = Bukkit.getPlayer(plugin.getPracticeManager().getActiveDuels().get(p.getUniqueId()));
                String oppName = opp != null ? opp.getName() : "Oponente";
                lines.add(ChatColor.YELLOW + "Duelo: " + ChatColor.LIGHT_PURPLE + oppName);
            } else {
                lines.add(ChatColor.YELLOW + "Duelo: " + ChatColor.GRAY + "Ninguno");
            }
        } else if (plugin.isUhcActive() && plugin.getParticipants().contains(p.getName().toLowerCase())) {
            // Active UHC Participant Scoreboard
            String title = ChatColor.GOLD + ChatColor.BOLD.toString() + plugin.getEventName().toUpperCase();
            if (!obj.getDisplayName().equals(title)) {
                obj.setDisplayName(title);
            }
            
            lines.add(ChatColor.YELLOW + "Fase: " + ChatColor.WHITE + plugin.getGameState());
            
            // Format elapsed seconds as MM:SS or HH:MM:SS
            int elapsedSeconds = plugin.getElapsedSeconds();
            int h = elapsedSeconds / 3600;
            int m = (elapsedSeconds % 3600) / 60;
            int s = elapsedSeconds % 60;
            String timeStr = (h > 0 ? String.format("%02d:%02d:%02d", h, m, s) : String.format("%02d:%02d", m, s));
            lines.add(ChatColor.YELLOW + "Tiempo: " + ChatColor.GREEN + timeStr);
            lines.add(""); // Empty line
            lines.add(ChatColor.YELLOW + "Borde: " + ChatColor.RED + "±" + plugin.getCurrentBorderSize());
            lines.add(ChatColor.YELLOW + "Vivos: " + ChatColor.GREEN + plugin.getAliveCount());
            lines.add(""); // Empty line
            int kills = plugin.getPlayerKills().getOrDefault(p.getUniqueId(), 0);
            lines.add(ChatColor.YELLOW + "Kills: " + ChatColor.RED + kills);
        } else {
            // Lobby / Spectator Scoreboard
            String title = ChatColor.AQUA + ChatColor.BOLD.toString() + "RANKIT NETWORK";
            if (!obj.getDisplayName().equals(title)) {
                obj.setDisplayName(title);
            }
            
            lines.add(ChatColor.YELLOW + "Lobby: " + ChatColor.WHITE + "Spawn Principal");
            lines.add(ChatColor.YELLOW + "Online: " + ChatColor.GREEN + Bukkit.getOnlinePlayers().size());
            lines.add(""); // Empty line
            
            int practiceCount = plugin.getPracticeManager().getPracticePlayers().size();
            lines.add(ChatColor.YELLOW + "Practice FFA: " + ChatColor.LIGHT_PURPLE + practiceCount + " jugando");
            
            String uhcStatus = plugin.isUhcActive() ? ChatColor.GREEN + "En Curso" : ChatColor.GRAY + "Esperando...";
            lines.add(ChatColor.YELLOW + "UHC Evento: " + uhcStatus);
            lines.add(""); // Empty line
            lines.add(ChatColor.YELLOW + "Seed: " + ChatColor.AQUA + readSeedFromPropertiesJava());
        }
        
        lines.add(ChatColor.GRAY + "---------------------");
        lines.add(ChatColor.AQUA + plugin.getServerIp());
        
        // Reset old scores to achieve flicker-free updates in a single tick!
        for (String entry : new ArrayList<>(sb.getEntries())) {
            sb.resetScores(entry);
        }
        
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

    private String readSeedFromPropertiesJava() {
        try {
            File propFile = new File(plugin.getDataFolder().getParentFile().getParentFile(), "server.properties");
            if (propFile.exists()) {
                List<String> lines = Files.readAllLines(propFile.toPath(), StandardCharsets.UTF_8);
                for (String line : lines) {
                    if (line.startsWith("level-seed=")) {
                        String seedVal = line.substring("level-seed=".length()).trim();
                        return seedVal.isEmpty() ? "Default" : seedVal;
                    }
                }
            }
        } catch (Exception e) {
            // Fallback
        }
        return "1746271928";
    }

    private String getUniqueEmptyLine(int score) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < score; i++) {
            sb.append("§r");
        }
        return sb.toString();
    }
}
