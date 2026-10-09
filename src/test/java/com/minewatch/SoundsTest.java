package com.minewatch;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 코드에서 쓰는 사운드 이벤트 이름이 sounds.json 에 모두 있고, 각 이벤트에 재생할 소리가 있는지 검사한다. */
class SoundsTest {
    /** 소스에서 Sfx 로 재생하는 이벤트 이름들. 새로 쓰면 여기에도 추가한다. */
    private static final List<String> USED = List.of(
            "pulse_fire_crack", "reload_open", "blink", "recall_start", "recall_arrive", "health_pack_pickup",
            "ult_ready", "kill", "hit_crit", "hit", "bomb_throw", "bomb_explode", "bomb_stick", "melee_swing",
            "perk_avail_note", "perk_pick_enchant", "perk_open", "perk_close", "perk_pick_tick");

    private static JsonObject load() throws Exception {
        try (var in = SoundsTest.class.getResourceAsStream("/assets/minewatch/sounds.json")) {
            assertNotNull(in);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test void allUsedEventsExist() throws Exception {
        JsonObject o = load();
        for (String n : USED) assertTrue(o.has(n), "sounds.json 에 없는 이벤트: " + n);
    }

    @Test void everyEventHasAtLeastOneSound() throws Exception {
        JsonObject o = load();
        for (String k : o.keySet()) {
            JsonArray arr = o.getAsJsonObject(k).getAsJsonArray("sounds");
            assertTrue(arr != null && arr.size() > 0, k + ": 소리가 비어 있음");
        }
    }

    @Test void onlyVanillaSoundsAreReferenced() throws Exception {
        JsonObject o = load();
        for (String k : o.keySet())
            for (var e : o.getAsJsonObject(k).getAsJsonArray("sounds")) {
                String name = e.isJsonObject() ? e.getAsJsonObject().get("name").getAsString() : e.getAsString();
                assertTrue(name.startsWith("minecraft:"), k + ": 외부 오디오 참조 " + name);
            }
    }
}
