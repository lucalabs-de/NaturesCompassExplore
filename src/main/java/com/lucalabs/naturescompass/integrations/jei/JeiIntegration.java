package com.lucalabs.naturescompass.integrations.jei;

import com.lucalabs.naturescompass.NaturesCompass;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

@JeiPlugin
public class JeiIntegration implements IModPlugin {
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
        RegistrationUtils.registerCalibrationRecipes(registration);
    }
}
