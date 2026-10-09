package com.minewatch.client.mixin;

import com.minewatch.client.HeroSkins;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 영웅을 고른 플레이어는 기본 스티브/알렉스 대신 영웅 스킨을 입는다(다른 플레이어 화면에서도). */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class PlayerSkinMixin {
    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void mw$heroSkin(CallbackInfoReturnable<SkinTextures> cir) {
        int hero = HeroSkins.heroFor(((AbstractClientPlayerEntity) (Object) this).getUuid());
        if (hero == 0) return;
        Identifier tex = HeroSkins.texture(hero);
        if (tex == null) return;
        SkinTextures o = cir.getReturnValue();
        cir.setReturnValue(new SkinTextures(tex, null, o.capeTexture(), o.elytraTexture(), SkinTextures.Model.WIDE, true));
    }
}
