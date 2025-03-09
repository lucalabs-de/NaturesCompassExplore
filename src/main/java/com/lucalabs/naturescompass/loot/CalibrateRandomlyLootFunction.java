package com.lucalabs.naturescompass.loot;

import com.google.common.collect.Lists;
import com.google.gson.*;
import com.lucalabs.naturescompass.NaturesCompass;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.ConditionalLootFunction;
import net.minecraft.loot.function.CopyNbtLootFunction;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.loot.provider.nbt.LootNbtProvider;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CalibrateRandomlyLootFunction extends ConditionalLootFunction {

    public static final String ID = "calibrate_randomly";

    private final List<Identifier> availableBiomes;

    protected CalibrateRandomlyLootFunction(LootCondition[] conditions, List<Identifier> availableBiomes) {
        super(conditions);
        this.availableBiomes = availableBiomes;
    }

    @Override
    protected ItemStack process(ItemStack stack, LootContext context) {
        Random random = new Random();
        if (availableBiomes.isEmpty() || !stack.isOf(NaturesCompass.NATURES_COMPASS_ITEM)) {
            return stack;
        }

        Identifier chosenBiome = availableBiomes.get(random.nextInt(availableBiomes.size()));
        NaturesCompass.NATURES_COMPASS_ITEM.setBiomeId(stack, chosenBiome);

        // TODO potentially search for biome here already?

        return stack;
    }

    @Override
    public LootFunctionType getType() {
        return NaturesCompass.CALIBRATE_RANDOMLY_LOOT_FUNCTION;
    }

    public static CalibrateRandomlyLootFunction.Builder builder(List<Identifier> biomes) {
        return new Builder(biomes);
    }

    public static class Serializer extends ConditionalLootFunction.Serializer<CalibrateRandomlyLootFunction> {
        public Serializer() {
        }

        public void toJson(JsonObject jsonObject, CalibrateRandomlyLootFunction lootFunction, JsonSerializationContext jsonSerializationContext) {
            super.toJson(jsonObject, lootFunction, jsonSerializationContext);
            JsonArray jsonArray = new JsonArray();
            lootFunction.availableBiomes.stream().map(Identifier::toString).forEach(jsonArray::add);
            jsonObject.add("availableBiomes", jsonArray);
        }

        public CalibrateRandomlyLootFunction fromJson(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootCondition[] lootConditions) {
            JsonArray biomes = jsonObject.getAsJsonArray("availableBiomes");
            List<Identifier> availableBiomes = new ArrayList<>();

            for (JsonElement e : biomes) {
                availableBiomes.add(new Identifier(e.getAsString()));
            }

            return new CalibrateRandomlyLootFunction(lootConditions, availableBiomes);
        }
    }

    public static class Builder extends ConditionalLootFunction.Builder<CalibrateRandomlyLootFunction.Builder> {
        private final List<Identifier> availableBiomes;

        Builder(List<Identifier> availableBiomes) {
            this.availableBiomes = availableBiomes;
        }

        protected CalibrateRandomlyLootFunction.Builder getThisBuilder() {
            return this;
        }

        public LootFunction build() {
            return new CalibrateRandomlyLootFunction(this.getConditions(), this.availableBiomes);
        }
    }
}
