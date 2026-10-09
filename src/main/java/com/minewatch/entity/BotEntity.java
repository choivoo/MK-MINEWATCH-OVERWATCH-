package com.minewatch.entity;

import com.minewatch.hero.Hero;
import com.minewatch.server.BotManager;
import com.minewatch.server.BotStats;
import com.minewatch.server.BotStats.Difficulty;
import com.minewatch.server.BotStats.Type;
import com.minewatch.server.MapData;
import com.minewatch.server.MatchManager;
import com.minewatch.server.OwDamage;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** 매치용 AI 봇. 서버에서만 생각하고, 클라이언트는 GeckoLib 모델로 그린다. */
public class BotEntity extends PathAwareEntity implements GeoEntity {
    private static final TrackedData<Integer> TYPE = DataTracker.registerData(BotEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> TEAM = DataTracker.registerData(BotEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> DIFFICULTY = DataTracker.registerData(BotEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private LivingEntity target;
    private int cooldown, reaction, strafeTicks, strafeDir = 1, shotCount, stunTicks;

    /** 기절: 해당 틱 동안 AI 가 멈춘다. */
    public void stun(int ticks) { stunTicks = Math.max(stunTicks, ticks); }

    public BotEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 0;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.30)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(TYPE, 0).add(TEAM, -1).add(DIFFICULTY, 1);
    }

    /** 스폰 직후 호출: 종류/팀/난이도 설정, 체력 반영, 이름표. */
    public void setup(Type type, int team, Difficulty diff) {
        dataTracker.set(TYPE, type.ordinal());
        dataTracker.set(TEAM, team);
        dataTracker.set(DIFFICULTY, diff.ordinal());
        var hp = getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (hp != null) hp.setBaseValue(type.health / Hero.HP_SCALE);
        setHealth(getMaxHealth());
        setPersistent();
        boolean ally = type == Type.FRIENDLY;
        setCustomName(Text.literal(ally ? "아군 봇" : "적 봇").formatted(ally ? Formatting.AQUA : Formatting.RED));
        setCustomNameVisible(true);
    }

    public Type botType() { return Type.of(dataTracker.get(TYPE)); }
    public int botTeam() { return dataTracker.get(TEAM); }
    public Difficulty difficulty() { return Difficulty.of(dataTracker.get(DIFFICULTY)); }

    // ---- GeckoLib ----

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<BotEntity>(this, "idle", 0, state ->
                state.setAndContinue(RawAnimation.begin().thenLoop("animation.bot_" + botType().key + ".idle"))));
        AnimationController<BotEntity> fire = new AnimationController<BotEntity>(this, "fire", 0, state -> PlayState.STOP);
        for (Type t : Type.values()) {
            for (String side : new String[]{"l", "r"})
                fire.triggerableAnim("shoot_" + side + "_" + t.key,
                        RawAnimation.begin().then("animation.bot_" + t.key + ".shoot_" + side, Animation.LoopType.PLAY_ONCE));
        }
        controllers.add(fire);
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    // ---- AI (서버) ----

    @Override
    public void tick() {
        super.tick();
        if (getWorld() instanceof ServerWorld sw && isAlive()) aiTick(sw);
    }

    private boolean isEnemy(LivingEntity e) {
        if (e == this || !e.isAlive()) return false;
        if (e instanceof ServerPlayerEntity p && p.isSpectator()) return false;
        int t = MatchManager.teamOfEntity(e);
        return t >= 0 && t != botTeam();
    }

    private LivingEntity findTarget(ServerWorld w) {
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        List<LivingEntity> pool = new ArrayList<>(w.getPlayers());
        pool.addAll(BotManager.all());
        for (LivingEntity e : pool) {
            if (!isEnemy(e)) continue;
            double d = squaredDistanceTo(e);
            if (d < bestD) { bestD = d; best = e; }
        }
        return best;
    }

