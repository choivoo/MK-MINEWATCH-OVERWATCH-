package com.minewatch.test;

import com.minewatch.hero.Hero;
import com.minewatch.hero.HeroRegistry;
import com.minewatch.hero.HeroState;
import com.minewatch.net.InputPayload;
import com.minewatch.server.HeroManager;
import com.minewatch.server.OwDamage;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

/** 영웅별 능력이 실제 서버에서 의도대로 동작하는지 검증한다. 틱은 직접 돌려 결정적으로 확인한다. */
public class HeroGameTests implements FabricGameTest {

    private static void check(boolean ok, String msg) { if (!ok) throw new RuntimeException(msg); }

    /** 평평한 바닥을 깔고 (원점 기준) 플레이어를 +z 방향으로 세운다. */
    private static ServerPlayerEntity player(TestContext ctx, Hero hero, double x, double z, float yaw) {
        for (int dx = 0; dx < 8; dx++) for (int dz = 0; dz < 8; dz++) ctx.setBlockState(dx, 0, dz, Blocks.STONE);
        ServerPlayerEntity p = ctx.createMockCreativeServerPlayerInWorld();
        p.changeGameMode(GameMode.SURVIVAL);
        check(p.interactionManager.getGameMode() == GameMode.SURVIVAL, "가짜 플레이어를 생존 모드로 바꾸지 못함: " + p.interactionManager.getGameMode());
        Vec3d at = ctx.getAbsolute(new Vec3d(x, 1, z));
        p.refreshPositionAndAngles(at.x, at.y, at.z, yaw, 0f);
        p.setHeadYaw(yaw);
        if (hero != null) HeroManager.select(p, hero);
        return p;
    }

    private static LivingEntity dummy(TestContext ctx, double x, double z) {
        var g = EntityType.IRON_GOLEM.create(ctx.getWorld());
        Vec3d at = ctx.getAbsolute(new Vec3d(x, 1, z));
        g.refreshPositionAndAngles(at.x, at.y, at.z, 0, 0);
        g.setAiDisabled(true);
        g.setNoGravity(true);
        ctx.getWorld().spawnEntity(g);
        return g;
    }

    private static void press(ServerPlayerEntity p, int flags) {
        HeroManager.stateOf(p).input = new InputPayload(flags, 0, 0);
    }

    private static void ticks(ServerPlayerEntity p, int n) {
        for (int i = 0; i < n; i++) HeroManager.tickAll(List.of(p));
    }

    private static void release(ServerPlayerEntity p) {
        press(p, 0);
        ticks(p, 1);
    }

    @GameTest(batchId = "h1", templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 100)
    public void soldierRifleSprintAndHelix(TestContext ctx) {
        ServerPlayerEntity p = player(ctx, HeroRegistry.SOLDIER76, 3.5, 1.5, 0f);
        LivingEntity d = dummy(ctx, 3.5, 6.5);
        check(p.getMainHandStack().getItem() == com.minewatch.ModItems.PULSE_RIFLE, "소총이 지급되지 않음");
        check(Math.abs(p.getMaxHealth() * Hero.HP_SCALE - 200) < 0.5, "최대 체력이 200 OW 가 아님: " + p.getMaxHealth() * Hero.HP_SCALE);

        float before = d.getHealth();
        press(p, InputPayload.FIRE);
        ticks(p, 10);
        HeroState s = HeroManager.stateOf(p);
        check(d.getHealth() < before, "소총이 대상에게 피해를 주지 못함");
        check(s.gun.ammo < 30, "탄약이 줄지 않음");
        check(s.ultPoints > 0, "피해를 줘도 궁극기가 충전되지 않음");
        release(p);

        float afterRifle = d.getHealth();
        press(p, InputPayload.ABILITY2);
        ticks(p, 1);
        check(d.getHealth() < afterRifle, "헬릭스 로켓이 피해를 주지 못함");
        check(s.cd[0] > 100, "헬릭스 쿨다운이 시작되지 않음");

        // 스프린트 토글(전진 입력 필요)
        release(p);
        HeroManager.stateOf(p).input = new InputPayload(InputPayload.ABILITY1, 1f, 0);
        ticks(p, 1);
        check(s.flag, "스프린트가 켜지지 않음");
        ctx.complete();
    }

