package com.lucalabs.naturescompass.integrations.emi;

import com.lucalabs.naturescompass.NaturesCompass;
import com.lucalabs.naturescompass.recipes.CalibrationRecipe;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.util.Identifier;

public class EmiIntegration implements EmiPlugin {

    public static final EmiStack CARTOGRAPHY_WORKSTATION = EmiStack.of(Items.CARTOGRAPHY_TABLE);

    public static final EmiRecipeCategory CALIBRATION_CATEGORY
            = new EmiRecipeCategory(Identifier.of(NaturesCompass.MOD_ID, "calibration"), CARTOGRAPHY_WORKSTATION);


    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(CALIBRATION_CATEGORY);
        registry.addWorkstation(CALIBRATION_CATEGORY, CARTOGRAPHY_WORKSTATION);

        RecipeManager recipeManager = registry.getRecipeManager();
        for (CalibrationRecipe r : recipeManager.listAllOfType(CalibrationRecipe.Type.INSTANCE)) {
            registry.addRecipe(new CalibrationEmiRecipe(r));
        }
    }
}
