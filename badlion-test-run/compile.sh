#!/bin/bash
# Script de compilación de plugins de Badlion.net
set -e

echo "========================================="
# Copiar spigot-api.jar y demás dependencias locales a la carpeta principal
echo "[1/7] Instalando dependencias locales en el repositorio Maven..."
cp "/usr/src/badlion/Server Jars & Plugins/spigot-api.jar" "/usr/src/badlion/spigot-api.jar"
cp "/usr/src/badlion/Server Jars & Plugins/Votifier.jar" "/usr/src/badlion/Votifier.jar"
cp "/usr/src/badlion/Server Jars & Plugins/CleanroomGenerator.jar" "/usr/src/badlion/CleanroomGenerator.jar"
cp "/usr/src/badlion/Server Jars & Plugins/spigot-server.jar" "/usr/src/badlion/spigot-server.jar"
cp "/usr/src/badlion/Server Jars & Plugins/WorldEdit.jar" "/usr/src/badlion/WorldEdit.jar"

# Instalar spigot-api.jar y Votifier.jar en la caché local de Maven como artefactos legítimos con versión específica
mvn install:install-file -Dfile="/usr/src/badlion/Server Jars & Plugins/spigot-api.jar" -DgroupId=org.bukkit -DartifactId=bukkit -Dversion=1.8.8-R0.1-SNAPSHOT -Dpackaging=jar
mvn install:install-file -Dfile="/usr/src/badlion/Server Jars & Plugins/Votifier.jar" -DgroupId=com.vexsoftware -DartifactId=votifier -Dversion=1.8.8-R0.1-SNAPSHOT -Dpackaging=jar
mvn install:install-file -Dfile="/usr/src/badlion/Server Jars & Plugins/CleanroomGenerator.jar" -DgroupId=io.nv.bukkit -DartifactId=cleanroomgenerator -Dversion=1.0.0 -Dpackaging=jar
mvn install:install-file -Dfile="/usr/src/badlion/Server Jars & Plugins/spigot-server.jar" -DgroupId=org.bukkit -DartifactId=craftbukkit -Dversion=1.8.8-R0.1-SNAPSHOT -Dpackaging=jar
mvn install:install-file -Dfile="/usr/src/badlion/Server Jars & Plugins/WorldEdit.jar" -DgroupId=com.sk89q -DartifactId=worldedit -Dversion=1.0.0 -Dpackaging=jar

# Eliminar scope system y systemPath de todos los pom.xml para convertirlos en dependencias normales
echo "Corrigiendo dependencias en todos los pom.xml..."
find /usr/src/badlion -name "pom.xml" -exec sed -i '/<scope>system<\/scope>/d' {} \;
find /usr/src/badlion -name "pom.xml" -exec sed -i '/<systemPath>/d' {} \;

# Compilar common
echo "[2/7] Compilando net.badlion.common:common..."
cd /usr/src/badlion/common
mvn clean install -DskipTests -o

# Compilar gberry
echo "[3/7] Compilando net.badlion.gberry:gberry..."
cd /usr/src/badlion/gberry
mvn clean install -DskipTests -o

# Compilar gpermissions
echo "[4/7] Compilando net.badlion.gpermissions:gpermissions..."
cd /usr/src/badlion/gpermissions
mvn clean install -DskipTests -o

# Compilar banmanager
echo "[5/7] Compilando net.badlion.banmanager:banmanager..."
cd /usr/src/badlion/banmanager
mvn clean install -DskipTests -o

# Compilar smellychat
echo "[6/7] Compilando net.badlion.smellychat:smellychat..."
cd /usr/src/badlion/smellychat
mvn clean install -DskipTests -o

# Compilar gguard
echo "[6.05/7] Compilando net.badlion.gguard:gguard..."
cd /usr/src/badlion/gguard
mvn clean install -DskipTests -o

# Compilar smellycases
echo "[6.1/7] Compilando net.badlion.smellycases:smellycases..."
cd /usr/src/badlion/smellycases
mvn clean install -DskipTests -o

# Compilar cosmetics
echo "[6.2/7] Compilando net.badlion.cosmetics:cosmetics..."
cd /usr/src/badlion/cosmetics
mvn clean install -DskipTests -o

# Compilar smellylobby
echo "[6.3/7] Compilando net.badlion.smellylobby:smellylobby..."
cd /usr/src/badlion/smellylobby
mvn clean install -DskipTests -o

# Compilar statemachine (dependencia de potpvp)
echo "[6.4/7] Compilando net.badlion.statemachine:statemachine..."
cd /usr/src/badlion/statemachine
mvn clean install -DskipTests -o

# Compilar gcheat (dependencia de potpvp)
echo "[6.5/7] Compilando net.badlion.gcheat:gcheat..."
cd /usr/src/badlion/gcheat
mvn clean install -DskipTests -o

