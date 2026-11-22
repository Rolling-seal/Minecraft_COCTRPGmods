package com.coc.rules;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 纯规则骰子引擎：表达式、百分骰、奖惩骰、成功等级。 */
public final class DiceRoller {
    private static final Pattern DICE_PATTERN = Pattern.compile("^(\\d*)[dD](\\d+)([+-]\\d+)?$");
    private static final Pattern INT_PATTERN = Pattern.compile("^[+-]?\\d+$");

    private DiceRoller() {
    }

    public record DiceRoll(String expression, int total, List<Integer> rolls, int modifier) {
    }

    public record PercentileRoll(int roll, int tens, int units, List<Integer> tensDice, int bonus) {
    }

    public record CheckResult(int skill, Difficulty difficulty, PercentileRoll percentile,
                              SuccessLevel level, boolean success, boolean critical, boolean fumble) {
        public int effectiveTarget() {
            return difficulty.target(skill);
        }
    }

    public static DiceRoll roll(String expression, RandomSource random) {
        String expr = expression == null ? "" : expression.trim().replace(" ", "");
        Matcher m = DICE_PATTERN.matcher(expr);
        if (!m.matches()) {
            throw new IllegalArgumentException("无法识别的骰子表达式: " + expression);
        }
        int count = m.group(1).isEmpty() ? 1 : Integer.parseInt(m.group(1));
        int sides = Integer.parseInt(m.group(2));
        int modifier = m.group(3) == null ? 0 : Integer.parseInt(m.group(3));
        if (count < 1 || count > 100) {
            throw new IllegalArgumentException("骰子数量必须在 1~100 之间");
        }
        if (sides < 2 || sides > 100) {
            throw new IllegalArgumentException("骰子面数必须在 2~100 之间");
        }
        List<Integer> rolls = new ArrayList<>(count);
        int total = modifier;
        for (int i = 0; i < count; i++) {
            int value = random.nextInt(sides) + 1;
            rolls.add(value);
            total += value;
        }
        return new DiceRoll(expr, total, rolls, modifier);
    }

    /** 支持 "1d6"、"-1d3"、"+2"、"5"。用于事件效果数值。 */
    public static int evalNumber(String expression, RandomSource random) {
        if (expression == null || expression.isBlank()) return 0;
        String expr = expression.trim().replace(" ", "");
        int sign = 1;
        if (expr.startsWith("-")) {
            sign = -1;
            expr = expr.substring(1);
        } else if (expr.startsWith("+")) {
            expr = expr.substring(1);
        }
        if (expr.isEmpty()) return 0;
        if (INT_PATTERN.matcher(expr).matches()) {
            return sign * Integer.parseInt(expr);
        }
        return sign * roll(expr, random).total();
    }

    /** bonus>0 为奖励骰，bonus<0 为惩罚骰，最多 ±2。 */
    public static PercentileRoll percentile(int bonus, RandomSource random) {
        int b = Math.max(-2, Math.min(2, bonus));
        int units = random.nextInt(10);
        int diceCount = Math.abs(b) + 1;
        List<Integer> tensDice = new ArrayList<>(diceCount);
        int chosenTens = 0;
        int chosenValue = b < 0 ? -1 : 101;
        for (int i = 0; i < diceCount; i++) {
            int tens = random.nextInt(10) * 10;
            tensDice.add(tens);
            int value = (tens == 0 && units == 0) ? 100 : tens + units;
            if (b < 0) {
                // 惩罚骰：取更大（更差）的结果
                if (value > chosenValue) {
                    chosenValue = value;
                    chosenTens = tens;
                }
            } else {
                // 奖励骰：取更小（更好）的结果
                if (value < chosenValue) {
                    chosenValue = value;
                    chosenTens = tens;
                }
            }
        }
        return new PercentileRoll(chosenValue, chosenTens, units, tensDice, b);
    }

    public static CheckResult check(int skill, Difficulty difficulty, int bonus, RandomSource random) {
        int value = Math.max(0, Math.min(999, skill));
        PercentileRoll p = percentile(bonus, random);
        SuccessLevel level = evaluate(p.roll(), value);
        boolean success = level == SuccessLevel.CRITICAL || level.rank() >= difficulty.requiredRank();
        return new CheckResult(value, difficulty, p, level, success,
                level == SuccessLevel.CRITICAL, level == SuccessLevel.FUMBLE);
    }

    /** COC 7e 成功等级。97-100 的边界按技能是否 <50 判定大失败。 */
    public static SuccessLevel evaluate(int roll, int skill) {
        if (roll == 100 || (skill < 50 && roll >= 96)) return SuccessLevel.FUMBLE;
        if (roll == 1) return SuccessLevel.CRITICAL;
        if (roll <= Math.max(1, skill / 5)) return SuccessLevel.EXTREME;
        if (roll <= Math.max(1, skill / 2)) return SuccessLevel.HARD;
        if (roll <= skill) return SuccessLevel.REGULAR;
        return SuccessLevel.FAILURE;
    }

    public static int d100(RandomSource random) {
        return random.nextInt(100) + 1;
    }

    public static int d10(RandomSource random) {
        return random.nextInt(10) + 1;
    }

    public static int d6(RandomSource random) {
        return random.nextInt(6) + 1;
    }

    public static int d3(RandomSource random) {
        return random.nextInt(3) + 1;
    }
}
