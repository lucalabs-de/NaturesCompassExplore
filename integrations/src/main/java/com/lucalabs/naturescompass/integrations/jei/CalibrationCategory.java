package com.lucalabs.naturescompass.integrations.jei;

import com.lucalabs.naturescompass.integrations.Constants;
import com.lucalabs.naturescompass.integrations.recipes.CalibrationRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class CalibrationCategory extends AbstractRecipeCategory<CalibrationRecipe> implements IRecipeCategory<CalibrationRecipe> {

    public static final RecipeType<CalibrationRecipe> TYPE = new RecipeType<>(
            new ResourceLocation(Constants.MOD_ID, "calibration"), CalibrationRecipe.class);

    private final IDrawableStatic bigSlot;

    public CalibrationCategory(IGuiHelper helper, Item itemStack) {
        super(TYPE, Component.translatable("translation.naturescompass.calibration"), helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(itemStack)), 151 + 15, 91);
        bigSlot = helper.getOutputSlot();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder iRecipeLayoutBuilder, CalibrationRecipe calibrationRecipe, IFocusGroup iFocusGroup) {

    }
}
