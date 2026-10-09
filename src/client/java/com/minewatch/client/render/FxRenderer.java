package com.minewatch.client.render;

import com.minewatch.entity.FxEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** 연출 엔티티 렌더러: 항상 최대 밝기·반투명으로 그리고, 엔티티의 방향/길이를 모델에 반영한다. */
public class FxRenderer extends GeoEntityRenderer<FxEntity> {
    private static final int FULL_BRIGHT = 0xF000F0;

    public FxRenderer(EntityRendererFactory.Context ctx) { super(ctx, new Model()); }

    @Override
    public RenderLayer getRenderType(FxEntity e, Identifier tex, VertexConsumerProvider vcp, float partial) {
        return RenderLayer.getEntityTranslucentEmissive(tex);
    }

    @Override
    public void render(FxEntity e, float yaw, float partial, MatrixStack m, VertexConsumerProvider vcp, int light) {
        super.render(e, yaw, partial, m, vcp, FULL_BRIGHT);
    }

    @Override
    protected void applyRotations(FxEntity e, MatrixStack m, float age, float rotationYaw, float partial, float scale) {
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - e.getYaw(partial)));
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-e.getPitch(partial)));
        // 모델은 길이 1블록(-Z 방향)으로 만들어 두고 서버가 정한 길이로 늘인다
        m.scale(1f, 1f, e.len());
    }

    private static final class Model extends GeoModel<FxEntity> {
        @Override public Identifier getModelResource(FxEntity e) { return Identifier.of("minewatch", "geo/fx/" + e.model() + ".geo.json"); }
        @Override public Identifier getTextureResource(FxEntity e) { return Identifier.of("minewatch", "textures/fx/" + e.kind() + ".png"); }
        @Override public Identifier getAnimationResource(FxEntity e) { return Identifier.of("minewatch", "animations/fx/" + e.model() + ".animation.json"); }
    }
}
