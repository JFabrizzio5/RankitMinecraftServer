package com.antigravity.uhc.generator;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import java.util.Random;

public class VoidGenerator extends ChunkGenerator {
    @Override
    public byte[] generate(World world, Random random, int x, int z) {
        return new byte[32768];
    }
    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return new Location(world, 0.5, 101.5, 0.5);
    }
}
