package com.minewatch.client.screen;

import com.minewatch.client.MineWatchClient;
import com.minewatch.client.Sprites;
import com.minewatch.hero.Hero;
import com.minewatch.hero.HeroRegistry;
import com.minewatch.hero.Role;
import com.minewatch.net.SelectHeroPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** 역할 탭 + 영웅 카드(Seafle 로스터 스프라이트). 월드 안에서만 선택 가능. */
public class HeroSelectScreen extends Screen {
    private static final int ORANGE = 0xFFF99E1A, BLUE = 0xFF9CC8FF, DIM = 0xFF5F6B7A, WHITE = 0xFFFFFFFF;
    private static final int TAB_W = 104, TAB_H = 39, CARD_W = 66, CARD_H = 72;

    private final Screen parent;
    private Role tab = Role.DAMAGE;

    private record Hit(int x, int y, int w, int h, Runnable action) {
        boolean contains(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }
    private final List<Hit> hits = new ArrayList<>();

    public HeroSelectScreen(Screen parent) {
        super(Text.translatable("screen.minewatch.hero_select"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), b -> close())
                .dimensions(width / 2 - 60, height - 30, 120, 20).build());
    }

    private boolean exists(Identifier id) { return client.getResourceManager().getResource(id).isPresent(); }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float d) {
        ctx.fillGradient(0, 0, width, height, 0xE60A1020, 0xE6182640);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        super.render(ctx, mx, my, d);
        hits.clear();
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, ORANGE);

        // 역할 탭
        int tx = width / 2 - (TAB_W * 3 + 12) / 2, ty = 26;
        for (Role r : Role.values()) {
            final Role role = r;
            boolean sel = r == tab, over = mx >= tx && mx < tx + TAB_W && my >= ty && my < ty + TAB_H;
            Sprites.draw(ctx, Sprites.gui("pick/roster_hdr_" + r.key), tx, ty, TAB_W, TAB_H, 160, 60,
                    sel ? ORANGE : over ? WHITE : DIM);
            if (sel) ctx.fill(tx, ty + TAB_H, tx + TAB_W, ty + TAB_H + 2, ORANGE);
            hits.add(new Hit(tx, ty, TAB_W, TAB_H, () -> tab = role));
            tx += TAB_W + 6;
        }

        // 영웅 카드
        List<Hero> heroes = new ArrayList<>();
        for (Hero h : HeroRegistry.all()) if (h.role() == tab) heroes.add(h);
        if (heroes.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.minewatch.coming_soon"), width / 2, 110, 0xFFAAAAAA);
        }
        int cx = width / 2 - (heroes.size() * (CARD_W + 10) - 10) / 2, cy = 86;
        int current = MineWatchClient.state.heroId();
        for (Hero h : heroes) {
            final Hero hero = h;
            boolean sel = h.numericId == current, over = mx >= cx && mx < cx + CARD_W && my >= cy && my < cy + CARD_H;
            Identifier bust = Sprites.gui("pick/roster_bust_" + h.id);
            Identifier label = Sprites.gui("pick/roster_label_" + h.id);
            if (over || sel) Sprites.draw(ctx, Sprites.gui("pick/roster_glow_b"), cx - 18, cy - 14, CARD_W + 36, CARD_W + 36, 96, 96, (sel ? ORANGE : BLUE) & 0x66FFFFFF | 0x66000000);
            if (exists(bust)) ctx.drawTexture(bust, cx + 5, cy + 6, 56, 56, 0, 0, 111, 111, 111, 111);
            else ctx.fill(cx + 5, cy + 6, cx + 61, cy + 62, 0xFF1B2638);
            Sprites.draw(ctx, Sprites.gui(sel || over ? "pick/roster_frame_h" : "pick/roster_frame_n"),
                    cx - (sel || over ? 3 : 0), cy - (sel || over ? 3 : 0), sel || over ? CARD_W + 6 : CARD_W, sel || over ? CARD_H + 4 : CARD_H,
                    sel || over ? 152 : 120, sel || over ? 156 : 130, sel ? ORANGE : over ? WHITE : BLUE);
            if (exists(label)) Sprites.draw(ctx, label, cx - 7, cy + CARD_H + 2, 80, 21, 320, 84, WHITE);
            else ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("hero.minewatch." + h.id), cx + CARD_W / 2, cy + CARD_H + 6, WHITE);
            hits.add(new Hit(cx, cy, CARD_W, CARD_H, () -> pick(hero)));
            cx += CARD_W + 10;
        }

        if (client != null && client.world == null)
            ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.minewatch.join_world"), width / 2, height - 46, 0xFFFFAA00);
    }

    private void pick(Hero h) {
        if (client != null && client.world != null) {
            ClientPlayNetworking.send(new SelectHeroPayload(h.id));
            client.setScreen(parent);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        for (Hit h : hits) if (h.contains(mx, my)) { h.action().run(); return true; }
        return false;
    }

    /** 개발용 스크린샷 하니스가 탭을 고르는 데 쓴다. */
    public void devSetTab(Role r) { tab = r; }

    @Override public void close() { client.setScreen(parent); }
}
