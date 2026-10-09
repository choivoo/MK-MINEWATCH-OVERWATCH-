package com.minewatch.client.screen;

import com.minewatch.client.HeroSkins;
import com.minewatch.hero.Hero;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

/** 영웅 스킨을 입은 3D 플레이어 모델을 화면의 원하는 자리에 그리는 도우미(메인 화면/상점 공용). */
final class HeroFigures {
    /** cx, cy: 화면 좌표의 모델 중심, size: 모델 크기, roll: 기울기(도), lookX/lookY: 시선(마우스처럼 취급) */
    static void draw(DrawContext ctx, MinecraftClient mc, Hero hero, float cx, float cy, int size, float roll, float lookX, float lookY) {
        if (mc.player == null || hero == null) return;
        int prev = HeroSkins.preview;
        HeroSkins.preview = hero.numericId;
        int slot = mc.player.getInventory().selectedSlot;
        ItemStack old = mc.player.getMainHandStack();
        boolean swap = hero.weapon() != null;
        if (swap) mc.player.getInventory().setStack(slot, new ItemStack(hero.weapon()));
        var m = ctx.getMatrices();
        m.push();
        m.translate(cx, cy, 0);
        m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
        m.translate(-cx, -cy, 0);
        int half = size;
        InventoryScreen.drawEntity(ctx, (int) cx - half, (int) cy - half, (int) cx + half, (int) cy + half, size, 0.0625f,
                cx + lookX, cy + lookY, mc.player);
        m.pop();
        if (swap) mc.player.getInventory().setStack(slot, old);
        HeroSkins.preview = prev;
    }
    private HeroFigures() {}
}
