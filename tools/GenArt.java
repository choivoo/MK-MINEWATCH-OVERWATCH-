import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * 영웅 초상화(도트, 큐브 느낌)와 능력/퍽 아이콘(흰색 마스크)을 만든다.
 * 사용: java tools/GenArt.java src/main/resources/assets/minewatch/textures/gui
 */
public class GenArt {
    static final Map<Character, Integer> COLORS = Map.ofEntries(
            Map.entry('h', 0x5B4636), Map.entry('s', 0xE0AC82), Map.entry('t', 0xBC8559), Map.entry('v', 0xE23B3B),
            Map.entry('V', 0xFF9A9A), Map.entry('w', 0xF2F2F2), Map.entry('b', 0x2F5DA8), Map.entry('n', 0x1E3A68),
            Map.entry('k', 0x14161B), Map.entry('g', 0x6C7480), Map.entry('y', 0xFFD23F), Map.entry('p', 0x8C7BC9),
            Map.entry('P', 0x6455A3), Map.entry('o', 0xF99E1A), Map.entry('d', 0x3A3F4A), Map.entry('a', 0xA9B2BD),
            Map.entry('f', 0xE9E9E9), Map.entry('r', 0xB8352F), Map.entry('l', 0xF2D06B), Map.entry('L', 0xC9A23F),
            Map.entry('m', 0xF0F0F0), Map.entry('c', 0x00D4FF), Map.entry('G', 0xD4AF37), Map.entry('e', 0x9A6A4A),
            Map.entry('u', 0x8A9A5B), Map.entry('z', 0x2B6E5C));

    static String[] center(String[] rows) {
        String[] out = new String[16];
        for (int i = 0; i < 16; i++) {
            String r = i < rows.length ? rows[i] : "";
            int left = (16 - r.length()) / 2;
            out[i] = ".".repeat(Math.max(0, left)) + r.replace(' ', '.') + ".".repeat(Math.max(0, 16 - left - r.length()));
        }
        return out;
    }

    static final String[] SOLDIER = {
        "", "  hhhhhh", " hhhhhhhh", " hhhhhhhh", " hsssssssh", " hvvvvvvvh", " hvVvvvVvh", " hsssssssh",
        "  sssttss", "  ssssss", "   ssss", " bbwwwwbb", "bbbbwwbbbb", "bbbbbwbbbbb", "bbbbbbbbbbbb", "nbbbbbbbbbbn"};
    static final String[] WIDOW = {
        "", "  kkkkkk", " kkkkkkkk", "kkkpppppkk", "kkpppppppk", "kkpyPppyPk", "kkpyyppyyk", "kkppppppkk",
        " kppPPPPpk", "  pppppp", "   pPPp", " kkppppkk", "kkkkpppkkkk", "kkkkkkkkkkkk", "kdkkkkkkkkdk", "dddkkkkkkddd"};
    static final String[] REIN = {
        "   aaaa", "  aaaaaa", " aaaGGaaa", " aaaGGaaa", " aasssssaa", " aassksskaa"
                .replace("k", "k"), " ffsssssff", " ffsssssff", " fffssssfff", " ffffffffff", "  fffffff", " aagaagaaa", "aaggggggggaa", "aaggaagaggaa", "agggggggggga", "aaaaaaaaaaaa"};
    static final String[] HOG = {
        "", "", "  sssssss", " ssssssssss", " sssmmmmsss", " ssmmkkmmss", " ssmkmmkmss", " ssmmkkmmss",
        " sssmmmmsss", "  ssssssss", "  zzzzzzzz", " zzzzzzzzzz", "zzzkkzzkkzzz", "zzzzzzzzzzzz", "ggzzzzzzzzgg", "gggggggggggg"};
    static final String[] ANA = {
        "", "  nnnnnn", " nnbbbbnn", "nnbbbbbbnn", "nnhhhhhhnn", "nnhssssshnn".substring(0, 10), " hssssssh", " hsnsssnsh",
        " hsssssssh", "  ssseess", "  ssssss", "   ssss", " bbnnnnbb", "bbbbnnbbbb", "bbbbbbbbbbbb", "nbbbbbbbbbbn"};
    static final String[] MERCY = {
        " GGGGGGGG", "G        G", " GGGGGGGG", "  llllll", " llllllll", " lllsssslll", " llsssssslll"
                .substring(0, 11), " lssbssbsl", " lsssssssl", "  ssstss", "  ssssss", "  wwwwww", " wwGwwGww", "wwwwwwwwww", "wwwwwwwwwwww", "wGwwwwwwwwGw"};

