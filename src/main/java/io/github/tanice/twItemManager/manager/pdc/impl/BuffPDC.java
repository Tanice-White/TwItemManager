package io.github.tanice.twItemManager.manager.pdc.impl;

import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeCalculateSection;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeType;
import io.github.tanice.twItemManager.manager.pdc.type.DamageType;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static io.github.tanice.twItemManager.util.Logger.logWarning;
import static io.github.tanice.twItemManager.util.Tool.enumMapToString;
import static io.github.tanice.twItemManager.util.Tool.mapToString;

@Getter
public class BuffPDC extends CalculablePDC {
    /* js配置文件名 */
    private String jsName;
    /* 属性结束时间(负数则永续) */
    protected long endTimeStamp;
    /* 激活几率 */
    protected double chance;
    /* 激活间隔(s) */
    protected int cd;
    /* 持续时间 */
    protected int duration;

    private Path jsPath;
    private transient Context jsContext;

    public BuffPDC() {
        super();
    }

    public BuffPDC(@NotNull String innerName, @NotNull AttributeCalculateSection acs, @NotNull ConfigurationSection cfg) {
        super(innerName, acs, cfg.getConfigurationSection(ATTR_SECTION_KEY));
        jsName = "default";
        jsPath = null;
        endTimeStamp = 0;
        chance = cfg.getDouble("chance", 0D);
        cd = cfg.getInt("cd", 0);
        duration = cfg.getInt("duration", 0);
    }

    public BuffPDC(@NotNull String jsName, @NotNull Path jsPath) {
        this.jsName = jsName;
        this.jsPath = jsPath;
        initializeJS();  // 获取环境并初始化值
    }

    @Override
    public String toString() {
        return "CalculablePDC{" +
                "priority=" + priority + ", " +
                "jsName=" + jsName + ".js, " +
                "itemInnerName=" + innerName + ", " +
                "attributeCalculateSection=" + attributeCalculateSection + ", " +
                "attribute-addition=" + enumMapToString(vMap) +
                "type-addition=" + enumMapToString(tMap) +
                "endTimeStamp=" + endTimeStamp + ", " +
                "chance=" + chance + ", " +
                "cd=" + cd + ", " +
                "duration=" + duration + ", " +
                "}";
    }

    /**
     * 传入的参数为: attacker(攻击方) defender(被攻击方) damage(伤害值)
     * 如果类型是 OTHER，除了EntityPDC之外一般为bug
     * 类型 TIMER 会根据 cd 和 duration 持续伤害或标记
     * BEFORE_DAMAGE 会在伤害计算前执行 此时 damage 无意义
     * AFTER_DAMAGE 会在伤害计算后执行 此时 damage 表示最终伤害
     */
    public Object execute(Object... params) {
        try {
            Value runFunction = jsContext.getBindings("js").getMember("run");
            if (runFunction != null && runFunction.canExecute()) {
                return runFunction.execute(params).as(Object.class);
            }
            return null;
        } catch (PolyglotException e) {
            throw new RuntimeException("JS execution error", e);
        }
    }

    /**
     * 初始化JavaScript执行环境
     */
    private void initializeJS() {
        try {
            jsContext = Context.newBuilder("js").allowAllAccess(true).allowHostClassLookup(className -> true).build();
            String jsContent = Files.readString(jsPath);
            jsContext.eval("js", jsContent);
            readJSVariables();
            readEnumMapVariables();
            readAttributeCalculateSection();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize JS for buff: " + innerName, e);
        }
    }

    /**
     * 获取JS文件中定义的全局变量
     * @param variableName 变量名
     * @return 变量的值（转换为Java对象），如果不存在则返回null
     */
    private @Nullable Object getJSVariable(String variableName) {
        if (jsContext == null) return null;
        try {
            Value value = jsContext.getBindings("js").getMember(variableName);
            if (value == null || value.isNull()) return null;
            return valueToJavaObject(value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get JS variable: " + variableName, e);
        }
    }

    /**
     * 从JS文件中读取变量并初始化类属性
     */
    private void readJSVariables() {
        Object o;
        // 读取基础属性
        this.innerName = (String) getJSVariable("innerName");
        o = getJSVariable("priority");
        this.priority = (Integer) (o == null ? Integer.MAX_VALUE : o);
        o = getJSVariable("chance");
        this.chance = (Double) (o == null ? 0D : o);
        o = getJSVariable("cd");
        this.cd = (Integer) (o == null ? 0 : o);
        o = getJSVariable("duration");
        this.duration = (Integer) (o == null ? 0 : o);
    }

    /**
     * 读取枚举映射变量
     */
    private void readEnumMapVariables() {
        // 读取vMap
        Value jsVMap = jsContext.getBindings("js").getMember("vMap");
        if (jsVMap != null && jsVMap.hasMembers()) {
            for (AttributeType type : AttributeType.values()) {
                String typeName = type.name();
                if (jsVMap.hasMember(typeName)) {
                    Value value = jsVMap.getMember(typeName);
                    if (value.isNumber()) {
                        vMap.put(type, value.asDouble());
                    }
                }
            }
        }

        // 读取tMap
        Value jsTMap = jsContext.getBindings("js").getMember("tMap");
        if (jsTMap != null && jsTMap.hasMembers()) {
            for (DamageType type : DamageType.values()) {
                String typeName = type.name();
                if (jsTMap.hasMember(typeName)) {
                    Value value = jsTMap.getMember(typeName);
                    if (value.isNumber()) {
                        tMap.put(type, value.asDouble());
                    }
                }
            }
        }
    }

    /**
     * 读取计算区属性
     */
    private void readAttributeCalculateSection() {
        Value jsSection = jsContext.getBindings("js").getMember("attributeCalculateSection");
        if (jsSection != null && jsSection.isString()) {
            String sectionStr = jsSection.asString();
            try {
                this.attributeCalculateSection = AttributeCalculateSection.valueOf(sectionStr);
            } catch (IllegalArgumentException e) {
                logWarning(jsName + ".js 文件中的 attributeCalculateSection 无效");
                this.attributeCalculateSection = AttributeCalculateSection.OTHER;
            }
        }
    }

    /**
     * 将GraalVM的Value对象转换为Java对象
     */
    private Object valueToJavaObject(@NotNull Value value) {
        if (value.isHostObject()) {
            return value.asHostObject();
        } else if (value.isString()) {
            return value.asString();
        } else if (value.isNumber()) {
            if (value.fitsInInt()) {
                return value.asInt();
            } else if (value.fitsInLong()) {
                return value.asLong();
            } else if (value.fitsInDouble()) {
                return value.asDouble();
            } else {
                return value.as(Number.class);
            }
        } else if (value.isBoolean()) {
            return value.asBoolean();
        } else if (value.isDate()) {
            return value.asDate();
        } else if (value.isDuration()) {
            return value.asDuration();
        } else if (value.isTime()) {
            return value.asTime();
        } else if (value.isTimeZone()) {
            return value.asTimeZone();
        } else if (value.isInstant()) {
            return value.asInstant();
        } else if (value.hasArrayElements()) {
            // 数组类型，转换为Java List
            int size = (int) value.getArraySize();
            List<Object> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                list.add(valueToJavaObject(value.getArrayElement(i)));
            }
            return list;
        } else if (value.hasMembers()) {
            // 对象类型，转换为Java Map
            Map<String, Object> map = new HashMap<>();
            for (String key : value.getMemberKeys()) {
                map.put(key, valueToJavaObject(value.getMember(key)));
            }
            return map;
        } else {
            return value;
        }
    }
}
