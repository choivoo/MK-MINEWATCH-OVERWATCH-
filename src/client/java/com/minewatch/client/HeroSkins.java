package com.minewatch.client;

import com.minewatch.hero.Hero;
import com.minewatch.hero.HeroRegistry;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

/** 영웅을 고른 플레이어에게 영웅 스킨(마인크래프트 캐릭터 모델)을 입히기 위한 클라이언트 상태. */
public final class HeroSkins {
    private static final Map<UUID, Integer> HERO_OF = new ConcurrentHashMap<>();
    /** 영웅 선택/로비 화면의 3D 미리보기: 0 이 아니면 내 캐릭터가 이 영웅의 스킨으로 보인다. */
    public static volatile int preview = 0;

    public static void set(UUID id, int heroId) { if (heroId == 0) HERO_OF.remove(id); else HERO_OF.put(id, heroId); }
    public static void clear() { HERO_OF.clear(); preview = 0; }

    /** 해당 플레이어가 입어야 할 영웅 번호. 없으면 0. */
    public static int heroFor(UUID id) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (preview != 0 && mc.player != null && mc.player.getUuid().equals(id)) return preview;
        return HERO_OF.getOrDefault(id, 0);
    }

    /** 영웅 번호의 스킨 텍스처. 없으면 null. */
    public static Identifier texture(int heroId) {
        Hero h = HeroRegistry.get(heroId);
        return h == null ? null : Identifier.of("minewatch", "textures/skin/" + h.id + ".png");
    }
    private HeroSkins() {}
}
