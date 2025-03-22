package com.lucalabs.naturescompass.integrations.jei;

import com.lucalabs.naturescompass.NaturesCompass;
import com.lucalabs.naturescompass.recipes.CalibrationRecipe;
import com.lucalabs.naturescompass.utils.BiomeUtils;
import com.lucalabs.naturescompass.utils.TextUtils;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.annotation.MethodsReturnNonnullByDefault;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Optional;

@MethodsReturnNonnullByDefault
public class CalibrationCategory implements IRecipeCategory<CalibrationRecipe> {

    // this is a bit of a hack, I need the world to get the human-readable biome name. The weak reference avoids
    // creating memory leaks.
    public static WeakReference<World> world;

    private static final Identifier BASE_TEXTURE = new Identifier("textures/gui/container/cartography_table.png");
    private static final Identifier BLANK_BACKGROUND = new Identifier("textures/gui/demo_background.png");

    private final IDrawable icon;
    private final IDrawable arrow;
    private final IDrawable plus;
    private final IDrawable slot;
    private final IDrawable bigSlot;

    private final IGuiHelper helper;

    public CalibrationCategory(IGuiHelper helper) {
        this.helper = helper;

        icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Items.CARTOGRAPHY_TABLE));
        arrow = helper.createDrawable(BASE_TEXTURE, 38, 34, 22, 15);
        slot = helper.createDrawable(BASE_TEXTURE, 14, 14, 18, 18);
        bigSlot = helper.createDrawable(BASE_TEXTURE, 140, 34, 26, 26);
        plus = helper.createDrawable(BASE_TEXTURE, 16, 35, 13, 13);
    }

    @Override
    public RecipeType<CalibrationRecipe> getRecipeType() {
        return JeiIntegration.CALIBRATION_RECIPE;
    }

    @Override
    public Text getTitle() {
        return Text.translatable("string.naturescompass.calibration");
    }

    @Override
    public IDrawable getBackground() {
        return helper.createDrawable(BLANK_BACKGROUND, 10, 10, 130, 63);
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CalibrationRecipe recipe, IFocusGroup focus) {
        DefaultedList<Ingredient> ingredients = recipe.getIngredients();
        ItemStack resultStack = recipe.getOutput(DynamicRegistryManager.EMPTY);

        builder.addSlot(RecipeIngredientRole.INPUT, 5, 5).addItemStack(new ItemStack(NaturesCompass.NATURES_COMPASS_ITEM));

        for (int i = 0; i < Math.min(ingredients.size(), 3); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, 5 + 18 * i, 42)
                    .addItemStacks(List.of(ingredients.get(i).getMatchingStacks()));
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, 100, 24).addItemStack(resultStack);
    }

    @Override
    public void draw(CalibrationRecipe recipe, IRecipeSlotsView slotsView, DrawContext matrixStack, double mouseX, double mouseY) {
        arrow.draw(matrixStack, 65, 24);
        plus.draw(matrixStack, 6, 25);
        slot.draw(matrixStack, 4, 4);
        slot.draw(matrixStack, 4, 41);
        slot.draw(matrixStack, 22, 41);
        slot.draw(matrixStack, 40, 41);
        bigSlot.draw(matrixStack, 95, 19);
        drawBiomeHint(recipe, matrixStack);
    }

    private void drawBiomeHint(CalibrationRecipe recipe, DrawContext matrixStack) {
        if (world == null) {
            return;
        }

        World w = world.get();
        if (w == null) {
           return;
        }

        Optional<Biome> b = BiomeUtils.getBiomeForIdentifier(w, recipe.getBiomeId());
        if (b.isEmpty()) {
            return;
        }

        String biomeName = BiomeUtils.getBiomeName(w, b.get());
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        String biomeNameTruncated = TextUtils.abbreviateText(textRenderer, biomeName, 100);

        int width = textRenderer.getWidth(biomeNameTruncated);
        int x = 128;
        int y = 3;

        matrixStack.drawText(
                textRenderer,
                Text.literal(biomeNameTruncated).formatted(Formatting.ITALIC, Formatting.GRAY),
                x - width, // right-aligned
                y,
                7,
                false);
    }
}
