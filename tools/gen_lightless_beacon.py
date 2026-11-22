# -*- coding: utf-8 -*-
"""新增《无光信标》中文原创改编剧本（11 幕 + 5 支线）。
结构参考官方免费入门模组 The Lightless Beacon 的海难/灯塔/深潜者框架，正文全部重写，不复制官方文本。
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


def out(text, *effects, handout=None):
    d = {"text": text}
    if effects:
        d["effects"] = list(effects)
    if handout:
        d["handout"] = handout
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
        "tags": tags or ["scenario", "beacon"],
        "conditions": {},
        "options": options,
    }


NEW = []

NEW.append(event(
    "s_lb_voyage", "第一幕：夜航",
    "1926 年 4 月 12 日，新月的夜晚。海鸥号在暴雨里颠簸，白塔岛的灯塔本该在午夜亮起，但海面上只有黑。",
    [
        opt("帮水手收帆", "climb", "regular",
            out("你抓住湿滑的缆绳，看见礁石间有一线白光——不是灯塔，是浪打在沉船上的反光。",
                e("san", value="-1"), handout="夜航：礁石间有沉船反光，灯塔没有亮。"),
            out("浪把你从桅杆上掀下来，你抓住栏杆，肩膀撞在船舷上。",
                e("hp", value="-1d3"), e("san", value="-1"))),
        opt("问船长", "psychology", "regular",
            out("船长盯着黑掉的海面：『灯塔三小时前就该亮了。这不对劲。』",
                e("san", value="-1"), handout="船长：灯塔三小时前就该亮了。"),
            out("船长突然闭嘴，把望远镜塞给你。镜片里只有黑。", e("san", value="-1d3"))),
        opt("检查货舱", "spot_hidden", "regular",
            out("货舱里有一只钉死的木箱，箱角刻着盐沼镇的地址和达贡的印记。",
                e("san", value="-1d3"), handout="货舱木箱：盐沼镇地址；达贡印记。"),
            out("你碰到木箱时，里面传来硬币碰撞的声音，像有人在数钱。", e("san", value="-1d3"))),
    ]))

NEW.append(event(
    "s_lb_wreck", "第二幕：触礁",
    "船底传来一声闷响。海水从货舱涌进来，船长在甲板上喊：『放救生艇！』",
    [
        opt("抓住救生艇", "dodge", "regular",
            out("你抓住艇绳，和两名水手一起把救生艇放进水里。你顺手带上了医疗包。",
                e("san", value="-1"), e("item", id="minecraft:paper", count=1),
                handout="救生艇：医疗包、绳子、一盏还能亮的提灯。"),
            out("浪把救生艇拍在船舷上。你被甩进水里，呛了一口咸水。",
                e("hp", value="-1d4"), e("san", value="-1d3"))),
        opt("跳海游向岛", "swim", "regular",
            out("你游过冰冷的浪，脚下偶尔碰到礁石。上岸时，你看见灯塔的黑影就在头顶。",
                e("san", value="-1d3")),
            out("海流把你往礁石上推。你抓住一块尖石，手掌被割开。",
                e("hp", value="-1d4"), e("san", value="-1d3"))),
        opt("抢救物资", "spot_hidden", "regular",
            out("你在货舱口抓住一只防水袋，里面有一卷绳、一盒火柴和一把信号枪。",
                e("san", value="-1"), e("item", id="minecraft:iron_ingot", count=1),
                handout="防水袋：绳子、火柴、信号枪。"),
            out("货舱门在你面前关上。你砸开一条缝，只抢出一只空箱子。",
                e("hp", value="-1d2"), e("san", value="-1d3"))),
    ]))

NEW.append(event(
    "s_lb_shore", "第三幕：白塔岛",
    "你们被浪冲上黑礁滩。白塔岛的灯塔在头顶沉默，海风里有鱼腥味。",
    [
        opt("找路上灯塔", "navigate", "regular",
            out("你找到一条湿滑的石阶，阶上有巨大的蹼状脚印，一直通向灯塔。",
                e("san", value="-1d3"), handout="石阶上的蹼状脚印，通向灯塔。"),
            out("你在礁石间绕了很久，最后回到原地。潮水正在上涨。", e("san", value="-1d3"))),
        opt("搜海滩", "spot_hidden", "regular",
            out("你在残骸里找到一只钉死的木箱和几枚刻着怪脸的旧金币。",
                e("san", value="-1d3"), e("item", id="minecraft:gold_nugget", count=2),
                handout="海滩残骸：钉死的木箱、刻着怪脸的旧金币。"),
            out("你翻开一块船板，下面是一只被啃掉一半的海鸟。", e("san", value="-1d4"))),
        opt("先去船屋", "listen", "regular",
            out("船屋里有微弱的呼吸声，还有金属轻轻碰撞的声音。",
                e("san", value="-1"), handout="船屋里有呼吸声和金属碰撞声。"),
            out("船屋的门自己开了一条缝，里面很黑。", e("san", value="-1d3"))),
    ]))

NEW.append(event(
    "s_lb_cottage", "第四幕：守塔人小屋",
    "小屋门开着。守塔人倒在发电机旁，胸口被撕开；地上有几条鱼一样的小东西的尸体，还有一把打空的手枪。",
    [
        opt("检查尸体", "medicine", "regular",
            out("他被有蹼的爪子撕开；手枪少了三发子弹。他死前曾向什么东西开枪。",
                e("san", value="-1d3"), handout="守塔人：被蹼爪撕开；手枪少了三发子弹。"),
            out("尸体还是温的。他的手指在灰尘里写了一个字：『箱』。", e("san", value="-1d4"))),
        opt("搜屋子", "spot_hidden", "regular",
            out("你找到守塔人的日志、一袋旧金币和一把还能用的霰弹枪。",
                e("san", value="-1d3"), e("item", id="minecraft:gold_nugget", count=2),
                handout="守塔人物品：日志、一袋旧金币、霰弹枪。"),
            out("抽屉里只有一张盐沼镇的名片，背面写着你的名字。", e("san", value="-1d4"))),
        opt("检查小生物", "occult", "regular",
            out("它们不是鱼。它们有四肢、有肺，眼睛像人。你在其中一只的胃里找到一枚金币。",
                e("san", value="-1d4"), handout="小生物：有四肢和肺，眼睛像人；胃里有金币。"),
            out("其中一只突然抽动，咬住你的手指。", e("hp", value="-1d2"), e("san", value="-1d3"))),
    ]))

NEW.append(event(
    "s_lb_lighthouse", "第五幕：灯塔",
    "灯塔的灯室被砸碎，镜片上有弹孔。发电机被人为破坏，无线电还亮着微弱的绿灯。",
    [
        opt("修发电机", "mech_repair", "regular",
            out("你接上被扯断的线，发电机咳嗽着转起来。灯塔的灯泡亮了一下，又灭了。",
                e("san", value="-1"), handout="发电机修好：灯塔灯泡闪了一下，线路仍不稳定。"),
            out("你被电了一下，手麻了半分钟。发电机没有反应。", e("hp", value="-1d2"))),
        opt("用无线电", "elec_repair", "regular",
            out("你调到一个海岸警卫队频道，断断续续地喊出岛名和‘灯塔熄灭’。对面回了三个字：『坚持住。』",
                e("san", value="1d3"), handout="无线电：联系上海岸警卫队，对方说‘坚持住’。"),
            out("无线电里只有海浪声，然后是一个不属于人类的呼吸。", e("san", value="-1d3"))),
        opt("查灯室", "spot_hidden", "regular",
            out("镜片是从里面被砸碎的；地上有蹼爪印和一枚旧金币。",
                e("san", value="-1d3"), handout="灯室：镜片从里面被砸碎；蹼爪印、旧金币。"),
            out("你抬头时，灯室顶上有东西动了一下。", e("san", value="-1d4"))),
    ]))

NEW.append(event(
    "s_lb_wreckage", "第六幕：沉船残骸",
    "退潮后，礁石间露出沉船残骸和一只钉死的木箱。箱子上有达贡的印记。",
    [
        opt("撬开木箱", "locksmith", "regular",
            out("箱子里全是旧金币和几枚刻着怪脸的护符；金币上沾着干掉的海藻。",
                e("san", value="-1d3"), e("item", id="minecraft:gold_nugget", count=3),
                handout="木箱：旧金币、怪脸护符、干海藻。"),
            out("箱盖弹开，里面爬出一只像鱼又像人的小东西。", e("san", value="-1d4"),
                e("spawn", id="minecraft:silverfish", count=1))),
        opt("沿足迹", "track", "regular",
            out("蹼状脚印从木箱一直延伸到海蚀洞；洞里有微弱的蓝光。",
                e("san", value="-1d3"), handout="足迹通向海蚀洞；洞里有蓝光。"),
            out("足迹在礁石上绕成圈，最后回到你脚下。", e("san", value="-1d4"))),
        opt("检查船名", "history", "regular",
            out("船首刻着‘达贡号’；这是一艘二十年前失踪的走私船，船上载着南洋古钱。",
                e("san", value="-1"), handout="沉船是二十年前失踪的走私船‘达贡号’，载有南洋古钱。"),
            out("船名在你读完后变成了你的名字。", e("san", value="-1d3"))),
    ]))

NEW.append(event(
    "s_lb_cave", "第七幕：海蚀洞",
    "洞穴里都是海水和鱼腥味。墙上刻着达贡的符号，地上堆着金币和被啃过的骨头。",
    [
        opt("潜行", "stealth", "regular",
            out("你躲在礁石后，看见十几只年轻者围着一只穿旧外套的成熟体；它在数金币。",
                e("san", value="-1d4"), handout="海蚀洞：年轻者围着一只穿旧外套的成熟体；它在数金币。"),
            out("一块碎石从你脚下滚进水里。所有年轻者同时转头。", e("san", value="-1d6"))),
        opt("观察仪式", "occult", "regular",
            out("它们在把金币放进一只石槽；石槽下面刻着‘归还’两个字。",
                e("san", value="-1d3"), e("mythos", value=1),
                handout="海蚀洞仪式：金币被放进石槽；石槽下刻着‘归还’。"),
            out("你读懂了一个符号，然后所有人都开始头痛。", e("san", value="-1d6"))),
        opt("正面冲突", "firearms_handgun", "regular",
            out("你打中一只成熟体，年轻者四散；洞深处传来更多湿漉漉的脚步声。",
                e("san", value="-1d4"), e("hp", value="-1d3"),
                handout="成熟体受伤；洞深处还有更多年轻者。"),
            out("子弹打在石壁上。所有年轻者朝你涌来。", e("hp", value="-1d6"), e("san", value="-1d6"))),
    ]))

NEW.append(event(
    "s_lb_survivor", "第八幕：船屋里的幸存者",
    "船屋的渔网下面有人在发抖。是海鸥号的水手艾伦，他说守塔人偷了‘不该拿的东西’。",
    [
        opt("安抚他", "psychology", "regular",
            out("艾伦告诉你：守塔人马修从沉船里捞了金币，想卖给盐沼镇的人；昨晚‘他们’就来了。",
                e("san", value="-1d3"), handout="艾伦：马修捞了沉船金币，想卖给盐沼镇；昨晚‘他们’来了。"),
            out("艾伦抓住你的袖子，反复说：『别开灯。别开灯。』", e("san", value="-1d3"))),
        opt("追问", "persuade", "regular",
            out("艾伦承认：他也拿了两枚金币，藏在船屋地板下。他把金币交给你。",
                e("san", value="-1d3"), e("item", id="minecraft:gold_nugget", count=2),
                handout="艾伦交出两枚藏起来的金币。"),
            out("艾伦突然改口，说他不认识守塔人。你看见他口袋里有金币的轮廓。", e("san", value="-1d3"))),
        opt("检查船屋", "spot_hidden", "regular",
            out("船屋后门停着一艘还能用的划艇，油箱里还有半箱油；墙上有马修的便条：『别让灯灭。』",
                e("san", value="-1"), handout="船屋：可用的划艇、半箱油、马修的便条‘别让灯灭’。"),
            out("你拉开防水布，下面是一具被啃过的水手尸体。", e("san", value="-1d4"))),
    ]))

NEW.append(event(
    "s_lb_generator", "第九幕：点亮灯塔",
    "发电机修好了。你可以点亮灯塔，或保持黑暗。海蚀洞方向传来搬动石头的声音。",
    [
        opt("点亮灯塔", "elec_repair", "regular",
            out("灯塔亮了。光柱扫过海面，也扫过礁石上的蹼爪。你听见洞里传来愤怒的尖叫。",
                e("san", value="-1d3"), e("mythos", value=1),
                handout="灯塔点亮：海岸警卫队可能看见；洞里的东西被激怒。"),
            out("灯泡爆了。黑暗里，所有蹼爪同时朝灯塔转过来。", e("san", value="-1d4"))),
        opt("只发无线电", "elec_repair", "regular",
            out("你调好频道，发出求救信号。对面回：『黎明到。坚持住。』",
                e("san", value="1d3"), handout="无线电求救成功：海岸警卫队黎明到。"),
            out("无线电里只有静电和海水声。", e("san", value="-1d3"))),
        opt("保持黑暗/设陷阱", "survival", "regular",
            out("你们用绳、椅子和煤油在门口设了陷阱；黑暗里，蹼爪踩上了第一根绳。",
                e("san", value="-1"), handout="小屋门口设好绳套与煤油陷阱。"),
            out("你拉绳子时，绳套自己松了。黑暗里有人在笑。", e("san", value="-1d4"))),
    ]))

NEW.append(event(
    "s_lb_siege", "第十幕：围攻",
    "午夜，海蚀洞方向传来湿漉漉的脚步声。年轻者爬上礁石，成熟体在灯塔下站着。",
    [
        opt("守住小屋", "firearms_rifle", "regular",
            out("你们守住房门，打退了第一波年轻者；成熟体退到灯塔阴影里。",
                e("san", value="-1d4"), e("hp", value="-1d3"),
                handout="守住第一波：成熟体退进灯塔阴影。"),
            out("年轻者从窗户涌进来。你们退到二楼，弹药不多了。",
                e("hp", value="-1d6"), e("san", value="-1d6"))),
        opt("用火/灯", "throw", "regular",
            out("煤油灯砸在礁石上，火墙把年轻者逼回海里；成熟体在火后看着你。",
                e("san", value="-1d4"), handout="火墙逼退年轻者；成熟体还在。"),
            out("火被海风吹灭。年轻者跨过余烬。", e("hp", value="-1d4"), e("san", value="-1d6"))),
        opt("用金币引开", "throw", "regular",
            out("你把金币撒向海蚀洞。年轻者转身去抢，成熟体发出一声低吼。",
                e("san", value="-1d3"), handout="金币引开年轻者；成熟体没有动。"),
            out("金币落在水裡，没有人去捡。成熟体朝你走来。", e("san", value="-1d6"))),
        opt("躲进灯塔", "stealth", "regular",
            out("你们躲进灯室，听着蹼爪在铁梯上上下下；它们没有找到你们。",
                e("san", value="-1d4"), handout="躲进灯室：蹼爪在铁梯上搜索，没有找到你们。"),
            out("灯室的门被撞开。你看见成熟体的脸贴着玻璃。", e("san", value="-1d8"))),
    ]))

NEW.append(event(
    "s_lb_dawn", "第十一幕：黎明",
    "天边发白。海岸警卫队的船出现在雾里；洞穴里还有声音。",
    [
        opt("发信号", "survival", "regular",
            out("信号枪的红色焰火穿过雾。警卫队的船鸣笛回应，放下救生艇。",
                e("san", value="1d3"), handout="结局：信号成功，海岸警卫队救援。"),
            out("信号弹受潮，只冒出白烟。警卫队的船从雾里驶过，没有看见。",
                e("san", value="-1d3"))),
        opt("封住洞穴", "mech_repair", "regular",
            out("你们用石块和渔网封住洞口。最后一铲落下时，洞里传来一声像叹息的水声。",
                e("san", value="1d6"), handout="结局：海蚀洞被封住；洞里传来叹息。"),
            out("封口塌了一半；海水从石缝里涌出来。", e("san", value="-1d3"))),
        opt("烧掉金币", "throw", "regular",
            out("金币在火里熔化，发出腥臭。洞里的声音停了。",
                e("san", value="1d3"), e("mythos", value=1),
                handout="结局：金币被烧毁；洞里的声音停止。"),
            out("金币烧不化。火焰变成蓝色，洞里的声音更近了。", e("san", value="-1d4"))),
        opt("带走金币", outcome=out("你把金币装进口袋。离开时，口袋比海水更冷。",
                                      e("san", value="-1d3"),
                                      handout="结局：带走金币；口袋比海水更冷。")),
    ]))

# ===== 支线 =====
NEW.append(event(
    "s_lb_side_coins", "支线：会数钱的金币",
    "你在残骸里捡到一枚刻着怪脸的旧金币。它在你手心里轻轻震动，像在数你的脉搏。",
    [
        opt("把金币放回去", outcome=out("金币落回水里，震动停了。你听见一声很轻的叹息。", e("san", value="-1"))),
        opt("收进口袋", "occult", "regular",
            out("金币安静下来。你开始梦见海底有人数钱。", e("san", value="-1d3"),
                e("item", id="minecraft:gold_nugget", count=1),
                handout="旧金币：收进口袋后，梦见海底有人数钱。"),
            out("金币在你口袋里发烫。你掏出来时，它变成了一枚牙齿。", e("san", value="-1d4"))),
    ], tags=["scenario", "beacon", "side"]))

NEW.append(event(
    "s_lb_side_log", "支线：守塔人的日志",
    "日志最后一页被海水泡皱：『它们只要金币。把金币还回去，别开灯。』",
    [
        opt("读完整本", "library_use", "regular",
            out("日志记录：马修从沉船捞了金币，联系盐沼镇买家；昨晚有人敲门，门外没有脚印。",
                e("san", value="-1d3"), handout="守塔人日志：金币来自沉船；盐沼镇买家；门外没有脚印。"),
            out("日志的字迹在你眼前游动，你只记住‘别开灯’。", e("san", value="-1d3"))),
        opt("撕下最后一页", outcome=out("你把最后一页塞进口袋。纸上的字还在慢慢变。",
                                          e("san", value="-1"), e("item", id="minecraft:paper", count=1),
                                          handout="日志最后一页：‘它们只要金币。把金币还回去，别开灯。’")),
    ], tags=["scenario", "beacon", "side"]))

NEW.append(event(
    "s_lb_side_radio", "支线：无线电里的第三个人",
    "无线电里有人在报坐标，声音像被水泡过。你数了数，房间里只有你们几个人。",
    [
        opt("回应", "listen", "regular",
            out("对方重复了你的名字，然后说：『别让灯灭。』信号断了。",
                e("san", value="-1d3"), handout="无线电：对方叫出你的名字，说‘别让灯灭’。"),
            out("无线电里传来海水灌进麦克风的声音。", e("san", value="-1d4"))),
        opt("关闭无线电", outcome=out("你关掉无线电。静电声停了，但房间里还有第三个呼吸声。", e("san", value="-1d3"))),
    ], tags=["scenario", "beacon", "side"]))

NEW.append(event(
    "s_lb_side_altar", "支线：石槽祭坛",
    "洞穴深处有一只石槽，里面堆着金币和骨头。石槽底部刻着‘归还’两个字。",
    [
        opt("检查祭坛", "occult", "regular",
            out("石槽是达贡的祭坛；金币不是财宝，是‘租金’。拿走它们的人会被追讨。",
                e("san", value="-1d4"), e("mythos", value=1),
                handout="石槽祭坛：金币是‘租金’；拿走的人会被追讨。"),
            out("你读懂了‘归还’，然后听见洞里所有年轻者同时吸气。", e("san", value="-1d6"))),
        opt("破坏符号", "fighting_brawl", "regular",
            out("你砸碎石槽边缘。洞里的蓝光暗了一瞬，年轻者发出尖叫。",
                e("san", value="-1d4"), handout="石槽被破坏；蓝光暗了一瞬。"),
            out("石槽纹丝不动，你的手却开始抽筋。", e("hp", value="-1d3"), e("san", value="-1d3"))),
    ], tags=["scenario", "beacon", "side"]))

NEW.append(event(
    "s_lb_side_allen", "支线：艾伦的金币",
    "艾伦的手一直捂着口袋。你看见里面露出一点金色的边。",
    [
        opt("逼问", "intimidate", "regular",
            out("艾伦交出两枚金币，哭着说：『我只拿了两枚。它们说只要还回去就没事。』",
                e("san", value="-1d3"), e("item", id="minecraft:gold_nugget", count=2),
                handout="艾伦交出两枚金币：‘它们说只要还回去就没事。’"),
            out("艾伦突然尖叫，把口袋里的东西扔进海里。", e("san", value="-1d3"))),
        opt("安慰他", "charm", "regular",
            out("艾伦把金币交给你，告诉你海蚀洞的位置和‘它们怕火’。",
                e("san", value="-1"), handout="艾伦：海蚀洞位置；它们怕火。"),
            out("艾伦抱住头，反复说：『别让它们看见我。』", e("san", value="-1d3"))),
    ], tags=["scenario", "beacon", "side"]))

for d in NEW:
    with open(os.path.join(EVENTS, d["id"] + ".json"), "w", encoding="utf-8") as f:
        json.dump(d, f, ensure_ascii=False, indent=2)
        f.write("\n")

index_path = os.path.join(EVENTS, "index.json")
index = json.load(open(index_path, encoding="utf-8"))
names = index.setdefault("events", [])
for d in NEW:
    fn = d["id"] + ".json"
    if fn not in names:
        names.append(fn)
json.dump(index, open(index_path, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
open(index_path, "a", encoding="utf-8").write("\n")

scenario = {
    "id": "lightless_beacon",
    "title": "无光信标",
    "description": "向官方免费入门模组《无光信标》(The Lightless Beacon) 致敬的中文原创改编：海难、熄灭的灯塔、守塔人之死与礁石洞里的‘租金’。",
    "intro": "1926 年 4 月 12 日，新月的夜晚。海鸥号在暴风雨里颠簸。白塔岛的灯塔本该在午夜亮起，但海面上只有黑。\n\n（KP 提示：本剧本共十一幕，适合新手。正文为中文原创改编，不包含官方原文。建议关闭随机事件，按玩家行动在“幕”页触发。）",
    "outro": "天亮时，海岸警卫队的船把你们接走。白塔岛的灯塔仍然黑着。你低头看口袋里的古钱，它比海水更冷。",
    "sanReward": "1d10",
    "events": [
        "s_lb_voyage",
        "s_lb_wreck",
        "s_lb_shore",
        "s_lb_cottage",
        "s_lb_lighthouse",
        "s_lb_wreckage",
        "s_lb_cave",
        "s_lb_survivor",
        "s_lb_generator",
        "s_lb_siege",
        "s_lb_dawn",
    ],
    "acts": [
        {"act": 1, "event": "s_lb_voyage", "title": "夜航",
         "summary": "海鸥号在风暴中航行；灯塔未亮，货舱有达贡印记的木箱。",
         "trigger": "玩家在船上、帮忙收帆/问船长/查货舱时。",
         "sideEvents": []},
        {"act": 2, "event": "s_lb_wreck", "title": "触礁",
         "summary": "船触礁；救生艇、跳海或抢救物资。",
         "trigger": "船体进水、玩家决定如何逃生时。",
         "sideEvents": []},
        {"act": 3, "event": "s_lb_shore", "title": "白塔岛",
         "summary": "登岛；石阶蹼爪印、海滩残骸、船屋呼吸声。",
         "trigger": "玩家上岸、找路/搜海滩/去船屋时。",
         "sideEvents": ["s_lb_side_coins"]},
        {"act": 4, "event": "s_lb_cottage", "title": "守塔人小屋",
         "summary": "守塔人尸体、小生物尸体、手枪；日志与金币。",
         "trigger": "玩家进入小屋、检查尸体/搜屋/看小生物时。",
         "sideEvents": ["s_lb_side_log"]},
        {"act": 5, "event": "s_lb_lighthouse", "title": "灯塔",
         "summary": "灯室被砸、发电机被破坏、无线电可用；可求救。",
         "trigger": "玩家进灯塔、修发电机/用无线电/查灯室时。",
         "sideEvents": ["s_lb_side_radio"]},
        {"act": 6, "event": "s_lb_wreckage", "title": "沉船残骸",
         "summary": "退潮后的达贡号残骸、木箱金币、通向海蚀洞的足迹。",
         "trigger": "玩家搜海滩残骸、撬木箱/沿足迹时。",
         "sideEvents": ["s_lb_side_coins"]},
        {"act": 7, "event": "s_lb_cave", "title": "海蚀洞",
         "summary": "达贡符号、金币石槽、年轻者与成熟体；可潜行/观察/冲突。",
         "trigger": "玩家进入海蚀洞、观察祭坛或与深潜者冲突时。",
         "sideEvents": ["s_lb_side_altar"]},
        {"act": 8, "event": "s_lb_survivor", "title": "船屋里的幸存者",
         "summary": "水手艾伦；守塔人偷金币卖给盐沼镇；船屋有划艇。",
         "trigger": "玩家发现/安抚/追问艾伦，或检查船屋时。",
         "sideEvents": ["s_lb_side_allen"]},
        {"act": 9, "event": "s_lb_generator", "title": "点亮灯塔",
         "summary": "点亮灯塔/只发无线电/保持黑暗设陷阱；会激怒洞里的东西。",
         "trigger": "玩家决定开灯、发信号或设防时。",
         "sideEvents": []},
        {"act": 10, "event": "s_lb_siege", "title": "围攻",
         "summary": "年轻者与成熟体夜袭；守屋/火/金币/躲灯塔。",
         "trigger": "天黑后、蹼爪逼近小屋/灯塔时。",
         "sideEvents": []},
        {"act": 11, "event": "s_lb_dawn", "title": "黎明",
         "summary": "海岸警卫队或逃生；封洞/烧金币/带走金币。",
         "trigger": "天亮、救援出现或玩家处理洞穴/金币时。",
         "sideEvents": []},
    ],
}
with open(os.path.join(SCENARIOS, "lightless_beacon.json"), "w", encoding="utf-8") as f:
    json.dump(scenario, f, ensure_ascii=False, indent=2)
    f.write("\n")

sp = os.path.join(SCENARIOS, "index.json")
si = json.load(open(sp, encoding="utf-8"))
scens = si.setdefault("scenarios", [])
if "lightless_beacon.json" not in scens:
    scens.append("lightless_beacon.json")
json.dump(si, open(sp, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
open(sp, "a", encoding="utf-8").write("\n")

print("lightless:", len(scenario["events"]), "acts,", len(NEW), "new events")
