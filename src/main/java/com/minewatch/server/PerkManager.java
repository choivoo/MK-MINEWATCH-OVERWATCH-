package com.minewatch.server;

import com.minewatch.hero.Hero;
import com.minewatch.hero.HeroState;
import com.minewatch.hero.PerkProgress;
import com.minewatch.hero.Perks;
import com.minewatch.net.PerkStatePayload;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;

/** 퍽 경험치 지급, 선택 검증/적용, 클라이언트 동기화. */
public final class PerkManager {
    public static final int KILL_XP = 100;

    public static void addXp(ServerPlayerEntity p, double amount) {
        HeroState s = HeroManager.stateOf(p);
        if (s == null || !MatchManager.active()) return;          // 퍽은 매치 중에만 쌓인다
        int before = s.perk.stage;
        s.addXp(amount);
        if (s.perk.stage != before && s.perk.offerTier() >= 0) {
            p.sendMessage(Text.translatable("hud.minewatch.perk_available"), true);
            Sfx.play(p, "perk_avail_note", 1f, 1f);
        }
    }

    /** 적 처치: 경험치 보너스 + 처치 퍽. */
    public static void onKill(ServerPlayerEntity killer) {
        HeroState s = HeroManager.stateOf(killer);
        Hero h = HeroManager.heroOf(killer);
        if (s == null || h == null) return;
        addXp(killer, KILL_XP);
        if (s.killReload) h.reloadNow(killer, s);
    }

    /** 클라이언트가 보낸 선택을 검증하고 적용한다. */
    public static void choose(ServerPlayerEntity p, String perkId) {
        HeroState s = HeroManager.stateOf(p);
        Hero h = HeroManager.heroOf(p);
        if (s == null || h == null) return;
        int tier = s.perk.offerTier();
        Perks.Perk perk = Perks.find(perkId);
        if (tier < 0 || perk == null || !perk.hero().equals(h.id) || perk.tier() != tier) return;   // 제안되지 않은 퍽
        if (!s.perk.choose(tier, perk.id())) return;
        Perks.apply(s, perk);
        HeroManager.applyMaxHealth(p);
        Sfx.play(p, "perk_pick_enchant", 1f, 1f);
        p.sendMessage(Text.translatable("hud.minewatch.perk_chosen", Text.translatable("perk.minewatch." + perk.key())), true);
        sync(p, s, h);
    }

    public static PerkStatePayload payload(HeroState s, Hero h) {
        PerkProgress pr = s.perk;
        String a = "", b = "";
        int tier = pr.offerTier();
        if (tier >= 0) {
            List<Perks.Perk> offers = Perks.offers(h.id, tier);
            if (offers.size() >= 2) { a = offers.get(0).id(); b = offers.get(1).id(); }
        }
        return new PerkStatePayload((int) pr.xp, pr.nextThreshold(), pr.stage, a, b, pr.minor, pr.major);
    }

    public static void sync(ServerPlayerEntity p, HeroState s, Hero h) {
        ServerPlayNetworking.send(p, payload(s, h));
        s.perk.dirty = false;
    }
    private PerkManager() {}
}
