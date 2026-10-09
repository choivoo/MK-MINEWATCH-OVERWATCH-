package com.minewatch.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 매치 전 위치(로비) 기록의 저장 형식. 서버가 매치 도중 꺼져도 다음 접속 때 로비로 돌려보낼 수 있도록
 * 월드 폴더의 minewatch_origins.json 에 보관한다. (순수 로직: 마인크래프트 클래스에 의존하지 않음)
 */
public final class OriginStore {
    public record Entry(String world, double x, double y, double z, float yaw, float pitch) {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type TYPE = new TypeToken<LinkedHashMap<String, Entry>>() {}.getType();

    public static String toJson(Map<String, Entry> map) { return GSON.toJson(map, TYPE); }

    /** 손상되었거나 비어 있으면 빈 맵. */
    public static Map<String, Entry> fromJson(String json) {
        try {
            Map<String, Entry> m = GSON.fromJson(json, TYPE);
            return m == null ? new LinkedHashMap<>() : m;
        } catch (RuntimeException e) {
            return new LinkedHashMap<>();
        }
    }

    public static void save(Path file, Map<String, Entry> map) {
        try {
            if (map.isEmpty()) { Files.deleteIfExists(file); return; }
            Files.writeString(file, toJson(map));
        } catch (IOException e) {
            System.err.println("[MineWatch] 위치 기록을 저장하지 못했습니다: " + e);
        }
    }

    public static Map<String, Entry> load(Path file) {
        try {
            return Files.exists(file) ? fromJson(Files.readString(file)) : new LinkedHashMap<>();
        } catch (IOException e) {
            return new LinkedHashMap<>();
        }
    }
    private OriginStore() {}
}
