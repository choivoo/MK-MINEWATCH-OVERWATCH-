package com.minewatch;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/** 영웅 무기 아이템. (모델은 임시: 바닐라 텍스처를 빌려 쓴다. GeckoLib 모델은 M8) */
public final class ModItems {
    public static final Item PULSE_PISTOLS = weapon("pulse_pistols");
    public static final Item PULSE_RIFLE = weapon("pulse_rifle");
    public static final Item SNIPER_RIFLE = weapon("sniper_rifle");
    public static final Item ROCKET_HAMMER = weapon("rocket_hammer");
    public static final Item SCRAP_GUN = weapon("scrap_gun");
    public static final Item BIOTIC_RIFLE = weapon("biotic_rifle");
    public static final Item CADUCEUS_STAFF = weapon("caduceus_staff");

    private static Item weapon(String id) {
        return Registry.register(Registries.ITEM, Identifier.of("minewatch", id), new HeroWeaponItem());
    }

    public static void init() {}
    private ModItems() {}
}