# Compilar enderpearlcd (dependencia de potpvp)
echo "[6.6/7] Compilando net.badlion.enderpearlcd:enderpearlcd..."
cd /usr/src/badlion/enderpearlcd
mvn clean install -DskipTests -o

# Compilar cmdsigns (dependencia de potpvp)
echo "[6.7/7] Compilando net.badlion.cmdsigns:cmdsigns..."
cd /usr/src/badlion/cmdsigns
mvn clean install -DskipTests -o

# Compilar colors (dependencia de potpvp)
echo "[6.8/7] Compilando net.badlion.colors:colors..."
cd /usr/src/badlion/colors
mvn clean install -DskipTests -o

# Compilar smellymapvotes (dependencia de potpvp)
echo "[6.9/7] Compilando net.badlion.smellymapvotes:smellymapvotes..."
cd /usr/src/badlion/smellymapvotes
mvn clean install -DskipTests -o

# Compilar potpvp (Practice principal)
echo "[6.95/7] Compilando net.badlion.potpvp:potpvp..."
cd /usr/src/badlion/potpvp
mvn clean install -DskipTests -o

# Compilar worldborder
echo "[6.96/7] Compilando net.badlion.worldborder:worldborder..."
cd /usr/src/badlion/worldborder
mvn clean install -DskipTests -o

# Compilar worldrotator
echo "[6.97/7] Compilando net.badlion.worldrotator:worldrotator..."
cd /usr/src/badlion/worldrotator
mvn clean install -DskipTests -o

# Compilar ministats
echo "[6.98/7] Compilando net.badlion.ministats:ministats..."
cd /usr/src/badlion/ministats
mvn clean install -DskipTests -o

# Compilar combattag
echo "[6.99/7] Compilando net.badlion.combattag:combattag..."
cd /usr/src/badlion/combattag
mvn clean install -DskipTests -o

# Compilar disguise
echo "[7.0/7] Compilando net.badlion.disguise:disguise..."
cd /usr/src/badlion/disguise
mvn clean install -DskipTests -o

# Compilar arenacommon
echo "[7.05/7] Compilando net.badlion.arenacommon:arenacommon..."
cd /usr/src/badlion/arenacommon
mvn clean install -DskipTests -o

# Compilar mpg
echo "[7.1/7] Compilando net.badlion.mpg:mpg..."
cd /usr/src/badlion/mpg
mvn clean install -DskipTests -o

# Compilar mpglobby
echo "[7.15/7] Compilando net.badlion.mpglobby:mpglobby..."
cd /usr/src/badlion/mpglobby
mvn clean install -DskipTests -o

# Compilar mpgchat
echo "[7.2/7] Compilando net.badlion.mpgchat:mpgchat..."
cd /usr/src/badlion/mpgchat
mvn clean install -DskipTests -o

# Compilar uhc
echo "[7.25/7] Compilando net.badlion.uhc:uhc..."
cd /usr/src/badlion/uhc
mvn clean install -DskipTests -o

# Compilar uhcchat
echo "[7.3/7] Compilando net.badlion.uhcchat:uhcchat..."
cd /usr/src/badlion/uhcchat
mvn clean install -DskipTests -o

# Compilar uhcworldgenerator
echo "[7.35/7] Compilando net.badlion.uhcworldgenerator:uhcworldgenerator..."
cd /usr/src/badlion/uhcworldgenerator
mvn clean install -DskipTests -o

# Compilar uhcmeetup
echo "[7.4/7] Compilando net.badlion.uhcmeetup:uhcmeetup..."
cd /usr/src/badlion/uhcmeetup
mvn clean install -DskipTests -o

echo "[7.5/7] Copiando jarras compiladas al directorio de salida..."
mkdir -p /output
cp /usr/src/badlion/gberry/target/badliongberry.jar /output/
cp /usr/src/badlion/gpermissions/target/badliongpermissions.jar /output/
cp /usr/src/badlion/banmanager/target/badlionbanmanager.jar /output/
cp /usr/src/badlion/smellychat/target/badlionsmellychat.jar /output/
cp /usr/src/badlion/gguard/target/badliongguard.jar /output/
cp /usr/src/badlion/smellycases/target/badlionsmellycases.jar /output/
cp /usr/src/badlion/cosmetics/target/badlioncosmetics.jar /output/
cp /usr/src/badlion/smellylobby/target/badlionsmellylobby.jar /output/

