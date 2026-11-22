# -*- coding: utf-8 -*-
"""生成内置事件包与示例剧本。仅用于本项目内容创作，输出到 assets/coc。"""
import json
import os

BASE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "coc")
EVENTS = os.path.join(BASE, "events")
SCENARIOS = os.path.join(BASE, "scenarios")
os.makedirs(EVENTS, exist_ok=True)
os.makedirs(SCENARIOS, exist_ok=True)


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


def opt(text, skill=None, difficulty=None, success=None, failure=None, bonus=0, outcome=None):
    d = {"text": text}
    if skill:
        d["skill"] = skill
        d["difficulty"] = difficulty or "regular"
        if bonus:
            d["bonus"] = bonus
        if success:
            d["success"] = success
        if failure:
            d["failure"] = failure
    elif outcome:
        d["outcome"] = outcome
    return d


def event(eid, title, desc, options, weight=10, tags=None, conditions=None,
          kp_only=False, once=False, cooldown=0):
    d = {
        "id": eid,
        "title": title,
        "description": desc,
        "weight": weight,
        "kpOnly": kp_only,
        "once": once,
        "cooldown": cooldown,
        "tags": tags or [],
        "conditions": conditions or {},
        "options": options,
    }
    return d


ALL = []

# 1
ALL.append(event(
    "whispering_fog", "雾中低语",
    "浓雾毫无征兆地从林间涌出。雾里有细碎的说话声，像许多人在同时低声念着你的名字。",
    [
        opt("凝神倾听", "listen", "regular",
            out("你听清了几个词：『门』、『潮汐』、『它还醒着』。", e("san", value="-1d3"),
                handout="雾中的词句：门、潮汐、它还醒着。"),
            out("声音突然贴到耳边。你猛地回头，只看见自己的影子。", e("san", value="-1d6"),
                e("effect", id="minecraft:nausea", duration=200))),
        opt("循声走进雾中", "navigate", "regular",
            out("你在雾里走了很久，最后在一块湿石头上发现一枚冰凉的碎片。",
                e("san", value="-1"), e("item", id="minecraft:amethyst_shard", count=1),
                handout="雾散后，你手里多了一枚不属于任何已知矿物的碎片。"),
            out("雾里有什么东西跟着你。你听见了蹄声。", e("san", value="-1d6"),
                e("spawn", id="minecraft:husk", count=1))),
        opt("捂住耳朵快步离开", outcome=out("你走出雾区，但耳膜里仍残留着低频的震动。", e("san", value="-1"))),
    ], weight=20, tags=["fog", "voice"]))

# 2
ALL.append(event(
    "old_monolith", "古老石碑",
    "林间空地上立着一块黑色石碑，表面刻满不属于任何人类文字的螺旋。苔藓在它脚下长成环状。",
    [
        opt("辨认碑文", "occult", "regular",
            out("你的目光顺着螺旋滑下去，某种比语言更古老的东西在你脑中短暂地睁开了一只眼。",
                e("mythos", value=1), e("san", value="-1d4"),
                handout="石碑铭文：『当群星归位，沉睡者将不再梦见自己。』"),
            out("碑文在你眼中开始蠕动。你踉跄后退，鼻腔里满是海水味。", e("san", value="-1d6"),
                e("effect", id="minecraft:nausea", duration=300))),
        opt("拓印碑文", "art_craft", "regular",
            out("拓印成功。纸上留下清晰的螺旋，但边缘似乎多出了几道原本不存在的笔画。",
                e("item", id="minecraft:paper", count=3), e("san", value="-1d3"),
                handout="一份可疑的石碑拓片。"),
            out("纸在拓印过程中自行裂开，像被什么东西从内侧划破。", e("san", value="-1d3"))),
        opt("绕开石碑", outcome=out("你远远绕开。走出很远后，你才发现自己一直在数着脚步，仿佛在配合某种节奏。",
                                     e("san", value="-1"))),
    ], weight=15, tags=["monolith", "mythos"]))

# 3
ALL.append(event(
    "missing_villager", "失踪的村民",
    "村口的木门上钉着一张寻人启事。照片里的男人在微笑，但照片背面写着日期——三年前。",
    [
        opt("追踪足迹", "track", "regular",
            out("你在一处废弃矿井旁找到他的工具袋，里面有一块刻着螺旋的骨头。",
                e("item", id="minecraft:bone", count=1), e("san", value="-1d3"),
                handout="失踪者的工具袋：一把锈锤、一块刻着螺旋的骨头。"),
            out("足迹在一面墙前消失。墙后传来指甲刮擦的声音。", e("san", value="-1d4"),
                e("spawn", id="minecraft:zombie", count=1))),
        opt("询问村民", "persuade", "regular",
            out("一个老妇人告诉你：『他每晚都回来，但我们不敢开门。』", e("san", value="-1"),
                handout="老妇人的话：他每晚都回来，但我们不敢开门。"),
            out("所有村民同时停下手中的活，盯着你，直到你离开。", e("san", value="-1d4"))),
        opt("不介入", outcome=out("你撕下寻人启事，把它塞进口袋。照片上的男人仍在微笑。", e("san", value="-1"))),
    ], weight=15, tags=["village", "missing"]))

