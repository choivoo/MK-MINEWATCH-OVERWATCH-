import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * 연출(FxEntity)용 Bedrock geo 모델 + 키프레임 애니메이션 + 텍스처 생성기. Blockbench 로 그대로 열린다.
 * 사용: java tools/GenFx.java src/main/resources/assets/minewatch
 * 좌표: 픽셀(16 = 1블록). 길이가 있는 모델은 -Z 방향으로 16px(=1블록)이고, 게임에서 서버가 정한 길이로 늘어난다.
 * 텍스처 8x8 = 4칸(4x4): 0 흰 코어, 1 주색, 2 반투명 글로우, 3 어두운 강조.
 */
public class GenFx {
    record Cube(double x, double y, double z, double w, double h, double d, int cell, double rx, double ry, double rz) {}

    static final class Bone {
        final String name; final double[] pivot; final List<Cube> cubes = new ArrayList<>();
        Bone(String name, double px, double py, double pz) { this.name = name; pivot = new double[]{px, py, pz}; }
        Bone box(double cx, double cy, double cz, double w, double h, double d, int cell) { return rot(cx, cy, cz, w, h, d, cell, 0, 0, 0); }
        Bone rot(double cx, double cy, double cz, double w, double h, double d, int cell, double rx, double ry, double rz) {
            cubes.add(new Cube(cx - w / 2, cy - h / 2, cz - d / 2, w, h, d, cell, rx, ry, rz)); return this;
        }
    }

    static String n(double v) { return String.format(Locale.ROOT, "%.4f", v).replaceAll("0+$", "").replaceAll("\\.$", ""); }
    static String arr(double... v) { StringBuilder b = new StringBuilder("["); for (int i = 0; i < v.length; i++) { if (i > 0) b.append(", "); b.append(n(v[i])); } return b.append("]").toString(); }
    static String face(String f, int c) { return "\"" + f + "\": {\"uv\": [" + ((c % 2) * 4 + 1) + ", " + ((c / 2) * 4 + 1) + "], \"uv_size\": [2, 2]}"; }

    static String geo(String id, List<Bone> bones) {
        StringBuilder s = new StringBuilder();
        s.append("{\n  \"format_version\": \"1.12.0\",\n  \"minecraft:geometry\": [{\n    \"description\": {\"identifier\": \"geometry.").append(id)
         .append("\", \"texture_width\": 8, \"texture_height\": 8, \"visible_bounds_width\": 8, \"visible_bounds_height\": 8, \"visible_bounds_offset\": [0, 0, 0]},\n    \"bones\": [\n");
        for (int i = 0; i < bones.size(); i++) {
            Bone b = bones.get(i);
            s.append("      {\"name\": \"").append(b.name).append("\", \"pivot\": ").append(arr(b.pivot)).append(", \"cubes\": [\n");
            for (int j = 0; j < b.cubes.size(); j++) {
                Cube c = b.cubes.get(j);
                s.append("        {\"origin\": ").append(arr(c.x, c.y, c.z)).append(", \"size\": ").append(arr(c.w, c.h, c.d));
                if (c.rx != 0 || c.ry != 0 || c.rz != 0) s.append(", \"pivot\": ").append(arr(c.x + c.w / 2, c.y + c.h / 2, c.z + c.d / 2)).append(", \"rotation\": ").append(arr(c.rx, c.ry, c.rz));
                s.append(", \"uv\": {");
                String[] fs = {"north", "south", "east", "west", "up", "down"};
                for (int k = 0; k < 6; k++) { if (k > 0) s.append(", "); s.append(face(fs[k], c.cell)); }
                s.append("}}").append(j + 1 < b.cubes.size() ? "," : "").append("\n");
            }
            s.append("      ]}").append(i + 1 < bones.size() ? "," : "").append("\n");
        }
        return s.append("    ]\n  }]\n}\n").toString();
    }

