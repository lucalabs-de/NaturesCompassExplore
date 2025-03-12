package com.lucalabs.naturescompass.workers;

import com.lucalabs.naturescompass.NaturesCompass;
import com.lucalabs.naturescompass.utils.BiomeUtils;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;

import java.util.Stack;
import java.util.TreeSet;

public class BiomeMeasureWorker implements WorldWorkerManager.IWorker {

    private final ServerWorld world;
    private final Identifier biomeId;
    private final int[] yValues;
    private final int sampleInterval;

    private final BlockPos origin;

    private int maxX;
    private int maxZ;
    private int minX;
    private int minZ;
    private boolean finished;

    private GridSquare current;
    private TreeSet<GridSquare> visited;
    private TreeSet<GridSquare> outsideBiome;
    private Stack<GridSquare> steps;

    private final Callback callback;
    private long time;

    public BiomeMeasureWorker(ServerWorld world, BlockPos biome, Callback callback) {
        this.callback = callback;

        this.world = world;
        this.biomeId = BiomeUtils.getIdentifierForBiome(world, BiomeUtils.getBiomeAtPosition(world, biome));
        this.yValues = MathHelper.stream(biome.getY(), world.getBottomY() + 1, world.getTopY(), 64).toArray();
        this.sampleInterval = 16;

        this.origin = biome;

        this.finished = false;
        this.maxX = this.origin.getX();
        this.maxZ = this.origin.getZ();
        this.minX = this.origin.getX();
        this.minZ = this.origin.getZ();

        this.current = new GridSquare(0, 0);
    }

    @Override
    public boolean doWork() {
        steps.push(current);

        byte crossBiomePattern = getCrossBiomePatternAt(current);

        // corners are the only candidates that can change the bounding box
        if (isCorner(crossBiomePattern)) {
            Vec3i coords = current.getCoordinates(origin, sampleInterval);

            if (coords.getX() > this.maxX) {
                this.maxX = coords.getX();
            } else if (coords.getX() < this.minX) {
                this.minX = coords.getX();
            }

            if (coords.getZ() > this.maxZ) {
                this.maxZ = coords.getZ();
            } else if (coords.getZ() < this.minZ) {
                this.minZ = coords.getZ();
            }
        }

        if (!isOnBorder(crossBiomePattern)) {
            // when we're not at a border, check if we are at an inner corner or not
            byte diagonalBiomePattern = getDiagonalBiomePatternAt(current);
            if (isFullyInside((byte) (diagonalBiomePattern + crossBiomePattern))) {
                // if not, just move east to eventually reach the border. This should only happen in the beginning.
                this.current = current.getNeighbour(Direction.EAST);
                return true;
            } else {
                byte visitedPattern = getCrossVisitedPatternAt(current);
                Direction nextDirection =
                        decideNextDirectionAtInnerCorner((byte) (diagonalBiomePattern + crossBiomePattern), visitedPattern);

                if (nextDirection != Direction.UP) {
                    this.current = current.getNeighbour(nextDirection);
                    return true;
                } else {
                    return backtrack();
                }
            }
        } else {
            byte visitedPattern = getCrossVisitedPatternAt(current);

            Direction nextDirection = decideNextDirectionAtBorder(crossBiomePattern, visitedPattern);

            if (nextDirection != Direction.UP) {
                this.current = current.getNeighbour(nextDirection);
                return true;
            } else {
                return backtrack();
            }
        }
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

    private boolean backtrack() {
        if (steps.size() < 2) {
            finished = true;
            callback.onBoundingRectangleComputed(
                    new BiomeUtils.BoundingBox(
                            new BlockPos(minX, origin.getY(), minZ),
                            new BlockPos(maxX, origin.getY(), maxZ)));

            return false;
        }

        steps.pop();
        this.current = steps.pop();
        return true;
    }

    // The biome patterns look like this
    //         d 1 a
    //         4 x 2    -->   dcba 4321
    //         c 3 b
    // where the bits are 1 if the corresponding (center of the) grid cell is inside the biome
    private Direction decideNextDirectionAtInnerCorner(byte biomePattern, byte visitedPattern) {
        byte combinedPattern = (byte) (biomePattern | visitedPattern);

        if (isBitSetAt(visitedPattern, 0)) {
            // TODO
        }

        return Direction.UP;
    }

    private Direction decideNextDirectionAtBorder(byte biomePattern, byte visitedPattern) {
        // TODO
        return Direction.UP;
    }

    private boolean isCorner(byte pattern) {
        // the pattern represents a corner iff two subsequent indices (modulo 4) are 0 (i.e. outside the biome)
        for (int i = 0; i < 3; i++) {
            byte mask = (byte) (3 << i);
            if ((~pattern & mask) != 0) {
                return true;
            }
        }

        return (~pattern & 0b1001) != 0;
    }

    private boolean isOnBorder(byte pattern) {
        return (pattern << 4) != 0;
    }

    private boolean isFullyInside(byte pattern) {
        return pattern == 0;
    }

    private byte getCrossBiomePatternAt(GridSquare s) {
        Direction direction = Direction.NORTH;
        byte pattern = 0;
        for (int i = 0; i < 4; i++) {
            GridSquare n = s.getNeighbour(direction);

            if (outsideBiome.contains(n)) {
                continue;
            }

            if (BiomeUtils.isBiomeAtPositionEqual(world, biomeId, n.getCoordinates(origin, sampleInterval))) {
                pattern += (byte) (1 << i);
            }
            direction = direction.rotateYClockwise();
        }

        return pattern;
    }

    private byte getDiagonalBiomePatternAt(GridSquare s) {
        Direction direction = Direction.NORTH;
        byte pattern = 0;
        for (int i = 0; i < 4; i++) {
            GridSquare n = s.getDiagonalNeighbour(direction);

            if (outsideBiome.contains(n)) {
                continue;
            }

            if (BiomeUtils.isBiomeAtPositionEqual(world, biomeId, n.getCoordinates(origin, sampleInterval))) {
                pattern += (byte) (1 << 3 << i);
            }
            direction = direction.rotateYClockwise();
        }

        return pattern;
    }

    private byte getCrossVisitedPatternAt(GridSquare s) {
        Direction direction = Direction.NORTH;
        byte pattern = 0;
        for (int i = 0; i < 4; i++) {
            GridSquare n = s.getNeighbour(direction);
            if (visited.contains(n)) {
                pattern += (byte) (1 << i);
            }
            direction = direction.rotateYClockwise();
        }

        return pattern;
    }

    private boolean isBitSetAt(byte pattern, int i) {
        if (i > 7) {
            return false;
        }

        return (pattern & (1 << i)) != 0;
    }

    @FunctionalInterface
    public interface Callback {
        void onBoundingRectangleComputed(BiomeUtils.BoundingBox b);
    }

    private record GridSquare(int x, int z) {
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
    }
}
