package com.lucalabs.naturescompass.screens;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BiomeChoiceScreen extends HandledScreen<BiomeChoiceScreenHandler> {
    private static final Identifier BASE_TEXTURE = new Identifier("textures/gui/container/cartography_table.png");
    private static final Identifier BLANK_BACKGROUND = new Identifier("textures/gui/demo_background.png");

    public BiomeChoiceScreen(BiomeChoiceScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        --this.titleY;
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        this.renderBackground(context);
        int i = this.x;
        int j = this.y;
        context.drawTexture(BASE_TEXTURE, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight);

        // cover up existing slots
        context.drawTexture(BLANK_BACKGROUND, i, j, 0, 0, this.backgroundWidth - 3, this.backgroundHeight / 2 - 10);

        final int offset = 16;

        // draw slots
        context.drawTexture(BASE_TEXTURE, i + offset + 14, j + 14, 14, 14, 18, 18);
        context.drawTexture(BASE_TEXTURE, i + offset + 14, j + 51, 14, 14, 18, 18);
        context.drawTexture(BASE_TEXTURE, i + offset + 32, j + 51, 14, 14, 18, 18);
        context.drawTexture(BASE_TEXTURE, i + offset + 50, j + 51, 14, 14, 18, 18);

        // draw plus
        context.drawTexture(BASE_TEXTURE, i + offset + 16, j + 35, 16, 35, 13, 13);

        // draw arrow
        context.drawTexture(BASE_TEXTURE, i + offset + 75, j + 34, 38, 34, 22, 15);

        // draw output slot
        context.drawTexture(BASE_TEXTURE, i + offset + 105, j + 29, 140, 34, 26, 26);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return true;
    }
}
