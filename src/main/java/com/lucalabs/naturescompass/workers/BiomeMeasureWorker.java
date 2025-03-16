package com.lucalabs.naturescompass.workers;

import com.lucalabs.naturescompass.NaturesCompass;
import com.lucalabs.naturescompass.utils.BiomeUtils;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;

import java.util.Stack;
import java.util.TreeSet;

public class BiomeMeasureWorker implements WorldWorkerManager.IWorker {

    private final ServerWorld world;
    private final Identifier biomeId;
    private final int[] yValues;
    private final int sampleInterval;

    private final BlockPos origin;
    private final TreeSet<GridSquare> visited;
    private final TreeSet<GridSquare> outsideBiome;
    private final Stack<GridSquare> steps;
    private final Callback callback;
    private int maxX;
    private int maxZ;
    private int minX;
    private int minZ;
    private boolean finished;
    private GridSquare current;
    private long time;

    public BiomeMeasureWorker(ServerWorld world, BlockPos biome, Identifier biomeId, Callback callback) {
        this.callback = callback;

        this.world = world;
        this.biomeId = biomeId;
        this.yValues = MathHelper.stream(biome.getY(), world.getBottomY() + 1, world.getTopY(), 64).toArray();
        this.sampleInterval = 16;

        this.origin = biome;

        this.finished = false;
        this.maxX = this.origin.getX();
        this.maxZ = this.origin.getZ();
        this.minX = this.origin.getX();
        this.minZ = this.origin.getZ();

        this.visited = new TreeSet<>();
        this.outsideBiome = new TreeSet<>();
        this.steps = new Stack<>();

        this.current = new GridSquare(0, 0);
    }

