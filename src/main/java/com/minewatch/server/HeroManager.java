package com.minewatch.server;

import com.minewatch.hero.Hero;
import com.minewatch.hero.HeroState;
import com.minewatch.net.PoolsPayload;
import com.minewatch.net.StatePayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

public final class HeroManager {
    private static final class Entry { Hero hero; HeroState state; PoolsPayload lastPools; }
    private static final Map<UUID, Entry> ENTRIES = new HashMap<>();

    public static Hero heroOf(ServerPlayerEntity p) {
        Entry e = ENTRIES.get(p.getUuid());
        return e == null ? null : e.hero;
    }
    public static HeroState stateOf(ServerPlayerEntity p) {
        Entry e = ENTRIES.get(p.getUuid());
        return e == null ? null : e.state;
    }

    /** 영웅 기본 체력 + 퍽 보너스로 최대 체력을 맞춘다(늘어난 만큼은 즉시 채움). */
    public static void applyMaxHealth(ServerPlayerEntity p) {
        Entry e = ENTRIES.get(p.getUuid());
        if (e == null) return;
        double target = (e.hero.maxHealthOw() + e.state.bonusMaxHealthOw) / Hero.HP_SCALE;
        EntityAttributeInstance a = p.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (a == null) return;
        double gain = target - a.getBaseValue();
        a.setBaseValue(target);
        if (gain > 0) p.setHealth((float) Math.min(target, p.getHealth() + gain));
    }

    /** 같은 영웅으로 상태를 새로 만든다(매치 시작 시 퍽/쿨다운 초기화). */
    public static void reselect(ServerPlayerEntity p) {
        Entry e = ENTRIES.get(p.getUuid());
        if (e != null) select(p, e.hero);
    }

    private static void setMaxHealth(ServerPlayerEntity p, double mc) {
        EntityAttributeInstance a = p.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (a != null) a.setBaseValue(mc);
        p.setHealth(p.getMaxHealth());
    }

    public static void select(ServerPlayerEntity p, Hero hero) {
        Entry old = ENTRIES.remove(p.getUuid());
        if (old != null) old.hero.onDeselect(p, old.state);
        com.minewatch.hero.HeroKit.clearWeapons(p);
        if (hero == null) {
            setMaxHealth(p, 20);
            ServerPlayNetworking.send(p, StatePayload.NONE);
            ServerPlayNetworking.send(p, PoolsPayload.NONE);
            ServerPlayNetworking.send(p, com.minewatch.net.PerkStatePayload.NONE);
            return;
        }
        Entry e = new Entry();
        e.hero = hero; e.state = hero.createState();
        e.state.pools.init(hero.maxArmor(), hero.maxShield());
        ENTRIES.put(p.getUuid(), e);
        setMaxHealth(p, hero.maxHealthOw() / Hero.HP_SCALE);
        hero.onSelect(p, e.state);
        giveWeapon(p, hero);
    }

    /** 영웅 무기를 1번 슬롯에 지급한다. 원래 있던 아이템은 인벤토리로 옮기거나 떨어뜨린다. */
    private static void giveWeapon(ServerPlayerEntity p, Hero hero) {
        if (hero.weapon() == null) return;
        var inv = p.getInventory();
        var old = inv.getStack(0);
        inv.setStack(0, new net.minecraft.item.ItemStack(hero.weapon()));
        if (!old.isEmpty() && !(old.getItem() instanceof com.minewatch.HeroWeaponItem)) inv.offerOrDrop(old);
        inv.selectedSlot = 0;
    }

    /** 리스폰 후 새 플레이어 엔티티에 영웅 체력 설정을 다시 적용한다. */
    public static void onRespawn(ServerPlayerEntity p) {
        Entry e = ENTRIES.get(p.getUuid());
        if (e != null) setMaxHealth(p, (e.hero.maxHealthOw() + e.state.bonusMaxHealthOw) / Hero.HP_SCALE);
    }

    /** 접속 종료 시 마지막으로 쓰던 영웅을 기억해 두었다가 재접속하면 복원한다. */
    private static final Map<UUID, Hero> LAST = new HashMap<>();
    public static Hero last(UUID id) { return LAST.get(id); }

    public static void remove(ServerPlayerEntity p) {
        Entry e = ENTRIES.remove(p.getUuid());
        if (e != null) LAST.put(p.getUuid(), e.hero);
    }
    public static void clear() { ENTRIES.clear(); LAST.clear(); }

    public static void setInput(ServerPlayerEntity p, com.minewatch.net.InputPayload in) {
        Entry e = ENTRIES.get(p.getUuid());
        if (e != null) e.state.input = in.sanitized();
    }

    public static void tickAll(Iterable<ServerPlayerEntity> players) {
        for (ServerPlayerEntity p : players) {
            Entry e = ENTRIES.get(p.getUuid());
            if (e == null) continue;
            if (!p.isAlive() || p.isSpectator()) {
                e.hero.onDeath(p, e.state);
                e.state.pools.refill();      // 부활 시 방어구/보호막 가득
                e.state.ultReady = false;
                continue;
            }
            if (MatchManager.inputLocked(p)) e.state.input = com.minewatch.net.InputPayload.EMPTY;
            e.state.pools.tick();
            e.hero.tick(p, e.state);
            e.state.prevInput = e.state.input;
            notifyUlt(p, e);
            if (e.state.speedAmp >= 0) p.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SPEED, 3, e.state.speedAmp, false, false, false));
            if (e.state.perk.dirty || p.age % 20 == 0) PerkManager.sync(p, e.state, e.hero);
            ServerPlayNetworking.send(p, e.hero.toPayload(e.state));
            var pl = e.state.pools;
            PoolsPayload pp = new PoolsPayload((int) Math.ceil(pl.armor), (int) pl.maxArmor, (int) Math.ceil(pl.shield), (int) pl.maxShield);
            if (!pp.equals(e.lastPools)) { e.lastPools = pp; ServerPlayNetworking.send(p, pp); }
        }
    }

    /** 궁극기가 가득 차는 순간 한 번 알림. */
    private static void notifyUlt(ServerPlayerEntity p, Entry e) {
        boolean ready = e.state.ultPoints >= e.hero.ultCost();
        if (ready && !e.state.ultReady) {
            Sfx.play(p, "ult_ready", 1f, 1f);
            p.sendMessage(Text.literal("궁극기 준비 완료!"), true);
        }
        e.state.ultReady = ready;
    }
    private HeroManager() {}
}
