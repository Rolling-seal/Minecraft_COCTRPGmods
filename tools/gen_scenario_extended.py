# -*- coding: utf-8 -*-
"""生成《黑暗中的低语》十二幕扩展：新增 6 个主幕事件 + 5 个支线事件。
在 gen_events.py 之后运行；不会改动既有随机事件内容。
"""
import json
import os

BASE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "coc")
EVENTS = os.path.join(BASE, "events")
SCENARIOS = os.path.join(BASE, "scenarios")


def e(kind, **kw):
    d = {"type": kind}
    d.update({k: v for k, v in kw.items() if v is not None})
    return d


def out(text, *effects, handout=None, phobia=None, mania=None, next=None):
    d = {"text": text}
    if effects:
        d["effects"] = list(effects)
    if handout:
        d["handout"] = handout
    if phobia:
        d["phobia"] = phobia
    if mania:
        d["mania"] = mania
    if next:
        d["next"] = next
    return d


def opt(text, skill=None, difficulty=None, success=None, failure=None, outcome=None):
    d = {"text": text}
    if skill:
        d["skill"] = skill
        d["difficulty"] = difficulty or "regular"
        if success:
            d["success"] = success
        if failure:
            d["failure"] = failure
    elif outcome:
        d["outcome"] = outcome
    return d


def event(eid, title, desc, options, tags=None):
    return {
        "id": eid,
        "title": title,
        "description": desc,
        "weight": 1,
        "kpOnly": True,
        "once": False,
        "cooldown": 0,
        "tags": tags or ["scenario"],
        "conditions": {},
        "options": options,
    }


NEW_EVENTS = []

# ===== 第三幕：井 =====
NEW_EVENTS.append(event(
    "s_dw_well", "第三幕：井底的回声",
    "井台的石沿被磨得发亮。木桶还在滴水，井绳却像被人从下面拽过，绷得笔直。",
    [
        opt("放下井绳打水", "survival", "regular",
            out("水桶提上来时，里面没有水，只有一只湿透的童鞋、一把小铜钥匙，和一层黑色的海水。",
                e("san", value="-1d3"), e("item", id="minecraft:iron_nugget", count=1),
                handout="井底打捞物：一只童鞋、一把小铜钥匙、黑色的海水。"),
            out("井绳突然绷紧，另一端传来重量。你松手时，井底有什么东西已经爬到了半途。",
                e("san", value="-1d4"), e("spawn", id="minecraft:drowned", count=1))),
        opt("侧耳倾听井底", "listen", "regular",
            out("敲击的节奏和你的心跳完全一致。你数到第七下时，井底传来一声很轻的叹息。",
                e("san", value="-1"), handout="井底的敲击节奏与你的心跳一致。"),
            out("井底有人用你的旧名叫你。你抬头时，井口的光已经变成了海水的颜色。",
                e("san", value="-1d4"))),
        opt("投石问路", outcome=out("石头落水的声音很浅。敲击停了一拍，然后从更近的地方重新响起。",
                                      e("san", value="-1"))),
    ]))

# ===== 第五幕：告解室 =====
NEW_EVENTS.append(event(
    "s_dw_confessional", "第五幕：牧师的告解",
    "告解室的木门半掩着。里面没有神父，只有一件挂着的旧法衣，和一本来不及合上的日记。",
    [
        opt("阅读日记", "library_use", "regular",
            out("日记最后一页写着：『它叫伊奥斯。不要让它忘记自己。』旁边画着一扇门和一个空白的人形。",
                e("san", value="-1d3"), e("mythos", value=1),
                handout="牧师日记：真名『伊奥斯』；不要让它忘记自己；门与空白人形。"),
            out("字迹在你眼前化开，像被水浸过的墨。你只来得及记住一个音节：『伊……』",
                e("san", value="-1d4"), handout="日记残页：一个音节『伊……』")),
        opt("搜查暗格", "spot_hidden", "regular",
            out("告解室地板下有一个小暗格：一把黄铜钥匙、一张画像草图，草图背面写着『烧掉它』。",
                e("san", value="-1d3"), e("item", id="minecraft:paper", count=1),
                handout="暗格：黄铜钥匙、画像草图、背面写着『烧掉它』。"),
            out("你摸到暗格时，黑暗里有一只手也摸到了你的手腕。", e("san", value="-1d4"),
                e("hp", value="-1d2"))),
        opt("听告解室后面", "listen", "regular",
            out("墙后是空的。你听见地窖里的油灯燃烧声，还有很多人同时低声念着同一个词。",
                e("san", value="-1"), handout="告解室墙后是空的，声音通向地窖。"),
            out("有人贴着你的耳朵呼吸。你转身，只看见那件旧法衣在动。", e("san", value="-1d3"))),
    ]))

