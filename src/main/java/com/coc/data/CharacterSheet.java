package com.coc.data;

import com.coc.rules.CharacterRules;
import com.coc.rules.SanityRules;
import com.coc.rules.SkillRegistry;
import com.coc.rules.Stats;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 调查员角色卡。服务端权威，客户端持只读副本。 */
public final class CharacterSheet {
    public static final String[] BACKSTORY_KEYS = {
            "信念", "重要之人", "意义非凡之地", "宝贵之物", "特质", "伤口"
    };

    public UUID uuid = UUID.randomUUID();
    public String name = "";
    public int age = 25;
    public String occupationId = "";
    public String occupationName = "";

    public final Stats stats = new Stats();

    public int hp;
    public int maxHp;
    public int mp;
    public int maxMp;
    public int san;
    public int maxSan = 99;
    public int mythos;
    public int mov = 8;
    public String db = "0";
    public int build;

    public final Map<String, Integer> skills = new LinkedHashMap<>();
    public final Set<String> improvementMarks = new LinkedHashSet<>();

    public long tempInsanityEnd;
    public String tempInsanity = "";
    public boolean indefiniteInsanity;
    public boolean retired;
    public List<String> phobias = new ArrayList<>();
    public List<String> manias = new ArrayList<>();

    public int dailySanLoss;
    public long lastSanDay = -1;

    public final List<Handout> handouts = new ArrayList<>();
    public final Map<String, String> backstory = new LinkedHashMap<>();
    public String notes = "";

    public boolean dying;

    public CharacterSheet() {
        for (String key : BACKSTORY_KEYS) backstory.put(key, "");
    }

    public CharacterSheet(UUID uuid, String name) {
        this();
        this.uuid = uuid;
        this.name = name;
    }

    public void recalculateDerived() {
        maxHp = CharacterRules.maxHp(stats);
        maxMp = CharacterRules.maxMp(stats);
        mov = CharacterRules.mov(stats, age);
        db = CharacterRules.dbText(stats.str + stats.siz);
        build = CharacterRules.buildValue(stats.str + stats.siz);
        maxSan = SanityRules.maxSan(mythos);
        san = Math.max(0, Math.min(san, maxSan));
        hp = Math.max(0, Math.min(hp, maxHp));
        mp = Math.max(0, Math.min(mp, maxMp));
        if (hp <= 0) dying = true;
    }

    public int skill(String id) {
        return skills.getOrDefault(id, 0);
    }

    public String skillName(String id) {
        return SkillRegistry.nameOf(id);
    }

    public void setSkill(String id, int value) {
        skills.put(id, Math.max(0, Math.min(99, value)));
    }

    public void markImprovement(String id) {
        if (id != null) improvementMarks.add(id);
    }

    public boolean isTempInsane(long gameTime) {
        return tempInsanityEnd > gameTime;
    }

    public boolean isInsane(long gameTime) {
        return retired || indefiniteInsanity || isTempInsane(gameTime);
    }

    public void addHandout(Handout handout) {
        handouts.add(handout);
        while (handouts.size() > 100) handouts.remove(0);
    }

    public double sanRatio() {
        return maxSan <= 0 ? 0 : Math.max(0, Math.min(1, (double) san / maxSan));
    }

    public String statusLine(long gameTime) {
        if (retired) return "永久疯狂（SAN 归零）";
        StringBuilder sb = new StringBuilder();
        if (indefiniteInsanity) sb.append("不定期疯狂");
        if (isTempInsane(gameTime)) {
            if (sb.length() > 0) sb.append(" · ");
            long ticks = tempInsanityEnd - gameTime;
            sb.append("临时疯狂（约 ").append(Math.max(1, ticks / 1000)).append(" 小时）");
        }
        if (dying) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append("濒死");
        }
        return sb.length() == 0 ? "神志清醒" : sb.toString();
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("uuid", uuid.toString());
        tag.putString("name", name);
        tag.putInt("age", age);
        tag.putString("occupation_id", occupationId);
        tag.putString("occupation_name", occupationName);
        tag.put("stats", stats.toNbt());
        tag.putInt("hp", hp);
        tag.putInt("max_hp", maxHp);
        tag.putInt("mp", mp);
        tag.putInt("max_mp", maxMp);
        tag.putInt("san", san);
        tag.putInt("max_san", maxSan);
        tag.putInt("mythos", mythos);
        tag.putInt("mov", mov);
        tag.putString("db", db);
        tag.putInt("build", build);

