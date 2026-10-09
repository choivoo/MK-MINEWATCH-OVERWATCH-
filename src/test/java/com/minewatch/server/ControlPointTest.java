package com.minewatch.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ControlPointTest {
    private static int run(ControlPoint p, int ticks, int a, int b) {
        int captured = -1;
        for (int i = 0; i < ticks; i++) { int c = p.tick(a, b); if (c >= 0) captured = c; }
        return captured;
    }

    @Test void capturesAfterFiveSeconds() {
        ControlPoint p = new ControlPoint();
        assertEquals(-1, run(p, 99, 1, 0));
        assertEquals(0, run(p, 1, 1, 0));
        assertEquals(0, p.owner);
    }

    @Test void contestedFreezesProgress() {
        ControlPoint p = new ControlPoint();
        run(p, 50, 0, 2);
        int before = p.progress;
        run(p, 200, 3, 2);
        assertEquals(before, p.progress);
        assertEquals(-1, p.owner);
    }

    @Test void enemyMustDrainBeforeCapturing() {
        ControlPoint p = new ControlPoint();
        run(p, 100, 1, 0);                    // A 점령
        run(p, 60, 0, 1);                     // B 가 서도 소유자는 유지, 진행도만 쌓임
        assertEquals(0, p.owner);
        assertEquals(1, p.capTeam);
        assertEquals(60, p.progress);
        assertEquals(1, run(p, 40, 0, 1));    // 100 에 도달하면 B 가 가져감
        assertEquals(1, p.owner);
    }

    @Test void opposingTeamDrainsAnotherTeamsProgress() {
        ControlPoint p = new ControlPoint();
        run(p, 40, 1, 0);                     // A 가 40 진행
        run(p, 10, 0, 1);                     // B 가 서면 2씩 감소 -> 20
        assertEquals(20, p.progress);
        run(p, 10, 0, 1);                     // 0 이 되면 B 가 점령 시도 팀이 됨
        assertEquals(1, p.capTeam);
        assertEquals(1, p.progress);          // 0 에 도달한 같은 틱에 B 의 첫 진행이 반영됨
    }

    @Test void progressDecaysWhenEmpty() {
        ControlPoint p = new ControlPoint();
        run(p, 30, 1, 0);
        run(p, 40, 0, 0);
        assertEquals(0, p.progress);
        assertEquals(-1, p.capTeam);
    }

    @Test void ownerStandingResetsEnemyProgress() {
        ControlPoint p = new ControlPoint();
        run(p, 100, 1, 0);
        run(p, 30, 0, 1);
        run(p, 15, 1, 0);                     // 소유자가 서 있으면 2씩 감소
        assertEquals(0, p.progress);
    }
}
