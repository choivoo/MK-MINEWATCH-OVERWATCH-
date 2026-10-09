package com.minewatch.client.screen;

import com.minewatch.client.HeroSkins;
import com.minewatch.client.MineWatchClient;
import com.minewatch.client.UiSound;
import com.minewatch.hero.Hero;
import com.minewatch.hero.HeroRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** 오버워치 2 메인 메뉴. 하늘을 나는 영웅들(영웅 스킨 3D)과 왼쪽 세로 메뉴로 이루어진다. */
public class HomeScreen extends Screen {
    private static final int ORANGE = 0xFFFF7A3C, WHITE = 0xFFFFFFFF, DIM = 0xFFD6E8F8, YELLOW = 0xFFFFC21A;

    private record Item(String key, float scale, boolean fresh, Runnable action) {}
    /** {영웅 id, 중심 x 비율, 중심 y 비율, 크기(높이 비율), 기울기(도), 시선 x, 시선 y, 위상} — 원작 키아트의 낙하 구도를 본뜬 배치 */
    private static final Object[][] FLYERS = {
        {"widowmaker", 0.36f, 0.20f, 0.14f, -28f, 40f, -30f, 0.0f},
        {"reinhardt", 0.56f, 0.22f, 0.18f, 18f, -30f, -50f, 1.3f},
        {"ana", 0.74f, 0.13f, 0.13f, 24f, -40f, -20f, 2.1f},
        {"mercy", 0.65f, 0.43f, 0.16f, -14f, -30f, -50f, 0.7f},
        {"roadhog", 0.34f, 0.74f, 0.15f, -34f, 50f, -10f, 2.6f},
        {"soldier76", 0.60f, 0.77f, 0.16f, 20f, -20f, -40f, 1.8f},
        {"tracer", 0.46f, 0.52f, 0.24f, -20f, 60f, -70f, 0.4f},
    };

    private final Screen parent;
    private final List<Item> items = new ArrayList<>();
    private int[] itemY = new int[0];
    private int hover = -1;
    private long opened;

