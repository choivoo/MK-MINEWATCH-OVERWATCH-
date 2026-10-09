package com.minewatch.server;

/**
 * 점령지 판정(순수 로직). 한 팀만 점령지 안에 있으면 100틱(5초) 후 점령한다.
 * 양 팀이 함께 있으면 정지, 점령 진행 중인 팀과 다른 팀이 서면 진행도가 먼저 감소한다.
 */
public final class ControlPoint {
    public static final int CAPTURE_TICKS = 100;

    public int owner = -1, capTeam = -1, progress = 0;

    public void reset() { owner = -1; capTeam = -1; progress = 0; }

    /** 한 틱 진행. 이번 틱에 새로 점령한 팀(0/1)을 반환, 없으면 -1. */
    public int tick(int presentA, int presentB) {
        if (presentA > 0 && presentB > 0) return -1;                  // 교전 중: 정지
        int only = presentA > 0 ? 0 : presentB > 0 ? 1 : -1;
        if (only >= 0 && only != owner) {
            if (capTeam != only) {
                if (progress > 0) progress = Math.max(0, progress - 2);
                if (progress == 0) capTeam = only;
            }
            if (capTeam == only && ++progress >= CAPTURE_TICKS) {
                owner = only; capTeam = -1; progress = 0;
                return only;
            }
        } else if (progress > 0) {
            progress = Math.max(0, progress - (only >= 0 ? 2 : 1));
            if (progress == 0) capTeam = -1;
        }
        return -1;
    }
}
