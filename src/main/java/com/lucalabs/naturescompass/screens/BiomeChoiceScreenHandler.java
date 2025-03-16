package com.lucalabs.naturescompass.screens;

import com.lucalabs.naturescompass.NaturesCompass;
import com.lucalabs.naturescompass.items.NaturesCompassItem;
import com.lucalabs.naturescompass.network.SearchPacket;
import com.lucalabs.naturescompass.recipes.CalibrationRecipe;
import com.lucalabs.naturescompass.utils.BiomeUtils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class BiomeChoiceScreenHandler extends ScreenHandler {
    final Slot inputSlot;
    final Slot outputSlot;
    final List<Slot> ingredientSlots;

    private final SimpleInventory input;
    private final CraftingResultInventory output;

    private final World world;
    private final ScreenHandlerContext context;

    public BiomeChoiceScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
    }

    public BiomeChoiceScreenHandler(int syncId, PlayerInventory playerInventory, final ScreenHandlerContext context) {
        super(NaturesCompass.BIOME_SCREEN_HANDLER, syncId);

        this.world = playerInventory.player.getWorld();
        this.context = context;

        this.output = new CraftingResultInventory();
        this.input = new SimpleInventory(4) {
            public void markDirty() {
                super.markDirty();
                onContentChanged(this);
            }
        };

        this.inputSlot = this.addSlot(new Slot(this.input, 0, 31, 15) {
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(NaturesCompass.NATURES_COMPASS_ITEM);
            }
        });
        this.outputSlot = this.addSlot(new Slot(this.output, 4, 126, 34) {
            public boolean canInsert(ItemStack stack) {
                return false;
            }

            public void onTakeItem(PlayerEntity player, ItemStack stack) {
                Identifier biomeId = NaturesCompass.NATURES_COMPASS_ITEM.getBiomeId(stack);
                Optional<Biome> biome = BiomeUtils.getBiomeForIdentifier(world, biomeId);

                biome.ifPresent((Biome b) -> {
                    inputSlot.takeStack(1);
                    ingredientSlots.forEach((Slot s) -> s.takeStack(1));
                    searchForBiome(player, b, stack);
                });

                super.onTakeItem(player, stack);
            }
        });

        this.ingredientSlots = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Slot ingredientSlot = this.addSlot(new Slot(this.input, i + 1, 31 + i * 18, 52));
            this.ingredientSlots.add(ingredientSlot);
        }

        int i;
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    public void onContentChanged(Inventory inventory) {
        populateResult();
    }

    public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
        if (slot.inventory == this.output) {
            return false;
        }

        if (slot.inventory == this.input) {
            return stack.getItem() instanceof NaturesCompassItem;
        }

        return super.canInsertIntoSlot(stack, slot);
    }

    void populateResult() {
        Optional<CalibrationRecipe> match =
                this.world.getRecipeManager().getFirstMatch(CalibrationRecipe.Type.INSTANCE, this.input, this.world);

        if (match.isPresent()) {
            this.outputSlot.setStackNoCallbacks(match.get().getOutput(this.world.getRegistryManager()).copy());
        } else {
            this.outputSlot.setStackNoCallbacks(ItemStack.EMPTY);
        }

        this.sendContentUpdates();
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot == 4) {
                if (!this.insertItem(originalStack, 5, 41, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickTransfer(originalStack, newStack);
                slot.onTakeItem(player, newStack);
            } else if (invSlot <= 3) {
                if (!this.insertItem(originalStack, 5, 41, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (invSlot < 32) {
                if (!this.insertItem(originalStack, 32, 41, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (invSlot < 41 && !this.insertItem(originalStack, 5, 32, false)) {
                return ItemStack.EMPTY;
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            }

            slot.markDirty();
            if (originalStack.getCount() == newStack.getCount()) {
                return ItemStack.EMPTY;
            }

            this.sendContentUpdates();
        }

        return newStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return canUse(ScreenHandlerContext.EMPTY, player, Blocks.CARTOGRAPHY_TABLE);
    }

    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.output.removeStack(1);
        context.run((world, pos) -> {
            this.dropInventory(player, this.input);
        });
    }

    private void searchForBiome(PlayerEntity player, Biome biome, ItemStack compass) {
        UUID compassId = NaturesCompass.NATURES_COMPASS_ITEM.getUuid(compass);
        if (compassId != null) {
            ClientPlayNetworking.send(
                    SearchPacket.ID,
                    new SearchPacket(compassId, BiomeUtils.getIdentifierForBiome(world, biome), player.getBlockPos()));
        }
    }
}
