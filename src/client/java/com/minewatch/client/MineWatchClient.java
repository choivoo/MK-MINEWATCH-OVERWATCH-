package com.minewatch.client;

import com.minewatch.ModItems;
import com.minewatch.net.InputPayload;
import com.minewatch.net.StatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;

public class MineWatchClient implements ClientModInitializer {
    public static volatile StatePayload state = StatePayload.NONE;

    static final String CAT = "category.minewatch";
    static final KeyBinding ABILITY1 = key("key.minewatch.ability1", GLFW.GLFW_KEY_LEFT_SHIFT); // 블링크
    static final KeyBinding ABILITY2 = key("key.minewatch.ability2", GLFW.GLFW_KEY_E);          // 리콜
    static final KeyBinding ULT = key("key.minewatch.ult", GLFW.GLFW_KEY_Q);
    static final KeyBinding RELOAD = key("key.minewatch.reload", GLFW.GLFW_KEY_R);
    static final KeyBinding MELEE = key("key.minewatch.melee", GLFW.GLFW_KEY_V);

    private static KeyBinding key(String name, int code) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(name, InputUtil.Type.KEYSYM, code, CAT));
    }

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(StatePayload.ID, (payload, ctx) -> state = payload);
        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> state = StatePayload.NONE);

        // 영웅 활성 중에는 기본 조작(웅크리기/버리기)과 충돌하지 않도록 먼저 소비
        ClientTickEvents.START_CLIENT_TICK.register(mc -> {
            if (state.heroId() == 0 || mc.player == null) return;
            mc.options.sneakKey.setPressed(false);
            while (mc.options.dropKey.wasPressed()) { /* Q 는 궁극기 */ }
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null || state.heroId() == 0) return;
            boolean gui = mc.currentScreen != null;
            boolean holding = mc.player.getMainHandStack().isOf(ModItems.PULSE_PISTOLS);
            int b = 0;
            if (!gui) {
                if (holding && mc.options.attackKey.isPressed()) b |= InputPayload.FIRE;
                if (RELOAD.isPressed()) b |= InputPayload.RELOAD;
                if (ABILITY1.isPressed()) b |= InputPayload.ABILITY1;
                if (ABILITY2.isPressed()) b |= InputPayload.ABILITY2;
                if (ULT.isPressed()) b |= InputPayload.ULT;
                if (MELEE.isPressed()) b |= InputPayload.MELEE;
            }
            // 사격 반동(작은 화면 흔들림)
            if ((b & InputPayload.FIRE) != 0 && !state.reloading() && state.ammo() > 0) {
                var r = mc.player.getRandom();
                mc.player.setPitch(mc.player.getPitch() + (r.nextFloat() - 0.5f) * 0.5f);
                mc.player.setYaw(mc.player.getYaw() + (r.nextFloat() - 0.5f) * 0.4f);
            }
            float f = mc.player.input.movementForward, s = mc.player.input.movementSideways;
            ClientPlayNetworking.send(new InputPayload(b, f, s));
        });

        // 권총을 든 동안 바닐라 공격/채굴 차단
        AttackBlockCallback.EVENT.register((p, w, h, pos, dir) ->
                w.isClient && state.heroId() != 0 && p.getMainHandStack().isOf(ModItems.PULSE_PISTOLS) ? ActionResult.FAIL : ActionResult.PASS);
        AttackEntityCallback.EVENT.register((p, w, h, e, hit) ->
                state.heroId() != 0 && p.getMainHandStack().isOf(ModItems.PULSE_PISTOLS) ? ActionResult.FAIL : ActionResult.PASS);

        HudRenderCallback.EVENT.register(OverwatchHud::render);
    }
}
