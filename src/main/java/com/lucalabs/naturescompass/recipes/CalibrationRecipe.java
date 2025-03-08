package com.lucalabs.naturescompass.recipes;

import com.lucalabs.naturescompass.NaturesCompass;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

public class CalibrationRecipe implements Recipe<SimpleInventory> {

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
    public boolean matches(SimpleInventory inventory, World world) {
        if (inventory.size() != 4) {
            return false;
        }

        List<ItemStack> inputs = inventory.stacks.stream()
                .filter(not(ItemStack::isEmpty))
                .collect(Collectors.toCollection(ArrayList::new));

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
        ItemStack output = new ItemStack(NaturesCompass.NATURES_COMPASS_ITEM);
        NaturesCompass.NATURES_COMPASS_ITEM.setBiomeId(output, biomeId);
        return output;
    }

    @Override
    public Identifier getId() {
        return this.id;
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
    public ItemStack craft(SimpleInventory inventory, DynamicRegistryManager registryManager) {
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
        public static final String ID = "biome_choice";

        private Type() {
        }
    }
}
