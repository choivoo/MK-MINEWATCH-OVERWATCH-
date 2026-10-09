package com.minewatch.client;

import com.minewatch.net.KillFeedPayload;
import com.minewatch.net.MatchPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/** 상단 점수/타이머 바와 킬피드. */
final class MatchHud {
    static volatile MatchPayload match = MatchPayload.NONE;
    private record Entry(KillFeedPayload kf, long time) {}
    private static final List<Entry> FEED = new ArrayList<>();
    private static final int BLUE = 0xFF3FA9FF, RED = 0xFFFF4B4B, DARK = 0xAA0B1220;

    static void addKill(KillFeedPayload kf) {
        synchronized (FEED) { FEED.add(new Entry(kf, System.currentTimeMillis())); if (FEED.size() > 6) FEED.remove(0); }
    }
    static void reset() { match = MatchPayload.NONE; synchronized (FEED) { FEED.clear(); } }

    private static int color(int team, int mine) { return team < 0 ? 0xFFFFFFFF : team == mine ? BLUE : RED; }

    static void render(DrawContext g, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        TextRenderer tr = mc.textRenderer;
        int w = g.getScaledWindowWidth();
        MatchPayload m = match;

        if (m.state() != 0) {
            int mine = Math.max(m.myTeam(), 0);
            int my = mine == 0 ? m.scoreA() : m.scoreB(), en = mine == 0 ? m.scoreB() : m.scoreA();
            int sec = m.ticksLeft() / 20;
            String time = String.format("%d:%02d", sec / 60, sec % 60);
            g.fill(w / 2 - 60, 4, w / 2 + 60, 24, DARK);
            g.fill(w / 2 - 60, 22, w / 2 - 1, 24, BLUE);
            g.fill(w / 2 + 1, 22, w / 2 + 60, 24, RED);
            g.drawCenteredTextWithShadow(tr, String.valueOf(my), w / 2 - 42, 9, BLUE);
            g.drawCenteredTextWithShadow(tr, String.valueOf(en), w / 2 + 42, 9, RED);
            g.drawCenteredTextWithShadow(tr, time, w / 2, 9, 0xFFFFFFFF);
            String label = switch (m.state()) {
                case 1 -> "준비: " + (sec + 1);
                case 3 -> m.scoreA() == m.scoreB() ? "무승부" : ((m.scoreA() > m.scoreB()) == (mine == 0) ? "승리!" : "패배");
                default -> "목표 " + m.target() + "킬";
            };
            g.drawCenteredTextWithShadow(tr, label, w / 2, 27, 0xFFFFFFFF);

            if (m.mode() == 1) {   // 점령전: 점령지 상태 바
                int bx = w / 2 - 40, by = 40;
                g.fill(bx - 1, by - 1, bx + 81, by + 7, DARK);
                int ownerCol = m.pointOwner() < 0 ? 0xFF8899AA : color(m.pointOwner(), mine);
                g.fill(bx, by, bx + 80, by + 6, (ownerCol & 0x00FFFFFF) | 0x66000000);
                if (m.pointCapTeam() >= 0 && m.pointProgress() > 0)
                    g.fill(bx, by, bx + Math.round(80 * m.pointProgress() / 100f), by + 6, color(m.pointCapTeam(), mine));
                String pt = m.pointOwner() < 0 ? "점령지: 중립" : (m.pointOwner() == mine ? "점령지: 아군" : "점령지: 적군");
                g.drawCenteredTextWithShadow(tr, pt, w / 2, by + 9, 0xFFFFFFFF);
            }
        }

        long now = System.currentTimeMillis();
        int mine = Math.max(m.myTeam(), 0), y = 6;
        synchronized (FEED) {
            FEED.removeIf(e -> now - e.time > 6000);
            for (Entry e : FEED) {
                KillFeedPayload k = e.kf;
                String killer = k.killer().isEmpty() ? "" : k.killer() + "  ▶  ";
                int vw = tr.getWidth(k.victim()), kw = tr.getWidth(killer);
                int x = w - 8 - vw - kw;
                g.fill(x - 3, y - 2, w - 5, y + 10, DARK);
                if (!killer.isEmpty()) g.drawText(tr, killer, x, y, color(k.killerTeam(), mine), true);
                g.drawText(tr, k.victim(), x + kw, y, color(k.victimTeam(), mine), true);
                y += 13;
            }
        }
    }
    private MatchHud() {}
}
