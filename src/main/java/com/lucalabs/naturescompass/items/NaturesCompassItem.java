package com.lucalabs.naturescompass.items;

import com.lucalabs.naturescompass.utils.BiomeUtils;
import com.lucalabs.naturescompass.utils.CompassState;
import com.lucalabs.naturescompass.utils.ItemUtils;
import com.lucalabs.naturescompass.workers.BiomeSearchWorker;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class NaturesCompassItem extends Item {

    private BiomeSearchWorker worker;

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

        if (associatedBiome.isPresent()) {
            tooltip.add(Text.literal(BiomeUtils.getBiomeNameForDisplay(world, associatedBiome.get()))
                    .formatted(Formatting.GOLD));
        }
    }

    public void searchForBiome(ServerWorld world, PlayerEntity player, ItemStack stack, Identifier biomeId, BlockPos pos) {
        Optional<Biome> optionalBiome = BiomeUtils.getBiomeForIdentifier(world, biomeId);
        if (optionalBiome.isPresent()) {
            if (worker != null) {
                worker.stop();
            }
            worker = new BiomeSearchWorker(world, player, stack, optionalBiome.get(), pos);
            worker.start();
        }
    }

    public void succeed(ItemStack stack, PlayerEntity player, int x, int z, int samples) {
        setFound(stack, x, z, samples, player);
        worker = null;
    }

    public void fail(ItemStack stack, int searchRadius, int samples) {
        setNotFound(stack, searchRadius, samples);
        worker = null;
    }

    public UUID getUuid(ItemStack stack) {
        if (ItemUtils.verifyNBT(stack)) {
            if (!stack.getNbt().contains("ID")) {
                stack.getNbt().putUuid("ID", UUID.randomUUID());
            }

            return stack.getNbt().getUuid("ID");
        }
        return null;
    }

    public void setFound(ItemStack stack, int x, int z, int samples, PlayerEntity player) {
        if (ItemUtils.verifyNBT(stack)) {
            stack.getNbt().putInt("State", CompassState.FOUND.getId());
            stack.getNbt().putInt("FoundX", x);
            stack.getNbt().putInt("FoundZ", z);
            stack.getNbt().putInt("Samples", samples);
        }
    }

    public void setNotFound(ItemStack stack, int searchRadius, int samples) {
        if (ItemUtils.verifyNBT(stack)) {
            stack.getNbt().putInt("State", CompassState.NOT_FOUND.getId());
            stack.getNbt().putInt("SearchRadius", searchRadius);
            stack.getNbt().putInt("Samples", samples);
        }
    }

    public void setBiomeId(ItemStack stack, Identifier biomeID) {
        if (ItemUtils.verifyNBT(stack)) {
            stack.getNbt().putString("BiomeID", biomeID.toString());
        }
    }

    public CompassState getState(ItemStack stack) {
        if (ItemUtils.verifyNBT(stack)) {
            return CompassState.fromId(stack.getNbt().getInt("State"));
        }

        return null;
    }

    public int getFoundBiomeX(ItemStack stack) {
        if (ItemUtils.verifyNBT(stack)) {
            return stack.getNbt().getInt("FoundX");
        }

        return 0;
    }

    public int getFoundBiomeZ(ItemStack stack) {
        if (ItemUtils.verifyNBT(stack)) {
            return stack.getNbt().getInt("FoundZ");
        }

        return 0;
    }

    public Identifier getBiomeId(ItemStack stack) {
        if (ItemUtils.verifyNBT(stack)) {
            return new Identifier(stack.getNbt().getString("BiomeID"));
        }

        return new Identifier("");
    }
}
