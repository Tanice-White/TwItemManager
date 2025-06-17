package io.github.tanice.twItemManager.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AlgorithmUtil {
    @Contract(pure = true)
    public static int findMaxLessThanOrEqualIndex(@NotNull List<Double> list, double target) {
        int left = 0;
        int right = list.size() - 1;
        int resultIndex = -1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            double midValue = list.get(mid);
            if (midValue <= target) {
                resultIndex = mid;
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        if (resultIndex < 0) resultIndex = 0;
        else if (resultIndex > list.size() - 1) resultIndex = list.size() - 1;

        return resultIndex;
    }
}
