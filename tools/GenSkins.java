import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * 영웅 스킨(64x64 마인크래프트 플레이어 스킨)과 얼굴 샷(프로필)을 만든다.
 *   java tools/GenSkins.java skins <출력 폴더>              영웅 스킨 PNG 생성 (트레이서는 Seafle 스킨을 복사해서 쓴다)
 *   java tools/GenSkins.java busts <스킨 폴더> <출력 폴더>   각 스킨의 머리/어깨를 3D 로 렌더링해 111x111 프로필 PNG 생성
 *   java tools/GenSkins.java preview <스킨.png> <출력.png> [배율]   스킨 전개도를 확대해서 확인용으로 저장
 */
public class GenSkins {
    // ---- 스킨 영역: {u, v, 너비, 높이, 깊이} ----
    static final Map<String, int[]> PART = new HashMap<>();
    static {
        PART.put("head", new int[]{0, 0, 8, 8, 8});    PART.put("hat", new int[]{32, 0, 8, 8, 8});
        PART.put("body", new int[]{16, 16, 8, 12, 4}); PART.put("jacket", new int[]{16, 32, 8, 12, 4});
        PART.put("rArm", new int[]{40, 16, 4, 12, 4}); PART.put("rSleeve", new int[]{40, 32, 4, 12, 4});
        PART.put("lArm", new int[]{32, 48, 4, 12, 4}); PART.put("lSleeve", new int[]{48, 48, 4, 12, 4});
        PART.put("rLeg", new int[]{0, 16, 4, 12, 4});  PART.put("rPants", new int[]{0, 32, 4, 12, 4});
        PART.put("lLeg", new int[]{16, 48, 4, 12, 4}); PART.put("lPants", new int[]{0, 48, 4, 12, 4});
    }

    static int[] face(String part, String face) {
        int[] p = PART.get(part);
        int u = p[0], v = p[1], w = p[2], h = p[3], d = p[4];
        return switch (face) {
            case "top" -> new int[]{u + d, v, w, d};
            case "bottom" -> new int[]{u + d + w, v, w, d};
            case "right" -> new int[]{u, v + d, d, h};
            case "front" -> new int[]{u + d, v + d, w, h};
            case "left" -> new int[]{u + d + w, v + d, d, h};
            case "back" -> new int[]{u + 2 * d + w, v + d, w, h};
            default -> throw new IllegalArgumentException(face);
        };
    }

