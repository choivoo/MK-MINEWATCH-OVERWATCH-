package com.minewatch;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModItems {
    public static final Item PULSE_PISTOLS = Registry.register(Registries.ITEM,
            Identifier.of("minewatch", "pulse_pistols"), new Item(new Item.Settings().maxCount(1)));
    public static void init() {}
    private ModItems() {}
}
