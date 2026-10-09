package com.minewatch;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * 사운드 이벤트 등록. assets/minewatch/sounds.json 의 모든 키를 이벤트로 등록한다.
 * 기본 소리는 바닐라 소리를 피치/볼륨만 바꿔 쓰므로 별도 오디오 파일(저작권)이 없다.
 * 리소스팩이 같은 이름의 sounds.json 항목을 덮어쓰면 원하는 오디오로 바꿀 수 있다(docs/SOUNDS.md).
 */
public final class ModSounds {
    private static final Map<String, SoundEvent> EVENTS = new LinkedHashMap<>();

    public static void init() {
        try (InputStream in = ModSounds.class.getResourceAsStream("/assets/minewatch/sounds.json")) {
            if (in == null) { System.err.println("[MineWatch] sounds.json 을 찾지 못했습니다."); return; }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (String key : root.keySet()) {
                Identifier id = Identifier.of("minewatch", key);
                EVENTS.put(key, Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id)));
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("[MineWatch] 사운드 등록 실패: " + e);
        }
    }

    /** 이벤트 이름으로 조회. 없으면 null. */
    public static SoundEvent get(String name) { return EVENTS.get(name); }
    public static int count() { return EVENTS.size(); }
    public static java.util.Set<String> names() { return EVENTS.keySet(); }
    private ModSounds() {}
}
