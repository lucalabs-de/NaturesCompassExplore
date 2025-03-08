package com.lucalabs.naturescompass.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lucalabs.naturescompass.NaturesCompass;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NaturesCompassConfig {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static int maxSamples = 50000;
    public static int radiusModifier = 2500;
    public static int sampleSpaceModifier = 16;
    public static boolean fixBiomeNames = true;
    public static boolean pointToClosest = true;
    public static Map<Identifier, List<Ingredient>> calibrationRecipes = getDefaultRecipes();
    private static Path configFilePath;

    public static void load() {
        Reader reader;
        if (getFilePath().toFile().exists()) {
            try {
                reader = Files.newBufferedReader(getFilePath());

                Data data = gson.fromJson(reader, Data.class);

                try {
                    maxSamples = data.common.maxSamples;
                    radiusModifier = data.common.radiusModifier;
                    sampleSpaceModifier = data.common.sampleSpaceModifier;

                    fixBiomeNames = data.client.fixBiomeNames;
                    pointToClosest = data.common.pointToClosestBiome;

                    calibrationRecipes = toIngredientMap(data.common.recipes);
                } catch (NullPointerException e) {
                    NaturesCompass.LOGGER.error("Failed to parse config, is a field missing? If this error persists, try deleting your config file.");
                }

                reader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        save();
    }

    public static void save() {
        try {
            Writer writer = Files.newBufferedWriter(getFilePath());
            Data data = new Data(
                    new Data.Common(
                            maxSamples,
                            radiusModifier,
                            sampleSpaceModifier,
                            pointToClosest,
                            fromIngredientMap(calibrationRecipes)
                    ),
                    new Data.Client(fixBiomeNames));
            gson.toJson(data, writer);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Path getFilePath() {
        if (configFilePath == null) {
            configFilePath = FabricLoader.getInstance().getConfigDir().resolve(NaturesCompass.MODID + ".json");
        }
        return configFilePath;
    }

    private static Map<Identifier, List<Ingredient>> toIngredientMap(List<Data.Common.CalibrationRecipe> data) {
        Map<Identifier, List<Ingredient>> result = new HashMap<>();

        for (Data.Common.CalibrationRecipe recipe : data) {
            Identifier biome = new Identifier(recipe.biomeIdentifier);
            List<Ingredient> ingredients = new ArrayList<>();

            for (String ingredient : recipe.ingredients) {
                Identifier item = new Identifier(ingredient);
                if (!Registries.ITEM.containsId(item)) {
                    NaturesCompass.LOGGER.error("Error in JSON config, no such item: {}", ingredient);
                }

                ingredients.add(Ingredient.ofItems(Registries.ITEM.get(item)));
            }

            result.put(biome, ingredients);
        }

        return result;
    }

    private static List<Data.Common.CalibrationRecipe> fromIngredientMap(Map<Identifier, List<Ingredient>> map) {
        List<Data.Common.CalibrationRecipe> result = new ArrayList<>();

        for (Map.Entry<Identifier, List<Ingredient>> entry : map.entrySet()) {
            List<String> ingredientIds = entry.getValue().stream().map((ingredient) ->
                    ingredient.toJson().getAsJsonObject().getAsJsonPrimitive("item").getAsString()).toList();

            result.add(new Data.Common.CalibrationRecipe(entry.getKey().toString(), ingredientIds));
        }

        return result;
    }

    private static Map<Identifier, List<Ingredient>> getDefaultRecipes() {
        Map<Identifier, List<Ingredient>> biomeIngredients = new HashMap<>();

        biomeIngredients.put(Identifier.of("minecraft", "ocean"), List.of(
                Ingredient.ofItems(Items.KELP),
                Ingredient.ofItems(Items.WATER_BUCKET)));
        biomeIngredients.put(Identifier.of("minecraft", "deep_ocean"), List.of(
                Ingredient.ofItems(Items.KELP),
                Ingredient.ofItems(Items.WATER_BUCKET),
                Ingredient.ofItems(Items.WATER_BUCKET)));
        biomeIngredients.put(Identifier.of("minecraft", "cold_ocean"), List.of(
                Ingredient.ofItems(Items.SALMON),
                Ingredient.ofItems(Items.WATER_BUCKET)));
        biomeIngredients.put(Identifier.of("minecraft", "deep_cold_ocean"), List.of(
                Ingredient.ofItems(Items.WATER_BUCKET),
                Ingredient.ofItems(Items.WATER_BUCKET)));
        biomeIngredients.put(Identifier.of("minecraft", "frozen_ocean"), List.of(
                Ingredient.ofItems(Items.SNOWBALL),
                Ingredient.ofItems(Items.WATER_BUCKET)));
        biomeIngredients.put(Identifier.of("minecraft", "deep_frozen_ocean"), List.of(
                Ingredient.ofItems(Items.SNOWBALL),
                Ingredient.ofItems(Items.WATER_BUCKET),
                Ingredient.ofItems(Items.WATER_BUCKET)));
        biomeIngredients.put(Identifier.of("minecraft", "lukewarm_ocean"), List.of(
                Ingredient.ofItems(Items.KELP),
                Ingredient.ofItems(Items.PUFFERFISH),
                Ingredient.ofItems(Items.WATER_BUCKET)));
        biomeIngredients.put(Identifier.of("minecraft", "deep_lukewarm_ocean"), List.of(
                Ingredient.ofItems(Items.KELP),
                Ingredient.ofItems(Items.WATER_BUCKET),
                Ingredient.ofItems(Items.WATER_BUCKET)));
        biomeIngredients.put(Identifier.of("minecraft", "warm_ocean"), List.of(
                Ingredient.ofItems(Items.SEA_PICKLE),
                Ingredient.ofItems(Items.WATER_BUCKET)));
        biomeIngredients.put(Identifier.of("minecraft", "river"), List.of(
                Ingredient.ofItems(Items.CLAY_BALL),
                Ingredient.ofItems(Items.WATER_BUCKET),
                Ingredient.ofItems(Items.SUGAR_CANE)));
        biomeIngredients.put(Identifier.of("minecraft", "frozen_river"), List.of(
                Ingredient.ofItems(Items.CLAY_BALL),
                Ingredient.ofItems(Items.WATER_BUCKET),
                Ingredient.ofItems(Items.SNOWBALL)));
        biomeIngredients.put(Identifier.of("minecraft", "plains"), List.of(Ingredient.ofItems(Items.WHEAT), Ingredient.ofItems(Items.HAY_BLOCK), Ingredient.ofItems(Items.POPPY)));
        biomeIngredients.put(Identifier.of("minecraft", "sunflower_plains"), List.of(Ingredient.ofItems(Items.SUNFLOWER)));
        biomeIngredients.put(Identifier.of("minecraft", "forest"), List.of(Ingredient.ofItems(Items.OAK_LOG), Ingredient.ofItems(Items.APPLE), Ingredient.ofItems(Items.RED_MUSHROOM)));
        biomeIngredients.put(Identifier.of("minecraft", "flower_forest"), List.of(Ingredient.ofItems(Items.ALLIUM), Ingredient.ofItems(Items.DANDELION)));
        biomeIngredients.put(Identifier.of("minecraft", "birch_forest"), List.of(Ingredient.ofItems(Items.BIRCH_LOG), Ingredient.ofItems(Items.BIRCH_SAPLING), Ingredient.ofItems(Items.ROSE_BUSH)));
        biomeIngredients.put(Identifier.of("minecraft", "dark_forest"), List.of(Ingredient.ofItems(Items.DARK_OAK_LOG), Ingredient.ofItems(Items.BROWN_MUSHROOM), Ingredient.ofItems(Items.APPLE)));
        biomeIngredients.put(Identifier.of("minecraft", "old_growth_birch_forest"), List.of(Ingredient.ofItems(Items.BIRCH_LOG), Ingredient.ofItems(Items.LARGE_FERN), Ingredient.ofItems(Items.OXEYE_DAISY)));
        biomeIngredients.put(Identifier.of("minecraft", "jungle"), List.of(Ingredient.ofItems(Items.COCOA_BEANS), Ingredient.ofItems(Items.MELON_SLICE), Ingredient.ofItems(Items.JUNGLE_LOG)));
        biomeIngredients.put(Identifier.of("minecraft", "sparse_jungle"), List.of(Ingredient.ofItems(Items.VINE), Ingredient.ofItems(Items.MELON_SLICE), Ingredient.ofItems(Items.COCOA_BEANS)));
        biomeIngredients.put(Identifier.of("minecraft", "bamboo_jungle"), List.of(Ingredient.ofItems(Items.BAMBOO), Ingredient.ofItems(Items.PANDA_SPAWN_EGG), Ingredient.ofItems(Items.PODZOL)));
        biomeIngredients.put(Identifier.of("minecraft", "taiga"), List.of(Ingredient.ofItems(Items.SPRUCE_LOG), Ingredient.ofItems(Items.SWEET_BERRIES), Ingredient.ofItems(Items.FERN)));
        biomeIngredients.put(Identifier.of("minecraft", "snowy_taiga"), List.of(Ingredient.ofItems(Items.SPRUCE_LOG), Ingredient.ofItems(Items.SNOWBALL), Ingredient.ofItems(Items.SWEET_BERRIES)));
        biomeIngredients.put(Identifier.of("minecraft", "savanna"), List.of(
                Ingredient.ofItems(Items.ACACIA_SAPLING)));
        biomeIngredients.put(Identifier.of("minecraft", "desert"), List.of(Ingredient.ofItems(Items.SAND), Ingredient.ofItems(Items.CACTUS), Ingredient.ofItems(Items.DEAD_BUSH)));
        biomeIngredients.put(Identifier.of("minecraft", "swamp"), List.of(Ingredient.ofItems(Items.MUD), Ingredient.ofItems(Items.LILY_PAD), Ingredient.ofItems(Items.SLIME_BALL)));
        biomeIngredients.put(Identifier.of("minecraft", "mangrove_swamp"), List.of(Ingredient.ofItems(Items.MANGROVE_LOG), Ingredient.ofItems(Items.MANGROVE_PROPAGULE), Ingredient.ofItems(Items.MUD)));
        biomeIngredients.put(Identifier.of("minecraft", "badlands"), List.of(Ingredient.ofItems(Items.RED_SAND), Ingredient.ofItems(Items.TERRACOTTA), Ingredient.ofItems(Items.GOLD_NUGGET)));
        biomeIngredients.put(Identifier.of("minecraft", "meadow"), List.of(Ingredient.ofItems(Items.CORNFLOWER), Ingredient.ofItems(Items.DANDELION), Ingredient.ofItems(Items.OXEYE_DAISY)));
        biomeIngredients.put(Identifier.of("minecraft", "cherry_grove"), List.of(
                Ingredient.ofItems(Items.CHERRY_SAPLING)));
        biomeIngredients.put(Identifier.of("minecraft", "snowy_plains"), List.of(
                Ingredient.ofItems(Items.SNOWBALL),
                Ingredient.ofItems(Items.DIRT)));
        biomeIngredients.put(Identifier.of("minecraft", "ice_spikes"), List.of(Ingredient.ofItems(Items.PACKED_ICE), Ingredient.ofItems(Items.BLUE_ICE), Ingredient.ofItems(Items.SNOWBALL)));
        biomeIngredients.put(Identifier.of("minecraft", "dripstone_caves"), List.of(
                Ingredient.ofItems(Items.POINTED_DRIPSTONE),
                Ingredient.ofItems(Items.DRIPSTONE_BLOCK)));
        biomeIngredients.put(Identifier.of("minecraft", "lush_caves"), List.of(
                Ingredient.ofItems(Items.MOSS_BLOCK),
                Ingredient.ofItems(Items.GLOW_BERRIES),
                Ingredient.ofItems(Items.AZALEA)));
        biomeIngredients.put(Identifier.of("minecraft", "windswept_forest"), List.of(
                Ingredient.ofItems(Items.OAK_SAPLING),
                Ingredient.ofItems(Items.GUNPOWDER)));
        biomeIngredients.put(Identifier.of("minecraft", "windswept_hills"), List.of(
                Ingredient.ofItems(Items.STONE),
                Ingredient.ofItems(Items.GUNPOWDER)));
        biomeIngredients.put(Identifier.of("minecraft", "windswept_gravelly_hills"), List.of(
                Ingredient.ofItems(Items.GRAVEL),
                Ingredient.ofItems(Items.GUNPOWDER)));
        biomeIngredients.put(Identifier.of("minecraft", "windswept_savanna"), List.of(
                Ingredient.ofItems(Items.ACACIA_SAPLING),
                Ingredient.ofItems(Items.GUNPOWDER)));

        return biomeIngredients;
    }

    private static class Data {

        private final Common common;
        private final Client client;

        public Data(Common common, Client client) {
            this.common = common;
            this.client = client;
        }

        private static class Common {
            private final String maxSamplesComment = "The maximum number of samples to be taken when searching for a biome.";
            private final int maxSamples;

            private final String radiusModifierComment = "biomeSize * radiusModifier = maxSearchRadius. Raising this value will increase search accuracy but will potentially make the process more resource intensive.";
            private final int radiusModifier;

            private final String sampleSpaceModifierComment = "biomeSize * sampleSpaceModifier = sampleSpace. Lowering this value will increase search accuracy but will make the process more resource intensive.";
            private final int sampleSpaceModifier;

            private final String pointToClosestBiomeComment = "Instead of calibrating the compass to a fixed biome, it will always point at the matching biome closest to the players position. Disable to improve performance.";
            private final boolean pointToClosestBiome;

            private final List<CalibrationRecipe> recipes;

            private Common(
                    int maxSamples,
                    int radiusModifier,
                    int sampleSpaceModifier,
                    boolean pointToClosest,
                    List<CalibrationRecipe> recipes) {
                this.maxSamples = maxSamples;
                this.radiusModifier = radiusModifier;
                this.sampleSpaceModifier = sampleSpaceModifier;
                this.pointToClosestBiome = pointToClosest;
                this.recipes = recipes;
            }

            private static class CalibrationRecipe {
                private final String biomeIdentifier;
                private final List<String> ingredients;

                private CalibrationRecipe(String biome, List<String> ingredients) {
                    this.biomeIdentifier = biome;
                    this.ingredients = ingredients;
                }
            }
        }

        private static class Client {
            private final String fixBiomeNamesComment = "Fixes biome names by adding missing spaces. Ex: ForestHills becomes Forest Hills";
            private final boolean fixBiomeNames;

            private Client() {
                fixBiomeNames = true;
            }

            private Client(boolean fixBiomeNames) {
                this.fixBiomeNames = fixBiomeNames;
            }
        }
    }

}
