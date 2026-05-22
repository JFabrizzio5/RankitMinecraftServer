const express = require('express');
const cors = require('cors');
const path = require('path');
const fs = require('fs');
const net = require('net');
const { Rcon } = require('rcon-client');

const app = express();
const PORT = process.env.PORT || 3000;

// Enable CORS and JSON parsing
app.use(cors());
app.use(express.json());

// Server environment variables
const RCON_HOST = process.env.RCON_HOST || 'mc-server';
const RCON_PORT = parseInt(process.env.RCON_PORT || '25575', 10);
const RCON_PASSWORD = process.env.RCON_PASSWORD || 'uhc_secure_rcon_pass_2026';
const STATS_DIR = process.env.STATS_DIR || path.join(__dirname, 'mc-data', 'world', 'stats');
const USERCACHE_PATH = process.env.USERCACHE_PATH || path.join(__dirname, 'mc-data', 'usercache.json');

// --- UHC TOURNAMENT GAME STATE & CONFIGURATION ---
const UHC_CONFIG_FILE = path.join(__dirname, 'uhc-config.json');

let uhcState = {
  active: false,
  state: 'idle', // idle, grace, zone1, zone2, zone3, finished
  startTime: null,
  graceTime: 20, // minutes
  startBorderSize: 2000,
  playersPerBorder: 4, // border density spread
  calcBorderByDensity: false,
  scenarios: {
    cutClean: true,
    fireless: false,
    noClean: false,
    doubleHealth: false
  },
  zone1Border: 1000,
  zone1Time: 10, // minutes of shrink
  zone2Border: 500,
  zone2Time: 10,
  zone3Border: 100,
  zone3Time: 10,
  currentBorderSize: 2000,
  elapsedSeconds: 0,
  timerInterval: null
};

// Load saved config if exists
try {
  if (fs.existsSync(UHC_CONFIG_FILE)) {
    const saved = JSON.parse(fs.readFileSync(UHC_CONFIG_FILE, 'utf8'));
    uhcState = { ...uhcState, ...saved, active: false, state: 'idle', startTime: null, elapsedSeconds: 0, timerInterval: null };
  }
} catch (e) {
  console.error('Failed to load UHC config:', e.message);
}

function saveUhcConfig() {
  try {
    const dataToSave = {
      graceTime: uhcState.graceTime,
      startBorderSize: uhcState.startBorderSize,
      playersPerBorder: uhcState.playersPerBorder,
      calcBorderByDensity: uhcState.calcBorderByDensity,
      scenarios: uhcState.scenarios,
      zone1Border: uhcState.zone1Border,
      zone1Time: uhcState.zone1Time,
      zone2Border: uhcState.zone2Border,
      zone2Time: uhcState.zone2Time,
      zone3Border: uhcState.zone3Border,
      zone3Time: uhcState.zone3Time
    };
    fs.writeFileSync(UHC_CONFIG_FILE, JSON.stringify(dataToSave, null, 2), 'utf8');
  } catch (e) {
    console.error('Failed to save UHC config:', e.message);
  }
}

async function executeRconCommand(command) {
  try {
    if (rconClient.connected) {
      await rconClient.send(command);
    }
  } catch (err) {
    console.error(`[RCON Command Fail] /${command}:`, err.message);
  }
}