    /** 애니메이션 한 개: bone -> 채널(rotation/scale/position) -> 시간 -> 값 */
    static final class Anim {
        final String id; final double length; final boolean loop;
        final Map<String, Map<String, List<String>>> bones = new LinkedHashMap<>();
        Anim(String id, double length, boolean loop) { this.id = id; this.length = length; this.loop = loop; }
        Anim key(String bone, String channel, double t, double x, double y, double z) {
            bones.computeIfAbsent(bone, k -> new LinkedHashMap<>()).computeIfAbsent(channel, k -> new ArrayList<>())
                 .add("\"" + n(t) + "\": " + arr(x, y, z));
            return this;
        }
        String json() {
            StringBuilder s = new StringBuilder();
            s.append("{\n  \"format_version\": \"1.8.0\",\n  \"animations\": {\n    \"animation.").append(id).append(".play\": {\n      \"loop\": ")
             .append(loop ? "true" : "\"hold_on_last_frame\"").append(",\n      \"animation_length\": ").append(n(length)).append(",\n      \"bones\": {\n");
            int bi = 0;
            for (var be : bones.entrySet()) {
                s.append("        \"").append(be.getKey()).append("\": {");
                int ci = 0;
                for (var ce : be.getValue().entrySet()) {
                    s.append("\"").append(ce.getKey()).append("\": {").append(String.join(", ", ce.getValue())).append("}");
                    if (++ci < be.getValue().size()) s.append(", ");
                }
                s.append("}").append(++bi < bones.size() ? "," : "").append("\n");
            }
            return s.append("      }\n    }\n  }\n}\n").toString();
        }
    }