# ===== 第六幕：墓地 =====
NEW_EVENTS.append(event(
    "s_dw_graveyard", "第六幕：没有名字的坟",
    "教堂后的墓地没有一块碑上写着名字。只有最里面一座新坟前，插着一块木牌，上面是你的旧名。",
    [
        opt("查看自家旧坟", "history", "regular",
            out("碑上的日期停在二十年前。你在旁边的土里找到一张泡烂的全家福，照片里的孩子和你很像。",
                e("san", value="-1d3"),
                handout="旧坟：日期停在二十年前；一张泡烂的全家福；照片里的孩子很像你。"),
            out("坟里是空的。土是温的，像刚有人从里面坐起来。", e("san", value="-1d4"))),
        opt("挖开新坟", "climb", "regular",
            out("棺材里只有一个稻草人，穿着你的旧衣服，脸上贴着你的照片。",
                e("san", value="-1d4"), handout="新坟：稻草人穿着你的旧衣服，贴着你的照片。"),
            out("一只冰冷的手从土里抓住你的脚踝。", e("hp", value="-1d4"), e("san", value="-1d4"))),
        opt("在坟前祈祷", "occult", "regular",
            out("你念完祷词。所有无字碑同时安静下来，像在听。你忽然明白：这些坟里埋的是被献上的名字。",
                e("san", value="1d3"), handout="无字碑下埋的是被献给沉睡者的名字。"),
            out("所有无字碑同时转向你。你听见土下有东西在笑。", e("san", value="-1d6"))),
    ]))

# ===== 第八幕：石台 =====
NEW_EVENTS.append(event(
    "s_dw_slab", "第八幕：石台",
    "石台上的人形穿着你的旧衣服。它的胸口放着那封信，脸是一张空白画布，画布后面露出一角金色画框。",
    [
        opt("检查那封信", "spot_hidden", "regular",
            out("信纸上的字是你的笔迹，日期却是二十年前。信末多了一行你从未写过的话：『别让它想起自己。』",
                e("san", value="-1d3"),
                handout="石台上的信：你的笔迹、二十年前的日期、『别让它想起自己』。"),
            out("信在你手里碎成盐粒。石台上的人形轻轻动了一下。", e("san", value="-1d3"))),
        opt("查看画像", "occult", "regular",
            out("画像里是一个没有脸的人。画框背面刻着真名『伊奥斯』，还有一行小字：『烧掉锚点』。",
                e("san", value="-1d6"), e("mythos", value=1),
                handout="画像：没有脸的人；背面刻着真名『伊奥斯』与『烧掉锚点』。"),
            out("画像里的脸慢慢变成了你的脸。你听见画布后面有人在呼吸。", e("san", value="-1d6"))),
        opt("触碰人形", "medicine", "regular",
            out("它的皮肤是温的，像在发烧。你碰到它的瞬间，看见了一座海底城市和那扇门。",
                e("san", value="-1d4"), handout="人形的梦：海底城市，所有街道通向同一扇门。"),
            out("它抓住你的手腕。它的力气大得不像一个躺着的东西。", e("hp", value="-1d3"),
                e("san", value="-1d6"))),
    ]))

# ===== 第十幕：仪式 =====
NEW_EVENTS.append(event(
    "s_dw_ritual", "第十幕：守梦人",
    "地窖的门被推开。剩下的村民穿着法衣走进来，像没有睡醒一样开始吟唱。石台上的人形随着节拍抬起手。",
    [
        opt("打断吟唱", "fighting_brawl", "regular",
            out("你撞倒领唱的人。吟唱断了，村民像被剪断线的木偶一样倒下，但石台上的人形已经坐了起来。",
                e("san", value="-1d4"), e("hp", value="-1d3"), handout="仪式被打断，但沉睡者已经坐起。"),
            out("他们唱完了最后一个音节。地窖里的油灯同时熄灭。", e("san", value="-1d6"),
                e("spawn", id="minecraft:husk", count=1))),
        opt("混入吟唱", "stealth", "regular",
            out("你压低声音跟着念。最后一句是：『伊奥斯，回到梦里去。』你记住了真名。",
                e("san", value="-1d3"), e("mythos", value=1),
                handout="仪式最后一句：『伊奥斯，回到梦里去。』"),
            out("一个村民转过头，用你的声音继续唱。所有人的脸都朝向你。", e("san", value="-1d6"))),
        opt("躲起来观察", "listen", "regular",
            out("你听见领唱的人说：『只要画像还在，它就醒不了。』他们都在看石台后的画框。",
                e("san", value="-1d3"), handout="守梦人的话：只要画像还在，它就醒不了。"),
            out("有人吹灭了你的灯。黑暗里，吟唱声围了过来。", e("hp", value="-1d4"),
                e("san", value="-1d4"))),
    ]))

