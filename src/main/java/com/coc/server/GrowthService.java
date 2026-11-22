package com.coc.server;

import com.coc.data.CharacterSheet;
import com.coc.net.CocNetwork;
import com.coc.rules.DiceRoller;
import com.coc.rules.SkillDef;
import com.coc.rules.SkillRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/** 成长检定：对每个有成长标记的技能掷 d100，大于当前值则 +1d10。 */
public final class GrowthService {
    private GrowthService() {
    }

    public static List<String> grow(ServerPlayer player, CharacterSheet sheet) {
        List<String> lines = new ArrayList<>();
        if (player == null || sheet == null) return lines;
        if (sheet.improvementMarks.isEmpty()) {
            player.sendSystemMessage(Component.literal("没有成长标记。先成功使用技能再来成长。")
                    .withStyle(ChatFormatting.YELLOW));
            return lines;
        }
        RandomSource random = player.serverLevel().random;
        List<String> marks = new ArrayList<>(sheet.improvementMarks);
        sheet.improvementMarks.clear();
        for (String id : marks) {
            SkillDef def = SkillRegistry.resolve(id);
            if (def == null) continue;
            int old = sheet.skill(def.id);
            int roll = DiceRoller.d100(random);
            if (roll > old) {
                int gain = DiceRoller.d10(random);
                int newValue = Math.min(99, old + gain);
                sheet.setSkill(def.id, newValue);
                String line = def.name + "：" + old + " → " + newValue + "（d100=" + roll + "）";
                lines.add(line);
                player.sendSystemMessage(Component.literal("【成长】" + line)
                        .withStyle(ChatFormatting.GREEN));
            } else {
                String line = def.name + "：未成长（d100=" + roll + " ≤ " + old + "）";
                lines.add(line);
                player.sendSystemMessage(Component.literal("【成长】" + line)
                        .withStyle(ChatFormatting.GRAY));
            }
        }
        CocNetwork.syncSheet(player, sheet);
        return lines;
    }
}
