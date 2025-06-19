var inner_name = "测试持续掉血";

var priority= 0;  // 必写
var calculate_type = "timer"; // 必写
var chance = 0.75;
var cd = 20;
var duration = 119;

// Timer部分只有run函数有效，其余部分只有属性有效
// 属性部分
var damage= 0;
var armor= 0;
var critical_strike_chance= 0.8;
var critical_strike_damage= 1.5;
var armor_toughness= 0;
var pre_armor_reduction= 0;
var after_armor_reduction= 0;
var mana_cost= -0.1;
var skill_cooldown= -0.1;
// 玩家伤害属性增加
var melee= 0.1;  // 战士伤害
var magic= 0.1;  // 法师伤害
var ranged= 0.1; // 射手伤害
var rouge= 0.1;  // 盗贼伤害
var summon= 0.1; // 召唤伤害
var other= 0; // 其他

// Timer类的参数只有目标实体
function run(livingEntity) {
    if (livingEntity instanceof Player) {
        livingEntity.sendMessage("你在流血");
    }
    return false;  // 返回否表示“禁止后续属性计算”  // Timer类的返回值无效
}