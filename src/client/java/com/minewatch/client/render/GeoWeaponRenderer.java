package com.minewatch.client.render;

import com.minewatch.GeoWeaponItem;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** GeckoLib 영웅 무기(1인칭 팔 + 무기) 렌더러. */
public class GeoWeaponRenderer extends GeoItemRenderer<GeoWeaponItem> {
    /** 현재 그리는 시점이 1인칭인지(팔 포함 모델) 여부. 3인칭/GUI/바닥은 무기만 그리는 _tp 모델을 쓴다. */
    static boolean firstPerson;

    public GeoWeaponRenderer() { super(new Model()); }

    @Override
    public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices, VertexConsumerProvider vcp, int light, int overlay) {
        firstPerson = mode.isFirstPerson();
        super.render(stack, mode, matrices, vcp, light, overlay);
    }

    private static String tpName(GeoWeaponItem i) {
        return i.geoName.equals("tracer_arms") ? "pulse_pistol" : i.geoName + "_tp";
    }

    private static final class Model extends GeoModel<GeoWeaponItem> {
        @Override public Identifier getModelResource(GeoWeaponItem i) { return Identifier.of("minewatch", "geo/item/" + (firstPerson ? i.geoName : tpName(i)) + ".geo.json"); }
        @Override public Identifier getTextureResource(GeoWeaponItem i) { return Identifier.of("minewatch", "textures/item/" + (!firstPerson && i.geoName.equals("tracer_arms") ? "pulse_pistol" : i.textureName) + ".png"); }
        @Override public Identifier getAnimationResource(GeoWeaponItem i) { return Identifier.of("minewatch", "animations/item/" + i.geoName + ".animation.json"); }
    }
}
