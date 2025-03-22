package com.lucalabs.naturescompass.integrations.jei;

import com.lucalabs.naturescompass.NaturesCompass;
import com.lucalabs.naturescompass.config.NaturesCompassConfig;
import com.lucalabs.naturescompass.recipes.CalibrationRecipe;
import com.lucalabs.naturescompass.utils.ItemUtils;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@MethodsReturnNonnullByDefault
@JeiPlugin
public class JeiIntegration implements IModPlugin {

    public static RecipeType<CalibrationRecipe> CALIBRATION_RECIPE = RecipeType.create(NaturesCompass.MOD_ID, "calibration", CalibrationRecipe.class);

    @Override
    public @NotNull Identifier getPluginUid() {
        return new Identifier(NaturesCompass.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper helper = registration.getJeiHelpers().getGuiHelper();

        registration.addRecipeCategories(
                new CalibrationCategory(helper)
        );
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        // TODO get the actual registered recipes to avoid JEI warnings
        registration.addRecipes(
                CALIBRATION_RECIPE,
                NaturesCompassConfig.calibrationRecipes.entrySet().stream().sorted(this::compareCalibrationRecipes).map(e -> new CalibrationRecipe(
                        e.getValue().stream().sorted(this::compareIngredients).toList(),
                        e.getKey(),
                        Identifier.of(NaturesCompass.MOD_ID, e.getKey().getPath()))).toList());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Items.CARTOGRAPHY_TABLE), CALIBRATION_RECIPE);
    }

    private int compareIngredients(Ingredient i1, Ingredient i2) {
        String i1Name = ItemUtils.getComparableIngredientName(i1);
        String i2Name = ItemUtils.getComparableIngredientName(i2);

        return i1Name.compareTo(i2Name);
    }

    private int compareCalibrationRecipes(
            Map.Entry<Identifier, List<Ingredient>> e1,
            Map.Entry<Identifier, List<Ingredient>> e2) {
        List<Ingredient> i1 = e1.getValue();
        List<Ingredient> i2 = e2.getValue();

        if (i1.isEmpty()) {
            return -1;
        }

        if (i2.isEmpty()) {
            return 1;
        }

        int sizeDiff = i1.size() - i2.size();

        for (int i = 0; i < Math.min(i1.size(), i2.size()); i++) {
            if (compareIngredients(i1.get(i), i2.get(i)) != 0) {
                return compareIngredients(i1.get(i), i2.get(i));
            }

            if (sizeDiff != 0) {
                return sizeDiff;
            }
        }

        return 0;
    }

}
