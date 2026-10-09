package com.minewatch.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QueueLogicTest {
    private static UUID id(int n) { return new UUID(0, n); }

    private static List<UUID> run(QueueLogic q, int ticks, boolean running) {
        for (int i = 0; i < ticks; i++) { List<UUID> r = q.tick(running); if (r != null) return r; }
        return null;
    }

    @Test void waitsForMinimumPlayers() {
        QueueLogic q = new QueueLogic(2, 6, 100);
        q.join(id(1));
        assertNull(run(q, 500, false));
        assertEquals(-1, q.secondsLeft());
    }

    @Test void startsAfterCountdownOnceMinimumReached() {
        QueueLogic q = new QueueLogic(2, 6, 100);
        q.join(id(1)); q.join(id(2));
        assertNull(run(q, 99, false));
        assertEquals(1, q.secondsLeft());
        List<UUID> r = run(q, 1, false);
        assertEquals(List.of(id(1), id(2)), r);
        assertEquals(0, q.size());
    }

    @Test void startsImmediatelyWhenFull() {
        QueueLogic q = new QueueLogic(2, 3, 1000);
        q.join(id(1)); q.join(id(2)); q.join(id(3)); q.join(id(4));
        List<UUID> r = q.tick(false);
        assertEquals(List.of(id(1), id(2), id(3)), r);   // 대기 순서대로 최대 인원
        assertEquals(1, q.size());                       // 4번째는 다음 매치로
    }

    @Test void leavingBelowMinimumResetsCountdown() {
        QueueLogic q = new QueueLogic(2, 6, 100);
        q.join(id(1)); q.join(id(2));
        run(q, 50, false);
        q.leave(id(2));
        assertEquals(-1, q.secondsLeft());
        q.join(id(3));
        assertNull(run(q, 99, false));                   // 카운트다운이 처음부터 다시
        assertEquals(List.of(id(1), id(3)), run(q, 1, false));
    }

    @Test void doesNotStartWhileMatchRunning() {
        QueueLogic q = new QueueLogic(2, 6, 10);
        q.join(id(1)); q.join(id(2));
        assertNull(run(q, 1000, true));
        assertEquals(2, q.size());
        assertTrue(run(q, 20, false) != null);
    }

    @Test void duplicateJoinIgnoredAndLeaveReportsState() {
        QueueLogic q = new QueueLogic(1, 2, 5);
        assertTrue(q.join(id(1)));
        assertFalse(q.join(id(1)));
        assertEquals(1, q.size());
        assertTrue(q.leave(id(1)));
        assertFalse(q.leave(id(1)));
    }
}
