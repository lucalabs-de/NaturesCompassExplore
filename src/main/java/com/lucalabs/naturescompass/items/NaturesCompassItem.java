package com.lucalabs.naturescompass.items;

import com.lucalabs.naturescompass.NaturesCompass;
import com.lucalabs.naturescompass.utils.BiomeUtils;
import com.lucalabs.naturescompass.utils.CompassState;
import com.lucalabs.naturescompass.utils.ItemUtils;
import com.lucalabs.naturescompass.workers.BiomeMeasureWorker;
import com.lucalabs.naturescompass.workers.BiomeSearchWorker;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class NaturesCompassItem extends Item {

    public static final int MIN_BIOME_DISTANCE_MODIFIER = 200;

    public BiomeSearchWorker searchWorker;
    public BiomeMeasureWorker measureWorker;

    public NaturesCompassItem() {
        super(new FabricItemSettings().maxCount(1));
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext type) {
        if (world == null) {
            return;
        }

        Identifier associatedBiomeId = getBiomeId(stack);
        Optional<Biome> associatedBiome = BiomeUtils.getBiomeForIdentifier(world, associatedBiomeId);

        associatedBiome.ifPresent(biome -> tooltip.add(Text.literal(BiomeUtils.getBiomeNameForDisplay(world, biome))
                .formatted(Formatting.GOLD)));
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient()) {
            ServerWorld serverWorld = (ServerWorld) world;
            BlockPos curPos = entity.getBlockPos();
            switch (getState(stack)) {
                case INACTIVE:
                    if (hasBiomeId(stack)) {
                        searchForBiome(serverWorld, stack, getBiomeId(stack), curPos);
                    }
                    break;
                case FOUND_SECOND_CLOSEST_MIN_DIST:
                    if (!isClosestStillValid(stack, curPos)) {
                        NaturesCompass.LOGGER.info("Tracked biome may no longer be closest, recalibrating...");
                        searchForBiome(serverWorld, stack, getBiomeId(stack), curPos);
                    }
            }
        }
    }

