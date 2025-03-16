package com.lucalabs.naturescompass.utils;

import com.lucalabs.naturescompass.NaturesCompass;
import com.lucalabs.naturescompass.items.NaturesCompassItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public abstract class ItemUtils {

    public static boolean verifyNBT(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != NaturesCompass.NATURES_COMPASS_ITEM) {
            return false;
        } else if (!stack.hasNbt()) {
            stack.setNbt(new NbtCompound());
        }

        return true;
    }

    public static ItemStack getNatureCompassInInventory(PlayerEntity player, UUID compassId) {
        PlayerInventory inv = player.getInventory();

        for (int i = 0; i < inv.size(); i++) {
            ItemStack cur = inv.getStack(i);
            if (verifyNBT(cur)) {
                if (cur.getNbt().contains(NaturesCompassItem.NbtProperties.ID)
                        && cur.getNbt().getUuid(NaturesCompassItem.NbtProperties.ID).equals(compassId)) {
                    return cur;
                }
            }
        }

        return ItemStack.EMPTY;
    }

    public static ItemStack getNatureCompassUnderCursor(ServerPlayerEntity player, UUID compassId) {
        ItemStack underCursor = player.currentScreenHandler.getCursorStack();
        if (!underCursor.isEmpty()) {
            if (verifyNBT(underCursor)) {
                if (underCursor.getNbt().contains(NaturesCompassItem.NbtProperties.ID)
                        && underCursor.getNbt().getUuid(NaturesCompassItem.NbtProperties.ID).equals(compassId)) {
                    return underCursor;
                }
            }
        }

        return ItemStack.EMPTY;
    }
}
