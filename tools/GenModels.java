import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/** 팔레트 텍스처(16x16, 4x4 칸 16색)와 큐브 기반 무기 모델 JSON 을 만든다. 사용: java GenModels.java <resources/assets/minewatch 경로> */
public class GenModels {
    static final int[] PALETTE = {
        0x2B2F36, 0x59616B, 0x9AA3AD, 0xE8EEF5, 0xF99E1A, 0x00D4FF, 0xD93A3A, 0x3A7BD9,
        0xF2C94C, 0x4CD964, 0x8E5BD9, 0x6B4A2B, 0x7B8794, 0x111418, 0xD4AF37, 0xF2A0C0
    };
    static final int DARK = 0, MID = 1, LIGHT = 2, WHITE = 3, ORANGE = 4, CYAN = 5, RED = 6, BLUE = 7,
            YELLOW = 8, GREEN = 9, PURPLE = 10, BROWN = 11, STEEL = 12, BLACK = 13, GOLD = 14;

    record Cube(double x1, double y1, double z1, double x2, double y2, double z2, int c) {}

    static String face(String name, int c) {
        int col = c % 4, row = c / 4;
        return String.format("\"%s\": {\"uv\": [%d, %d, %d, %d], \"texture\": \"#p\"}", name, col * 4, row * 4, col * 4 + 4, row * 4 + 4);
    }

