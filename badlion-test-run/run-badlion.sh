#!/bin/bash
# Orquestador del despliegue del Servidor Badlion.net de prueba
set -e

BASE_DIR="/root/testingminecraft/badlion-test-run"
SOURCE_DIR="/root/testingminecraft/badlion-test"

echo "=========================================================="
echo "    INICIANDO DESPLIEGUE DEL SERVIDOR BADLION.NET DE PRUEBA"
echo "=========================================================="

# 1. Compilar plugins núcleo en contenedor de Maven
echo "[1/6] Compilando plugins núcleo en contenedor Maven con las correcciones..."
mkdir -p "$BASE_DIR/plugins"
mkdir -p "$BASE_DIR/.m2"
docker run --rm \
  -v "$SOURCE_DIR/Badlion.net:/usr/src/badlion" \
  -v "$BASE_DIR/compile.sh:/compile.sh" \
  -v "$BASE_DIR/plugins:/output" \
  -v "$BASE_DIR/.m2:/root/.m2" \
  maven:3.6.3-jdk-8 bash /compile.sh

# 2. Preparar directorios de Minecraft
echo "[2/6] Inicializando directorios de mc-data..."
mkdir -p "$BASE_DIR/mc-data"
mkdir -p "$BASE_DIR/mc-data/plugins"
mkdir -p "$BASE_DIR/mc-data/plugins/Gberry"
mkdir -p "$BASE_DIR/mc-data/plugins/BanManager"
mkdir -p "$BASE_DIR/mc-data/plugins/GPermissions"

# Copiar Spigot y plugins iniciales provistos por Badlion
echo "Copiando jarra de servidor y plugins precompilados..."
cp "$SOURCE_DIR/Badlion.net/Server Jars & Plugins/spigot-server.jar" "$BASE_DIR/mc-data/spigot-server.jar"
cp "$SOURCE_DIR/Badlion.net/Server Jars & Plugins/CleanroomGenerator.jar" "$BASE_DIR/mc-data/plugins/"
cp "$SOURCE_DIR/Badlion.net/Server Jars & Plugins/WorldEdit.jar" "$BASE_DIR/mc-data/plugins/"
cp "$SOURCE_DIR/Badlion.net/Server Jars & Plugins/Factions.jar" "$BASE_DIR/mc-data/plugins/"
cp "$SOURCE_DIR/Badlion.net/Server Jars & Plugins/Buycraft.jar" "$BASE_DIR/mc-data/plugins/"
cp "$SOURCE_DIR/Badlion.net/Server Jars & Plugins/Votifier.jar" "$BASE_DIR/mc-data/plugins/"
cp "$SOURCE_DIR/Badlion.net/Server Jars & Plugins/flow-nbt-1.0.1-SNAPSHOT.jar" "$BASE_DIR/mc-data/plugins/"
cp "$SOURCE_DIR/Badlion.net/Server Jars & Plugins/slf4j-api-1.7.6.jar" "$BASE_DIR/mc-data/plugins/"

# Copiar plugins que acabamos de compilar
echo "Instalando plugins Badlion núcleo compilados..."
cp "$BASE_DIR/plugins/badliongberry.jar" "$BASE_DIR/mc-data/plugins/"
cp "$BASE_DIR/plugins/badliongpermissions.jar" "$BASE_DIR/mc-data/plugins/"
cp "$BASE_DIR/plugins/badlionbanmanager.jar" "$BASE_DIR/mc-data/plugins/"
cp "$BASE_DIR/plugins/badlionsmellychat.jar" "$BASE_DIR/mc-data/plugins/"
cp "$BASE_DIR/plugins/badliongguard.jar" "$BASE_DIR/mc-data/plugins/"
cp "$BASE_DIR/plugins/badlionsmellycases.jar" "$BASE_DIR/mc-data/plugins/"
cp "$BASE_DIR/plugins/badlioncosmetics.jar" "$BASE_DIR/mc-data/plugins/"
cp "$BASE_DIR/plugins/badlionsmellylobby.jar" "$BASE_DIR/mc-data/plugins/"

# Copiar nuevos plugins de Practice y dependencias
cp "$BASE_DIR/plugins/statemachine.jar" "$BASE_DIR/mc-data/plugins/"
cp "$BASE_DIR/plugins/gcheat.jar" "$BASE_DIR/mc-data/plugins/"
cp "$BASE_DIR/plugins/enderpearlcd.jar" "$BASE_DIR/mc-data/plugins/"
cp "$BASE_DIR/plugins/badlioncmdsigns.jar" "$BASE_DIR/mc-data/plugins/"
cp "$BASE_DIR/plugins/badlioncolors.jar" "$BASE_DIR/mc-data/plugins/"
cp "$BASE_DIR/plugins/badlionsmellymapvotes.jar" "$BASE_DIR/mc-data/plugins/"
cp "$BASE_DIR/plugins/badlionpotpvp.jar" "$BASE_DIR/mc-data/plugins/"

