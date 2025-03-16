package com.lucalabs.naturescompass;

import com.lucalabs.naturescompass.config.NaturesCompassConfig;
import com.lucalabs.naturescompass.items.NaturesCompassItem;
import com.lucalabs.naturescompass.loot.CalibrateRandomlyLootFunction;
import com.lucalabs.naturescompass.loot.LootManager;
import com.lucalabs.naturescompass.network.SearchPacket;
import com.lucalabs.naturescompass.recipes.CalibrationRecipe;
import com.lucalabs.naturescompass.recipes.CalibrationRecipeSerializer;
import com.lucalabs.naturescompass.screens.BiomeChoiceScreenHandler;
import mezz.jei.api.IModPlugin;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemGroups;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class NaturesCompass implements ModInitializer {

    public static final String MOD_ID = "naturescompass";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static final NaturesCompassItem NATURES_COMPASS_ITEM = new NaturesCompassItem();

    public static final LootFunctionType CALIBRATE_RANDOMLY_LOOT_FUNCTION =
            Registry.register(
                    Registries.LOOT_FUNCTION_TYPE,
                    Identifier.of(MOD_ID, CalibrateRandomlyLootFunction.ID),
                    new LootFunctionType(new CalibrateRandomlyLootFunction.Serializer()));

    public static List<Identifier> allowedBiomes;    public static final ScreenHandlerType<BiomeChoiceScreenHandler> BIOME_SCREEN_HANDLER =
            Registry.register(
                    Registries.SCREEN_HANDLER,
                    Identifier.of("naturescompass", "biome_choice_screen"),
                    new ScreenHandlerType<>(BiomeChoiceScreenHandler::new, FeatureSet.empty()));

    @Override
    public void onInitialize() {
        NaturesCompassConfig.load();

//        NaturesCompassIntegrations.setRecipes(NaturesCompassConfig.getRawIngredientMap());
        IModPlugin m;

        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "naturescompass"), NATURES_COMPASS_ITEM);
        Registry.register(Registries.RECIPE_SERIALIZER, CalibrationRecipeSerializer.ID, CalibrationRecipeSerializer.INSTANCE);
        Registry.register(Registries.RECIPE_TYPE, Identifier.of(MOD_ID, CalibrationRecipe.Type.ID), CalibrationRecipe.Type.INSTANCE);

        LootTableEvents.MODIFY.register(LootManager::addCompassesToLootTables);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(NATURES_COMPASS_ITEM));

        ServerPlayNetworking.registerGlobalReceiver(SearchPacket.ID, SearchPacket::apply);

        allowedBiomes = new ArrayList<>();
    }

    private void test(IModPlugin m) {
        Identifier i = m.getPluginUid();
    }
}