async function generateLobbyStructure() {
  console.log('[Lobby Engine] Generating beautiful quartz & glass spawn lobby at 0, 100, 0...');
  try {
    // 1. Set world spawn point
    await executeRconCommand('setworldspawn 0 100 0');
    
    // 2. Clear space first (11x11x5 space filled with air to avoid players getting stuck)
    await executeRconCommand('fill -5 100 -5 5 103 5 air');
    
    // 3. Floor (11x11 of Quartz Blocks)
    await executeRconCommand('fill -5 99 -5 5 99 5 quartz_block');
    
    // 4. Glass Walls (3 blocks high)
    await executeRconCommand('fill -5 100 -5 -5 102 5 glass');
    await executeRconCommand('fill 5 100 -5 5 102 5 glass');
    await executeRconCommand('fill -5 100 -5 5 102 -5 glass');
    await executeRconCommand('fill -5 100 5 5 102 5 glass');
    
    // 5. Quartz Roof (11x11 Quartz Blocks)
    await executeRconCommand('fill -5 103 -5 5 103 5 quartz_block');
    
    // 6. Corner lightings (Glowstone)
    await executeRconCommand('setblock -4 102 -4 glowstone');
    await executeRconCommand('setblock 4 102 -4 glowstone');
    await executeRconCommand('setblock -4 102 4 glowstone');
    await executeRconCommand('setblock 4 102 4 glowstone');
    
    // 7. If tournament is not running, teleport everyone here and set to adventure mode
    if (!uhcState.active) {
      await executeRconCommand('gamemode adventure @a');
      await executeRconCommand('tp @a 0 100 0');
    }
    console.log('[Lobby Engine] Lobby structure generated successfully!');
  } catch (err) {
    console.error('[Lobby Engine] Error generating lobby:', err.message);
  }
}

