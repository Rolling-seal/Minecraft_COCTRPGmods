package com.coc.rules;

/** 检定难度。COC 7e：常规=技能值，困难=1/2，极难=1/5。 */
public enum Difficulty {
    REGULAR("常规", 1),
    HARD("困难", 2),
    EXTREME("极难", 5);

    private final String cn;
    private final int divisor;

    Difficulty(String cn, int divisor) {
        this.cn = cn;
        this.divisor = divisor;
    }

    public String cn() {
        return cn;
    }

    public int divisor() {
        return divisor;
    }

    public int target(int skill) {
        return Math.max(1, skill / divisor);
    }

    public int requiredRank() {
        return switch (this) {
            case REGULAR -> SuccessLevel.REGULAR.rank();
            case HARD -> SuccessLevel.HARD.rank();
            case EXTREME -> SuccessLevel.EXTREME.rank();
        };
    }

    public static Difficulty byName(String name) {
        if (name == null) return REGULAR;
        for (Difficulty d : values()) {
            if (d.name().equalsIgnoreCase(name) || d.cn.equals(name)) return d;
        }
        return REGULAR;
    }
}
