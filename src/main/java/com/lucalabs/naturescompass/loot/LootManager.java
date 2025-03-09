package com.lucalabs.naturescompass.loot;

import com.lucalabs.naturescompass.NaturesCompass;
import com.lucalabs.naturescompass.config.NaturesCompassConfig;
import net.fabricmc.fabric.api.loot.v2.FabricLootTableBuilder;
import net.fabricmc.fabric.api.loot.v2.LootTableSource;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.util.List;

public abstract class LootManager {
    private static final List<Identifier> lootTables = List.of(
            LootTables.SIMPLE_DUNGEON_CHEST,
            LootTables.DESERT_PYRAMID_CHEST,
            LootTables.JUNGLE_TEMPLE_CHEST,
            LootTables.IGLOO_CHEST_CHEST
    );

    public static void addCompassesToLootTables(
            ResourceManager resourceManager,
            net.minecraft.loot.LootManager lootManager,
            Identifier id,
            FabricLootTableBuilder tableBuilder,
            LootTableSource source) {
        if (source.isBuiltin() && lootTables.contains(id)) {
            tableBuilder.pool(buildCompassLootPool().build());
        }
    }

    private static LootPool.Builder buildCompassLootPool() {
        return LootPool.builder()
                .with(ItemEntry.builder(NaturesCompass.NATURES_COMPASS_ITEM)
                        .conditionally(RandomChanceLootCondition.builder(NaturesCompassConfig.lootChance))
                        .apply(CalibrateRandomlyLootFunction
                                .builder(NaturesCompassConfig.lootableBiomes)
                                .conditionally(RandomChanceLootCondition.builder(.95f)))); // also include uncalibrated compasses, but rarely
    }

}
