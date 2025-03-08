package com.lucalabs.naturescompass;

import java.util.ArrayList;
import java.util.List;

import com.lucalabs.naturescompass.recipes.CalibrationRecipe;
import com.lucalabs.naturescompass.recipes.CalibrationRecipeSerializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lucalabs.naturescompass.screens.BiomeChoiceScreenHandler;
import com.lucalabs.naturescompass.config.NaturesCompassConfig;
import com.lucalabs.naturescompass.items.NaturesCompassItem;
import com.lucalabs.naturescompass.network.SearchPacket;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class NaturesCompass implements ModInitializer {

    public static final String MODID = "naturescompass";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static final NaturesCompassItem NATURES_COMPASS_ITEM = new NaturesCompassItem();
    public static final ScreenHandlerType<BiomeChoiceScreenHandler> BIOME_SCREEN_HANDLER =
            Registry.register(
                    Registries.SCREEN_HANDLER,
                    Identifier.of("naturescompass", "biome_choice_screen"),
                    new ScreenHandlerType<>(BiomeChoiceScreenHandler::new, FeatureSet.empty()));

    public static List<Identifier> allowedBiomes;
    public static ListMultimap<Identifier, Identifier> dimensionIDsForAllowedBiomeIDs;

    @Override
    public void onInitialize() {
        NaturesCompassConfig.load();

        Registry.register(Registries.ITEM, new Identifier(MODID, "naturescompass"), NATURES_COMPASS_ITEM);
        Registry.register(Registries.RECIPE_SERIALIZER, CalibrationRecipeSerializer.ID, CalibrationRecipeSerializer.INSTANCE);
        Registry.register(Registries.RECIPE_TYPE, Identifier.of(MODID, CalibrationRecipe.Type.ID), CalibrationRecipe.Type.INSTANCE);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(NATURES_COMPASS_ITEM));

        ServerPlayNetworking.registerGlobalReceiver(SearchPacket.ID, SearchPacket::apply);

        allowedBiomes = new ArrayList<>();
        dimensionIDsForAllowedBiomeIDs = ArrayListMultimap.create();
    }
}