# 4
ALL.append(event(
    "dream_city", "梦中之城",
    "你闭上眼，却看见一座沉在海底的城市。街道以不可能的角度折叠，某个巨大的影子正在城市中央翻身。",
    [
        opt("记住城市布局", "navigate", "hard",
            out("你记住了三条街与一座塔的位置。醒来后，这些图案仍清晰地烙在视网膜上。",
                e("mythos", value=1), e("san", value="-1d6"),
                handout="梦中的城市布局：三条倾斜的街道，一座没有门的塔。"),
            out("街道在你注视下重组。你听见自己的名字从海底传来。", e("san", value="-1d10"),
                e("effect", id="minecraft:darkness", duration=200))),
        opt("强迫自己醒来", "psychoanalysis", "regular",
            out("你用疼痛把自己拽回现实。枕边有一层湿漉漉的盐霜。", e("san", value="-1d3")),
            out("你醒不过来。梦里的城市开始下雨，雨滴是温的。", e("san", value="-1d6"),
                e("effect", id="minecraft:nausea", duration=300))),
        opt("沉溺其中", outcome=out("你放任自己沉下去。醒来时，嘴角带着海水味，记忆里多了一段不属于你的童年。",
                                      e("san", value="-1d8"), e("mythos", value=1))),
    ], weight=10, tags=["dream", "city"], conditions={"daytime": "night"}))

# 5
ALL.append(event(
    "deep_one_tracks", "深潜者的足迹",
    "河岸泥地上出现了一串巨大的蹼状脚印，从水里一直延伸到芦苇深处。空气里有股腥咸的潮气。",
    [
        opt("采集黏液", "science", "regular",
            out("你刮下一小瓶黏液。它在瓶壁内侧缓慢地逆着重力爬行。",
                e("item", id="minecraft:slime_ball", count=2), e("san", value="-1d3"),
                handout="深潜者黏液样本：具有逆重力运动特征。"),
            out("黏液突然沸腾，溅到你的手背。皮肤下有什么东西短暂地鼓起。", e("san", value="-1d4"),
                e("effect", id="minecraft:nausea", duration=200))),
        opt("沿足迹追踪", "track", "hard",
            out("你在芦苇深处发现一块被啃干净的鱼骨，上面刻着与石碑相同的螺旋。",
                e("san", value="-1d3"), e("item", id="minecraft:bone", count=1),
                handout="鱼骨上的螺旋与石碑铭文一致。"),
            out("水面上浮起一双没有眼白的眼睛。", e("san", value="-1d4"),
                e("spawn", id="minecraft:drowned", count=1))),
        opt("离开河岸", outcome=out("你退回安全距离。直到天黑，你都没敢喝水。", e("san", value="-1"))),
    ], weight=10, tags=["deep_one", "water"]))

# 6
ALL.append(event(
    "mad_painter", "疯狂的画家",
    "一间废弃画室里挂满了同一张脸。画中人没有五官，但每一幅都在用不同的姿势看着门口。",
    [
        opt("与他交谈", "psychology", "regular",
            out("画家说他只是『替它画像』。他求你带走最后一幅，别再让它看见。", e("san", value="-1"),
                handout="画家的话：我只是替它画像。它一直在看。"),
            out("画家突然开始用画笔戳自己的眼睛。你退出画室时，听见画布后面有人在笑。",
                e("san", value="-1d6"), e("effect", id="minecraft:nausea", duration=300))),
        opt("查看画作", "spot_hidden", "regular",
            out("你在画布背面发现一行小字：『她住在镜子里』。", e("mythos", value=1),
                e("san", value="-1d4"), handout="画布背面的字：她住在镜子里。"),
            out("画中那张没有五官的脸，在你注视时慢慢转向了你。", e("san", value="-1d6"),
                e("effect", id="minecraft:blindness", duration=100))),
        opt("买下最后一幅画", outcome=out("画家只收了一枚硬币。画框背面刻着你的名字。",
                                          e("item", id="minecraft:painting", count=1),
                                          e("san", value="-1d3"))),
    ], weight=12, tags=["artist", "mirror"]))

# 7
ALL.append(event(
    "forbidden_book", "禁书",
    "一本没有封面的书躺在旧书架上。书页之间夹着一根干枯的触须，它在你靠近时微微卷曲。",
    [
        opt("阅读", "occult", "hard",
            out("你读完了一章，理解了某种不该被理解的几何关系。", e("mythos", value=1),
                e("san", value="-1d6"), handout="禁书残章：关于角度与饥饿的论述。"),
            out("文字在你眼前重排成一张嘴。你合上书，发现书页已经少了一页。", e("san", value="-1d10"),
                e("effect", id="minecraft:blindness", duration=200))),
        opt("烧掉它", outcome=out("火焰是蓝色的。烧完后，灰烬在地上拼出一个笑脸。",
                                   e("san", value="-1d3"))),
        opt("带走", outcome=out("你把书塞进背包。它比看上去重得多，像装着水。",
                                e("item", id="minecraft:book", count=1), e("san", value="-1"))),
    ], weight=8, tags=["book", "mythos"]))