# ===== 第十二幕：黎明 =====
NEW_EVENTS.append(event(
    "s_dw_dawn", "第十二幕：黎明",
    "地窖安静下来。你们爬出教堂时，天边刚泛白。村口站着一些人，他们不再躲藏，只是沉默地看着你们。",
    [
        opt("烧掉教堂", outcome=out("火焰从长椅烧到祭坛。你听见地底传来一声很长的叹息，然后村庄第一次安静得像个正常的早晨。",
                                      e("san", value="1d3"), e("mythos", value=1),
                                      handout="结局：教堂烧毁；地底的梦暂时安静了。")),
        opt("封住地窖", "mech_repair", "regular",
            out("你用木板和石块封死地窖入口。最后一锤落下时，门后传来一声轻轻的敲门声，像是告别。",
                e("san", value="1d6"), handout="结局：地窖被封死；门后传来一声告别般的敲击。"),
            out("封口没有完全合拢。你离开时，身后仍有一线海水从石缝里渗出来。",
                e("san", value="-1d3"), handout="结局：地窖只封住了一半；海水仍在渗出。")),
        opt("带走日记与画像", outcome=out("你带走日记和烧焦的画框。离开村口时，照片里的孩子似乎眨了一下眼。",
                                            e("san", value="1d3"),
                                            handout="结局：带走日记与画像；照片里的孩子眨了一下眼。")),
        opt("直接离开", outcome=out("你没有回头。走出很远后，你才发现口袋里多了一小撮湿沙。",
                                      e("san", value="1d3"),
                                      handout="结局：离开村庄；口袋里多了一小撮湿沙。")),
    ]))

# ===== 支线事件 =====
NEW_EVENTS.append(event(
    "s_dw_side_child", "支线：窗后的孩子",
    "一扇窗开了一条缝。一个七八岁的孩子把手指放在嘴唇上，示意你过去。",
    [
        opt("问他看到了什么", "charm", "regular",
            out("孩子说：『他们每天晚上都去教堂做梦，醒来就忘了。你别去。』",
                e("san", value="-1"), handout="孩子的话：村民每晚去教堂做梦，醒来就忘。"),
            out("孩子突然尖叫，窗户被从里面猛地关上。", e("san", value="-1d3"))),
        opt("给他一点食物", "persuade", "regular",
            out("孩子收下食物，从窗缝塞给你一张蜡笔画：一个没有脸的人站在画框里。",
                e("san", value="-1"), e("item", id="minecraft:paper", count=1),
                handout="孩子的蜡笔画：没有脸的人站在画框里。"),
            out("孩子盯着你的口袋，说：『你也有那个名字。』然后关上了窗。", e("san", value="-1d3"))),
    ], tags=["scenario", "side"]))

NEW_EVENTS.append(event(
    "s_dw_side_well_rope", "支线：下井",
    "你们把井绳系在腰上，顺着湿滑的井壁往下。水面比想象中深，井壁上全是螺旋。",
    [
        opt("继续下潜", "swim", "regular",
            out("水下有一扇被海草缠住的木门。门缝里卡着一枚铜钥匙和一片孩子的指甲。",
                e("san", value="-1d4"), e("item", id="minecraft:iron_nugget", count=1),
                handout="井底：一扇被海草缠住的木门；铜钥匙；一片孩子的指甲。"),
            out("有什么东西从水下握住了你的脚踝。你被拖下去时，看见井壁上的螺旋在转动。",
                e("hp", value="-1d4"), e("san", value="-1d6"))),
        opt("立刻上井", "climb", "regular",
            out("你爬出井口时，井绳在身后断了。井底传来一声失望的叹息。", e("san", value="-1d3")),
            out("井绳被从下面割断。你抓住井沿时，脚踝上多了一圈青紫的手印。",
                e("hp", value="-1d6"), e("san", value="-1d4"))),
    ], tags=["scenario", "side"]))

