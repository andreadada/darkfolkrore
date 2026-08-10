package com.darkfolklore.core.traits;

import com.darkfolklore.core.DarkFolkloreCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

import java.util.EnumMap;
import java.util.Map;

public final class FolkloreTags {
    private static final Map<ItemTrait, TagKey<Item>> ITEM_TAGS;
    private static final Map<CreatureTrait, TagKey<EntityType<?>>> ENTITY_TAGS;

    static {
        EnumMap<ItemTrait, TagKey<Item>> itemTags = new EnumMap<>(ItemTrait.class);
        for (ItemTrait trait : ItemTrait.values()) {
            itemTags.put(trait, TagKey.create(Registries.ITEM, id(trait.name().toLowerCase())));
        }
        ITEM_TAGS = Map.copyOf(itemTags);

        EnumMap<CreatureTrait, TagKey<EntityType<?>>> entityTags = new EnumMap<>(CreatureTrait.class);
        for (CreatureTrait trait : CreatureTrait.values()) {
            entityTags.put(trait, TagKey.create(Registries.ENTITY_TYPE, id(trait.name().toLowerCase())));
        }
        ENTITY_TAGS = Map.copyOf(entityTags);
    }

    private FolkloreTags() {}

    public static TagKey<Item> item(ItemTrait trait) { return ITEM_TAGS.get(trait); }
    public static TagKey<EntityType<?>> entity(CreatureTrait trait) { return ENTITY_TAGS.get(trait); }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(DarkFolkloreCore.MOD_ID, path);
    }
}
