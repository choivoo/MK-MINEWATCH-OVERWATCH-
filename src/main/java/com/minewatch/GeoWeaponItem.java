package com.minewatch;

import java.util.function.Consumer;
import net.minecraft.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * GeckoLib 로 그리는 영웅 무기(1인칭 팔 + 무기가 한 모델). 모델/텍스처/애니메이션 이름은 geoName 에서 정해진다:
 * geo/item/<geoName>.geo.json, textures/item/<texture>.png, animations/item/<geoName>.animation.json
 * (애니메이션 이름은 "animation.<geoName>.<동작>").
 */
public class GeoWeaponItem extends HeroWeaponItem implements GeoItem {
    /** 재생할 수 있는 동작 이름. 모델에 없는 동작은 조용히 무시된다. */
    public static final String[] ACTIONS = {"fire", "fire_left", "fire_right", "reload", "blink", "recall", "melee", "bomb", "swing", "run"};

    public final String geoName, textureName, animName;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** 클라이언트 초기화에서 주입한다(렌더러 클래스는 클라이언트 전용이라 여기서는 직접 만들 수 없다). */
    public GeoRenderProvider renderProvider;

    public GeoWeaponItem(String geoName, String textureName) { this(geoName, textureName, geoName); }

    /** animName: 애니메이션 이름 접두("animation.<animName>.<동작>"). 모델 이름과 다를 때만 따로 지정한다. */
    public GeoWeaponItem(String geoName, String textureName, String animName) {
        this.animName = animName;
        this.geoName = geoName;
        this.textureName = textureName;
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        if (renderProvider != null) consumer.accept(renderProvider);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<GeoWeaponItem>(this, "main", 2, state ->
                state.setAndContinue(RawAnimation.begin().thenLoop("animation." + animName + ".idle"))));
        AnimationController<GeoWeaponItem> action = new AnimationController<GeoWeaponItem>(this, "action", 1,
                state -> software.bernie.geckolib.animation.PlayState.STOP);
        for (String a : ACTIONS)
            action.triggerableAnim(a, RawAnimation.begin().then("animation." + animName + "." + a, Animation.LoopType.PLAY_ONCE));
        controllers.add(action);
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    /** 클라이언트에서 해당 스택의 동작 애니메이션을 바로 재생한다(서버 왕복 없음). */
    public void playLocal(ItemStack stack, String action) {
        var manager = cache.getManagerForId(GeoItem.getId(stack));
        if (manager != null) manager.tryTriggerAnimation("action", action);
    }
}
