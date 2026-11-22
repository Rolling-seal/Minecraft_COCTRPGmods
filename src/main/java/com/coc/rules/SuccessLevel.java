package com.coc.rules;

/** 成功等级。rank 越高越好；失败 rank=1，大失败=0。 */
public enum SuccessLevel {
    FUMBLE("大失败", 0, 0xFF8B0000),
    FAILURE("失败", 1, 0xFFB03030),
    REGULAR("常规成功", 2, 0xFF3FA34D),
    HARD("困难成功", 3, 0xFF2E8B57),
    EXTREME("极难成功", 4, 0xFF1E6FD9),
    CRITICAL("大成功", 5, 0xFFD4AF37);

    private final String cn;
    private final int rank;
    private final int color;

    SuccessLevel(String cn, int rank, int color) {
        this.cn = cn;
        this.rank = rank;
        this.color = color;
    }

    public String cn() {
        return cn;
    }

    public int rank() {
        return rank;
    }

    public int color() {
        return color;
    }
}
