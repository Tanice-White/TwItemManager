package io.github.tanice.twItemManager.manager.pdc.impl;

import io.github.tanice.twItemManager.constance.key.AttributeKey;
import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeCalculateSection;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeType;
import io.github.tanice.twItemManager.manager.pdc.type.BuffActiveCondition;
import io.github.tanice.twItemManager.manager.pdc.type.DamageType;
import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static io.github.tanice.twItemManager.constance.key.ConfigKey.*;
import static io.github.tanice.twItemManager.util.Logger.logWarning;
import static io.github.tanice.twItemManager.util.Tool.enumMapToString;

/**
 * BUFF的抽象
 * 只有 AttributionCalculateSection 中的 BEFORE_DAMAGE 、 AFTER_DAMAGE 、 TIMER 需要执行 execute 方法
 * 其中只有 TIMER 需要 cd属性
 * 其余计算类都能变为 AttributePDC 中
 */
@Getter
public class BuffPDC extends CalculablePDC {
    @Serial
    private static final long serialVersionUID = 1L;

    /* js配置文件名 */
    private String jsName;
    /* 激活间隔(s) */
    protected int cd;
    private String jsPath;
    private transient Context jsContext;
    private String jsContent;

    /* buff描述(用于自动生成 lore) */
    @Getter
    private String lore;

    /* buff生效的角色条件 */
    @Getter
    private BuffActiveCondition buffActiveCondition;

    /* buff 是否启用 */
    @Getter
    private boolean enable;

    /* 额外添加 */
    private String particle;
    private Color particleColor;
    private int particleNum;

    public BuffPDC() {
        super();
    }

    public BuffPDC(AttributeCalculateSection s) {
        super(s);
    }

    public BuffPDC(@NotNull String innerName, @NotNull ConfigurationSection cfg) {
        super(innerName, AttributeCalculateSection.valueOf(cfg.getString(ACS, "OTHER").toUpperCase()), cfg);
        jsName = "default";
        jsPath = null;
        enable = cfg.getBoolean(ENABLE, true);
        cd = cfg.getInt(CD, -1);
        lore = cfg.getString(LORE, "buff: " + innerName);
        buffActiveCondition = BuffActiveCondition.valueOf(cfg.getString(BUFF_ACTIVE_CONDITION, "all").toUpperCase());
    }

    public BuffPDC(@NotNull String jsName, @NotNull Path jsPath) {
        this.jsName = jsName;
        this.jsPath = jsPath.toAbsolutePath().toString();
        this.initializeJS();
        this.readJSVariables();
    }

    @Override
    public @NotNull String toString() {
        return "BuffPDC{" +
                "priority=" + priority + ", " +
                "jsName=" + jsName + ".js, " +
                "buffInnerName=" + innerName + ", " +
                "attributeCalculateSection=" + attributeCalculateSection + ", " +
                "attribute-addition=" + enumMapToString(vMap) +
                "type-addition=" + enumMapToString(tMap) +
                "endTimeStamp=" + endTimeStamp + ", " +
                "chance=" + chance + ", " +
                "cd=" + cd + ", " +
                "duration=" + duration +
                "}";
    }

    @Override
    public @NotNull Map<AttributeKey, String> toLoreMap() {
        return Map.of();
    }

    /**
     * 传入的参数为: TwDamageEvent 类, 含 attacker(LivingEntity攻击方) defender(LivingEntity被攻击方) damage(伤害值) List(3)[防御前减伤, 护甲值, 防御后减伤]
     * 如果类型是 OTHER，除了EntityPDC之外一般为bug
     * 类型 TIMER 会根据 cd 和 duration 持续伤害或标记
     * BEFORE_DAMAGE 会在伤害计算前执行 此时 damage List 无意义
     * BETWEEN_DAMAGE_AD_DEFENCE 会在伤害计算后，防御减伤计算前执行 均有意义
     * AFTER_DAMAGE 会在伤害计算后执行 此时 damage 表示最终伤害  List 无意义
     */
    public Object execute(Object params) {
        this.initializeJS();
        try {
            Value runFunction = jsContext.getBindings("js").getMember("run");
            if (runFunction != null && runFunction.canExecute()) {
                return runFunction.execute(params).asBoolean();
            } else logWarning("buff: " + innerName + " 的 run 函数无法执行");
            return true;
        } catch (Exception e) {
            logWarning("js执行错误(后续计算继续执行): " + e.getMessage());
            return true;
        }
    }

    /**
     * 初始化JavaScript执行环境
     */
    private void initializeJS() {
        jsContext = Context.newBuilder("js")
                .allowAllAccess(true)
                .option("engine.WarnInterpreterOnly", "false")
                .hostClassLoader(getClass().getClassLoader())
                .build();
        try {
            jsContent = Files.readString(Path.of(jsPath));
            // Bukkit 核心类
            jsContext.eval("js",
                    """
                            var Random = Java.type('java.util.Random');
                            
                            var Player = Java.type('org.bukkit.entity.Player');
                            var LivingEntity = Java.type('org.bukkit.entity.LivingEntity');
                            var Entity = Java.type('org.bukkit.entity.Entity');
                            var Particle = Java.type('org.bukkit.Particle');
                            var Color = Java.type('org.bukkit.Color');
                            var Location = Java.type('org.bukkit.Location');
                            var Sound = Java.type('org.bukkit.Sound');
                            
                            var TwDamageEvent = Java.type('io.github.tanice.twItemManager.event.TwDamageEvent');
                            """
            );

            jsContext.eval("js", jsContent);
        } catch (IOException e) {
            logWarning("Failed to read js file: " + jsPath);
        }
    }

    /**
     * 从JS文件中读取变量并初始化类属性
     */
    private void readJSVariables() {
        innerName = getScriptValue(INNER_NAME, "未读取到变量");
        enable = getScriptValue(ENABLE, true);
        priority = getScriptValue(PRIORITY, Integer.MAX_VALUE);
        chance = getScriptValue(CHANCE, 0D);
        cd = getScriptValue(CD, 0);
        duration = getScriptValue(DURATION, 0);
        String acs = getScriptValue(ACS, "other");
        attributeCalculateSection = AttributeCalculateSection.valueOf(acs.toUpperCase());
        lore = getScriptValue(LORE, "buff: " + innerName);
        String bac = getScriptValue(BUFF_ACTIVE_CONDITION, "all");
        buffActiveCondition = BuffActiveCondition.valueOf(bac.toUpperCase());
        for (AttributeType at : AttributeType.values()) {
            vMap.put(at, getScriptValue(at.toString().toLowerCase(), 0D));
        }
        for (DamageType dt : DamageType.values()) {
            tMap.put(dt, getScriptValue(dt.toString().toLowerCase(), 0D));
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getScriptValue(String variableName, T defaultValue) {
        try {
            Value value = jsContext.getBindings("js").getMember(variableName);
            if (value == null || value.isNull()) {
                return defaultValue;
            }
            // 处理基本类型
            if (defaultValue instanceof String) {
                return (T) value.asString();
            } else if (defaultValue instanceof Integer) {
                return (T) Integer.valueOf(value.asInt());
            } else if (defaultValue instanceof Double) {
                return (T) Double.valueOf(value.asDouble());
            } else if (defaultValue instanceof Boolean) {
                return (T) Boolean.valueOf(value.asBoolean());
            }
            // 其他类型尝试转换
            return (T) value.as(defaultValue.getClass());
        } catch (Exception e) {
            logWarning("JS变量转换失败 [" + variableName + "]: " + e.getMessage());
            return defaultValue;
        }
    }
}
