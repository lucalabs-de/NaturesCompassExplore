package com.lucalabs.naturescompass.integrations.jei;

import com.lucalabs.naturescompass.integrations.Constants;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

@JeiPlugin
public class JeiIntegration implements IModPlugin {
    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return new ResourceLocation(Constants.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper helper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new CalibrationCategory(helper, Items.CARTOGRAPHY_TABLE)
        );
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        RegistrationUtils.registerCalibrationRecipes(registration);
    }
}
