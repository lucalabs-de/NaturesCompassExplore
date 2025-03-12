package com.lucalabs.naturescompass.workers;

import com.lucalabs.naturescompass.NaturesCompass;
import com.lucalabs.naturescompass.config.NaturesCompassConfig;
import com.lucalabs.naturescompass.utils.BiomeUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;

public class BiomeSearchWorker implements WorldWorkerManager.IWorker {

    private final int maxRadius;
    private final int sampleInterval;
    private final int maxSamples;
    private final ServerWorld world;
    private final Identifier biomeId;
    private final ItemStack stack;
    private final BlockPos startPos;
    private Direction direction;
    private int samples;
    private int nextLength;

    private int x;
    private int z;
    private int[] yValues;
    private int length;
    private boolean finished;

    public BiomeSearchWorker(ServerWorld world, ItemStack stack, Biome biome, BlockPos startPos) {
        this(world, stack, biome, startPos, 0);
    }

    public BiomeSearchWorker(ServerWorld world, ItemStack stack, Biome biome, BlockPos startPos, int minRadius) {
        this.world = world;
        this.stack = stack;
        this.startPos = startPos;

        sampleInterval = NaturesCompassConfig.sampleIntervalModifier * BiomeUtils.getBiomeSize(world);
        maxSamples = NaturesCompassConfig.maxSamples;
        maxRadius = NaturesCompassConfig.radiusModifier * BiomeUtils.getBiomeSize(world);

        int minRadiusInSamples = minRadius / sampleInterval;

        x = startPos.getX() - minRadiusInSamples * sampleInterval;
        z = startPos.getZ() + minRadiusInSamples * sampleInterval;

        yValues = MathHelper.stream(startPos.getY(), world.getBottomY() + 1, world.getTopY(), 64).toArray();

        nextLength = (2 * minRadiusInSamples + 1) * sampleInterval;
        length = 0;
        direction = Direction.UP;

        finished = false;
        samples = 0;
        biomeId = BiomeUtils.getIdentifierForBiome(world, biome);
    }

    public void start() {
        if (maxRadius > 0 && sampleInterval > 0) {
            NaturesCompass.LOGGER.info("Starting search: {} sample space, {} max samples, {} max radius", sampleInterval, maxSamples, maxRadius);
            WorldWorkerManager.addWorker(this);
        } else {
            fail();
        }
    }

    @Override
    public boolean hasWork() {
        return !finished && getRadius() <= maxRadius && samples <= maxSamples;
    }

    @Override
    public boolean doWork() {
        if (hasWork()) {
            if (direction == Direction.NORTH) {
                z -= sampleInterval;
            } else if (direction == Direction.EAST) {
                x += sampleInterval;
            } else if (direction == Direction.SOUTH) {
                z += sampleInterval;
            } else if (direction == Direction.WEST) {
                x -= sampleInterval;
            }

            int sampleX = BiomeCoords.fromBlock(x);
            int sampleZ = BiomeCoords.fromBlock(z);

            for (int y : yValues) {
                int sampleY = BiomeCoords.fromBlock(y);
                final Biome biomeAtPos = world.getChunkManager().getChunkGenerator().getBiomeSource().getBiome(sampleX, sampleY, sampleZ, world.getChunkManager().getNoiseConfig().getMultiNoiseSampler()).value();
                final Identifier biomeAtPosID = BiomeUtils.getIdentifierForBiome(world, biomeAtPos);
                if (biomeAtPosID != null && biomeAtPosID.equals(biomeId)) {
                    succeed();
                }
            }

            samples++;
            length += sampleInterval;
            if (length >= nextLength) {
                if (direction != Direction.UP) {
                    nextLength += sampleInterval;
                    direction = direction.rotateYClockwise();
                } else {
                    direction = Direction.NORTH;
                }
                length = 0;
            }

        }

        if (hasWork()) {
            return true;
        } else if (!finished) {
            fail();
        }

        return false;
    }

    private void succeed() {
        NaturesCompass.LOGGER.info("Search succeeded: {} radius, {} samples", getRadius(), samples);
        NaturesCompass.NATURES_COMPASS_ITEM.foundBiome(stack, x, z, this.x, this.z, samples);
        finished = true;
    }

    private void fail() {
        NaturesCompass.LOGGER.info("Search failed: {} radius, {} samples", getRadius(), samples);
        NaturesCompass.NATURES_COMPASS_ITEM.fail(stack, roundRadius(getRadius(), 500), samples);
        finished = true;
    }

    public void stop() {
        NaturesCompass.LOGGER.info("Search stopped: {} radius, {} samples", getRadius(), samples);
        finished = true;
    }

    private int getRadius() {
        return BiomeUtils.getDistanceToBiome(startPos, x, z);
    }

    private int roundRadius(int radius, int roundTo) {
        return (radius / roundTo) * roundTo;
    }

}
