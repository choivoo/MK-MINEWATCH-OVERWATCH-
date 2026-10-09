package com.minewatch.hero;

import java.util.ArrayDeque;
import net.minecraft.util.math.Vec3d;

public class TracerState extends HeroState {
    public static final class Snapshot {
        public final Vec3d pos; public final float yaw, pitch, health; public final int ammo;
        public Snapshot(Vec3d pos, float yaw, float pitch, float health, int ammo) {
            this.pos = pos; this.yaw = yaw; this.pitch = pitch; this.health = health; this.ammo = ammo;
        }
    }
    public int ammo = Tracer.MAX_AMMO;
    public int reloadTicks = 0;
    public boolean rightHand = false;
    public double bulletAcc = 0;

    public int blinkCharges = Tracer.MAX_BLINK;
    public int blinkTimer = 0;

    public final ArrayDeque<Snapshot> history = new ArrayDeque<>();
    public boolean recalling = false;
    public int recallCooldown = 0;
    public Vec3d recallOrigin = Vec3d.ZERO;
    public float recallHealth;
    public int recallTicks = 0;

    public int meleeCooldown = 0;
}
