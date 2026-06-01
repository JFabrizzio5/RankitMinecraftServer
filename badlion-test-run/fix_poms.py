import os
import re

def fix_dependency(match):
    dep_content = match.group(0)
    if 'artifactId>bukkit<' in dep_content or 'artifactId>votifier<' in dep_content or 'artifactId>craftbukkit<' in dep_content:
        # replace any version tag inside this dependency block with 1.8.8-R0.1-SNAPSHOT
        dep_content = re.sub(r'<version>[^<]+</version>', '<version>1.8.8-R0.1-SNAPSHOT</version>', dep_content)
    elif 'artifactId>cleanroomgenerator<' in dep_content:
        # replace any version tag inside this dependency block with 1.0.0
        dep_content = re.sub(r'<version>[^<]+</version>', '<version>1.0.0</version>', dep_content)
    elif 'artifactId>worldedit<' in dep_content:
        # replace any version tag inside this dependency block with 1.0.0
        dep_content = re.sub(r'<version>[^<]+</version>', '<version>1.0.0</version>', dep_content)
    return dep_content

def process_pom(filepath):
    with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
        content = f.read()
    
    # Replace dependency versions
    new_content = re.sub(r'<dependency>.*?</dependency>', fix_dependency, content, flags=re.DOTALL)
    
    if new_content != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Fixed {filepath}")

def main():
    root_dir = "/root/testingminecraft/badlion-test/Badlion.net"
    for dirpath, _, filenames in os.walk(root_dir):
        for filename in filenames:
            if filename == "pom.xml":
                process_pom(os.path.join(dirpath, filename))

if __name__ == "__main__":
    main()
