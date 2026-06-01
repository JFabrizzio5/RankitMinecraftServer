package com.antigravity.uhc.commands;

import com.antigravity.uhc.UhcPvpScenarios;
import com.antigravity.uhc.tasks.ChunkPregenerator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import java.util.Map;
import java.util.UUID;

public class ScenarioCommand implements CommandExecutor {
    private final UhcPvpScenarios plugin;

    public ScenarioCommand(UhcPvpScenarios plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("uhcscenario")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Uso: /uhcscenario <cutclean|noclean|timebomb|vanillaplus|enchant17|active|scoreboard|lobby|pregen|participants|practice> <valores...>");
                return true;
            }
            
            String scenario = args[0].toLowerCase();
            
            if (scenario.equals("lobbyplayers")) {
                int lobbyId = 1;
                try {
                    lobbyId = Integer.parseInt(args[1]);
                } catch (Exception e) {}
                
                java.util.List<String> players = new java.util.ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getWorld().getName().equals("lobby")) {
                        double x = p.getLocation().getX();
                        if (lobbyId == 1 && x >= -100 && x <= 100) {
                            players.add(p.getName());
                        } else if (lobbyId == 2 && x >= 900 && x <= 1100) {
                            players.add(p.getName());
                        }
                    }
                }
                
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < players.size(); i++) {
                    sb.append("\"").append(players.get(i)).append("\"");
                    if (i < players.size() - 1) sb.append(",");
                }
                sb.append("]");
                sender.sendMessage(sb.toString());
                return true;
            }
            
            String valStr = args[1].toLowerCase();
            boolean value = valStr.equals("true") || valStr.equals("on") || valStr.equals("1");
            
            if (scenario.equals("cutclean")) {
                plugin.setCutcleanEnabled(value);
                sender.sendMessage(ChatColor.GREEN + "CutClean configurado en: " + value);
                Bukkit.broadcastMessage(ChatColor.GOLD + "[UHC] Escenario CutClean " + 
                        (value ? ChatColor.GREEN + "ACTIVADO" : ChatColor.RED + "DESACTIVADO"));
                return true;
            } else if (scenario.equals("noclean")) {
                plugin.setNocleanEnabled(value);
                if (!value) {
                    plugin.getNoCleanPlayers().clear();
                }
                sender.sendMessage(ChatColor.GREEN + "NoClean configurado en: " + value);
                Bukkit.broadcastMessage(ChatColor.GOLD + "[UHC] Escenario NoClean " + 
                        (value ? ChatColor.GREEN + "ACTIVADO" : ChatColor.RED + "DESACTIVADO"));
                return true;
            } else if (scenario.equals("timebomb")) {
                plugin.setTimebombEnabled(value);
                sender.sendMessage(ChatColor.GREEN + "TimeBomb configurado en: " + value);
                Bukkit.broadcastMessage(ChatColor.GOLD + "[UHC] Escenario TimeBomb " + 
                        (value ? ChatColor.GREEN + "ACTIVADO" : ChatColor.RED + "DESACTIVADO"));
                return true;
            } else if (scenario.equals("vanillaplus")) {
                plugin.setVanillaPlusEnabled(value);
                sender.sendMessage(ChatColor.GREEN + "Vanilla Plus configurado en: " + value);
                Bukkit.broadcastMessage(ChatColor.GOLD + "[UHC] Escenario Vanilla Plus " + 
                        (value ? ChatColor.GREEN + "ACTIVADO" : ChatColor.RED + "DESACTIVADO"));
                return true;
            } else if (scenario.equals("enchant17")) {
                plugin.setEnchant17Enabled(value);
                sender.sendMessage(ChatColor.GREEN + "Mesas de encantamiento 1.7.10 configuradas en: " + value);
                Bukkit.broadcastMessage(ChatColor.GOLD + "[UHC] Mesas de encantamiento 1.7.10 " + 
                        (value ? ChatColor.GREEN + "ACTIVADAS" : ChatColor.RED + "DESACTIVADAS"));
                return true;
            } else if (scenario.equals("scoreboard")) {
                if (args.length >= 7) {
                    plugin.setServerIp(args[1]);
                    plugin.setEventName(args[2].replace("_", " "));
                    plugin.setGameState(args[3].replace("_", " "));
                    try {
                        plugin.setElapsedSeconds(Integer.parseInt(args[4]));
                        plugin.setCurrentBorderSize(Integer.parseInt(args[5]));
                        plugin.setAliveCount(Integer.parseInt(args[6]));
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Error parseando números.");
                    }
                    plugin.getScoreboardManager().updateAllScoreboards();
                }
                return true;
            } else if (scenario.equals("lobby")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    plugin.getLobbyManager().teleToLobby(p);
                }
                sender.sendMessage(ChatColor.GREEN + "Todos los jugadores teletransportados y reseteados al Lobby.");
                return true;
            } else if (scenario.equals("pregen")) {
                int radius;
                try {
                    radius = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Radio de pregen inválido.");
                    return true;
                }
                if (plugin.getPregeneratorTask() != null) {
                    plugin.getPregeneratorTask().cancel();
                }
                World uhcWorld = Bukkit.getWorlds().get(0);
                ChunkPregenerator task = new ChunkPregenerator(plugin, uhcWorld, radius, sender);
                plugin.setPregeneratorTask(task);
                task.runTaskTimer(plugin, 1L, 10L); // batch load chunks every 10 ticks
                return true;
            } else if (scenario.equals("participants")) {
                String subcmd = args[1].toLowerCase();
                if (subcmd.equals("clear")) {
                    plugin.getParticipants().clear();
                    sender.sendMessage(ChatColor.GREEN + "Lista de participantes limpiada.");
                    return true;
                } else if (subcmd.equals("add") && args.length >= 3) {
                    String pName = args[2];
                    plugin.getParticipants().add(pName.toLowerCase());
                    sender.sendMessage(ChatColor.GREEN + "Participante añadido: " + pName);
                    return true;
                }
                sender.sendMessage(ChatColor.RED + "Subcomando participante desconocido. Usa: clear o add <jugador>");
                return true;
            } else if (scenario.equals("active")) {
                plugin.setUhcActive(value);
                if (!value) {
                    // Reset combat log state
                    for (Villager villager : plugin.getCombatLogVillagers().values()) {
                        if (villager != null && villager.isValid()) {
                            villager.remove();
                        }
                    }
                    plugin.getCombatLogVillagers().clear();
                    plugin.getCombatLogInventories().clear();
                    plugin.getCombatLogArmor().clear();
                    plugin.getCombatLogNames().clear();
                    plugin.getKilledOfflinePlayers().clear();
                    plugin.getParticipants().clear();
                    
                    // Reset scoreboard variables and kills
                    plugin.getPlayerKills().clear();
                    plugin.setElapsedSeconds(0);
                    plugin.setGameState("Espera");
                    
                    if (plugin.getPregeneratorTask() != null) {
                        plugin.getPregeneratorTask().cancel();
                        plugin.setPregeneratorTask(null);
                    }
                }
                sender.sendMessage(ChatColor.GREEN + "UHC activo configurado en: " + value);
                return true;
            } else if (scenario.equals("practice")) {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Uso: /uhcscenario practice <true|false|join|leave|duel|status> <valores...>");
                    return true;
                }
                String sub = args[1].toLowerCase();
                if (sub.equals("true") || sub.equals("on") || sub.equals("1")) {
                    plugin.getPracticeManager().setPracticeActive(true);
                    sender.sendMessage(ChatColor.GREEN + "Practice habilitado.");
                    return true;
                } else if (sub.equals("false") || sub.equals("off") || sub.equals("0")) {
                    plugin.getPracticeManager().setPracticeActive(false);
                    sender.sendMessage(ChatColor.RED + "Practice deshabilitado.");
                    return true;
                } else if (sub.equals("status")) {
                    StringBuilder js = new StringBuilder();
                    js.append("{\"active\":").append(plugin.getPracticeManager().isPracticeActive()).append(",");
                    js.append("\"players\":[");
                    boolean first = true;
                    for (UUID uuid : plugin.getPracticeManager().getPracticePlayers()) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            if (!first) js.append(",");
                            js.append("\"").append(p.getName()).append("\"");
                            first = false;
                        }
                    }
                    js.append("],\"kills\":{");
                    first = true;
                    for (Map.Entry<UUID, Integer> entry : plugin.getPracticeManager().getKills().entrySet()) {
                        Player p = Bukkit.getPlayer(entry.getKey());
                        if (p != null) {
                            if (!first) js.append(",");
                            js.append("\"").append(p.getName()).append("\":").append(entry.getValue());
                            first = false;
                        }
                    }
                    js.append("},\"deaths\":{");
                    first = true;
                    for (Map.Entry<UUID, Integer> entry : plugin.getPracticeManager().getDeaths().entrySet()) {
                        Player p = Bukkit.getPlayer(entry.getKey());
                        if (p != null) {
                            if (!first) js.append(",");
                            js.append("\"").append(p.getName()).append("\":").append(entry.getValue());
                            first = false;
                        }
                    }
                    js.append("}}");
                    sender.sendMessage(js.toString());
                    return true;
                } else if (sub.equals("join")) {
                    if (args.length < 3) {
                        sender.sendMessage(ChatColor.RED + "Uso: /uhcscenario practice join <jugador>");
                        return true;
                    }
                    Player p = Bukkit.getPlayer(args[2]);
                    if (p == null) {
                        sender.sendMessage(ChatColor.RED + "Jugador no encontrado.");
                        return true;
                    }
                    plugin.getPracticeManager().joinPractice(p);
                    sender.sendMessage(ChatColor.GREEN + p.getName() + " se unió a Practice.");
                    return true;
                } else if (sub.equals("leave")) {
                    if (args.length < 3) {
                        sender.sendMessage(ChatColor.RED + "Uso: /uhcscenario practice leave <jugador>");
                        return true;
                    }
                    Player p = Bukkit.getPlayer(args[2]);
                    if (p == null) {
                        sender.sendMessage(ChatColor.RED + "Jugador no encontrado.");
                        return true;
                    }
                    plugin.getPracticeManager().leavePractice(p);
                    sender.sendMessage(ChatColor.GREEN + p.getName() + " salió de Practice.");
                    return true;
                } else if (sub.equals("duel")) {
                    if (args.length < 4) {
                        sender.sendMessage(ChatColor.RED + "Uso: /uhcscenario practice duel <jugador1> <jugador2>");
                        return true;
                    }
                    Player p1 = Bukkit.getPlayer(args[2]);
                    Player p2 = Bukkit.getPlayer(args[3]);
                    if (p1 == null || p2 == null) {
                        sender.sendMessage(ChatColor.RED + "Uno o ambos jugadores no se encuentran online o válidos.");
                        return true;
                    }
                    plugin.getPracticeManager().startDuel(p1, p2);
                    sender.sendMessage(ChatColor.GREEN + "Duelo iniciado entre " + p1.getName() + " y " + p2.getName());
                    return true;
                }
                sender.sendMessage(ChatColor.RED + "Subcomando desconocido de practice: true|false|join|leave|duel|status");
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "Escenario no reconocido. Opciones: cutclean, noclean, timebomb, vanillaplus, enchant17, scoreboard, lobby, pregen, participants, active, practice");
                return true;
            }
        }
        return false;
    }
}
