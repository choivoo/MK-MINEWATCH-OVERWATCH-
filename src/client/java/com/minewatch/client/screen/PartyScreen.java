package com.minewatch.client.screen;

import com.minewatch.net.PartyActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** 게임 설정: 모드, 목표, 내 팀. 값은 정적으로 유지되어 로비의 "게임 시작"에서 쓰인다. */
public class PartyScreen extends Screen {
    private static final int[][] TARGETS = {{10, 20, 30, 50}, {60, 100, 150}};
    private static int mode = 0, targetIdx = 1, team = -1, difficulty = 1;

    private final Screen parent;

    public PartyScreen(Screen parent) {
        super(Text.translatable("screen.minewatch.settings"));
        this.parent = parent;
    }

    static void send(String action) {
        if (ClientPlayNetworking.canSend(PartyActionPayload.ID))
            ClientPlayNetworking.send(new PartyActionPayload(action, mode, TARGETS[mode][targetIdx], team, difficulty));
    }

    static Text summary() {
        return Text.translatable("screen.minewatch.party.mode.summary",
                Text.translatable("screen.minewatch.party.mode." + mode), TARGETS[mode][targetIdx]);
    }

    @Override
    protected void init() {
        boolean inWorld = client != null && client.world != null;
        int x = width / 2 - 100, y = height / 4;
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.minewatch.party.mode", Text.translatable("screen.minewatch.party.mode." + mode)), b -> {
            mode = 1 - mode; targetIdx = Math.min(targetIdx, TARGETS[mode].length - 1); rebuild();
        }).dimensions(x, y, 200, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.minewatch.party.target", TARGETS[mode][targetIdx]), b -> {
            targetIdx = (targetIdx + 1) % TARGETS[mode].length; rebuild();
        }).dimensions(x, y + 24, 200, 20).build());
        String teamKey = team < 0 ? "auto" : team == 0 ? "a" : "b";
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.minewatch.party.team", Text.translatable("screen.minewatch.party.team." + teamKey)), b -> {
            team = team == 1 ? -1 : team + 1;
            if (inWorld) send("team");
            rebuild();
        }).dimensions(x, y + 48, 200, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.minewatch.party.difficulty",
                Text.translatable("screen.minewatch.party.difficulty." + difficulty)), b -> {
            difficulty = (difficulty + 1) % 3; rebuild();
        }).dimensions(x, y + 72, 200, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.minewatch.party.stop"), b -> send("stop"))
                .dimensions(x, y + 102, 200, 20).build()).active = inWorld;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), b -> close())
                .dimensions(x, y + 132, 200, 20).build());
    }

    private void rebuild() { clearChildren(); init(); }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        super.render(ctx, mx, my, d);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 4 - 24, 0xFFA000);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.minewatch.party.hint"),
                width / 2, height / 4 + 160, 0xAAAAAA);
    }

    @Override public void close() { client.setScreen(parent); }
}
