package com.minewatch;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minewatch.hero.Perks;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 코드가 참조하는 에셋 파일이 모두 존재하고 형식이 올바른지 검사한다(게임을 켜지 않고도 누락을 잡는다). */
class AssetsTest {
    private static final String ASSETS = "/assets/minewatch/";
    private static final List<String> HEROES = List.of("tracer", "soldier76", "widowmaker", "reinhardt", "roadhog", "ana", "mercy");
    /** 영웅 슬롯 아이콘 이름(StatePayload.Slot icon). Seafle 스프라이트가 있는 blink/recall 은 제외. */
    private static final List<String> ABILITY_ICONS = List.of("sprint", "helix", "grapple", "venom", "charge", "firestrike",
            "breather", "hook", "grenade", "sleep", "guardian");
    /** 무기 geo 이름 -> 애니메이션 접두. */
    private static final List<String[]> WEAPONS = List.of(
            new String[]{"tracer_arms", "tracer"}, new String[]{"pulse_rifle", "pulse_rifle"}, new String[]{"sniper_rifle", "sniper_rifle"},
            new String[]{"rocket_hammer", "rocket_hammer"}, new String[]{"scrap_gun", "scrap_gun"},
            new String[]{"biotic_rifle", "biotic_rifle"}, new String[]{"caduceus_staff", "caduceus_staff"});

    private static boolean exists(String path) { return AssetsTest.class.getResource(ASSETS + path) != null; }

    private static String read(String path) throws Exception {
        try (InputStream in = AssetsTest.class.getResourceAsStream(ASSETS + path)) {
            assertNotNull(in, "없음: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test void everyHeroHasBustPortrait() {
        for (String h : HEROES) assertTrue(exists("textures/gui/pick/roster_bust_" + h + ".png"), "초상화 없음: " + h);
    }

    @Test void everyAbilityHasIcon() {
        for (String i : ABILITY_ICONS) assertTrue(exists("textures/gui/ability/" + i + ".png"), "능력 아이콘 없음: " + i);
    }

    @Test void everyPerkHasIcon() {
        for (Perks.Perk p : Perks.all()) assertTrue(exists("textures/gui/perk/" + p.key() + "_64.png"), "퍽 아이콘 없음: " + p.key());
    }

    @Test void everyWeaponHasModelItemJsonGeoAndAnimations() throws Exception {
        for (String[] w : WEAPONS) {
            String geo = w[0], anim = w[1];
            JsonObject g = JsonParser.parseString(read("geo/item/" + geo + ".geo.json")).getAsJsonObject();
            assertTrue(g.has("minecraft:geometry"), geo + ": geo 형식 오류");
            JsonObject a = JsonParser.parseString(read("animations/item/" + geo + ".animation.json")).getAsJsonObject().getAsJsonObject("animations");
            for (String need : List.of("idle", "reload"))
                assertTrue(a.has("animation." + anim + "." + need), geo + ": 애니메이션 누락 " + need);
        }
        for (String id : List.of("pulse_pistols", "pulse_rifle", "sniper_rifle", "rocket_hammer", "scrap_gun", "biotic_rifle", "caduceus_staff")) {
            JsonObject m = JsonParser.parseString(read("models/item/" + id + ".json")).getAsJsonObject();
            assertTrue(m.get("parent").getAsString().equals("minecraft:builtin/entity"), id + ": GeckoLib 아이템은 builtin/entity 여야 함");
        }
    }

    @Test void weaponTexturesExist() {
        assertTrue(exists("textures/item/tracer_arms.png"));
        assertTrue(exists("textures/item/weapon_palette.png"));
    }
}