async function startUhcGame() {
  console.log('[UHC Engine] Starting UHC tournament...');
  uhcState.active = true;
  uhcState.state = 'grace';
  uhcState.startTime = Date.now();
  uhcState.elapsedSeconds = 0;

  // 1. Initial server setup
  await executeRconCommand('worldborder center 0 0');
  
  // Calculate initial border based on density check
  let finalStartBorder = uhcState.startBorderSize;
  if (uhcState.calcBorderByDensity) {
    try {
      const listRes = await rconClient.send('list');
      const listRegex = /There are (\d+)/i;
      const match = listRes.match(listRegex);
      const playerCount = match ? parseInt(match[1], 10) : 0;
      if (playerCount > 0) {
        finalStartBorder = Math.max(200, Math.ceil(playerCount / uhcState.playersPerBorder) * 500);
        console.log(`[UHC Engine] Calculated initial border based on player density (${playerCount} players, ${uhcState.playersPerBorder} per border block): ${finalStartBorder}`);
      }
    } catch (e) {
      console.warn('[UHC Engine] Failed to auto-calculate border density, using default startBorderSize:', e.message);
    }
  }
  uhcState.currentBorderSize = finalStartBorder;
  await executeRconCommand(`worldborder set ${finalStartBorder}`);

  // Game rules
  await executeRconCommand('gamerule naturalRegeneration false');
  await executeRconCommand('gamerule pvp false');
  await executeRconCommand('difficulty hard');
  await executeRconCommand('time set 0');
  await executeRconCommand('weather clear');
  await executeRconCommand('clear @a');
  await executeRconCommand('effect @a clear');

  // Change players to survival mode and spread them!
  await executeRconCommand('gamemode survival @a');
  const maxRange = Math.max(50, Math.floor(finalStartBorder / 2) - 20);
  await executeRconCommand(`spreadplayers 0 0 100 ${maxRange} false @a`);

  // Scenarios setup
  if (uhcState.scenarios.fireless) {
    await executeRconCommand('gamerule doFireTick false');
  } else {
    await executeRconCommand('gamerule doFireTick true');
  }

  if (uhcState.scenarios.doubleHealth) {
    await executeRconCommand('effect @a minecraft:health_boost 99999 4');
    await executeRconCommand('effect @a minecraft:instant_health 5');
  }

  // Grace items & effects
  await executeRconCommand('effect @a minecraft:resistance 15 255');
  await executeRconCommand('effect @a minecraft:saturation 15 255');

  // Titles
  await executeRconCommand('title @a title {"text":"UHC INICIADO","color":"gold"}');
  await executeRconCommand(`title @a subtitle {"text":"Tiempo de gracia: ${uhcState.graceTime} min","color":"yellow"}`);
  await executeRconCommand(`say ¡El torneo UHC ha comenzado! Tiempo de gracia: ${uhcState.graceTime} minutos. PvP desactivado.`);

  // Active timer loop
  if (uhcState.timerInterval) clearInterval(uhcState.timerInterval);
  
  uhcState.timerInterval = setInterval(async () => {
    if (!uhcState.active) {
      clearInterval(uhcState.timerInterval);
      return;
    }

    uhcState.elapsedSeconds++;

    const totalGraceSeconds = uhcState.graceTime * 60;
    const zone1EndSeconds = totalGraceSeconds + (uhcState.zone1Time * 60);
    const zone2EndSeconds = zone1EndSeconds + (uhcState.zone2Time * 60);
    const zone3EndSeconds = zone2EndSeconds + (uhcState.zone3Time * 60);

    // Dynamic border size estimation for UI
    if (uhcState.state === 'grace') {
      uhcState.currentBorderSize = finalStartBorder;
    } else if (uhcState.state === 'zone1') {
      const elapsedInZone = uhcState.elapsedSeconds - totalGraceSeconds;
      const totalZoneTime = uhcState.zone1Time * 60;
      const progress = Math.min(1, elapsedInZone / totalZoneTime);
      uhcState.currentBorderSize = Math.round(finalStartBorder - (finalStartBorder - uhcState.zone1Border) * progress);
    } else if (uhcState.state === 'zone2') {
      const elapsedInZone = uhcState.elapsedSeconds - zone1EndSeconds;
      const totalZoneTime = uhcState.zone2Time * 60;
      const progress = Math.min(1, elapsedInZone / totalZoneTime);
      uhcState.currentBorderSize = Math.round(uhcState.zone1Border - (uhcState.zone1Border - uhcState.zone2Border) * progress);
    } else if (uhcState.state === 'zone3') {
      const elapsedInZone = uhcState.elapsedSeconds - zone2EndSeconds;
      const totalZoneTime = uhcState.zone3Time * 60;
      const progress = Math.min(1, elapsedInZone / totalZoneTime);
      uhcState.currentBorderSize = Math.round(uhcState.zone2Border - (uhcState.zone2Border - uhcState.zone3Border) * progress);
    } else if (uhcState.state === 'finished') {
      uhcState.currentBorderSize = uhcState.zone3Border;
    }

    // State Transitions
    // Grace ends -> PvP Active & Zone 1 starts
    if (uhcState.elapsedSeconds === totalGraceSeconds) {
      uhcState.state = 'zone1';
      await executeRconCommand('gamerule pvp true');
      await executeRconCommand(`worldborder set ${uhcState.zone1Border} ${uhcState.zone1Time * 60}`);
      await executeRconCommand('title @a title {"text":"ZONA 1","color":"gold"}');
      await executeRconCommand('title @a subtitle {"text":"PvP ACTIVO - Borde reduciéndose","color":"red"}');
      await executeRconCommand(`say ¡El tiempo de gracia ha terminado! PvP activado. El borde se reduce a ${uhcState.zone1Border} bloques.`);
      await executeRconCommand('playeffect @a ambient.weather.thunder');
    }
    // Zone 1 ends -> Zone 2 starts
    else if (uhcState.elapsedSeconds === zone1EndSeconds) {
      uhcState.state = 'zone2';
      await executeRconCommand(`worldborder set ${uhcState.zone2Border} ${uhcState.zone2Time * 60}`);
      await executeRconCommand('title @a title {"text":"ZONA 2","color":"gold"}');
      await executeRconCommand('title @a subtitle {"text":"Borde reduciéndose a Zona 2","color":"yellow"}');
      await executeRconCommand(`say ¡Borde de Zona 1 cerrado! Iniciando reducción a Zona 2: ${uhcState.zone2Border} bloques.`);
      await executeRconCommand('playeffect @a ambient.weather.thunder');
    }
    // Zone 2 ends -> Zone 3 starts
    else if (uhcState.elapsedSeconds === zone2EndSeconds) {
      uhcState.state = 'zone3';
      await executeRconCommand(`worldborder set ${uhcState.zone3Border} ${uhcState.zone3Time * 60}`);
      await executeRconCommand('title @a title {"text":"ZONA FINAL","color":"red"}');
      await executeRconCommand('title @a subtitle {"text":"Borde reduciéndose a Zone 3","color":"dark_red"}');
      await executeRconCommand(`say ¡Borde de Zona 2 cerrado! Iniciando reducción final a Zona 3: ${uhcState.zone3Border} bloques.`);
      await executeRconCommand('playeffect @a ambient.weather.thunder');
    }
    // Zone 3 ends -> Finished
    else if (uhcState.elapsedSeconds >= zone3EndSeconds && uhcState.state !== 'finished') {
      uhcState.state = 'finished';
      uhcState.currentBorderSize = uhcState.zone3Border;
      await executeRconCommand('title @a title {"text":"BORDE FINAL","color":"dark_red"}');
      await executeRconCommand('title @a subtitle {"text":"¡El borde ha dejado de moverse!","color":"gold"}');
      await executeRconCommand(`say ¡El borde final ha alcanzado su límite mínimo de ${uhcState.zone3Border} bloques!`);
    }

  }, 1000);
}

