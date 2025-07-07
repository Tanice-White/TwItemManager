package io.github.tanice.twItemManager.manager.pdc.impl;

import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeCalculateSection;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeType;
import io.github.tanice.twItemManager.manager.pdc.type.BuffActiveCondition;
import io.github.tanice.twItemManager.manager.pdc.type.DamageType;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
public class BuffPDC extends CalculablePDC implements Cloneable {
    @Serial
    private static final long serialVersionUID = 1L;

    /* js配置文件名 */
    private String jsName;
    private String jsPath;
    private transient Context jsContext;
    private String jsContent;

    /* 激活间隔(s) */
    protected int cd;

    /* buff生效的角色条件 */
    @Getter
    private BuffActiveCondition buffActiveCondition;

    /* buff 是否启用 */
    @Getter
    private boolean enable;

    /* buff 还能生效的时间 - 数据库读取使用, 其他情况不允许读取 */
    @Getter
    @Setter
    private long deltaTime;

    /* 属性结束时间(负数则永续) - 非js使用 */
    @Getter
    @Setter
    private long endTimeStamp;

    /* 激活几率 */
    @Getter
    @Setter
    private double chance;

    /* 持续时间 - 均使用(Timer类只使用这个做判断) */
    @Getter
    @Setter
    private int duration;

    /* 额外添加 */
    private String particle;
    private Color particleColor;
    private int particleNum;

    /* 非实体显示的Lore */
    private List<String> lore;

    /**
     * 序列化使用
     */
    public BuffPDC() {
        super();
    }

    public BuffPDC(@NotNull String innerName, @NotNull ConfigurationSection cfg) {
        super(innerName, AttributeCalculateSection.valueOf(cfg.getString(ACS, "OTHER").toUpperCase()), cfg);
        jsName = "default";
        jsPath = null;
        enable = cfg.getBoolean(ENABLE, true);
        cd = cfg.getInt(CD, -1);
        deltaTime = -1;
        endTimeStamp = -1;
        /* 覆写 */
        chance = cfg.getDouble(CHANCE, 1D);
        duration = cfg.getInt(DURATION, -1);
        /* 非偶数duration提示 */
        if (duration >= 0 && duration % 2 != 0) {
            duration--;
            logWarning("duration必须是偶数，否则该buff会永续");
        }
        lore = cfg.getStringList(LORE);
        buffActiveCondition = BuffActiveCondition.valueOf(cfg.getString(BUFF_ACTIVE_CONDITION, "all").toUpperCase());
    }

    public BuffPDC(@NotNull String jsName, @NotNull Path jsPath) {
        this.jsName = jsName;
        this.jsPath = jsPath.toAbsolutePath().toString();
        this.initializeJS();
        this.readJSVariables();
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

    public boolean isTimer() {
        return this.attributeCalculateSection == AttributeCalculateSection.TIMER;
    }

    @Override
    public BuffPDC clone() {
        try {
            BuffPDC clone = (BuffPDC) super.clone();
            // 复制不可变字段
            clone.jsName = this.jsName;
            clone.cd = this.cd;
            clone.jsPath = this.jsPath;
            clone.jsContent = this.jsContent;
            clone.buffActiveCondition = this.buffActiveCondition;
            clone.enable = this.enable;
            clone.particle = this.particle;
            clone.particleNum = this.particleNum;
            // 复制可变字段
            clone.attributeTypeModifiers = this.attributeTypeModifiers.clone();
            clone.damageTypeModifiers = this.damageTypeModifiers.clone();
            // 重置瞬态字段
            clone.jsContext = null;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public @NotNull String toString() {
        return "BuffPDC{" +
                "priority=" + priority + ", " +
                "jsName=" + jsName + ".js, " +
                "buffInnerName=" + innerName + ", " +
                "attributeCalculateSection=" + attributeCalculateSection + ", " +
                "attribute-addition=" + enumMapToString(attributeTypeModifiers) +
                "type-addition=" + enumMapToString(damageTypeModifiers) +
                "endTimeStamp=" + endTimeStamp + ", " +
                "chance=" + chance + ", " +
                "cd=" + cd + ", " +
                "duration=" + duration + ", " +
                "deltaTime=" + deltaTime +
                "}";
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
        lore = getScriptValue(LORE, List.of(innerName));

        String acs = getScriptValue(ACS, "other");
        attributeCalculateSection = AttributeCalculateSection.valueOf(acs.toUpperCase());

        String bac = getScriptValue(BUFF_ACTIVE_CONDITION, "all");
        buffActiveCondition = BuffActiveCondition.valueOf(bac.toUpperCase());

        for (AttributeType at : AttributeType.values()) {
            attributeTypeModifiers.put(at, getScriptValue(at.toString().toLowerCase(), 0D));
        }
        for (DamageType dt : DamageType.values()) {
            damageTypeModifiers.put(dt, getScriptValue(dt.toString().toLowerCase(), 0D));
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
            // 对 List<String> 的支持
            } else if (defaultValue instanceof List) {
                if (!value.hasArrayElements()) return defaultValue;

                List<String> resultList = new ArrayList<>();
                long arraySize = value.getArraySize();
                for (long i = 0; i < arraySize; i++) {
                    Value element = value.getArrayElement(i);
                    resultList.add(element.asString());
                }
                return (T) resultList;
            }
            return (T) value.as(defaultValue.getClass());
        } catch (Exception e) {
            logWarning("JS变量转换失败 [" + variableName + "]: " + e.getMessage());
            return defaultValue;
        }
    }
}
