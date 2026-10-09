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
    private com.minewatch.net.QueuePayload shownQueue = com.minewatch.net.QueuePayload.NONE;

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
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.minewatch.mode.ai"), b -> {
            PartyScreen.send("play_ai");
            client.setScreen(null);
        }).dimensions(x, y + 88, w, 22).tooltip(Tooltip.of(Text.translatable("screen.minewatch.mode.ai.tooltip"))).build());
        boolean inQueue = MineWatchClient.queue.inQueue();
        addDrawableChild(ButtonWidget.builder(Text.translatable(inQueue ? "screen.minewatch.queue.leave" : "screen.minewatch.mode.online"), b -> {
            PartyScreen.send(MineWatchClient.queue.inQueue() ? "queue_leave" : "queue_join");
        }).dimensions(x, y + 114, w, 22).tooltip(Tooltip.of(Text.translatable("screen.minewatch.mode.online.tooltip"))).build());
        shownQueue = MineWatchClient.queue;
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
        int rx = Math.max(width - 220, 36 + 190 + 30), ry = height / 2 - 40;
        Hero h = HeroRegistry.get(MineWatchClient.state.heroId());
        ctx.drawText(textRenderer, Text.translatable("screen.minewatch.current_hero"), rx, ry, DIM, false);
        ctx.drawText(textRenderer, h == null ? Text.translatable("screen.minewatch.no_hero")
                : Text.translatable("hero.minewatch." + h.id), rx, ry + 12, WHITE, true);
        ctx.drawText(textRenderer, Text.translatable("screen.minewatch.current_mode"), rx, ry + 36, DIM, false);
        ctx.drawText(textRenderer, PartyScreen.summary(), rx, ry + 48, WHITE, true);
        var q = MineWatchClient.queue;
        if (q.inQueue()) {
            ctx.drawText(textRenderer, Text.translatable("screen.minewatch.queue.status", q.queued(), q.min()), 36, height / 2 + 78, ORANGE, true);
            if (q.seconds() >= 0) ctx.drawText(textRenderer, Text.translatable("screen.minewatch.queue.countdown", q.seconds()), 36, height / 2 + 90, WHITE, true);
        }
    }

    @Override
    public void tick() {
        // 큐 참가/취소 상태가 바뀌면 버튼 문구를 갱신한다
        if (MineWatchClient.queue.inQueue() != shownQueue.inQueue()) clearAndInit();
    }

    @Override public void close() { client.setScreen(parent); }
}
