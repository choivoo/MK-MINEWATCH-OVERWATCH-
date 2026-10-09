package com.minewatch.client.render;

import com.minewatch.entity.BotEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** 봇 렌더러: Seafle 봇 모델(geo) + 발광 마스크. */
public class BotRenderer extends GeoEntityRenderer<BotEntity> {
    public BotRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new Model());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    private static final class Model extends GeoModel<BotEntity> {
        private static String name(BotEntity b) { return "bot_" + b.botType().key; }
        @Override public Identifier getModelResource(BotEntity b) { return Identifier.of("minewatch", "geo/entity/" + name(b) + ".geo.json"); }
        @Override public Identifier getTextureResource(BotEntity b) { return Identifier.of("minewatch", "textures/entity/" + name(b) + ".png"); }
        @Override public Identifier getAnimationResource(BotEntity b) { return Identifier.of("minewatch", "animations/entity/" + name(b) + ".animation.json"); }
    }
}