    static void bust(Path out, String[] rows, int bgA, int bgB, boolean mirror) throws Exception {
        String[] g = center(rows);
        BufferedImage im = new BufferedImage(111, 111, BufferedImage.TYPE_INT_ARGB);
        Graphics2D d = im.createGraphics();
        d.setPaint(new GradientPaint(0, 0, new Color(bgA), 0, 111, new Color(bgB)));
        d.fillRect(0, 0, 111, 111);
        int cell = 7, ox = (111 - cell * 16) / 2, oy = (111 - cell * 16) / 2 + 3;
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                char c = g[y].charAt(mirror ? 15 - x : x);
                if (c == '.') continue;
                Integer rgb = COLORS.get(c);
                if (rgb == null) continue;
                d.setColor(new Color(rgb));
                d.fillRect(ox + x * cell, oy + y * cell, cell, cell);
                d.setColor(new Color(0, 0, 0, 50));                         // 큐브 느낌의 아래쪽 음영
                d.fillRect(ox + x * cell, oy + y * cell + cell - 1, cell, 1);
            }
        d.dispose();
        Files.createDirectories(out.getParent());
        ImageIO.write(im, "png", out.toFile());
    }

    // ---- 아이콘(흰색 마스크, 64x64) ----
    static Graphics2D begin(BufferedImage im) {
        Graphics2D g = im.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        return g;
    }

    interface Glyph { void draw(Graphics2D g); }

    static void icon(Path out, Glyph gl) throws Exception {
        BufferedImage im = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = begin(im);
        gl.draw(g);
        g.dispose();
        Files.createDirectories(out.getParent());
        ImageIO.write(im, "png", out.toFile());
    }

    static void poly(Graphics2D g, int... xy) {
        Polygon p = new Polygon();
        for (int i = 0; i < xy.length; i += 2) p.addPoint(xy[i], xy[i + 1]);
        g.fillPolygon(p);
    }

    static void chevrons(Graphics2D g, int n) {
        for (int i = 0; i < n; i++) { int x = 12 + i * 14; g.drawLine(x, 14, x + 16, 32); g.drawLine(x + 16, 32, x, 50); }
    }

    static void clock(Graphics2D g, boolean two) {
        g.draw(new Ellipse2D.Double(10, 10, 44, 44));
        g.drawLine(32, 32, 32, 18); g.drawLine(32, 32, 44, 38);
        if (two) g.fill(new Ellipse2D.Double(28, 54, 8, 8));
    }

    static void shield(Graphics2D g, boolean barrier) {
        Path2D p = new Path2D.Double();
        p.moveTo(32, 6); p.lineTo(54, 14); p.lineTo(50, 40); p.quadTo(44, 54, 32, 60); p.quadTo(20, 54, 14, 40); p.lineTo(10, 14); p.closePath();
        if (barrier) { g.draw(p); g.draw(new Arc2D.Double(18, 16, 28, 28, 200, 140, Arc2D.OPEN)); } else g.fill(p);
    }

    static void heart(Graphics2D g) {
        Path2D p = new Path2D.Double();
        p.moveTo(32, 56); p.curveTo(2, 34, 10, 6, 32, 20); p.curveTo(54, 6, 62, 34, 32, 56); p.closePath();
        g.fill(p);
    }

    static void plus(Graphics2D g, int cx, int cy, int r) { g.fillRect(cx - 4, cy - r, 8, 2 * r); g.fillRect(cx - r, cy - 4, 2 * r, 8); }

    public static void main(String[] a) throws Exception {
        Path root = Path.of(a[0]);
        Path pick = root.resolve("pick");
        bust(pick.resolve("roster_bust_soldier76.png"), SOLDIER, 0x2A3A5A, 0x141C2E, false);
        bust(pick.resolve("roster_bust_widowmaker.png"), WIDOW, 0x4A2A6A, 0x1A1030, false);
        bust(pick.resolve("roster_bust_reinhardt.png"), REIN, 0x4A4F5C, 0x1C1F26, false);
        bust(pick.resolve("roster_bust_roadhog.png"), HOG, 0x4F5A2E, 0x1F2410, false);
        bust(pick.resolve("roster_bust_ana.png"), ANA, 0x1E4A6A, 0x0E1E30, false);
        bust(pick.resolve("roster_bust_mercy.png"), MERCY, 0x6A5A2A, 0x30280E, false);

        Path ab = root.resolve("ability");
        icon(ab.resolve("sprint.png"), g -> chevrons(g, 3));
        icon(ab.resolve("helix.png"), g -> { poly(g, 10, 54, 18, 36, 28, 46); poly(g, 22, 40, 52, 10, 56, 14, 26, 44); g.drawLine(8, 40, 20, 52); });
        icon(ab.resolve("grapple.png"), g -> { g.draw(new Arc2D.Double(10, 6, 34, 34, 90, 200, Arc2D.OPEN)); g.drawLine(44, 23, 56, 56); poly(g, 50, 52, 62, 52, 56, 62); });
        icon(ab.resolve("venom.png"), g -> { g.fill(new Ellipse2D.Double(14, 22, 36, 30)); poly(g, 32, 4, 22, 22, 42, 22); g.fillRect(8, 50, 48, 6); });
        icon(ab.resolve("charge.png"), g -> { poly(g, 6, 32, 34, 8, 34, 24, 58, 24, 58, 40, 34, 40, 34, 56); });
        icon(ab.resolve("firestrike.png"), g -> { Path2D p = new Path2D.Double(); p.moveTo(8, 50); p.curveTo(20, 40, 12, 30, 28, 24); p.curveTo(40, 20, 34, 12, 52, 8); p.curveTo(46, 22, 56, 30, 44, 42); p.curveTo(34, 52, 24, 52, 8, 50); g.fill(p); });
        icon(ab.resolve("breather.png"), g -> { g.draw(new Ellipse2D.Double(8, 8, 48, 48)); plus(g, 32, 32, 14); });
        icon(ab.resolve("hook.png"), g -> { for (int i = 0; i < 3; i++) g.draw(new Ellipse2D.Double(6 + i * 14, 6 + i * 12, 16, 12)); g.draw(new Arc2D.Double(34, 34, 24, 24, 180, 220, Arc2D.OPEN)); });
        icon(ab.resolve("grenade.png"), g -> { g.fill(new Ellipse2D.Double(12, 20, 40, 40)); g.fillRect(26, 8, 12, 14); g.drawLine(38, 10, 52, 6); });
        icon(ab.resolve("sleep.png"), g -> { g.drawLine(6, 58, 40, 24); poly(g, 38, 26, 50, 14, 54, 30); g.setStroke(new BasicStroke(4f)); g.drawLine(40, 6, 54, 6); g.drawLine(54, 6, 40, 20); g.drawLine(40, 20, 54, 20); });
        icon(ab.resolve("guardian.png"), g -> { for (int s = -1; s <= 1; s += 2) { Path2D p = new Path2D.Double(); p.moveTo(32, 40); p.curveTo(32 + s * 6, 10, 32 + s * 28, 6, 32 + s * 30, 30); p.curveTo(32 + s * 22, 22, 32 + s * 14, 34, 32 + s * 10, 48); p.closePath(); g.fill(p); } });

        Path pk = root.resolve("perk");
        icon(pk.resolve("quick_reload_64.png"), g -> { g.draw(new Arc2D.Double(10, 10, 44, 44, 40, 280, Arc2D.OPEN)); poly(g, 44, 6, 58, 20, 40, 22); g.fillRect(28, 26, 8, 14); });
        icon(pk.resolve("sprint_boost_64.png"), g -> chevrons(g, 3));
        icon(pk.resolve("fleet_64.png"), g -> chevrons(g, 3));
        icon(pk.resolve("cd_primary_64.png"), g -> clock(g, false));
        icon(pk.resolve("cd_secondary_64.png"), g -> clock(g, true));
        icon(pk.resolve("ult_gain_64.png"), g -> { g.draw(new Ellipse2D.Double(6, 6, 52, 52)); poly(g, 36, 10, 18, 36, 30, 36, 26, 54, 46, 26, 34, 26); });
        icon(pk.resolve("damage_up_64.png"), g -> { g.draw(new Ellipse2D.Double(10, 14, 40, 40)); g.drawLine(30, 8, 30, 20); g.drawLine(30, 48, 30, 60); plus(g, 52, 12, 8); });
        icon(pk.resolve("tough_armor_64.png"), g -> shield(g, false));
        icon(pk.resolve("strong_barrier_64.png"), g -> shield(g, true));
        icon(pk.resolve("thick_skin_64.png"), g -> heart(g));
        icon(pk.resolve("potent_heal_64.png"), g -> { g.draw(new Ellipse2D.Double(6, 6, 52, 52)); plus(g, 32, 32, 16); });
        System.out.println("generated art");
    }
}