# 8
ALL.append(event(
    "graveyard_night", "墓园之夜",
    "墓园的土是新的，但没有一座坟前有名字。远处有人在挖坑，铁锹声停了一下，又继续。",
    [
        opt("守夜", "listen", "regular",
            out("你听见土里传来心跳。节奏和你的一模一样。", e("san", value="-1"),
                handout="墓园笔记：土里的心跳与我同步。"),
            out("铁锹声停了。有什么东西从背后拍了拍你的肩。", e("san", value="-1d6"),
                e("spawn", id="minecraft:zombie", count=1))),
        opt("挖开一座新坟", "climb", "regular",
            out("棺材里只有一具穿着你衣服的稻草人。", e("san", value="-1d3"),
                e("item", id="minecraft:bone", count=2)),
            out("你挖到一半，土里伸出一只手抓住了你的脚踝。", e("san", value="-1d6"),
                e("spawn", id="minecraft:husk", count=1))),
        opt("祈祷", "occult", "regular",
            out("你念完祷词，土里的心跳慢了半拍。", e("san", value="1d3")),
            out("你的祷词被另一个声音接了下去。它念得比你熟练。", e("san", value="-1d3"))),
    ], weight=10, tags=["graveyard", "night"], conditions={"daytime": "night"}))

# 9
ALL.append(event(
    "radio_static", "收音机杂音",
    "一台老收音机自行开机。杂音里有人用你的声音播报一场尚未发生的葬礼。",
    [
        opt("调频", "elec_repair", "regular",
            out("你调到一个清晰频道。广播念出了一串坐标，位于你脚下。", e("san", value="-1"),
                handout="广播坐标：就在你脚下。"),
            out("收音机外壳变得滚烫，杂音里有人在倒数。", e("san", value="-1d3"),
                e("effect", id="minecraft:nausea", duration=200))),
        opt("关闭收音机", outcome=out("你拔掉电源，它仍在播放。你只好把音量调到最小。", e("san", value="-1"))),
        opt("记录杂音", "science", "regular",
            out("你录下了三段重复的摩尔斯码，译出来是：『别回头』。", e("san", value="-1d3"),
                e("item", id="minecraft:paper", count=1), handout="杂音译码：别回头。"),
            out("录音在回放时多了一个人的呼吸声。", e("san", value="-1d3"))),
    ], weight=10, tags=["radio", "voice"]))

# 10
ALL.append(event(
    "mirror_reflection", "镜中倒影",
    "镜子里的你比你慢了一拍。你抬手，它先抬手；你放下，它仍举着。",
    [
        opt("凝视镜中", "psychology", "hard",
            out("倒影终于与你同步。它在镜面上哈了一口气，写下：『放我出去』。", e("san", value="-1d3"),
                handout="镜面的字：放我出去。"),
            out("倒影伸出手，穿过了镜面。你后退时撞翻了椅子。", e("san", value="-1d6"),
                e("effect", id="minecraft:blindness", duration=100))),
        opt("打碎镜子", outcome=out("镜子碎成七片。每一片里都有一个没有离开的你。", e("san", value="-1"),
                                          e("hp", value="-1d3"))),
        opt("转身离开", outcome=out("你背对镜子。身后传来轻轻的敲击声，三下，停顿，三下。", e("san", value="-1"))),
    ], weight=10, tags=["mirror"]))

# 11
ALL.append(event(
    "blood_moon", "血月",
    "月亮变成了暗红色。村里的狗同时停止吠叫，所有影子都朝向同一个方向。",
    [
        opt("观察天象", "occult", "regular",
            out("你看懂了血色云层的走向——那不是气象，是一张正在合拢的嘴。", e("mythos", value=1),
                e("san", value="-1d6"), handout="血月笔记：云层是一张正在合拢的嘴。"),
            out("你盯得太久，月亮里似乎有一只瞳孔在收缩。", e("san", value="-1d8"),
                e("effect", id="minecraft:darkness", duration=300))),
        opt("躲进屋里", outcome=out("你关紧门窗。整夜都有什么东西在屋顶上踱步。", e("san", value="-1"))),
        opt("向血月祈祷", "occult", "hard",
            out("祈祷得到了回应。一道温热的视线落在你后颈上。", e("san", value="-1d4"),
                e("mythos", value=1)),
            out("你念错了音节。屋顶上的脚步声停在你正上方。", e("san", value="-1d10"),
                e("spawn", id="minecraft:zombie", count=1))),
    ], weight=6, tags=["blood_moon", "night"], conditions={"daytime": "night"}))

# 12
ALL.append(event(
    "black_goat", "黑山羊幼崽",
    "灌木丛里站着一只没有脸的黑山羊。它的蹄子踩过的地方，草叶全部朝同一个方向倒伏。",
    [
        opt("逃跑", "dodge", "regular",
            out("你跑出很远才敢回头。山羊仍站在原地，但它的角变长了一截。", e("san", value="-1d3")),
            out("你的脚踝被什么东西绊住。低头时，你看见草叶正缠绕上来。", e("hp", value="-1d6"),
                e("san", value="-1d6"))),
        opt("战斗", "fighting_brawl", "hard",
            out("你打断了它的一根角。断口里流出的不是血，而是黑色的草籽。", e("san", value="-1d3"),
                e("item", id="minecraft:bone", count=1)),
            out("它的蹄子踢中了你的肋骨。你听见自己体内有什么东西裂开。", e("hp", value="-1d8"))),
        opt("献上祭品", outcome=out("你放下随身携带的食物。山羊低下头，用没有脸的一面『看』了你很久。",
                                     e("san", value="-1d4"), e("mythos", value=1))),
    ], weight=6, tags=["goat", "mythos"]))