NEW_EVENTS.append(event(
    "s_dw_side_priest_room", "支线：牧师的房间",
    "牧师住宅的门没有锁。桌上摆着两副餐具，好像主人刚离开。壁炉里的灰还是温的。",
    [
        opt("搜查书房", "spot_hidden", "regular",
            out("书桌抽屉里有一本账册：二十年来，村里每个新生儿都被记下名字，旁边写着『献给梦』。",
                e("san", value="-1d3"), handout="账册：二十年来每个新生儿都被记下名字，标注『献给梦』。"),
            out("你拉开抽屉，里面是一撮黑色的草茎和一只干枯的婴儿鞋。", e("san", value="-1d4"))),
        opt("读墙上的信", "library_use", "regular",
            out("墙上钉着牧师的最后一封信：『它不是神。它只是一直在做梦。别让它醒。』",
                e("san", value="-1d3"), handout="牧师最后的话：它不是神，它只是一直在做梦；别让它醒。"),
            out("墙上的字在你读完后重新排列成一句话：『你回来了。』", e("san", value="-1d4"))),
    ], tags=["scenario", "side"]))

NEW_EVENTS.append(event(
    "s_dw_side_night", "支线：夜宿村庄",
    "你们决定在村里过夜。午夜，所有窗户同时亮起，又同时熄灭。",
    [
        opt("守夜", "listen", "regular",
            out("你听见村民们赤脚走过街道，朝教堂走去。他们边走边低声念着同一个名字：『伊奥斯。』",
                e("san", value="-1d3"), handout="午夜：村民赤脚走向教堂，低声念着『伊奥斯』。"),
            out("你打了一个盹。醒来时，你站在教堂门口，手里握着一根不属于你的蜡烛。",
                e("san", value="-1d6"))),
        opt("睡觉", outcome=out("梦里，你站在海底城市的门口。门后有人用你的声音说：『回来。』",
                                  e("san", value="-1d6"), e("mythos", value=1),
                                  handout="夜梦：海底城市的门；门后用你的声音说『回来』。")),
    ], tags=["scenario", "side"]))

NEW_EVENTS.append(event(
    "s_dw_side_portrait", "支线：取走画像",
    "你伸手去取石台后的画像。画框是温的，像刚从火边拿下来。画布上那张没有脸的脸，正对着你。",
    [
        opt("取下画像", "occult", "regular",
            out("画框背面刻着真名『伊奥斯』，还有一行小字：『烧掉锚点，它就会回到梦里。』",
                e("san", value="-1d4"), handout="画像背面：真名『伊奥斯』；『烧掉锚点，它就会回到梦里』。"),
            out("你碰到画框时，画布上慢慢浮现出你的脸。你猛地松手，画像却没有掉下去。",
                e("san", value="-1d6"))),
        opt("当场烧掉", "throw", "regular",
            out("画像烧起来时发出尖叫声。石台上的人形僵住了一瞬，但火很快被地窖的湿气压灭。",
                e("san", value="-1d6"), handout="画像被烧毁一部分；人形僵住了一瞬，但火熄灭了。"),
            out("火把刚靠近，火焰就自己缩了回去。画布里的脸笑了。", e("san", value="-1d4"))),
    ], tags=["scenario", "side"]))

for d in NEW_EVENTS:
    fn = d["id"] + ".json"
    with open(os.path.join(EVENTS, fn), "w", encoding="utf-8") as f:
        json.dump(d, f, ensure_ascii=False, indent=2)
        f.write("\n")

# update event index
index_path = os.path.join(EVENTS, "index.json")
index = json.load(open(index_path, encoding="utf-8"))
names = index.setdefault("events", [])
for d in NEW_EVENTS:
    fn = d["id"] + ".json"
    if fn not in names:
        names.append(fn)
