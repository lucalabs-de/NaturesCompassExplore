package com.lucalabs.naturescompass.utils;

import com.lucalabs.naturescompass.config.NaturesCompassConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;

import java.util.Optional;

public abstract class BiomeUtils {

    public static Registry<Biome> getBiomeRegistry(World world) {
        return world.getRegistryManager().get(RegistryKeys.BIOME);
    }

    public static Identifier getIdentifierForBiome(World world, Biome biome) {
        return getBiomeRegistry(world).getId(biome);
    }

    public static Optional<Biome> getBiomeForIdentifier(World world, Identifier id) {
        return getBiomeRegistry(world).getOrEmpty(id);
    }


    @Environment(EnvType.SERVER)
    public static boolean isBiomeAtPositionEqual(ServerWorld world, Identifier biomeId, Vec3i pos) {
        Identifier id = getIdentifierForBiome(world, getBiomeAtPosition(world, pos));
        return biomeId.equals(id);
    }

    @Environment(EnvType.SERVER)
    public static boolean isBiomeAtPositionEqual(ServerWorld world, Identifier biomeId, int x, int y, int z) {
        Identifier id = getIdentifierForBiome(world, getBiomeAtPosition(world, x, y, z));
        return biomeId.equals(id);
    }

    @Environment(EnvType.SERVER)
    public static Biome getBiomeAtPosition(ServerWorld world, Vec3i pos) {
        return getBiomeAtPosition(world, pos.getX(), pos.getY(), pos.getZ());
    }

    @Environment(EnvType.SERVER)
    public static Biome getBiomeAtPosition(ServerWorld world, int x, int y, int z) {
        int biomeX = BiomeCoords.fromBlock(x);
        int biomeY = BiomeCoords.fromBlock(y);
        int biomeZ = BiomeCoords.fromBlock(z);

        return world.getChunkManager().getChunkGenerator().getBiomeSource().getBiome(x, y, z, world.getChunkManager().getNoiseConfig().getMultiNoiseSampler()).value();
    }

    public static int getBiomeSize(World world) {
        // TODO
        return 4;
    }

    public static int getDistanceToBiome(BlockPos startPos, int biomeX, int biomeZ) {
        return (int) MathHelper.sqrt((float) startPos.getSquaredDistance(new BlockPos(biomeX, startPos.getY(), biomeZ)));
    }

    @Environment(EnvType.CLIENT)
    public static String getBiomeNameForDisplay(World world, Biome biome) {

        if (biome != null) {
            if (NaturesCompassConfig.fixBiomeNames) {
                final String original = getBiomeName(world, biome);
                StringBuilder fixed = new StringBuilder();
                char pre = ' ';
                for (int i = 0; i < original.length(); i++) {
                    final char c = original.charAt(i);
                    if (Character.isUpperCase(c) && Character.isLowerCase(pre) && Character.isAlphabetic(pre)) {
                        fixed.append(" ");
                    }
                    fixed.append(c);
                    pre = c;
                }

                return fixed.toString();
            }

            if (getIdentifierForBiome(world, biome) != null) {
                return I18n.translate(getIdentifierForBiome(world, biome).toString());
            }
        }

        return "";
    }

    @Environment(EnvType.CLIENT)
    public static String getBiomeName(World world, Biome biome) {
        return I18n.translate(Util.createTranslationKey("biome", getIdentifierForBiome(world, biome)));
    }

    public record BoundingBox(BlockPos nw, BlockPos se) {
       int getMaxDistanceFrom(BlockPos reference) {
           // TODO
           return 0;
       }
    }
}