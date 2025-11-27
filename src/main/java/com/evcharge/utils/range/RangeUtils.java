package com.evcharge.utils.range;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * 区间工具类，用于动态生成区间范围。
 * 支持泛型数据类型，可生成整数和浮点数类型的区间。
 * 区间划分支持动态步长计算，同时可根据需求对最大值向上取整。
 */
public class RangeUtils {

    /**
     * 通用方法：动态生成区间列表。
     *
     * @param min       数据的最小值，泛型T，必须实现Number和Comparable接口。
     * @param max       数据的最大值，泛型T。
     * @param numRanges 希望划分的区间数，必须大于0。
     * @param roundTo   向上取整的基准值。例如：
     *                  - 若roundTo=100，则最大值会向上取整到最近的100的倍数。
     *                  - 若roundTo=null或0，则不进行取整。
     * @param roundFunc 向上取整函数，定义取整逻辑。BiFunction类型，接收两个参数（当前值和基准值），返回调整后的值。
     * @param <T>       数据类型，可以是Integer、Double等实现了Number和Comparable接口的类型。
     * @return 返回区间列表，每个区间包含一个最小值和最大值。
     * @throws IllegalArgumentException 如果区间数小于等于0，或最小值大于等于最大值。
     */
//    public static <T extends Number & Comparable<T>> List<Range<T>> generateRanges(T min
//            , T max
//            , int numRanges
//            , T roundTo
//            , BiFunction<T, T, T> roundFunc
//    ) {
//
//        // 参数校验
//        if (numRanges <= 0 || min.compareTo(max) >= 0) {
//            throw new IllegalArgumentException("区间数必须大于0，且最小值必须小于最大值");
//        }
//
//        List<Range<T>> ranges = new LinkedList<>();
//
//        // 如果需要向上取整到基准数，调整 max
//        if (roundTo != null && roundTo.doubleValue() > 0) {
//            max = roundFunc.apply(max, roundTo);
//        }
//
//        // 计算步长
//        double step = (max.doubleValue() - min.doubleValue()) / numRanges;
//
//        // 根据步长生成区间
//        for (int i = 0; i < numRanges; i++) {
//            double lowerBound = min.doubleValue() + i * step;
//            double upperBound = (i == numRanges - 1) ? max.doubleValue() : lowerBound + step;
//
//            // 根据泛型类型进行转换
//            T lower = convertToType(lowerBound, min);
//            T upper = convertToType(upperBound, min);
//
//            ranges.add(new Range<>(i, lower, upper));
//        }
//
//        return ranges;
//    }
    public static <T extends Number & Comparable<T>> List<Range<T>> generateRanges(List<Range<T>> originalRanges
            , T min
            , T max
            , int numRanges
            , T roundTo
            , BiFunction<T, T, T> roundFunc
    ) {
        // 参数校验
        if (numRanges <= 0 || min.compareTo(max) >= 0) {
            throw new IllegalArgumentException("区间数必须大于0，且最小值必须小于最大值");
        }

        // 用于返回最终的区间列表
        List<Range<T>> ranges;
        int currentIndex = 0;
        if (originalRanges == null || originalRanges.isEmpty()) {
            ranges = new LinkedList<>();
        } else {
            ranges = new LinkedList<>(originalRanges);
            currentIndex = originalRanges.get(originalRanges.size() - 1).index + 1;
        }

        // 如果需要向上取整到基准数，调整 max
        if (roundTo != null && roundTo.doubleValue() > 0) {
            max = roundFunc.apply(max, roundTo);
        }

        // 计算步长
        double step = (max.doubleValue() - min.doubleValue()) / numRanges;

        // 根据步长生成区间
        for (int i = 0; i < numRanges; i++) {
            double lowerBound = min.doubleValue() + i * step;
            double upperBound = (i == numRanges - 1) ? max.doubleValue() : lowerBound + step;

            // 根据泛型类型进行转换
            T lower = convertToType(lowerBound, min);
            T upper = convertToType(upperBound, min);

            // 创建新的区间，并设置累计的 index
            Range<T> range = new Range<>(currentIndex++, lower, upper);
            ranges.add(range);  // 将新生成的区间添加到 ranges 列表
        }

        return ranges;  // 返回包含原始区间和新生成区间的完整列表
    }

    /**
     * 生成浮点数类型的区间列表。
     *
     * @param min       数据的最小值。
     * @param max       数据的最大值。
     * @param numRanges 希望划分的区间数。
     * @param roundTo   向上取整的基准值（如100、500）。如果为0，则不向上取整。
     * @return 返回浮点数区间列表，每个区间包含一个最小值和最大值。
     */
    public static List<Range<Double>> generateRangesDouble(double min, double max, int numRanges, double roundTo) {
        return generateRanges(null
                , min
                , max
                , numRanges
                , roundTo
                , (value, base) -> Math.ceil(value / base) * base
        );
    }

