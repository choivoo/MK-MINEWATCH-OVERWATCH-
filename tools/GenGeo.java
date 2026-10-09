import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * GeckoLib(Bedrock geo) 무기 모델 + 1인칭 팔 + 애니메이션 생성기. Blockbench 로 열어 볼 수 있는 표준 형식이다.
 * 사용: java tools/GenGeo.java src/main/resources/assets/minewatch
 *
 * 좌표(픽셀, 16 = 1 블록): 오른손 손잡이가 원점, 앞쪽이 -z, 위가 +y. 모든 색은 16x16 팔레트 텍스처의 칸으로 칠한다.
 */
public class GenGeo {
    // 팔레트(4x4 칸). 인덱스를 색 이름으로 쓴다.
    static final int DARK = 0, MID = 1, LIGHT = 2, WHITE = 3, ORANGE = 4, CYAN = 5, RED = 6, BLUE = 7,
            YELLOW = 8, GREEN = 9, PURPLE = 10, BROWN = 11, STEEL = 12, BLACK = 13, GOLD = 14, SKIN = 15;
    static final int[] PALETTE = {
        0x2B2F36, 0x59616B, 0x9AA3AD, 0xE8EEF5, 0xF99E1A, 0x00D4FF, 0xD93A3A, 0x3A7BD9,
        0xF2C94C, 0x4CD964, 0x8E5BD9, 0x6B4A2B, 0x7B8794, 0x111418, 0xD4AF37, 0xE8B48A
    };

    record Cube(double cx, double cy, double cz, double w, double h, double d, int color, double rx, double ry, double rz) {}

    static final class Bone {
        final String name, parent; final double[] pivot; final List<Cube> cubes = new ArrayList<>();
        Bone(String name, String parent, double px, double py, double pz) { this.name = name; this.parent = parent; this.pivot = new double[]{px, py, pz}; }
        Bone box(double cx, double cy, double cz, double w, double h, double d, int c) { cubes.add(new Cube(cx, cy, cz, w, h, d, c, 0, 0, 0)); return this; }
        Bone rot(double cx, double cy, double cz, double w, double h, double d, int c, double rx, double ry, double rz) { cubes.add(new Cube(cx, cy, cz, w, h, d, c, rx, ry, rz)); return this; }
    }

    static String n(double v) { return String.format(Locale.ROOT, "%.4f", v).replaceAll("0+$", "").replaceAll("\\.$", ""); }
    static String arr(double... v) { StringBuilder b = new StringBuilder("["); for (int i = 0; i < v.length; i++) { if (i > 0) b.append(", "); b.append(n(v[i])); } return b.append("]").toString(); }

    static String face(String name, int c) {
        int col = c % 4, row = c / 4;
        return "\"" + name + "\": {\"uv\": [" + (col * 4 + 1) + ", " + (row * 4 + 1) + "], \"uv_size\": [2, 2]}";
    }

