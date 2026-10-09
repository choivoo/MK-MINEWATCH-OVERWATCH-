package com.minewatch.client.screen;

import com.minewatch.client.HeroSkins;
import com.minewatch.client.MineWatchClient;
import com.minewatch.client.Sprites;
import com.minewatch.client.UiSound;
import com.minewatch.hero.Hero;
import com.minewatch.hero.HeroRegistry;
import com.minewatch.hero.Role;
import com.minewatch.net.SelectHeroPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * 오버워치 2 스타일 영웅 선택. 연한 라벤더 블러 배경 위에 선택한 영웅의 스킨 모델이 크게 서 있고,
 * 아래에는 역할 버튼과 영웅 목록, 오른쪽 위에는 영웅 정보, 가운데 아래에는 "확인" 버튼이 있다.
 * 영웅을 바꾸면 모델이 번쩍이며 새로 등장한다.
 */
public class HeroSelectScreen extends Screen {
    private static final int RW = 426, RH = 240;                      // 기준 해상도(모든 UI 를 이 좌표로 그리고 화면에 맞게 확대)
    private static final int WHITE = 0xFFFFFFFF, CYAN = 0xFF35D8FF, YELLOW = 0xFFFFD23F, DARK = 0xFF1B2440, BLUE = 0xFF3F8CFF;

    private final Screen parent;
    private Role tab = Role.DAMAGE;
    private boolean randomTab = false;
    private Hero preview;
    private long previewSince = System.currentTimeMillis() - 2000;
    private float u, ox, oy;

    private record Hit(int x, int y, int w, int h, Runnable action) {
        boolean contains(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }
    private final List<Hit> hits = new ArrayList<>();

    public HeroSelectScreen(Screen parent) {
        super(Text.translatable("screen.minewatch.hero_select"));
        this.parent = parent;
        Hero cur = HeroRegistry.get(MineWatchClient.state.heroId());
        preview = cur != null ? cur : HeroRegistry.all().iterator().next();
        tab = preview.role();
    }

    /** 개발용 스크린샷 하니스가 영웅 미리보기를 고르는 데 쓴다. */
    public void devPreview(String id) { Hero h = HeroRegistry.get(id); tab = h.role(); randomTab = false; setPreview(h); previewSince = System.currentTimeMillis() - 800; }

    /** 개발용 스크린샷 하니스가 역할 탭을 고르는 데 쓴다. */
    public void devSetTab(Role r) { tab = r; randomTab = false; List<Hero> l = heroesOfTab(); if (!l.isEmpty()) setPreview(l.get(0)); }

    @Override protected void init() { applyPreview(); UiSound.play("pick_open"); }
    @Override public void removed() { HeroSkins.preview = 0; }

    private void applyPreview() { HeroSkins.preview = preview.numericId; }

    private void setPreview(Hero h) {
        if (h == preview) return;
        preview = h;
        previewSince = System.currentTimeMillis();
        applyPreview();
        UiSound.play("pick_select_click");
    }

    private List<Hero> heroesOfTab() {
        List<Hero> l = new ArrayList<>();
        for (Hero h : HeroRegistry.all()) if (randomTab || h.role() == tab) l.add(h);
        return l;
    }

    // ---- 그리기 ----

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float d) {
        super.renderBackground(ctx, mx, my, d);                              // 월드 블러
        ctx.fillGradient(0, 0, width, height, 0xB9B9B4EA, 0xB98F8ACB);         // 라벤더 색조
    }

    private static float easeOutBack(float t) { float c = 1.70158f; t -= 1; return 1 + (c + 1) * t * t * t + c * t * t; }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        hits.clear();
        u = Math.min(width / (float) RW, height / (float) RH);
        ox = (width - RW * u) / 2f; oy = (height - RH * u) / 2f;
        int rmx = (int) ((mx - ox) / u), rmy = (int) ((my - oy) / u);
        long now = System.currentTimeMillis();
        float t = Math.min(1f, (now - previewSince) / 450f);

        renderBackground(ctx, mx, my, delta);