# 13
ALL.append(event(
    "nyarlathotep_herald", "奈亚拉托提普的使者",
    "一个穿着旧式西装的男人站在路口。他的影子有太多关节，笑起来时牙齿数量不对。",
    [
        opt("与他交涉", "persuade", "hard",
            out("他告诉你一个名字，并说：『在正确的时刻念出它。』", e("san", value="-1d6"),
                handout="使者给的名字：阿撒托斯之下的一个音节。"),
            out("他歪了歪头，你的舌头突然不受控制地说出一串陌生语言。", e("san", value="-1d10"),
                e("mythos", value=1))),
        opt("拒绝他", outcome=out("他礼貌地让开路。你走出十步后，发现自己的影子少了一只手。",
                                    e("san", value="-1d6"), e("spawn", id="minecraft:enderman", count=1))),
        opt("注视他的脸", outcome=out("你看见了他的脸。之后你再也无法准确回忆起自己的脸。",
                                        e("san", value="-1d10"), e("mythos", value=1),
                                        e("effect", id="minecraft:blindness", duration=200))),
    ], weight=3, tags=["nyarlathotep", "mythos"]))

# 14
ALL.append(event(
    "star_spawn", "星之眷族",
    "夜空裂开一道缝。某个由触须和光组成的庞大轮廓从裂缝里垂下来，像在钓什么。",
    [
        opt("躲藏", "stealth", "hard",
            out("它没有发现你。你听见它用某种类似鲸歌的声音叹了口气。", e("san", value="-1d3")),
            out("它的触须擦过你的藏身处。你闻到了星际尘埃的味道。", e("san", value="-1d8"),
                e("spawn", id="minecraft:enderman", count=1))),
        opt("观察它", "occult", "regular",
            out("你数清了它的一条触须上有十三个关节。这个数字让你安心，也让你恐惧。",
                e("mythos", value=1), e("san", value="-1d6"),
                handout="星之眷族观察记录：十三节触须。"),
            out("它忽然转向你所在的方向。你听见自己的名字从天上落下来。", e("san", value="-1d10"))),
        opt("逃跑", outcome=out("你跑进树林。头顶的裂缝合拢时，整片天空发出湿纸撕裂的声音。",
                                  e("san", value="-1d3"))),
    ], weight=4, tags=["star_spawn", "mythos"]))

# 15
ALL.append(event(
    "cult_ritual", "邪教仪式",
    "谷仓里传出有节奏的吟唱。门缝下渗出蓝光，空气里漂着盐和铁锈的味道。",
    [
        opt("潜入", "stealth", "regular",
            out("你躲在草垛后，看清了祭坛上的符号与一本摊开的笔记。", e("san", value="-1d3"),
                handout="祭坛符号：与石碑上的螺旋同源。"),
            out("一块松动的木板在你脚下断裂。吟唱停了。", e("san", value="-1d6"),
                e("spawn", id="minecraft:zombie", count=1))),
        opt("打断仪式", "firearms_handgun", "regular",
            out("枪声打断了吟唱。教徒四散奔逃，祭坛上只留下一枚温热的硬币。", e("san", value="-1d4"),
                e("item", id="minecraft:gold_nugget", count=3)),
            out("子弹打在祭坛上，弹孔里流出海水。教徒们同时转头看向你。", e("hp", value="-1d6"),
                e("san", value="-1d6"))),
        opt("加入吟唱", outcome=out("你跟着念了三个音节。第四个音节不需要你念，它自己从你喉咙里长了出来。",
                                     e("mythos", value=1), e("san", value="-1d10"))),
    ], weight=8, tags=["cult", "ritual"]))

# 16
ALL.append(event(
    "strange_phone", "陌生来电",
    "你的手机响了。来电显示是你自己的号码。接听键自己在屏幕上微微发亮。",
    [
        opt("接听", "listen", "regular",
            out("电话那头是你自己的声音：『不要相信镜子里的那个。』然后是一阵忙音。", e("san", value="-1d3"),
                handout="电话留言：不要相信镜子里的那个。"),
            out("电话那头只有呼吸声。呼吸的节奏越来越快，最后变成了你的心跳。", e("san", value="-1d6"),
                e("effect", id="minecraft:nausea", duration=200))),
        opt("挂断", outcome=out("你按下挂断。手机立刻又响了，还是你自己的号码。", e("san", value="-1"))),
        opt("回拨", "occult", "regular",
            out("接电话的是一个老妇人。她说：『你终于打回来了。』然后念出一串坐标。", e("san", value="-1d3"),
                handout="回拨得到的坐标：一处废弃教堂。"),
            out("电话接通了，但对面是海底。你听见气泡和某种巨大的翻身声。", e("san", value="-1d6"))),
    ], weight=8, tags=["phone", "voice"]))