# Copiar nuevos plugins de UHC, MPG y dependencias
cp "$BASE_DIR/plugins/badlionworldborder.jar" "$BASE_DIR/mc-data/plugins/"
cp "$BASE_DIR/plugins/badlionworldrotator.jar" "$BASE_DIR/mc-data/plugins/"
cp "$BASE_DIR/plugins/badlionministats.jar" "$BASE_DIR/mc-data/plugins/"
cp "$BASE_DIR/plugins/badlioncombattag.jar" "$BASE_DIR/mc-data/plugins/"
cp "$BASE_DIR/plugins/badliondisguise.jar" "$BASE_DIR/mc-data/plugins/"
cp "$BASE_DIR/plugins/badlionarenacommon.jar" "$BASE_DIR/mc-data/plugins/"
cp "$BASE_DIR/plugins/badlionmpg.jar" "$BASE_DIR/mc-data/plugins/"
cp "$BASE_DIR/plugins/badlionmpglobby.jar" "$BASE_DIR/mc-data/plugins/"
cp "$BASE_DIR/plugins/badlionmpgchat.jar" "$BASE_DIR/mc-data/plugins/"
cp "$BASE_DIR/plugins/badlionuhc.jar" "$BASE_DIR/mc-data/plugins/"
cp "$BASE_DIR/plugins/badlionuhcchat.jar" "$BASE_DIR/mc-data/plugins/"
cp "$BASE_DIR/plugins/badlionuhcworldgenerator.jar" "$BASE_DIR/mc-data/plugins/"
cp "$BASE_DIR/plugins/badlionuhcmeetup.jar" "$BASE_DIR/mc-data/plugins/"



# 3. Crear archivos de configuración de Minecraft
echo "[3/6] Escribiendo configuraciones del servidor de Minecraft..."
# Eula
echo "eula=true" > "$BASE_DIR/mc-data/eula.txt"

# server.properties
cat <<EOT > "$BASE_DIR/mc-data/server.properties"
spawn-protection=0
query.port=25565
pvp=true
difficulty=1
enable-query=false
port=25565
online-mode=false
allow-flight=true
view-distance=10
server-ip=10.0.0.5
motd=Badlion.net Test Server
EOT

# bukkit.yml (con credenciales de base de datos leídas por Gberry)
cat <<EOT > "$BASE_DIR/mc-data/bukkit.yml"
settings:
  allow-end: true
  warn-on-overload: true
  permissions-file: permissions.yml
  update-folder: update
  ping-packet-limit: 100
  use-map-color-cache: true
db_user: postgres
db_pass: postgres_pass
db_db: badlion
worlds:
  world:
    generator: CleanroomGenerator:.
EOT

# Copiar configuraciones personalizadas de plugins
cp "$BASE_DIR/configs/gberry-config.yml" "$BASE_DIR/mc-data/plugins/Gberry/config.yml"
cp "$BASE_DIR/configs/banmanager-config.yml" "$BASE_DIR/mc-data/plugins/BanManager/config.yml"
cp "$BASE_DIR/configs/gpermissions-config.yml" "$BASE_DIR/mc-data/plugins/GPermissions/config.yml"

mkdir -p "$BASE_DIR/mc-data/plugins/CmdSigns"
cp "$BASE_DIR/configs/cmdsigns-config.yml" "$BASE_DIR/mc-data/plugins/CmdSigns/config.yml"

mkdir -p "$BASE_DIR/mc-data/plugins/WorldRotator"
cp "$BASE_DIR/configs/worldrotator-config.yml" "$BASE_DIR/mc-data/plugins/WorldRotator/config.yml"

mkdir -p "$BASE_DIR/mc-data/plugins/ColorName"
cp "$BASE_DIR/configs/colors-config.yml" "$BASE_DIR/mc-data/plugins/ColorName/config.yml"


# 4. Levantar la pila de Docker
echo "[4/6] Limpiando mapa viejo para forzar regeneración del vacío y levantando contenedores..."
rm -rf "$BASE_DIR/mc-data/world"
cd "$BASE_DIR"
docker compose down
docker compose up -d

# 5. Inicializar CouchDB
echo "[5/6] Ejecutando script de inicialización de bases de datos..."
chmod +x "$BASE_DIR/init-db.sh"
"$BASE_DIR/init-db.sh"

# 6. Mostrar logs en tiempo real
echo "[6/6] Despliegue completado. Mostrando logs en tiempo real del Minecraft server..."
echo "Usa Ctrl+C para salir de los logs sin apagar el servidor."
docker compose logs -f badlion-mc
