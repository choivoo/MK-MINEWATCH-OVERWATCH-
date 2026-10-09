package com.minewatch.client.screen;

import com.minewatch.hero.Hero;
import com.minewatch.hero.HeroRegistry;
import com.minewatch.hero.Role;
import com.minewatch.net.SelectHeroPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** 역할별 탭 + 영웅 카드. 월드 안에서만 선택 가능. */
public class HeroSelectScreen extends Screen {
    private final Screen parent;
    private Role tab = Role.DAMAGE;

    public HeroSelectScreen(Screen parent) {
        super(Text.translatable("screen.minewatch.hero_select"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearChildren();
        int x = width / 2 - 150;
        for (Role r : Role.values()) {
            addDrawableChild(ButtonWidget.builder(Text.translatable("role.minewatch." + r.key), b -> { tab = r; init(); })
                    .dimensions(x, 30, 98, 20).build());
            x += 101;
        }
        int y = 62;
        for (Hero h : HeroRegistry.all()) {
            if (h.role() != tab) continue;
            addDrawableChild(ButtonWidget.builder(Text.translatable("hero.minewatch." + h.id), b -> pick(h))
                    .dimensions(width / 2 - 100, y, 200, 24).build());
            y += 28;
        }
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), b -> close())
                .dimensions(width / 2 - 100, height - 30, 200, 20).build());
    }

    private void pick(Hero h) {
        if (client != null && client.world != null) {
            ClientPlayNetworking.send(new SelectHeroPayload(h.id));
            client.setScreen(parent);
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        super.render(ctx, mx, my, d);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);
        boolean any = HeroRegistry.all().stream().anyMatch(h -> h.role() == tab);
        if (!any) ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.minewatch.coming_soon"), width / 2, 80, 0xAAAAAA);
        if (client != null && client.world == null)
            ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.minewatch.join_world"), width / 2, height - 46, 0xFFAA00);
    }

    @Override public void close() { client.setScreen(parent); }
}
