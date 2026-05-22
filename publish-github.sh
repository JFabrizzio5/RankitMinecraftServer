#!/bin/bash
# Script automatizado para publicar el proyecto en GitHub como RankitMinecraftServer

echo "=========================================================="
echo "   PUBLICADOR DE REPOSITORIO GITHUB - RANKIT UHC SYSTEM"
echo "=========================================================="

# 1. Inicializar repositorio local si no existe
if [ ! -d ".git" ]; then
    echo "[Git] Inicializando repositorio Git local..."
    git init
    git branch -M main
fi

# 2. Agregar archivos
echo "[Git] Preparando archivos para commit..."
git add .

# 3. Hacer el commit inicial
echo "[Git] Creando commit inicial..."
git commit -m "Initial commit of Rankit UHC Tournament System with auto-version compatibility and lobby"

# 4. Verificar si GitHub CLI (gh) está instalado y autenticado
if command -v gh &> /dev/null; then
    echo "[GitHub] GitHub CLI encontrado. Verificando autenticación..."
    if gh auth status &> /dev/null; then
        echo "[GitHub] ¡Autenticado exitosamente en GitHub CLI!"
        echo "[GitHub] Creando el repositorio público 'RankitMinecraftServer'..."
        
        # Crear repo en GitHub y empujar el código
        if gh repo create RankitMinecraftServer --public --source=. --push; then
            echo "=========================================================="
            echo " ¡COMPLETADO! Tu repositorio se ha publicado en:"
            echo " https://github.com/$(gh api user --jq .login)/RankitMinecraftServer"
            echo "=========================================================="
            exit 0
        else
            echo "[Error] No se pudo crear el repositorio con GitHub CLI."
        fi
    else
        echo "[Advertencia] GitHub CLI no está autenticado."
    fi
fi

# 5. Método alternativo mediante token de acceso personal (PAT)
echo ""
echo "----------------------------------------------------------"
echo " CONFIGURACIÓN CON TOKEN DE ACCESO PERSONAL (PAT) DE GITHUB"
echo "----------------------------------------------------------"
echo "Si no estás autenticado en gh, por favor introduce tus datos:"
read -p "Ingresa tu usuario de GitHub: " username
read -sp "Ingresa tu Token de Acceso Personal (PAT) de GitHub: " token
echo ""

if [ -z "$username" ] || [ -z "$token" ]; then
    echo "[Error] El usuario o el token no pueden estar vacíos."
    exit 1
fi

# Crear repo vía API REST de GitHub usando curl
echo "[GitHub] Creando el repositorio vía API de GitHub..."
create_response=$(curl -H "Authorization: token $token" \
     -d '{"name":"RankitMinecraftServer","public":true,"description":"Minecraft UHC Tournament infrastructure with Live Dashboard and API control"}' \
     https://api.github.com/user/repos)

# Extraer URL del remoto
repo_url="https://$username:$token@github.com/$username/RankitMinecraftServer.git"

echo "[Git] Agregando origen remoto..."
git remote remove origin 2>/dev/null
git remote add origin "$repo_url"

echo "[Git] Empujando código a la rama main..."
if git push -u origin main; then
    echo "=========================================================="
    echo " ¡ÉXITO! Tu repositorio se ha subido exitosamente a:"
    echo " https://github.com/$username/RankitMinecraftServer"
    echo "=========================================================="
else
    echo "[Error] No se pudo subir el código. Verifica tu token y conexión."
fi
