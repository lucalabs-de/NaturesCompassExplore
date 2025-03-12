package com.lucalabs.naturescompass.workers;

import com.lucalabs.naturescompass.NaturesCompass;
import com.lucalabs.naturescompass.config.NaturesCompassConfig;
import com.lucalabs.naturescompass.utils.BiomeUtils;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;

public class BiomeSearchWorker implements WorldWorkerManager.IWorker {

    private final int maxRadius;
    private final int sampleInterval;
    private final int maxSamples;

    private final ServerWorld world;
    private final Identifier biomeId;
    private final BlockPos startPos;
    private final int[] yValues;

    private Direction direction;
    private int samples;
    private int nextLength;
    private int x;
    private int z;
    private int length;
    private boolean finished;

    private final SuccessCallback successCallback;
    private final FailureCallback failureCallback;
    private long startTime;

    public BiomeSearchWorker(
            ServerWorld world,
            Biome biome,
            BlockPos startPos,
            SuccessCallback successCallback,
            FailureCallback failureCallback) {
        this(world, biome, startPos, 0, successCallback, failureCallback);
    }

    public BiomeSearchWorker(
            ServerWorld world,
            Biome biome,
            BlockPos startPos,
            int minRadius,
            SuccessCallback successCallback,
            FailureCallback failureCallback) {
        this.successCallback = successCallback;
        this.failureCallback = failureCallback;

        this.world = world;
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
            startTime = System.currentTimeMillis();
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

            for (int y : yValues) {
                if (BiomeUtils.isBiomeAtPositionEqual(world, biomeId, x, y, z)) {
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
        long duration = System.currentTimeMillis() - startTime;
        NaturesCompass.LOGGER.info("Search succeeded in {}ms: {} radius, {} samples", duration, getRadius(), samples);
        successCallback.onBiomeFound(x, z, samples);
        finished = true;
    }

    private void fail() {
        NaturesCompass.LOGGER.info("Search failed: {} radius, {} samples", getRadius(), samples);
        NaturesCompass.NATURES_COMPASS_ITEM.fail(stack, roundRadius(getRadius(), 500), samples);
        failureCallback.onFailure(roundRadius(getRadius(), 500), samples);
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

    @FunctionalInterface
    public interface SuccessCallback {
        void onBiomeFound(int x, int z, int samples);
    }

    @FunctionalInterface
    public interface FailureCallback {
        void onFailure(int radius, int samples);
    }
}