    static String model(String id, List<Cube> cubes) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"credit\": \"MineWatch cube model: ").append(id).append("\",\n");
        sb.append("  \"textures\": { \"p\": \"minewatch:item/weapon_palette\", \"particle\": \"minewatch:item/weapon_palette\" },\n");
        sb.append("  \"elements\": [\n");
        for (int i = 0; i < cubes.size(); i++) {
            Cube c = cubes.get(i);
            sb.append(String.format("    {\"from\": [%s, %s, %s], \"to\": [%s, %s, %s], \"faces\": {", n(c.x1), n(c.y1), n(c.z1), n(c.x2), n(c.y2), n(c.z2)));
            String[] faces = {"north", "east", "south", "west", "up", "down"};
            for (int f = 0; f < faces.length; f++) { sb.append(face(faces[f], c.c)); if (f < faces.length - 1) sb.append(", "); }
            sb.append("}}").append(i < cubes.size() - 1 ? ",\n" : "\n");
        }
        sb.append("  ],\n");
        sb.append("  \"display\": {\n");
        sb.append("    \"gui\": {\"rotation\": [30, 225, 0], \"translation\": [0, 0, 0], \"scale\": [0.9, 0.9, 0.9]},\n");
        sb.append("    \"ground\": {\"rotation\": [0, 0, 0], \"translation\": [0, 2, 0], \"scale\": [0.5, 0.5, 0.5]},\n");
        sb.append("    \"fixed\": {\"rotation\": [0, 90, 0], \"translation\": [0, 0, 0], \"scale\": [0.9, 0.9, 0.9]},\n");
        sb.append("    \"thirdperson_righthand\": {\"rotation\": [0, -90, 0], \"translation\": [0, 3, 1], \"scale\": [0.7, 0.7, 0.7]},\n");
        sb.append("    \"thirdperson_lefthand\": {\"rotation\": [0, -90, 0], \"translation\": [0, 3, 1], \"scale\": [0.7, 0.7, 0.7]},\n");
        sb.append("    \"firstperson_righthand\": {\"rotation\": [0, -90, 0], \"translation\": [1.5, 2.5, 1.5], \"scale\": [0.7, 0.7, 0.7]},\n");
        sb.append("    \"firstperson_lefthand\": {\"rotation\": [0, -90, 0], \"translation\": [1.5, 2.5, 1.5], \"scale\": [0.7, 0.7, 0.7]}\n");
        sb.append("  }\n}\n");
        return sb.toString();
    }

    static String n(double v) { return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v); }

    // 총구는 +x(오른쪽), 손잡이는 아래(-y), 두께는 z 중앙(7~9) 기준. 크기는 16 칸 안.
    static List<Cube> pistols() {
        List<Cube> l = new ArrayList<>();
        // 쌍권총: 두 정이 z 방향으로 나란히
        for (int side = 0; side < 2; side++) {
            double z = side == 0 ? 4 : 9;
            l.add(new Cube(2, 6, z, 11, 9, z + 3, MID));          // 슬라이드
            l.add(new Cube(11, 7, z + 0.5, 15, 8.5, z + 2.5, DARK)); // 총열
            l.add(new Cube(3, 2, z, 6, 6, z + 3, DARK));          // 손잡이
            l.add(new Cube(6.5, 3.5, z + 0.8, 8, 6, z + 2.2, DARK)); // 방아쇠울
            l.add(new Cube(6, 9, z + 0.5, 10, 10, z + 2.5, ORANGE)); // 상단 장식
            l.add(new Cube(14.5, 7.2, z + 0.7, 15.5, 8.3, z + 2.3, CYAN)); // 총구 발광
            l.add(new Cube(8, 6.2, z + 3, 10, 8.8, z + 3.3, CYAN));  // 측면 발광
        }
        return l;
    }

    static List<Cube> rifle() {
        List<Cube> l = new ArrayList<>();
        l.add(new Cube(1, 6, 7, 9, 10, 9, DARK));                  // 몸통
        l.add(new Cube(9, 7.5, 7.3, 15.5, 9, 8.7, MID));           // 총열
        l.add(new Cube(2, 3, 7.2, 4.5, 6, 8.8, DARK));             // 손잡이
        l.add(new Cube(5, 3.5, 7.3, 7, 6, 8.7, STEEL));            // 탄창
        l.add(new Cube(0, 6.5, 7.2, 1, 9.5, 8.8, BLACK));          // 개머리판
        l.add(new Cube(4, 10, 7.4, 8, 11.2, 8.6, BLUE));           // 조준경
        l.add(new Cube(14.5, 7.7, 7.5, 16, 8.8, 8.5, CYAN));       // 총구
        return l;
    }

    static List<Cube> sniper() {
        List<Cube> l = new ArrayList<>();
        l.add(new Cube(0, 6, 7, 8, 9.5, 9, BLACK));
        l.add(new Cube(8, 7.2, 7.4, 16, 8.6, 8.6, STEEL));         // 긴 총열
        l.add(new Cube(2.5, 3, 7.2, 5, 6, 8.8, BLACK));
        l.add(new Cube(3, 9.5, 7.2, 10, 11.5, 8.8, PURPLE));       // 큰 조준경
        l.add(new Cube(2.5, 9.5, 7, 3, 11.8, 9, DARK));
        l.add(new Cube(10, 9.5, 7, 10.5, 11.8, 9, DARK));
        l.add(new Cube(15, 7.2, 7.3, 16, 8.6, 8.7, PURPLE));
        l.add(new Cube(6, 4.5, 7.3, 7.5, 6, 8.7, STEEL));
        return l;
    }

    static List<Cube> hammer() {
        List<Cube> l = new ArrayList<>();
        l.add(new Cube(1, 7.3, 7.3, 11, 8.7, 8.7, STEEL));         // 자루
        l.add(new Cube(10, 4, 5, 15, 12, 11, MID));                // 망치 머리
        l.add(new Cube(10.5, 4.5, 5.5, 14.5, 11.5, 10.5, LIGHT));
        l.add(new Cube(14.5, 6, 6.5, 15.5, 10, 9.5, ORANGE));      // 타격면
        l.add(new Cube(0, 7, 7, 1.5, 9, 9, DARK));                 // 손잡이 끝
        l.add(new Cube(8, 6.8, 6.8, 10, 9.2, 9.2, GOLD));
        return l;
    }

    static List<Cube> scrapGun() {
        List<Cube> l = new ArrayList<>();
        l.add(new Cube(1, 5.5, 6.5, 8, 10.5, 9.5, BROWN));         // 몸통
        l.add(new Cube(8, 6.5, 6.8, 15, 9.5, 9.2, STEEL));         // 굵은 총열
        l.add(new Cube(14.5, 6.2, 6.5, 16, 9.8, 9.5, DARK));       // 총구
        l.add(new Cube(2, 2.5, 7, 4.5, 5.5, 9, DARK));
        l.add(new Cube(5, 10.5, 7, 9, 12, 9, RED));                // 호퍼
        l.add(new Cube(6, 3.5, 7, 7.5, 5.5, 9, RED));              // 펌프
        l.add(new Cube(9, 9.5, 7.2, 12, 10.5, 8.8, YELLOW));       // 경고 줄무늬
        return l;
    }

    static List<Cube> bioticRifle() {
        List<Cube> l = new ArrayList<>();
        l.add(new Cube(1, 6, 7, 9, 9.5, 9, MID));
        l.add(new Cube(9, 7.3, 7.3, 15, 8.7, 8.7, LIGHT));
        l.add(new Cube(2, 3, 7.2, 4.5, 6, 8.8, DARK));
        l.add(new Cube(4, 9.5, 7.3, 8, 11, 8.7, BLUE));            // 조준경
        l.add(new Cube(5, 5, 6.6, 8, 6, 9.4, GREEN));              // 치유 캡슐
        l.add(new Cube(10, 6.5, 6.6, 11.5, 7.3, 9.4, CYAN));       // 에너지 링
        l.add(new Cube(14.5, 7.1, 7.1, 16, 8.9, 8.9, BLUE));
        return l;
    }

    static List<Cube> staff() {
        List<Cube> l = new ArrayList<>();
        l.add(new Cube(0, 7.4, 7.4, 15, 8.6, 8.6, GOLD));          // 지팡이 몸통
        l.add(new Cube(13, 6, 6, 16, 10, 10, YELLOW));             // 머리 장식
        l.add(new Cube(14, 7, 7, 15, 9, 9, WHITE));                // 보석
        l.add(new Cube(3, 6.6, 6.6, 4.5, 9.4, 9.4, ORANGE));       // 손잡이 띠
        l.add(new Cube(8, 6.8, 6.8, 9, 9.2, 9.2, ORANGE));
        l.add(new Cube(12.5, 9.5, 7.5, 14, 12, 8.5, WHITE));       // 날개 장식
        l.add(new Cube(12.5, 4, 7.5, 14, 6.5, 8.5, WHITE));
        return l;
    }

    public static void main(String[] a) throws Exception {
        Path root = Path.of(a[0]);
        Path tex = root.resolve("textures/item/weapon_palette.png");
        Files.createDirectories(tex.getParent());
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < 16; i++)
            for (int y = 0; y < 4; y++) for (int x = 0; x < 4; x++)
                img.setRGB((i % 4) * 4 + x, (i / 4) * 4 + y, 0xFF000000 | PALETTE[i]);
        ImageIO.write(img, "png", tex.toFile());

        Path models = root.resolve("models/item");
        Files.createDirectories(models);
        Files.writeString(models.resolve("pulse_pistols.json"), model("pulse_pistols", pistols()));
        Files.writeString(models.resolve("pulse_rifle.json"), model("pulse_rifle", rifle()));
        Files.writeString(models.resolve("sniper_rifle.json"), model("sniper_rifle", sniper()));
        Files.writeString(models.resolve("rocket_hammer.json"), model("rocket_hammer", hammer()));
        Files.writeString(models.resolve("scrap_gun.json"), model("scrap_gun", scrapGun()));
        Files.writeString(models.resolve("biotic_rifle.json"), model("biotic_rifle", bioticRifle()));
        Files.writeString(models.resolve("caduceus_staff.json"), model("caduceus_staff", staff()));
        System.out.println("generated");
    }
}
