package com.coc.events;

import java.util.ArrayList;
import java.util.List;

/** 剧本幕次元数据：给 KP 面板显示“这一幕发生什么/何时触发”。 */
public final class ScenarioAct {
    public int act;
    public String event = "";
    public String title = "";
    public String summary = "";
    public String trigger = "";
    public List<String> sideEvents = new ArrayList<>();
}
