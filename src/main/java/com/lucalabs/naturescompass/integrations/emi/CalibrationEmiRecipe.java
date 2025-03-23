package com.lucalabs.naturescompass.integrations.emi;

import com.lucalabs.naturescompass.NaturesCompass;
import com.lucalabs.naturescompass.recipes.CalibrationRecipe;
import com.lucalabs.naturescompass.utils.BiomeUtils;
import com.lucalabs.naturescompass.utils.TextUtils;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Optional;

public class CalibrationEmiRecipe implements EmiRecipe {

    public static WeakReference<World> world;

    private final Identifier id;
    private final Identifier biomeId;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;

    public CalibrationEmiRecipe(CalibrationRecipe r) {
        this.id = r.getId();
        this.biomeId = r.getBiomeId();
        this.inputs = r.getIngredients().stream().map(EmiIngredient::of).toList();
        this.outputs = List.of(EmiStack.of(r.getOutput(DynamicRegistryManager.EMPTY)));
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return EmiIntegration.CALIBRATION_CATEGORY;
    }

    @Override
    public @Nullable Identifier getId() {
        return id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return inputs;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return outputs;
    }

    @Override
    public int getDisplayWidth() {
        return 130;
    }

    @Override
    public int getDisplayHeight() {
        return 63;
    }

    @Override
    public boolean supportsRecipeTree() {
        return false;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 65, 24);
        widgets.addTexture(EmiTexture.PLUS, 6, 25);

        widgets.addSlot(EmiStack.of(NaturesCompass.NATURES_COMPASS_ITEM), 4, 4).recipeContext(this);

        for (int i = 0; i < 3; i++) {
            if (i < inputs.size()) {
                widgets.addSlot(inputs.get(i), 4 + 18 * i, 41).recipeContext(this);
            } else {
                widgets.addSlot(4 + 18 * i, 41);
            }
        }

        widgets.addSlot(outputs.get(0), 95, 19).recipeContext(this);

        drawBiomeHint(widgets);
    }

    private void drawBiomeHint(WidgetHolder widgets) {
        if (world == null) {
            return;
        }

        World w = world.get();
        if (w == null) {
            return;
        }

        Optional<Biome> b = BiomeUtils.getBiomeForIdentifier(w, biomeId);
        if (b.isEmpty()) {
            return;
        }

        String biomeName = BiomeUtils.getBiomeName(w, b.get());
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        String biomeNameTruncated = TextUtils.abbreviateText(textRenderer, biomeName, 100);

        int width = textRenderer.getWidth(biomeNameTruncated);
        int x = 128;
        int y = 3;

        widgets.addText(
                Text.literal(biomeNameTruncated).formatted(Formatting.ITALIC, Formatting.GRAY),
                x - width, // right-aligned
                y,
                7,
                false);
    }
}