json.dump(index, open(index_path, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
open(index_path, "a", encoding="utf-8").write("\n")

# renumber titles of existing six events
renumber = {
    "s_dark_wispers_prologue": "第一幕：没有署名的信",
    "s_dark_wispers_village": "第二幕：沉默的村庄",
    "s_dark_wispers_church": "第四幕：教堂",
    "s_dark_wispers_cellar": "第七幕：地下",
    "s_dark_wispers_dream": "第九幕：梦",
    "s_dark_wispers_finale": "第十一幕：真名",
}
for eid, title in renumber.items():
    p = os.path.join(EVENTS, eid + ".json")
    d = json.load(open(p, encoding="utf-8"))
    d["title"] = title
    json.dump(d, open(p, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    open(p, "a", encoding="utf-8").write("\n")

# scenario with 12 acts metadata
scenario = {
    "id": "dark_whispers",
    "title": "黑暗中的低语",
    "description": "一封没有署名的信把你带回故乡。村庄沉默，教堂地窖里有东西在敲墙。",
    "intro": "你收到一封没有署名的信。信纸上有海水干涸后的盐渍，正文只有一句：『回村里来，它又开始做梦了。』\n\n（KP 提示：本剧本共十二幕。请按玩家行动推进；不要连续点“下一幕”，也不要用“自动结算”代替玩家的选择。）",
    "outro": "地窖重新安静下来。你走出教堂时，天已经亮了。村口有人向你点头，但没有人和你说话。\n\n你低头看信，纸上的字迹已经消失，只剩下一片盐渍。",
    "sanReward": "1d10",
    "events": [
        "s_dark_wispers_prologue",
        "s_dark_wispers_village",
        "s_dw_well",
        "s_dark_wispers_church",
        "s_dw_confessional",
        "s_dw_graveyard",
        "s_dark_wispers_cellar",
        "s_dw_slab",
        "s_dark_wispers_dream",
        "s_dw_ritual",
        "s_dark_wispers_finale",
        "s_dw_dawn",
    ],
    "acts": [
        {"act": 1, "event": "s_dark_wispers_prologue", "title": "没有署名的信",
         "summary": "收到无署名信，决定回村；确认旧名与回村动机。",
         "trigger": "开场读信；玩家检查信封或决定出发时。", "sideEvents": []},
        {"act": 2, "event": "s_dark_wispers_village", "title": "沉默的村庄",
         "summary": "村口无人、窗后有人影；获得“教堂地下有东西”的线索。",
         "trigger": "玩家进入村庄、询问村民、查看井或前往教堂时。",
         "sideEvents": ["s_dw_side_child"]},
        {"act": 3, "event": "s_dw_well", "title": "井底的回声",
         "summary": "井壁螺旋、井底敲击；可捞到童鞋与铜钥匙。",
         "trigger": "玩家调查井、放下井绳或倾听井底时。",
         "sideEvents": ["s_dw_side_well_rope"]},
        {"act": 4, "event": "s_dark_wispers_church", "title": "教堂",
         "summary": "长椅上坐满“睡着的人”；祭坛铭文/告解室/祈祷。",
         "trigger": "玩家进入教堂并开始调查时。",
         "sideEvents": ["s_dw_side_priest_room"]},
        {"act": 5, "event": "s_dw_confessional", "title": "牧师的告解",
         "summary": "日记揭示仪式与真名“伊奥斯”；暗格里有画像草图。",
         "trigger": "玩家搜查告解室、阅读日记或寻找暗格时。",
         "sideEvents": ["s_dw_side_priest_room"]},
        {"act": 6, "event": "s_dw_graveyard", "title": "没有名字的坟",
         "summary": "发现自家旧坟与新坟草人；村庄献祭名字的真相。",
         "trigger": "玩家去墓地、找自家坟或挖开新坟时。",
         "sideEvents": ["s_dw_side_night"]},
        {"act": 7, "event": "s_dark_wispers_cellar", "title": "地下",
         "summary": "掀开祭坛地板；潜行、火把或正面闯入地窖。",
         "trigger": "玩家掀开地板、进入地窖时。", "sideEvents": []},
        {"act": 8, "event": "s_dw_slab", "title": "石台",
         "summary": "人形穿着你的旧衣服；信件、画像锚点与真名。",
         "trigger": "玩家检查石台人形、信件或画像时。",
         "sideEvents": ["s_dw_side_portrait"]},
        {"act": 9, "event": "s_dark_wispers_dream", "title": "梦",
         "summary": "海底城市与门；一次 SAN；带回门/真名的线索。",
         "trigger": "玩家在地窖失去意识、入睡或被拖入梦时。", "sideEvents": []},
        {"act": 10, "event": "s_dw_ritual", "title": "守梦人",
         "summary": "村民举行仪式；打断、混入或躲藏观察。",
         "trigger": "玩家醒来、地窖来人、仪式开始时。", "sideEvents": []},
        {"act": 11, "event": "s_dark_wispers_finale", "title": "真名",
         "summary": "人形坐起；念真名、烧画像或逃跑。",
         "trigger": "仪式被阻止或玩家正面面对人形时。", "sideEvents": []},
        {"act": 12, "event": "s_dw_dawn", "title": "黎明",
         "summary": "烧教堂、封地窖、带走证据或离开；决定村子的结局。",
         "trigger": "终幕结束后，玩家处理现场、准备离开时。",
         "sideEvents": ["s_dw_side_portrait"]},
    ],
}
with open(os.path.join(SCENARIOS, "dark_whispers.json"), "w", encoding="utf-8") as f:
    json.dump(scenario, f, ensure_ascii=False, indent=2)
    f.write("\n")

print("extended scenario:", len(scenario["events"]), "acts,", len(NEW_EVENTS), "new events")
