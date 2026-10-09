package com.minewatch.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** 흰색 알파 마스크 스프라이트를 색을 입혀 그리는 도우미. */
public final class Sprites {
    public static Identifier gui(String path) { return Identifier.of("minewatch", "textures/gui/" + path + ".png"); }

    /** 스프라이트 전체(texW x texH 픽셀)를 (x,y,w,h)에 argb 색조로 그린다. */
    public static void draw(DrawContext g, Identifier tex, int x, int y, int w, int h, int texW, int texH, int argb) {
        float a = (argb >>> 24 & 0xFF) / 255f, r = (argb >> 16 & 0xFF) / 255f, gr = (argb >> 8 & 0xFF) / 255f, b = (argb & 0xFF) / 255f;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setShaderColor(r, gr, b, a);
        g.drawTexture(tex, x, y, w, h, 0, 0, texW, texH, texW, texH);
        g.setShaderColor(1f, 1f, 1f, 1f);
    }
    private Sprites() {}
}