# 17
ALL.append(event(
    "faceless_photo", "无脸照片",
    "桌上多了一张照片。照片里是你和一群陌生人，所有人的脸都被仔细地刮掉了。",
    [
        opt("检查照片", "spot_hidden", "regular",
            out("照片背面有你的笔迹：『第三个是我。』", e("san", value="-1d3"),
                handout="照片背面的字：第三个是我。"),
            out("你数了数照片里的人数。数到第三个时，照片上的空白处出现了眼睛。", e("san", value="-1d6"))),
        opt("烧毁照片", outcome=out("照片烧起来时发出尖叫声。灰烬落在地上，拼成一个箭头。", e("san", value="-1d3"))),
        opt("收进背包", outcome=out("你把照片收好。从那天起，你每次照镜子都觉得照片里多了一个人。",
                                      e("item", id="minecraft:paper", count=1), e("san", value="-1"))),
    ], weight=10, tags=["photo"]))

# 18
ALL.append(event(
    "underground_hum", "地底嗡鸣",
    "地面下传来持续的低频嗡鸣。你脚下的石子随着某种节拍轻轻跳动。",
    [
        opt("挖掘", "climb", "regular",
            out("你挖到一块紫色的晶体碎片。嗡鸣在指尖碰到它时停了半秒。", e("san", value="-1d3"),
                e("item", id="minecraft:amethyst_shard", count=1)),
            out("镐头打在某种柔软的东西上。一股温热的气流从裂缝里喷出来。", e("hp", value="-1d4"),
                e("san", value="-1d4"))),
        opt("贴近地面倾听", "listen", "hard",
            out("你听懂了嗡鸣的节奏：它在数数。数到你的年龄时停住了。", e("san", value="-1d6"),
                e("mythos", value=1), handout="地底嗡鸣在数数，数到你的年龄时停住。"),
            out("嗡鸣突然变成尖啸。你的耳朵流出一缕温热的液体。", e("san", value="-1d6"),
                e("effect", id="minecraft:nausea", duration=300))),
        opt("离开", outcome=out("你走出很远，脚底仍残留着那种节拍。", e("san", value="-1"))),
    ], weight=10, tags=["underground", "sound"]))

# 19
ALL.append(event(
    "lost_child", "迷路的孩子",
    "一个穿旧外套的孩子站在路灯下。他说自己找不到家，但你注意到他没有影子。",
    [
        opt("送他回家", "navigate", "regular",
            out("你按他指的路走到一栋空屋。门开后，屋里只有一面镜子，镜中是你小时候的房间。",
                e("san", value="1d3"), handout="空屋的镜子：映出你童年的房间。"),
            out("路越走越偏。孩子在你身后轻轻哼歌，歌词是你的名字。", e("san", value="-1d4"))),
        opt("追问来历", "psychology", "regular",
            out("孩子说：『我在等上一个送我回家的人。』", e("san", value="-1"),
                handout="孩子的话：我在等上一个送我回家的人。"),
            out("孩子的脸在你注视下模糊起来，像被水泡过的照片。", e("san", value="-1d6"))),
        opt("置之不理", outcome=out("你继续走。身后传来脚步声，一直跟到你家门口才停。", e("san", value="-1"))),
    ], weight=12, tags=["child", "ghost"]))

# 20
ALL.append(event(
    "mushroom_ring", "蘑菇圈",
    "草地上长出一圈完美的白色蘑菇。圈内的草比外面矮一截，像被什么压过。",
    [
        opt("踏入圈中", "occult", "regular",
            out("你站在圈里，听见远处传来宴会的声音。离开时，口袋里多了一块冷掉的蛋糕。",
                e("san", value="-1d6"), e("mythos", value=1),
                handout="蘑菇圈：圈内能听见不属于此处的宴会声。"),
            out("圈内的地面突然变软。你低头，看见无数细小的菌丝正沿着鞋底往上爬。",
                e("san", value="-1d8"), e("effect", id="minecraft:nausea", duration=300))),
        opt("观察", "natural_world", "regular",
            out("你发现蘑菇的排列与某张星图一致。", e("san", value="-1d3"),
                handout="蘑菇圈对应一张陌生星图。"),
            out("蘑菇在你注视下缓慢转向，全部朝向你的影子。", e("san", value="-1d3"))),
        opt("绕开", outcome=out("你绕开蘑菇圈。走出几步后，听见圈里有人礼貌地鼓了鼓掌。", e("san", value="-1"))),
    ], weight=12, tags=["mushroom", "fae"]))

# 21
ALL.append(event(
    "tide_of_worms", "蠕虫潮",
    "地面开始起伏。成千上万条灰白色蠕虫从土里钻出来，朝同一个方向缓慢迁移。",
    [
        opt("逃跑", "dodge", "regular",
            out("你踩着石块跳过虫潮。它们没有追你，只是继续朝海边前进。", e("san", value="-1d3")),
            out("你摔进虫堆。它们没有咬你，只是从你的衣领里爬过，留下黏滑的痕迹。",
                e("hp", value="-1d4"), e("san", value="-1d4"))),
        opt("踩出一条路", "fighting_brawl", "regular",
            out("你硬生生踩出一条路。鞋底被腐蚀出几个小洞。", e("san", value="-1d3")),
            out("虫群突然改变方向，全部涌向你的脚。", e("hp", value="-1d6"), e("san", value="-1d4"))),
        opt("观察它们的去向", "natural_world", "hard",
            out("它们全部钻进海里。海面下有什么巨大的东西张开嘴，接住了这场盛宴。",
                e("mythos", value=1), e("san", value="-1d6"),
                handout="蠕虫潮的终点：海面下的巨口。"),
            out("虫群停下，齐齐抬头看你。你听见它们同时发出细小的笑声。", e("san", value="-1d6"))),
    ], weight=10, tags=["worms", "sea"]))

