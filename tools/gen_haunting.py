# -*- coding: utf-8 -*-
"""新增《鬼屋》中文原创改编剧本（11 幕 + 5 支线）。
结构参考官方新手模组《鬼屋》(The Haunting) 的调查框架，正文全部重写，不复制官方文本。
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
        "tags": tags or ["scenario", "haunting"],
        "conditions": {},
        "options": options,
    }


NEW = []

NEW.append(event(
    "s_th_prologue", "第一幕：委托",
    "事务所的窗外在下雨。罗兰先生把一串黄铜钥匙推过桌面，钥匙上挂着一块褪色的木牌：槐树街 17 号。",
    [
        opt("检查合同与钥匙", "spot_hidden", "regular",
            out("合同上的房主是‘维尔家族’，签名日期是二十年前。钥匙冷得像刚从冰水里捞出来。",
                e("san", value="-1"), handout="委托合同：房主维尔家族，二十年前的签名；钥匙异常冰冷。"),
            out("钥匙在你手里转了一圈，木牌上的字变成了‘别进去’。", e("san", value="-1d3"))),
        opt("询问罗兰", "psychology", "regular",
            out("罗兰避开你的眼睛：『上一个租客只是搬走了。』你确定他在说谎。",
                e("san", value="-1"), handout="罗兰在说谎：上一个租客不是‘搬走’。"),
            out("罗兰突然闭嘴，盯着你身后。你回头，只看见雨。", e("san", value="-1d3"))),
        opt("直接去旧宅", outcome=out("你收下钥匙。雨停之前，你已经站在槐树街口。", e("san", value="-1"))),
    ]))

NEW.append(event(
    "s_th_neighborhood", "第二幕：槐树街 17 号",
    "槐树街 17 号是一栋维多利亚式旧宅，窗玻璃被木板封了一半。隔壁邻居站在门廊上，假装在浇一盆已经枯死的花。",
    [
        opt("询问邻居", "persuade", "regular",
            out("邻居说，前一个租客半夜跑出来，说墙里有人走路；再之前的人都没住满一个月。",
                e("san", value="-1"), handout="邻居：前一个租客说墙里有人走路；之前的租客都没住满一个月。"),
            out("邻居把水浇到你鞋上，转身进门。门后有人低声说：『别进去。』", e("san", value="-1d3"))),
        opt("检查信箱与门廊", "spot_hidden", "regular",
            out("信箱里塞满二十年前的广告和一张维尔家族的讣告；门廊地板上有拖拽痕迹。",
                e("san", value="-1d3"), handout="信箱：二十年前的广告、维尔家族讣告、门廊拖拽痕迹。"),
            out("信箱里有一只死老鼠，嘴里咬着写你名字的纸片。", e("san", value="-1d3"))),
        opt("直接进门", outcome=out("钥匙插进锁孔时，门自己开了一条缝。里面很冷。", e("san", value="-1"))),
    ]))

NEW.append(event(
    "s_th_ground_floor", "第三幕：一楼",
    "门厅的灰尘有半指厚。客厅家具都被白布盖着，壁炉冷得像一口井。地下室的门被一个衣柜堵住，衣柜后面传来三下敲击。",
    [
        opt("检查客厅与壁炉", "spot_hidden", "regular",
            out("壁炉里有一张烧了一半的照片：维尔家族站在墙前，墙上有一道新砖。",
                e("san", value="-1d3"), handout="烧焦的照片：维尔家族站在一道新砖墙前。"),
            out("照片里的脸都转向你。你把照片放下时，它还是热的。", e("san", value="-1d3"))),
        opt("听墙并回敲", "listen", "regular",
            out("你敲三下，墙里回三下；你敲两下，墙里回两下。它在模仿你。",
                e("san", value="-1"), handout="墙里的敲击会模仿你的节奏。"),
            out("墙里传来你的声音：『进来。』", e("san", value="-1d4"))),
        opt("移开衣柜", "climb", "regular",
            out("衣柜后面是地下室门，门缝里吹出冷风。门上刻着‘维尔’两个字。",
                e("san", value="-1"), handout="地下室门：门上刻着‘维尔’，门缝有冷风。"),
            out("衣柜倒下来，灰尘里有人咳嗽。", e("hp", value="-1d3"), e("san", value="-1"))),
    ]))

NEW.append(event(
    "s_th_bedroom", "第四幕：二楼卧室",
    "二楼的卧室门半掩着。床铺得很整齐，像有人刚离开；镜子被一块黑布盖住，床头柜上放着一本翻开的日记。",
    [
        opt("读日记", "library_use", "regular",
            out("伊莱亚斯记录：墙里有人每晚敲三下；他开始梦见一个叫维尔的人；最后一页只有‘它在我床下’。",
                e("san", value="-1d3"), handout="伊莱亚斯的日记：墙里敲三下；梦见维尔；最后一页‘它在我床下’。"),
            out("字迹在你眼前爬动，你只读懂‘床下’两个字。", e("san", value="-1d3"))),
        opt("掀开镜子", "occult", "regular",
            out("镜子里不是卧室，是一间砖墙密室；一个穿旧西装的男人站在镜中，背对着你。",
                e("san", value="-1d6"), handout="镜子：映出砖墙密室，一个穿旧西装的男人背对着你。"),
            out("镜中的男人慢慢转身，你看见自己的脸。", e("san", value="-1d6"))),
        opt("检查床", "medicine", "regular",
            out("床板下有皮带扣和干涸的血迹；床垫里缝着一小袋盐。",
                e("san", value="-1d3"), e("item", id="minecraft:paper", count=1),
                handout="床下：皮带扣、干涸血迹、床垫里的一小袋盐。"),
            out("床单自己收紧，缠住你的手腕。", e("hp", value="-1d2"), e("san", value="-1d3"))),
    ]))

NEW.append(event(
    "s_th_attic", "第五幕：阁楼",
    "阁楼比房子本身更旧。纸箱堆到屋顶，墙上贴满剪报和一张孩子的蜡笔画：一个男人站在砖墙里面。",
    [
        opt("查旧箱子", "spot_hidden", "regular",
            out("箱子里有维尔家族的相册、一枚铜护符和骨匕首的草图。",
                e("san", value="-1"), e("item", id="minecraft:gold_nugget", count=1),
                handout="旧箱子：维尔家族相册、铜护符、骨匕首草图。"),
            out("相册里的每一张脸都缺了眼睛。", e("san", value="-1d3"))),
        opt("读剪报", "library_use", "regular",
            out("二十年前，奥古斯特·维尔在自家地下室失踪；警方封了房子，没有找到尸体。",
                e("san", value="-1d3"), handout="剪报：奥古斯特·维尔二十年前在地下室失踪；警方封屋，未找到尸体。"),
            out("剪报上的日期每天都在变。今天的日期是你的生日。", e("san", value="-1d4"))),
        opt("看孩子的画", "psychology", "regular",
            out("画背面写着：『他说只要墙还在，他就不会走。』",
                e("san", value="-1d3"), handout="孩子的画：男人站在砖墙里；背面写着‘只要墙还在，他就不会走’。"),
            out("画里的男人少了一只手，你的手开始发麻。", e("san", value="-1d4"))),
    ]))

NEW.append(event(
    "s_th_research", "第六幕：旧记录",
    "镇图书馆的地方志里夹着维尔家的记录；教区教堂的牧师记得那场‘没有尸体的葬礼’。",
    [
        opt("查教区记录", "history", "regular",
            out("记录写着：维尔‘未被埋葬，封于宅内’；附有盐、铁与真名的束缚仪式。",
                e("san", value="-1d3"), handout="教区记录：维尔未被埋葬，封于宅内；盐、铁、真名可束缚。"),
            out("记录被墨水涂掉，只剩一句：『不要打开墙。』", e("san", value="-1d3"))),
        opt("询问牧师", "persuade", "regular",
            out("老牧师告诉你：念出维尔的全名，把盐和铁放进墙里，再烧掉他的锚点。",
                e("san", value="-1"), handout="牧师的建议：念全名，盐与铁封墙，烧掉锚点。"),
            out("牧师突然用拉丁语念了一段驱魔词，然后忘了你。", e("san", value="-1d3"))),
        opt("查旧报纸", "library_use", "regular",
            out("过去二十年，槐树街 17 号的租客都失踪或搬走；最近一个是伊莱亚斯。",
                e("san", value="-1d3"), handout="旧报纸：二十年里租客不断失踪/搬走；最近一个是伊莱亚斯。"),
            out("报纸上多了一篇讣告，死者的名字是你。", e("san", value="-1d6"))),
    ]))

NEW.append(event(
    "s_th_basement", "第七幕：地下室",
    "地下室比外面低十度。架子上摆满玻璃罐，里面泡着看不清的东西。最里面的砖墙是新的，砖缝里渗出盐霜。",
    [
        opt("搜查酒窖", "spot_hidden", "regular",
            out("你找到一把铁锤、一袋盐和一本被水泡过的仪式笔记。",
                e("san", value="-1"), e("item", id="minecraft:iron_ingot", count=1),
                handout="酒窖：铁锤、一袋盐、被水泡过的仪式笔记。"),
            out("一个罐子自己碎了，里面的东西在笑。", e("san", value="-1d3"))),
        opt("听砖墙", "listen", "regular",
            out("墙后有呼吸，节奏和你一样；你停下，它也停下。",
                e("san", value="-1"), handout="新砖墙后有呼吸，节奏与你同步。"),
            out("墙里传来你母亲的声音，叫你的小名。", e("san", value="-1d4"))),
        opt("拆砖", "mech_repair", "regular",
            out("砖墙被拆开一个洞，里面是暗室。冷风从洞里吹出来。",
                e("san", value="-1d3"), handout="砖墙被拆开：里面是一间没有窗的暗室。"),
            out("砖墙塌了一角，灰尘里有什么抓住你的手腕。", e("hp", value="-1d4"), e("san", value="-1d3"))),
    ]))

NEW.append(event(
    "s_th_hidden_room", "第八幕：砖墙后的房间",
    "砖墙后面是一间没有窗的暗室。一把旧椅子对着墙，椅子上坐着一具干瘪的遗体，胸口插着一把骨匕首。",
    [
        opt("检查遗骸", "medicine", "regular",
            out("遗骸是奥古斯特·维尔；他的下颌被撬开，像死前在喊什么。匕首柄上刻着‘锚’。",
                e("san", value="-1d4"), handout="遗骸：奥古斯特·维尔；下颌被撬开；匕首柄刻着‘锚’。"),
            out("遗骸的嘴唇动了一下，像是在数你的心跳。", e("san", value="-1d6"))),
        opt("取匕首与护符", "occult", "regular",
            out("匕首是锚点；铜护符能暂时挡住它。你拿走时，遗体叹了口气。",
                e("san", value="-1d4"), e("mythos", value=1),
                e("item", id="minecraft:gold_nugget", count=1),
                handout="匕首是锚点；铜护符可暂时阻挡；遗体在叹气。"),
            out("你碰到匕首时，椅子上的遗体抬起了头。", e("san", value="-1d6"))),
        opt("砸碎遗骸", "fighting_brawl", "regular",
            out("遗骸碎成盐和骨头；墙里传来尖叫。",
                e("san", value="-1d6"), handout="遗骸被砸碎；墙里传来尖叫。"),
            out("椅子上的遗体抓住你的手臂，力气大得不像死人。", e("hp", value="-1d4"), e("san", value="-1d6"))),
    ]))

NEW.append(event(
    "s_th_night_watch", "第九幕：守夜",
    "天黑了。你们决定留下守夜。十一点十七分，所有钟停在同一秒，楼上的床开始吱呀作响。",
    [
        opt("守夜", "listen", "regular",
            out("你看见一个穿旧西装的男人从墙里走出来；他念着自己的名字，像在确认自己还在。",
                e("san", value="-1d4"), handout="守夜：穿旧西装的男人从墙里走出，念着‘奥古斯特·维尔’。"),
            out("他走到你面前，用你的声音说：『你替我留下来。』", e("san", value="-1d6"))),
        opt("去卧室查看", "stealth", "regular",
            out("床下有一道暗门，通往墙里的夹层；里面有孩子的玩具和维尔的怀表。",
                e("san", value="-1d3"), e("item", id="minecraft:gold_nugget", count=1),
                handout="床下暗门：墙里夹层；孩子玩具、维尔怀表。"),
            out("床下伸出一只手，抓住你的脚踝。", e("hp", value="-1d3"), e("san", value="-1d4"))),
        opt("睡觉/被诱", outcome=out("梦里，维尔坐在你床边。他说只要有人记得他，他就无法离开。",
                                      e("san", value="-1d4"), e("mythos", value=1),
                                      handout="梦中的维尔：只要有人记得他，他就无法离开。")),
    ]))

NEW.append(event(
    "s_th_exorcism", "第十幕：驱逐",
    "维尔站在暗室中央。他的脸像一张被水泡过的照片，身后的墙里伸出许多只手。",
    [
        opt("烧掉遗骸与匕首", "throw", "regular",
            out("火从匕首烧到遗骸；维尔发出无声的尖叫，墙里的手缩了回去。",
                e("san", value="-1d6"), handout="结局：遗骸与匕首被烧毁；墙里的手缩回。"),
            out("火熄灭了；维尔转向你，墙里的手继续往前伸。", e("san", value="-1d8"))),
        opt("念出束缚咒", "occult", "hard",
            out("你念出维尔的全名和束缚词；他像被抽走骨头一样塌下去。",
                e("san", value="-1d4"), e("mythos", value=1),
                handout="结局：念出全名与束缚词；维尔塌落，墙里安静。"),
            out("你念错一个音节；墙里的手抓住了你。", e("san", value="-1d8"), e("hp", value="-1d4"))),
        opt("封墙并填入盐铁", "mech_repair", "regular",
            out("你把盐和铁填进墙缝，砖墙合拢；敲击声停了。",
                e("san", value="-1d3"), handout="结局：盐铁封墙；敲击声停止。"),
            out("墙合拢到一半，里面伸出一只手。", e("hp", value="-1d4"), e("san", value="-1d6"))),
        opt("逃跑", "dodge", "regular",
            out("你逃出房子；身后的门自己关上。",
                e("san", value="-1d4"), handout="结局：逃出旧宅；门在身后关上。"),
            out("楼梯变成向下的墙；你又回到暗室。", e("san", value="-1d8"))),
    ]))

NEW.append(event(
    "s_th_aftermath", "第十一幕：余波",
    "天亮了。罗兰先生站在街对面，手里攥着合同。二楼的窗帘动了一下。",
    [
        opt("交还钥匙，隐瞒真相", outcome=out("罗兰松了口气。你离开时，钥匙在口袋里变暖了。",
                                                e("san", value="1d3"),
                                                handout="结局：交还钥匙；钥匙在口袋里变暖。")),
        opt("追问罗兰与维尔家族", "psychology", "regular",
            out("罗兰承认房子是维尔家族托管的；他也不知道墙里有什么，只知道‘不能拆’。",
                e("san", value="-1d3"), handout="罗兰：房子由维尔家族托管；他只知道‘不能拆’。"),
            out("罗兰突然改口，说他从没见过你。", e("san", value="-1d4"))),
        opt("烧掉房子", outcome=out("火焰吞没旧宅；你听见墙里有人松了口气。",
                                      e("san", value="1d3"), e("mythos", value=1),
                                      handout="结局：旧宅被烧毁；墙里有人松了口气。")),
        opt("带走护符/日记", outcome=out("你带走护符与日记；夜里，护符微微发热。",
                                          e("san", value="1d3"),
                                          handout="结局：带走护符与日记；护符在夜里发热。")),
    ]))

# ===== 支线 =====
NEW.append(event(
    "s_th_side_dog", "支线：邻居的狗",
    "邻居的狗对着二楼空窗狂吠，然后夹着尾巴躲到门廊下。",
    [
        opt("安抚它", "charm", "regular",
            out("狗安静下来，用鼻子顶你的口袋，里面多了一枚旧纽扣。",
                e("san", value="-1"), handout="狗叼来的旧纽扣：刻着‘V’字。"),
            out("狗突然咬住你的袖子，把你往街上拖。", e("hp", value="-1d2"), e("san", value="-1d3"))),
        opt("观察它在看什么", "listen", "regular",
            out("二楼窗后站着一个人影；你眨眼时，那里只有木板。",
                e("san", value="-1d3"), handout="狗在看二楼空窗：窗后曾站着一个人影。"),
            out("狗对着你身后叫。你回头，什么也没有。", e("san", value="-1d3"))),
    ], tags=["scenario", "haunting", "side"]))

NEW.append(event(
    "s_th_side_letters", "支线：信箱里的旧信",
    "信箱最底层压着一叠没有寄出的信，收件人都是‘奥古斯特·维尔’，日期停在二十年前。",
    [
        opt("拆开最上面一封", "spot_hidden", "regular",
            out("信里只有一句：『你说过墙里的东西不会出来。』署名是伊莱亚斯。",
                e("san", value="-1d3"), handout="旧信：伊莱亚斯写给维尔——‘你说过墙里的东西不会出来’。"),
            out("信纸在你手里碎成盐。", e("san", value="-1d3"))),
        opt("全部带走", outcome=out("你把信塞进包里。它们比看起来重得多。",
                                      e("san", value="-1"), e("item", id="minecraft:paper", count=2),
                                      handout="一叠旧信：收件人维尔，日期停在二十年前。")),
    ], tags=["scenario", "haunting", "side"]))

NEW.append(event(
    "s_th_side_mirror", "支线：镜中密室",
    "你掀开镜子上的黑布。镜中映出的不是卧室，而是一间没有窗的砖墙密室。",
    [
        opt("凝视镜中", "occult", "regular",
            out("穿旧西装的男人在镜中转身。他指着自己胸口，那里插着一把骨匕首。",
                e("san", value="-1d4"), handout="镜中：维尔指着胸口的骨匕首。"),
            out("镜中的你伸出手，指尖碰到你的脸。", e("san", value="-1d6"))),
        opt("把布盖回去", outcome=out("你盖回黑布。镜子里传来一声很轻的‘谢谢’。", e("san", value="-1"))),
    ], tags=["scenario", "haunting", "side"]))

NEW.append(event(
    "s_th_side_rats", "支线：地窖鼠群",
    "你踩到一块松动的砖，几十只老鼠从罐子后面涌出来，朝同一个方向跑——砖墙。",
    [
        opt("驱赶鼠群", "fighting_brawl", "regular",
            out("老鼠四散；砖墙前留下一小堆骨头和一枚铜扣。",
                e("san", value="-1"), handout="鼠群留下的骨头与铜扣。"),
            out("老鼠没有咬你，只是从你脚边绕过，像在躲什么。", e("san", value="-1d3"))),
        opt("跟着它们", "stealth", "regular",
            out("老鼠全部钻进砖墙下的缝隙；你听见墙里有什么在啃东西。",
                e("san", value="-1d3"), handout="老鼠钻进砖墙缝隙；墙里有啃咬声。"),
            out("鼠群突然停下，齐齐回头看你。", e("san", value="-1d3"))),
    ], tags=["scenario", "haunting", "side"]))

NEW.append(event(
    "s_th_side_client", "支线：罗兰的秘密",
    "你跟踪罗兰。他没有回事务所，而是绕到槐树街后巷，对着旧宅的地下室窗户说话。",
    [
        opt("偷听", "stealth", "regular",
            out("罗兰说：『再撑一个月，等合同到期。』墙里有人回答：『你答应过让我出去。』",
                e("san", value="-1d4"), handout="罗兰与墙里的对话：再撑一个月；墙里说‘你答应过让我出去’。"),
            out("罗兰突然回头，直直看向你藏身的地方。", e("san", value="-1d6"))),
        opt("当面质问", "intimidate", "regular",
            out("罗兰承认：维尔家族付钱让他定期送人来‘看房’，但他不知道墙里是什么。",
                e("san", value="-1d3"), handout="罗兰承认：维尔家族付钱让他定期送人来看房。"),
            out("罗兰尖叫着跑开；墙里传来笑声。", e("san", value="-1d4"))),
    ], tags=["scenario", "haunting", "side"]))

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
    "id": "the_haunting",
    "title": "鬼屋",
    "description": "向官方新手模组《鬼屋》(The Haunting) 致敬的中文原创改编：一栋被封死的旧宅，墙里有声音，前任租客失踪。",
    "intro": "房产经纪人罗兰先生把一串黄铜钥匙推到你面前：『槐树街 17 号，前任租客失踪三周了。邻居说墙里有声音。你只需要进去看看，确认房子还能不能租。』\n\n（KP 提示：本剧本共十一幕，适合新手。正文为中文原创改编，不包含官方原文。建议关闭随机事件，按玩家调查顺序在“幕”页触发。）",
    "outro": "你们离开槐树街时，天刚亮。罗兰先生在门口等着，手里攥着合同。你回头看了一眼，二楼的窗帘动了一下，但那里没有人。\n\n钥匙在你口袋里，比昨天更冷。",
    "sanReward": "1d10",
    "events": [
        "s_th_prologue",
        "s_th_neighborhood",
        "s_th_ground_floor",
        "s_th_bedroom",
        "s_th_attic",
        "s_th_research",
        "s_th_basement",
        "s_th_hidden_room",
        "s_th_night_watch",
        "s_th_exorcism",
        "s_th_aftermath",
    ],
    "acts": [
        {"act": 1, "event": "s_th_prologue", "title": "委托",
         "summary": "罗兰委托调查槐树街17号；前任租客失踪，邻居说墙里有声音。",
         "trigger": "玩家在事务所接受委托、检查合同或询问罗兰时。",
         "sideEvents": ["s_th_side_client"]},
        {"act": 2, "event": "s_th_neighborhood", "title": "槐树街 17 号",
         "summary": "旧宅外观、邻居、信箱；确认房子多年无人住满。",
         "trigger": "玩家到达槐树街、询问邻居或检查信箱时。",
         "sideEvents": ["s_th_side_dog", "s_th_side_letters"]},
        {"act": 3, "event": "s_th_ground_floor", "title": "一楼",
         "summary": "白布家具、冷壁炉、被衣柜堵住的地下室门；墙里有三下敲击。",
         "trigger": "玩家进入一楼、检查客厅/壁炉或移开衣柜时。",
         "sideEvents": ["s_th_side_letters"]},
        {"act": 4, "event": "s_th_bedroom", "title": "二楼卧室",
         "summary": "整齐的床、蒙布镜子、伊莱亚斯的日记；床下有皮带与血迹。",
         "trigger": "玩家上二楼、读日记、掀镜子或检查床时。",
         "sideEvents": ["s_th_side_mirror"]},
        {"act": 5, "event": "s_th_attic", "title": "阁楼",
         "summary": "维尔家族相册、铜护符、剪报与孩子画的‘墙里人’。",
         "trigger": "玩家上阁楼、查旧箱子或读剪报时。",
         "sideEvents": []},
        {"act": 6, "event": "s_th_research", "title": "旧记录",
         "summary": "图书馆与教堂记录：维尔未被埋葬、封于宅内；盐铁与真名可束缚。",
         "trigger": "玩家去图书馆/教堂查记录、问牧师时。",
         "sideEvents": []},
        {"act": 7, "event": "s_th_basement", "title": "地下室",
         "summary": "玻璃罐、鼠群、新砖墙与墙后的呼吸。",
         "trigger": "玩家下地下室、搜查酒窖或拆砖时。",
         "sideEvents": ["s_th_side_rats"]},
        {"act": 8, "event": "s_th_hidden_room", "title": "砖墙后的房间",
         "summary": "暗室、椅子上的遗骸、胸口骨匕首；匕首是锚点。",
         "trigger": "玩家拆开砖墙、进入暗室或检查遗骸/匕首时。",
         "sideEvents": []},
        {"act": 9, "event": "s_th_night_watch", "title": "守夜",
         "summary": "夜里钟停、床响、维尔从墙里走出；可守夜/查卧室/入睡。",
         "trigger": "玩家决定留下过夜、天黑后守屋时。",
         "sideEvents": []},
        {"act": 10, "event": "s_th_exorcism", "title": "驱逐",
         "summary": "维尔现身；烧遗骸/念束缚咒/盐铁封墙/逃跑。",
         "trigger": "玩家正面面对维尔、准备驱逐或摧毁锚点时。",
         "sideEvents": []},
        {"act": 11, "event": "s_th_aftermath", "title": "余波",
         "summary": "天亮后处理罗兰、合同、钥匙与旧宅；决定结局与后续钩子。",
         "trigger": "事件解决/天亮后，玩家找罗兰、处理现场或离开时。",
         "sideEvents": ["s_th_side_client"]},
    ],
}
with open(os.path.join(SCENARIOS, "the_haunting.json"), "w", encoding="utf-8") as f:
    json.dump(scenario, f, ensure_ascii=False, indent=2)
    f.write("\n")

# update scenario index
sp = os.path.join(SCENARIOS, "index.json")
si = json.load(open(sp, encoding="utf-8"))
scens = si.setdefault("scenarios", [])
if "the_haunting.json" not in scens:
    scens.append("the_haunting.json")
json.dump(si, open(sp, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
open(sp, "a", encoding="utf-8").write("\n")

print("haunting:", len(scenario["events"]), "acts,", len(NEW), "new events")
