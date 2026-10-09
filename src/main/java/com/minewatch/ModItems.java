package com.minewatch;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/** 영웅 무기 아이템. 모두 GeckoLib 모델(1인칭 팔 + 무기)로 그려진다. */
public final class ModItems {
    public static final Item PULSE_PISTOLS = geo("pulse_pistols", new GeoWeaponItem("tracer_arms", "tracer_arms", "tracer"));
    public static final Item PULSE_RIFLE = geo("pulse_rifle", new GeoWeaponItem("pulse_rifle", "weapon_palette"));
    public static final Item SNIPER_RIFLE = geo("sniper_rifle", new GeoWeaponItem("sniper_rifle", "weapon_palette"));
    public static final Item ROCKET_HAMMER = geo("rocket_hammer", new GeoWeaponItem("rocket_hammer", "weapon_palette"));
    public static final Item SCRAP_GUN = geo("scrap_gun", new GeoWeaponItem("scrap_gun", "weapon_palette"));
    public static final Item BIOTIC_RIFLE = geo("biotic_rifle", new GeoWeaponItem("biotic_rifle", "weapon_palette"));
    public static final Item CADUCEUS_STAFF = geo("caduceus_staff", new GeoWeaponItem("caduceus_staff", "weapon_palette"));

    private static Item geo(String id, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of("minewatch", id), item);
    }


    public static void init() {}
    private ModItems() {}
}
