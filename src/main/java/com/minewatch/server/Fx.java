package com.minewatch.server;

import com.minewatch.entity.FxEntity;
import com.minewatch.entity.ModEntities;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** 블록벤치 키프레임 연출(FxEntity)을 소환하는 서버 도우미. */
public final class Fx {
    private static final double BULLET_SPEED = 2.5;

    /** 위치 at 에서 dir 방향으로 len 길이의 연출을 소환한다(정지). */
    public static FxEntity spawn(ServerWorld w, String kind, Vec3d at, Vec3d dir, float len, int life) {
        FxEntity e = ModEntities.FX.create(w);
        if (e == null) return null;
        e.refreshPositionAndAngles(at.x, at.y, at.z, yaw(dir), pitch(dir));
        e.setup(kind, len, life);
        w.spawnEntity(e);
        return e;
    }

    /** 총구에서 목표까지 날아가는 총알 궤적. color: cyan, gold, orange, green, violet */
    public static void bullet(ServerWorld w, Vec3d from, Vec3d to, String color) {
        Vec3d d = to.subtract(from);
        double dist = d.length();
        if (dist < 0.3) return;
        Vec3d dir = d.multiply(1 / dist);
        int life = Math.max(1, (int) Math.ceil(dist / BULLET_SPEED));
        FxEntity e = spawn(w, "bullet_" + color, from, dir, (float) Math.min(2.5, dist), life);
        if (e != null) e.setVelocity(dir.multiply(BULLET_SPEED));
    }

    /** HeroKit.beam 의 색(0~1)을 가장 가까운 총알 색 이름으로 바꾼다. */
    public static String colorName(float r, float g, float b) {
        if (b > r && b > g) return b > 0.8f && r > 0.7f ? "violet" : "cyan";
        if (r > 0.8f && b > 0.7f) return "violet";
        if (g > r && g > b) return "green";
        return g > 0.65f ? "gold" : "orange";
    }

    public static float yaw(Vec3d d) { return (float) (MathHelper.atan2(-d.x, d.z) * 180 / Math.PI); }
    public static float pitch(Vec3d d) { return (float) (-Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 180 / Math.PI); }
    private Fx() {}
}
