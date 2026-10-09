package com.minewatch.client.screen;

import com.minewatch.client.MineWatchClient;
import com.minewatch.client.UiSound;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** 플레이 화면: 모드 카드(빠른 대전 / AI 대전 / 온라인 배틀)와 게임 설정. */
public class PlayScreen extends Screen {
    private static final int ORANGE = 0xFFFF7A3C, WHITE = 0xFFFFFFFF, DIM = 0xFFB8CCE4;
    private record Card(String key, Runnable action) {}
    private final Screen parent;
    private Card[] cards;
    private boolean shownInQueue;
    private int hover = -1;

    public PlayScreen(Screen parent) {
        super(Text.translatable("menu.minewatch.play"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        shownInQueue = MineWatchClient.queue.inQueue();
        cards = new Card[]{
            new Card("screen.minewatch.play", () -> { PartyScreen.send("play"); client.setScreen(null); }),
            new Card("screen.minewatch.mode.ai", () -> { PartyScreen.send("play_ai"); client.setScreen(null); }),
            new Card(shownInQueue ? "screen.minewatch.queue.leave" : "screen.minewatch.mode.online",
                    () -> PartyScreen.send(MineWatchClient.queue.inQueue() ? "queue_leave" : "queue_join")),
        };
        clearChildren();
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.minewatch.settings"), b -> client.setScreen(new PartyScreen(this)))
                .dimensions(width - 124, height - 28, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), b -> close()).dimensions(24, height - 28, 80, 20).build());
    }

    private int cardX(int i) { int w = cardW(); return (width - (w * 3 + 12 * 2)) / 2 + i * (w + 12); }
    private int cardW() { return Math.min(150, (width - 60) / 3); }

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
        hover = -1;
        int y0 = 60, h = height - 120;
        for (int i = 0; i < cards.length; i++) {
            int x = cardX(i), w = cardW();
            boolean on = mx >= x && mx < x + w && my >= y0 && my < y0 + h;
            if (on) hover = i;
            ctx.fill(x, y0, x + w, y0 + h, on ? 0xCC1C4A8A : 0xAA0E2A52);
            ctx.fill(x, y0, x + w, y0 + 3, on ? ORANGE : 0xFF5A8AC8);
            Text name = Text.translatable(cards[i].key).formatted(Formatting.ITALIC);
            m.push(); m.translate(x + 10, y0 + 14, 0); m.scale(1.5f, 1.5f, 1f);
            ctx.drawText(textRenderer, name, 0, 0, WHITE, true);
            m.pop();
            String tip = i == 0 ? "screen.minewatch.play.tooltip" : i == 1 ? "screen.minewatch.mode.ai.tooltip" : "screen.minewatch.mode.online.tooltip";
            int ty = y0 + 40;
            for (var l : textRenderer.wrapLines(Text.translatable(tip), w - 20)) { ctx.drawText(textRenderer, l, x + 10, ty, DIM, false); ty += 11; }
        }
        ctx.drawText(textRenderer, PartyScreen.summary(), 24, height - 54, WHITE, true);
        var q = MineWatchClient.queue;
        if (q.inQueue()) {
            ctx.drawText(textRenderer, Text.translatable("screen.minewatch.queue.status", q.queued(), q.min()), 24, height - 42, ORANGE, true);
            if (q.seconds() >= 0) ctx.drawText(textRenderer, Text.translatable("screen.minewatch.queue.countdown", q.seconds()), 200, height - 42, WHITE, true);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (hover >= 0 && button == 0) { UiSound.play("pick_select_click"); cards[hover].action.run(); return true; }
        return super.mouseClicked(mx, my, button);
    }

    @Override public void tick() { if (MineWatchClient.queue.inQueue() != shownInQueue) init(); }
    @Override public void close() { client.setScreen(parent); }
}
