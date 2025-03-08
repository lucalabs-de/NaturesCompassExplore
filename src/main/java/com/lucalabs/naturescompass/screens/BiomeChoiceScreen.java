package com.lucalabs.naturescompass.screens;

import com.lucalabs.naturescompass.utils.BiomeUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

public class BiomeChoiceScreen extends HandledScreen<BiomeChoiceScreenHandler> {
    private static final Identifier TEXTURE = new Identifier("textures/gui/container/stonecutter.png");
    private static final Item DEFAULT_ICON = Items.GRASS_BLOCK;
    private static final Map<Identifier, Item> BIOME_ICONS = Map.<Identifier, Item>ofEntries(
            entry(new Identifier("minecraft", "plains"), Items.GRASS_BLOCK),
            entry(new Identifier("minecraft", "snowy_plains"), Items.SNOW_BLOCK),
            entry(new Identifier("minecraft", "sunflower_plains"), Items.SUNFLOWER),
            entry(new Identifier("minecraft", "meadow"), Items.CORNFLOWER),
            entry(new Identifier("minecraft", "forest"), Items.OAK_SAPLING),
            entry(new Identifier("minecraft", "flower_forest"), Items.ALLIUM),
            entry(new Identifier("minecraft", "birch_forest"), Items.BIRCH_SAPLING),
            entry(new Identifier("minecraft", "old_growth_birch_forest"), Items.BIRCH_LOG),
            entry(new Identifier("minecraft", "dark_forest"), Items.DARK_OAK_SAPLING),
            entry(new Identifier("minecraft", "jungle"), Items.JUNGLE_SAPLING),
            entry(new Identifier("minecraft", "sparse_jungle"), Items.JUNGLE_SAPLING),
            entry(new Identifier("minecraft", "bamboo_jungle"), Items.BAMBOO),
            entry(new Identifier("minecraft", "taiga"), Items.SPRUCE_SAPLING),
            entry(new Identifier("minecraft", "snowy_taiga"), Items.SNOW_BLOCK),
            entry(new Identifier("minecraft", "old_growth_pine_taiga"), Items.SPRUCE_LOG),
            entry(new Identifier("minecraft", "old_growth_spruce_taiga"), Items.SPRUCE_LOG),
            entry(new Identifier("minecraft", "savanna"), Items.ACACIA_SAPLING),
            entry(new Identifier("minecraft", "savanna_plateau"), Items.TERRACOTTA),
            entry(new Identifier("minecraft", "windswept_savanna"), Items.ACACIA_LOG),
            entry(new Identifier("minecraft", "desert"), Items.CACTUS),
            entry(new Identifier("minecraft", "badlands"), Items.RED_SAND),
            entry(new Identifier("minecraft", "eroded_badlands"), Items.ORANGE_TERRACOTTA),
            entry(new Identifier("minecraft", "wooded_badlands"), Items.DEAD_BUSH),
            entry(new Identifier("minecraft", "windswept_hills"), Items.STONE),
            entry(new Identifier("minecraft", "windswept_gravelly_hills"), Items.GRAVEL),
            entry(new Identifier("minecraft", "windswept_forest"), Items.OAK_LOG),
            entry(new Identifier("minecraft", "swamp"), Items.LILY_PAD),
            entry(new Identifier("minecraft", "mangrove_swamp"), Items.MANGROVE_PROPAGULE),
            entry(new Identifier("minecraft", "river"), Items.WATER_BUCKET),
            entry(new Identifier("minecraft", "frozen_river"), Items.ICE),
            entry(new Identifier("minecraft", "beach"), Items.SAND),
            entry(new Identifier("minecraft", "snowy_beach"), Items.SNOWBALL),
            entry(new Identifier("minecraft", "stony_shore"), Items.COBBLESTONE),
            entry(new Identifier("minecraft", "ice_spikes"), Items.PACKED_ICE),
            entry(new Identifier("minecraft", "mushroom_fields"), Items.RED_MUSHROOM),
            entry(new Identifier("minecraft", "dripstone_caves"), Items.DRIPSTONE_BLOCK),
            entry(new Identifier("minecraft", "lush_caves"), Items.MOSS_BLOCK),
            entry(new Identifier("minecraft", "deep_dark"), Items.SCULK),
            entry(new Identifier("minecraft", "grove"), Items.POWDER_SNOW_BUCKET),
            entry(new Identifier("minecraft", "snowy_slopes"), Items.SNOW),
            entry(new Identifier("minecraft", "jagged_peaks"), Items.PACKED_ICE),
            entry(new Identifier("minecraft", "frozen_peaks"), Items.SNOW_BLOCK),
            entry(new Identifier("minecraft", "stony_peaks"), Items.STONE),
            entry(new Identifier("minecraft", "cherry_grove"), Items.CHERRY_SAPLING),
            entry(new Identifier("minecraft", "ocean"), Items.WATER_BUCKET),
            entry(new Identifier("minecraft", "deep_ocean"), Items.PRISMARINE_SHARD),
            entry(new Identifier("minecraft", "warm_ocean"), Items.TROPICAL_FISH_BUCKET),
            entry(new Identifier("minecraft", "lukewarm_ocean"), Items.SEAGRASS),
            entry(new Identifier("minecraft", "deep_lukewarm_ocean"), Items.SEAGRASS),
            entry(new Identifier("minecraft", "cold_ocean"), Items.COD_BUCKET),
            entry(new Identifier("minecraft", "deep_cold_ocean"), Items.COD_BUCKET),
            entry(new Identifier("minecraft", "frozen_ocean"), Items.BLUE_ICE),
            entry(new Identifier("minecraft", "deep_frozen_ocean"), Items.BLUE_ICE)
    );

