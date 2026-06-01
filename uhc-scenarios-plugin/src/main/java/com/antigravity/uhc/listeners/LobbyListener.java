package com.antigravity.uhc.listeners;

import com.antigravity.uhc.UhcPvpScenarios;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LobbyListener implements Listener {
    private final UhcPvpScenarios plugin;

    // Public static maps for cosmetic choices, accessible by other classes
    public static final java.util.Set<UUID> hidePlayers = ConcurrentHashMap.newKeySet();
    public static final java.util.Map<UUID, String> killSounds = new ConcurrentHashMap<>();
    public static final java.util.Map<UUID, String> particleEffects = new ConcurrentHashMap<>();

    public LobbyListener(final UhcPvpScenarios plugin) {
        this.plugin = plugin;

        // Repeating task to display particles for players in Lobby/Practice
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : particleEffects.keySet()) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline() && player.getWorld().getName().equals("lobby")) {
                        String effectType = particleEffects.get(uuid);
                        if (effectType.equals("heart")) {
                            player.getWorld().playEffect(player.getLocation().add(0, 2.0, 0), org.bukkit.Effect.HEART, 0);
                        } else if (effectType.equals("flame")) {
                            player.getWorld().playEffect(player.getLocation().add(0, 0.2, 0), org.bukkit.Effect.FLAME, 0);
                        } else if (effectType.equals("happy")) {
                            player.getWorld().playEffect(player.getLocation().add(0, 1.0, 0), org.bukkit.Effect.HAPPY_VILLAGER, 0);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 10L, 10L); // Every 0.5s
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        // Only trigger lobby items if they are not in UHC game
        if (plugin.isUhcActive() && plugin.getParticipants().contains(player.getName().toLowerCase())) {
            return;
        }
        // Also don't trigger if they are in a practice match
        if (plugin.getPracticeManager().getPracticePlayers().contains(player.getUniqueId())) {
            // But let them use their sword/potions, just ignore lobby items
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        String displayName = item.getItemMeta().getDisplayName();

        if (displayName.contains("Practice FFA")) {
            event.setCancelled(true);
            plugin.getPracticeManager().joinPractice(player);
        } else if (displayName.contains("Duelos 1v1")) {
            event.setCancelled(true);
            openDuelingMenu(player);
        } else if (displayName.contains("Ver Partida UHC")) {
            event.setCancelled(true);
            spectateActiveUhc(player);
        } else if (displayName.contains("Ajustes y Cosméticos")) {
            event.setCancelled(true);
            openCosmeticsMenu(player);
        } else if (displayName.contains("Selector de Lobby")) {
            event.setCancelled(true);
            openLobbySelector(player);
        } else if (displayName.contains("Panel de Administrador")) {
            event.setCancelled(true);
            if (player.isOp()) {
                openAdminPanel(player);
            } else {
                player.sendMessage(ChatColor.RED + "No tienes permisos para usar este panel.");
            }
        }
    }

    private void openLobbySelector(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.YELLOW + "Selector de Lobby");
        
        // Background panes
        ItemStack bg = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta bgMeta = bg.getItemMeta();
        bgMeta.setDisplayName(" ");
        bg.setItemMeta(bgMeta);
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, bg);
        }
        
        // Slot 2: Planta A (Quartz)
        ItemStack quartzItem = new ItemStack(Material.QUARTZ_BLOCK);
        ItemMeta qMeta = quartzItem.getItemMeta();
        qMeta.setDisplayName(ChatColor.AQUA + ChatColor.BOLD.toString() + "Planta A " + ChatColor.GRAY + "(Cúpula de Cuarzo)");
        List<String> qLore = new ArrayList<>();
        qLore.add(ChatColor.YELLOW + "Click para teletransportarte.");
        qMeta.setLore(qLore);
        quartzItem.setItemMeta(qMeta);
        inv.setItem(2, quartzItem);
        
        // Slot 6: Planta B (Emerald/Obsidian)
        ItemStack emeraldItem = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta eMeta = emeraldItem.getItemMeta();
        eMeta.setDisplayName(ChatColor.GREEN + ChatColor.BOLD.toString() + "Planta B " + ChatColor.GRAY + "(Cúpula de Esmeralda)");
        List<String> eLore = new ArrayList<>();
        eLore.add(ChatColor.YELLOW + "Click para teletransportarte.");
        eMeta.setLore(eLore);
        emeraldItem.setItemMeta(eMeta);
        inv.setItem(6, emeraldItem);
        
        player.openInventory(inv);
    }

    private void openDuelingMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.LIGHT_PURPLE + "Menú de Duelos");
        
        // Background panes
        ItemStack bg = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta bgMeta = bg.getItemMeta();
        bgMeta.setDisplayName(" ");
        bg.setItemMeta(bgMeta);
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, bg);
        }
        
        // Slot 2: Queue 1v1
        ItemStack queueItem = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta qMeta = queueItem.getItemMeta();
        qMeta.setDisplayName(ChatColor.AQUA + ChatColor.BOLD.toString() + "Buscar Partida 1v1");
        List<String> qLore = new ArrayList<>();
        boolean inQueue = plugin.getPracticeManager().getDuelQueue().contains(player.getUniqueId());
        qLore.add(ChatColor.YELLOW + "Estado: " + (inQueue ? ChatColor.GREEN + "BUSCANDO..." : ChatColor.GRAY + "Ninguno"));
        qLore.add(ChatColor.GRAY + "Entra a la cola automática de matchmaking");
        qLore.add(ChatColor.GRAY + "para emparejarte con un oponente aleatorio.");
        qMeta.setLore(qLore);
        queueItem.setItemMeta(qMeta);
        inv.setItem(2, queueItem);
        
        // Slot 6: Challenge direct
        ItemStack directItem = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        ItemMeta dMeta = directItem.getItemMeta();
        dMeta.setDisplayName(ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "Retar a un Jugador");
        List<String> dLore = new ArrayList<>();
        dLore.add(ChatColor.GRAY + "Selecciona un jugador activo del lobby");
        dLore.add(ChatColor.GRAY + "para enviarle una solicitud de duelo directo.");
        dMeta.setLore(dLore);
        directItem.setItemMeta(dMeta);
        inv.setItem(6, directItem);
        
        player.openInventory(inv);
    }

    private void spectateActiveUhc(Player player) {
        if (!plugin.isUhcActive()) {
            player.sendMessage(ChatColor.RED + "El UHC no está activo en este momento.");
            return;
        }

        player.setGameMode(GameMode.SPECTATOR);
        player.getInventory().clear();
        player.getInventory().addItem(new ItemStack(Material.COMPASS));
        
        // Find a suitable target or world spawn
        Player target = null;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SURVIVAL && !p.getName().equalsIgnoreCase(player.getName())) {
                target = p;
                break;
            }
        }

        if (target != null) {
            player.teleport(target.getLocation());
            player.sendMessage(ChatColor.GREEN + "Teletransportado al jugador activo " + target.getName() + " como espectador.");
        } else {
            player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            player.sendMessage(ChatColor.GREEN + "Teletransportado al spawn del mundo UHC como espectador.");
        }
    }

    private void openDuelSelector(Player player) {
        List<Player> lobbyPlayers = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            // Find players in lobby who are not in practice or UHC game
            if (!plugin.getPracticeManager().getPracticePlayers().contains(p.getUniqueId()) &&
                (!plugin.isUhcActive() || !plugin.getParticipants().contains(p.getName().toLowerCase())) &&
                !p.getUniqueId().equals(player.getUniqueId())) {
                lobbyPlayers.add(p);
            }
        }

        int size = 9;
        while (size < lobbyPlayers.size() && size < 54) {
            size += 9;
        }

        Inventory inv = Bukkit.createInventory(null, size, ChatColor.DARK_GRAY + "Retar a un Duelo");

        for (int i = 0; i < lobbyPlayers.size() && i < size; i++) {
            Player target = lobbyPlayers.get(i);
            ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwner(target.getName());
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + target.getName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "Click para retar a un Duelo 1v1.");
            meta.setLore(lore);
            head.setItemMeta(meta);
            inv.setItem(i, head);
        }

        player.openInventory(inv);
    }

    private void openCosmeticsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.GOLD + "Ajustes y Cosméticos");

        // Background
        ItemStack bg = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta bgMeta = bg.getItemMeta();
        bgMeta.setDisplayName(" ");
        bg.setItemMeta(bgMeta);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, bg);
        }

        // Slot 11: Player Visibility
        boolean hidden = hidePlayers.contains(player.getUniqueId());
        ItemStack visItem = new ItemStack(hidden ? Material.MAGMA_CREAM : Material.SLIME_BALL);
        ItemMeta visMeta = visItem.getItemMeta();
        visMeta.setDisplayName(ChatColor.AQUA + ChatColor.BOLD.toString() + "Visibilidad de Jugadores");
        List<String> visLore = new ArrayList<>();
        visLore.add(ChatColor.YELLOW + "Estado: " + (hidden ? ChatColor.RED + "OCULTOS" : ChatColor.GREEN + "VISIBLES"));
        visLore.add(ChatColor.GRAY + "Click para alternar la visibilidad de");
        visLore.add(ChatColor.GRAY + "los demás jugadores en el lobby.");
        visMeta.setLore(visLore);
        visItem.setItemMeta(visMeta);
        inv.setItem(11, visItem);

        // Slot 13: Particle Effects
        String part = particleEffects.getOrDefault(player.getUniqueId(), "Ninguno");
        ItemStack partItem = new ItemStack(Material.QUARTZ);
        ItemMeta partMeta = partItem.getItemMeta();
        partMeta.setDisplayName(ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "Efecto de Partículas");
        List<String> partLore = new ArrayList<>();
        partLore.add(ChatColor.YELLOW + "Efecto: " + ChatColor.GREEN + part.toUpperCase());
        partLore.add(ChatColor.GRAY + "Click para elegir tu efecto visual.");
        partMeta.setLore(partLore);
        partItem.setItemMeta(partMeta);
        inv.setItem(13, partItem);

        // Slot 15: Kill Sound Effects
        String snd = killSounds.getOrDefault(player.getUniqueId(), "Ninguno");
        ItemStack sndItem = new ItemStack(Material.NOTE_BLOCK);
        ItemMeta sndMeta = sndItem.getItemMeta();
        sndMeta.setDisplayName(ChatColor.YELLOW + ChatColor.BOLD.toString() + "Sonido al Matar");
        List<String> sndLore = new ArrayList<>();
        sndLore.add(ChatColor.YELLOW + "Sonido: " + ChatColor.GREEN + snd.toUpperCase());
        sndLore.add(ChatColor.GRAY + "Click para elegir tu efecto de");
        sndLore.add(ChatColor.GRAY + "sonido cuando eliminas a alguien.");
        sndMeta.setLore(sndLore);
        sndItem.setItemMeta(sndMeta);
        inv.setItem(15, sndItem);

        player.openInventory(inv);
    }

    private void openParticleMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.LIGHT_PURPLE + "Selección de Partículas");

        // Hearts
        ItemStack hearts = new ItemStack(Material.RED_ROSE);
        ItemMeta hMeta = hearts.getItemMeta();
        hMeta.setDisplayName(ChatColor.RED + "Corazones Amorosos");
        hearts.setItemMeta(hMeta);
        inv.setItem(1, hearts);

        // Flames
        ItemStack flames = new ItemStack(Material.FIREWORK);
        ItemMeta fMeta = flames.getItemMeta();
        fMeta.setDisplayName(ChatColor.GOLD + "Llamas de Combate");
        flames.setItemMeta(fMeta);
        inv.setItem(3, flames);

        // Happy villager
        ItemStack happy = new ItemStack(Material.EMERALD);
        ItemMeta haMeta = happy.getItemMeta();
        haMeta.setDisplayName(ChatColor.GREEN + "Destellos Esmeralda");
        happy.setItemMeta(haMeta);
        inv.setItem(5, happy);

        // Reset
        ItemStack reset = new ItemStack(Material.BARRIER);
        ItemMeta rMeta = reset.getItemMeta();
        rMeta.setDisplayName(ChatColor.GRAY + "Ninguno (Desactivar)");
        reset.setItemMeta(rMeta);
        inv.setItem(7, reset);

        player.openInventory(inv);
    }

    private void openSoundMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.YELLOW + "Selección de Sonidos");

        // Thunder
        ItemStack thunder = new ItemStack(Material.FEATHER);
        ItemMeta tMeta = thunder.getItemMeta();
        tMeta.setDisplayName(ChatColor.AQUA + "Tormenta Eléctrica");
        thunder.setItemMeta(tMeta);
        inv.setItem(1, thunder);

        // Wolf
        ItemStack wolf = new ItemStack(Material.BONE);
        ItemMeta wMeta = wolf.getItemMeta();
        wMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Aullido de Lobo salvaje");
        wolf.setItemMeta(wMeta);
        inv.setItem(3, wolf);

        // Villager
        ItemStack villager = new ItemStack(Material.EMERALD);
        ItemMeta vMeta = villager.getItemMeta();
        vMeta.setDisplayName(ChatColor.GREEN + "Aldeano Satisfecho");
        villager.setItemMeta(vMeta);
        inv.setItem(5, villager);

        // Reset
        ItemStack reset = new ItemStack(Material.BARRIER);
        ItemMeta rMeta = reset.getItemMeta();
        rMeta.setDisplayName(ChatColor.GRAY + "Ninguno (Desactivar)");
        reset.setItemMeta(rMeta);
        inv.setItem(7, reset);

        player.openInventory(inv);
    }

    private void openAdminPanel(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.RED + "Panel de Administrador");

        // Background panes
        ItemStack bg = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15); // Black
        ItemMeta bgMeta = bg.getItemMeta();
        bgMeta.setDisplayName(" ");
        bg.setItemMeta(bgMeta);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, bg);
        }

        // Slot 10: UHC Activo
        ItemStack activeItem = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta activeMeta = activeItem.getItemMeta();
        activeMeta.setDisplayName(ChatColor.GOLD + "UHC Activo");
        List<String> activeLore = new ArrayList<>();
        activeLore.add(ChatColor.YELLOW + "Estado: " + (plugin.isUhcActive() ? ChatColor.GREEN + "ENCENDIDO" : ChatColor.RED + "APAGADO"));
        activeLore.add(ChatColor.GRAY + "Click para alternar el estado.");
        activeMeta.setLore(activeLore);
        activeItem.setItemMeta(activeMeta);
        inv.setItem(10, activeItem);

        // Slot 11: CutClean
        ItemStack ccItem = new ItemStack(Material.IRON_ORE);
        ItemMeta ccMeta = ccItem.getItemMeta();
        ccMeta.setDisplayName(ChatColor.GOLD + "Escenario: CutClean");
        List<String> ccLore = new ArrayList<>();
        ccLore.add(ChatColor.YELLOW + "Estado: " + (plugin.isCutcleanEnabled() ? ChatColor.GREEN + "ENCENDIDO" : ChatColor.RED + "APAGADO"));
        ccLore.add(ChatColor.GRAY + "Click para alternar.");
        ccMeta.setLore(ccLore);
        ccItem.setItemMeta(ccMeta);
        inv.setItem(11, ccItem);

        // Slot 12: NoClean
        ItemStack ncItem = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta ncMeta = ncItem.getItemMeta();
        ncMeta.setDisplayName(ChatColor.GOLD + "Escenario: NoClean");
        List<String> ncLore = new ArrayList<>();
        ncLore.add(ChatColor.YELLOW + "Estado: " + (plugin.isNocleanEnabled() ? ChatColor.GREEN + "ENCENDIDO" : ChatColor.RED + "APAGADO"));
        ncLore.add(ChatColor.GRAY + "Click para alternar.");
        ncMeta.setLore(ncLore);
        ncItem.setItemMeta(ncMeta);
        inv.setItem(12, ncItem);

        // Slot 13: TimeBomb
        ItemStack tbItem = new ItemStack(Material.TNT);
        ItemMeta tbMeta = tbItem.getItemMeta();
        tbMeta.setDisplayName(ChatColor.GOLD + "Escenario: TimeBomb");
        List<String> tbLore = new ArrayList<>();
        tbLore.add(ChatColor.YELLOW + "Estado: " + (plugin.isTimebombEnabled() ? ChatColor.GREEN + "ENCENDIDO" : ChatColor.RED + "APAGADO"));
        tbLore.add(ChatColor.GRAY + "Click para alternar.");
        tbMeta.setLore(tbLore);
        tbItem.setItemMeta(tbMeta);
        inv.setItem(13, tbItem);

        // Slot 14: Vanilla Plus
        ItemStack vpItem = new ItemStack(Material.APPLE);
        ItemMeta vpMeta = vpItem.getItemMeta();
        vpMeta.setDisplayName(ChatColor.GOLD + "Escenario: Vanilla Plus");
        List<String> vpLore = new ArrayList<>();
        vpLore.add(ChatColor.YELLOW + "Estado: " + (plugin.isVanillaPlusEnabled() ? ChatColor.GREEN + "ENCENDIDO" : ChatColor.RED + "APAGADO"));
        vpLore.add(ChatColor.GRAY + "Click para alternar.");
        vpMeta.setLore(vpLore);
        vpItem.setItemMeta(vpMeta);
        inv.setItem(14, vpItem);

        // Slot 15: Enchant 1.7
        ItemStack e17Item = new ItemStack(Material.ENCHANTMENT_TABLE);
        ItemMeta e17Meta = e17Item.getItemMeta();
        e17Meta.setDisplayName(ChatColor.GOLD + "Mesas 1.7.10");
        List<String> e17Lore = new ArrayList<>();
        e17Lore.add(ChatColor.YELLOW + "Estado: " + (plugin.isEnchant17Enabled() ? ChatColor.GREEN + "ENCENDIDO" : ChatColor.RED + "APAGADO"));
        e17Lore.add(ChatColor.GRAY + "Click para alternar.");
        e17Meta.setLore(e17Lore);
        e17Item.setItemMeta(e17Meta);
        inv.setItem(15, e17Item);

        // Slot 16: Practice Activo
        ItemStack pracItem = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta pracMeta = pracItem.getItemMeta();
        pracMeta.setDisplayName(ChatColor.GOLD + "Practice Mode");
        List<String> pracLore = new ArrayList<>();
        pracLore.add(ChatColor.YELLOW + "Estado: " + (plugin.getPracticeManager().isPracticeActive() ? ChatColor.GREEN + "HABILITADO" : ChatColor.RED + "DESHABILITADO"));
        pracLore.add(ChatColor.GRAY + "Click para alternar.");
        pracMeta.setLore(pracLore);
        pracItem.setItemMeta(pracMeta);
        inv.setItem(16, pracItem);

        // Slot 22: Teletransportar todos al Lobby
        ItemStack lobbyTpItem = new ItemStack(Material.SLIME_BALL);
        ItemMeta lobbyTpMeta = lobbyTpItem.getItemMeta();
        lobbyTpMeta.setDisplayName(ChatColor.AQUA + "Enviar todos al Lobby Spawn");
        List<String> lobbyTpLore = new ArrayList<>();
        lobbyTpLore.add(ChatColor.GRAY + "Teletransporta y resetea a todos");
        lobbyTpLore.add(ChatColor.GRAY + "los jugadores al Spawn del Lobby.");
        lobbyTpMeta.setLore(lobbyTpLore);
        lobbyTpItem.setItemMeta(lobbyTpMeta);
        inv.setItem(22, lobbyTpItem);

        // Slot 23: Pregenerar Chunks
        ItemStack pregenItem = new ItemStack(Material.BEACON);
        ItemMeta pregenMeta = pregenItem.getItemMeta();
        pregenMeta.setDisplayName(ChatColor.GREEN + "Pre-generar Chunks UHC");
        List<String> pregenLore = new ArrayList<>();
        pregenLore.add(ChatColor.GRAY + "Inicia la pre-generación de un");
        pregenLore.add(ChatColor.GRAY + "radio de 2000 bloques alrededor del");
        pregenLore.add(ChatColor.GRAY + "centro para prevenir lag del mapa.");
        pregenMeta.setLore(pregenLore);
        pregenItem.setItemMeta(pregenMeta);
        inv.setItem(23, pregenItem);

        // Slot 24: Regenerar Mundo
        ItemStack regenItem = new ItemStack(Material.GRASS);
        ItemMeta regenMeta = regenItem.getItemMeta();
        regenMeta.setDisplayName(ChatColor.RED + "Regenerar Mundo UHC");
        List<String> regenLore = new ArrayList<>();
        regenLore.add(ChatColor.GRAY + "Solicita a la API Node.js la");
        regenLore.add(ChatColor.GRAY + "creación de un nuevo mapa aleatorio.");
        regenLore.add(ChatColor.GRAY + "¡El servidor se reiniciará al instante!");
        regenMeta.setLore(regenLore);
        regenItem.setItemMeta(regenMeta);
        inv.setItem(24, regenItem);

        // Slot 25: Alternar Lobby
        int currentLobby = plugin.getLobbyManager().getActiveLobby();
        ItemStack lobbyItem = new ItemStack(Material.PORTAL);
        ItemMeta lobbyMeta = lobbyItem.getItemMeta();
        lobbyMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Alternar Lobby Spawn");
        List<String> lobbyLore = new ArrayList<>();
        lobbyLore.add(ChatColor.YELLOW + "Lobby Activo: " + ChatColor.GOLD + (currentLobby == 1 ? "Planta A (Cuarzo)" : "Planta B (Esmeralda)"));
        lobbyLore.add(ChatColor.GRAY + "Click para alternar y teletransportar");
        lobbyLore.add(ChatColor.GRAY + "a todos los usuarios del lobby.");
        lobbyMeta.setLore(lobbyLore);
        lobbyItem.setItemMeta(lobbyMeta);
        inv.setItem(25, lobbyItem);

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() == null) return;
        String title = event.getInventory().getName();
        Player player = (Player) event.getWhoClicked();

        if (title.equals(ChatColor.YELLOW + "Selector de Lobby")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == 2) {
                player.closeInventory();
                // Send player to lobby 1
                player.teleport(new org.bukkit.Location(Bukkit.getWorld("lobby"), 0.5, 101.5, 0.5, 0f, 0f));
                player.sendMessage(ChatColor.GREEN + "¡Te has teletransportado a la Planta A (Cúpula de Cuarzo)!");
            } else if (slot == 6) {
                player.closeInventory();
                // Send player to lobby 2
                player.teleport(new org.bukkit.Location(Bukkit.getWorld("lobby"), 1000.5, 101.5, 1000.5, 0f, 0f));
                player.sendMessage(ChatColor.GREEN + "¡Te has teletransportado a la Planta B (Cúpula de Esmeralda)!");
            }
            return;
        }

        if (title.equals(ChatColor.LIGHT_PURPLE + "Menú de Duelos")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == 2) {
                player.closeInventory();
                plugin.getPracticeManager().toggleQueue(player);
            } else if (slot == 6) {
                openDuelSelector(player);
            }
            return;
        }

        if (title.equals(ChatColor.DARK_GRAY + "Retar a un Duelo")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() != Material.SKULL_ITEM) return;

            SkullMeta meta = (SkullMeta) clicked.getItemMeta();
            if (meta == null || meta.getOwner() == null) return;

            Player target = Bukkit.getPlayer(meta.getOwner());
            if (target == null || !target.isOnline()) {
                player.sendMessage(ChatColor.RED + "El oponente no se encuentra en línea.");
                player.closeInventory();
                return;
            }

            player.closeInventory();
            plugin.getPracticeManager().startDuel(player, target);
            return;
        }

        if (title.equals(ChatColor.GOLD + "Ajustes y Cosméticos")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == 11) { // Visibility Toggle
                if (hidePlayers.contains(player.getUniqueId())) {
                    hidePlayers.remove(player.getUniqueId());
                    for (Player other : Bukkit.getOnlinePlayers()) {
                        player.showPlayer(other);
                    }
                    player.sendMessage(ChatColor.GREEN + "¡Ahora puedes ver a otros jugadores en el Lobby!");
                } else {
                    hidePlayers.add(player.getUniqueId());
                    for (Player other : Bukkit.getOnlinePlayers()) {
                        player.hidePlayer(other);
                    }
                    player.sendMessage(ChatColor.GREEN + "¡Has ocultado a todos los jugadores en el Lobby!");
                }
                player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                openCosmeticsMenu(player);
            } else if (slot == 13) { // Particle Selector
                player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                openParticleMenu(player);
            } else if (slot == 15) { // Sound Selector
                player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                openSoundMenu(player);
            }
            return;
        }

        if (title.equals(ChatColor.LIGHT_PURPLE + "Selección de Partículas")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null) return;

            if (clicked.getType() == Material.RED_ROSE) {
                particleEffects.put(player.getUniqueId(), "heart");
                player.sendMessage(ChatColor.GREEN + "Partículas de corazones seleccionadas.");
            } else if (clicked.getType() == Material.FIREWORK) {
                particleEffects.put(player.getUniqueId(), "flame");
                player.sendMessage(ChatColor.GREEN + "Partículas de llamas de combate seleccionadas.");
            } else if (clicked.getType() == Material.EMERALD) {
                particleEffects.put(player.getUniqueId(), "happy");
                player.sendMessage(ChatColor.GREEN + "Partículas de destellos esmeralda seleccionadas.");
            } else if (clicked.getType() == Material.BARRIER) {
                particleEffects.remove(player.getUniqueId());
                player.sendMessage(ChatColor.GRAY + "Efectos de partículas desactivados.");
            }

            player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1.0f, 1.0f);
            player.closeInventory();
            return;
        }

        if (title.equals(ChatColor.YELLOW + "Selección de Sonidos")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null) return;

            if (clicked.getType() == Material.FEATHER) {
                killSounds.put(player.getUniqueId(), "thunder");
                player.sendMessage(ChatColor.GREEN + "Efecto de sonido de Tormenta Eléctrica (Trueno) activado.");
            } else if (clicked.getType() == Material.BONE) {
                killSounds.put(player.getUniqueId(), "wolf");
                player.sendMessage(ChatColor.GREEN + "Efecto de sonido de Aullido de Lobo activado.");
            } else if (clicked.getType() == Material.EMERALD) {
                killSounds.put(player.getUniqueId(), "villager");
                player.sendMessage(ChatColor.GREEN + "Efecto de sonido de Aldeano Satisfecho activado.");
            } else if (clicked.getType() == Material.BARRIER) {
                killSounds.remove(player.getUniqueId());
                player.sendMessage(ChatColor.GRAY + "Efecto de sonido al matar desactivado.");
            }

            player.playSound(player.getLocation(), Sound.ORB_PICKUP, 1.0f, 1.0f);
            player.closeInventory();
            return;
        }

        if (title.equals(ChatColor.RED + "Panel de Administrador")) {
            event.setCancelled(true);
            if (!player.isOp()) {
                player.closeInventory();
                return;
            }

            int slot = event.getRawSlot();
            boolean update = false;

            if (slot == 10) { // UHC Activo
                boolean current = plugin.isUhcActive();
                plugin.setUhcActive(!current);
                if (current) {
                    plugin.getParticipants().clear();
                    plugin.setGameState("Espera");
                    plugin.setElapsedSeconds(0);
                    Bukkit.broadcastMessage(ChatColor.GOLD + "[Control] " + ChatColor.RED + "El torneo UHC ha sido desactivado por un administrador.");
                } else {
                    plugin.setGameState("Activo");
                    Bukkit.broadcastMessage(ChatColor.GOLD + "[Control] " + ChatColor.GREEN + "El torneo UHC ha sido activado por un administrador.");
                }
                update = true;
            } else if (slot == 11) { // CutClean
                boolean val = !plugin.isCutcleanEnabled();
                plugin.setCutcleanEnabled(val);
                Bukkit.broadcastMessage(ChatColor.GOLD + "[Control] Escenario CutClean " + (val ? ChatColor.GREEN + "ACTIVADO" : ChatColor.RED + "DESACTIVADO"));
                update = true;
            } else if (slot == 12) { // NoClean
                boolean val = !plugin.isNocleanEnabled();
                plugin.setNocleanEnabled(val);
                if (!val) plugin.getNoCleanPlayers().clear();
                Bukkit.broadcastMessage(ChatColor.GOLD + "[Control] Escenario NoClean " + (val ? ChatColor.GREEN + "ACTIVADO" : ChatColor.RED + "DESACTIVADO"));
                update = true;
            } else if (slot == 13) { // TimeBomb
                boolean val = !plugin.isTimebombEnabled();
                plugin.setTimebombEnabled(val);
                Bukkit.broadcastMessage(ChatColor.GOLD + "[Control] Escenario TimeBomb " + (val ? ChatColor.GREEN + "ACTIVADO" : ChatColor.RED + "DESACTIVADO"));
                update = true;
            } else if (slot == 14) { // Vanilla Plus
                boolean val = !plugin.isVanillaPlusEnabled();
                plugin.setVanillaPlusEnabled(val);
                Bukkit.broadcastMessage(ChatColor.GOLD + "[Control] Escenario Vanilla Plus " + (val ? ChatColor.GREEN + "ACTIVADO" : ChatColor.RED + "DESACTIVADO"));
                update = true;
            } else if (slot == 15) { // Enchant 1.7
                boolean val = !plugin.isEnchant17Enabled();
                plugin.setEnchant17Enabled(val);
                Bukkit.broadcastMessage(ChatColor.GOLD + "[Control] Mecánica Mesas 1.7.10 " + (val ? ChatColor.GREEN + "ACTIVADO" : ChatColor.RED + "DESACTIVADO"));
                update = true;
            } else if (slot == 16) { // Practice Active
                boolean val = !plugin.getPracticeManager().isPracticeActive();
                plugin.getPracticeManager().setPracticeActive(val);
                update = true;
            } else if (slot == 22) { // Teleport all to lobby
                for (Player p : Bukkit.getOnlinePlayers()) {
                    plugin.getLobbyManager().teleToLobby(p);
                }
                player.sendMessage(ChatColor.GREEN + "Has teletransportado a todos los jugadores al Lobby.");
                player.closeInventory();
            } else if (slot == 23) { // Pregenerar Chunks
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "uhcscenario pregen 2000");
                player.sendMessage(ChatColor.GREEN + "[Control] Iniciando pre-generación de 2000 bloques de radio en el mundo UHC para evitar lag.");
                player.closeInventory();
            } else if (slot == 24) { // Regenerar Mundo
                player.sendMessage(ChatColor.RED + "[Control] Solicitando regeneración de mundo UHC a la API...");
                player.closeInventory();
                
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            java.net.URL url = new java.net.URL("http://api-controller:3000/api/world/regenerate");
                            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                            conn.setRequestMethod("POST");
                            conn.setRequestProperty("Content-Type", "application/json");
                            conn.setDoOutput(true);
                            java.io.OutputStream os = conn.getOutputStream();
                            os.write("{}".getBytes());
                            os.flush();
                            os.close();
                            
                            int code = conn.getResponseCode();
                            if (code == 200) {
                                Bukkit.broadcastMessage(ChatColor.RED + "[Control] ¡Mundo UHC regenerado exitosamente! El servidor de Minecraft se reiniciará en 5 segundos...");
                            } else {
                                player.sendMessage(ChatColor.RED + "[API] Error de la API al regenerar: Código " + code);
                            }
                        } catch (Exception e) {
                            player.sendMessage(ChatColor.RED + "[API] Error al conectar con el controlador Node.js: " + e.getMessage());
                        }
                    }
                }).start();
            } else if (slot == 25) { // Alternar Lobby
                int lobbyId = plugin.getLobbyManager().getActiveLobby() == 1 ? 2 : 1;
                plugin.getLobbyManager().setActiveLobby(lobbyId);
                
                String lobbyName = lobbyId == 1 ? "Planta A (Cúpula de Cuarzo)" : "Planta B (Cúpula de Esmeralda)";
                Bukkit.broadcastMessage(ChatColor.GOLD + "[Control] ¡El spawn del Lobby ha sido cambiado a la " + ChatColor.LIGHT_PURPLE + lobbyName + ChatColor.GOLD + " por un administrador!");
                
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getWorld().getName().equals("lobby")) {
                        plugin.getLobbyManager().teleToLobby(p);
                    }
                }
                update = true;
            }

            if (update) {
                openAdminPanel(player);
            }
        }
    }

    // Safety checks for lobby protection
    private boolean isPlayerInLobbySpawn(Player player) {
        if (player.getWorld().getName().equals("lobby")) {
            return !plugin.getPracticeManager().getPracticePlayers().contains(player.getUniqueId());
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isPlayerInLobbySpawn(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isPlayerInLobbySpawn(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        if (isPlayerInLobbySpawn(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player p = (Player) event.getEntity();
            if (isPlayerInLobbySpawn(p)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Hide players for the joining player if they have hidePlayers enabled
        if (hidePlayers.contains(player.getUniqueId())) {
            for (Player other : Bukkit.getOnlinePlayers()) {
                player.hidePlayer(other);
            }
        }
        
        // Hide the joining player for anyone else who has hidePlayers enabled
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (hidePlayers.contains(other.getUniqueId())) {
                other.hidePlayer(player);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        hidePlayers.remove(player.getUniqueId());
        killSounds.remove(player.getUniqueId());
        particleEffects.remove(player.getUniqueId());
    }
}
