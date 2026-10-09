package com.minewatch.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 키프레임 애니메이션으로 움직이는 연출 전용 엔티티(총알 궤적, 블링크 잔상, 리콜 고리, 펄스 폭탄 등).
 * 모델/애니메이션은 geo/fx/&lt;모델&gt;.geo.json, animations/fx/&lt;모델&gt;.animation.json 에 있다.
 * 서버가 종류(kind)·길이·수명을 정해 소환하고, 수명이 다하면 스스로 사라진다.
 */
public class FxEntity extends Entity implements GeoEntity {
    private static final TrackedData<String> KIND = DataTracker.registerData(FxEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Float> LEN = DataTracker.registerData(FxEntity.class, TrackedDataHandlerRegistry.FLOAT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int life = 10;

    public FxEntity(EntityType<? extends FxEntity> type, World world) {
        super(type, world);
        noClip = true;
        setNoGravity(true);
    }

    public void setup(String kind, float len, int life) {
        dataTracker.set(KIND, kind);
        dataTracker.set(LEN, len);
        this.life = life;
    }

    public String kind() { return dataTracker.get(KIND); }
    public float len() { return dataTracker.get(LEN); }
    /** 모델 이름: bullet_gold 처럼 색 접미사가 붙은 종류는 같은 모델을 쓴다. */
    public String model() { String k = kind(); return k.startsWith("bullet_") ? "bullet" : k; }

    @Override protected void initDataTracker(DataTracker.Builder b) {
        b.add(KIND, "impact");
        b.add(LEN, 1f);
    }

    @Override public void tick() {
        super.tick();
        setPosition(getPos().add(getVelocity()));
        if (!getWorld().isClient && age >= life) discard();
    }

    @Override public boolean shouldRender(double distance) { return distance < 96 * 96; }
    @Override public boolean isAttackable() { return false; }
    @Override protected void readCustomDataFromNbt(NbtCompound nbt) {}
    @Override protected void writeCustomDataToNbt(NbtCompound nbt) {}
    @Override public boolean shouldSave() { return false; }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "fx", 0, st -> st.setAndContinue(RawAnimation.begin().thenPlay("animation." + model() + ".play"))));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
