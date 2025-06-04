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

import java.nio.file.Files;
import java.nio.file.Path;

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
    /* js配置文件名 */
    private String jsName;
    /* 激活间隔(s) */
    protected int cd;
    private Path jsPath;
    private transient Context jsContext;

    public BuffPDC() {
        super();
    }

    public BuffPDC(@NotNull String innerName, @NotNull ConfigurationSection cfg) {
        super(innerName, AttributeCalculateSection.valueOf(cfg.getString(ACS, "OTHER").toUpperCase()), cfg.getConfigurationSection(ATTR_SECTION_KEY));
        jsName = "default";
        jsPath = null;
        cd = cfg.getInt("cd", -1);
    }

    public BuffPDC(@NotNull String jsName, @NotNull Path jsPath) {
        this.jsName = jsName;
        this.jsPath = jsPath;
        initializeJS();
        readJSVariables();
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
        } catch (Exception e) {
            logWarning("Buff: " + innerName + " 初始化错误");
        }
    }

    /**
     * 从JS文件中读取变量并初始化类属性
     */
    private void readJSVariables() {
        innerName = getScriptValue(INNER_NAME, "default");
        priority = getScriptValue(PRIORITY, Integer.MAX_VALUE);
        chance = getScriptValue(CHANCE, 0D);
        cd = getScriptValue(CD, 0);
        duration = getScriptValue(DURATION, 0);
        attributeCalculateSection = getScriptValue(ACS, AttributeCalculateSection.OTHER);
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
            if (value == null || value.isNull()) return defaultValue;
            return (T) value;
        } catch (Exception e) {
            logWarning("JS变量:" + variableName + "读取错误");
            return defaultValue;
        }
    }
}