// --- RECONNECTING RCON CLIENT CLASS ---
class ReconnectingRcon {
  constructor() {
    this.rcon = null;
    this.connected = false;
    this.connecting = false;
    this.reconnectTimer = null;
  }

  async connect() {
    if (this.connected && this.rcon) return this.rcon;
    if (this.connecting) return null;

    this.connecting = true;
    console.log(`[RCON] Attempting to connect to ${RCON_HOST}:${RCON_PORT}...`);

    try {
      this.rcon = await Rcon.connect({
        host: RCON_HOST,
        port: RCON_PORT,
        password: RCON_PASSWORD,
        timeout: 5000,
      });

      this.connected = true;
      this.connecting = false;
      console.log('[RCON] Connected successfully!');

      // Auto build/refresh lobby on connection
      setTimeout(() => {
        generateLobbyStructure().catch(() => {});
      }, 3000);

      // Set up error handlers for socket termination
      this.rcon.on('end', () => {
        console.warn('[RCON] Connection closed by remote host.');
        this.handleDisconnect();
      });

      this.rcon.on('error', (err) => {
        console.error('[RCON] Socket error:', err.message);
        this.handleDisconnect();
      });

      return this.rcon;
    } catch (err) {
      console.error(`[RCON] Failed connection: ${err.message}`);
      this.connecting = false;
      this.handleDisconnect();
      return null;
    }
  }

  handleDisconnect() {
    this.connected = false;
    this.rcon = null;
    
    if (!this.reconnectTimer) {
      console.log('[RCON] Scheduling reconnection in 5 seconds...');
      this.reconnectTimer = setTimeout(() => {
        this.reconnectTimer = null;
        this.connect().catch(() => {});
      }, 5000);
    }
  }

  async send(command) {
    if (!this.connected || !this.rcon) {
      const client = await this.connect();
      if (!client) {
        throw new Error('RCON server is offline or unreachable.');
      }
    }
    return await this.rcon.send(command);
  }
}

const rconClient = new ReconnectingRcon();
// Proactively connect to RCON
rconClient.connect().catch(() => {});

// --- TCP PING UTILITY ---
// Validates if Minecraft is running at port 25565
function pingMinecraftServer() {
  return new Promise((resolve) => {
    const socket = new net.Socket();
    const startTime = Date.now();

    socket.setTimeout(2000);

    socket.connect(25565, RCON_HOST, () => {
      const ping = Date.now() - startTime;
      socket.destroy();
      resolve({ online: true, ping });
    });

    socket.on('error', () => {
      socket.destroy();
      resolve({ online: false, ping: 0 });
    });

    socket.on('timeout', () => {
      socket.destroy();
      resolve({ online: false, ping: 0 });
    });
  });
}

// --- USERCACHE PARSER ---
function loadUserCache() {
  const cacheMap = new Map();
  try {
    if (fs.existsSync(USERCACHE_PATH)) {
      const content = fs.readFileSync(USERCACHE_PATH, 'utf8');
      const data = JSON.parse(content);
      if (Array.isArray(data)) {
        data.forEach((entry) => {
          if (entry.name && entry.uuid) {
            // Clean uuid to match easily (lowercase, strip hyphens)
            const cleanUuid = entry.uuid.toLowerCase().replace(/-/g, '');
            cacheMap.set(cleanUuid, entry.name);
          }
        });
      }
    }
  } catch (err) {
    console.error('[Parser] Error loading usercache.json:', err.message);
  }
  return cacheMap;
}

