package com.minewatch.server;

import com.minewatch.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;

/** 서버에서 MineWatch 사운드 이벤트를 재생하는 도우미. 없는 이름은 조용히 무시한다. */
public final class Sfx {
    /** 월드의 해당 위치에서 모두에게 들리게 재생. */
    public static void at(ServerWorld w, double x, double y, double z, String name, float volume, float pitch) {
        SoundEvent e = ModSounds.get(name);
        if (e != null) w.playSound(null, x, y, z, e, SoundCategory.PLAYERS, volume, pitch);
    }

    public static void at(Entity src, String name, float volume, float pitch) {
        if (src.getWorld() instanceof ServerWorld w) at(w, src.getX(), src.getY(), src.getZ(), name, volume, pitch);
    }

    /** 해당 플레이어에게만 들리게 재생(UI/알림음). */
    public static void play(ServerPlayerEntity p, String name, float volume, float pitch) {
        SoundEvent e = ModSounds.get(name);
        if (e != null) p.playSoundToPlayer(e, SoundCategory.PLAYERS, volume, pitch);
    }
    private Sfx() {}
}