# 22
ALL.append(event(
    "clock_stopped", "停摆的钟",
    "所有的钟都停在 3:17。包括你手腕上的表，以及远处教堂的钟楼。",
    [
        opt("修理钟表", "mech_repair", "regular",
            out("你把表拆开，发现齿轮之间夹着一小片干枯的膜。表重新走动，但倒着走。",
                e("san", value="-1d3"), e("item", id="minecraft:clock", count=1)),
            out("齿轮突然全部崩开。你听见表壳里传来一声很轻的叹息。", e("san", value="-1d3"))),
        opt("等待指针再次走动", "listen", "regular",
            out("3:18 时，所有钟同时响了一声。你听见空气里有人松了口气。", e("san", value="-1d4"),
                handout="3:18，所有钟同时响了一声。"),
            out("指针没有动，但秒针在你耳边越走越快。", e("san", value="-1d6"),
                e("effect", id="minecraft:nausea", duration=300))),
        opt("砸碎它", outcome=out("你砸碎了表。碎片里爬出一只没有壳的蜗牛。", e("san", value="-1d3"))),
    ], weight=10, tags=["clock", "time"]))

# 23
ALL.append(event(
    "whisper_in_ear", "耳畔低语",
    "有人贴着你耳边说话，但你身后空无一人。低语用的是你母亲的声音。",
    [
        opt("回应", "listen", "regular",
            out("低语告诉你一个地址，并说：『别带别人来。』", e("san", value="-1d3"),
                handout="低语给的地址：城郊一间废弃诊所。"),
            out("你回应后，低语变成了许多个声音同时喊你的名字。", e("san", value="-1d6"),
                e("spawn", id="minecraft:enderman", count=1))),
        opt("捂住耳朵", outcome=out("你捂住耳朵。低语改为从你的胸腔里响起。", e("san", value="-1d3"))),
        opt("循声寻找", "stealth", "regular",
            out("你在墙角找到一只小小的骨哨。吹响时，低语停了。", e("san", value="-1d3"),
                e("item", id="minecraft:bone", count=1)),
            out("声音把你引到一面墙前。墙上浮现出一张正在说话的嘴。", e("san", value="-1d6"))),
    ], weight=15, tags=["whisper", "voice"]))

# 24
ALL.append(event(
    "shadow_figure", "影中人",
    "街角站着一个完全由阴影构成的人形。它没有五官，但你确信它正在笑。",
    [
        opt("追踪", "stealth", "regular",
            out("它把你带到一间空屋，在墙上留下一行字后消失了。", e("san", value="-1d3"),
                handout="影中人留下的字：它已经进来了。"),
            out("你跟着它转过街角，却发现它在等你。它抬起手指了指你身后。", e("san", value="-1d6"))),
        opt("无视它", outcome=out("你目不斜视地走过。它在你经过时轻轻鼓了鼓掌。", e("san", value="-1"))),
        opt("照亮它", "occult", "regular",
            out("光照过去，它像被烫到一样缩进墙缝。地上留下一小撮灰。", e("san", value="-1d4"),
                handout="影中人畏惧强光。"),
            out("光线下，它的影子里露出无数张重叠的脸。", e("san", value="-1d8"),
                e("effect", id="minecraft:blindness", duration=100))),
    ], weight=12, tags=["shadow"]))

# 25
ALL.append(event(
    "the_dreamer", "沉睡者",
    "山谷中央躺着一个巨大的人形。它的胸口缓慢起伏，呼吸带起整片森林的潮声。",
    [
        opt("靠近", "occult", "hard",
            out("你在它掌心里发现一座微缩的城市。街道上有人在走动，其中一个人抬头看了你一眼。",
                e("mythos", value=2), e("san", value="-1d10"),
                handout="沉睡者掌心的城市：其中一个人抬头看了你一眼。"),
            out("它翻了个身。你被气浪掀飞，落地时发现自己少了一段记忆。", e("san", value="-1d10"),
                e("effect", id="minecraft:darkness", duration=300))),
        opt("跪拜", outcome=out("你跪下。它的呼吸停了一拍，像是在确认什么。", e("mythos", value=1),
                                  e("san", value="-1d8"))),
        opt("逃离", outcome=out("你跑出山谷。身后传来一声漫长的、满足的叹息。", e("san", value="-1d6"))),
    ], weight=2, once=True, tags=["dreamer", "mythos"]))