    private void aiTick(ServerWorld w) {
        if (MatchManager.state() != MatchManager.LIVE) { getNavigation().stop(); return; }
        if (stunTicks > 0) { stunTicks--; getNavigation().stop(); return; }
        Type type = botType();
        Difficulty diff = difficulty();

        if (target == null || !target.isAlive() || age % 20 == 0) {
            LivingEntity found = findTarget(w);
            if (found != target) reaction = diff.reactionTicks;
            target = found;
        }

        boolean control = MatchManager.mode() == MatchManager.MODE_CONTROL && MapData.get().hasPoint();
        if (target == null) {
            if (control && age % 20 == 0) goToPoint();
            return;
        }

        double dist = distanceTo(target);
        boolean los = canSee(target);
        getLookControl().lookAt(target.getX(), target.getEyeY(), target.getZ());

        if (control && (!los || dist > 22) && age % 10 == 0 && !atPoint()) { goToPoint(); }
        else if (!los || dist > type.range) { if (age % 8 == 0) getNavigation().startMovingTo(target, 1.0); }
        else {
            getNavigation().stop();
            if (--strafeTicks <= 0) { strafeDir = random.nextBoolean() ? 1 : -1; strafeTicks = 20 + random.nextInt(30); }
            getMoveControl().strafeTo(dist < type.range * 0.4 ? -0.5f : 0f, 0.6f * strafeDir);
        }

        if (los) { if (reaction > 0) reaction--; } else reaction = Math.max(reaction, diff.reactionTicks / 2);
        if (cooldown > 0) cooldown--;
        if (los && reaction <= 0 && cooldown <= 0 && dist <= type.range * 1.15) {
            fire(w, type, diff, dist);
            cooldown = BotStats.fireInterval(type, diff);
        }
    }

    private boolean atPoint() {
        double[] p = MapData.get().point;
        if (p == null) return false;
        double dx = getX() - p[0], dz = getZ() - p[2];
        double r = MapData.get().radius * 0.7;
        return dx * dx + dz * dz <= r * r;
    }

    private void goToPoint() {
        double[] p = MapData.get().point;
        if (p != null && !atPoint()) getNavigation().startMovingTo(p[0], p[1], p[2], 1.0);
    }

    private void fire(ServerWorld w, Type type, Difficulty diff, double dist) {
        boolean hit = random.nextDouble() < BotStats.hitChance(type, diff, dist);
        Vec3d from = getEyePos().add(getRotationVec(1f).multiply(0.8));
        Vec3d aim = target.getBoundingBox().getCenter();
        Vec3d impact = hit ? aim : aim.add((random.nextDouble() - 0.5) * 3.0, (random.nextDouble() - 0.5) * 1.5, (random.nextDouble() - 0.5) * 3.0);
        trace(w, from, impact);
        triggerAnim("fire", "shoot_" + (shotCount++ % 2 == 0 ? "l" : "r") + "_" + type.key);
        w.playSound(null, getBlockPos(), type.splash > 0 ? SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH : SoundEvents.BLOCK_NOTE_BLOCK_SNARE.value(),
                SoundCategory.HOSTILE, 0.6f, type.splash > 0 ? 0.8f : 1.6f);

        if (type.splash > 0) {
            w.spawnParticles(ParticleTypes.EXPLOSION, impact.x, impact.y, impact.z, 1, 0, 0, 0, 0);
            w.playSound(null, impact.x, impact.y, impact.z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.HOSTILE, 0.7f, 1.2f);
            Box box = new Box(impact, impact).expand(type.splash);
            for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, box, this::isEnemy)) {
                double d = e.getBoundingBox().getCenter().distanceTo(impact);
                double dmg = BotStats.splashDamage(type, d);
                if (dmg > 0) OwDamage.deal(this, e, dmg, false, true);
            }
        } else if (hit) {
            OwDamage.deal(this, target, type.damage, false, false);
        }
    }

    /** 총알 궤적을 팀 색 입자로 표시. */
    private void trace(ServerWorld w, Vec3d from, Vec3d to) {
        Vector3f col = botTeam() == 0 ? new Vector3f(0.3f, 0.7f, 1f) : new Vector3f(1f, 0.35f, 0.3f);
        DustParticleEffect fx = new DustParticleEffect(col, 0.8f);
        Vec3d d = to.subtract(from);
        int n = (int) Math.min(40, d.length() * 2);
        for (int i = 1; i <= n; i++) {
            Vec3d p = from.add(d.multiply(i / (double) n));
            w.spawnParticles(fx, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (!getWorld().isClient) BotManager.onBotDeath(this);
    }

    @Override public boolean canImmediatelyDespawn(double distanceSquared) { return false; }
}