// --- MOCK DATA ---
const MOCK_LEADERBOARD = [
  { rank: 1, name: 'Technoblade', uuid: 'mock-1', kills: 15, deaths: 0, status: 'Alive' },
  { rank: 2, name: 'Dream', uuid: 'mock-2', kills: 12, deaths: 0, status: 'Alive' },
  { rank: 3, name: 'Sapnap', uuid: 'mock-3', kills: 9, deaths: 0, status: 'Alive' },
  { rank: 4, name: 'Philsa', uuid: 'mock-4', kills: 8, deaths: 0, status: 'Alive' },
  { rank: 5, name: 'GeorgeNotFound', uuid: 'mock-5', kills: 4, deaths: 1, status: 'Eliminated' },
  { rank: 6, name: 'TommyInnit', uuid: 'mock-6', kills: 2, deaths: 1, status: 'Eliminated' },
  { rank: 7, name: 'Tubbo', uuid: 'mock-7', kills: 1, deaths: 1, status: 'Eliminated' },
  { rank: 8, name: 'WilburSoot', uuid: 'mock-8', kills: 0, deaths: 1, status: 'Eliminated' }
];

// --- API ENDPOINTS ---

// 1. /api/status
app.get('/api/status', async (req, res) => {
  try {
    const pingStatus = await pingMinecraftServer();
    if (!pingStatus.online) {
      return res.json({
        online: false,
        ping: 0,
        rconConnected: false,
        playerCount: 0,
        maxPlayers: 0,
        players: [],
        rawList: ''
      });
    }

    let players = [];
    let playerCount = 0;
    let maxPlayers = 20; // fallback default
    let rawList = '';

    if (rconClient.connected) {
      try {
        rawList = await rconClient.send('list');
        // Parse list response (e.g. "There are 2/20 players online: Player1, Player2")
        // Or older Paper: "There are 2 players online: Player1, Player2"
        const listRegex = /There are (\d+)(?:\/(\d+))? players online:(.*)/i;
        const match = rawList.match(listRegex);
        
        if (match) {
          playerCount = parseInt(match[1], 10);
          if (match[2]) {
            maxPlayers = parseInt(match[2], 10);
          }
          const namesPart = match[3].trim();
          if (namesPart) {
            players = namesPart.split(',').map(name => name.trim()).filter(Boolean);
          }
        } else {
          // Alternative generic match for simple formats
          const simpleMatch = /online:\s*(.*)/i.exec(rawList);
          if (simpleMatch && simpleMatch[1]) {
            players = simpleMatch[1].split(',').map(name => name.trim()).filter(Boolean);
            playerCount = players.length;
          }
        }
      } catch (rconErr) {
        console.warn('[API Status] Failed to query players list via RCON:', rconErr.message);
      }
    }

    res.json({
      online: true,
      ping: pingStatus.ping,
      rconConnected: rconClient.connected,
      playerCount: playerCount || players.length,
      maxPlayers,
      players,
      rawList
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 2. /api/stop
app.post('/api/stop', async (req, res) => {
  try {
    console.log('[API Stop] Received shutdown request.');
    if (!rconClient.connected) {
      return res.status(503).json({ error: 'RCON connection is offline. Cannot stop server.' });
    }
    const response = await rconClient.send('stop');
    res.json({ success: true, message: 'Server shutdown command sent successfully.', response });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 3. /api/command
app.post('/api/command', async (req, res) => {
  const { command } = req.body;
  if (!command) {
    return res.status(400).json({ error: 'Missing parameter: command' });
  }

  try {
    if (!rconClient.connected) {
      return res.status(503).json({ error: 'RCON client is not connected to server.' });
    }
    console.log(`[API Command] Executing: /${command}`);
    const response = await rconClient.send(command);
    res.json({ success: true, response });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 4. /api/stats
app.get('/api/stats', (req, res) => {
  try {
    if (!fs.existsSync(STATS_DIR)) {
      console.log(`[Parser] Stats folder not found at ${STATS_DIR}. Returning mock UHC leaderboard.`);
      return res.json({ source: 'mock', data: MOCK_LEADERBOARD });
    }

    const files = fs.readdirSync(STATS_DIR).filter((file) => file.endsWith('.json'));

    if (files.length === 0) {
      console.log('[Parser] Stats folder empty. Returning mock UHC leaderboard.');
      return res.json({ source: 'mock', data: MOCK_LEADERBOARD });
    }

    const cacheMap = loadUserCache();
    const statsList = [];

    files.forEach((filename) => {
      try {
        const filepath = path.join(STATS_DIR, filename);
        const uuid = filename.replace('.json', '');
        const cleanUuid = uuid.toLowerCase().replace(/-/g, '');

        const content = fs.readFileSync(filepath, 'utf8');
        const json = JSON.parse(content);

        // Fetch statistics compatible with Paper/Spigot 1.8.8 flat format & newer nested format
        let kills = 0;
        let deaths = 0;

        // 1.8 flat format keys
        if (json['stat.playerKills'] !== undefined) kills = json['stat.playerKills'];
        else if (json['stat.killEntity.Player'] !== undefined) kills = json['stat.killEntity.Player'];

        if (json['stat.deaths'] !== undefined) deaths = json['stat.deaths'];

        // 1.15+ nested format support (just in case)
        if (json.stats) {
          const custom = json.stats['minecraft:custom'];
          if (custom) {
            if (custom['minecraft:player_kills'] !== undefined) kills = custom['minecraft:player_kills'];
            if (custom['minecraft:deaths'] !== undefined) deaths = custom['minecraft:deaths'];
          }
        }

        // Map UUID to Username via usercache
        const username = cacheMap.get(cleanUuid) || uuid.substring(0, 8);

        statsList.push({
          uuid,
          name: username,
          kills,
          deaths,
          status: deaths > 0 ? 'Eliminated' : 'Alive'
        });
      } catch (fileErr) {
        console.error(`[Parser] Error reading stats file ${filename}:`, fileErr.message);
      }
    });

    // Sort leaderboard by kills descending, and alive status prioritized, then alphabetically
    statsList.sort((a, b) => {
      if (b.kills !== a.kills) return b.kills - a.kills;
      if (a.status !== b.status) return a.status === 'Alive' ? -1 : 1;
      return a.name.localeCompare(b.name);
    });

    // Add positions
    const sortedLeaderboard = statsList.map((entry, index) => ({
      rank: index + 1,
      ...entry
    }));

    res.json({ source: 'live', data: sortedLeaderboard });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// --- UHC API ENDPOINTS ---

// 5. GET /api/uhc/status
app.get('/api/uhc/status', (req, res) => {
  res.json({
    active: uhcState.active,
    state: uhcState.state,
    startTime: uhcState.startTime,
    graceTime: uhcState.graceTime,
    startBorderSize: uhcState.startBorderSize,
    playersPerBorder: uhcState.playersPerBorder,
    calcBorderByDensity: uhcState.calcBorderByDensity,
    scenarios: uhcState.scenarios,
    zone1Border: uhcState.zone1Border,
    zone1Time: uhcState.zone1Time,
    zone2Border: uhcState.zone2Border,
    zone2Time: uhcState.zone2Time,
    zone3Border: uhcState.zone3Border,
    zone3Time: uhcState.zone3Time,
    currentBorderSize: uhcState.currentBorderSize,
    elapsedSeconds: uhcState.elapsedSeconds
  });
});

// 6. POST /api/uhc/config
app.post('/api/uhc/config', (req, res) => {
  try {
    const {
      graceTime,
      startBorderSize,
      playersPerBorder,
      calcBorderByDensity,
      scenarios,
      zone1Border,
      zone1Time,
      zone2Border,
      zone2Time,
      zone3Border,
      zone3Time
    } = req.body;

    if (uhcState.active) {
      return res.status(400).json({ error: 'No se puede modificar la configuración mientras hay un UHC activo.' });
    }

    if (graceTime !== undefined) uhcState.graceTime = parseInt(graceTime, 10);
    if (startBorderSize !== undefined) uhcState.startBorderSize = parseInt(startBorderSize, 10);
    if (playersPerBorder !== undefined) uhcState.playersPerBorder = parseInt(playersPerBorder, 10);
    if (calcBorderByDensity !== undefined) uhcState.calcBorderByDensity = !!calcBorderByDensity;
    if (scenarios !== undefined) uhcState.scenarios = { ...uhcState.scenarios, ...scenarios };
    
    if (zone1Border !== undefined) uhcState.zone1Border = parseInt(zone1Border, 10);
    if (zone1Time !== undefined) uhcState.zone1Time = parseInt(zone1Time, 10);
    
    if (zone2Border !== undefined) uhcState.zone2Border = parseInt(zone2Border, 10);
    if (zone2Time !== undefined) uhcState.zone2Time = parseInt(zone2Time, 10);
    
    if (zone3Border !== undefined) uhcState.zone3Border = parseInt(zone3Border, 10);
    if (zone3Time !== undefined) uhcState.zone3Time = parseInt(zone3Time, 10);

    saveUhcConfig();
    res.json({ success: true, config: uhcState });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 7. POST /api/uhc/start
app.post('/api/uhc/start', async (req, res) => {
  try {
    if (uhcState.active) {
      return res.status(400).json({ error: 'El torneo UHC ya está activo.' });
    }
    if (!rconClient.connected) {
      return res.status(503).json({ error: 'Consola RCON no conectada. No se puede iniciar el UHC.' });
    }

    await startUhcGame();
    res.json({ success: true, message: '¡Torneo UHC iniciado exitosamente!' });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 8. POST /api/uhc/stop
app.post('/api/uhc/stop', async (req, res) => {
  try {
    if (!uhcState.active) {
      return res.status(400).json({ error: 'No hay ningún torneo UHC activo.' });
    }

    console.log('[UHC Engine] Stopping and resetting UHC...');
    uhcState.active = false;
    uhcState.state = 'idle';
    uhcState.elapsedSeconds = 0;
    
    if (uhcState.timerInterval) {
      clearInterval(uhcState.timerInterval);
      uhcState.timerInterval = null;
    }

    // Reset RCON settings
    await executeRconCommand('worldborder set 60000');
    await executeRconCommand('gamerule pvp true');
    await executeRconCommand('gamerule naturalRegeneration true');
    await executeRconCommand('effect @a clear');
    await executeRconCommand('clear @a');
    
    // Clear custom scenarios
    if (uhcState.scenarios.doubleHealth) {
      await executeRconCommand('effect @a clear');
    }

    await executeRconCommand('title @a title {"text":"UHC DETENIDO","color":"red"}');
    await executeRconCommand('title @a subtitle {"text":"El administrador detuvo el juego","color":"yellow"}');
    await executeRconCommand('say ¡El torneo UHC ha sido cancelado y reiniciado por el administrador!');

    // Rebuild and teleport to lobby
    await generateLobbyStructure();

    res.json({ success: true, message: '¡Torneo UHC detenido y reiniciado exitosamente!' });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 9. POST /api/lobby/regenerate
app.post('/api/lobby/regenerate', async (req, res) => {
  try {
    if (!rconClient.connected) {
      return res.status(503).json({ error: 'La consola RCON no está conectada. No se puede regenerar el lobby.' });
    }
    await generateLobbyStructure();
    res.json({ success: true, message: '¡Lobby del torneo regenerado y construído exitosamente!' });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Serve frontend assets statically
app.use(express.static(path.join(__dirname, 'public')));

// Fallback to index.html for general routes
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// Start Express Listener
app.listen(PORT, () => {
  console.log(`==================================================`);
  console.log(` UHC TOURNAMENT CONTROLLER RUNNING ON PORT ${PORT} `);
  console.log(`==================================================`);
});
