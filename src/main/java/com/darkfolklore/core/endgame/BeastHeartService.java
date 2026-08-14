package com.darkfolklore.core.endgame;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;
import java.util.UUID;

/** Marks the provider Demon Heart stack as the unique Beast Heart reward without registering another item. */
public final class BeastHeartService {
    public static final ResourceLocation DEMON_HEART = ResourceLocation.parse("the_day_of_the_beast:demon_heart");
    private static final String MARKER = "darkfolklore_beast_heart";
    private static final String BOSS = "darkfolklore_beast_boss";
    private static final String SLAYER = "darkfolklore_beast_slayer";
    private static final String CREATED = "darkfolklore_beast_created";

    private BeastHeartService() {}

    public static boolean isDemonHeart(ItemStack stack) {
        return !stack.isEmpty() && DEMON_HEART.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static boolean isBeastHeart(ItemStack stack) {
        if (!isDemonHeart(stack)) return false;
        CustomData custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return custom.copyTag().getBoolean(MARKER);
    }

    public static boolean isNormalDemonHeart(ItemStack stack) {
        return isDemonHeart(stack) && !isBeastHeart(stack);
    }

    public static Optional<ItemStack> create(UUID boss, UUID slayer, long gameTime) {
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(DEMON_HEART);
        if (item.isEmpty()) return Optional.empty();
        ItemStack stack = new ItemStack(item.get());
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(MARKER, true);
        if (boss != null) tag.putUUID(BOSS, boss);
        if (slayer != null) tag.putUUID(SLAYER, slayer);
        tag.putLong(CREATED, Math.max(0L, gameTime));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Beast Heart").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return Optional.of(stack);
    }
}
