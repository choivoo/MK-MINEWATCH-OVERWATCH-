package com.minewatch.client.mixin;

import com.minewatch.client.MineWatchClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 조준경(저격/생체 소총)을 켠 동안 시야각을 크게 줄여 확대한다. */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void mw$scopeZoom(CallbackInfoReturnable<Double> cir) {
        if (MineWatchClient.state.heroId() != 0 && MineWatchClient.state.scoped()) cir.setReturnValue(cir.getReturnValue() * 0.25);
    }
}