# Copiar plugins de Practice y dependencias
find /usr/src/badlion/statemachine/target/ -maxdepth 1 -name "*.jar" ! -name "original-*" ! -name "dependency-reduced-*" -exec cp {} /output/statemachine.jar \; 2>/dev/null || true
find /usr/src/badlion/gcheat/target/ -maxdepth 1 -name "*.jar" ! -name "original-*" ! -name "dependency-reduced-*" -exec cp {} /output/gcheat.jar \; 2>/dev/null || true
find /usr/src/badlion/enderpearlcd/target/ -maxdepth 1 -name "*.jar" ! -name "original-*" ! -name "dependency-reduced-*" -exec cp {} /output/enderpearlcd.jar \; 2>/dev/null || true
find /usr/src/badlion/cmdsigns/target/ -maxdepth 1 -name "*.jar" ! -name "original-*" ! -name "dependency-reduced-*" -exec cp {} /output/badlioncmdsigns.jar \; 2>/dev/null || true
find /usr/src/badlion/colors/target/ -maxdepth 1 -name "*.jar" ! -name "original-*" ! -name "dependency-reduced-*" -exec cp {} /output/badlioncolors.jar \; 2>/dev/null || true
find /usr/src/badlion/smellymapvotes/target/ -maxdepth 1 -name "*.jar" ! -name "original-*" ! -name "dependency-reduced-*" -exec cp {} /output/badlionsmellymapvotes.jar \; 2>/dev/null || true
find /usr/src/badlion/potpvp/target/ -maxdepth 1 -name "*.jar" ! -name "original-*" ! -name "dependency-reduced-*" -exec cp {} /output/badlionpotpvp.jar \; 2>/dev/null || true

# Copiar nuevos plugins de UHC y MPG
find /usr/src/badlion/worldborder/target/ -maxdepth 1 -name "*.jar" ! -name "original-*" ! -name "dependency-reduced-*" -exec cp {} /output/badlionworldborder.jar \; 2>/dev/null || true
find /usr/src/badlion/worldrotator/target/ -maxdepth 1 -name "*.jar" ! -name "original-*" ! -name "dependency-reduced-*" -exec cp {} /output/badlionworldrotator.jar \; 2>/dev/null || true
find /usr/src/badlion/ministats/target/ -maxdepth 1 -name "*.jar" ! -name "original-*" ! -name "dependency-reduced-*" -exec cp {} /output/badlionministats.jar \; 2>/dev/null || true
find /usr/src/badlion/combattag/target/ -maxdepth 1 -name "*.jar" ! -name "original-*" ! -name "dependency-reduced-*" -exec cp {} /output/badlioncombattag.jar \; 2>/dev/null || true
find /usr/src/badlion/disguise/target/ -maxdepth 1 -name "*.jar" ! -name "original-*" ! -name "dependency-reduced-*" -exec cp {} /output/badliondisguise.jar \; 2>/dev/null || true
find /usr/src/badlion/arenacommon/target/ -maxdepth 1 -name "*.jar" ! -name "original-*" ! -name "dependency-reduced-*" -exec cp {} /output/badlionarenacommon.jar \; 2>/dev/null || true
find /usr/src/badlion/mpg/target/ -maxdepth 1 -name "*.jar" ! -name "original-*" ! -name "dependency-reduced-*" -exec cp {} /output/badlionmpg.jar \; 2>/dev/null || true
find /usr/src/badlion/mpglobby/target/ -maxdepth 1 -name "*.jar" ! -name "original-*" ! -name "dependency-reduced-*" -exec cp {} /output/badlionmpglobby.jar \; 2>/dev/null || true
find /usr/src/badlion/mpgchat/target/ -maxdepth 1 -name "*.jar" ! -name "original-*" ! -name "dependency-reduced-*" -exec cp {} /output/badlionmpgchat.jar \; 2>/dev/null || true
find /usr/src/badlion/uhc/target/ -maxdepth 1 -name "*.jar" ! -name "original-*" ! -name "dependency-reduced-*" -exec cp {} /output/badlionuhc.jar \; 2>/dev/null || true
find /usr/src/badlion/uhcchat/target/ -maxdepth 1 -name "*.jar" ! -name "original-*" ! -name "dependency-reduced-*" -exec cp {} /output/badlionuhcchat.jar \; 2>/dev/null || true
find /usr/src/badlion/uhcworldgenerator/target/ -maxdepth 1 -name "*.jar" ! -name "original-*" ! -name "dependency-reduced-*" -exec cp {} /output/badlionuhcworldgenerator.jar \; 2>/dev/null || true
find /usr/src/badlion/uhcmeetup/target/ -maxdepth 1 -name "*.jar" ! -name "original-*" ! -name "dependency-reduced-*" -exec cp {} /output/badlionuhcmeetup.jar \; 2>/dev/null || true

echo "========================================="
echo "¡COMPILACIÓN DE BADLION COMPLETADA CON ÉXITO!"
echo "========================================="