    public HomeScreen(Screen parent) {
        super(Text.translatable("screen.minewatch.home"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        opened = System.currentTimeMillis();
        items.clear();
        items.add(new Item("menu.minewatch.stadium", 1.4f, false, () -> { PartyScreen.send("play_ai"); client.setScreen(null); }));
        items.add(new Item("menu.minewatch.play", 2.0f, false, () -> client.setScreen(new PlayScreen(this))));
        items.add(new Item("menu.minewatch.hero", 2.0f, false, () -> client.setScreen(new HeroSelectScreen(this))));
        items.add(new Item("menu.minewatch.shop", 2.0f, true, () -> client.setScreen(new PanelScreen(this, PanelScreen.Kind.SHOP))));
        items.add(new Item("menu.minewatch.battlepass", 2.0f, false, () -> client.setScreen(new PerkScreen())));
        items.add(new Item("menu.minewatch.loot", 1.15f, false, () -> client.setScreen(new PanelScreen(this, PanelScreen.Kind.LOOT))));
        items.add(new Item("menu.minewatch.social", 1.15f, false, () -> client.setScreen(new PartyScreen(this))));
        items.add(new Item("menu.minewatch.profile", 1.15f, false, () -> client.setScreen(new PanelScreen(this, PanelScreen.Kind.PROFILE))));
        items.add(new Item("menu.minewatch.challenges", 1.15f, false, () -> client.setScreen(new PanelScreen(this, PanelScreen.Kind.CHALLENGES))));
        layout();
    }

    /** 위에서 아래로 쌓는다. 큰 항목은 크게, 아래 작은 항목은 촘촘하게. */
    private void layout() {
        itemY = new int[items.size()];
        int y = (int) (height * 0.21f);
        for (int i = 0; i < items.size(); i++) {
            itemY[i] = y;
            y += (int) (items.get(i).scale * 9) + (items.get(i).scale > 1.9f ? 6 : items.get(i).scale > 1.4f ? 12 : 6);
        }
    }

    private int textW(Item it) { return (int) (textRenderer.getWidth(Text.translatable(it.key)) * it.scale) + 6; }

    private int hit(double mx, double my) {
        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            if (mx >= 24 && mx < 24 + textW(it) + (it.fresh ? 34 : 0) && my >= itemY[i] - 2 && my < itemY[i] + it.scale * 9 + 2) return i;
        }
        return -1;
    }

    private static void cloud(DrawContext g, int cx, int cy, int rx, int ry, int alpha) {
        for (int dy = -ry; dy <= ry; dy++) {
            int hw = (int) (rx * Math.sqrt(1 - (dy / (double) ry) * (dy / (double) ry)));
            g.fill(cx - hw, cy + dy, cx + hw, cy + dy + 1, (alpha << 24) | 0xFFFFFF);
        }
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float d) {
        ctx.fillGradient(0, 0, width, height, 0xFF1E78D8, 0xFF8FD0F8);
        // 하늘색 곡선(원작 키아트의 푸른 궤적)
        for (int i = 0; i < 70; i++) {
            double t = i / 70.0;
            int x = (int) (width * (0.10 + 0.50 * t)), y = (int) (height * (0.30 - 0.22 * Math.sin(t * Math.PI * 0.9)));
            int th = 3 + (int) (7 * Math.sin(t * Math.PI));
            ctx.fill(x, y, x + 6, y + th, ((int) (90 * Math.sin(t * Math.PI)) << 24) | 0xBFF0FF);
        }
        // 구름
        cloud(ctx, (int) (width * 0.05), (int) (height * 0.95), (int) (width * 0.22), (int) (height * 0.16), 120);
        cloud(ctx, (int) (width * 0.30), (int) (height * 1.0), (int) (width * 0.20), (int) (height * 0.12), 100);
        cloud(ctx, (int) (width * 0.88), (int) (height * 0.98), (int) (width * 0.20), (int) (height * 0.15), 110);
        cloud(ctx, (int) (width * 0.02), (int) (height * 0.45), (int) (width * 0.06), (int) (height * 0.10), 70);
        ctx.fillGradient(0, 0, width / 3, height, 0x501040A0, 0x00000000);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        super.render(ctx, mx, my, d);
        long now = System.currentTimeMillis();
        float t = Math.min(1f, (now - opened) / 600f);
        var mc = client;

        // 하늘을 나는 영웅들(스킨 입은 3D 모델) — 천천히 흔들리며 떠 있다
        for (Object[] f : FLYERS) {
            Hero h = HeroRegistry.get((String) f[0]);
            float phase = (float) f[7];
            float bob = (float) Math.sin(now / 1300.0 + phase) * 5f, tilt = (float) Math.sin(now / 1700.0 + phase) * 4f;
            float pop = t >= 1f ? 1f : 0.7f + 0.3f * t;
            int size = (int) (height * (float) f[3] * pop);
            HeroFigures.draw(ctx, mc, h, width * (float) f[1], height * (float) f[2] + bob, size, (float) f[4] + tilt, (float) f[5], (float) f[6]);
        }
        HeroSkins.preview = 0;

        // 좌상단 로고
        var m = ctx.getMatrices();
        m.push(); m.translate(16, 12, 0); m.scale(1.8f, 1.8f, 1f);
        ctx.drawText(textRenderer, Text.literal("MINEWATCH").formatted(Formatting.ITALIC, Formatting.BOLD), 0, 0, WHITE, true);
        m.pop();

        // 왼쪽 메뉴
        hover = hit(mx, my);
        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            float a = Math.min(1f, Math.max(0f, t * 3f - i * 0.15f));
            int x = 24 - (int) ((1f - a) * 30);
            boolean on = i == hover;
            Text label = Text.translatable(it.key).formatted(Formatting.ITALIC);
            int color = i == 0 ? (on ? 0xFFFFA070 : ORANGE) : on ? 0xFFFFE08A : WHITE;
            m.push(); m.translate(x, itemY[i], 0); m.scale(it.scale, it.scale, 1f);
            ctx.drawText(textRenderer, label, 0, 0, color, true);
            m.pop();
            if (it.fresh) {
                int bx = x + textW(it) + 4, by = itemY[i] + 2;
                ctx.fill(bx, by, bx + 28, by + 11, YELLOW);
                ctx.drawText(textRenderer, Text.translatable("menu.minewatch.new"), bx + 3, by + 2, 0xFF1A1A1A, false);
            }
        }

        // 우상단 프로필 바 + 카드 두 장
        int rw = Math.min(150, width / 4), rx = width - rw - 14;
        ctx.fill(rx, 10, rx + rw, 26, 0xCC0E2A52);
        ctx.fill(rx, 10, rx + 3, 26, ORANGE);
        ctx.drawText(textRenderer, Text.literal(mc.getSession().getUsername()), rx + 8, 14, WHITE, true);
        Hero cur = HeroRegistry.get(MineWatchClient.state.heroId());
        card(ctx, rx, height - 138, rw, 56, Text.translatable("screen.minewatch.current_hero"),
                cur == null ? Text.translatable("screen.minewatch.no_hero") : Text.translatable("hero.minewatch." + cur.id).formatted(Formatting.ITALIC));
        card(ctx, rx, height - 76, rw, 56, Text.translatable("screen.minewatch.current_mode"), PartyScreen.summary());
        var q = MineWatchClient.queue;
        if (q.inQueue()) ctx.drawText(textRenderer, Text.translatable("screen.minewatch.queue.status", q.queued(), q.min()), rx, height - 14, ORANGE, true);
    }

    private void card(DrawContext ctx, int x, int y, int w, int h, Text title, Text body) {
        ctx.fill(x, y, x + w, y + h, 0xDD0E1830);
        ctx.fill(x, y, x + w, y + 2, ORANGE);
        ctx.drawText(textRenderer, title, x + 8, y + 10, DIM, false);
        ctx.drawText(textRenderer, body, x + 8, y + 28, WHITE, true);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int i = hit(mx, my);
        if (i >= 0 && button == 0) {
            UiSound.play("pick_select_click");
            items.get(i).action.run();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override public void removed() { HeroSkins.preview = 0; }
    @Override public void close() { client.setScreen(parent); }
}
