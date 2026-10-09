package com.minewatch.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** 마인워치 홈 화면. 모드 버튼은 해당 마일스톤 구현 전까지 비활성. */
public class HomeScreen extends Screen {
    private final Screen parent;

    public HomeScreen(Screen parent) {
        super(Text.translatable("screen.minewatch.home"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = width / 2 - 100, y = height / 4 + 10;
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.minewatch.hero_select"),
                b -> client.setScreen(new HeroSelectScreen(this))).dimensions(x, y, 200, 24).build());
        String[] modes = {"ai", "party", "online"};
        for (int i = 0; i < modes.length; i++) {
            addDrawableChild(ButtonWidget.builder(Text.translatable("screen.minewatch.mode." + modes[i]), b -> {})
                    .dimensions(x, y + 30 * (i + 1), 200, 24)
                    .tooltip(Tooltip.of(Text.translatable("screen.minewatch.coming_soon"))).build()).active = false;
        }
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), b -> close())
                .dimensions(x, y + 140, 200, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        super.render(ctx, mx, my, d);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 4 - 20, 0xFFA000);
    }

    @Override public void close() { client.setScreen(parent); }
}
