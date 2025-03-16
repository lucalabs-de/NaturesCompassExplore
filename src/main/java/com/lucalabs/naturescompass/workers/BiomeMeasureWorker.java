package com.lucalabs.naturescompass.workers;

import com.lucalabs.naturescompass.NaturesCompass;
import com.lucalabs.naturescompass.utils.BiomeUtils;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class BiomeMeasureWorker implements WorldWorkerManager.IWorker {

    private final ServerWorld world;
    private final Identifier biomeId;
    private final int[] yValues;
    private final int sampleInterval;
    private final BlockPos origin;
    private final Direction[] directionOrder;

    private final TreeSet<GridSquare> outsideBiome;
    private final Map<Direction, GridSquare> current;

    private int currentDirection;
    private boolean finished;

    private final Callback callback;

    private long time;

    public BiomeMeasureWorker(ServerWorld world, BlockPos biome, Identifier biomeId, Callback callback) {
        this.callback = callback;
        this.finished = false;

        this.world = world;
        this.biomeId = biomeId;
        this.yValues = MathHelper.stream(biome.getY(), world.getBottomY() + 1, world.getTopY(), 64).toArray();
        this.sampleInterval = 16;

        this.origin = biome;

        this.outsideBiome = new TreeSet<>();
        this.current = new HashMap<>() {{
            put(Direction.NORTH, GridSquare.ORIGIN);
            put(Direction.EAST, GridSquare.ORIGIN);
            put(Direction.SOUTH, GridSquare.ORIGIN);
            put(Direction.WEST, GridSquare.ORIGIN);
        }};

        this.directionOrder = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
    }


    @Override
    public boolean doWork() {
        Direction d = getCurrentDirection();
        GridSquare pos = current.get(d);

        // TODO (when this is implemented, one should also reduce the min biome distance modifier)

        return false;
    }

    @Override
    public boolean hasWork() {
        return !finished;
    }

    public void start() {
        NaturesCompass.LOGGER.info("Measuring biome {}", biomeId);
        time = System.currentTimeMillis();
        WorldWorkerManager.addWorker(this);
    }

    public void stop() {
        finished = true;
    }

    private void succeed() {
        long duration = System.currentTimeMillis() - time;
        NaturesCompass.LOGGER.info("Measuring biome {} took {}ms", biomeId, duration);

        Vec3i north = current.get(Direction.NORTH).getCoordinates(origin, sampleInterval);
        Vec3i east = current.get(Direction.EAST).getCoordinates(origin, sampleInterval);
        Vec3i south = current.get(Direction.SOUTH).getCoordinates(origin, sampleInterval);
        Vec3i west = current.get(Direction.WEST).getCoordinates(origin, sampleInterval);

        NaturesCompass.LOGGER.info("Biome {} has bounding box of {}x{} blocks",
                biomeId,
                east.getX() - west.getX(),
                south.getZ() - north.getZ());

        callback.onBoundingRectangleComputed(
                new BiomeUtils.BoundingBox(
                        new BlockPos(west.getX(), origin.getY(), north.getZ()),
                        new BlockPos(east.getX(), origin.getY(), south.getZ())));
    }

    private Direction getCurrentDirection() {
       return directionOrder[currentDirection % directionOrder.length];
    }

    @FunctionalInterface
    public interface Callback {
        void onBoundingRectangleComputed(BiomeUtils.BoundingBox b);
    }

    private record GridSquare(int x, int z) implements Comparable<GridSquare> {
        static GridSquare ORIGIN = new GridSquare(0, 0);

        Vec3i getCoordinates(Vec3i relativeTo, int gridSize) {
            return new Vec3i(relativeTo.getX() + x * gridSize, relativeTo.getY(), relativeTo.getZ() + z * gridSize);
        }

        GridSquare getNeighbour(Direction direction) {
            return switch (direction) {
                case NORTH -> new GridSquare(x, z - 1); // this is correct, negative y is north
                case SOUTH -> new GridSquare(x, z + 1);
                case WEST -> new GridSquare(x - 1, z);
                case EAST -> new GridSquare(x + 1, z);
                default -> this;
            };
        }

        /// returns the diagonal neighbour obtained by moving in @p direction and rotating 45 degrees clockwise
        /// (or counter-clockwise if you assume NORTH to point down)
        GridSquare getDiagonalNeighbour(Direction direction) {
            return switch (direction) {
                case NORTH -> new GridSquare(x + 1, z - 1);
                case SOUTH -> new GridSquare(x - 1, z + 1);
                case WEST -> new GridSquare(x - 1, z - 1);
                case EAST -> new GridSquare(x + 1, z + 1);
                default -> this;
            };
        }

        @Override
        public int compareTo(@NotNull BiomeMeasureWorker.GridSquare other) {
            if (this.x == other.x) {
                return this.z - other.z;
            }

            return this.x - other.x;
        }
    }
}
