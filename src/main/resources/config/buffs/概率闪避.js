var inner_name = "测试闪避";
var enable = true;

var lore = "这是这个buff的具体描述，测试闪避"
var role_condition = "defender"
var priority= 0;  // 必写
// 想只要自己闪避应该放在 hold_buff
var calculate_type = "before_damage"; // 必写
var chance = 0.5;

function run(TwDamageEvent) {
    // 可以自己在内部扩展生效条件
    var attacker = TwDamageEvent.getAttacker();
    var defender = TwDamageEvent.getDefender();
    var random = new Random();
    if (random.nextDouble() < chance) {
        attacker.sendMessage("对方闪避了");
        defender.sendMessage("~闪避~")
        // 可以播放个声音啥的
        // 双方互换位置
        var l = attacker.getLocation();
        attacker.teleport(defender.getLocation());
        defender.teleport(l);
        return false;  // 后续不执行
    }
    // 没有闪避则后续执行
    return true;
}