        var m = ctx.getMatrices();
        m.push(); m.translate(ox, oy, 0); m.scale(u, u, 1f);

        drawHeading(ctx);
        drawSlots(ctx);                                                       // 모델 뒤
        m.pop();

        // ---- 3D 영웅 모델 (실제 화면 좌표) ----
        float pop = t >= 1f ? 1f : 0.82f + 0.18f * easeOutBack(t);
        int size = (int) (height * 0.36f * pop);
        double sway = Math.sin(now / 900.0) * 18;
        ItemStack old = mc.player.getMainHandStack();
        int slot = mc.player.getInventory().selectedSlot;
        boolean swap = preview.weapon() != null;
        if (swap) mc.player.getInventory().setStack(slot, new ItemStack(preview.weapon()));
        InventoryScreen.drawEntity(ctx, width / 2 - 140, 10, width / 2 + 140, height - 10, size, 0.0625f,
                (float) (mx + sway), (float) (my - height * 0.18), mc.player);
        if (swap) mc.player.getInventory().setStack(slot, old);
        // 등장 섬광: 흰색 번쩍임 + 세로 빔
        if (t < 1f) {
            float a = (1f - t) * (1f - t);
            ctx.fill(0, 0, width, height, ((int) (a * 150) << 24) | 0xFFFFFF);
            int bw = (int) (width * 0.16f * (1f - t * 0.6f));
            ctx.fillGradient(width / 2 - bw / 2, 0, width / 2 + bw / 2, height, ((int) (a * 190) << 24) | 0xFFFFFF, ((int) (a * 60) << 24) | 0x9FD8FF);
        }

