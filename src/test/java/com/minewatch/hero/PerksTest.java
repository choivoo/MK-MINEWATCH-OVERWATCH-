package com.minewatch.hero;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PerksTest {
    private static final List<String> HEROES = List.of("tracer", "soldier76", "widowmaker", "reinhardt", "roadhog", "ana", "mercy");

    @Test void everyHeroHasTwoMinorAndTwoMajorPerks() {
        for (String h : HEROES) {
            assertEquals(2, Perks.offers(h, 0).size(), h + " 마이너");
            assertEquals(2, Perks.offers(h, 1).size(), h + " 메이저");
        }
    }

    @Test void perkIdsAreUniqueAndResolvable() {
        Set<String> ids = new HashSet<>();
        for (Perks.Perk p : Perks.all()) {
            assertTrue(ids.add(p.id()), "중복 id " + p.id());
            assertEquals(p, Perks.find(p.id()));
        }
        assertEquals(HEROES.size() * 4, ids.size());
    }

    @Test void everyPerkHasKoreanAndEnglishText() throws Exception {
        for (String lang : List.of("ko_kr", "en_us")) {
            try (var in = PerksTest.class.getResourceAsStream("/assets/minewatch/lang/" + lang + ".json")) {
                assertNotNull(in, lang);
                JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (Perks.Perk p : Perks.all()) {
                    assertTrue(o.has("perk.minewatch." + p.key()), lang + " 이름 누락: " + p.key());
                    assertTrue(o.has("perk.minewatch." + p.key() + ".desc"), lang + " 설명 누락: " + p.key());
                }
            }
        }
    }

    @Test void applyingPerksChangesState() {
        HeroState s = new HeroState();
        Perks.apply(s, Perks.find("ana:quick_reload"));
        assertEquals(1.3, s.reloadSpeed, 1e-9);
        Perks.apply(s, Perks.find("ana:potent_heal"));
        assertEquals(1.1, s.healMult, 1e-9);
        assertEquals(1.1, s.healOut(), 1e-9);
        Perks.apply(s, Perks.find("ana:ult_gain"));
        assertEquals(1.25, s.ultGainMult, 1e-9);
        Perks.apply(s, Perks.find("ana:cd_primary"));
        s.cd[0] = 130;
        for (int i = 0; i < 100; i++) s.tickEffects();           // 1.3배 빠르게 줄어 100틱에 130 소진
        assertEquals(0, s.cd[0]);
        Perks.apply(s, Perks.find("reinhardt:tough_armor"));
        assertEquals(50, s.pools.maxArmor, 1e-9);
        Perks.apply(s, Perks.find("roadhog:thick_skin"));
        assertEquals(100, s.bonusMaxHealthOw, 1e-9);
        Perks.apply(s, Perks.find("tracer:kinetic_reload"));
        assertTrue(s.killReload);
    }

    @Test void damagePerkRaisesOutgoingDamageButNotBuffs() {
        HeroState s = new HeroState();
        Perks.apply(s, Perks.find("widowmaker:damage_up"));
        assertEquals(1.08, s.outMult(), 1e-9);
        assertEquals(1.0, s.healOut(), 1e-9);
        s.boostOut(0.5, 10);
        assertEquals(1.5 * 1.08, s.outMult(), 1e-9);
    }

    @Test void ultGainScalesAllCharging() {
        Hero tracer = new Tracer();
        HeroState s = new HeroState();
        Perks.apply(s, Perks.find("ana:ult_gain"));
        s.addUlt(tracer, 100);
        assertEquals(125, s.ultPoints, 1e-9);
    }

    @Test void progressUnlocksMinorThenMajorInOrder() {
        PerkProgress p = new PerkProgress();
        assertEquals(-1, p.offerTier());
        p.addXp(PerkProgress.MINOR_XP - 1);
        assertEquals(-1, p.offerTier());
        p.addXp(1);
        assertEquals(0, p.offerTier());
        assertFalse(p.choose(1, "x:major"));                       // 단계가 다르면 거부
        assertTrue(p.choose(0, "tracer:blink_packs"));
        assertEquals(-1, p.offerTier());
        p.addXp(PerkProgress.MAJOR_XP);
        assertEquals(1, p.offerTier());
        assertTrue(p.choose(1, "tracer:temporal_regen"));
        assertEquals(-1, p.nextThreshold());
        assertFalse(p.choose(1, "tracer:quantum_entanglement"));   // 이미 선택함
    }

    @Test void majorUnlocksImmediatelyIfXpAlreadyHighWhenMinorChosen() {
        PerkProgress p = new PerkProgress();
        p.addXp(5000);
        assertEquals(0, p.offerTier());
        p.choose(0, "a");
        assertEquals(1, p.offerTier());
    }

    @Test void nonPositiveXpIgnored() {
        PerkProgress p = new PerkProgress();
        p.addXp(-50); p.addXp(0);
        assertEquals(0, p.xp, 0);
    }
}
