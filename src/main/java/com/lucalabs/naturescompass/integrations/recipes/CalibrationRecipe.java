package com.lucalabs.naturescompass.integrations.recipes;

import com.lucalabs.naturescompass.NaturesCompass;
import mezz.jei.api.recipe.RecipeType;

public class CalibrationRecipe {
    public static RecipeType<CalibrationRecipe> TYPE = RecipeType.create(NaturesCompass.MOD_ID, "calibration", CalibrationRecipe.class);
}
