#!/bin/bash
# Script para hacer el servidor compatible con 1.7.10 y 1.8.x

echo "=========================================================="
echo " CONFIGURANDO COMPATIBILIDAD 1.7.10 Y 1.8 PARA EL SERVIDOR "
echo "=========================================================="

PLUGINS_DIR="/root/testingminecraft/mc-data/plugins"

# Crear directorio si no existe
mkdir -p "$PLUGINS_DIR"

echo "[1/3] Descargando ViaVersion (5.9.1)..."
curl -L -o "$PLUGINS_DIR/ViaVersion-5.9.1.jar" "https://github.com/ViaVersion/ViaVersion/releases/download/v5.9.1/ViaVersion-5.9.1.jar"

echo "[2/3] Descargando ViaRewind (4.1.1)..."
curl -L -o "$PLUGINS_DIR/ViaRewind-4.1.1.jar" "https://github.com/ViaVersion/ViaRewind/releases/download/v4.1.1/ViaRewind-4.1.1.jar"

echo "[3/3] Reiniciando el servidor de Minecraft para cargar los plugins..."
docker compose restart mc-server

echo "=========================================================="
echo " ¡LISTO! El servidor de Minecraft ahora es compatible"
echo " con clientes de la versión 1.7.2 hasta la 1.8.x."
echo "=========================================================="