    private float scrollAmount;
    private boolean mouseClicked;
    private int scrollOffset;
    private boolean canChoose;

    public BiomeChoiceScreen(BiomeChoiceScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        handler.setContentsChangedListener(this::onInventoryChange);
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
        context.drawTexture(TEXTURE, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight);
        int k = (int) (41.0F * this.scrollAmount);
        context.drawTexture(TEXTURE, i + 119, j + 15 + k, 176 + (this.shouldScroll() ? 0 : 12), 0, 12, 15);
        int l = this.x + 52;
        int m = this.y + 14;
        int n = this.scrollOffset + 12;
        this.renderRecipeBackground(context, mouseX, mouseY, l, m, n);
        this.renderRecipeIcons(context, l, m, n);
    }

    private void renderRecipeBackground(DrawContext context, int mouseX, int mouseY, int x, int y, int scrollOffset) {
        if (this.canChoose) {
            for (int i = this.scrollOffset; i < scrollOffset && i < this.handler.getAvailableBiomesCount(); ++i) {
                int j = i - this.scrollOffset;
                int k = x + j % 4 * 16;
                int l = j / 4;
                int m = y + l * 18 + 2;
                int n = this.backgroundHeight;
                if (i == this.handler.getSelectedBiome()) {
                    n += 18;
                } else if (mouseX >= k && mouseY >= m && mouseX < k + 16 && mouseY < m + 18) {
                    n += 36;
                }

                context.drawTexture(TEXTURE, k, m - 1, 0, n, 16, 18);
            }
        }
    }

    private void renderRecipeIcons(DrawContext context, int x, int y, int scrollOffset) {
        if (this.canChoose) {
            for (int i = this.scrollOffset; i < scrollOffset && i < this.handler.getAvailableBiomesCount(); ++i) {
                int j = i - this.scrollOffset;
                int k = x + j % 4 * 16;
                int l = j / 4;
                int m = y + l * 18 + 2;
                context.drawItem(getBiomeItem(i), k, m);
            }
        }
    }

    protected void drawMouseoverTooltip(DrawContext context, int x, int y) {
        super.drawMouseoverTooltip(context, x, y);
        if (this.canChoose) {
            int i = this.x + 52;
            int j = this.y + 14;
            int k = this.scrollOffset + 12;
            List<Biome> list = this.handler.getAvailableBiomes();

            for (int l = this.scrollOffset; l < k && l < this.handler.getAvailableBiomesCount(); ++l) {
                int m = l - this.scrollOffset;
                int n = i + m % 4 * 16;
                int o = j + m / 4 * 18 + 2;
                if (x >= n && x < n + 16 && y >= o && y < o + 18) {
                    context.drawTooltip(
                            this.textRenderer,
                            Text.literal(BiomeUtils.getBiomeNameForDisplay(this.client.world, list.get(l))), x, y);
                }
            }
        }

    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.mouseClicked = false;
        if (this.canChoose) {
            int i = this.x + 52;
            int j = this.y + 14;
            int k = this.scrollOffset + 12;

            for (int l = this.scrollOffset; l < k; ++l) {
                int m = l - this.scrollOffset;
                double d = mouseX - (double) (i + m % 4 * 16);
                double e = mouseY - (double) (j + m / 4 * 18);
                if (d >= 0.0 && e >= 0.0 && d < 16.0 && e < 18.0 && this.handler.onButtonClick(this.client.player, l)) {
                    MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
                    this.client.interactionManager.clickButton(this.handler.syncId, l);
                    return true;
                }
            }

            i = this.x + 119;
            j = this.y + 9;
            if (mouseX >= (double) i && mouseX < (double) (i + 12) && mouseY >= (double) j && mouseY < (double) (j + 54)) {
                this.mouseClicked = true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.mouseClicked && this.shouldScroll()) {
            int i = this.y + 14;
            int j = i + 54;
            this.scrollAmount = ((float) mouseY - (float) i - 7.5F) / ((float) (j - i) - 15.0F);
            this.scrollAmount = MathHelper.clamp(this.scrollAmount, 0.0F, 1.0F);
            this.scrollOffset = (int) ((double) (this.scrollAmount * (float) this.getMaxScroll()) + 0.5) * 4;
            return true;
        } else {
            return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (this.shouldScroll()) {
            int i = this.getMaxScroll();
            float f = (float) amount / (float) i;
            this.scrollAmount = MathHelper.clamp(this.scrollAmount - f, 0.0F, 1.0F);
            this.scrollOffset = (int) ((double) (this.scrollAmount * (float) i) + 0.5) * 4;
        }

        return true;
    }

    private boolean shouldScroll() {
        return this.handler.getAvailableBiomes().size() > 12;
    }

    protected int getMaxScroll() {
        return ((this.handler).getAvailableBiomes().size() + 4 - 1) / 4 - 3;
    }

    private void onInventoryChange() {
        this.canChoose = this.handler.getAreBiomesChoosable();
        if (!this.canChoose) {
            this.scrollAmount = 0.0F;
            this.scrollOffset = 0;
        }
    }

    private ItemStack getBiomeItem(int i) {
        Biome b = this.handler.getAvailableBiomes().get(i);
        if (b != null) {
            Item icon = BIOME_ICONS.getOrDefault(this.handler.getBiomeIdentifierAt(i), DEFAULT_ICON);
            return new ItemStack(icon);
        }

        return ItemStack.EMPTY;
    }
}
