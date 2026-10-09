package com.minewatch.client.dev;

import com.minewatch.client.screen.HeroSelectScreen;
import com.minewatch.client.screen.PartyScreen;
import com.minewatch.client.screen.PerkScreen;
import com.minewatch.hero.Hero;
import com.minewatch.hero.HeroRegistry;
import com.minewatch.hero.Role;
import com.minewatch.server.GameMaps;
import com.minewatch.server.HeroManager;
import com.minewatch.server.MatchManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * 개발 전용 스크린샷 하니스. -Dminewatch.dev=1 로 실행하면(`gradlew runDevShots`) 월드에 자동 입장해
 * 정해진 장면을 차례로 만들고 run/screenshots/dev_*.png 로 저장한 뒤 종료한다.
 * 일반 플레이에서는 시스템 속성이 없으므로 아무 일도 하지 않는다.
 */
public final class DevHarness {
    private record Step(int waitTicks, Consumer<MinecraftClient> action) {}
    private static final List<Step> STEPS = new ArrayList<>();
    private static int index = 0, wait = 0, worldTicks = 0;
    private static String only = "";

    public static void init() {
        only = System.getProperty("minewatch.only", "");
        script();
        ClientTickEvents.END_CLIENT_TICK.register(DevHarness::tick);
        System.out.println("[MineWatch] 개발 하니스 활성화: 장면 " + STEPS.size() + "개");
    }