    static String geo(String id, List<Bone> bones) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n \"format_version\": \"1.12.0\",\n \"minecraft:geometry\": [\n  {\n   \"description\": {\n    \"identifier\": \"geometry.").append(id)
          .append("\",\n    \"texture_width\": 16,\n    \"texture_height\": 16,\n    \"visible_bounds_width\": 8,\n    \"visible_bounds_height\": 8,\n    \"visible_bounds_offset\": [0, 0.75, 0]\n   },\n   \"bones\": [\n");
        for (int i = 0; i < bones.size(); i++) {
            Bone b = bones.get(i);
            sb.append("    {\"name\": \"").append(b.name).append("\"");
            if (b.parent != null) sb.append(", \"parent\": \"").append(b.parent).append("\"");
            sb.append(", \"pivot\": ").append(arr(b.pivot));
            if (!b.cubes.isEmpty()) {
                sb.append(", \"cubes\": [\n");
                for (int k = 0; k < b.cubes.size(); k++) {
                    Cube c = b.cubes.get(k);
                    sb.append("      {\"origin\": ").append(arr(c.cx - c.w / 2, c.cy - c.h / 2, c.cz - c.d / 2))
                      .append(", \"size\": ").append(arr(c.w, c.h, c.d));
                    if (c.rx != 0 || c.ry != 0 || c.rz != 0)
                        sb.append(", \"pivot\": ").append(arr(c.cx, c.cy, c.cz)).append(", \"rotation\": ").append(arr(c.rx, c.ry, c.rz));
                    sb.append(", \"uv\": {");
                    String[] faces = {"north", "east", "south", "west", "up", "down"};
                    for (int f = 0; f < faces.length; f++) { sb.append(face(faces[f], c.color)); if (f < faces.length - 1) sb.append(", "); }
                    sb.append("}}").append(k < b.cubes.size() - 1 ? ",\n" : "\n");
                }
                sb.append("    ]");
            }
            sb.append("}").append(i < bones.size() - 1 ? ",\n" : "\n");
        }
        return sb.append("   ]\n  }\n ]\n}\n").toString();
    }

    // ---- 애니메이션 ----
    static String kf(String[] times, double[][] vals) {
        StringBuilder b = new StringBuilder("{");
        for (int i = 0; i < times.length; i++) { if (i > 0) b.append(", "); b.append("\"").append(times[i]).append("\": ").append(arr(vals[i])); }
        return b.append("}").toString();
    }

    /** 한 동작 블록. channels: {뼈이름, 채널(position/rotation), times..., } 형태를 직접 문자열로 넘긴다. */
    static String animBlock(String name, double length, boolean loop, String bonesJson) {
        return "  \"animation." + name + "\": {\n   \"animation_length\": " + n(length) + (loop ? ",\n   \"loop\": true" : "") + ",\n   \"bones\": {" + bonesJson + "}\n  }";
    }

    static String bone(String bone, String posKf, String rotKf) {
        StringBuilder b = new StringBuilder("\n    \"" + bone + "\": {");
        boolean any = false;
        if (posKf != null) { b.append("\"position\": ").append(posKf); any = true; }
        if (rotKf != null) { if (any) b.append(", "); b.append("\"rotation\": ").append(rotKf); }
        return b.append("}").toString();
    }

    static String animations(String id, boolean hasMag, boolean hammer) {
        List<String> a = new ArrayList<>();
        a.add(animBlock(id + ".idle", 3, true,
                bone("root", kf(new String[]{"0", "1.5", "3"}, new double[][]{{0, 0, 0}, {0, -0.25, 0}, {0, 0, 0}}),
                        kf(new String[]{"0", "1.5", "3"}, new double[][]{{0, 0, 0}, {0.8, 0.3, 0}, {0, 0, 0}}))));
        a.add(animBlock(id + ".fire", 0.14, false,
                bone("gun", kf(new String[]{"0", "0.03", "0.14"}, new double[][]{{0, 0, 0}, {0, 0.2, 1.8}, {0, 0, 0}}),
                        kf(new String[]{"0", "0.03", "0.14"}, new double[][]{{0, 0, 0}, {-4, 0, 0}, {0, 0, 0}}))));
        String reloadBones = bone("root", kf(new String[]{"0", "0.25", "1.0", "1.35"}, new double[][]{{0, 0, 0}, {0, -2.5, 1}, {0, -2.5, 1}, {0, 0, 0}}),
                kf(new String[]{"0", "0.25", "1.0", "1.35"}, new double[][]{{0, 0, 0}, {28, 0, -8}, {28, 0, -8}, {0, 0, 0}}));
        if (hasMag) reloadBones += "," + bone("mag", kf(new String[]{"0", "0.3", "0.5", "0.55", "0.9"}, new double[][]{{0, 0, 0}, {0, -7, 0}, {0, -7, 0}, {0, -7, 0}, {0, 0, 0}}), null);
        a.add(animBlock(id + ".reload", 1.35, false, reloadBones));
        a.add(animBlock(id + ".swing", hammer ? 0.75 : 0.45, false,
                bone("root", kf(new String[]{"0", hammer ? "0.18" : "0.1", hammer ? "0.4" : "0.22", hammer ? "0.75" : "0.45"},
                        new double[][]{{0, 0, 0}, {0, 2, 3}, {0, -3, -6}, {0, 0, 0}}),
                        kf(new String[]{"0", hammer ? "0.18" : "0.1", hammer ? "0.4" : "0.22", hammer ? "0.75" : "0.45"},
                                new double[][]{{0, 0, 0}, {hammer ? -50 : -20, 0, 0}, {hammer ? 55 : 25, 0, 0}, {0, 0, 0}}))));
        return "{\n \"format_version\": \"1.8.0\",\n \"animations\": {\n" + String.join(",\n", a) + "\n }\n}\n";
    }

    // ---- 공통 팔(오른손/왼손) ----
    static void rightArm(Bone b, int sleeve) {
        b.box(0, -0.2, 0.2, 3.4, 3.2, 3.6, SKIN);                                  // 손
        b.rot(0.6, -3.8, 7.2, 3.8, 3.8, 11, sleeve, 28, 0, 0);                    // 소매(팔뚝) - 카메라 쪽으로 내려온다
    }

    static void leftArm(Bone b, double hx, double hy, double hz, int sleeve) {
        b.box(hx, hy, hz, 3.2, 3.0, 3.4, SKIN);
        b.rot(hx - 1.2, hy - 4.2, hz + 7.5, 3.6, 3.6, 11, sleeve, 28, -6, 0);
    }

    static List<Bone> base(int sleeve, boolean leftHand, double lx, double ly, double lz) {
        List<Bone> l = new ArrayList<>();
        l.add(new Bone("root", null, 0, 0, 0));
        Bone gun = new Bone("gun", "root", 0, 0, 0);
        l.add(gun);
        Bone arm = new Bone("arm_r", "gun", 0, 0, 0);
        rightArm(arm, sleeve);
        l.add(arm);
        if (leftHand) {
            Bone al = new Bone("arm_l", "gun", lx, ly, lz);
            leftArm(al, lx, ly, lz, sleeve);
            l.add(al);
        }
        return l;
    }

    static Bone get(List<Bone> l, String name) { for (Bone b : l) if (b.name.equals(name)) return b; throw new IllegalArgumentException(name); }

    static List<Bone> pulseRifle() {
        List<Bone> l = base(BLUE, true, -2.4, 0.6, -9);
        Bone g = get(l, "gun");
        g.box(0, 1.8, -5, 2.6, 3.4, 13, DARK).box(0, 3.9, -5, 1.4, 0.8, 9, STEEL).box(0, 2.2, -14.5, 1.4, 1.4, 7, MID)
         .box(0, 2.2, -18.6, 1.8, 1.8, 1.2, CYAN).box(0, -2.2, -0.5, 2.2, 5, 2.6, DARK).box(0, 0.8, 5, 2.2, 3.4, 5, MID)
         .box(0, 4.9, -4, 1.6, 1.2, 3, BLUE).box(0, 1.8, -11, 2.9, 0.5, 2, ORANGE);
        l.add(new Bone("mag", "gun", 0, -1.2, -6.5).box(0, -1.2, -6.5, 2, 4.4, 3, STEEL));
        return l;
    }

    static List<Bone> sniper() {
        List<Bone> l = base(PURPLE, true, -2.2, 0.8, -10);
        Bone g = get(l, "gun");
        g.box(0, 1.6, -6, 2.2, 3, 14, BLACK).box(0, 2, -19, 1.2, 1.2, 14, STEEL).box(0, 2, -26.4, 1.8, 1.8, 2.4, DARK)
         .box(0, 5, -6, 2.2, 2.2, 9, PURPLE).box(0, 5, -11.2, 2.8, 2.8, 1, DARK).box(0, 5, -1.4, 2.6, 2.6, 1.2, DARK)
         .box(0, -2.4, -0.5, 2.2, 5, 2.6, BLACK).box(0, 0.8, 6, 2, 3.4, 7, BLACK).box(0, 3.4, -6, 1, 1.4, 3, STEEL);
        l.add(new Bone("mag", "gun", 0, -1.2, -6.5).box(0, -1.4, -7, 1.8, 4, 2.4, STEEL));
        return l;
    }

    static List<Bone> hammer() {
        List<Bone> l = base(RED, true, 0, -0.4, -6);
        Bone g = get(l, "gun");
        g.box(0, 0, -8, 1.6, 1.6, 20, STEEL).box(0, 0, 3.4, 2.4, 2.4, 2.2, DARK).box(0, 0, -20, 7, 9, 12, MID)
         .box(0, 0, -26.4, 6, 8, 0.8, ORANGE).box(4.4, 0, -17, 1.4, 3.4, 6, RED).box(-4.4, 0, -17, 1.4, 3.4, 6, RED)
         .box(0, 4.8, -20, 4, 0.8, 8, LIGHT).box(0, 0, -13.2, 2.4, 2.4, 1.2, GOLD);
        return l;
    }

    static List<Bone> scrapGun() {
        List<Bone> l = base(BROWN, true, 0, -3.8, -9);
        Bone g = get(l, "gun");
        g.box(0, 1, -6, 4, 5, 14, BROWN).box(0, 1.4, -17, 3.4, 3.4, 10, STEEL).box(0, 1.4, -22.6, 4.2, 4.2, 1.8, DARK)
         .box(0, 5.2, -5, 3, 3, 5, RED).box(0, 3.7, -9, 4.2, 0.5, 3, YELLOW).box(0, -3, 0, 2.4, 5, 2.8, DARK)
         .box(0, -1.6, -9, 2.8, 2.8, 5, RED).box(0, 2.2, 3, 3, 3, 4, MID);
        return l;
    }

    static List<Bone> bioticRifle() {
        List<Bone> l = base(GREEN, true, -2.2, 0.6, -9);
        Bone g = get(l, "gun");
        g.box(0, 1.6, -5, 2.8, 3.4, 13, MID).box(0, 2, -14, 1.6, 1.6, 8, LIGHT).box(0, 5, -5, 2, 2, 6, BLUE)
         .box(0, -1, -6, 3.6, 3.6, 3, GREEN).box(0, 2, -11, 3, 3, 1, CYAN).box(0, 2, -18.4, 2, 2, 1, BLUE)
         .box(0, -2.4, -0.5, 2.2, 5, 2.6, DARK).box(0, 0.8, 5, 2.2, 3, 5, MID).box(0, 3.4, 0, 1.6, 1.6, 2, WHITE);
        return l;
    }

    static List<Bone> staff() {
        List<Bone> l = base(WHITE, false, 0, 0, 0);
        Bone g = get(l, "gun");
        g.box(0, 0.8, -8, 1.2, 1.2, 26, GOLD).box(0, 0.8, -22, 2.8, 2.8, 2.8, WHITE)
         .box(1.8, 2.6, -22, 0.7, 0.7, 4, YELLOW).box(-1.8, 2.6, -22, 0.7, 0.7, 4, YELLOW)
         .box(1.8, -1, -22, 0.7, 0.7, 4, YELLOW).box(-1.8, -1, -22, 0.7, 0.7, 4, YELLOW)
         .rot(5.2, 2.2, -19.5, 7, 0.5, 3.4, WHITE, 0, 0, 18).rot(-5.2, 2.2, -19.5, 7, 0.5, 3.4, WHITE, 0, 0, -18)
         .box(0, 0.8, -13, 2.2, 2.2, 1, ORANGE).box(0, 0.8, -3, 2.2, 2.2, 1, ORANGE);
        return l;
    }

    static void write(Path root, String id, List<Bone> bones, boolean hasMag, boolean hammer) throws Exception {
        Files.writeString(root.resolve("geo/item/" + id + ".geo.json"), geo(id, bones));
        Files.writeString(root.resolve("animations/item/" + id + ".animation.json"), animations(id, hasMag, hammer));
    }

    public static void main(String[] a) throws Exception {
        Path root = Path.of(a[0]);
        Files.createDirectories(root.resolve("geo/item"));
        Files.createDirectories(root.resolve("animations/item"));
        Files.createDirectories(root.resolve("textures/item"));
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < 16; i++)
            for (int y = 0; y < 4; y++) for (int x = 0; x < 4; x++) img.setRGB((i % 4) * 4 + x, (i / 4) * 4 + y, 0xFF000000 | PALETTE[i]);
        ImageIO.write(img, "png", root.resolve("textures/item/weapon_palette.png").toFile());
        write(root, "pulse_rifle", pulseRifle(), true, false);
        write(root, "sniper_rifle", sniper(), true, false);
        write(root, "rocket_hammer", hammer(), false, true);
        write(root, "scrap_gun", scrapGun(), false, false);
        write(root, "biotic_rifle", bioticRifle(), false, false);
        write(root, "caduceus_staff", staff(), false, false);
        System.out.println("generated 6 weapon geo + animation files");
    }
}
