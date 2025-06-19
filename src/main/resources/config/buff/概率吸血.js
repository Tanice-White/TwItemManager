var inner_name = "测试吸血";

var priority= 0;  // 必写
var calculate_type = "after_damage"; // 必写
var chance = 0.75;
var cd = 20;
var duration = 119;

function run(TwDamageEvent) {
    var attacker = TwDamageEvent.getAttacker;
    var damage = TwDamageEvent.getDamage();
    var x = damage * 0.4;
    var random = new Random();
    if (random.nextDouble() < chance) attacker.setHealth(Math.min(attacker.getMaxHealth(), attacker.getHealth() + x))
    attacker.sendMessage("吸血: " + x);
    return true;
}