    @GameTest(batchId = "h2", templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 100)
    public void widowmakerScopeChargeAndVenom(TestContext ctx) {
        ServerPlayerEntity p = player(ctx, HeroRegistry.WIDOWMAKER, 3.5, 1.5, 0f);
        LivingEntity d = dummy(ctx, 3.5, 7.5);
        HeroState s = HeroManager.stateOf(p);

        press(p, InputPayload.ALT_FIRE);
        ticks(p, 1);
        release(p);
        check(s.flag, "조준경이 켜지지 않음");
        ticks(p, 30);                                    // 완전 충전(27틱)
        float before = d.getHealth();
        press(p, InputPayload.FIRE);
        ticks(p, 1);
        float lost = (before - d.getHealth()) * (float) Hero.HP_SCALE;
        check(lost > 80, "완전 충전 저격 피해가 너무 낮음: " + lost);
        check(s.cd[2] > 0, "저격 재사용 대기가 시작되지 않음");

        // 독 지뢰 설치: 앞쪽 바닥을 겨냥
        release(p);
        p.setPitch(60f);
        press(p, InputPayload.ABILITY2);
        ticks(p, 1);
        check(com.minewatch.server.VenomMines.count() >= 1, "독 지뢰가 설치되지 않음");
        com.minewatch.server.VenomMines.clear();
        ctx.complete();
    }

    @GameTest(batchId = "h3", templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 100)
    public void reinhardtSwingBarrierAndFireStrike(TestContext ctx) {
        ServerPlayerEntity p = player(ctx, HeroRegistry.REINHARDT, 3.5, 1.5, 0f);
        check(Math.abs(p.getMaxHealth() * Hero.HP_SCALE - 325) < 0.5, "체력이 325 OW 가 아님");
        HeroState s = HeroManager.stateOf(p);
        check(s.pools.armor == 275, "방어구 275 가 아님: " + s.pools.armor);

        LivingEntity near = dummy(ctx, 3.5, 4.5);                  // 3 블록 앞
        float before = near.getHealth();
        press(p, InputPayload.FIRE);
        ticks(p, 1);
        check(near.getHealth() < before, "망치 휘두르기가 피해를 주지 못함");
        release(p);

        // 방벽: 앞에서 오는 공격은 방벽이 흡수하고 체력/방어구는 그대로
        press(p, InputPayload.ALT_FIRE);
        ticks(p, 1);
        check(s.barrierUp, "방벽이 올라가지 않음");
        ServerPlayerEntity attacker = player(ctx, null, 3.5, 6.5, 180f);
        float hpBefore = p.getHealth();
        double armorBefore = s.pools.armor, barrierBefore = s.barrierHp;
        OwDamage.deal(attacker, p, 100, false, false);
        check(p.getHealth() == hpBefore && s.pools.armor == armorBefore, "정면 공격이 방벽을 뚫음");
        check(s.barrierHp < barrierBefore, "방벽 체력이 줄지 않음");

        // 뒤에서 오는 공격은 방벽이 막지 못한다
        ServerPlayerEntity behind = player(ctx, null, 3.5, 0.5, 0f);
        double armorBefore2 = s.pools.armor;
        OwDamage.deal(behind, p, 100, false, false);
        check(s.pools.armor < armorBefore2 || p.getHealth() < hpBefore, "후방 공격이 방벽에 막힘");

        // 화염 강타는 관통 직선 피해
        release(p);
        LivingEntity far = dummy(ctx, 3.5, 7.0);
        float farBefore = far.getHealth();
        press(p, InputPayload.ABILITY2);
        ticks(p, 1);
        check(far.getHealth() < farBefore, "화염 강타가 피해를 주지 못함");
        ctx.complete();
    }

    @GameTest(batchId = "h4", templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 100)
    public void roadhogShotgunHookAndBreather(TestContext ctx) {
        ServerPlayerEntity p = player(ctx, HeroRegistry.ROADHOG, 3.5, 1.5, 0f);
        check(Math.abs(p.getMaxHealth() * Hero.HP_SCALE - 650) < 0.5, "체력이 650 OW 가 아님");
        HeroState s = HeroManager.stateOf(p);
        LivingEntity d = dummy(ctx, 3.5, 4.5);

        float before = d.getHealth();
        press(p, InputPayload.FIRE);
        ticks(p, 1);
        float lost = (before - d.getHealth()) * (float) Hero.HP_SCALE;
        check(lost > 20, "스크랩 건 산탄 피해가 너무 낮음: " + lost);
        check(s.gun.ammo == 4, "탄약이 1 줄지 않음: " + s.gun.ammo);
        release(p);

        // 사슬 갈고리: 먼 대상을 앞으로 끌어온다 (조준선을 가리는 가까운 더미는 치운다)
        d.discard();
        LivingEntity far = dummy(ctx, 3.5, 7.0);
        double zBefore = far.getZ();
        press(p, InputPayload.ABILITY2);
        ticks(p, 1);
        check(far.getZ() < zBefore - 1.0, "갈고리가 대상을 끌어오지 못함");
        release(p);

        // 숨 돌리기: 체력 회복 + 받는 피해 감소
        p.setHealth(p.getMaxHealth() * 0.3f);
        float low = p.getHealth();
        press(p, InputPayload.ABILITY1);
        ticks(p, 12);
        check(p.getHealth() > low + 3, "숨 돌리기가 치유하지 않음");
        check(s.inMult() < 1.0, "숨 돌리기 중 피해 감소가 없음");
        ctx.complete();
    }