//    @Override
//    public boolean isItemBarVisible(ItemStack stack) {
//        return getState(stack) != CompassState.UNKNOWN;
//    }
//
//    @Override
//    public int getItemBarStep(ItemStack stack) {
//        return getBiomeId(stack).hashCode() % 13;
//    }

    public void searchForBiome(ServerWorld world, ItemStack stack, Identifier biomeId, BlockPos pos) {
        Optional<Biome> optionalBiome = BiomeUtils.getBiomeForIdentifier(world, biomeId);
        if (optionalBiome.isPresent()) {
            setState(stack, CompassState.SEARCHING);

            if (searchWorker != null) {
                searchWorker.stop();
            }

            searchWorker = new BiomeSearchWorker(
                    world,
                    optionalBiome.get(),
                    pos,
                    (x, z, s) -> foundBiome(world, stack, x, z, pos.getX(), pos.getY(), pos.getZ(), s),
                    (r, s) -> fail(stack, r, s));

            searchWorker.start();
        }
    }

    public void searchForSecondClosestBiome(ServerWorld world, ItemStack stack, BlockPos origin, BlockPos closestBiome) {
        Identifier biomeId = getBiomeId(stack);
        Optional<Biome> optionalBiome = BiomeUtils.getBiomeForIdentifier(world, biomeId);

        if (optionalBiome.isPresent()) {

            if (measureWorker != null) {
                measureWorker.stop();
            }

            measureWorker = new BiomeMeasureWorker(world, closestBiome, biomeId, (boundingBox) -> {

                if (searchWorker != null) {
                    searchWorker.stop();
                }

                // the measurement works in 16x16 grids, so the actual maximum distance to a point in the biome might
                // be up to 8 blocks larger
                int maxDistance = boundingBox.getMaxDistanceFrom(origin) + 8;

                // We require a minimum distance for two biome occurrences to be considered distinct. This avoids having
                // to update the compass too often.
                int minBiomeDistance = MIN_BIOME_DISTANCE_MODIFIER * BiomeUtils.getBiomeSize(world);
                int minDistance = new BiomeUtils.BoundingBox(closestBiome, closestBiome).getMaxDistanceFrom(origin) + minBiomeDistance;
                int innerRadius = Math.max(maxDistance, minDistance);

                searchWorker = new BiomeSearchWorker(
                        world,
                        optionalBiome.get(),
                        origin,
                        innerRadius, // only start searching beyond the closest biome
                        (x, z, s) -> foundBiome(world, stack, x, z, origin.getX(), origin.getY(), origin.getZ(), s),
                        (r, s) -> fail(stack, r, s));

                searchWorker.start();
            });

            measureWorker.start();
        }
    }

    public void foundBiome(ServerWorld world, ItemStack stack, int x, int z, int xO, int yO, int zO, int samples) {
        switch (getState(stack)) {
            case SEARCHING:
                setClosestFound(stack, x, z, xO, zO, samples);
                searchForSecondClosestBiome(world, stack, new BlockPos(xO, yO, zO), new BlockPos(x, 0, z));
                break;
            case FOUND_CLOSEST:
                setSecondClosestFound(stack, x, z, xO, zO, samples);
                break;
        }

    }

    public void fail(ItemStack stack, int searchRadius, int samples) {
        if (getState(stack) == CompassState.FOUND_CLOSEST) {
            setSecondClosestNotFound(stack, searchRadius, samples);
        } else {
            setClosestNotFound(stack, searchRadius, samples);
        }

        searchWorker = null;
    }

    public UUID getUuid(ItemStack stack) {
        if (ItemUtils.verifyNBT(stack)) {
            if (!stack.getNbt().contains(NbtProperties.ID)) {
                stack.getNbt().putUuid(NbtProperties.ID, UUID.randomUUID());
            }

            return stack.getNbt().getUuid(NbtProperties.ID);
        }
        return null;
    }

    public void setClosestFound(ItemStack stack, int x, int z, int xO, int zO, int samples) {
        if (ItemUtils.verifyNBT(stack)) {
            stack.getNbt().putInt(NbtProperties.STATE, CompassState.FOUND_CLOSEST.getId());
            stack.getNbt().putInt(NbtProperties.ORIGIN_X, xO);
            stack.getNbt().putInt(NbtProperties.ORIGIN_Z, zO);
            stack.getNbt().putInt(NbtProperties.CLOSEST_X, x);
            stack.getNbt().putInt(NbtProperties.CLOSEST_Z, z);
            stack.getNbt().putInt(NbtProperties.SAMPLES, samples);
        }
    }

    public void setClosestNotFound(ItemStack stack, int searchRadius, int samples) {
        if (ItemUtils.verifyNBT(stack)) {
            stack.getNbt().putInt(NbtProperties.STATE, CompassState.CLOSEST_NOT_FOUND.getId());
            stack.getNbt().putInt(NbtProperties.SEARCH_RADIUS, searchRadius);
            stack.getNbt().putInt(NbtProperties.SAMPLES, samples);
        }
    }

    public void setSecondClosestFound(ItemStack stack, int x, int z, int xO, int zO, int samples) {
        if (ItemUtils.verifyNBT(stack)) {
            stack.getNbt().putInt(NbtProperties.STATE, CompassState.FOUND_SECOND_CLOSEST_MIN_DIST.getId());

            // ensure this is actually farther away than the closest biome (our sampling might have gotten unlucky, or two biomes are really close together)
            long closestX = stack.getNbt().getInt(NbtProperties.CLOSEST_X);
            long closestZ = stack.getNbt().getInt(NbtProperties.CLOSEST_Z);

            Vec3d toClosest = new Vec3d(closestX - xO, 0.0, closestZ - zO);
            Vec3d toSecondClosest = new Vec3d(x - xO, 0.0, z - zO);

            double distToClosest = toClosest.length();
            double distToSecondClosest = toSecondClosest.length();

            if (distToClosest > distToSecondClosest) {
                // oh-oh, let's swap
                stack.getNbt().putDouble(NbtProperties.DISTANCE_TO_SECOND_CLOSEST, distToClosest);
                stack.getNbt().putInt(NbtProperties.CLOSEST_X, x);
                stack.getNbt().putInt(NbtProperties.CLOSEST_Z, z);
            } else {
                stack.getNbt().putDouble(NbtProperties.DISTANCE_TO_SECOND_CLOSEST, distToSecondClosest);
                stack.getNbt().putInt(NbtProperties.SAMPLES, samples);
            }
        }
    }

    public void setSecondClosestNotFound(ItemStack stack, int searchRadius, int samples) {
        if (ItemUtils.verifyNBT(stack)) {
            stack.getNbt().putInt(NbtProperties.STATE, CompassState.FOUND_SECOND_CLOSEST_MIN_DIST.getId());
            stack.getNbt().putInt(NbtProperties.SEARCH_RADIUS, searchRadius);
            stack.getNbt().putInt(NbtProperties.SAMPLES, samples);
        }
    }

    public void setBiomeId(ItemStack stack, Identifier biomeID) {
        if (ItemUtils.verifyNBT(stack)) {
            stack.getNbt().putString(NbtProperties.BIOME, biomeID.toString());
        }
    }

    public Identifier getBiomeId(ItemStack stack) {
        if (ItemUtils.verifyNBT(stack)) {
            return new Identifier(stack.getNbt().getString(NbtProperties.BIOME));
        }

        return new Identifier("");
    }

    public boolean hasBiomeId(ItemStack stack) {
        if (ItemUtils.verifyNBT(stack)) {
            return stack.getNbt().contains(NbtProperties.BIOME);
        }

        return false;
    }

    public void setState(ItemStack stack, CompassState state) {
        if (ItemUtils.verifyNBT(stack)) {
            stack.getNbt().putInt(NbtProperties.STATE, state.getId());
        }
    }

    public CompassState getState(ItemStack stack) {
        if (ItemUtils.verifyNBT(stack)) {
            return CompassState.fromId(stack.getNbt().getInt(NbtProperties.STATE));
        }

        return CompassState.UNKNOWN;
    }

    public BlockPos getFoundBiomePos(ItemStack stack) {
        return getClosestBiomePos(stack);
    }

    public BlockPos getClosestBiomePos(ItemStack stack) {
        if (ItemUtils.verifyNBT(stack)) {
            int x = stack.getNbt().getInt(NbtProperties.CLOSEST_X);
            int z = stack.getNbt().getInt(NbtProperties.CLOSEST_Z);

            return new BlockPos(x, 0, z);
        }

        return BlockPos.ORIGIN;
    }

    public BlockPos getOriginPos(ItemStack stack) {
        if (ItemUtils.verifyNBT(stack)) {
            int x = stack.getNbt().getInt(NbtProperties.ORIGIN_X);
            int z = stack.getNbt().getInt(NbtProperties.ORIGIN_Z);

            return new BlockPos(x, 0, z);
        }

        return BlockPos.ORIGIN;
    }

    public double getDistanceToSecondClosest(ItemStack stack) {
        if (ItemUtils.verifyNBT(stack)) {
            return stack.getNbt().getDouble(NbtProperties.DISTANCE_TO_SECOND_CLOSEST);
        }

        return 0.0;
    }

    public boolean isClosestStillValid(ItemStack stack, BlockPos playerPos) {
        if (!ItemUtils.verifyNBT(stack)) {
            NaturesCompass.LOGGER.error("NBT not valid");
            return false;
        }

        // if someone reads this and knows how to do this without the square root, lmk
        if (getState(stack) == CompassState.FOUND_SECOND_CLOSEST_MIN_DIST) {
            Vec3d origin = getOriginPos(stack).toCenterPos();
            Vec3d closest = getFoundBiomePos(stack).toCenterPos();
            Vec3d current = playerPos.toCenterPos();

            Vec3d originToCur = current.subtract(origin);
            Vec3d curToClosest = closest.subtract(current);

            double originDistToSecondClosest = getDistanceToSecondClosest(stack);
            double originDistToCur = originToCur.horizontalLength();
            double maxSafeDist = originDistToSecondClosest - originDistToCur;

            double curSqDistToClosest = curToClosest.horizontalLengthSquared();

            return curSqDistToClosest < maxSafeDist * maxSafeDist;
        }

        return true;
    }

    public static class NbtProperties {
        public static final String ID = "ID";
        public static final String STATE = "State";
        public static final String ORIGIN_X = "OriginX";
        public static final String ORIGIN_Z = "OriginZ";
        public static final String CLOSEST_X = "FoundX";
        public static final String CLOSEST_Z = "FoundZ";
        public static final String DISTANCE_TO_SECOND_CLOSEST = "FoundDist2";
        public static final String SAMPLES = "Samples";
        public static final String SEARCH_RADIUS = "SearchRadius";
        public static final String BIOME = "BiomeID";
    }
}