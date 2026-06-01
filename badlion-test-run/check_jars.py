import zipfile
import os

def check_jar(path):
    print(f"Checking jar: {path}")
    if not os.path.exists(path):
        print("File does not exist")
        return
    with zipfile.ZipFile(path, 'r') as jar:
        names = jar.namelist()
        for name in names:
            if "TippedArrow" in name or "PotionData" in name:
                print(f"  Found class: {name}")

check_jar("/usr/src/badlion/spigot-api.jar")
check_jar("/usr/src/badlion/spigot-server.jar")
check_jar("/root/testingminecraft/badlion-test/Badlion.net/Server Jars & Plugins/spigot-api.jar")
check_jar("/root/testingminecraft/badlion-test/Badlion.net/Server Jars & Plugins/spigot-server.jar")
