package com.lucalabs.naturescompass;

import com.lucalabs.naturescompass.integrations.emi.CalibrationEmiRecipe;
import com.lucalabs.naturescompass.integrations.jei.CalibrationCategory;
import com.lucalabs.naturescompass.items.NaturesCompassItem;
import com.lucalabs.naturescompass.network.SyncPacket;
import com.lucalabs.naturescompass.screens.BiomeChoiceScreen;
import com.lucalabs.naturescompass.utils.CompassState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.item.ClampedModelPredicateProvider;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

public class NaturesCompassClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(SyncPacket.ID, SyncPacket::apply);

        HandledScreens.register(NaturesCompass.BIOME_SCREEN_HANDLER, BiomeChoiceScreen::new);

        ModelPredicateProviderRegistry.register(NaturesCompass.NATURES_COMPASS_ITEM, new Identifier("angle"), new ClampedModelPredicateProvider() {
            private final WeakHashMap<ItemStack, InterpolationData> interpolationData = new WeakHashMap<>();

            @Override
            public float unclampedCall(ItemStack stack, ClientWorld world, LivingEntity entityLiving, int seed) {
                if (entityLiving == null && !stack.isInFrame()) {
                    return 0.0F;
                } else {
                    final boolean entityExists = entityLiving != null;
                    final Entity entity = entityExists ? entityLiving : stack.getFrame();

                    if (world == null && entity.getWorld() instanceof ClientWorld) {
                        world = (ClientWorld) entity.getWorld();
                    }

                    double rotationDeg = entityExists ? (double) entity.getYaw() : getFrameRotation((ItemFrameEntity) entity);
                    double rotation = Math.toRadians(rotationDeg % 360.0D);
                    double adjustedRotation = Math.PI - (rotation - Math.PI / 2 - getAngle(world, entity, stack));

                    if (entityExists) {
                        adjustedRotation = interpolateToNewRotation(world, getInterpolationData(stack), adjustedRotation);
                    }

                    final float f = (float) (adjustedRotation / (Math.PI * 2D));
                    return MathHelper.floorMod(f, 1.0F);
                }
            }

            private double interpolateToNewRotation(ClientWorld world, InterpolationData data, double newRotation) {
                if (world.getTime() != data.lastUpdateTick) {
                    data.lastUpdateTick = world.getTime();
                    double d0 = newRotation - data.rotation;
                    // normalize to [-pi, pi] to take the shortest route
                    d0 = d0 % (Math.PI * 2D);
                    d0 = MathHelper.floorMod(d0 + Math.PI, Math.PI * 2D) - Math.PI;
                    data.rota += d0 * 0.1D;
                    data.rota *= 0.8D;
                    data.rotation += data.rota;
                }

                return data.rotation;
            }

            private double getFrameRotation(ItemFrameEntity itemFrame) {
                return MathHelper.wrapDegrees(180 + itemFrame.getHorizontalFacing().getHorizontal() * 90);
            }

            private double getAngle(ClientWorld world, Entity entity, ItemStack stack) {
                if (stack.getItem() == NaturesCompass.NATURES_COMPASS_ITEM) {
                    NaturesCompassItem compassItem = (NaturesCompassItem) stack.getItem();
                    BlockPos pos;
                    CompassState curState = compassItem.getState(stack);
                    if (curState == CompassState.FOUND_CLOSEST || curState == CompassState.FOUND_SECOND_CLOSEST_MIN_DIST) {
                        pos = compassItem.getFoundBiomePos(stack);
                    } else {
                        pos = world.getSpawnPos();
                    }
                    return Math.atan2((double) pos.getZ() - entity.getPos().z, (double) pos.getX() - entity.getPos().x);
                }
                return 0.0D;
            }

            private InterpolationData getInterpolationData(ItemStack stack) {
                if (!interpolationData.containsKey(stack)) {
                    interpolationData.put(stack, new InterpolationData());
                }
                return interpolationData.get(stack);
            }
        });

        if (FabricLoader.getInstance().isModLoaded("jei")) {
            ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
                ClientWorld world = client.world;
                if (world != null) {
                    CalibrationCategory.world = new WeakReference<>(world);
                }
            });
        }

        if (FabricLoader.getInstance().isModLoaded("emi")) {
            ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
                ClientWorld world = client.world;
                if (world != null) {
                    CalibrationEmiRecipe.world = new WeakReference<>(world);
                }
            });
        }
    }

    private static class InterpolationData {
        double rotation;
        double rota;
        long lastUpdateTick;

        InterpolationData() {
            this.rotation = 0;
            this.rota = 0;
            this.lastUpdateTick = 0;
        }
    }

}
