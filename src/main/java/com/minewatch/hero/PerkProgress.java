package com.minewatch.hero;

/**
 * 퍽 진행도(순수 로직). 경험치가 기준에 도달하면 마이너 퍽 -> 메이저 퍽 순서로 선택권이 생긴다.
 * stage: 0 아무것도 없음, 1 마이너 선택 가능, 2 마이너 선택 완료, 3 메이저 선택 가능, 4 메이저 선택 완료.
 */
public final class PerkProgress {
    public static final int MINOR_XP = 400, MAJOR_XP = 1200;

    public double xp;
    public int stage;
    public String minor = "", major = "";
    /** 마지막으로 클라이언트에 알린 상태와 달라졌는지. */
    public boolean dirty = true;

    public void addXp(double amount) {
        if (amount <= 0) return;
        xp += amount;
        advance();
        dirty = true;
    }

    private void advance() {
        if (stage == 0 && xp >= MINOR_XP) stage = 1;
        if (stage == 2 && xp >= MAJOR_XP) stage = 3;
    }

    /** 지금 선택할 수 있는 퍽 단계(0 마이너, 1 메이저). 없으면 -1. */
    public int offerTier() { return stage == 1 ? 0 : stage == 3 ? 1 : -1; }

    /** 다음 목표 경험치. 모두 선택했으면 -1. */
    public int nextThreshold() {
        return stage == 0 || stage == 1 ? MINOR_XP : stage == 2 || stage == 3 ? MAJOR_XP : -1;
    }

    /** 선택. 해당 단계에서 가능한 선택이면 true. */
    public boolean choose(int tier, String id) {
        if (tier != offerTier() || id == null || id.isEmpty()) return false;
        if (tier == 0) { minor = id; stage = 2; } else { major = id; stage = 4; }
        advance();
        dirty = true;
        return true;
    }
}