    static final class Skin {
        final BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        void px(int x, int y, int rgb) { if (x >= 0 && y >= 0 && x < 64 && y < 64) img.setRGB(x, y, 0xFF000000 | rgb); }
        void clear(int x, int y) { img.setRGB(x, y, 0); }
        void rect(int u, int v, int w, int h, int rgb) { for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) px(u + x, v + y, rgb); }
        /** 부위의 모든 면을 한 색으로 칠한다. */
        void all(String part, int rgb) { int[] p = PART.get(part); rect(p[0], p[1], 2 * p[4] + 2 * p[2], p[4] + p[3], rgb); clearCorners(part); }
        void clearCorners(String part) {
            int[] p = PART.get(part);
            for (int y = 0; y < p[4]; y++) for (int x = 0; x < p[4]; x++) { clear(p[0] + x, p[1] + y); clear(p[0] + p[4] + 2 * p[2] + x, p[1] + y); }
        }
        void fill(String part, String f, int rgb) { int[] r = face(part, f); rect(r[0], r[1], r[2], r[3], rgb); }
        void at(String part, String f, int x, int y, int rgb) { int[] r = face(part, f); if (x >= 0 && y >= 0 && x < r[2] && y < r[3]) px(r[0] + x, r[1] + y, rgb); }
        void row(String part, String f, int y, int rgb) { int[] r = face(part, f); for (int x = 0; x < r[2]; x++) px(r[0] + x, r[1] + y, rgb); }
        void col(String part, String f, int x, int rgb) { int[] r = face(part, f); for (int y = 0; y < r[3]; y++) px(r[0] + x, r[1] + y, rgb); }
        void sub(String part, String f, int x, int y, int w, int h, int rgb) { for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) at(part, f, x + i, y + j, rgb); }
        void eraseSub(String part, String f, int x, int y, int w, int h) {
            int[] r = face(part, f);
            for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) clear(r[0] + x + i, r[1] + y + j);
        }
        /** 모자 층 전체를 비운다. */
        void clearHat() { for (int y = 0; y < 16; y++) for (int x = 32; x < 64; x++) clear(x, y); }
    }

    /** 얼굴(기본 층): 피부색 + 눈 + 입. */
    static void face(Skin s, int skin, int eyeWhite, int iris, int mouth) {
        s.all("head", skin);
        s.sub("head", "front", 1, 4, 2, 1, eyeWhite); s.sub("head", "front", 5, 4, 2, 1, eyeWhite);
        s.at("head", "front", 2, 4, iris); s.at("head", "front", 5, 4, iris);
        s.sub("head", "front", 3, 6, 2, 1, mouth);
    }

    /** 몸통/팔다리를 기본색으로. 장갑/신발 줄 포함. */
    static void outfit(Skin s, int body, int arm, int glove, int leg, int boot, int skinTone, boolean bareArms) {
        s.all("body", body); s.all("jacket", body);
        for (String a : new String[]{"rArm", "lArm"}) s.all(a, bareArms ? skinTone : arm);
        for (String a : new String[]{"rSleeve", "lSleeve"}) s.all(a, arm);
        for (String a : new String[]{"rLeg", "lLeg", "rPants", "lPants"}) s.all(a, leg);
        for (String a : new String[]{"rArm", "lArm", "rSleeve", "lSleeve"})
            for (String f : new String[]{"front", "back", "left", "right"}) s.sub(a, f, 0, 9, 4, 3, glove);
        for (String a : new String[]{"rLeg", "lLeg", "rPants", "lPants"})
            for (String f : new String[]{"front", "back", "left", "right"}) s.sub(a, f, 0, 9, 4, 3, boot);
    }

    static void both(Skin s, java.util.function.Consumer<String[]> fn) {
        fn.accept(new String[]{"rArm", "rSleeve"});
        fn.accept(new String[]{"lArm", "lSleeve"});
    }

    // ---- 영웅 ----
    static Skin soldier76() {
        Skin s = new Skin();
        int navy = 0x23407A, white = 0xF2F2F2, gray = 0x3A3F4A;
        outfit(s, navy, navy, 0x1E2128, gray, 0x14161B, 0xD9A27A, false);
        face(s, 0xD9A27A, 0xF2F2F2, 0x2B2B2B, 0xA86B4A);
        s.fill("head", "top", 0x3F3328);
        for (String f : new String[]{"front", "left", "right", "back"}) s.row("head", f, 0, 0x3F3328);
        s.sub("head", "front", 0, 1, 1, 1, 0x3F3328); s.sub("head", "front", 7, 1, 1, 1, 0x3F3328);
        for (String f : new String[]{"left", "right"}) s.sub("head", f, 0, 1, 8, 2, 0x7A7A7A);
        s.fill("head", "back", 0x3F3328); s.sub("head", "back", 0, 5, 8, 3, 0xD9A27A);
        // 붉은 바이저
        s.sub("head", "front", 0, 3, 8, 2, 0xC62828); s.sub("head", "front", 1, 3, 2, 1, 0xFF8A80); s.sub("head", "front", 5, 3, 2, 1, 0xFF8A80);
        s.sub("head", "left", 5, 3, 3, 2, 0xC62828); s.sub("head", "right", 0, 3, 3, 2, 0xC62828);
        // 모자 층: 바이저 테두리 + 머리카락 두께
        s.clearHat();
        s.sub("hat", "front", 0, 2, 8, 1, 0x2A2A2A); s.sub("hat", "front", 0, 5, 8, 1, 0x2A2A2A);
        s.fill("hat", "top", 0x4A3B30);
        for (String f : new String[]{"front", "left", "right", "back"}) s.row("hat", f, 0, 0x4A3B30);
        // 몸통: 흰 칼라, 등에 "76"
        s.sub("body", "front", 2, 0, 4, 1, white); s.col("body", "front", 3, white); s.col("body", "front", 4, white);
        s.sub("body", "front", 0, 6, 8, 1, 0x1A2B55);
        s.sub("body", "back", 1, 3, 3, 1, white); s.sub("body", "back", 3, 4, 1, 4, white);                   // 7
        s.sub("body", "back", 4, 3, 3, 1, white); s.col("body", "back", 4, white); s.sub("body", "back", 4, 5, 3, 1, white);
        s.sub("body", "back", 4, 7, 3, 1, white); s.col("body", "back", 6, white);                             // 6
        s.sub("body", "front", 0, 10, 8, 2, 0x3A3F4A);
        both(s, p -> { for (String f : new String[]{"front", "back", "left", "right"}) { s.row(p[1], f, 4, white); s.row(p[0], f, 4, white); } });
        return s;
    }

    static Skin widowmaker() {
        Skin s = new Skin();
        int suit = 0x20192E, purple = 0x6C4BB8;
        outfit(s, suit, suit, 0x2D2150, suit, 0x14101C, 0x8B7CC8, false);
        face(s, 0x8B7CC8, 0xFFD23F, 0x120E1A, 0x4B3A8C);
        s.sub("head", "front", 1, 4, 2, 1, 0xFFD23F); s.sub("head", "front", 5, 4, 2, 1, 0xFFD23F);
        s.at("head", "front", 1, 3, 0x120E1A); s.at("head", "front", 2, 3, 0x120E1A); s.at("head", "front", 5, 3, 0x120E1A); s.at("head", "front", 6, 3, 0x120E1A);
        for (String f : new String[]{"top", "left", "right", "back"}) s.fill("head", f, 0x16131F);
        s.sub("head", "front", 0, 0, 8, 2, 0x16131F); s.sub("head", "front", 0, 2, 1, 2, 0x16131F); s.sub("head", "front", 7, 2, 1, 2, 0x16131F);
        s.sub("head", "left", 0, 3, 3, 5, 0x8B7CC8); s.sub("head", "right", 5, 3, 3, 5, 0x8B7CC8);
        s.clearHat();
        s.fill("hat", "back", 0x1D1927); s.sub("hat", "left", 4, 0, 4, 8, 0x1D1927); s.sub("hat", "right", 0, 0, 4, 8, 0x1D1927);
        s.fill("hat", "top", 0x1D1927); s.row("hat", "front", 0, 0x1D1927);
        s.col("body", "front", 3, purple); s.col("body", "front", 4, purple); s.sub("body", "front", 2, 3, 4, 1, purple); s.sub("body", "front", 3, 2, 2, 1, 0xC9B8FF);
        s.sub("body", "front", 0, 8, 8, 1, purple); s.sub("body", "back", 0, 8, 8, 1, purple); s.col("body", "back", 3, purple); s.col("body", "back", 4, purple);
        both(s, p -> { for (String f : new String[]{"front", "back", "left", "right"}) { s.row(p[1], f, 3, purple); s.row(p[0], f, 3, purple); } });
        for (String a : new String[]{"rLeg", "lLeg", "rPants", "lPants"}) for (String f : new String[]{"front", "back"}) s.row(a, f, 5, purple);
        return s;
    }

    static Skin reinhardt() {
        Skin s = new Skin();
        int silver = 0xA9B2BD, dark = 0x59616B, gold = 0xD4AF37;
        outfit(s, silver, silver, 0x3A3F4A, dark, 0x2A2F38, 0xD9A07A, false);
        face(s, 0xD9A07A, 0xF2F2F2, 0x3A5F8A, 0xA86B4A);
        s.sub("head", "front", 0, 5, 8, 3, 0xDDDDDD); s.sub("head", "front", 3, 6, 2, 1, 0xA86B4A);
        s.at("head", "front", 6, 3, 0xA85A4A); s.at("head", "front", 6, 2, 0xA85A4A);
        s.sub("head", "left", 3, 5, 5, 3, 0xDDDDDD); s.sub("head", "right", 0, 5, 5, 3, 0xDDDDDD); s.sub("head", "back", 0, 5, 8, 3, 0xDDDDDD);
        // 투구(모자 층): 얼굴 부분만 열려 있다
        s.clearHat();
        for (String f : new String[]{"top", "back", "left", "right", "front"}) s.fill("hat", f, 0xB4BCC6);
        s.eraseSub("hat", "front", 1, 2, 6, 6);
        s.col("hat", "top", 3, gold); s.col("hat", "top", 4, gold); s.col("hat", "back", 3, gold); s.col("hat", "back", 4, gold);
        s.sub("hat", "front", 3, 0, 2, 2, gold);
        s.sub("body", "front", 1, 1, 6, 1, gold); s.sub("body", "front", 3, 2, 2, 3, gold); s.sub("body", "front", 0, 9, 8, 1, gold);
        s.sub("body", "back", 1, 1, 6, 1, gold); s.sub("body", "back", 3, 2, 2, 3, gold);
        both(s, p -> {
            for (String f : new String[]{"front", "back", "left", "right", "top"}) { s.row(p[1], f, 0, gold); s.row(p[0], f, 0, gold); }
            s.sub(p[1], "front", 0, 1, 4, 2, 0xC4CCD6);
        });
        for (String a : new String[]{"rLeg", "lLeg", "rPants", "lPants"}) for (String f : new String[]{"front", "back", "left", "right"}) { s.row(a, f, 5, gold); s.row(a, f, 6, gold); }
        return s;
    }

    static Skin roadhog() {
        Skin s = new Skin();
        int green = 0x55723A, skin = 0xD9A27A, strap = 0x2B2B2B;
        outfit(s, green, skin, 0x2B2B2B, 0x3F4A2B, 0x14161B, skin, true);
        face(s, skin, 0xF2F2F2, 0x2B2B2B, 0xA86B4A);
        s.fill("head", "top", skin);
        s.clearHat();
        s.sub("hat", "front", 0, 2, 8, 6, 0xF0F0F0);
        s.sub("hat", "front", 1, 3, 2, 2, 0x111111); s.sub("hat", "front", 5, 3, 2, 2, 0x111111); s.sub("hat", "front", 3, 5, 2, 3, 0x9AA3AD);
        s.sub("hat", "left", 3, 2, 5, 6, 0xF0F0F0); s.sub("hat", "right", 0, 2, 5, 6, 0xF0F0F0); s.sub("hat", "back", 0, 2, 8, 2, 0xF0F0F0);
        s.sub("hat", "front", 0, 2, 8, 1, 0xC8C8C8);
        s.at("head", "top", 3, 3, 0xA86B4A); s.at("head", "top", 4, 4, 0xA86B4A);
        s.sub("body", "front", 0, 0, 2, 12, strap); s.sub("body", "front", 6, 0, 2, 12, strap);
        s.sub("body", "front", 0, 9, 8, 2, 0x5A3E24); s.sub("body", "front", 1, 11, 6, 1, 0x9AA3AD);
        s.sub("body", "back", 0, 9, 8, 2, 0x5A3E24);
        s.sub("body", "front", 3, 3, 2, 3, 0x3A4A28);
        both(s, p -> { s.sub(p[0], "front", 1, 3, 2, 1, 0x7B4A3A); s.sub(p[0], "front", 1, 5, 2, 1, 0x7B4A3A); });
        return s;
    }

    static Skin ana() {
        Skin s = new Skin();
        int navy = 0x1F3560, light = 0x6EA8E8, tan = 0x9A7B55;
        outfit(s, navy, navy, 0xC9B08A, 0x182A4F, tan, 0xB07A55, false);
        face(s, 0xB07A55, 0xF2F2F2, 0x1F1A17, 0x7A4A3A);
        s.at("head", "front", 6, 3, 0x3A6EC8); s.at("head", "front", 6, 5, 0x3A6EC8); s.at("head", "front", 7, 4, 0x3A6EC8);
        s.fill("head", "top", 0x1A1A22); s.row("head", "front", 0, 0x1A1A22);
        s.clearHat();
        s.fill("hat", "top", 0x2F5DA8); s.fill("hat", "back", 0x2F5DA8); s.sub("hat", "left", 0, 0, 8, 6, 0x2F5DA8); s.sub("hat", "right", 0, 0, 8, 6, 0x2F5DA8);
        s.sub("hat", "front", 0, 0, 8, 2, 0x2F5DA8); s.sub("hat", "front", 0, 2, 8, 1, 0xDADDE6); s.sub("hat", "front", 0, 3, 1, 3, 0x2F5DA8); s.sub("hat", "front", 7, 3, 1, 3, 0x2F5DA8);
        s.sub("hat", "back", 0, 6, 8, 2, 0x2F5DA8);
        s.sub("body", "front", 2, 1, 4, 5, light); s.col("body", "front", 3, 0xF2F2F2); s.sub("body", "front", 0, 9, 8, 1, tan);
        s.sub("body", "back", 2, 1, 4, 5, 0x2A4A85); s.sub("body", "back", 0, 9, 8, 1, tan);
        both(s, p -> { for (String f : new String[]{"front", "back", "left", "right"}) { s.row(p[1], f, 3, light); s.row(p[0], f, 3, light); } });
        for (String a : new String[]{"rLeg", "lLeg", "rPants", "lPants"}) for (String f : new String[]{"front", "back", "left", "right"}) s.row(a, f, 5, light);
        return s;
    }

    static Skin mercy() {
        Skin s = new Skin();
        int white = 0xF2F2F2, gold = 0xD4AF37, blond = 0xF2D06B, skin = 0xF2C7A5;
        outfit(s, white, white, gold, white, gold, skin, false);
        face(s, skin, 0xF2F2F2, 0x4A7FD6, 0xD98A8A);
        s.fill("head", "top", blond); s.sub("head", "front", 0, 0, 8, 2, blond); s.sub("head", "front", 0, 2, 1, 3, blond); s.sub("head", "front", 7, 2, 1, 3, blond);
        s.fill("head", "back", blond); s.sub("head", "left", 0, 0, 8, 8, blond); s.sub("head", "right", 0, 0, 8, 8, blond);
        s.sub("head", "left", 0, 3, 3, 5, skin); s.sub("head", "right", 5, 3, 3, 5, skin);
        s.clearHat();
        s.sub("hat", "back", 2, 1, 4, 7, blond); s.sub("hat", "left", 4, 0, 4, 8, blond); s.sub("hat", "right", 0, 0, 4, 8, blond);
        s.fill("hat", "top", blond);
        s.row("hat", "front", 0, 0xFFE066); s.row("hat", "left", 0, 0xFFE066); s.row("hat", "right", 0, 0xFFE066); s.row("hat", "back", 0, 0xFFE066);
        s.sub("hat", "top", 1, 1, 6, 1, 0xFFE066); s.sub("hat", "top", 1, 6, 6, 1, 0xFFE066); s.sub("hat", "top", 1, 1, 1, 6, 0xFFE066); s.sub("hat", "top", 6, 1, 1, 6, 0xFFE066);
        s.sub("body", "front", 0, 0, 8, 1, gold); s.col("body", "front", 0, gold); s.col("body", "front", 7, gold); s.at("body", "front", 3, 3, 0x6EC8FF); s.at("body", "front", 4, 3, 0x6EC8FF);
        s.sub("body", "front", 0, 9, 8, 1, gold); s.sub("body", "back", 0, 9, 8, 1, gold); s.col("body", "back", 3, gold); s.col("body", "back", 4, gold);
        both(s, p -> { for (String f : new String[]{"front", "back", "left", "right"}) { s.row(p[1], f, 3, gold); s.row(p[0], f, 3, gold); } });
        for (String a : new String[]{"rLeg", "lLeg", "rPants", "lPants"}) for (String f : new String[]{"front", "back", "left", "right"}) s.row(a, f, 5, gold);
        return s;
    }

    // ---- 마무리: 면 음영 + 디더 질감 + 가장자리 어둡게(셀 셰이딩) ----
    static int hash(int x, int y, int k) {
        int h = x * 374761393 + y * 668265263 + k * 2147483647;
        h = (h ^ (h >>> 13)) * 1274126177;
        return (h ^ (h >>> 16)) & 0xFFFF;
    }

    static int mul(int rgb, double f) {
        int r = Math.min(255, Math.max(0, (int) Math.round(((rgb >> 16) & 255) * f)));
        int g = Math.min(255, Math.max(0, (int) Math.round(((rgb >> 8) & 255) * f)));
        int b = Math.min(255, Math.max(0, (int) Math.round((rgb & 255) * f)));
        return (r << 16) | (g << 8) | b;
    }

    /** 모든 부위의 모든 면에 위쪽은 밝게/아래쪽은 어둡게, 가장자리는 살짝 어둡게, 미세한 점 질감을 입힌다. */
    static void shade(Skin s) {
        int idx = 0;
        for (var e : PART.entrySet()) {
            idx++;
            for (String f : new String[]{"top", "bottom", "right", "front", "left", "back"}) {
                int[] r = face(e.getKey(), f);
                double base = switch (f) { case "top" -> 1.10; case "bottom" -> 0.72; case "front" -> 1.0; case "back" -> 0.90; default -> 0.86; };
                for (int y = 0; y < r[3]; y++) for (int x = 0; x < r[2]; x++) {
                    int p = s.img.getRGB(r[0] + x, r[1] + y);
                    if ((p >>> 24) == 0) continue;
                    double g = base * (1.06 - 0.14 * y / Math.max(1, r[3] - 1));
                    if (x == 0 || x == r[2] - 1) g *= 0.93;
                    if (y == r[3] - 1 && !f.equals("top") && !f.equals("bottom")) g *= 0.94;
                    g *= 1 + ((hash(x, y, idx * 7 + f.length()) % 1000) / 1000.0 - 0.5) * 0.07;
                    s.px(r[0] + x, r[1] + y, mul(p & 0xFFFFFF, g));
                }
            }
        }
    }

    /** 8열 문자 그림으로 한 면을 그린다. '.' 는 건드리지 않는다. */
    static void paint(Skin s, String part, String face, String[] rows, Map<Character, Integer> pal) {
        for (int y = 0; y < rows.length; y++) for (int x = 0; x < rows[y].length(); x++) {
            char c = rows[y].charAt(x);
            if (c != '.') { Integer col = pal.get(c); if (col != null) s.at(part, face, x, y, col); }
        }
    }

    static Map<Character, Integer> pal(Object... kv) {
        Map<Character, Integer> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((Character) kv[i], (Integer) kv[i + 1]);
        return m;
    }

    /** 영역을 곱셈 비율로 밝게/어둡게. */
    static void tone(Skin s, String part, String f, int x, int y, int w, int h, double k) {
        int[] r = face(part, f);
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) {
            if (x + i < 0 || y + j < 0 || x + i >= r[2] || y + j >= r[3]) continue;
            int p = s.img.getRGB(r[0] + x + i, r[1] + y + j);
            if ((p >>> 24) != 0) s.px(r[0] + x + i, r[1] + y + j, mul(p & 0xFFFFFF, k));
        }
    }

    static void toneAll(Skin s, String part, int x, int y, int w, int h, double k) {
        for (String f : new String[]{"front", "back", "left", "right"}) tone(s, part, f, x, y, w, h, k);
    }

    static void dots(Skin s, String part, String f, int rgb, int... xy) { for (int i = 0; i < xy.length; i += 2) s.at(part, f, xy[i], xy[i + 1], rgb); }

    static Skin make(String id) {
        Skin s = switch (id) {
            case "soldier76" -> soldier76(); case "widowmaker" -> widowmaker(); case "reinhardt" -> reinhardt();
            case "roadhog" -> roadhog(); case "ana" -> ana(); default -> mercy();
        };
        shade(s);
        switch (id) {
            case "soldier76" -> touchSoldier(s); case "widowmaker" -> touchWidow(s); case "reinhardt" -> touchRein(s);
            case "roadhog" -> touchHog(s); case "ana" -> touchAna(s); default -> touchMercy(s);
        }
        return s;
    }

    static void touchSoldier(Skin s) {
        var p = pal('h', 0x5A4B3E, 'H', 0x7C6A58, 'g', 0x9C9C98, 's', 0xD9A27A, 'S', 0xC48C66, 'd', 0xA8704F, 'k', 0x17181C,
                'v', 0xFF6B5E, 'r', 0xD22E2E, 'R', 0x8E1A1A, 'm', 0x8C4E3A, 'b', 0x5E4A3A, 'w', 0xF2F2F2);
        paint(s, "head", "front", new String[]{
            "hHhhHhhh",
            "hggssggh",
            "sbbssbbs",
            "kkkkkkkk",
            "kvvRRrrk",
            "sSssddsS",
            "sdsmmmsd",
            "bbsssbbb"}, p);
        paint(s, "head", "top", new String[]{"hhhhhhhh", "hHhhhhHh", "hhhHhhhh", "hhhhhhhh", "hHhhhhHh", "hhhhHhhh", "hhhhhhhh", "hhhhhhhh"}, p);
        // 흰 어깨 줄, 가슴 패드, 탄창 파우치, 무릎 보호대
        tone(s, "body", "front", 1, 1, 6, 5, 1.12); tone(s, "body", "front", 0, 6, 8, 1, 0.8);
        s.sub("body", "front", 3, 1, 2, 1, 0xE6E6E6); s.col("body", "front", 3, 0x1B2F5E);
        for (int i = 0; i < 3; i++) { s.sub("body", "front", i * 3, 9, 2, 3, 0x4C535F); s.sub("body", "front", i * 3, 9, 2, 1, 0x7B8492); }
        dots(s, "body", "front", 0xD4A64A, 1, 7, 6, 7);
        for (String a : new String[]{"rLeg", "lLeg", "rPants", "lPants"}) {
            s.sub(a, "front", 0, 6, 4, 2, 0x2A2E37); s.row(a, "front", 6, 0x4B5260);
            tone(s, a, "front", 0, 2, 4, 3, 0.9);
        }
        s.sub("rLeg", "front", 0, 2, 2, 3, 0x353B46); s.sub("rPants", "front", 0, 2, 2, 3, 0x353B46);
        both(s, q -> { s.sub(q[1], "front", 0, 1, 4, 2, 0x2D5099); tone(s, q[1], "front", 1, 5, 2, 3, 0.85); });
    }

    static void touchWidow(Skin s) {
        var p = pal('h', 0x1B1626, 'H', 0x2C2340, 's', 0x8F82CF, 'S', 0x7466B8, 'y', 0xFFD23F, 'k', 0x0C0912, 'l', 0xA99BEB, 'm', 0x3B2D78, 'p', 0x6C4BB8);
        paint(s, "head", "front", new String[]{
            "hHhhhhHh",
            "hHhhhhHh",
            "hSssssSh",
            "hkyyskyh",
            "hsskkssh",
            "hSsllsSh",
            "hsmmmmsh",
            "hhsssshh"}, p);
        tone(s, "body", "front", 0, 0, 8, 12, 0.95);
        paint(s, "body", "front", new String[]{
            "........",
            "..pppp..",
            "..pllp..",
            "...pp...",
            "..p..p..",
            "........"}, pal('p', 0x7A58CC, 'l', 0xC9B8FF));
        s.sub("body", "front", 0, 8, 8, 1, 0x7A58CC); s.sub("body", "front", 0, 9, 8, 1, 0x2A1E4A);
        dots(s, "body", "front", 0xC9B8FF, 1, 8, 6, 8);
        for (String a : new String[]{"rLeg", "lLeg", "rPants", "lPants"}) {
            s.col(a, "front", 1, 0x3A2C66); s.col(a, "front", 2, 0x3A2C66); tone(s, a, "front", 0, 0, 4, 12, 1.0);
            s.row(a, "front", 5, 0x7A58CC); s.row(a, "front", 6, 0x2A1E4A);
        }
        both(s, q -> { s.col(q[1], "front", 0, 0x3A2C66); s.sub(q[1], "front", 0, 1, 4, 1, 0x7A58CC); });
    }

    static void touchRein(Skin s) {
        var p = pal('s', 0xD9A07A, 'S', 0xC28A66, 'w', 0xF1F1F1, 'W', 0xD9D9D9, 'b', 0x3A5F8A, 'B', 0x20405E, 'm', 0x9A5A48, 'x', 0xA84A3C, 'g', 0xDDDDDD);
        paint(s, "head", "front", new String[]{
            "........",
            "sSssssSs",
            "sWwssWws",
            "sbBssbBs",
            "ssSssSss",
            "sxsSSsxs",
            "wgmmmmgw",
            "wwgwwgww"}, p);
        // 흉갑 패널 + 리벳 + 금 테두리
        for (String f : new String[]{"front", "back"}) {
            tone(s, "body", f, 0, 0, 8, 12, 0.96); tone(s, "body", f, 1, 2, 6, 4, 1.15);
            s.sub("body", f, 0, 6, 8, 1, 0x7A838F);
            dots(s, "body", f, 0xF3F6FA, 1, 2, 6, 2, 1, 5, 6, 5);
        }
        s.sub("body", "front", 2, 2, 4, 3, 0xBFC8D3); s.sub("body", "front", 3, 3, 2, 2, 0xE8C453);
        for (String a : new String[]{"rLeg", "lLeg", "rPants", "lPants"}) {
            tone(s, a, "front", 0, 1, 4, 4, 1.1);
            s.sub(a, "front", 0, 5, 4, 2, 0xC9A62E); s.row(a, "front", 7, 0x7E6718); dots(s, a, "front", 0xFFE08A, 0, 5, 3, 5);
        }
        both(s, q -> {
            s.sub(q[1], "front", 0, 0, 4, 4, 0xB4BDC8); s.sub(q[1], "front", 0, 3, 4, 1, 0x7A838F);
            dots(s, q[1], "front", 0xF3F6FA, 0, 0, 3, 0, 0, 2, 3, 2);
            s.row(q[1], "front", 0, 0xD4AF37);
        });
    }

    static void touchHog(Skin s) {
        var p = pal('s', 0xD9A27A, 'S', 0xC48C66, 'd', 0xA8704F, 'w', 0xF0F0F0, 'W', 0xBDBDBD, 'k', 0x0E0E10, 'g', 0x9AA3AD, 'm', 0x7A4A3A);
        paint(s, "head", "front", new String[]{
            "ssssssss",
            "sSssssSs",
            "sdsssdss",
            "ssSssSss",
            "ssssssss",
            "sssddsss",
            "ssmmmmss",
            "sSsssSss"}, p);
        // 마스크(모자 층): 흰 철제 + 검은 눈구멍 + 환기구
        s.clearHat();
        paint(s, "hat", "front", new String[]{
            "........",
            "........",
            "WwwwwwwW",
            "wkkwwkkw",
            "wkkwwkkw",
            "wwwggwww",
            "WwgkkgwW",
            "WwgkkgwW"}, p);
        s.sub("hat", "left", 3, 2, 5, 6, 0xE4E4E4); s.sub("hat", "right", 0, 2, 5, 6, 0xE4E4E4); s.sub("hat", "back", 0, 2, 8, 2, 0xD6D6D6);
        dots(s, "hat", "front", 0x7A7A7A, 0, 2, 7, 2, 0, 7, 7, 7);
        tone(s, "head", "top", 0, 0, 8, 8, 0.9);
        // 배: 살색 + 흉터 + 가죽 끈 + 체인
        for (String f : new String[]{"front"}) {
            tone(s, "body", f, 2, 4, 4, 5, 1.12);
            s.sub("body", f, 3, 6, 2, 1, 0xA8704F); s.at("body", f, 4, 8, 0x8C5A44);
        }
        for (int i = 0; i < 6; i++) { s.at("body", "front", 1 + i, 1 + i, 0xA8B0BA); s.at("body", "front", 1 + i, 2 + i, 0x5E656E); }
        s.sub("body", "front", 3, 9, 2, 2, 0xC9A62E);
        both(s, q -> {
            tone(s, q[0], "front", 0, 0, 4, 9, 1.0);
            s.sub(q[0], "front", 1, 4, 2, 1, 0x7B4A3A); s.sub(q[0], "front", 1, 6, 2, 1, 0x7B4A3A);
            s.sub(q[1], "front", 0, 8, 4, 1, 0x3A3A3A);
        });
        for (String a : new String[]{"rLeg", "lLeg", "rPants", "lPants"}) {
            s.sub(a, "front", 0, 6, 4, 1, 0x2B2B2B); dots(s, a, "front", 0x6B7A4C, 1, 2, 2, 3);
            tone(s, a, "front", 0, 7, 4, 2, 0.85);
        }
    }

    static void touchAna(Skin s) {
        var p = pal('s', 0xB07A55, 'S', 0x9A6844, 'd', 0x1B1A22, 'w', 0xF2F2F2, 'b', 0x3A6EC8, 'k', 0x15141A, 'c', 0x2F5DA8, 'C', 0x244A88, 'l', 0xDADDE6, 'm', 0x7A4A3A);
        paint(s, "head", "front", new String[]{
            "dddddddd",
            "dsssssss",
            "sSssssSs",
            "swkssbks",
            "ssSssbss",
            "sssSSbss",
            "ssmmmmss",
            "sSsssSss"}, p);
        // 스카프(모자 층) 가장자리 장식
        s.sub("hat", "front", 0, 0, 8, 2, 0x2F5DA8); s.row("hat", "front", 2, 0xDADDE6);
        dots(s, "hat", "front", 0x6EA8E8, 1, 1, 3, 1, 5, 1);
        for (String f : new String[]{"left", "right", "back"}) s.row("hat", f, 5, 0xDADDE6);
        // 상의: 파란 장식 띠 + 가죽 어깨끈 + 금 버클
        for (String f : new String[]{"front", "back"}) { tone(s, "body", f, 0, 0, 8, 12, 0.97); s.row("body", f, 6, 0x244A88); }
        for (int i = 0; i < 6; i++) { s.at("body", "front", 1 + i, 1 + i, 0x7A5A38); s.at("body", "front", 2 + i, 1 + i, 0x9A7B55); }
        s.sub("body", "front", 3, 9, 2, 1, 0xD4AF37);
        for (int i = 0; i < 8; i += 2) { s.at("body", "front", i, 11, 0x6EA8E8); s.at("body", "back", i, 11, 0x6EA8E8); }
        both(s, q -> { s.row(q[1], "front", 4, 0x6EA8E8); dots(s, q[1], "front", 0xDADDE6, 0, 4, 2, 4); });
        for (String a : new String[]{"rLeg", "lLeg", "rPants", "lPants"}) {
            s.row(a, "front", 6, 0x6EA8E8); tone(s, a, "front", 0, 7, 4, 2, 0.9); dots(s, a, "front", 0xDADDE6, 0, 5, 2, 5);
        }
    }

    static void touchMercy(Skin s) {
        var p = pal('s', 0xF2C7A5, 'S', 0xE0AE8A, 'y', 0xF2D06B, 'Y', 0xFFE79A, 'o', 0xD6B24C, 'b', 0x4A7FD6, 'w', 0xFFFFFF, 'p', 0xD98A8A, 'e', 0xC77C60);
        paint(s, "head", "front", new String[]{
            "YyyyyyyY",
            "yyYyyYyy",
            "ysSssSsy",
            "ywbsswby",
            "ysssssSy",
            "ysSppsSy",
            "ysspppsy",
            "oyssssyo"}, p);
        // 흰 슈트 + 금 갑옷 + 가슴 푸른 빛
        for (String f : new String[]{"front", "back"}) { tone(s, "body", f, 0, 0, 8, 12, 0.97); s.sub("body", f, 0, 6, 8, 1, 0xD6D6D6); }
        s.sub("body", "front", 2, 1, 4, 3, 0xE9C34E); s.sub("body", "front", 3, 2, 2, 1, 0x7FD6FF); s.at("body", "front", 3, 2, 0xE9FBFF);
        s.sub("body", "back", 1, 1, 6, 5, 0xE9C34E); s.sub("body", "back", 3, 2, 2, 3, 0xFFF0B0);
        dots(s, "body", "front", 0xFFF0B0, 2, 1, 5, 1);
        both(s, q -> { s.sub(q[1], "front", 0, 0, 4, 3, 0xE9C34E); s.row(q[1], "front", 0, 0xFFF0B0); });
        for (String a : new String[]{"rLeg", "lLeg", "rPants", "lPants"}) {
            s.sub(a, "front", 0, 3, 4, 2, 0xE9C34E); s.row(a, "front", 3, 0xFFF0B0);
            s.sub(a, "front", 0, 9, 4, 3, 0xD6B24C); s.row(a, "front", 9, 0xFFE79A);
        }
        // 포니테일(모자 층 뒤)
        s.sub("hat", "back", 2, 1, 4, 7, 0xF2D06B); s.sub("hat", "back", 3, 2, 2, 6, 0xFFE79A);
    }

    // ---- 3D 머리/어깨 렌더러(얼굴 샷) ----
    static double DY = 0;                 // 화면 세로 이동(프레임 기준 픽셀)

    static double[] rot(double x, double y, double z, double yaw, double pitch, double roll) {
        double cy = Math.cos(yaw), sy = Math.sin(yaw), cp = Math.cos(pitch), sp = Math.sin(pitch), cr = Math.cos(roll), sr = Math.sin(roll);
        double x1 = x * cy + z * sy, z1 = -x * sy + z * cy;                  // yaw (y 축)
        double y2 = y * cp - z1 * sp, z2 = y * sp + z1 * cp;                 // pitch (x 축)
        double x3 = x1 * cr - y2 * sr, y3 = x1 * sr + y2 * cr;               // roll (z 축)
        return new double[]{x3, y3, z2};
    }

    static void quad(BufferedImage skin, double[][] zb, int[] out, int size, double[][] c, int[] t,
                     double yaw, double pitch, double roll, double dist, double scale) {
        double[][] uv = {{0, 0}, {t[2], 0}, {t[2], t[3]}, {0, t[3]}};
        for (int[] tr : new int[][]{{0, 1, 2}, {0, 2, 3}}) {
            double[][] p = new double[3][];
            for (int k = 0; k < 3; k++) {
                double[] r = rot(c[tr[k]][0], c[tr[k]][1], c[tr[k]][2], yaw, pitch, roll);
                double depth = dist - r[2], persp = dist / depth;
                p[k] = new double[]{size / 2.0 + r[0] * scale * persp, size / 2.0 - r[1] * scale * persp + DY * size / 111.0, depth, uv[tr[k]][0], uv[tr[k]][1]};
            }
            raster(skin, zb, out, size, p, t);
        }
    }

    /** 머리 한 층(base 또는 hat)의 5 면(앞/뒤/좌/우/위). h = 반 크기(기본 4, 모자 층 4.45). */
    static void drawLayer(BufferedImage skin, double[][] zb, int[] out, int size, double h, boolean hat,
                          double yaw, double pitch, double roll, double dist, double scale) {
        int uo = hat ? 32 : 0;
        quad(skin, zb, out, size, new double[][]{{-h, h, h}, {h, h, h}, {h, -h, h}, {-h, -h, h}}, new int[]{uo + 8, 8, 8, 8}, yaw, pitch, roll, dist, scale);     // front
        quad(skin, zb, out, size, new double[][]{{h, h, -h}, {-h, h, -h}, {-h, -h, -h}, {h, -h, -h}}, new int[]{uo + 24, 8, 8, 8}, yaw, pitch, roll, dist, scale); // back
        quad(skin, zb, out, size, new double[][]{{h, h, h}, {h, h, -h}, {h, -h, -h}, {h, -h, h}}, new int[]{uo + 16, 8, 8, 8}, yaw, pitch, roll, dist, scale);     // left
        quad(skin, zb, out, size, new double[][]{{-h, h, -h}, {-h, h, h}, {-h, -h, h}, {-h, -h, -h}}, new int[]{uo, 8, 8, 8}, yaw, pitch, roll, dist, scale);       // right
        quad(skin, zb, out, size, new double[][]{{-h, h, -h}, {h, h, -h}, {h, h, h}, {-h, h, h}}, new int[]{uo + 8, 0, 8, 8}, yaw, pitch, roll, dist, scale);        // top
    }

    /** 머리 아래의 몸통/어깨(앞면만): 프로필에 목과 칼라가 보이게 한다. */
    static void drawBody(BufferedImage skin, double[][] zb, int[] out, int size, double yaw, double pitch, double roll, double dist, double scale) {
        double z = 2;
        double[][] quads = {{-4, 4, -4, -16, 20, 20, 8, 12}, {-8, -4, -4, -16, 44, 20, 4, 12}, {4, 8, -4, -16, 36, 52, 4, 12}};
        for (double[] q : quads)
            quad(skin, zb, out, size, new double[][]{{q[0], q[2], z}, {q[1], q[2], z}, {q[1], q[3], z}, {q[0], q[3], z}},
                    new int[]{(int) q[4], (int) q[5], (int) q[6], (int) q[7]}, yaw, pitch, roll, dist, scale);
    }

    static void raster(BufferedImage skin, double[][] zb, int[] out, int size, double[][] p, int[] t) {
        double minX = Math.min(p[0][0], Math.min(p[1][0], p[2][0])), maxX = Math.max(p[0][0], Math.max(p[1][0], p[2][0]));
        double minY = Math.min(p[0][1], Math.min(p[1][1], p[2][1])), maxY = Math.max(p[0][1], Math.max(p[1][1], p[2][1]));
        double den = (p[1][1] - p[2][1]) * (p[0][0] - p[2][0]) + (p[2][0] - p[1][0]) * (p[0][1] - p[2][1]);
        if (Math.abs(den) < 1e-9) return;
        for (int y = Math.max(0, (int) Math.floor(minY)); y <= Math.min(size - 1, (int) Math.ceil(maxY)); y++)
            for (int x = Math.max(0, (int) Math.floor(minX)); x <= Math.min(size - 1, (int) Math.ceil(maxX)); x++) {
                double px = x + 0.5, py = y + 0.5;
                double a = ((p[1][1] - p[2][1]) * (px - p[2][0]) + (p[2][0] - p[1][0]) * (py - p[2][1])) / den;
                double b = ((p[2][1] - p[0][1]) * (px - p[2][0]) + (p[0][0] - p[2][0]) * (py - p[2][1])) / den;
                double c = 1 - a - b;
                if (a < -1e-6 || b < -1e-6 || c < -1e-6) continue;
                double ia = a / p[0][2], ib = b / p[1][2], ic = c / p[2][2], is = ia + ib + ic;   // 원근 보정
                double depth = 1.0 / is;
                double u = (ia * p[0][3] + ib * p[1][3] + ic * p[2][3]) / is, v = (ia * p[0][4] + ib * p[1][4] + ic * p[2][4]) / is;
                int tx = t[0] + Math.min(t[2] - 1, Math.max(0, (int) Math.floor(u))), ty = t[1] + Math.min(t[3] - 1, Math.max(0, (int) Math.floor(v)));
                int argb = skin.getRGB(tx, ty);
                if ((argb >>> 24) < 128) continue;
                if (depth < zb[y][x]) { zb[y][x] = depth; out[y * size + x] = argb; }
            }
    }

    /** 머리/어깨를 렌더링한 111x111 이미지. 배경은 세로 그라데이션. */
    static BufferedImage bust(BufferedImage skin, int bgTop, int bgBottom, double yawDeg, double pitchDeg, double rollDeg, double scale) {
        int size = 111 * 3;                                                  // 3배로 그린 뒤 줄여서 계단을 부드럽게
        int[] out = new int[size * size];
        double[][] zb = new double[size][size];
        for (double[] row : zb) java.util.Arrays.fill(row, Double.MAX_VALUE);
        double yaw = Math.toRadians(yawDeg), pitch = Math.toRadians(pitchDeg), roll = Math.toRadians(rollDeg);
        drawBody(skin, zb, out, size, yaw, pitch, roll, 40, scale * 3);
        drawLayer(skin, zb, out, size, 4.0, false, yaw, pitch, roll, 40, scale * 3);
        drawLayer(skin, zb, out, size, 4.45, true, yaw, pitch, roll, 40, scale * 3);
        BufferedImage res = new BufferedImage(111, 111, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 111; y++) for (int x = 0; x < 111; x++) {
            long r = 0, g = 0, b = 0;
            for (int j = 0; j < 3; j++) for (int i = 0; i < 3; i++) {
                int yy = y * 3 + j, xx = x * 3 + i;
                int o = out[yy * size + xx];
                int p = (o >>> 24) == 0 ? 0xFF000000 | lerp(bgTop, bgBottom, yy / (double) size) : o;
                r += (p >> 16) & 255; g += (p >> 8) & 255; b += p & 255;
            }
            res.setRGB(x, y, 0xFF000000 | ((int) (r / 9) << 16) | ((int) (g / 9) << 8) | (int) (b / 9));
        }
        return res;
    }

    static int lerp(int a, int b, double t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    static void save(BufferedImage im, Path p) throws Exception { Files.createDirectories(p.getParent()); ImageIO.write(im, "png", p.toFile()); }

    public static void main(String[] a) throws Exception {
        switch (a[0]) {
            case "skins" -> {
                Path out = Path.of(a[1]);
                save(make("soldier76").img, out.resolve("soldier76.png")); save(make("widowmaker").img, out.resolve("widowmaker.png"));
                save(make("reinhardt").img, out.resolve("reinhardt.png")); save(make("roadhog").img, out.resolve("roadhog.png"));
                save(make("ana").img, out.resolve("ana.png")); save(make("mercy").img, out.resolve("mercy.png"));
                System.out.println("skins generated");
            }
            case "busts" -> {
                Path in = Path.of(a[1]), out = Path.of(a[2]);
                DY = Double.parseDouble(System.getProperty("dy", "-10"));
                // {영웅, 배경 상단, 배경 하단}. 각도/확대는 Seafle 트레이서 프로필과 비슷하게(거의 정면, 머리가 프레임을 채움).
                Object[][] heroes = {
                    {"tracer", 0x7A5A3A, 0x2E2218}, {"soldier76", 0x2A3A5A, 0x141C2E}, {"widowmaker", 0x4A2A6A, 0x1A1030},
                    {"reinhardt", 0x4A4F5C, 0x1C1F26}, {"roadhog", 0x4F5A2E, 0x1F2410}, {"ana", 0x1E4A6A, 0x0E1E30}, {"mercy", 0x6A5A2A, 0x30280E}};
                for (Object[] h : heroes) {
                    BufferedImage skin = ImageIO.read(in.resolve(h[0] + ".png").toFile());
                    save(bust(skin, (int) h[1], (int) h[2], -12, 6, -4, 8.8), out.resolve("roster_bust_" + h[0] + ".png"));
                }
                System.out.println("busts generated");
            }
            case "preview" -> {
                BufferedImage in = ImageIO.read(Path.of(a[1]).toFile());
                int k = a.length > 3 ? Integer.parseInt(a[3]) : 8;
                BufferedImage o = new BufferedImage(64 * k, 64 * k, BufferedImage.TYPE_INT_ARGB);
                for (int y = 0; y < 64 * k; y++) for (int x = 0; x < 64 * k; x++) {
                    int p = in.getRGB(x / k, y / k);
                    o.setRGB(x, y, (p >>> 24) == 0 ? 0xFF808890 : p);
                }
                save(o, Path.of(a[2]));
            }
            default -> throw new IllegalArgumentException(a[0]);
        }
    }
}