        m.push(); m.translate(ox, oy, 0); m.scale(u, u, 1f);
        drawHeroInfo(ctx);
        drawBottom(ctx, rmx, rmy);
        m.pop();
    }

    private void drawHeading(DrawContext ctx) {
        var m = ctx.getMatrices();
        // 큰 흐릿한 모드 이름 + 시안색 안내 문구
        m.push(); m.translate(14, 8, 0); m.scale(2.4f, 2.4f, 1f);
        ctx.drawText(textRenderer, Text.translatable("screen.minewatch.home").formatted(Formatting.ITALIC), 0, 0, 0xCCFFFFFF, true);
        m.pop();
        m.push(); m.translate(14, 34, 0); m.scale(1.45f, 1.45f, 1f);
        ctx.drawText(textRenderer, Text.translatable("screen.minewatch.pick.header").formatted(Formatting.ITALIC), 0, 0, CYAN, true);
        m.pop();
        ctx.drawText(textRenderer, PartyScreen.summary(), 15, 54, 0xCCFFFFFF, false);
    }

    /** 왼쪽: 내 프로필 육각 카드 + 비어 있는 팀 슬롯 4개. */
    private void drawSlots(DrawContext ctx) {
        int cy = 118;
        // 비어 있는 슬롯(육각 + 점 3개)
        for (int i = 0; i < 4; i++) {
            int cx = 118 + i * 72;
            hex(ctx, cx, cy, 24, 0x59FFFFFF);
            hex(ctx, cx, cy, 22, 0x66384C78);
            for (int k = -1; k <= 1; k++) ctx.fill(cx + k * 6 - 1, cy - 1, cx + k * 6 + 1, cy + 1, 0xEEFFFFFF);
        }
        // 내 카드: 푸른 테두리 육각 + 영웅 프로필
        int cx = 50;
        hex(ctx, cx, cy, 31, BLUE);
        hex(ctx, cx, cy, 28, 0xFF0B1B3C);
        Identifier bust = Sprites.gui("pick/roster_bust_" + preview.id);
        ctx.drawTexture(bust, cx - 20, cy - 20, 40, 40, 0, 0, 111, 111, 111, 111);
        String name = MinecraftClient.getInstance().getSession().getUsername();
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(name).formatted(Formatting.ITALIC), cx, cy + 36, 0xFF5AB4FF);
        Sprites.draw(ctx, Sprites.gui("pick/team_glyph_" + (preview.role() == Role.TANK ? "tank" : preview.role() == Role.SUPPORT ? "support" : "damage")),
                cx - 34, cy - 42, 10, 10, 64, 64, WHITE);
    }

    /** 오른쪽 위: 영웅 이름/역할/설명/능력/스킨. */
    private void drawHeroInfo(DrawContext ctx) {
        var m = ctx.getMatrices();
        Text name = Text.translatable("hero.minewatch." + preview.id).formatted(Formatting.ITALIC);
        float s = 2.6f;
        int nw = (int) (textRenderer.getWidth(name) * s);
        m.push(); m.translate(RW - 16 - nw, 10, 0); m.scale(s, s, 1f);
        ctx.drawText(textRenderer, name, 0, 0, WHITE, true);
        m.pop();
        ctx.fill(RW - 16 - Math.max(nw, 96), 36, RW - 16, 37, 0x99FFFFFF);
        Text role = Text.translatable("role.minewatch." + preview.role().key);
        ctx.drawText(textRenderer, role, RW - 16 - textRenderer.getWidth(role), 41, 0xFFEAF2FF, true);
        int y = 56;
        for (OrderedText l : textRenderer.wrapLines(Text.translatable("hero.minewatch." + preview.id + ".desc"), 150)) {
            ctx.drawText(textRenderer, l, RW - 16 - textRenderer.getWidth(l), y, 0xFFFFFFFF, true); y += 10;
        }
        y += 4;
        for (OrderedText l : textRenderer.wrapLines(Text.translatable("hero.minewatch." + preview.id + ".abilities"), 150)) {
            ctx.drawText(textRenderer, l, RW - 16 - textRenderer.getWidth(l), y, CYAN, true); y += 10;
        }
        // 스킨 선택 상자(표시용)
        int bx = RW - 16 - 96, by = y + 8;
        ctx.fill(bx, by, bx + 96, by + 14, 0xAAFFFFFF);
        ctx.drawText(textRenderer, Text.translatable("screen.minewatch.pick.skin").getString() + ": " + Text.translatable("screen.minewatch.pick.skin.default").getString() + "  ▾",
                bx + 4, by + 3, 0xFF2B3550, false);
    }

    private void drawBottom(DrawContext ctx, int rmx, int rmy) {
        // 가로 선(양 끝은 얇게)
        int ly = 158;
        ctx.fill(24, ly, RW - 24, ly + 1, 0x66FFFFFF);
        ctx.fillGradient(14, ly, 24, ly + 1, 0x00FFFFFF, 0x66FFFFFF);
        // 역할 버튼 4개(탱커/공격/지원/무작위)
        String[] glyph = {"tank", "damage", "support", "flex"};
        for (int i = 0; i < 4; i++) {
            int cx = RW / 2 + (int) ((i - 1.5) * 24), cy = ly;
            boolean on = i == 3 ? randomTab : (!randomTab && tab.ordinal() == i);
            boolean over = Math.abs(rmx - cx) < 10 && Math.abs(rmy - cy) < 10;
            int r = on || over ? 10 : 9;
            Sprites.draw(ctx, Sprites.gui(on ? "pick/team_btn_on" : "pick/team_btn"), cx - r, cy - r, 2 * r, 2 * r, 96, 96, WHITE);
            Sprites.draw(ctx, Sprites.gui("pick/team_glyph_" + glyph[i]), cx - 6, cy - 6, 12, 12, 64, 64, on ? 0xFF1B2440 : WHITE);
            final int idx = i;
            hits.add(new Hit(cx - 10, cy - 10, 20, 20, () -> {
                if (idx == 3) randomTab = true; else { randomTab = false; tab = Role.values()[idx]; }
                UiSound.play("pick_hover");
                List<Hero> l = heroesOfTab();
                if (!l.isEmpty() && !l.contains(preview)) setPreview(l.get(0));
            }));
        }

        // 영웅 목록: 역할 이름 + 프로필 타일
        List<Hero> heroes = heroesOfTab();
        Text label = randomTab ? Text.translatable("screen.minewatch.pick.role.random") : Text.translatable("role.minewatch." + tab.key);
        ctx.drawText(textRenderer, label, 24, 168, 0xFFFFFFFF, true);
        int ts = 32, x = 24;
        for (Hero h : heroes) {
            boolean sel = h == preview, over = rmx >= x && rmx < x + ts && rmy >= 180 && rmy < 180 + ts;
            int fc = sel ? YELLOW : over ? WHITE : 0xCC1B2440;
            ctx.fill(x - 2, 178, x + ts + 2, 180 + ts + 2, fc);
            ctx.drawTexture(Sprites.gui("pick/roster_bust_" + h.id), x, 180, ts, ts, 0, 0, 111, 111, 111, 111);
            Text nm = Text.translatable("hero.minewatch." + h.id);
            var m = ctx.getMatrices();
            m.push(); m.translate(x + ts / 2f, 180 + ts + 4, 0); m.scale(0.8f, 0.8f, 1f);
            ctx.drawCenteredTextWithShadow(textRenderer, nm, 0, 0, sel ? YELLOW : 0xFFFFFFFF);
            m.pop();
            final Hero hh = h;
            hits.add(new Hit(x, 180, ts, ts, () -> setPreview(hh)));
            x += ts + 6;
        }

        // 확인 버튼(Seafle 스프라이트: "확인" 글자 포함)
        int bw = 54, bh = 34, bx = RW / 2 - bw / 2, by = RH - bh - 2;
        boolean over = rmx >= bx && rmx < bx + bw && rmy >= by && rmy < by + bh;
        Sprites.draw(ctx, Sprites.gui("pick/footer_confirm"), bx, by, bw, bh, 220, 138, over ? 0xFFFFFFFF : 0xFFE8E8E8);
        hits.add(new Hit(bx, by, bw, bh, this::confirm));

        // 하단 단축키 안내
        keyHint(ctx, 14, RH - 11, "ESC", Text.translatable("gui.back").getString());
        keyHint(ctx, RW - 118, RH - 11, "ENTER", Text.translatable("screen.minewatch.pick.confirm").getString());
    }

    private void keyHint(DrawContext ctx, int x, int y, String key, String label) {
        int kw = textRenderer.getWidth(key) + 6;
        ctx.fill(x, y - 1, x + kw, y + 9, 0xCC20283F);
        ctx.drawText(textRenderer, key, x + 3, y, 0xFFFFFFFF, false);
        ctx.drawText(textRenderer, label, x + kw + 4, y, 0xFFFFFFFF, true);
    }

    /** 위쪽이 뾰족한 육각형(스캔라인으로 채움). */
    private static void hex(DrawContext ctx, int cx, int cy, int r, int argb) {
        for (int dy = -r; dy <= r; dy++) {
            double hw = Math.abs(dy) <= r / 2.0 ? r * 0.866 : 1.732 * (r - Math.abs(dy));
            ctx.fill((int) (cx - hw), cy + dy, (int) (cx + hw), cy + dy + 1, argb);
        }
    }

    // ---- 입력 ----

    private void confirm() {
        if (client != null && client.world != null) {
            ClientPlayNetworking.send(new SelectHeroPayload(preview.id));
            UiSound.play("pick_confirm_equip");
            client.setScreen(parent);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int rx = (int) ((mx - ox) / u), ry = (int) ((my - oy) / u);
        for (Hit h : hits) if (h.contains(rx, ry)) { h.action().run(); return true; }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        List<Hero> l = heroesOfTab();
        int i = l.indexOf(preview);
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { confirm(); return true; }
        if (key == GLFW.GLFW_KEY_RIGHT && !l.isEmpty()) { setPreview(l.get((i + 1) % l.size())); return true; }
        if (key == GLFW.GLFW_KEY_LEFT && !l.isEmpty()) { setPreview(l.get((i - 1 + l.size()) % l.size())); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void close() { UiSound.play("pick_close"); client.setScreen(parent); }
}
