package com.minewatch.client.render;

import com.minewatch.GeoWeaponItem;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** GeckoLib 영웅 무기(1인칭 팔 + 무기) 렌더러. */
public class GeoWeaponRenderer extends GeoItemRenderer<GeoWeaponItem> {
    public GeoWeaponRenderer() { super(new Model()); }

    private static final class Model extends GeoModel<GeoWeaponItem> {
        @Override public Identifier getModelResource(GeoWeaponItem i) { return Identifier.of("minewatch", "geo/item/" + i.geoName + ".geo.json"); }
        @Override public Identifier getTextureResource(GeoWeaponItem i) { return Identifier.of("minewatch", "textures/item/" + i.textureName + ".png"); }
        @Override public Identifier getAnimationResource(GeoWeaponItem i) { return Identifier.of("minewatch", "animations/item/" + i.geoName + ".animation.json"); }
    }
}
