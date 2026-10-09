package com.minewatch.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;

/** 월드에 저장되는 매치 맵 정보(팀 스폰, 점령지). world 폴더의 minewatch_map.json. */
public final class MapData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public String dim = "minecraft:overworld";
    /** 스폰: {x, y, z, yaw}. */
    public List<double[]> spawnA = new ArrayList<>();
    public List<double[]> spawnB = new ArrayList<>();
    /** 점령지 {x, y, z} (없으면 null) 와 반경. */
    public double[] point;
    public double radius = 5;

    private static MapData current = new MapData();
    public static MapData get() { return current; }

    public List<double[]> spawns(int team) { return team == 0 ? spawnA : spawnB; }
    public boolean hasSpawns() { return !spawnA.isEmpty() && !spawnB.isEmpty(); }
    public boolean hasPoint() { return point != null; }

    public ServerWorld world(MinecraftServer server) {
        return server.getWorld(RegistryKey.of(RegistryKeys.WORLD, Identifier.of(dim)));
    }

    private static Path file(MinecraftServer server) { return server.getSavePath(WorldSavePath.ROOT).resolve("minewatch_map.json"); }

    public static void load(MinecraftServer server) {
        Path f = file(server);
        current = new MapData();
        if (!Files.exists(f)) return;
        try {
            MapData d = GSON.fromJson(Files.readString(f), MapData.class);
            if (d != null) current = d;
        } catch (IOException | RuntimeException e) {
            System.err.println("[MineWatch] 맵 데이터를 읽지 못했습니다: " + e);
        }
    }

    public static void save(MinecraftServer server) {
        try { Files.writeString(file(server), GSON.toJson(current)); }
        catch (IOException e) { System.err.println("[MineWatch] 맵 데이터를 저장하지 못했습니다: " + e); }
    }

    public static void clear() { current = new MapData(); }

    public static void set(MinecraftServer server, MapData data) {
        current = data;
        save(server);
    }

    public static void addSpawn(MinecraftServer server, int team, double x, double y, double z, float yaw) {
        current.dim = current.dim == null ? "minecraft:overworld" : current.dim;
        current.spawns(team).add(new double[]{x, y, z, yaw});
        save(server);
    }

    public static void setPoint(MinecraftServer server, ServerWorld world, double x, double y, double z) {
        current.dim = world.getRegistryKey().getValue().toString();
        current.point = new double[]{x, y, z};
        save(server);
    }

    public static void reset(MinecraftServer server) { current = new MapData(); save(server); }
}