# 26
ALL.append(event(
    "cat_human_eyes", "人眼之猫",
    "一只黑猫蹲在墙头。它的眼睛是人的眼睛，瞳孔里映出你从未去过的房间。",
    [
        opt("抚摸它", "charm", "regular",
            out("猫允许你摸了一下。它的毛下没有温度，但你的紧张感消失了。", e("san", value="1d3")),
            out("猫突然抓住你的手，瞳孔里的房间开始着火。", e("san", value="-1d3"), e("hp", value="-1d2"))),
        opt("与它对视", "psychology", "hard",
            out("你在它的瞳孔里看见了自己的童年，以及一个你不认识的自己。", e("san", value="-1d3"),
                handout="猫瞳孔里的房间：你童年的卧室，但家具位置相反。"),
            out("猫的眼睛眨了一下。你的视野里多了一块永远无法聚焦的盲区。", e("san", value="-1d6"),
                e("effect", id="minecraft:blindness", duration=100))),
        opt("驱赶它", outcome=out("猫跳下墙头离开。它走路的姿势像人。", e("san", value="-1"))),
    ], weight=10, tags=["cat", "eyes"]))

# ===== 剧本事件 =====
scenario_events = [
    event("s_dark_wispers_prologue", "第一幕：没有署名的信",
          "你收到一封没有署名的信。信纸上有海水干涸后的盐渍，正文只有一句：『回村里来，它又开始做梦了。』",
          [
              opt("检查信封", "spot_hidden", "regular",
                  out("邮戳来自一个已经废弃二十年的小镇。信封内侧写着你的旧名。", e("san", value="-1"),
                      handout="信封内侧：你的旧名，以及一个废弃小镇的邮戳。"),
                  out("信封里掉出一根干枯的黑色草茎。", e("san", value="-1d3"))),
              opt("直接出发", outcome=out("你收拾行李，踏上回村的路。一路上，海风都从同一个方向吹来。",
                                            e("san", value="-1"))),
              opt("烧掉信", outcome=out("信烧完后，灰烬在桌上拼出村子的轮廓。", e("san", value="-1d3"))),
          ], weight=1, kp_only=True, tags=["scenario"]),
    event("s_dark_wispers_village", "第二幕：沉默的村庄",
          "村口没有人。每一扇窗后都有人影，但没有一扇窗打开。井边放着一只还在滴水的木桶。",
          [
              opt("询问村民", "persuade", "regular",
                  out("门开了一条缝。有人说：『教堂下面有东西在敲墙。』", e("san", value="-1"),
                      handout="村民的话：教堂下面有东西在敲墙。"),
                  out("门后的人压低声音说：『你不该回来。』然后门被彻底关死。", e("san", value="-1d3"))),
              opt("查看井", "spot_hidden", "regular",
                  out("井壁上刻着与石碑相同的螺旋。井底传来规律的敲击声。", e("san", value="-1d3"),
                      handout="井壁上的螺旋与石碑一致，井底有敲击声。"),
                  out("你探头时，井底有什么东西也抬起了头。", e("san", value="-1d6"))),
              opt("直接去教堂", outcome=out("你穿过空无一人的街道。钟楼上的钟停在 3:17。", e("san", value="-1"))),
          ], weight=1, kp_only=True, tags=["scenario"]),
    event("s_dark_wispers_church", "第三幕：教堂",
          "教堂的门虚掩着。长椅上坐满了人，他们都低着头，像是在祈祷，又像是睡着了。",
          [
              opt("阅读祭坛铭文", "occult", "regular",
                  out("铭文记载：『它被埋在地下，靠梦为生。只要还有人记得它，它就醒不来。』",
                      e("san", value="-1d3"), e("mythos", value=1),
                      handout="祭坛铭文：它靠梦为生。只要还有人记得它，它就醒不来。"),
                  out("铭文在你读完后自行剥落，露出下面一层更古老的文字。那些文字在流血。",
                      e("san", value="-1d6"))),
              opt("搜查告解室", "spot_hidden", "regular",
                  out("你在告解室找到一本日记，主人是二十年前失踪的牧师。", e("san", value="-1d3"),
                      e("item", id="minecraft:book", count=1),
                      handout="牧师的日记：『它要求我们不要停止做梦。』"),
                  out("告解室的帘子后面有人。你拉开帘子，只看见一套空衣服。", e("san", value="-1d4"))),
              opt("祈祷", "psychoanalysis", "regular",
                  out("你闭上眼睛。祷词让长椅上的『人』同时抬起了头。", e("san", value="-1d4")),
                  out("你的祈祷得到了回应，但回应你的不是神。", e("san", value="-1d6"))),
          ], weight=1, kp_only=True, tags=["scenario"]),
    event("s_dark_wispers_cellar", "第四幕：地下",
          "祭坛后的地板可以掀开。下面是一条向下的石阶，墙上挂着还在燃烧的油灯。",
          [
              opt("潜入", "stealth", "regular",
                  out("你无声地下到地窖。中央的石台上躺着一个人形，胸口放着你的信。", e("san", value="-1d4"),
                      handout="地窖石台上的人形：胸口放着与你收到的一模一样的信。"),
                  out("一块石板在你脚下断裂。地窖深处的敲击声停了。", e("san", value="-1d6"),
                      e("spawn", id="minecraft:husk", count=1))),
              opt("点燃火把", "survival", "regular",
                  out("火光驱散了部分阴影。你看见墙上刻满了名字，其中一行是你的。", e("san", value="-1d3"),
                      handout="地窖墙上的名字：其中一行是你的。"),
                  out("火把突然熄灭。黑暗里有人吹了一口气。", e("san", value="-1d4"))),
              opt("正面闯入", "fighting_brawl", "regular",
                  out("你打倒了一个守在石台上的教徒。他倒下时还在笑。", e("hp", value="-1d3"),
                      e("san", value="-1d3")),
                  out("三个教徒同时扑上来。你勉强脱身，但丢掉了火把。", e("hp", value="-1d6"),
                      e("san", value="-1d4"))),
          ], weight=1, kp_only=True, tags=["scenario"]),
    event("s_dark_wispers_dream", "第五幕：梦",
          "你在地窖里失去了意识。梦里，你站在那座海底城市的中央，所有街道都通向同一扇门。",
          [
              opt("记住城市布局", "navigate", "hard",
                  out("你记住了门的位置。醒来时，你的手心里攥着一把湿沙。", e("mythos", value=1),
                      e("san", value="-1d6"), handout="梦中的门：位于城市最低处的广场中央。"),
                  out("街道在你脚下折叠。你听见门后有人在叫你的名字。", e("san", value="-1d10"))),
              opt("抵抗醒来", "psychoanalysis", "regular",
                  out("你用疼痛唤醒自己。地窖的油灯还在燃烧，但你的头发湿透了。", e("san", value="-1d3")),
                  out("你醒不过来。梦里有人替你做了决定。", e("san", value="-1d6"))),
              opt("接受这一切", outcome=out("你向门走去。门后是另一个你，正等着交换位置。", e("mythos", value=1),
                                              e("san", value="-1d10"))),
          ], weight=1, kp_only=True, tags=["scenario"]),
    event("s_dark_wispers_finale", "终幕：真名",
          "石台上的人形坐了起来。它的脸是一张空白画布，而你忽然想起了那个名字。",
          [
              opt("念出真名", "occult", "hard",
                  out("你念出名字。人形僵住，身体像旧墙皮一样剥落。地窖里恢复了安静。",
                      e("san", value="-1d6"), e("mythos", value=1),
                      handout="你念出的真名：它被重新封回地下。"),
                  out("你念错了一个音节。人形转向你，空白画布上慢慢浮现出你的脸。",
                      e("san", value="-1d10"), e("mythos", value=1))),
              opt("用火把烧掉画像", "throw", "regular",
                  out("火把准确命中石台后的画像。人形发出无声的尖叫，化作一摊盐水。", e("san", value="-1d4"),
                      handout="画像被烧毁，人形化作盐水。"),
                  out("火把落在地上。画像里的脸笑了，火焰无法靠近。", e("san", value="-1d8"))),
              opt("逃跑", "dodge", "regular",
                  out("你逃出地窖。身后传来一声叹息，然后是长久的沉默。", e("san", value="-1d6")),
                  out("石阶变成了向下延伸的街道。你跑了很久，又回到了石台前。", e("san", value="-1d10"))),
          ], weight=1, kp_only=True, tags=["scenario"]),
]
ALL.extend(scenario_events)