        CompoundTag skillTag = new CompoundTag();
        for (Map.Entry<String, Integer> e : skills.entrySet()) {
            skillTag.putInt(e.getKey(), e.getValue());
        }
        tag.put("skills", skillTag);

        tag.put("marks", stringList(improvementMarks));
        tag.putLong("temp_end", tempInsanityEnd);
        tag.putString("temp_name", tempInsanity == null ? "" : tempInsanity);
        tag.putBoolean("indefinite", indefiniteInsanity);
        tag.putBoolean("retired", retired);
        tag.put("phobias", stringList(phobias));
        tag.put("manias", stringList(manias));
        tag.putInt("daily_loss", dailySanLoss);
        tag.putLong("last_day", lastSanDay);

        ListTag handoutList = new ListTag();
        for (Handout h : handouts) handoutList.add(h.toNbt());
        tag.put("handouts", handoutList);

        CompoundTag bg = new CompoundTag();
        for (Map.Entry<String, String> e : backstory.entrySet()) {
            bg.putString(e.getKey(), e.getValue());
        }
        tag.put("backstory", bg);
        tag.putString("notes", notes == null ? "" : notes);
        tag.putBoolean("dying", dying);
        return tag;
    }

    private static ListTag stringList(Iterable<String> values) {
        ListTag list = new ListTag();
        for (String value : values) {
            if (value != null) list.add(StringTag.valueOf(value));
        }
        return list;
    }

    public static CharacterSheet fromNbt(CompoundTag tag) {
        CharacterSheet sheet = new CharacterSheet();
        try {
            sheet.uuid = UUID.fromString(tag.getString("uuid"));
        } catch (Exception ignored) {
        }
        sheet.name = tag.getString("name");
        sheet.age = tag.contains("age") ? tag.getInt("age") : 25;
        sheet.occupationId = tag.getString("occupation_id");
        sheet.occupationName = tag.getString("occupation_name");
        if (tag.contains("stats")) {
            Stats s = Stats.fromNbt(tag.getCompound("stats"));
            sheet.stats.str = s.str;
            sheet.stats.con = s.con;
            sheet.stats.siz = s.siz;
            sheet.stats.dex = s.dex;
            sheet.stats.app = s.app;
            sheet.stats.intel = s.intel;
            sheet.stats.pow = s.pow;
            sheet.stats.edu = s.edu;
            sheet.stats.luck = s.luck;
        }
        sheet.hp = tag.getInt("hp");
        sheet.maxHp = tag.getInt("max_hp");
        sheet.mp = tag.getInt("mp");
        sheet.maxMp = tag.getInt("max_mp");
        sheet.san = tag.getInt("san");
        sheet.maxSan = tag.contains("max_san") ? tag.getInt("max_san") : 99;
        sheet.mythos = tag.getInt("mythos");
        sheet.mov = tag.contains("mov") ? tag.getInt("mov") : 8;
        sheet.db = tag.contains("db") ? tag.getString("db") : "0";
        sheet.build = tag.getInt("build");

        CompoundTag skillTag = tag.getCompound("skills");
        for (String key : skillTag.getAllKeys()) {
            sheet.skills.put(key, skillTag.getInt(key));
        }
        readStrings(tag.getList("marks", Tag.TAG_STRING), sheet.improvementMarks);
        sheet.tempInsanityEnd = tag.getLong("temp_end");
        sheet.tempInsanity = tag.getString("temp_name");
        sheet.indefiniteInsanity = tag.getBoolean("indefinite");
        sheet.retired = tag.getBoolean("retired");
        readStrings(tag.getList("phobias", Tag.TAG_STRING), sheet.phobias);
        readStrings(tag.getList("manias", Tag.TAG_STRING), sheet.manias);
        sheet.dailySanLoss = tag.getInt("daily_loss");
        sheet.lastSanDay = tag.getLong("last_day");

        ListTag handoutList = tag.getList("handouts", Tag.TAG_COMPOUND);
        for (int i = 0; i < handoutList.size(); i++) {
            sheet.handouts.add(Handout.fromNbt(handoutList.getCompound(i)));
        }

        CompoundTag bg = tag.getCompound("backstory");
        for (String key : BACKSTORY_KEYS) {
            sheet.backstory.put(key, bg.getString(key));
        }
        sheet.notes = tag.getString("notes");
        sheet.dying = tag.getBoolean("dying");
        return sheet;
    }

    private static void readStrings(ListTag list, java.util.Collection<String> target) {
        for (int i = 0; i < list.size(); i++) {
            String value = list.getString(i);
            if (!value.isEmpty()) target.add(value);
        }
    }
}