    private static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || mc.getServer() == null) return;
        if (++worldTicks < 120) return;                              // 월드 입장 직후 안정화
        if (wait > 0) { wait--; return; }
        if (index >= STEPS.size()) return;
        Step s = STEPS.get(index++);
        try { s.action().accept(mc); } catch (RuntimeException e) { System.out.println("[MineWatch] 하니스 오류: " + e); e.printStackTrace(); }
        wait = s.waitTicks();
    }

    // ---- 도우미 ----

    private static void add(int waitAfter, Consumer<MinecraftClient> a) { STEPS.add(new Step(waitAfter, a)); }

    private static void shot(MinecraftClient mc, String name) {
        mc.getToastManager().clear();
        ScreenshotRecorder.saveScreenshot(mc.runDirectory, "dev_" + name + ".png", mc.getFramebuffer(), t -> {});
        System.out.println("[MineWatch] 스크린샷 " + name);
    }

    private static void server(MinecraftClient mc, Consumer<ServerPlayerEntity> fn) {
        var srv = mc.getServer();
        var id = mc.player.getUuid();
        srv.execute(() -> { var sp = srv.getPlayerManager().getPlayer(id); if (sp != null) fn.accept(sp); });
    }

    private static boolean wanted(String group) { return only.isEmpty() || only.contains(group); }

    // ---- 장면 목록 ----

    private static void script() {
        add(40, mc -> mc.setScreen(null));                        // 월드 입장 시 자동으로 열린 로비를 닫는다
        if (wanted("ui")) {
            add(20, mc -> { mc.setScreen(null); });
            add(40, mc -> shot(mc, "01_lobby_auto"));
            add(10, mc -> mc.setScreen(new com.minewatch.client.screen.HomeScreen(null)));
            add(30, mc -> shot(mc, "02_lobby"));
            add(5, mc -> mc.setScreen(new com.minewatch.client.screen.PlayScreen(null)));
            add(10, mc -> shot(mc, "02_play"));
            add(5, mc -> mc.setScreen(new com.minewatch.client.screen.PanelScreen(null, com.minewatch.client.screen.PanelScreen.Kind.SHOP)));
            add(15, mc -> shot(mc, "02_shop"));
            add(5, mc -> mc.setScreen(new com.minewatch.client.screen.HomeScreen(null)));
            // 영웅 선택: 영웅마다 3D 미리보기(스킨) 장면. 처음 한 장은 등장 섬광 직후에 찍는다.
            add(10, mc -> { var s = new HeroSelectScreen(null); mc.setScreen(s); s.devPreview("tracer"); });
            add(18, mc -> shot(mc, "03_pick_flash"));
            for (Hero h : HeroRegistry.all()) {
                add(10, mc -> { if (mc.currentScreen instanceof HeroSelectScreen s) s.devPreview(h.id); });
                add(30, mc -> shot(mc, "03_pick_" + h.id));
            }
            add(10, mc -> mc.setScreen(new PartyScreen(null)));
            add(20, mc -> shot(mc, "04_settings"));
            add(5, mc -> mc.setScreen(null));
        }
        if (wanted("heroes")) {
            // 평평한 아레나(중앙 광장) 스폰에 세워 하늘/벽이 보이게 한다
            add(90, mc -> server(mc, sp -> {
                GameMaps.Def def = GameMaps.byId("plaza");
                GameMaps.buildAndApply(sp.getServer().getOverworld(), def);
                sp.teleport(sp.getServer().getOverworld(), def.baseX() + 0.5, GameMaps.FLOOR_Y, def.baseZ() - 10.5, 0f, 8f);
            }));
            for (Hero h : HeroRegistry.all()) {
                add(45, mc -> { mc.options.setPerspective(Perspective.FIRST_PERSON); server(mc, sp -> HeroManager.select(sp, h)); });
                add(5, mc -> shot(mc, "10_" + h.id + "_fp"));
                add(5, mc -> mc.options.setPerspective(Perspective.THIRD_PERSON_BACK));
                add(25, mc -> shot(mc, "11_" + h.id + "_tp"));
            }
            add(5, mc -> mc.options.setPerspective(Perspective.FIRST_PERSON));
        }
        if (wanted("anim")) {
            add(90, mc -> server(mc, sp -> {
                GameMaps.Def def = GameMaps.byId("plaza");
                GameMaps.buildAndApply(sp.getServer().getOverworld(), def);
                sp.teleport(sp.getServer().getOverworld(), def.baseX() + 0.5, GameMaps.FLOOR_Y, def.baseZ() - 10.5, 0f, 8f);
                HeroManager.select(sp, HeroRegistry.TRACER);
            }));
            add(40, mc -> mc.options.setPerspective(Perspective.FIRST_PERSON));
            for (String a : new String[]{"fire_left", "fire_right", "reload", "melee", "blink", "recall", "bomb", "run"}) {
                add(8, mc -> { if (mc.player.getMainHandStack().getItem() instanceof com.minewatch.GeoWeaponItem g) g.playLocal(mc.player.getMainHandStack(), a); });
                add(6, mc -> shot(mc, "40_tracer_anim_" + a + "_a"));
                add(8, mc -> shot(mc, "40_tracer_anim_" + a + "_b"));
                add(30, mc -> {});
            }
        }
        if (wanted("fx")) {
            add(90, mc -> server(mc, sp -> {
                GameMaps.Def def = GameMaps.byId("plaza");
                GameMaps.buildAndApply(sp.getServer().getOverworld(), def);
                sp.teleport(sp.getServer().getOverworld(), def.baseX() + 0.5, GameMaps.FLOOR_Y, def.baseZ() - 10.5, 0f, 8f);
                HeroManager.select(sp, HeroRegistry.TRACER);
            }));
            add(30, mc -> mc.options.setPerspective(Perspective.THIRD_PERSON_BACK));
            for (String k : new String[]{"bullet_cyan", "bullet_gold", "muzzle", "impact", "blink_trail", "blink_flash", "recall_ring", "bomb", "bomb_blast"}) {
                add(10, mc -> server(mc, sp -> {
                    var look = sp.getRotationVec(1f);
                    var at = sp.getPos().add(look.x * 4, 1.2, look.z * 4);
                    float len = k.startsWith("blink_trail") ? 6f : k.startsWith("bullet") ? 2.5f : 1f;
                    var d = k.startsWith("bullet") || k.equals("blink_trail") ? new net.minecraft.util.math.Vec3d(1, 0, 0) : new net.minecraft.util.math.Vec3d(0, 0, 1);
                    if (k.equals("blink_trail")) at = at.add(-3, 0, 0);
                    com.minewatch.server.Fx.spawn(sp.getServerWorld(), k, at.add(k.equals("recall_ring") || k.equals("bomb_blast") ? new net.minecraft.util.math.Vec3d(0, -1.2, 0) : net.minecraft.util.math.Vec3d.ZERO), d, len, 12);
                }));
                add(3, mc -> shot(mc, "50_fx_" + k + "_a"));
                add(3, mc -> shot(mc, "50_fx_" + k + "_b"));
                add(20, mc -> {});
            }
            add(5, mc -> mc.options.setPerspective(Perspective.FIRST_PERSON));
        }
        if (wanted("perk")) {
            add(5, mc -> server(mc, sp -> { HeroManager.select(sp, HeroRegistry.TRACER); HeroManager.stateOf(sp).perk.addXp(500); }));
            add(40, mc -> mc.setScreen(new PerkScreen()));
            add(20, mc -> shot(mc, "20_perk_minor"));
            add(5, mc -> mc.setScreen(null));
        }
        if (wanted("match")) {
            add(5, mc -> server(mc, sp -> {
                GameMaps.Def def = GameMaps.byId("plaza");
                GameMaps.buildAndApply(sp.getServer().getOverworld(), def);
                HeroManager.select(sp, HeroRegistry.SOLDIER76);
                MatchManager.start(sp.getServer(), MatchManager.MODE_TDM, 30, 300, true, 1);
            }));
            add(60, mc -> shot(mc, "30_match_countdown"));
            add(220, mc -> shot(mc, "31_match_live_start"));
            add(100, mc -> shot(mc, "32_match_live_bots"));
            add(5, mc -> mc.options.setPerspective(Perspective.THIRD_PERSON_FRONT));
            add(20, mc -> shot(mc, "33_match_third_person"));
            add(5, mc -> { mc.options.setPerspective(Perspective.FIRST_PERSON); server(mc, sp -> MatchManager.stop(sp.getServer())); });
        }
        add(40, mc -> { System.out.println("[MineWatch] 하니스 완료"); mc.scheduleStop(); });
    }
    private DevHarness() {}
}
