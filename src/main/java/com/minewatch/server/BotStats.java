package com.minewatch.server;

/** 봇 종류/난이도 능력치(순수 로직). 체력과 피해는 오버워치 단위. */
public final class BotStats {
    public enum Type {
        //          이름        체력  사거리  피해  발사간격(틱) 폭발반경(0=히트스캔)
        BASIC("basic", 150, 16, 8, 10, 0),
        ASSAULT("assault", 250, 9, 6, 4, 0),
        ROCKET("rocket", 200, 24, 40, 50, 3),
        FRIENDLY("friendly", 150, 16, 8, 10, 0);

        public final String key;
        public final int health, range, damage, fireTicks, splash;
        Type(String key, int health, int range, int damage, int fireTicks, int splash) {
            this.key = key; this.health = health; this.range = range; this.damage = damage;
            this.fireTicks = fireTicks; this.splash = splash;
        }
        public static Type of(int ordinal) { return values()[Math.floorMod(ordinal, values().length)]; }
    }

    public enum Difficulty {
        //         정확도  발사간격 배율  반응 틱  적 봇 수
        EASY(0.35, 1.5, 25, 3),
        NORMAL(0.55, 1.0, 12, 5),
        HARD(0.80, 0.75, 4, 7);

        public final double accuracy, rateMul;
        public final int reactionTicks, botsPerSide;
        Difficulty(double accuracy, double rateMul, int reactionTicks, int botsPerSide) {
            this.accuracy = accuracy; this.rateMul = rateMul; this.reactionTicks = reactionTicks; this.botsPerSide = botsPerSide;
        }
        public static Difficulty of(int ordinal) { return values()[Math.floorMod(ordinal, values().length)]; }
    }

    /** 거리별 명중 확률. 가까울수록 높고, 사거리 끝에서는 정확도의 60%. */
    public static double hitChance(Type type, Difficulty diff, double distance) {
        double t = Math.min(1.0, Math.max(0.0, distance / Math.max(1, type.range)));
        return diff.accuracy * (1.0 - 0.4 * t);
    }

    public static int fireInterval(Type type, Difficulty diff) {
        return Math.max(2, (int) Math.round(type.fireTicks * diff.rateMul));
    }

    /** 폭발 피해: 중심 100% -> 가장자리 50%. */
    public static double splashDamage(Type type, double distance) {
        if (type.splash <= 0 || distance > type.splash) return 0;
        return type.damage * (1.0 - 0.5 * distance / type.splash);
    }

    /** 적 팀 봇 구성: 일반/돌격/로켓 순환. */
    public static Type enemyTypeAt(int index) {
        return switch (Math.floorMod(index, 3)) { case 0 -> Type.BASIC; case 1 -> Type.ASSAULT; default -> Type.ROCKET; };
    }
    private BotStats() {}
}
