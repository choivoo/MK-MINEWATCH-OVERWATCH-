package com.minewatch.client.screen;

import com.minewatch.client.MineWatchClient;
import com.minewatch.hero.Hero;
import com.minewatch.hero.HeroRegistry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** 오버워치풍 로비 화면. 월드에 들어오면 자동으로 열린다. */
public class HomeScreen extends Screen {
    private static final int ORANGE = 0xFFF99E1A, WHITE = 0xFFFFFFFF, DIM = 0xFF9AA7B8;
    private final Screen parent;

    public HomeScreen(Screen parent) {
        super(Text.translatable("screen.minewatch.home"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = 36, y = height / 2 - 52, w = 190;
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.minewatch.play"), b -> {
            PartyScreen.send("play");
            client.setScreen(null);
        }).dimensions(x, y, w, 30).tooltip(Tooltip.of(Text.translatable("screen.minewatch.play.tooltip"))).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.minewatch.hero_select"),
                b -> client.setScreen(new HeroSelectScreen(this))).dimensions(x, y + 36, w, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.minewatch.settings"),
                b -> client.setScreen(new PartyScreen(this))).dimensions(x, y + 62, w, 22).build());
        for (int i = 0; i < 2; i++) {
            String key = i == 0 ? "ai" : "online";
            var btn = addDrawableChild(ButtonWidget.builder(Text.translatable("screen.minewatch.mode." + key), b -> {})
                    .dimensions(x, y + 88 + 26 * i, w, 22)
                    .tooltip(Tooltip.of(Text.translatable("screen.minewatch.coming_soon"))).build());
            btn.active = false;
        }
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.minewatch.close"), b -> close())
                .dimensions(x, y + 148, w, 20).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float d) {
        ctx.fillGradient(0, 0, width, height, 0xE60A1020, 0xE6182640);
        ctx.fill(0, 0, 3, height, ORANGE);                         // 좌측 포인트 라인
        ctx.fill(24, height / 2 - 72, 232, height / 2 - 71, ORANGE);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        super.render(ctx, mx, my, d);
        var m = ctx.getMatrices();
        m.push(); m.translate(36, height / 2 - 112, 0); m.scale(3f, 3f, 1f);
        ctx.drawText(textRenderer, "MINEWATCH", 0, 0, ORANGE, true);
        m.pop();
        ctx.drawText(textRenderer, Text.translatable("screen.minewatch.tagline"), 38, height / 2 - 84, DIM, false);

        // 우측: 현재 선택 정보
        int rx = width - 220, ry = height / 2 - 40;
        Hero h = HeroRegistry.get(MineWatchClient.state.heroId());
        ctx.drawText(textRenderer, Text.translatable("screen.minewatch.current_hero"), rx, ry, DIM, false);
        ctx.drawText(textRenderer, h == null ? Text.translatable("screen.minewatch.no_hero")
                : Text.translatable("hero.minewatch." + h.id), rx, ry + 12, WHITE, true);
        ctx.drawText(textRenderer, Text.translatable("screen.minewatch.current_mode"), rx, ry + 36, DIM, false);
        ctx.drawText(textRenderer, PartyScreen.summary(), rx, ry + 48, WHITE, true);
    }

    @Override public void close() { client.setScreen(parent); }
}
