package com.coc.rules;

import java.util.List;

/** 职业定义。点数公式遵循 COC 7e：EDU×4 或 EDU×2+另一属性×2。 */
public final class Occupation {
    public final String id;
    public final String name;
    public final String description;
    public final int eduMultiplier;
    public final Stat secondStat;
    public final int secondMultiplier;
    public final List<String> skills;

    public Occupation(String id, String name, String description, int eduMultiplier,
                      Stat secondStat, int secondMultiplier, List<String> skills) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.eduMultiplier = eduMultiplier;
        this.secondStat = secondStat;
        this.secondMultiplier = secondMultiplier;
        this.skills = skills;
    }

    public int points(Stats stats) {
        int points = stats.edu * eduMultiplier;
        if (secondStat != null) points += stats.get(secondStat) * secondMultiplier;
        return Math.max(0, points);
    }

    public String formulaText() {
        if (secondStat == null) return "EDU × " + eduMultiplier;
        return "EDU × " + eduMultiplier + " + " + secondStat.abbr() + " × " + secondMultiplier;
    }

    public boolean canSpendOccupation(String skillId) {
        return skills.contains(skillId) || "credit_rating".equals(skillId);
    }
}
