package com.minewatch.hero;

import java.util.LinkedHashMap;
import java.util.Map;

public final class HeroRegistry {
    private static final Map<String, Hero> BY_ID = new LinkedHashMap<>();
    private static final Map<Integer, Hero> BY_NUM = new LinkedHashMap<>();

    public static final Tracer TRACER = register(new Tracer());

    private static <T extends Hero> T register(T hero) {
        BY_ID.put(hero.id, hero);
        BY_NUM.put(hero.numericId, hero);
        return hero;
    }
    public static Hero get(String id) { return BY_ID.get(id); }
    public static Hero get(int numericId) { return BY_NUM.get(numericId); }
    public static Iterable<String> ids() { return BY_ID.keySet(); }
    public static java.util.Collection<Hero> all() { return BY_ID.values(); }
    public static void init() {}
    private HeroRegistry() {}
}
