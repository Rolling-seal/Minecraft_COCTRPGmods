package com.coc.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 客户端中英切换：中文原文作为 key，英文翻译来自 assets/coc/lang/coc_en.json。 */
public final class CocLang {
    private static final Type TYPE = new TypeToken<Map<String, String>>() { }.getType();
    private static final Gson GSON = new Gson();
    private static Map<String, String> en;
    private static List<Map.Entry<String, String>> phrases;

    private CocLang() {
    }

    public static boolean english() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getLanguageManager() == null) return false;
        String code = mc.getLanguageManager().getSelected();
        return code != null && code.toLowerCase().startsWith("en");
    }

    /** UI / 剧情文本精确翻译；没有翻译时保留中文。 */
    public static String tr(String zh) {
        if (zh == null || zh.isEmpty() || !english()) return zh;
        ensure();
        return en.containsKey(zh) ? en.get(zh) : zh;
    }

    /** 聊天/系统消息：先精确匹配，再做长短语替换，尽量把动态消息也英文化。 */
    public static String message(String text) {
        if (text == null || text.isEmpty() || !english()) return text;
        ensure();
        String exact = en.get(text);
        if (exact != null && !exact.isEmpty()) return exact;
        String result = text;
        for (Map.Entry<String, String> e : phrases) {
            String key = e.getKey();
            if (key.length() < 2 || e.getValue().isEmpty()) continue;
            if (result.contains(key)) result = result.replace(key, e.getValue());
        }
        return result;
    }

    private static synchronized void ensure() {
        if (en != null) return;
        en = new HashMap<>();
        try (InputStream stream = CocLang.class.getResourceAsStream("/assets/coc/lang/coc_en.json")) {
            if (stream != null) {
                Map<String, String> map = GSON.fromJson(
                        new InputStreamReader(stream, StandardCharsets.UTF_8), TYPE);
                if (map != null) en.putAll(map);
            }
        } catch (Exception ignored) {
        }
        phrases = new ArrayList<>(en.entrySet());
        phrases.sort(Comparator.comparingInt((Map.Entry<String, String> e) -> e.getKey().length()).reversed());
    }
}
