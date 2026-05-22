import urllib.request
import json
import os
import subprocess

def download_latest_release(repo_owner, repo_name, output_path):
    api_url = f"https://api.github.com/repos/{repo_owner}/{repo_name}/releases/latest"
    print(f"[API] Querying latest release for {repo_owner}/{repo_name}...")
    
    try:
        # Configure request with User-Agent to avoid API blocks
        req = urllib.request.Request(
            api_url, 
            headers={'User-Agent': 'Mozilla/5.0 (Minecraft UHC Controller)'}
        )
        
        with urllib.request.urlopen(req) as response:
            release_data = json.loads(response.read().decode('utf-8'))
            
        assets = release_data.get('assets', [])
        jar_asset = None
        
        # Find the asset that is a jar file
        for asset in assets:
            name = asset.get('name', '')
            if name.endswith('.jar') and not 'sources' in name.lower() and not 'javadoc' in name.lower():
                jar_asset = asset
                break
                
        if not jar_asset:
            raise Exception("No suitable .jar asset found in the latest release.")
            
        download_url = jar_asset.get('browser_download_url')
        asset_name = jar_asset.get('name')
        target_file = os.path.join(output_path, asset_name)
        
        print(f"[Download] Found asset: {asset_name}")
        print(f"[Download] URL: {download_url}")
        
        # Download the file
        download_req = urllib.request.Request(
            download_url,
            headers={'User-Agent': 'Mozilla/5.0'}
        )
        with urllib.request.urlopen(download_req) as dl_response:
            with open(target_file, 'wb') as f:
                f.write(dl_response.read())
                
        print(f"[Success] Downloaded {asset_name} successfully! (Size: {os.path.getsize(target_file)} bytes)")
        return target_file
        
    except Exception as e:
        print(f"[Error] Failed to process {repo_name}: {e}")
        return None

def main():
    plugins_dir = "/root/testingminecraft/mc-data/plugins"
    os.makedirs(plugins_dir, exist_ok=True)
    
    # Remove older/corrupted files if any
    for old_file in ["ViaVersion-5.9.1.jar", "ViaRewind-4.1.1.jar"]:
        old_path = os.path.join(plugins_dir, old_file)
        if os.path.exists(old_path):
            os.remove(old_path)
            print(f"[Clean] Removed older file: {old_file}")

    print("==========================================================")
    print(" INICIANDO DESCARGA REAL DE VIAVERSION, VIABACKWARDS, VIAREWIND & NOCHEATPLUS ")
    print("==========================================================")
    
    # 1. Download ViaVersion
    vv_file = download_latest_release("ViaVersion", "ViaVersion", plugins_dir)
    
    # 2. Download ViaBackwards (Required dependency for ViaRewind)
    vb_file = download_latest_release("ViaVersion", "ViaBackwards", plugins_dir)
    
    # 3. Download ViaRewind
    vr_file = download_latest_release("ViaVersion", "ViaRewind", plugins_dir)

    # 4. Download NoCheatPlus (Anti-Hack)
    ncp_file = download_latest_release("Updated-NoCheatPlus", "NoCheatPlus", plugins_dir)
    
    if vv_file and vb_file and vr_file and ncp_file:
        print("\n[5/5] Reiniciando el contenedor de Minecraft (mc-server) para cargar los plugins...")
        try:
            # Restart mc-server via docker compose
            subprocess.run(
                ["docker", "compose", "restart", "mc-server"],
                cwd="/root/testingminecraft",
                check=True
            )
            print("==========================================================")
            print(" ¡COMPLETADO! El servidor se ha reiniciado exitosamente.  ")
            print(" Ahora acepta clientes 1.7.10 y 1.8 con Anti-Cheat activo.")
            print("==========================================================")
        except Exception as e:
            print(f"[Error] No se pudo reiniciar el servidor de Minecraft: {e}")
    else:
        print("[Error] No se pudieron descargar todos los plugins (incluido NoCheatPlus). Revisa tu conexión de red.")

if __name__ == "__main__":
    main()
