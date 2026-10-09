package com.minewatch.client.mixin;

import com.minewatch.client.MineWatchClient;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 영웅 활성 중에는 바닐라 체력/배고픔/경험치/핫바 HUD 를 숨기고 오버워치 HUD 로 대체한다. */
@Mixin(InGameHud.class)
public class InGameHudMixin {
    private static boolean mw$hide() { return MineWatchClient.state.heroId() != 0; }

    @Inject(method = "renderStatusBars", at = @At("HEAD"), cancellable = true)
    private void mw$statusBars(CallbackInfo ci) { if (mw$hide()) ci.cancel(); }

    @Inject(method = "renderMountHealth", at = @At("HEAD"), cancellable = true)
    private void mw$mountHealth(CallbackInfo ci) { if (mw$hide()) ci.cancel(); }

    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void mw$xpBar(CallbackInfo ci) { if (mw$hide()) ci.cancel(); }

    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
    private void mw$xpLevel(CallbackInfo ci) { if (mw$hide()) ci.cancel(); }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void mw$hotbar(CallbackInfo ci) { if (mw$hide()) ci.cancel(); }
}
