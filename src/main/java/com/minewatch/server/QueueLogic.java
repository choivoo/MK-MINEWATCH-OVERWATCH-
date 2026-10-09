package com.minewatch.server;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 온라인 매치 큐(순수 로직). 최소 인원이 모이면 카운트다운을 시작하고,
 * 카운트다운이 끝나거나 최대 인원이 차면 대기 순서대로 최대 인원까지 뽑아 매치를 시작한다.
 */
public final class QueueLogic {
    private final int min, max, countdownTicks;
    private final Set<UUID> queued = new LinkedHashSet<>();
    private int countdown = -1;

    public QueueLogic(int min, int max, int countdownTicks) {
        this.min = Math.max(1, min);
        this.max = Math.max(this.min, max);
        this.countdownTicks = Math.max(1, countdownTicks);
    }

    public boolean join(UUID id) { return queued.add(id); }
    public boolean leave(UUID id) {
        boolean removed = queued.remove(id);
        if (queued.size() < min) countdown = -1;
        return removed;
    }
    public boolean contains(UUID id) { return queued.contains(id); }
    public int size() { return queued.size(); }
    public int min() { return min; }
    public Set<UUID> snapshot() { return new LinkedHashSet<>(queued); }
    public void clear() { queued.clear(); countdown = -1; }

    /** 남은 카운트다운(초, 올림). 진행 중이 아니면 -1. */
    public int secondsLeft() { return countdown < 0 ? -1 : (countdown + 19) / 20; }

    /**
     * 한 틱 진행. 매치를 시작해야 하면 참가자 목록을 반환(큐에서 제거됨), 아니면 null.
     * matchRunning 이 true 면 새 매치를 시작하지 않고 대기만 한다.
     */
    public List<UUID> tick(boolean matchRunning) {
        if (queued.size() < min) { countdown = -1; return null; }
        if (matchRunning) { countdown = -1; return null; }
        if (queued.size() >= max) return drain();
        if (countdown < 0) countdown = countdownTicks;
        if (--countdown <= 0) return drain();
        return null;
    }

    private List<UUID> drain() {
        List<UUID> picked = new ArrayList<>();
        for (UUID id : queued) { if (picked.size() >= max) break; picked.add(id); }
        queued.removeAll(picked);
        countdown = -1;
        return picked;
    }
}
