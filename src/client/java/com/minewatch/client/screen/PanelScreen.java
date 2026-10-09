package com.minewatch.client.screen;

import com.minewatch.client.UiSound;
import com.minewatch.hero.Hero;
import com.minewatch.hero.HeroRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** 메인 메뉴의 보조 화면(상점/전리품 상자/프로필/도전 과제)을 오버워치풍 패널로 보여 준다. */
public class PanelScreen extends Screen {
    public enum Kind { SHOP, LOOT, PROFILE, CHALLENGES }

    private static final int ORANGE = 0xFFFF7A3C, WHITE = 0xFFFFFFFF, DIM = 0xFFB8CCE4;
    private final Screen parent;
    private final Kind kind;
    private int selected;

    public PanelScreen(Screen parent, Kind kind) {
        super(Text.translatable("menu.minewatch." + kind.name().toLowerCase()));
        this.parent = parent;
        this.kind = kind;
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), b -> close()).dimensions(24, height - 28, 80, 20).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float d) {
        ctx.fillGradient(0, 0, width, height, 0xF0285A9A, 0xF08CC4EE);
        ctx.fillGradient(0, height / 2, width, height, 0x00000000, 0x80102040);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        super.render(ctx, mx, my, d);
        var m = ctx.getMatrices();
        m.push(); m.translate(24, 16, 0); m.scale(2.2f, 2.2f, 1f);
        ctx.drawText(textRenderer, title.copy().formatted(Formatting.ITALIC), 0, 0, WHITE, true);
        m.pop();
        List<Hero> heroes = new ArrayList<>(HeroRegistry.all());
        switch (kind) {
            case SHOP -> {
                // 모든 영웅의 기본(오버워치) 스킨 진열
                int n = heroes.size(), slotW = Math.min(110, (width - 60) / n);
                for (int i = 0; i < n; i++) {
                    int cx = 30 + slotW / 2 + i * slotW;
                    boolean over = mx >= cx - slotW / 2 && mx < cx + slotW / 2 && my > 50 && my < height - 40;
                    if (over) selected = i;
                    if (i == selected) ctx.fill(cx - slotW / 2 + 2, 52, cx + slotW / 2 - 2, height - 44, 0x40FFFFFF);
                    HeroFigures.draw(ctx, client, heroes.get(i), cx, height / 2 + 4, Math.max(30, slotW / 2 - 8), 0, (mx - cx) * 0.3f, -40);
                    Text name = Text.translatable("hero.minewatch." + heroes.get(i).id).formatted(Formatting.ITALIC);
                    ctx.drawCenteredTextWithShadow(textRenderer, name, cx, height - 66, WHITE);
                    ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("menu.minewatch.shop.owned"), cx, height - 54, 0xFF8CF08C);
                }
                ctx.drawText(textRenderer, Text.translatable("menu.minewatch.shop.note"), 24, 44, DIM, true);
            }
            case LOOT -> lines(ctx, "menu.minewatch.loot.l", 3);
            case PROFILE -> {
                var mc = client;
                ctx.drawText(textRenderer, Text.literal(mc.getSession().getUsername()).formatted(Formatting.BOLD), 24, 60, WHITE, true);
                var h = HeroRegistry.get(com.minewatch.client.MineWatchClient.state.heroId());
                ctx.drawText(textRenderer, Text.translatable("screen.minewatch.current_hero"), 24, 84, DIM, false);
                ctx.drawText(textRenderer, h == null ? Text.translatable("screen.minewatch.no_hero") : Text.translatable("hero.minewatch." + h.id), 24, 96, WHITE, true);
                ctx.drawText(textRenderer, Text.translatable("screen.minewatch.current_mode"), 24, 120, DIM, false);
                ctx.drawText(textRenderer, PartyScreen.summary(), 24, 132, WHITE, true);
                if (h != null) HeroFigures.draw(ctx, client, h, width - 120, height / 2 + 40, Math.min(70, height / 4), 0, (mx - width + 120) * 0.3f, -60);
            }
            case CHALLENGES -> lines(ctx, "menu.minewatch.challenge.l", 4);
        }
    }

    private void lines(DrawContext ctx, String key, int n) {
        for (int i = 1; i <= n; i++) {
            int y = 56 + (i - 1) * 34;
            ctx.fill(24, y, width - 24, y + 28, 0x99102A55);
            ctx.fill(24, y, 27, y + 28, ORANGE);
            ctx.drawText(textRenderer, Text.translatable(key + i), 36, y + 10, WHITE, true);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (kind == Kind.SHOP && my > 50 && my < height - 40) UiSound.play("pick_select_click");
        return super.mouseClicked(mx, my, button);
    }

    @Override public void close() { client.setScreen(parent); }
}
