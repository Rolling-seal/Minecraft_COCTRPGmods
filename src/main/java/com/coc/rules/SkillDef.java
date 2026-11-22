package com.coc.rules;

/** 技能定义。新增技能只需要在 SkillRegistry 注册一条。 */
public final class SkillDef {
    public enum BaseType {
        FIXED, DEX_HALF, EDU
    }

    public final String id;
    public final String name;
    public final BaseType baseType;
    public final int fixed;

    public SkillDef(String id, String name, BaseType baseType, int fixed) {
        this.id = id;
        this.name = name;
        this.baseType = baseType;
        this.fixed = fixed;
    }

    public int baseValue(int dex, int edu) {
        return switch (baseType) {
            case FIXED -> fixed;
            case DEX_HALF -> Math.max(1, dex / 2);
            case EDU -> Math.max(1, edu);
        };
    }
}
