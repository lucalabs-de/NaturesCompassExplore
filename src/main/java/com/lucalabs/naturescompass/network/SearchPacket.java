package com.lucalabs.naturescompass.network;

import com.lucalabs.naturescompass.NaturesCompass;
import com.lucalabs.naturescompass.items.NaturesCompassItem;
import com.lucalabs.naturescompass.utils.ItemUtils;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public class SearchPacket extends PacketByteBuf {

    public static final Identifier ID = new Identifier(NaturesCompass.MODID, "search");

    public SearchPacket(UUID compassId, Identifier biomeId, BlockPos pos) {
        super(Unpooled.buffer());
        writeIdentifier(biomeId);
        writeBlockPos(pos);
        writeUuid(compassId);
    }

    public static void apply(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        final Identifier biomeID = buf.readIdentifier();
        final BlockPos pos = buf.readBlockPos();
        final UUID compassId = buf.readUuid();

        server.execute(() -> {
            ItemStack inInv = ItemUtils.getNatureCompassInInventory(player, compassId);
            ItemStack underCursor = ItemUtils.getNatureCompassUnderCursor(player, compassId);
            ItemStack compass = inInv.isEmpty() ? underCursor : inInv;
            if (!compass.isEmpty()) {
                final ServerWorld world = player.getServerWorld();
                ((NaturesCompassItem) compass.getItem()).searchForBiome(world, compass, biomeID, pos);
            }
        });
    }

}