    @GameTest(batchId = "h5", templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 100)
    public void anaHealsAlliesAndDamagesEnemies(TestContext ctx) {
        ServerPlayerEntity p = player(ctx, HeroRegistry.ANA, 3.5, 1.5, 0f);
        ServerPlayerEntity ally = player(ctx, null, 3.5, 5.5, 180f);
        ally.setHealth(5f);
        press(p, InputPayload.FIRE);
        ticks(p, 1);
        check(ally.getHealth() > 5f + 5f, "생체 소총이 아군을 치유하지 못함: " + ally.getHealth());
        release(p);

        HeroState s = HeroManager.stateOf(p);
        check(s.ultPoints > 0, "치유로 궁극기가 충전되지 않음");

        // 적(골렘)에게는 피해
        ally.refreshPositionAndAngles(ally.getX() + 3, ally.getY(), ally.getZ(), 180f, 0f);   // 시선에서 비켜 둔다
        LivingEntity d = dummy(ctx, 3.5, 6.5);
        s.gun.acc = 1;
        float before = d.getHealth();
        press(p, InputPayload.FIRE);
        ticks(p, 1);
        check(d.getHealth() < before, "생체 소총이 적에게 피해를 주지 못함");
        release(p);

        // 수면총: 적 기절
        press(p, InputPayload.ABILITY2);
        ticks(p, 1);
        check(((MobEntity) d).hasStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOWNESS), "수면총이 기절시키지 않음");

        // 나노 강화제: 아군에게 공격력/방어 버프
        s.ultPoints = 10_000;
        ally.refreshPositionAndAngles(ally.getX() - 3, ally.getY(), ally.getZ(), 180f, 0f);
        HeroManager.select(ally, HeroRegistry.SOLDIER76);
        release(p);
        press(p, InputPayload.ULT);
        ticks(p, 1);
        HeroState as = HeroManager.stateOf(ally);
        check(as.outMult() > 1.4 && as.inMult() < 0.6, "나노 강화제 효과가 적용되지 않음");
        ctx.complete();
    }

    @GameTest(batchId = "h6", templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 100)
    public void mercyBeamsAndValkyrie(TestContext ctx) {
        ServerPlayerEntity p = player(ctx, HeroRegistry.MERCY, 3.5, 1.5, 0f);
        ServerPlayerEntity ally = player(ctx, HeroRegistry.SOLDIER76, 3.5, 6.5, 180f);
        ally.setHealth(4f);
        float low = ally.getHealth();
        press(p, InputPayload.FIRE);
        ticks(p, 20);
        check(ally.getHealth() > low + 3, "치유 빔이 아군을 치유하지 못함: " + ally.getHealth());
        release(p);

        press(p, InputPayload.ALT_FIRE);
        ticks(p, 1);
        check(HeroManager.stateOf(ally).outMult() > 1.2, "공격력 증폭 빔이 적용되지 않음");
        release(p);

        HeroState s = HeroManager.stateOf(p);
        s.ultPoints = 10_000;
        press(p, InputPayload.ULT);
        ticks(p, 1);
        check(p.getAbilities().allowFlying, "발키리 중 비행이 허용되지 않음");
        s.timer = 1;
        release(p);
        ticks(p, 2);
        check(!p.getAbilities().allowFlying, "발키리 종료 후에도 비행이 남아 있음");
        ctx.complete();
    }

    @GameTest(batchId = "h7", templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 100)
    public void switchingHeroSwapsWeaponAndHealth(TestContext ctx) {
        ServerPlayerEntity p = player(ctx, HeroRegistry.ROADHOG, 3.5, 1.5, 0f);
        check(p.getMainHandStack().getItem() == com.minewatch.ModItems.SCRAP_GUN, "로드호그 무기가 아님");
        HeroManager.select(p, HeroRegistry.TRACER);
        check(p.getMainHandStack().getItem() == com.minewatch.ModItems.PULSE_PISTOLS, "트레이서 무기가 아님");
        long weapons = 0;
        for (int i = 0; i < p.getInventory().size(); i++)
            if (p.getInventory().getStack(i).getItem() instanceof com.minewatch.HeroWeaponItem) weapons++;
        check(weapons == 1, "영웅 무기가 중복 지급됨: " + weapons);
        check(Math.abs(p.getMaxHealth() * Hero.HP_SCALE - 150) < 0.5, "체력이 150 OW 로 돌아오지 않음");
        HeroManager.select(p, null);
        weapons = 0;
        for (int i = 0; i < p.getInventory().size(); i++)
            if (p.getInventory().getStack(i).getItem() instanceof com.minewatch.HeroWeaponItem) weapons++;
        check(weapons == 0, "영웅 해제 후에도 무기가 남음");
        check(p.getMaxHealth() == 20, "해제 후 체력이 바닐라로 돌아오지 않음");
        ctx.complete();
    }
}