    @Override
    public boolean doWork() {
        steps.push(current);
        visited.add(current);

        byte biomePattern = getBiomePattern(current);
        String biomePatternString = String.format("%8s", Integer.toBinaryString(biomePattern & 0xFF)).replace(' ', '0');

        // corners are the only candidates that can change the bounding box
        if (isCorner(biomePattern)) {
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

        if (!isOnBorder(biomePattern)) {
            // when we're not at a border, check if we are at an inner corner or not
            if (isFullyInside(biomePattern)) {
                // if not, just move east to eventually reach the border. This should only happen in the beginning and when backtracking.
                GridSquare neighbourEast = current.getNeighbour(Direction.EAST);
                if (!visited.contains(neighbourEast)) {
                    this.current = current.getNeighbour(Direction.EAST);
                    return true;
                } else {
                    return backtrack();
                }
            } else {
                byte visitedPattern = getCrossVisitedPatternAt(current);
                Direction nextDirection =
                        decideNextDirectionAtInnerCorner(biomePattern, visitedPattern);

                if (nextDirection != Direction.UP) {
                    this.current = current.getNeighbour(nextDirection);
                    return true;
                } else {
                    return backtrack();
                }
            }
        } else {
            byte visitedPattern = getCrossVisitedPatternAt(current);

            Direction nextDirection = decideNextDirectionAtBorder(biomePattern, visitedPattern);

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

    private void succeed() {
        long duration = System.currentTimeMillis() - time;
        NaturesCompass.LOGGER.info("Measuring biome {} took {}ms", biomeId, duration);
        NaturesCompass.LOGGER.info("Biome {} has bounding box of {}x{} blocks", biomeId, maxX - minX, maxZ - minZ);
        callback.onBoundingRectangleComputed(
                new BiomeUtils.BoundingBox(
                        new BlockPos(minX, origin.getY(), minZ),
                        new BlockPos(maxX, origin.getY(), maxZ)));
    }

    private boolean backtrack() {
        if (steps.size() < 2) {
            finished = true;
            succeed();
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
        byte validPositions = (byte) (biomePattern & ~visitedPattern);
        byte validNeighbours = getNeighboursOnBorder(biomePattern);

        String validPositionsString = String.format("%8s", Integer.toBinaryString(validPositions & 0xFF)).replace(' ', '0');
        String validNeighboursString = String.format("%8s", Integer.toBinaryString(validNeighbours & 0xFF)).replace(' ', '0');

        Direction result = Direction.NORTH;

        for (int i = 0; i < 4; i++) {
            if (isBitSetAt(validNeighbours, i) && isBitSetAt(validPositions, i)) {
                return result;
            }

            result = result.rotateYClockwise();
        }

        return Direction.UP;
    }

    private Direction decideNextDirectionAtBorder(byte biomePattern, byte visitedPattern) {
        byte validPositions = (byte) (biomePattern & ~visitedPattern);
        byte validNeighbours = getNeighboursInBorderVicinity(biomePattern);

        String validPositionsString = String.format("%8s", Integer.toBinaryString(validPositions & 0xFF)).replace(' ', '0');
        String biomePatternString = String.format("%8s", Integer.toBinaryString(biomePattern & 0xFF)).replace(' ', '0');
        String visitedPatternString = String.format("%8s", Integer.toBinaryString(visitedPattern & 0xFF)).replace(' ', '0');
        String validNeighboursString = String.format("%8s", Integer.toBinaryString(validNeighbours & 0xFF)).replace(' ', '0');

        if (isExactlyOneSet(visitedPattern)) {
            // continue in the direction we were going if possible
            Direction next = Direction.SOUTH;
            for (int i = 0; i < 4; i++) {
                if (isBitSetAt(visitedPattern, i)) {
                    if (isBitSetAt(validPositions, (i + 2) % 4)) {
                        return next;
                    } else {
                        break; // we know that only one bit is set
                    }
                }
                next = next.rotateYClockwise();
            }
        }

        Direction next = Direction.NORTH;
        for (int i = 0; i < 4; i++) {
            if (isBitSetAt(validPositions, i) && isBitSetAt(validNeighbours, i)) {
                return next;
            }

            next = next.rotateYClockwise();
        }

        return Direction.UP;
    }

    private boolean isCorner(byte pattern) {
        // the pattern represents a corner iff two subsequent indices (modulo 4) are 0 (i.e. outside the biome)
        for (int i = 0; i < 3; i++) {
            byte mask = (byte) (3 << i);
            byte masked = (byte) (~pattern & mask);
            if (masked != 0 && !isExactlyOneSet(masked)) {
                return true;
            }
        }

        return (~pattern & 0b1001) != 0;
    }

    private boolean isOnBorder(byte pattern) {
        // we need to use toUnsignedInt here because Java promotes bytes to (signed) int before doing anything. This
        // causes trouble when the most significant bit is 1, i.e. the numeric value of our byte is negative. This
        // language makes me sad.
        return Byte.toUnsignedInt(pattern) % 16 != 15;
    }

    private byte getNeighboursInBorderVicinity(byte pattern) {
        byte result = 0;
        for (int i = 0; i < 4; i++) {
            // why doesn't -1 % 4 = 3? Why does this language have to be so annoying?
            if (!isBitSetAt(pattern, (i + 1) % 4) || !isBitSetAt(pattern, Math.floorMod(i - 1, 4))) {
                result |= (byte) (1 << i);
            }
        }

        return result;
    }

    private boolean isFullyInside(byte pattern) {
        return pattern == (byte) 0b1111_1111;
    }

    private byte getNeighboursOnBorder(byte biomePattern) {
        byte result = 0;
        for (int i = 0; i < 4; i++) {
            if (!isBitSetAt(biomePattern, 4 + i)) {
                result |= (byte) (1 << i);
                result |= (byte) (1 << ((i + 1) % 4));
            }
        }

        return result;
    }

    private byte getBiomePattern(GridSquare s) {
        return (byte) (getCrossBiomePatternAt(s) | getDiagonalBiomePatternAt(s));
    }

    private byte getCrossBiomePatternAt(GridSquare s) {
        Direction direction = Direction.NORTH;
        byte pattern = 0;
        for (int i = 0; i < 4; i++) {
            GridSquare n = s.getNeighbour(direction);
            direction = direction.rotateYClockwise();

            if (outsideBiome.contains(n)) {
                continue;
            }

//            // TODO remove later
//            Vec3i pos = n.getCoordinates(origin, sampleInterval);
//            for (int y :yValues) {
//                world.setBlockState(new BlockPos(pos.getX(), y, pos.getZ()), Blocks.BLUE_WOOL.getDefaultState());
//            }

            if (BiomeUtils.isBiomeAtAnyYValueEqual(world, biomeId, n.getCoordinates(origin, sampleInterval), yValues)) {
                pattern |= (byte) (1 << i);
            } else {
                outsideBiome.add(n);
            }
        }

        return pattern;
    }

    private byte getDiagonalBiomePatternAt(GridSquare s) {
        Direction direction = Direction.NORTH;
        byte pattern = 0;
        for (int i = 0; i < 4; i++) {
            GridSquare n = s.getDiagonalNeighbour(direction);
            direction = direction.rotateYClockwise();

            if (outsideBiome.contains(n)) {
                continue;
            }

            if (BiomeUtils.isBiomeAtAnyYValueEqual(world, biomeId, n.getCoordinates(origin, sampleInterval), yValues)) {
                pattern |= (byte) (1 << 4 << i);
            } else {
                outsideBiome.add(n);
            }
        }

        return pattern;
    }

    private byte getCrossVisitedPatternAt(GridSquare s) {
        Direction direction = Direction.NORTH;
        byte pattern = 0;
        for (int i = 0; i < 4; i++) {
            GridSquare n = s.getNeighbour(direction);
            direction = direction.rotateYClockwise();

            if (visited.contains(n)) {
                pattern |= (byte) (1 << i);
            }
        }

        return pattern;
    }

    private boolean isBitSetAt(byte pattern, int i) {
        if (i > 7) {
            return false;
        }

        return (pattern & (1 << i)) != 0;
    }

    private boolean isExactlyOneSet(byte pattern) {
        int patternI = Byte.toUnsignedInt(pattern);
        return pattern != 0 && (patternI & (patternI - 1)) == 0;
    }

    @FunctionalInterface
    public interface Callback {
        void onBoundingRectangleComputed(BiomeUtils.BoundingBox b);
    }

    private record GridSquare(int x, int z) implements Comparable<GridSquare> {
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
