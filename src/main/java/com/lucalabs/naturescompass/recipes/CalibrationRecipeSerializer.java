package com.lucalabs.naturescompass.recipes;

import com.google.gson.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class CalibrationRecipeSerializer implements RecipeSerializer<CalibrationRecipe> {

    public static final CalibrationRecipeSerializer INSTANCE = new CalibrationRecipeSerializer();
    public static final Identifier ID = Identifier.of("naturescompass", "biome_calibration");

    private CalibrationRecipeSerializer() {
    }

    @Override
    public CalibrationRecipe read(Identifier id, JsonObject json) {
        CalibrationRecipeJsonFormat recipeJson = new Gson().fromJson(json, CalibrationRecipeJsonFormat.class);

        if (recipeJson.inputs == null || recipeJson.biome == null) {
            throw new JsonSyntaxException("A required attribute is missing!");
        }

        Identifier biomeId = new Identifier(recipeJson.biome);
        List<Ingredient> ingredients = new ArrayList<>();

        for (JsonElement ingredient : recipeJson.inputs) {
            ingredients.add(Ingredient.fromJson(ingredient));
        }

        return new CalibrationRecipe(ingredients, biomeId, id);
    }

    @Override
    public CalibrationRecipe read(Identifier id, PacketByteBuf buf) {
        Identifier biomeId = buf.readIdentifier();
        List<Ingredient> ingredients = buf.readList(Ingredient::fromPacket);

        return new CalibrationRecipe(ingredients, biomeId, id);
    }

    @Override
    public void write(PacketByteBuf buf, CalibrationRecipe recipe) {
        buf.writeIdentifier(recipe.getBiomeId());
        buf.writeCollection(
                recipe.getIngredientList(),
                (PacketByteBuf b, Ingredient i) -> { i.write(b); });
    }

    static class CalibrationRecipeJsonFormat {
        JsonArray inputs;
        String biome;
    }
}
