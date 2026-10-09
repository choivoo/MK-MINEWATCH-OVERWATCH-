package com.minewatch;

import net.minecraft.item.Item;

/** 영웅 무기 아이템 표식. 이 아이템을 든 동안 클라이언트가 발사 입력을 보내고 바닐라 공격/사용이 막힌다. */
public class HeroWeaponItem extends Item {
    public HeroWeaponItem() { super(new Item.Settings().maxCount(1)); }
}
