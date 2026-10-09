package com.minewatch.client;

import com.minewatch.ModSounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;

/** 화면(UI)에서 쓰는 효과음. sounds.json 이벤트 이름으로 재생한다. */
public final class UiSound {
    public static void play(String name) { play(name, 1f); }

    public static void play(String name, float pitch) {
        SoundEvent e = ModSounds.get(name);
        if (e != null) MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(e, pitch));
    }
    private UiSound() {}
}
