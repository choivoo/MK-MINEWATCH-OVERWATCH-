package com.minewatch.client.screen;

import com.minewatch.ModSounds;
import com.minewatch.client.MineWatchClient;
import com.minewatch.client.Sprites;
import com.minewatch.net.PerkChoosePayload;
import com.minewatch.net.PerkStatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/** 퍽 선택: 마이너/메이저 중 지금 고를 수 있는 2개 중 하나를 고른다. */
public class PerkScreen extends Screen {
    private static final int ORANGE = 0xFFF99E1A, WHITE = 0xFFFFFFFF, DIM = 0xFF9AA7B8, CARD_W = 170, CARD_H = 150;

    public PerkScreen() { super(Text.translatable("screen.minewatch.perk.title")); }

    private static String key(String perkId) { return perkId.substring(perkId.indexOf(':') + 1); }

    private static void sound(String name) {
        SoundEvent e = ModSounds.get(name);
        if (e != null) net.minecraft.client.MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(e, 1f));
    }

    @Override protected void init() { sound("perk_open"); }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float d) {
        ctx.fillGradient(0, 0, width, height, 0xCC0A1020, 0xCC182640);
    }

    private int cardX(int i) { return width / 2 - CARD_W - 8 + i * (CARD_W + 16); }
    private int cardY() { return height / 2 - CARD_H / 2 + 6; }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        super.render(ctx, mx, my, d);
        PerkStatePayload st = MineWatchClient.perk;
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, ORANGE);
        if (st.offerA().isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.minewatch.perk.none"), width / 2, height / 2, DIM);
            return;
        }
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable(st.stage() == 3 ? "screen.minewatch.perk.major" : "screen.minewatch.perk.minor"), width / 2, 36, WHITE);
        String[] offers = {st.offerA(), st.offerB()};
        for (int i = 0; i < 2; i++) {
            int x = cardX(i), y = cardY();
            boolean over = mx >= x && mx < x + CARD_W && my >= y && my < y + CARD_H;
            ctx.fill(x - 1, y - 1, x + CARD_W + 1, y + CARD_H + 1, over ? ORANGE : 0xFF3A4A60);
            ctx.fill(x, y, x + CARD_W, y + CARD_H, 0xF0101A2C);
            String k = key(offers[i]);
            Identifier icon = Sprites.gui("perk/" + k + "_64");
            if (client.getResourceManager().getResource(icon).isPresent()) ctx.drawTexture(icon, x + CARD_W / 2 - 32, y + 10, 64, 64, 0, 0, 64, 64, 64, 64);
            else {
                ctx.fill(x + CARD_W / 2 - 24, y + 14, x + CARD_W / 2 + 24, y + 62, 0xFF1F2E48);
                ctx.drawCenteredTextWithShadow(textRenderer, String.valueOf(i + 1), x + CARD_W / 2, y + 34, over ? ORANGE : DIM);
            }
            ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("perk.minewatch." + k), x + CARD_W / 2, y + 80, over ? ORANGE : WHITE);
            int ty = y + 96;
            for (OrderedText line : textRenderer.wrapLines(Text.translatable("perk.minewatch." + k + ".desc"), CARD_W - 16)) {
                ctx.drawTextWithShadow(textRenderer, line, x + 8, ty, DIM);
                ty += 10;
            }
        }
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.minewatch.perk.hint"), width / 2, cardY() + CARD_H + 14, DIM);
    }

    private void choose(int i) {
        PerkStatePayload st = MineWatchClient.perk;
        String id = i == 0 ? st.offerA() : st.offerB();
        if (id.isEmpty()) return;
        sound("perk_pick_thump");
        ClientPlayNetworking.send(new PerkChoosePayload(id));
        close();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int i = 0; i < 2; i++) {
            int x = cardX(i), y = cardY();
            if (mx >= x && mx < x + CARD_W && my >= y && my < y + CARD_H) { choose(i); return true; }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_1) { choose(0); return true; }
        if (key == GLFW.GLFW_KEY_2) { choose(1); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override public void close() { sound("perk_close"); client.setScreen(null); }
}
