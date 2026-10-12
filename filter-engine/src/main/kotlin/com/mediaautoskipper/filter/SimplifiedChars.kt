package com.mediaautoskipper.filter

/**
 * Characters that exist only in Simplified Chinese.
 *
 * Deliberately conservative: a character is listed only when its Simplified form
 * is not a valid Traditional character and is not used in Japanese (shinjitai).
 * Forms shared with Japanese kanji (国, 学, 会, 体, 区, 医 ...) or that are also
 * valid Traditional characters (后, 里, 干, 台 ...) are left out on purpose, so a
 * Japanese or Traditional title made of such characters is never flagged.
 * The cost is that a Simplified title built only from shared characters is missed.
 */
internal object SimplifiedChars {
    private const val CHARS =
        "爱们这个为说话语请读谁课调谈论设许证识译诗认让记讲议访该详误诉词试诚诞询诸谅谊谋谓谜谢谣谦谨谱讥讨训讯讶评" +
            "见观规视觉览亲时间问闻门开关闭闪闲阳阴阵际陆队难欢险随隐页顶项顺须顾顿颁颂预领颇频题颜额风飘飞饭饮饱饰饿馆馈驱驶驻驾骂骄验骑骗骤" +
            "书买卖车轮转轻较载输辆辈边过还远运进递选逻迟适达迁遗邮邻违连逊辅辉辩轨轩软轰" +
            "办务动劳势励单卢卫厅历压厌叶叹吨听启员响团园围图圆圣场坏块坚坛垒垦垫壳处备复头夺奋奖妇妈娱婴孙宁宠审宪宫对导寻尔尘尝层岁岛岗币师帅带帧帮广庆库应废异弃张弹归录彻态怀总恳恶悬惊惩惯愿懒战执扩扫扬扰抚抛抢护报拟拥拦拨择挚挥损换摄摇摊撑敌敛斩时显晓晕晒暂机杀杂权极构枪柜栈样档桥梦检毕毙" +
            "汉汤沟没沪泪泽测浑浓润渐渔满滚滞滤灵灾灿炼烁烂烛烦热爷牍牵犹狈狮狱猎获玛环现琐瑶电疗疯监盘睁矿码砖础确祸离种积穷竖竞笔笋筑筛签简箩篮类粮紧" +
            "纠红纤约级纪纯纱纲纳纵纷纸纹纺纽线练组细织终绍经结绕绘给络绝统继续维绵综绿缀缓缘编缚缝缩网罗罚翘耸聪肃肠肤肿胀胆胜脉脏脑脚腊腾舰艰艺节芦苏苹茧荐荡荣药莲萝蓝蔼虏虑虾蚁蛮蝇补装裤" +
            "贝贞负贡财责贤败货质贩贪购贯贴贵贷贸费贺贼资赋赌赏赐赔赖赚赛赞赠赢赵趋跃" +
            "酿释鉴针钉钓钟钢钥钱钻铁铃铜银铺链销锁锅锋错锡键锦镇镜长" +
            "鱼鲁鲜鸟鸡鸣鸭鸽齐齿龄龙龟" +
            "优伟传伤价众侧侦俩俭债倾偿储儿兰养军农冯决冻况凉减凑凤凭击刚创删别刹剂剑剧劝" +
            "乡习乌亏亚产亩亲仅仓仪伞刘则"

    val set: Set<Char> = CHARS.toSet()
}