# 写事件文件
index = []
for d in ALL:
    fn = d["id"] + ".json"
    with open(os.path.join(EVENTS, fn), "w", encoding="utf-8") as f:
        json.dump(d, f, ensure_ascii=False, indent=2)
        f.write("\n")
    index.append(fn)
with open(os.path.join(EVENTS, "index.json"), "w", encoding="utf-8") as f:
    json.dump({"events": index}, f, ensure_ascii=False, indent=2)
    f.write("\n")

# 剧本
scenario = {
    "id": "dark_whispers",
    "title": "黑暗中的低语",
    "description": "一封没有署名的信把你带回故乡。村庄沉默，教堂地窖里有东西在敲墙。",
    "intro": "你收到一封没有署名的信。信纸上有海水干涸后的盐渍，正文只有一句：『回村里来，它又开始做梦了。』\n\n（KP 提示：本剧本共六幕。请在 KP 面板中逐幕推进；玩家事件卡选择后，点击“下一幕”。）",
    "outro": "地窖重新安静下来。你走出教堂时，天已经亮了。村口有人向你点头，但没有人和你说话。\n\n你低头看信，纸上的字迹已经消失，只剩下一片盐渍。",
    "sanReward": "1d10",
    "events": [
        "s_dark_wispers_prologue",
        "s_dark_wispers_village",
        "s_dark_wispers_church",
        "s_dark_wispers_cellar",
        "s_dark_wispers_dream",
        "s_dark_wispers_finale",
    ],
}
with open(os.path.join(SCENARIOS, "dark_whispers.json"), "w", encoding="utf-8") as f:
    json.dump(scenario, f, ensure_ascii=False, indent=2)
    f.write("\n")
with open(os.path.join(SCENARIOS, "index.json"), "w", encoding="utf-8") as f:
    json.dump({"scenarios": ["dark_whispers.json"]}, f, ensure_ascii=False, indent=2)
    f.write("\n")

print("events:", len(ALL), "scenarios: 1")
