/* ==========================================================================
   MINECRAFT UHC TOURNAMENT FRONTEND CLIENT
   ========================================================================== */

document.addEventListener('DOMContentLoaded', () => {
  // --- DOM ELEMENTS ---
  const statusGlow = document.getElementById('status-glow');
  const statusText = document.getElementById('status-text');
  
  const metricOnline = document.getElementById('metric-online');
  const metricKills = document.getElementById('metric-kills');
  const metricLeader = document.getElementById('metric-leader');
  const metricLeaderSub = document.getElementById('metric-leader-sub');
  
  const leaderboardBody = document.getElementById('leaderboard-body');
  const leaderboardBadge = document.getElementById('leaderboard-badge');
  const pingValue = document.getElementById('ping-value');
  const rconStatus = document.getElementById('rcon-status');
  
  // Console Elements
  const consoleOutput = document.getElementById('console-output');
  const consoleForm = document.getElementById('console-form');
  const consoleInput = document.getElementById('console-input');
  const consoleSend = document.getElementById('console-send');
  const quickCmdButtons = document.querySelectorAll('.cmd-pill');
  
  // Stop Modal Elements
  const btnStop = document.getElementById('btn-stop');
  const btnRegenLobby = document.getElementById('btn-regen-lobby');
  const stopModal = document.getElementById('stop-modal');
  const modalCancel = document.getElementById('modal-cancel');
  const modalConfirm = document.getElementById('modal-confirm');

  // Regen Modal Elements
  const btnRegenWorld = document.getElementById('btn-regen-world');
  const regenModal = document.getElementById('regen-modal');
  const modalRegenCancel = document.getElementById('modal-regen-cancel');
  const modalRegenConfirm = document.getElementById('modal-regen-confirm');
  
  const notificationContainer = document.getElementById('notification-container');
  const historyContainer = document.getElementById('history-container');
  const uhcEventName = document.getElementById('uhc-event-name');
  const uhcServerIp = document.getElementById('uhc-server-ip');

  // UHC DOM Elements
  const uhcBadge = document.getElementById('uhc-badge');
  const uhcPhaseVal = document.getElementById('uhc-phase-val');
  const uhcTimerVal = document.getElementById('uhc-timer-val');
  const uhcBordeVal = document.getElementById('uhc-border-val');
  const btnUhcStart = document.getElementById('btn-uhc-start');
  const btnUhcStop = document.getElementById('btn-uhc-stop');
  const btnUhcSave = document.getElementById('btn-uhc-save');
  const uhcConfigForm = document.getElementById('uhc-config-form');
  
  // Scenarios checkboxes
  const scenCutClean = document.getElementById('scen-cutclean');
  const scenFireless = document.getElementById('scen-fireless');
  const scenNoClean = document.getElementById('scen-noclean');
  const scenDoubleHealth = document.getElementById('scen-doublehealth');
  const scenTimeBomb = document.getElementById('scen-timebomb');
  const scenVanillaPlus = document.getElementById('scen-vanillaplus');
  const scenEnchant17 = document.getElementById('scen-enchant17');
  
  // Settings inputs
  const uhcGraceTime = document.getElementById('uhc-grace-time');
  const uhcFinalHealTime = document.getElementById('uhc-final-heal-time');
  const uhcWorldSeed = document.getElementById('uhc-world-seed');
  const uhcCalcDensity = document.getElementById('uhc-calc-density');
  const uhcStartBorder = document.getElementById('uhc-start-border');
  const uhcPlayersBorder = document.getElementById('uhc-players-border');
  
  // Zone inputs
  const uhcZ1Border = document.getElementById('uhc-z1-border');
  const uhcZ1Time = document.getElementById('uhc-z1-time');
  const uhcZ2Border = document.getElementById('uhc-z2-border');
  const uhcZ2Time = document.getElementById('uhc-z2-time');
  const uhcZ3Border = document.getElementById('uhc-z3-border');
  const uhcZ3Time = document.getElementById('uhc-z3-time');
  
  // Form conditional groups
  const groupManualBorder = document.getElementById('group-manual-border');
  const groupDensityPlayers = document.getElementById('group-density-players');

  // --- STATE VARIABLES ---
  let isServerOnline = false;
  let isRconConnected = false;
  let isUhcActive = false;
  let hasInitialConfigLoaded = false;

  // --- UTILITY: TOAST NOTIFICATIONS ---
  function showNotification(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    let iconClass = 'fa-circle-info';
    if (type === 'success') iconClass = 'fa-circle-check';
    if (type === 'danger') iconClass = 'fa-triangle-exclamation';
    
    toast.innerHTML = `
      <i class="fa-solid ${iconClass}"></i>
      <span>${message}</span>
    `;
    
    notificationContainer.appendChild(toast);
    
    // Auto-remove toast after 4 seconds
    setTimeout(() => {
      toast.style.animation = 'slide-in 0.3s cubic-bezier(0.4, 0, 0.2, 1) reverse';
      toast.style.opacity = '0';
      setTimeout(() => {
        toast.remove();
      }, 300);
    }, 4000);
  }

  // --- UTILITY: CONSOLE PRINTER ---
  function printToConsole(text, type = 'output-res') {
    const line = document.createElement('div');
    line.className = `console-line ${type}`;
    line.textContent = text;
    consoleOutput.appendChild(line);
    // Auto scroll to bottom
    consoleOutput.scrollTop = consoleOutput.scrollHeight;
  }

  // --- API SERVICE CALLS ---

  // 1. Fetch Server Status
  async function updateStatus() {
    try {
      const res = await fetch('/api/status');
      if (!res.ok) throw new Error('API unreachable');
      const data = await res.json();
      
      isServerOnline = data.online;
      isRconConnected = data.rconConnected;

      // Update Header Status Widget
      if (isServerOnline) {
        statusGlow.className = 'status-indicator online';
        statusText.textContent = 'ONLINE';
        statusText.style.color = 'var(--color-success)';
        
        pingValue.textContent = `${data.ping} ms`;
        pingValue.style.color = 'var(--color-success)';
      } else {
        statusGlow.className = 'status-indicator offline';
        statusText.textContent = 'OFFLINE';
        statusText.style.color = 'var(--color-danger)';
        
        pingValue.textContent = '-- ms';
        pingValue.style.color = 'var(--text-muted)';
      }

      // Update RCON Console status badge & enable inputs
      if (isRconConnected) {
        rconStatus.textContent = 'CONECTADO';
        rconStatus.className = 'status-badge connected';
        
        consoleInput.removeAttribute('disabled');
        consoleSend.removeAttribute('disabled');
        consoleInput.placeholder = "Escribe un comando de Minecraft...";
      } else {
        rconStatus.textContent = 'DESCONECTADO';
        rconStatus.className = 'status-badge';
        
        consoleInput.setAttribute('disabled', 'true');
        consoleSend.setAttribute('disabled', 'true');
        
        if (isServerOnline) {
          consoleInput.placeholder = "Conectando consola RCON...";
        } else {
          consoleInput.placeholder = "Servidor fuera de línea.";
        }
      }

      // Update Players Metric Card
      metricOnline.textContent = `${data.playerCount} / ${data.maxPlayers}`;
      
    } catch (err) {
      console.error('Error loading server status:', err);
      // Offline fallback
      statusGlow.className = 'status-indicator offline';
      statusText.textContent = 'DISCONNECTED';
      statusText.style.color = 'var(--color-danger)';
      pingValue.textContent = '-- ms';
      rconStatus.textContent = 'DESCONECTADO';
      rconStatus.className = 'status-badge';
      consoleInput.setAttribute('disabled', 'true');
      consoleSend.setAttribute('disabled', 'true');
      consoleInput.placeholder = "Error de conexión con API.";
      metricOnline.textContent = '0 / 0';
    }
  }

  // 2. Fetch Stats & Leaderboard
  async function updateStats() {
    try {
      const res = await fetch('/api/stats');
      if (!res.ok) throw new Error('API stats error');
      const payload = await res.json();
      const players = payload.data;
      const source = payload.source;

      // Update stats synchronization status badge
      leaderboardBadge.textContent = source === 'live' ? 'En Vivo' : 'Simulación';
      leaderboardBadge.style.backgroundColor = source === 'live' ? 'var(--color-success-glow)' : 'rgba(255,255,255,0.05)';
      leaderboardBadge.style.color = source === 'live' ? '#34d399' : 'var(--text-muted)';

      // Rebuild Table rows
      if (players.length === 0) {
        leaderboardBody.innerHTML = `
          <tr>
            <td colspan="4" class="loading-td">No hay estadísticas de jugadores disponibles todavía.</td>
          </tr>
        `;
        metricKills.textContent = '0';
        metricLeader.textContent = 'Nadie';
        metricLeaderSub.textContent = '0 Kills';
        return;
      }

      let totalKills = 0;
      let rowsHtml = '';
      
      players.forEach((player) => {
        totalKills += player.kills;

        // Rank format
        let rankHtml = '';
        if (player.rank === 1) rankHtml = `<span class="rank-badge rank-gold"><i class="fa-solid fa-crown"></i></span>`;
        else if (player.rank === 2) rankHtml = `<span class="rank-badge rank-silver">2</span>`;
        else if (player.rank === 3) rankHtml = `<span class="rank-badge rank-bronze">3</span>`;
        else rankHtml = `<span class="rank-badge rank-normal">${player.rank}</span>`;

        // Status badge
        let statusClass = 'eliminated';
        let statusLabel = 'Eliminado';
        let statusIcon = 'fa-skull-crossbones';

        if (player.status === 'Ganador') {
          statusClass = 'winner';
          statusLabel = 'Ganador';
          statusIcon = 'fa-trophy';
        } else if (typeof player.status === 'string' && player.status.startsWith('Top')) {
          statusClass = 'top';
          statusLabel = player.status;
          statusIcon = 'fa-medal';
        } else if (player.status === 'Alive') {
          statusClass = 'alive';
          statusLabel = 'Vivo';
          statusIcon = 'fa-heartbeat';
        }
        
        const statusHtml = `
          <span class="status-pill ${statusClass}">
            <i class="fa-solid ${statusIcon}"></i> ${statusLabel}
          </span>
        `;

        // Use mc-heads.net API for 3D skins (fallback works automatically inside API service)
        const avatarUrl = `https://mc-heads.net/avatar/${player.name}/64`;

        rowsHtml += `
          <tr>
            <td class="col-rank">${rankHtml}</td>
            <td class="col-player">
              <div class="player-profile">
                <img class="player-avatar" src="${avatarUrl}" alt="${player.name}" onerror="this.src='https://mc-heads.net/avatar/MHF_Steve/64'">
                <span class="player-name">${player.name}</span>
              </div>
            </td>
            <td class="col-status">${statusHtml}</td>
            <td class="col-kills text-right font-mono text-gold kills-count">${player.kills}</td>
          </tr>
        `;
      });

      leaderboardBody.innerHTML = rowsHtml;

      // Update Kills Card
      metricKills.textContent = totalKills;

      // Update Leader Card
      const leader = players[0];
      if (leader && leader.kills > 0) {
        metricLeader.textContent = leader.name;
        metricLeaderSub.textContent = `${leader.kills} Kills (${leader.status === 'Alive' ? 'Vivo' : 'Muerto'})`;
      } else {
        metricLeader.textContent = 'Empate';
        metricLeaderSub.textContent = 'Ninguno tiene bajas';
      }

    } catch (err) {
      console.error('Error fetching player UHC stats:', err);
      leaderboardBody.innerHTML = `
        <tr>
          <td colspan="4" class="loading-td text-danger">Error al consultar el ranking del torneo.</td>
        </tr>
      `;
    }
  }

  // 3. Send Console Command via RCON
  async function sendCommand(commandText) {
    let cleanText = commandText.trim();
    if (!cleanText) return;

    // Strip leading slashes
    while (cleanText.startsWith('/')) {
      cleanText = cleanText.substring(1);
    }

    printToConsole(`mc-server$ /${cleanText}`, 'input-cmd');
    consoleInput.value = '';

    try {
      const res = await fetch('/api/command', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ command: cleanText })
      });

      const data = await res.json();
      
      if (res.ok && data.success) {
        // Strip out Minecraft color codes standard formats (§r, §f, etc) to render nicely in text terminal
        const cleanResponse = data.response.replace(/§[0-9a-fk-or]/gi, '');
        printToConsole(cleanResponse || '[Servidor ejecutó el comando sin respuesta de consola.]', 'output-res');
      } else {
        printToConsole(`Error: ${data.error || 'No se pudo procesar'}`, 'error');
        showNotification(data.error || 'Fallo al ejecutar el comando.', 'danger');
      }
    } catch (err) {
      printToConsole(`Error de Red: No se pudo conectar al API controller`, 'error');
      showNotification('Conexión con controlador perdida.', 'danger');
    }
  }

  // 4. Request Server Stop
  async function shutdownServer() {
    try {
      showNotification('Enviando comando de apagado al servidor...', 'info');
      const res = await fetch('/api/stop', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      });
      
      const data = await res.json();
      if (res.ok && data.success) {
        showNotification('Servidor Minecraft ordenado a detenerse.', 'success');
        printToConsole('[RCON] Comando /stop ejecutado. El servidor se está cerrando.', 'system');
      } else {
        showNotification(data.error || 'No se pudo apagar el servidor.', 'danger');
      }
    } catch (err) {
      showNotification('Fallo de red al apagar el servidor.', 'danger');
    }
  }

  // --- UHC CLIENT METHODS ---

  // Helper to format elapsed seconds to HH:MM:SS
  function formatSeconds(totalSeconds) {
    const hrs = Math.floor(totalSeconds / 3600);
    const mins = Math.floor((totalSeconds % 3600) / 60);
    const secs = totalSeconds % 60;
    return [
      hrs.toString().padStart(2, '0'),
      mins.toString().padStart(2, '0'),
      secs.toString().padStart(2, '0')
    ].join(':');
  }

  function toggleDensityView(calcByDensity) {
    if (calcByDensity) {
      groupManualBorder.classList.add('hidden');
      groupDensityPlayers.classList.remove('hidden');
    } else {
      groupManualBorder.classList.remove('hidden');
      groupDensityPlayers.classList.add('hidden');
    }
  }

  function populateUhcForm(config) {
    if (!config) return;
    
    uhcEventName.value = config.eventName || 'UHC Evento';
    uhcServerIp.value = config.serverIp || 'play.rankit.net';
    
    // Scenarios
    if (config.scenarios) {
      scenCutClean.checked = !!config.scenarios.cutClean;
      scenFireless.checked = !!config.scenarios.fireless;
      scenNoClean.checked = !!config.scenarios.noClean;
      scenDoubleHealth.checked = !!config.scenarios.doubleHealth;
      scenTimeBomb.checked = !!config.scenarios.timeBomb;
      scenVanillaPlus.checked = !!config.scenarios.vanillaPlus;
      scenEnchant17.checked = !!config.scenarios.enchant17;
    }
    
    // Core parameters
    uhcGraceTime.value = config.graceTime || 20;
    uhcFinalHealTime.value = config.finalHealTime || 10;
    uhcWorldSeed.value = config.worldSeed || '';
    uhcCalcDensity.checked = !!config.calcBorderByDensity;
    uhcStartBorder.value = config.startBorderSize || 2000;
    uhcPlayersBorder.value = config.playersPerBorder || 4;
    
    // Zones
    uhcZ1Border.value = config.zone1Border || 1000;
    uhcZ1Time.value = config.zone1Time || 10;
    
    uhcZ2Border.value = config.zone2Border || 500;
    uhcZ2Time.value = config.zone2Time || 10;
    
    uhcZ3Border.value = config.zone3Border || 100;
    uhcZ3Time.value = config.zone3Time || 10;
    
    // Update visual density layout
    toggleDensityView(config.calcBorderByDensity);
  }

  function disableUhcForm(disabled) {
    const inputs = uhcConfigForm.querySelectorAll('input, button');
    inputs.forEach((input) => {
      if (input.id === 'btn-uhc-stop') return;
      if (disabled) {
        input.setAttribute('disabled', 'true');
      } else {
        input.removeAttribute('disabled');
      }
    });
  }

  // Poll UHC Tournament state
  async function updateUhcStatus() {
    try {
      const res = await fetch('/api/uhc/status');
      if (!res.ok) throw new Error('UHC API unreachable');
      const data = await res.json();
      
      isUhcActive = data.active;
      
      // Update badge status
      if (isUhcActive) {
        uhcBadge.textContent = 'UHC EN CURSO';
        uhcBadge.className = 'badge active';
        
        btnUhcStart.classList.add('hidden');
        btnUhcStop.classList.remove('hidden');
        btnUhcSave.setAttribute('disabled', 'true');
        
        disableUhcForm(true);
      } else {
        uhcBadge.textContent = 'UHC DETENIDO';
        uhcBadge.className = 'badge';
        
        btnUhcStart.classList.remove('hidden');
        btnUhcStop.classList.add('hidden');
        btnUhcSave.removeAttribute('disabled');
        
        disableUhcForm(false);
      }
      
      // Update HUD phase
      let phaseText = 'SIN INICIAR';
      switch (data.state) {
        case 'grace':
          phaseText = 'GRACIA (PvP OFF)';
          break;
        case 'zone1':
          phaseText = 'ZONA 1 (PvP ON)';
          break;
        case 'zone2':
          phaseText = 'ZONA 2';
          break;
        case 'zone3':
          phaseText = 'ZONA FINAL';
          break;
        case 'finished':
          phaseText = 'BORDE LÍMITE';
          break;
      }
      uhcPhaseVal.textContent = phaseText;
      
      // Update Timer
      uhcTimerVal.textContent = formatSeconds(data.elapsedSeconds || 0);
      
      // Update Border size display
      if (data.currentBorderSize) {
        uhcBordeVal.textContent = `${data.currentBorderSize} x ${data.currentBorderSize} bloques`;
      } else {
        uhcBordeVal.textContent = '-- bloques';
      }
      
      // Load initial configurations once
      if (!isUhcActive && !hasInitialConfigLoaded) {
        populateUhcForm(data);
        hasInitialConfigLoaded = true;
      }
      
    } catch (err) {
      console.error('Error fetching UHC status:', err);
    }
  }

  // --- EVENT LISTENERS ---

  // Console Submit
  consoleForm.addEventListener('submit', (e) => {
    e.preventDefault();
    const commandText = consoleInput.value;
    sendCommand(commandText);
  });

  // Quick Command Pills
  quickCmdButtons.forEach((btn) => {
    btn.addEventListener('click', () => {
      const cmd = btn.getAttribute('data-cmd');
      if (isRconConnected) {
        sendCommand(cmd);
      } else {
        showNotification('La consola RCON no está conectada.', 'danger');
      }
    });
  });

  // Stop Server Confirmation Overlay Modal
  btnStop.addEventListener('click', () => {
    if (!isServerOnline) {
      showNotification('El servidor de Minecraft ya está apagado.', 'info');
      return;
    }
    stopModal.classList.add('active');
  });

  modalCancel.addEventListener('click', () => {
    stopModal.classList.remove('active');
  });

  modalConfirm.addEventListener('click', () => {
    stopModal.classList.remove('active');
    shutdownServer();
  });

  // Regenerate spawn lobby
  if (btnRegenLobby) {
    btnRegenLobby.addEventListener('click', async () => {
      if (!isServerOnline) {
        showNotification('El servidor de Minecraft está apagado.', 'danger');
        return;
      }
      if (!isRconConnected) {
        showNotification('La consola RCON está desconectada.', 'danger');
        return;
      }

      try {
        showNotification('Construyendo lobby spawn en el servidor...', 'info');
        const res = await fetch('/api/lobby/regenerate', { method: 'POST' });
        const data = await res.json();
        if (res.ok && data.success) {
          showNotification('¡Lobby spawn reconstruído exitosamente!', 'success');
          printToConsole('[Lobby Engine] ¡Lobby del torneo generado exitosamente!', 'system');
        } else {
          showNotification(data.error || 'Fallo al construir el lobby.', 'danger');
        }
      } catch (err) {
        showNotification('Error de red al reconstruir el lobby.', 'danger');
      }
    });
  }

  // Close modal when clicking outer backdrop
  stopModal.addEventListener('click', (e) => {
    if (e.target === stopModal) {
      stopModal.classList.remove('active');
    }
  });

  // --- UHC EVENT BINDINGS ---
  
  // Toggle border manual / density input
  uhcCalcDensity.addEventListener('change', (e) => {
    toggleDensityView(e.target.checked);
  });

  // Save config
  btnUhcSave.addEventListener('click', async () => {
    if (isUhcActive) {
      showNotification('El torneo UHC ya está activo. No se pueden guardar cambios.', 'danger');
      return;
    }

    const config = {
      graceTime: parseInt(uhcGraceTime.value, 10),
      finalHealTime: parseInt(uhcFinalHealTime.value, 10),
      worldSeed: uhcWorldSeed.value,
      startBorderSize: parseInt(uhcStartBorder.value, 10),
      playersPerBorder: parseInt(uhcPlayersBorder.value, 10),
      calcBorderByDensity: uhcCalcDensity.checked,
      eventName: uhcEventName.value,
      serverIp: uhcServerIp.value,
      scenarios: {
        cutClean: scenCutClean.checked,
        fireless: scenFireless.checked,
        noClean: scenNoClean.checked,
        doubleHealth: scenDoubleHealth.checked,
        timeBomb: scenTimeBomb.checked,
        vanillaPlus: scenVanillaPlus.checked,
        enchant17: scenEnchant17.checked
      },
      zone1Border: parseInt(uhcZ1Border.value, 10),
      zone1Time: parseInt(uhcZ1Time.value, 10),
      zone2Border: parseInt(uhcZ2Border.value, 10),
      zone2Time: parseInt(uhcZ2Time.value, 10),
      zone3Border: parseInt(uhcZ3Border.value, 10),
      zone3Time: parseInt(uhcZ3Time.value, 10)
    };

    try {
      const res = await fetch('/api/uhc/config', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(config)
      });

      const data = await res.json();
      if (res.ok && data.success) {
        showNotification('Configuración de UHC guardada y sincronizada.', 'success');
        printToConsole('[Sistema] Configuración UHC guardada en el backend.', 'system');
      } else {
        showNotification(data.error || 'No se pudo guardar la configuración.', 'danger');
      }
    } catch (err) {
      showNotification('Error de conexión al guardar parámetros del UHC.', 'danger');
    }
  });

  // Start match
  btnUhcStart.addEventListener('click', async () => {
    if (!isServerOnline) {
      showNotification('El servidor Minecraft está OFFLINE. No se puede iniciar el UHC.', 'danger');
      return;
    }
    if (!isRconConnected) {
      showNotification('La consola RCON está desconectada. Esperando reconexión...', 'danger');
      return;
    }

    // Save parameters first
    btnUhcSave.click();

    setTimeout(async () => {
      try {
        showNotification('Iniciando escenario y cuenta regresiva del UHC...', 'info');
        const res = await fetch('/api/uhc/start', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' }
        });

        const data = await res.json();
        if (res.ok && data.success) {
          showNotification('¡Torneo UHC iniciado exitosamente!', 'success');
          printToConsole('[UHC Engine] ¡El torneo UHC ha sido iniciado por el panel!', 'system');
          updateUhcStatus();
        } else {
          showNotification(data.error || 'Fallo al iniciar el UHC.', 'danger');
        }
      } catch (err) {
        showNotification('Fallo de red al iniciar el UHC.', 'danger');
      }
    }, 600);
  });

  // Stop match
  btnUhcStop.addEventListener('click', async () => {
    if (!confirm('¿Detener el UHC actual? El borde se restaurará a 60,000 bloques y se limpiarán los efectos.')) {
      return;
    }

    try {
      showNotification('Restaurando servidor y deteniendo UHC...', 'info');
      const res = await fetch('/api/uhc/stop', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      });

      const data = await res.json();
      if (res.ok && data.success) {
        showNotification('UHC detenido y escenario restaurado.', 'success');
        printToConsole('[UHC Engine] ¡El administrador ha detenido y cancelado el torneo!', 'system');
        updateUhcStatus();
      } else {
        showNotification(data.error || 'Error al detener el UHC.', 'danger');
      }
    } catch (err) {
      showNotification('Fallo de red al detener el UHC.', 'danger');
    }
  });

  // --- WORLD REGENERATION MODAL & ACTIONS ---
  btnRegenWorld.addEventListener('click', () => {
    if (isUhcActive) {
      showNotification('No se puede regenerar el mundo mientras hay un UHC activo.', 'danger');
      return;
    }
    if (!isServerOnline) {
      showNotification('El servidor de Minecraft está apagado.', 'danger');
      return;
    }
    if (!isRconConnected) {
      showNotification('La consola RCON está desconectada.', 'danger');
      return;
    }
    regenModal.classList.add('active');
  });

  modalRegenCancel.addEventListener('click', () => {
    regenModal.classList.remove('active');
  });

  modalRegenConfirm.addEventListener('click', async () => {
    regenModal.classList.remove('active');
    
    try {
      showNotification('Guardando parámetros de semilla...', 'info');
      // Save config first to sync inputs
      btnUhcSave.click();
      
      const seedVal = uhcWorldSeed.value;
      showNotification('Iniciando regeneración del mundo UHC...', 'info');
      printToConsole(`[World Regenerator] Solicitando regeneración con semilla: "${seedVal || 'Aleatoria'}"`, 'system');
      
      const res = await fetch('/api/world/regenerate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ seed: seedVal })
      });
      
      const data = await res.json();
      if (res.ok && data.success) {
        showNotification('Regeneración iniciada. Servidor deteniéndose...', 'success');
        printToConsole('[World Regenerator] Comando enviado con éxito. El servidor se está reiniciando y eliminará las carpetas del mapa.', 'system');
      } else {
        showNotification(data.error || 'No se pudo regenerar el mundo.', 'danger');
      }
    } catch (err) {
      showNotification('Error de red al regenerar el mundo.', 'danger');
    }
  });

  // Close modals when clicking outer backdrop
  regenModal.addEventListener('click', (e) => {
    if (e.target === regenModal) {
      regenModal.classList.remove('active');
    }
  });

  let localHistory = []; // Cache to store tournament history records locally

  async function loadTournamentHistory() {
    try {
      const res = await fetch('/api/uhc/history');
      if (!res.ok) throw new Error('History API unreachable');
      localHistory = await res.json();
      renderTournamentHistory(localHistory);
    } catch (err) {
      console.error('Error fetching tournament history:', err);
    }
  }

  function renderTournamentHistory(historyList) {
    if (!historyList || historyList.length === 0) {
      historyContainer.innerHTML = `
        <div class="no-history">
          <i class="fa-solid fa-calendar-minus"></i>
          <p>No hay torneos registrados aún.</p>
        </div>
      `;
      return;
    }

    let html = '';
    historyList.forEach((record, index) => {
      const dateFormatted = new Date(record.date).toLocaleString('es-ES', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });

      const isDraw = !record.winner || record.winner.includes('Ninguno') || record.winnerKills === 0;
      const avatarUrl = isDraw 
        ? 'https://mc-heads.net/avatar/MHF_Question/64'
        : `https://mc-heads.net/avatar/${record.winner}/64`;

      const recordId = `history-rec-${index}`;
      
      let placementsHtml = '';
      if (record.placements && record.placements.length > 0) {
        placementsHtml = record.placements.map(p => {
          let rClass = 'normal';
          let rBadge = p.rank;
          if (p.rank === 1) { rClass = 'gold'; rBadge = '<i class="fa-solid fa-crown"></i>'; }
          else if (p.rank === 2) { rClass = 'silver'; }
          else if (p.rank === 3) { rClass = 'bronze'; }

          return `
            <div class="placement-row rank-${rClass}">
              <span class="pl-rank">${rBadge}</span>
              <span class="pl-name">${p.name}</span>
              <span class="pl-kills font-mono">${p.kills} Kills</span>
            </div>
          `;
        }).join('');
      }

      html += `
        <div class="history-card glass-card">
          <div class="history-card-header" style="display: flex; justify-content: space-between; align-items: flex-start; gap: 8px;">
            <div class="history-event-info">
              <span class="history-event-name font-mono text-gold">${record.eventName}</span>
              <span class="history-date"><i class="fa-solid fa-calendar-days"></i> ${dateFormatted}</span>
            </div>
            <div style="display: flex; align-items: center; gap: 8px;">
              <span class="history-server-ip" style="margin-right: 4px;"><i class="fa-solid fa-network-wired"></i> ${record.serverIp}</span>
              <button class="btn-history-edit btn-sm btn-icon ripple" data-index="${index}" title="Editar Torneo" style="background: rgba(var(--color-accent-rgb), 0.1); border: 1px solid rgba(var(--color-accent-rgb), 0.2); border-radius: 4px; color: var(--color-accent); cursor: pointer; padding: 4px 6px; font-size: 11px; display: inline-flex; align-items: center; justify-content: center; height: 24px; width: 24px;">
                <i class="fa-solid fa-pen"></i>
              </button>
              <button class="btn-history-delete btn-sm btn-icon ripple" data-index="${index}" title="Eliminar Torneo" style="background: rgba(239, 68, 68, 0.1); border: 1px solid rgba(239, 68, 68, 0.2); border-radius: 4px; color: #f87171; cursor: pointer; padding: 4px 6px; font-size: 11px; display: inline-flex; align-items: center; justify-content: center; height: 24px; width: 24px;">
                <i class="fa-solid fa-trash"></i>
              </button>
            </div>
          </div>
          
          <div class="history-card-body">
            <div class="winner-profile">
              <img class="winner-avatar" src="${avatarUrl}" alt="${record.winner}" onerror="this.src='https://mc-heads.net/avatar/MHF_Steve/64'">
              <div class="winner-details">
                <span class="winner-label">${isDraw ? 'PARTIDA SIN GANADOR' : 'CAMPEÓN UHC'}</span>
                <span class="winner-name font-mono">${record.winner}</span>
                <span class="winner-kills text-gold"><i class="fa-solid fa-skull"></i> ${record.winnerKills} Kills</span>
              </div>
            </div>
            
            <div class="history-meta font-mono">
              <div class="meta-item">
                <span class="meta-label">DURACIÓN:</span>
                <span class="meta-value">${record.duration}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">SEMILLA:</span>
                <span class="meta-value">${record.worldSeed || 'Aleatoria'}</span>
              </div>
            </div>
          </div>

          <div class="history-collapsible">
            <button class="btn-collapse" onclick="document.getElementById('${recordId}').classList.toggle('expanded'); this.classList.toggle('active');">
               <span>Ver Clasificación Completa</span> <i class="fa-solid fa-chevron-down"></i>
            </button>
            <div class="collapse-content" id="${recordId}">
              <div class="placements-list">
                ${placementsHtml || '<div class="no-placements">No hay clasificación disponible.</div>'}
              </div>
            </div>
          </div>
        </div>
      `;
    });

    historyContainer.innerHTML = html;
  }

  // --- HISTORY EDITING CRUD LOGIC ---
  const historyModal = document.getElementById('history-modal');
  const btnAddHistory = document.getElementById('btn-add-history');
  const btnHistoryModalClose = document.getElementById('btn-history-modal-close');
  const btnHistoryModalCancel = document.getElementById('btn-history-modal-cancel');
  const historyForm = document.getElementById('history-form');
  const historyEditIndexInput = document.getElementById('history-edit-index');
  const histEventName = document.getElementById('hist-event-name');
  const histServerIp = document.getElementById('hist-server-ip');
  const histWinner = document.getElementById('hist-winner');
  const histWinnerKills = document.getElementById('hist-winner-kills');
  const histDuration = document.getElementById('hist-duration');
  const histSeed = document.getElementById('hist-seed');
  const histPlacements = document.getElementById('hist-placements');
  const historyModalTitle = document.getElementById('history-modal-title');

  // Open modal for adding a new record
  btnAddHistory.addEventListener('click', () => {
    historyForm.reset();
    historyEditIndexInput.value = "-1";
    historyModalTitle.innerText = "Registrar Torneo Manual";
    histServerIp.value = "play.rankit.net";
    histEventName.value = "UHC Evento #" + (localHistory.length + 1);
    histWinner.value = "";
    histWinnerKills.value = "0";
    histDuration.value = "45:00";
    histSeed.value = "";
    histPlacements.value = "";
    historyModal.classList.add('active');
  });

  // Close modal
  function closeHistoryModal() {
    historyModal.classList.remove('active');
  }
  btnHistoryModalClose.addEventListener('click', closeHistoryModal);
  btnHistoryModalCancel.addEventListener('click', closeHistoryModal);

  // Close when clicking overlay
  historyModal.addEventListener('click', (e) => {
    if (e.target === historyModal) {
      closeHistoryModal();
    }
  });

  // Edit / Delete button click handling via event delegation
  historyContainer.addEventListener('click', async (e) => {
    const editBtn = e.target.closest('.btn-history-edit');
    const deleteBtn = e.target.closest('.btn-history-delete');
    
    if (editBtn) {
      const index = parseInt(editBtn.dataset.index, 10);
      const record = localHistory[index];
      if (!record) return;

      historyForm.reset();
      historyEditIndexInput.value = index;
      historyModalTitle.innerText = "Editar Torneo - " + record.eventName;
      
      histEventName.value = record.eventName || "";
      histServerIp.value = record.serverIp || "play.rankit.net";
      histWinner.value = record.winner || "";
      histWinnerKills.value = record.winnerKills !== undefined ? record.winnerKills : 0;
      histDuration.value = record.duration || "";
      histSeed.value = record.worldSeed || "";

      // Format placements back into text
      if (record.placements && record.placements.length > 0) {
        histPlacements.value = record.placements.map(p => `${p.name}, ${p.kills}`).join('\n');
      } else {
        histPlacements.value = "";
      }

      historyModal.classList.add('active');
    } else if (deleteBtn) {
      const index = parseInt(deleteBtn.dataset.index, 10);
      const record = localHistory[index];
      if (!record) return;

      if (confirm(`¿Estás seguro de que deseas eliminar permanentemente el registro del torneo "${record.eventName}"?`)) {
        try {
          const res = await fetch(`/api/uhc/history/${index}`, { method: 'DELETE' });
          if (!res.ok) throw new Error('Error al eliminar registro');
          showNotification('Torneo eliminado exitosamente.', 'success');
          loadTournamentHistory();
        } catch (err) {
          showNotification('Error al eliminar torneo: ' + err.message, 'danger');
        }
      }
    }
  });

  // Form submission (Save / Add)
  historyForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const index = parseInt(historyEditIndexInput.value, 10);
    const eventName = histEventName.value.trim();
    const serverIp = histServerIp.value.trim();
    const winner = histWinner.value.trim();
    const winnerKills = parseInt(histWinnerKills.value, 10) || 0;
    const duration = histDuration.value.trim();
    const worldSeed = histSeed.value.trim();
    
    // Parse placements
    const lines = histPlacements.value.split('\n').map(l => l.trim()).filter(Boolean);
    const placements = lines.map((line, idx) => {
      const parts = line.split(',');
      const name = parts[0].trim();
      const kills = parts[1] ? parseInt(parts[1].trim(), 10) : 0;
      return {
        name,
        kills: isNaN(kills) ? 0 : kills,
        rank: idx + 1,
        status: (idx === 0) ? 'Ganador' : `Top ${idx + 1}`
      };
    });

    // Fallback if placements is empty or doesn't have the winner as first element
    if (placements.length === 0 && winner) {
      placements.push({
        name: winner,
        kills: winnerKills,
        rank: 1,
        status: 'Ganador'
      });
    }

    const payload = {
      winner,
      winnerKills,
      eventName,
      serverIp,
      duration,
      worldSeed,
      placements
    };

    try {
      let url = '/api/uhc/history';
      let method = 'POST';
      if (index >= 0) {
        url = `/api/uhc/history/${index}`;
        method = 'PUT';
      }

      const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (!res.ok) throw new Error('Error al guardar registro en la API');
      
      showNotification(index >= 0 ? 'Torneo actualizado exitosamente.' : 'Torneo registrado exitosamente.', 'success');
      closeHistoryModal();
      loadTournamentHistory();
    } catch (err) {
      showNotification('Error al guardar torneo: ' + err.message, 'danger');
    }
  });

  // --- PRACTICE ARENA & DUELS MODULE ---
  let isPracticeActive = false;
  let practicePlayersList = [];

  async function updatePracticeStatus() {
    try {
      const res = await fetch('/api/practice/status');
      if (!res.ok) throw new Error('API Practice status error');
      const data = await res.json();

      const badge = document.getElementById('practice-badge');
      const statusVal = document.getElementById('practice-status-val');
      const toggleBtn = document.getElementById('btn-practice-toggle');
      const playersCountText = document.getElementById('practice-players-count');

      isPracticeActive = data.active;
      practicePlayersList = data.players || [];

      // Update badge and status displays
      if (isPracticeActive) {
        badge.textContent = "Practice Activo";
        badge.style.background = "rgba(16, 185, 129, 0.2)";
        badge.style.color = "#34d399";
        badge.style.border = "1px solid rgba(16, 185, 129, 0.4)";

        statusVal.textContent = "ACTIVO";
        statusVal.style.color = "#34d399";

        toggleBtn.innerHTML = '<i class="fa-solid fa-power-off"></i> DESHABILITAR PRACTICE';
        toggleBtn.className = "btn btn-danger ripple";
        toggleBtn.style.background = "var(--color-danger)";
        toggleBtn.style.border = "none";
      } else {
        badge.textContent = "Practice Desactivado";
        badge.style.background = "rgba(239, 68, 68, 0.2)";
        badge.style.color = "#f87171";
        badge.style.border = "1px solid rgba(239, 68, 68, 0.4)";

        statusVal.textContent = "DESACTIVADO";
        statusVal.style.color = "#f87171";

        toggleBtn.innerHTML = '<i class="fa-solid fa-power-off"></i> HABILITAR PRACTICE';
        toggleBtn.className = "btn btn-success ripple";
        toggleBtn.style.background = "var(--color-success)";
        toggleBtn.style.border = "none";
      }

      playersCountText.textContent = `${practicePlayersList.length} jugadores`;

      // 2. Fetch general online players to populate duel selectors
      const statusRes = await fetch('/api/status');
      const statusData = await statusRes.json();
      const onlinePlayers = statusData.players || [];

      const p1Select = document.getElementById('duel-p1-select');
      const p2Select = document.getElementById('duel-p2-select');
      const currentP1 = p1Select.value;
      const currentP2 = p2Select.value;

      p1Select.innerHTML = '<option value="">-- Seleccionar --</option>';
      p2Select.innerHTML = '<option value="">-- Seleccionar --</option>';

      onlinePlayers.forEach(name => {
        const opt1 = document.createElement('option');
        opt1.value = name;
        opt1.textContent = name;
        if (name === currentP1) opt1.selected = true;
        p1Select.appendChild(opt1);

        const opt2 = document.createElement('option');
        opt2.value = name;
        opt2.textContent = name;
        if (name === currentP2) opt2.selected = true;
        p2Select.appendChild(opt2);
      });

      // 3. Render Players Table
      const playersBody = document.getElementById('practice-players-body');
      if (onlinePlayers.length === 0) {
        playersBody.innerHTML = `
          <tr>
            <td colspan="3" style="text-align: center; color: var(--text-muted); padding: 20px;">No hay jugadores online.</td>
          </tr>
        `;
      } else {
        let playersHtml = '';
        onlinePlayers.forEach(name => {
          const inPractice = practicePlayersList.includes(name);
          const location = inPractice ? '<span class="status-badge connected" style="background: rgba(56, 189, 248, 0.2); color: #38bdf8; border: 1px solid rgba(56, 189, 248, 0.4); padding: 4px 8px; border-radius: 4px; font-weight: 600;">Practice Arena</span>' : '<span class="status-badge" style="background: rgba(255,255,255,0.05); color: var(--text-muted); padding: 4px 8px; border-radius: 4px;">Lobby Spawn</span>';
          const actionButton = inPractice
            ? `<button class="btn btn-sm btn-danger btn-p-leave" data-player="${name}" style="padding: 6px 12px; font-size: 11px; border-radius: var(--radius-sm); border: none; background: var(--color-danger); color: #fff; cursor: pointer; font-weight:600;">Sacar</button>`
            : `<button class="btn btn-sm btn-success btn-p-join" data-player="${name}" style="padding: 6px 12px; font-size: 11px; border-radius: var(--radius-sm); border: none; background: var(--color-success); color: #fff; cursor: pointer; font-weight:600;">Mandar</button>`;

          playersHtml += `
            <tr>
              <td style="font-weight: 600; color: #fff; vertical-align: middle; padding: 12px 8px;">${name}</td>
              <td style="vertical-align: middle; padding: 12px 8px;">${location}</td>
              <td style="text-align: right; vertical-align: middle; padding: 12px 8px;">${actionButton}</td>
            </tr>
          `;
        });
        playersBody.innerHTML = playersHtml;
      }

      // 4. Render Practice Leaderboard
      const leaderboardBody = document.getElementById('practice-leaderboard-body');
      const killsMap = data.kills || {};
      const deathsMap = data.deaths || {};
      
      const statsPlayersSet = new Set([...Object.keys(killsMap), ...Object.keys(deathsMap)]);
      const statsList = Array.from(statsPlayersSet).map(name => {
        const pk = killsMap[name] || 0;
        const pd = deathsMap[name] || 0;
        const kd = pd === 0 ? pk : (pk / pd).toFixed(2);
        return { name, kills: pk, deaths: pd, kd };
      });

      statsList.sort((a, b) => b.kills - a.kills || b.kd - a.kd);

      if (statsList.length === 0) {
        leaderboardBody.innerHTML = `
          <tr>
            <td colspan="5" style="text-align: center; color: var(--text-muted); padding: 20px;">No hay peleas registradas aún.</td>
          </tr>
        `;
      } else {
        let leaderboardHtml = '';
        statsList.forEach((stat, idx) => {
          let rankLabel = idx + 1;
          if (idx === 0) rankLabel = '👑';
          leaderboardHtml += `
            <tr>
              <td class="col-rank font-mono" style="font-weight: 700; color: ${idx === 0 ? '#fbbf24' : 'var(--text-muted)'}; padding: 12px 8px; text-align: center; vertical-align: middle;">${rankLabel}</td>
              <td style="font-weight: 600; color: #fff; padding: 12px 8px; vertical-align: middle;">${stat.name}</td>
              <td style="text-align: right; font-weight: 700; color: #34d399; padding: 12px 8px; vertical-align: middle;">${stat.kills}</td>
              <td style="text-align: right; color: #f87171; padding: 12px 8px; vertical-align: middle;">${stat.deaths}</td>
              <td style="text-align: right; font-weight: 700; color: #38bdf8; padding: 12px 8px; vertical-align: middle;">${stat.kd}</td>
            </tr>
          `;
        });
        leaderboardBody.innerHTML = leaderboardHtml;
      }

    } catch (err) {
      console.error('Error in updatePracticeStatus:', err);
    }
  }

  // --- PRACTICE EVENT LISTENERS ---
  
  // Toggle Practice active/inactive
  document.getElementById('btn-practice-toggle').addEventListener('click', async () => {
    const nextState = !isPracticeActive;
    try {
      const res = await fetch('/api/practice/toggle', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ active: nextState })
      });
      if (!res.ok) throw new Error('Error al cambiar modo Practice');
      showNotification(nextState ? '¡Modo Practice activado en el servidor!' : 'Modo Practice desactivado.', 'success');
      updatePracticeStatus();
    } catch (err) {
      showNotification('Error al cambiar modo Practice: ' + err.message, 'danger');
    }
  });

  // Players list actions (Join / Leave) delegation
  document.getElementById('practice-players-body').addEventListener('click', async (e) => {
    const joinBtn = e.target.closest('.btn-p-join');
    const leaveBtn = e.target.closest('.btn-p-leave');
    
    if (joinBtn) {
      const name = joinBtn.dataset.player;
      try {
        const res = await fetch('/api/practice/join', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ player: name })
        });
        if (!res.ok) throw new Error('Error al enviar jugador a Practice');
        showNotification(`Mandando a ${name} a la arena de Practice.`, 'success');
        updatePracticeStatus();
      } catch (err) {
        showNotification('Error al mandar jugador: ' + err.message, 'danger');
      }
    } else if (leaveBtn) {
      const name = leaveBtn.dataset.player;
      try {
        const res = await fetch('/api/practice/leave', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ player: name })
        });
        if (!res.ok) throw new Error('Error al sacar jugador de Practice');
        showNotification(`Sacando a ${name} de Practice de regreso al Lobby.`, 'success');
        updatePracticeStatus();
      } catch (err) {
        showNotification('Error al sacar jugador: ' + err.message, 'danger');
      }
    }
  });

  // Launch Duel 1v1
  document.getElementById('btn-launch-duel').addEventListener('click', async () => {
    const p1 = document.getElementById('duel-p1-select').value;
    const p2 = document.getElementById('duel-p2-select').value;
    
    if (!p1 || !p2) {
      showNotification('Por favor, selecciona dos jugadores para el duelo.', 'warning');
      return;
    }
    if (p1 === p2) {
      showNotification('No puedes emparejar a un jugador contra sí mismo.', 'warning');
      return;
    }
    
    try {
      const res = await fetch('/api/practice/duel', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ p1, p2 })
      });
      if (!res.ok) throw new Error('Error al emparejar duelo en la API');
      showNotification(`¡Duelo iniciado exitosamente entre ${p1} y ${p2}!`, 'success');
      updatePracticeStatus();
    } catch (err) {
      showNotification('Error al iniciar duelo: ' + err.message, 'danger');
    }
  });

  // --- AUTOMATIC REFRESH POLLING ---
  // Poll immediately on mount
  updateStatus();
  updateStats();
  updateUhcStatus();
  updatePracticeStatus();
  loadTournamentHistory();

  // Schedule status check, stats fetch, and UHC poll loops every 3 seconds
  setInterval(updateStatus, 3000);
  setInterval(updateStats, 3000);
  setInterval(updateUhcStatus, 3000);
  setInterval(updatePracticeStatus, 3000);
  setInterval(loadTournamentHistory, 6000);

  showNotification('Dashboard de Minecraft UHC Inicializado.', 'success');
});
