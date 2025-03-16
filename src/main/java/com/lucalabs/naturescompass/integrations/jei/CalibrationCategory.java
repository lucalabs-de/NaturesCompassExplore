package com.lucalabs.naturescompass.integrations.jei;

import com.lucalabs.naturescompass.NaturesCompass;
import com.lucalabs.naturescompass.integrations.recipes.CalibrationRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CalibrationCategory implements IRecipeCategory<CalibrationRecipe> {

    public static final RecipeType<CalibrationRecipe> TYPE = new RecipeType<>(
            new Identifier(NaturesCompass.MOD_ID, "calibration"), CalibrationRecipe.class);

    private final IDrawable icon;

    public CalibrationCategory(IGuiHelper helper) {
        icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Items.CARTOGRAPHY_TABLE));
    }

    @Override
    public RecipeType<CalibrationRecipe> getRecipeType() {
        return CalibrationRecipe.TYPE;
    }

    @Override
    public Text getTitle() {
        return null;
    }

    @Override
    public IDrawable getBackground() {
        return null;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CalibrationRecipe recipe, IFocusGroup focuse) {
    }
}
