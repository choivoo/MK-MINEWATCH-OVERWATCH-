package com.minewatch.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** 서버 설정(config/minewatch.json). 온라인 매치 큐 동작을 정한다. */
public final class ServerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** 매치 시작에 필요한 최소 인원. */
    public int minPlayers = 2;
    /** 한 매치의 최대 인원. */
    public int maxPlayers = 12;
    /** 최소 인원이 모인 뒤 매치 시작까지 대기(초). */
    public int queueSeconds = 15;
    /** 인원이 적을 때 각 팀을 이 인원까지 봇으로 채운다(0 이면 채우지 않음). */
    public int fillTeamSize = 0;
    /** 봇 난이도 0~2. */
    public int botDifficulty = 1;
    /** "tdm", "control", "random". */
    public String mode = "random";
    public int targetKills = 20;
    public int targetPoints = 100;

    private static ServerConfig current = new ServerConfig();
    public static ServerConfig get() { return current; }

    /** 잘못된 값은 안전한 범위로 보정한다. */
    public void sanitize() {
        minPlayers = Math.max(1, Math.min(minPlayers, 64));
        maxPlayers = Math.max(minPlayers, Math.min(maxPlayers, 64));
        queueSeconds = Math.max(1, Math.min(queueSeconds, 600));
        fillTeamSize = Math.max(0, Math.min(fillTeamSize, 16));
        botDifficulty = Math.max(0, Math.min(botDifficulty, 2));
        targetKills = Math.max(1, targetKills);
        targetPoints = Math.max(1, targetPoints);
        if (mode == null || !(mode.equals("tdm") || mode.equals("control"))) mode = "random";
    }

    public static void load() {
        Path f = FabricLoader.getInstance().getConfigDir().resolve("minewatch.json");
        ServerConfig c = new ServerConfig();
        try {
            if (Files.exists(f)) {
                ServerConfig read = GSON.fromJson(Files.readString(f), ServerConfig.class);
                if (read != null) c = read;
            }
            c.sanitize();
            Files.writeString(f, GSON.toJson(c));       // 누락된 항목을 채워 다시 저장
        } catch (IOException | RuntimeException e) {
            System.err.println("[MineWatch] 설정을 읽지 못해 기본값을 사용합니다: " + e);
            c = new ServerConfig();
        }
        current = c;
    }
}
