package com.evcharge.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;

public class DateUtils {

    /**
     * 获取当前月份(1-12)
     *
     * @return 当前月份
     */
    public static int getCurrentMonth() {
        return LocalDate.now().getMonthValue();
    }

    /**
     * 获取指定月份第一天的时间戳（毫秒）
     *
     * @param month 月份(1-12)，如果不传则默认为当前月份
     * @return 时间戳（毫秒）
     */
    public static long getMonthStartTimestamp(Integer month) {
        LocalDateTime dateTime;
        if (month == null) {
            // 如果不传月份，默认使用当前月份
            dateTime = LocalDateTime.now()
                    .withDayOfMonth(1)
                    .withHour(0)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);
        } else {
            // 验证月份是否有效
            if (month < 1 || month > 12) {
                throw new IllegalArgumentException("月份必须在1-12之间");
            }
            // 使用指定月份
            dateTime = LocalDateTime.now()
                    .withMonth(month)
                    .withDayOfMonth(1)
                    .withHour(0)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);
        }
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 获取指定月份最后一天的最后一毫秒时间戳
     *
     * @param month 月份(1-12)，如果不传则默认为当前月份
     * @return 时间戳（毫秒）
     */
    public static long getMonthEndTimestamp(Integer month) {
        LocalDateTime dateTime;
        if (month == null) {
            // 如果不传月份，默认使用当前月份
            dateTime = LocalDateTime.now()
                    .with(TemporalAdjusters.lastDayOfMonth())
                    .withHour(23)
                    .withMinute(59)
                    .withSecond(59)
                    .withNano(999_000_000); // 设置为999毫秒
        } else {
            // 验证月份是否有效
            if (month < 1 || month > 12) {
                throw new IllegalArgumentException("月份必须在1-12之间");
            }
            // 使用指定月份
            dateTime = LocalDateTime.now()
                    .withMonth(month)
                    .with(TemporalAdjusters.lastDayOfMonth())
                    .withHour(23)
                    .withMinute(59)
                    .withSecond(59)
                    .withNano(999_000_000); // 设置为999毫秒
        }
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }


    /**
     * 根据年月字符串（格式：yyyy-M）获取该月第一天零点时间戳
     *
     * @param yearMonth 年月字符串，格式：yyyy-M，例如：2024-1
     * @return 时间戳（毫秒）
     * @throws IllegalArgumentException 如果日期格式不正确
     */
    public static long getMonthStartTimestamp(String yearMonth) {
        try {
            // 解析年月字符串
            YearMonth ym = YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyy-MM"));

            // 构建该月第一天的 LocalDateTime
            LocalDateTime dateTime = ym.atDay(1)
                    .atStartOfDay();

            return dateTime.atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("年月字符串格式必须为'yyyy-MM'，例如'2024-01'", e);
        }
    }


    /**
     * 根据年月字符串（格式：yyyy-M）获取该月最后一天的最后一毫秒时间戳
     *
     * @param yearMonth 年月字符串，格式：yyyy-M，例如：2024-1
     * @return 时间戳（毫秒）
     * @throws IllegalArgumentException 如果日期格式不正确
     */
    public static long getMonthEndTimestamp(String yearMonth) {
        try {
            // 解析年月字符串
            YearMonth ym = YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyy-MM"));

            // 构建该月最后一天的 LocalDateTime，并设置为当天的最后一毫秒
            LocalDateTime dateTime = ym.atEndOfMonth()
                    .atTime(23, 59, 59, 999_000_000);

            return dateTime.atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("年月字符串格式必须为'yyyy-MM'，例如'2024-01'", e);
        }
    }
}