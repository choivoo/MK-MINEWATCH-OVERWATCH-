package com.minewatch.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * 스프라이트 그리기 도우미.
 * - draw: 일반 알파 블렌딩(흰색 알파 마스크를 색조로 칠함)
 * - add/mul: Seafle HUD 의 "_add"/"_mul" 쌍. 이 텍스처들은 알파가 1/255 로 거의 0 이고 RGB 가 내용이므로
 *   일반 블렌딩으로는 보이지 않는다. add 는 가산(ONE, ONE), mul 은 곱셈(DST_COLOR, ZERO) 블렌딩으로 그린다.
 */
public final class Sprites {
    public static Identifier gui(String path) { return Identifier.of("minewatch", "textures/gui/" + path + ".png"); }

    private static void quad(DrawContext g, Identifier tex, int x, int y, int w, int h, int texW, int texH, int argb) {
        float a = (argb >>> 24 & 0xFF) / 255f, r = (argb >> 16 & 0xFF) / 255f, gr = (argb >> 8 & 0xFF) / 255f, b = (argb & 0xFF) / 255f;
        g.draw();                      // 모아 둔 fill/텍스트를 먼저 그려서 그리기 순서를 지킨다
        g.setShaderColor(r, gr, b, a);
        g.drawTexture(tex, x, y, w, h, 0, 0, texW, texH, texW, texH);
        g.setShaderColor(1f, 1f, 1f, 1f);
    }

    /** 스프라이트 전체(texW x texH 픽셀)를 (x,y,w,h)에 argb 색조로 그린다(일반 알파 블렌딩). */
    public static void draw(DrawContext g, Identifier tex, int x, int y, int w, int h, int texW, int texH, int argb) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        quad(g, tex, x, y, w, h, texW, texH, argb);
    }

    /** 가산 합성: 배경에 (텍스처 RGB x 색조)를 더한다. 빛나는 부분에 쓴다. */
    public static void add(DrawContext g, Identifier tex, int x, int y, int w, int h, int texW, int texH, int argb) {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE);
        quad(g, tex, x, y, w, h, texW, texH, argb | 0xFF000000);
        RenderSystem.defaultBlendFunc();
    }

    /** 곱셈 합성: 배경에 (텍스처 RGB x 색조)를 곱한다. 어두운 바탕/그림자에 쓴다. */
    public static void mul(DrawContext g, Identifier tex, int x, int y, int w, int h, int texW, int texH, int argb) {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.DST_COLOR, GlStateManager.DstFactor.ZERO);
        quad(g, tex, x, y, w, h, texW, texH, argb | 0xFF000000);
        RenderSystem.defaultBlendFunc();
    }
    private Sprites() {}
}
