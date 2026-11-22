package com.coc.events;

import java.util.ArrayList;
import java.util.List;

/** 剧本 JSON 模型。按 events 顺序逐幕触发。 */
public final class ScenarioDef {
    public String id = "";
    public String title = "";
    public String description = "";
    public String intro = "";
    public String outro = "";
    public String sanReward = "1d10";
    public List<String> events = new ArrayList<>();
    public List<ScenarioAct> acts = new ArrayList<>();
}
