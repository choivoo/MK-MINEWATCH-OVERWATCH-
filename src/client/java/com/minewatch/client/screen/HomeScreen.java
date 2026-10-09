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
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** 오버워치 2 메인 메뉴풍 로비 화면. 월드에 들어오면 자동으로 열린다. */
public class HomeScreen extends Screen {
    private static final int ORANGE = 0xFFF99E1A, WHITE = 0xFFFFFFFF, DIM = 0xFFCFE4F7;

    private record Item(String key, Runnable action) {}

    private final Screen parent;
    private final List<Item> items = new ArrayList<>();
    private boolean shownInQueue;
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
        shownInQueue = MineWatchClient.queue.inQueue();
        items.add(new Item("screen.minewatch.play", () -> { PartyScreen.send("play"); client.setScreen(null); }));
        items.add(new Item("screen.minewatch.hero_select", () -> client.setScreen(new HeroSelectScreen(this))));
        items.add(new Item("screen.minewatch.mode.ai", () -> { PartyScreen.send("play_ai"); client.setScreen(null); }));
        items.add(new Item(shownInQueue ? "screen.minewatch.queue.leave" : "screen.minewatch.mode.online",
                () -> PartyScreen.send(MineWatchClient.queue.inQueue() ? "queue_leave" : "queue_join")));
        items.add(new Item("screen.minewatch.settings", () -> client.setScreen(new PartyScreen(this))));
        items.add(new Item("screen.minewatch.close", this::close));
    }

    private int itemY(int i) { return height / 2 - 62 + i * 30 + (i == items.size() - 1 ? 14 : 0); }

    private int hit(double mx, double my) {
        for (int i = 0; i < items.size(); i++) {
            int w = textRenderer.getWidth(Text.translatable(items.get(i).key)) * 2 + 40;
            if (mx >= 28 && mx < 28 + w && my >= itemY(i) - 4 && my < itemY(i) + 24) return i;
        }
        return -1;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float d) {
        // 오버워치 2 키아트풍 하늘색 배경 + 하단 어둡게
        ctx.fillGradient(0, 0, width, height, 0xF03E8FD6, 0xF0A9D8F5);
        ctx.fillGradient(0, height * 2 / 3, width, height, 0x00000000, 0x90102040);
        ctx.fillGradient(0, 0, width / 3, height, 0x80102a50, 0x00102a50);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        super.render(ctx, mx, my, d);
        long now = System.currentTimeMillis();
        float t = Math.min(1f, (now - opened) / 450f);

        // 중앙: 선택한 영웅(없으면 첫 영웅)을 영웅 스킨으로 3D 표시
        var mc = client;
        Hero h = HeroRegistry.get(MineWatchClient.state.heroId());
        Hero show = h != null ? h : HeroRegistry.all().iterator().next();
        if (mc.player != null) {
            HeroSkins.preview = show.numericId;
            ItemStack old = mc.player.getMainHandStack();
            int slot = mc.player.getInventory().selectedSlot;
            boolean swap = show.weapon() != null;
            if (swap) mc.player.getInventory().setStack(slot, new ItemStack(show.weapon()));
            double sway = Math.sin(now / 1100.0) * 24;
            InventoryScreen.drawEntity(ctx, width / 2 - 150, 10, width / 2 + 150, height - 6, (int) (height * 0.40f),
                    0.0625f, (float) (width / 2 + sway), (float) (height * 0.3f), mc.player);
            if (swap) mc.player.getInventory().setStack(slot, old);
        }

        // 좌측: 로고 + 큼직한 기울임 메뉴
        var m = ctx.getMatrices();
        m.push(); m.translate(28, 18, 0); m.scale(2.4f, 2.4f, 1f);
        ctx.drawText(textRenderer, Text.literal("MINEWATCH").formatted(Formatting.ITALIC), 0, 0, WHITE, true);
        m.pop();
        ctx.drawText(textRenderer, Text.translatable("screen.minewatch.tagline"), 30, 46, DIM, true);

        hover = hit(mx, my);
        for (int i = 0; i < items.size(); i++) {
            float a = Math.min(1f, Math.max(0f, t * 3f - i * 0.25f));
            int x = 28 - (int) ((1f - a) * 40);
            int y = itemY(i);
            boolean on = i == hover;
            Text label = Text.translatable(items.get(i).key).formatted(Formatting.ITALIC);
            if (on) {
                int w = textRenderer.getWidth(label) * 2 + 40;
                ctx.fill(x, y - 4, x + w, y + 24, 0x55FFFFFF);
                ctx.fill(x, y - 4, x + 3, y + 24, ORANGE);
            }
            m.push(); m.translate(x + 14, y + 2, 0); m.scale(2f, 2f, 1f);
            ctx.drawText(textRenderer, label, 0, 0, i == 0 ? ORANGE : WHITE, true);
            m.pop();
        }

        // 우측 카드: 현재 영웅 / 모드
        int rw = 150, rx = width - rw - 16;
        card(ctx, rx, 18, rw, 44, Text.translatable("screen.minewatch.current_hero"),
                h == null ? Text.translatable("screen.minewatch.no_hero") : Text.translatable("hero.minewatch." + h.id));
        card(ctx, rx, 68, rw, 44, Text.translatable("screen.minewatch.current_mode"), PartyScreen.summary());
        var q = MineWatchClient.queue;
        if (q.inQueue()) {
            card(ctx, rx, 118, rw, 44, Text.translatable("screen.minewatch.queue.status", q.queued(), q.min()),
                    q.seconds() >= 0 ? Text.translatable("screen.minewatch.queue.countdown", q.seconds()) : Text.empty());
        }

    }

    private void card(DrawContext ctx, int x, int y, int w, int h, Text title, Text body) {
        ctx.fill(x, y, x + w, y + h, 0xAA0E2A52);
        ctx.fill(x, y, x + 3, y + h, ORANGE);
        ctx.drawText(textRenderer, title, x + 10, y + 8, DIM, false);
        ctx.drawText(textRenderer, body, x + 10, y + 24, WHITE, true);
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

    @Override
    public void tick() {
        // 큐 참가/취소 상태가 바뀌면 메뉴 문구를 갱신한다
        if (MineWatchClient.queue.inQueue() != shownInQueue) init();
    }

    @Override public void removed() { HeroSkins.preview = 0; }

    @Override public void close() { client.setScreen(parent); }
}