    static void tex(Path dir, String name, int main, int dark) throws Exception {
        BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        int[] cols = {0xFFFFFFFF, 0xFF000000 | main, 0x58000000 | main, 0xFF000000 | dark};
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) img.setRGB(x, y, cols[(y / 4) * 2 + x / 4]);
        ImageIO.write(img, "png", dir.resolve(name + ".png").toFile());
    }

    public static void main(String[] a) throws Exception {
        Path root = Path.of(a[0]);
        Path g = root.resolve("geo/fx"), an = root.resolve("animations/fx"), t = root.resolve("textures/fx");
        Files.createDirectories(g); Files.createDirectories(an); Files.createDirectories(t);

        // ---- 총알 궤적: 길고 가는 빛줄기, 꼬리가 가늘어진다 ----
        List<Bone> bullet = List.of(
            new Bone("glow", 0, 0, -8).box(0, 0, -8, 3.5, 3.5, 16, 2),
            new Bone("core", 0, 0, -8).box(0, 0, -8, 1.2, 1.2, 16, 0),
            new Bone("tip", 0, 0, -16).box(0, 0, -15.5, 2.4, 2.4, 2, 1));
        Anim ab = new Anim("bullet", 0.6, false)
            .key("glow", "scale", 0, 1, 1, 1).key("glow", "scale", 0.3, 0.7, 0.7, 1).key("glow", "scale", 0.6, 0.3, 0.3, 1)
            .key("core", "scale", 0, 1, 1, 1).key("core", "scale", 0.6, 0.5, 0.5, 1);
        write(g, an, "bullet", geo("fx_bullet", bullet), ab);
        tex(t, "bullet_cyan", 0x3FD8FF, 0x0A4A66);
        tex(t, "bullet_gold", 0xFFC93A, 0x7A4A00);
        tex(t, "bullet_orange", 0xFF7A1A, 0x6A2200);
        tex(t, "bullet_green", 0x5CFF7A, 0x0B5A22);
        tex(t, "bullet_violet", 0xB45CFF, 0x3A1466);

        // ---- 총구 섬광: 교차한 판이 번쩍 ----
        List<Bone> muzzle = List.of(new Bone("star", 0, 0, 0)
            .box(0, 0, 0, 9, 1, 1, 1).box(0, 0, 0, 1, 9, 1, 1).box(0, 0, 0, 5, 5, 5, 2).box(0, 0, 0, 2.5, 2.5, 2.5, 0)
            .rot(0, 0, 0, 7, 1, 1, 1, 0, 0, 45).rot(0, 0, 0, 7, 1, 1, 1, 0, 0, -45));
        Anim am = new Anim("muzzle", 0.15, false)
            .key("star", "scale", 0, 0.3, 0.3, 0.3).key("star", "scale", 0.05, 1.3, 1.3, 1.3).key("star", "scale", 0.15, 0, 0, 0)
            .key("star", "rotation", 0, 0, 0, 0).key("star", "rotation", 0.15, 0, 0, 90);
        write(g, an, "muzzle", geo("fx_muzzle", muzzle), am);
        tex(t, "muzzle", 0xFFD84A, 0x7A4A00);

        // ---- 피격 불꽃 ----
        List<Bone> impact = List.of(new Bone("spark", 0, 0, 0)
            .box(0, 0, 0, 6, 0.8, 0.8, 1).box(0, 0, 0, 0.8, 6, 0.8, 1).box(0, 0, 0, 0.8, 0.8, 6, 1).box(0, 0, 0, 2, 2, 2, 0));
        Anim ai = new Anim("impact", 0.2, false)
            .key("spark", "scale", 0, 0.4, 0.4, 0.4).key("spark", "scale", 0.06, 1.2, 1.2, 1.2).key("spark", "scale", 0.2, 0, 0, 0)
            .key("spark", "rotation", 0, 0, 0, 0).key("spark", "rotation", 0.2, 45, 90, 0);
        write(g, an, "impact", geo("fx_impact", impact), ai);
        tex(t, "impact", 0xFFE9A0, 0x7A4A00);

        // ---- 블링크 궤적: 회전하는 푸른 띠가 가늘어지며 사라짐 ----
        Bone trail = new Bone("trail", 0, 0, -8);
        trail.box(0, 0, -8, 2.5, 2.5, 16, 0).box(0, 0, -8, 9, 9, 16, 2);
        Bone strips = new Bone("strips", 0, 0, -8);
        for (int i = 0; i < 4; i++) {
            double ang = Math.toRadians(i * 90 + 45);
            strips.rot(Math.cos(ang) * 5, Math.sin(ang) * 5, -8, 1.6, 3.5, 16, 1, 0, 0, i * 90 + 45);
        }
        Anim abt = new Anim("blink_trail", 0.45, false)
            .key("trail", "scale", 0, 1, 1, 1).key("trail", "scale", 0.15, 0.8, 0.8, 1).key("trail", "scale", 0.45, 0, 0, 1)
            .key("strips", "rotation", 0, 0, 0, 0).key("strips", "rotation", 0.45, 0, 0, 220)
            .key("strips", "scale", 0, 1.3, 1.3, 1).key("strips", "scale", 0.45, 0.1, 0.1, 1);
        write(g, an, "blink_trail", geo("fx_blink_trail", List.of(trail, strips)), abt);
        tex(t, "blink_trail", 0x45E0FF, 0x0A4A66);

        // ---- 블링크 잔상: 사람 크기의 기둥이 위로 늘어나며 사라짐 ----
        List<Bone> bf = List.of(
            new Bone("ghost", 0, 0, 0).box(0, 0, 0, 9, 30, 9, 2).box(0, 0, 0, 3.5, 28, 3.5, 0),
            new Bone("ring", 0, -15, 0).box(0, -15, 0, 18, 0.8, 18, 1));
        Anim abf = new Anim("blink_flash", 0.4, false)
            .key("ghost", "scale", 0, 1, 1, 1).key("ghost", "scale", 0.12, 0.8, 1.15, 0.8).key("ghost", "scale", 0.4, 0, 1.5, 0)
            .key("ring", "scale", 0, 0.3, 1, 0.3).key("ring", "scale", 0.4, 1.6, 1, 1.6)
            .key("ghost", "rotation", 0, 0, 0, 0).key("ghost", "rotation", 0.4, 0, 120, 0);
        write(g, an, "blink_flash", geo("fx_blink_flash", bf), abf);
        tex(t, "blink_flash", 0x45E0FF, 0x0A4A66);

        // ---- 리콜 고리: 세 겹의 고리가 서로 반대로 돌며 위로 모이는 시간 소용돌이 ----
        List<Bone> rc = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Bone r = new Bone("ring" + i, 0, 4 + i * 11, 0);
            double y = 4 + i * 11, rad = 13 - i * 2;
            int c = i == 1 ? 0 : 1;
            r.box(0, y, rad, rad * 2, 1.2, 1.2, c).box(0, y, -rad, rad * 2, 1.2, 1.2, c)
             .box(rad, y, 0, 1.2, 1.2, rad * 2, c).box(-rad, y, 0, 1.2, 1.2, rad * 2, c)
             .rot(0, y, 0, rad * 1.6, 1.2, 1.2, 2, 0, 45, 0).rot(0, y, 0, rad * 1.6, 1.2, 1.2, 2, 0, -45, 0);
            rc.add(r);
        }
        rc.add(new Bone("pillar", 0, 0, 0).box(0, 18, 0, 3, 36, 3, 2).box(0, 18, 0, 1.2, 36, 1.2, 0));
        Anim arc = new Anim("recall_ring", 1.2, false);
        for (int i = 0; i < 3; i++) {
            String b = "ring" + i;
            arc.key(b, "rotation", 0, 0, 0, 0).key(b, "rotation", 1.2, 0, (i % 2 == 0 ? 1 : -1) * 540, 0)
               .key(b, "scale", 0, 0.2, 1, 0.2).key(b, "scale", 0.3, 1.2, 1, 1.2).key(b, "scale", 1.2, 0.1, 1, 0.1)
               .key(b, "position", 0, 0, -6, 0).key(b, "position", 1.2, 0, 14 - i * 3, 0);
        }
        arc.key("pillar", "scale", 0, 0.1, 0.1, 0.1).key("pillar", "scale", 0.3, 1.4, 1, 1.4).key("pillar", "scale", 1.2, 0, 1.4, 0);
        write(g, an, "recall_ring", geo("fx_recall_ring", rc), arc);
        tex(t, "recall_ring", 0x45E0FF, 0x0A4A66);

        // ---- 펄스 폭탄(날아가는/붙은 상태): 파란 핵 + 도는 고리, 반복 ----
        Bone core = new Bone("core", 0, 0, 0);
        core.box(0, 0, 0, 5, 5, 5, 0).box(0, 0, 0, 7, 7, 7, 2).rot(0, 0, 0, 7, 7, 7, 2, 0, 45, 0);
        Bone orbit = new Bone("orbit", 0, 0, 0);
        orbit.box(0, 0, 5, 10, 0.8, 0.8, 1).box(0, 0, -5, 10, 0.8, 0.8, 1).box(5, 0, 0, 0.8, 0.8, 10, 1).box(-5, 0, 0, 0.8, 0.8, 10, 1);
        Anim abm = new Anim("bomb", 1.0, true)
            .key("orbit", "rotation", 0, 0, 0, 0).key("orbit", "rotation", 1, 360, 360, 0)
            .key("core", "scale", 0, 1, 1, 1).key("core", "scale", 0.25, 1.25, 1.25, 1.25).key("core", "scale", 0.5, 1, 1, 1)
            .key("core", "scale", 0.75, 1.25, 1.25, 1.25).key("core", "scale", 1, 1, 1, 1);
        write(g, an, "bomb", geo("fx_bomb", List.of(core, orbit)), abm);
        tex(t, "bomb", 0x3AA0FF, 0x0A2A66);

        // ---- 폭탄 폭발: 겹겹의 구가 부풀며 사라지고, 땅 고리가 퍼진다 ----
        List<Bone> bb = List.of(
            new Bone("shell", 0, 0, 0).box(0, 0, 0, 8, 8, 8, 2).rot(0, 0, 0, 8, 8, 8, 2, 45, 45, 0),
            new Bone("inner", 0, 0, 0).box(0, 0, 0, 5, 5, 5, 0).rot(0, 0, 0, 5, 5, 5, 0, 0, 45, 45),
            new Bone("wave", 0, 0, 0).box(0, 0, 0, 8, 0.8, 8, 1).rot(0, 0, 0, 8, 0.8, 8, 1, 0, 45, 0));
        Anim abb = new Anim("bomb_blast", 0.7, false)
            .key("shell", "scale", 0, 1, 1, 1).key("shell", "scale", 0.18, 12, 12, 12).key("shell", "scale", 0.7, 16, 16, 16)
            .key("shell", "rotation", 0, 0, 0, 0).key("shell", "rotation", 0.7, 0, 180, 0)
            .key("inner", "scale", 0, 1, 1, 1).key("inner", "scale", 0.15, 9, 9, 9).key("inner", "scale", 0.4, 0, 0, 0)
            .key("wave", "scale", 0, 1, 1, 1).key("wave", "scale", 0.5, 16, 1, 16).key("wave", "scale", 0.7, 0, 1, 0);
        write(g, an, "bomb_blast", geo("fx_bomb_blast", bb), abb);
        tex(t, "bomb_blast", 0x3AA0FF, 0x0A2A66);
        System.out.println("generated fx geo/animation/texture");
    }

    static void write(Path g, Path an, String id, String geoJson, Anim anim) throws Exception {
        Files.writeString(g.resolve(id + ".geo.json"), geoJson);
        Files.writeString(an.resolve(id + ".animation.json"), anim.json());
    }
}
