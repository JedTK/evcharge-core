package com.evcharge.utils.range;

/**
 * 通用范围类，用于表示一个数值区间（范围）。
 * 范围由最小值 `min` 和最大值 `max` 确定，适用于任何 `Number` 类型（如 Integer、Double 等）。
 *
 * @param <T> 范型参数，表示数值类型，必须是 `Number` 的子类。
 *            <p>
 *            示例用法：
 *            <pre>
 *                                                            Range<Integer> range = new Range<>(1, 10); // 整数范围
 *                                                            Range<Double> range = new Range<>(1.5, 10.5); // 浮点数范围
 *                                                        </pre>
 */
public class Range<T extends Number> {
    /**
     * 索引，可用于标识起始于结束
     */
    public int index;
    /**
     * 范围的最小值。
     */
    public T min;

    /**
     * 范围的最大值。
     */
    public T max;

    /**
     * 构造方法，用于创建一个范围对象。
     *
     * @param min 最小值，不能为 null。
     * @param max 最大值，不能为 null。
     */
    public Range(T min, T max) {
        this.min = min;
        this.max = max;
    }

    /**
     * 构造方法，用于创建一个范围对象。
     *
     * @param index 索引标识
     * @param min   最小值，不能为 null。
     * @param max   最大值，不能为 null。
     */
    public Range(int index, T min, T max) {
        this.index = index;
        this.min = min;
        this.max = max;
    }
}