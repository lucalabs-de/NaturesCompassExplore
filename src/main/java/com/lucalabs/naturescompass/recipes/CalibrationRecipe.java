package com.lucalabs.naturescompass.recipes;

import com.lucalabs.naturescompass.NaturesCompass;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class CalibrationRecipe implements Recipe<CraftingInventory> {

    private final List<Ingredient> ingredients;
    private final Identifier biomeId;
    private final Identifier id;

    public CalibrationRecipe(List<Ingredient> ingredients, Identifier biomeId, Identifier id) {
        assert ingredients.size() <= 3;

        this.ingredients = ingredients;
        this.biomeId = biomeId;
        this.id = id;
    }

    @Override
    public boolean matches(CraftingInventory inventory, World world) {
        if (inventory.getWidth() != 2 && inventory.getHeight() != 2) {
            return false;
        }

        List<ItemStack> inputs = inventory.getInputStacks();

        for (Ingredient i : ingredients) {
           Optional<ItemStack> match = inputs.stream().filter(i).findFirst();
           if (match.isEmpty()) {
               return false;
           }
           inputs.remove(match.get());
        }

        // the ingredients list doesn't contain the actual compass, which we want to be present exactly once
        return inputs.size() == 1 && inputs.get(0).isOf(NaturesCompass.NATURES_COMPASS_ITEM);
    }

    @Override
    public ItemStack getOutput(DynamicRegistryManager registryManager) {
        return null;
    }

    @Override
    public Identifier getId() {
        return null;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CalibrationRecipeSerializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    @Override
    public ItemStack craft(CraftingInventory inventory, DynamicRegistryManager registryManager) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean fits(int width, int height) {
        return true;
    }

    public List<Ingredient> getIngredientList() {
       return this.ingredients;
    }

    public Identifier getBiomeId() {
        return this.biomeId;
    }

    public static class Type implements RecipeType<CalibrationRecipe> {
        public static final Type INSTANCE = new Type();
        // This will be needed in step 4
        public static final String ID = "biome_choice";

        private Type() {
        }
    }
}