    /**
     * 生成浮点数类型的区间列表。
     *
     * @param min       数据的最小值。
     * @param max       数据的最大值。
     * @param numRanges 希望划分的区间数。
     * @param roundTo   向上取整的基准值（如100、500）。如果为0，则不向上取整。
     * @return 返回浮点数区间列表，每个区间包含一个最小值和最大值。
     */
    public static List<Range<Double>> generateRangesDouble(List<Range<Double>> originalRanges, double min, double max, int numRanges, double roundTo) {
        return generateRanges(originalRanges
                , min
                , max
                , numRanges
                , roundTo
                , (value, base) -> Math.ceil(value / base) * base
        );
    }

    /**
     * 生成浮点数类型的区间列表。
     *
     * @param min 数据的最小值。
     * @param max 数据的最大值。
     * @return 返回浮点数区间列表，每个区间包含一个最小值和最大值。
     */
    public static List<Range<Double>> generateRangesDouble(double min, double max) {
        return generateRanges(null
                , min
                , max
                , 4
                , 100.0
                , (value, base) -> Math.ceil(value / base) * base
        );
    }

    /**
     * 生成浮点数类型的区间列表。
     *
     * @param min       数据的最小值。
     * @param max       数据的最大值。
     * @param numRanges 希望划分的区间数。
     * @return 返回浮点数区间列表，每个区间包含一个最小值和最大值。
     */
    public static List<Range<Double>> generateRangesDouble(double min, double max, int numRanges) {
        return generateRanges(null
                , min
                , max
                , numRanges
                , 100.0
                , (value, base) -> Math.ceil(value / base) * base
        );
    }


    /**
     * 生成整数类型的区间列表。
     *
     * @param min 数据的最小值。
     * @param max 数据的最大值。
     * @return 返回整数区间列表，每个区间包含一个最小值和最大值。
     */
    public static List<Range<Integer>> generateRangesInt(int min, int max) {
        return generateRanges(null
                , min
                , max
                , 4
                , 100
                , (value, base) -> (int) Math.ceil((double) value / base) * base
        );
    }

    /**
     * 生成整数类型的区间列表。
     *
     * @param min       数据的最小值。
     * @param max       数据的最大值。
     * @param numRanges 希望划分的区间数。
     * @return 返回整数区间列表，每个区间包含一个最小值和最大值。
     */
    public static List<Range<Integer>> generateRangesInt(int min, int max, int numRanges) {
        return generateRanges(null
                , min
                , max
                , numRanges
                , 100
                , (value, base) -> (int) Math.ceil((double) value / base) * base
        );
    }

    /**
     * 生成整数类型的区间列表。
     *
     * @param min       数据的最小值。
     * @param max       数据的最大值。
     * @param numRanges 希望划分的区间数。
     * @param roundTo   向上取整的基准值（如100、500）。如果为0，则不向上取整。
     * @return 返回整数区间列表，每个区间包含一个最小值和最大值。
     */
    public static List<Range<Integer>> generateRangesInt(int min, int max, int numRanges, int roundTo) {
        return generateRanges(null
                , min
                , max
                , numRanges
                , roundTo
                , (value, base) -> (int) Math.ceil((double) value / base) * base
        );
    }

    /**
     * 生成整数类型的区间列表。
     *
     * @param min       数据的最小值。
     * @param max       数据的最大值。
     * @param numRanges 希望划分的区间数。
     * @param roundTo   向上取整的基准值（如100、500）。如果为0，则不向上取整。
     * @return 返回整数区间列表，每个区间包含一个最小值和最大值。
     */
    public static List<Range<Integer>> generateRangesInt(List<Range<Integer>> originalRanges, int min, int max, int numRanges, int roundTo) {
        return generateRanges(originalRanges
                , min
                , max
                , numRanges
                , roundTo
                , (value, base) -> (int) Math.ceil((double) value / base) * base
        );
    }

    /**
     * 将浮点数值转换为泛型 T 类型。
     *
     * @param value   需要转换的浮点数值。
     * @param example 泛型类型的示例对象，用于判断目标类型。
     * @param <T>     泛型类型。
     * @return 转换后的泛型类型值。
     */
    @SuppressWarnings("unchecked")
    private static <T extends Number> T convertToType(double value, T example) {
        if (example instanceof Integer) {
            return (T) Integer.valueOf((int) value);
        } else if (example instanceof Double) {
            return (T) Double.valueOf(value);
        }
        throw new UnsupportedOperationException("Unsupported type: " + example.getClass().getName());
    }
}