package io.github.tanice.twItemManager.util;

import io.github.tanice.twItemManager.manager.pdc.type.DamageType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import static io.github.tanice.twItemManager.infrastructure.PDCAPI.getCalculablePDC;
import static io.github.tanice.twItemManager.infrastructure.PDCAPI.getSlot;

public class Tool {
    /**
     * 将 Map 转换为格式化的字符串表示
     * @param map 需要转换的 Map
     * @param <K> 键的类型
     * @param <V> 值的类型
     * @return 格式化后的字符串
     */
    public static <K extends Enum<K>, V> @NotNull String enumMapToString(@Nullable EnumMap<K, V> map) {
        if (map == null || map.isEmpty()) return "{}";

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(entry.getKey().toString().toLowerCase()).append("=").append(entry.getValue());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 处理基本类型数组的重载方法
     * @param map 需要转换的 Map
     * @param <K> 键的类型
     * @return 格式化后的字符串
     */
    public static <K> @NotNull String mapToString1(@Nullable Map<K, double[]> map) {
        if (map == null || map.isEmpty()) return "{}";

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<K, double[]> entry : map.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(entry.getKey()).append("=");
            double[] value = entry.getValue();
            sb.append(Arrays.toString(value));
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 将 Map<String, Long> 转换为字符串，空映射显示为 "0"
     * @param map 要转换的映射，允许为null
     * @return 格式为 "{key1=value1, key2=value2}"，空映射返回 "0"
     */
    public static @NotNull String mapToString2(@Nullable Map<String, Long> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        Long v;
        boolean first = true;
        for (Map.Entry<String, Long> entry : map.entrySet()) {
            if (!first) sb.append(", ");
            v = entry.getValue();
            if (v == null) v = 0L;
            sb.append(entry.getKey()).append("=").append(v);
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 将 Map<String, Long> 转换为字符串，空映射显示为 "0"
     * @param map 要转换的映射，允许为null
     * @return 格式为 "{key1=value1, key2=value2}"，空映射返回 "0"
     */
    public static @NotNull String mapToString3(@Nullable Map<String, Integer> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        Integer v;
        boolean first = true;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (!first) sb.append(", ");
            v = entry.getValue();
            if (v == null) v = 0;
            sb.append(entry.getKey()).append("=").append(v);
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 获取Map中的最大键值对
     */
    public static DamageType getMax(EnumMap<DamageType, Double> map) {
        if (map == null || map.isEmpty()) return DamageType.OTHER;
        DamageType maxKey = null;
        double maxValue = Double.MIN_VALUE;
        for (Map.Entry<DamageType, Double> entry : map.entrySet()) {
            if (entry.getValue() > maxValue) {
                maxValue = entry.getValue();
                maxKey = entry.getKey();
            }
        }
        return maxKey;
    }
}
