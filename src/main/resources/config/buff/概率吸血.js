var inner_name = "测试吸血";
var enable = true;

var lore = "这是这个buff的具体描述，测试吸血"
var role_condition = "attacker"  // 和你将buff放在hold_buff attack_buff defence_buff 息息相关，共同作用
var priority= 1;  // 必写
var calculate_type = "after_damage"; // 必写
var duration = 118;

function run(TwDamageEvent) {
    var attacker = TwDamageEvent.getAttacker();
    var damage = TwDamageEvent.getDamage();
    var x = damage * 0.4;
    attacker.setHealth(Math.min(attacker.getMaxHealth(), attacker.getHealth() + x))
    attacker.sendMessage("吸血: " + x);
    return true;
}