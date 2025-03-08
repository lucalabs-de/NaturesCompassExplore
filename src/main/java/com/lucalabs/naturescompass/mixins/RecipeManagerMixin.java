package com.lucalabs.naturescompass.mixins;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.lucalabs.naturescompass.NaturesCompass;
import com.lucalabs.naturescompass.recipes.CalibrationRecipe;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {
    @Shadow
    private Map<RecipeType<?>, Map<Identifier, Recipe<?>>> recipes;

    @Shadow
    private Map<Identifier, Recipe<?>> recipesById;

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)V", at = @At(value = "TAIL"))
    private void addConfigRecipes(Map<Identifier, JsonElement> map, ResourceManager resourceManager, Profiler profiler, CallbackInfo ci) {
        Identifier recipeIdentifier = Identifier.of(NaturesCompass.MODID, "replacebysomethingunique");

        CalibrationRecipe recipe = new CalibrationRecipe(
                List.of(Ingredient.ofItems(Items.REDSTONE)),
                Identifier.of("minecraft", "forest"),
                recipeIdentifier);

        // this is an immutable map, and calling .put on it would give a runtime exception... mega cringe
        Map<Identifier, Recipe<?>> calibrationRecipes =
                recipes.getOrDefault(CalibrationRecipe.Type.INSTANCE, Collections.emptyMap());

        // so let's copy
        Map<Identifier, Recipe<?>> calibrationRecipesMutable = new HashMap<>(calibrationRecipes);
        Map<RecipeType<?>, Map<Identifier, Recipe<?>>> recipesMutable = new HashMap<>(recipes);
        Map<Identifier, Recipe<?>> recipesByIdMutable = new HashMap<>(recipesById);

        // now we can add our new recipes
        recipesByIdMutable.put(recipeIdentifier, recipe);
        calibrationRecipesMutable.put(recipeIdentifier, recipe);
        recipesMutable.put(CalibrationRecipe.Type.INSTANCE, calibrationRecipesMutable);

        // overwrite existing recipes
        recipes = recipesMutable;
        recipesById = recipesByIdMutable;
    }
}
