package com.evcharge.entity.sys;

import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.utils.MapUtil;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SysGlobalConfigEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * ID
     */
    public int id;
    /**
     * 父级ID
     */
    public long parent_id;
    /**
     * 配置中文名
     */
    public String name_text;
    /**
     * 配置名
     */
    public String name_code;
    /**
     * 配置值
     */
    public String value;
    /**
     * 状态：0不启用，1激活
     */
    public int status;
    /**
     * 是否应用于前端，0=否，1是
     */
    public int client_enabled;
    /**
     * 值的类型
     */
    public String value_type;
    /**
     * 说明
     */
    public String desc;
    /**
     * 索引排序
     */
    public long sort_index;
    /**
     * 创建时间
     */
    public long create_time;
    /**
     * 更新时间
     */
    public long update_time;

    //endregion

    private static SysGlobalConfigEntity _this;

    /**
     * 获取一个实例
     */
    public static SysGlobalConfigEntity getInstance() {
        if (_this == null) _this = new SysGlobalConfigEntity();
        return _this;
    }

    /**
     * 获取所有配置
     */
    public static Map<String, Object> allConfig() {
        Map<String, Object> config = getInstance().initCache().getMap("GlobalConfig:ALL");
        if (config == null || config.isEmpty()) {
            List<Map<String, Object>> list = getInstance().field("parent_id,name_text,name_code,value")
                    .where("status", 1)
                    .page(1, 1000)
                    .select();
            if (list.isEmpty()) return new LinkedHashMap<>();

            config = new LinkedHashMap<>();
            for (Map<String, Object> item : list) {
                String name = MapUtil.getString(item, "name_code");
                config.put(name, item);
            }
            getInstance().initCache().setMap("GlobalConfig:ALL", config, 86400000 * 7);
        }
        return config;
    }

    /**
     * 获取所有配置
     */
    public static Map<String, Object> getConfigWithGroupName(String group_name) {
        Map<String, Object> config = getInstance().initCache().getMap(String.format("GlobalConfig:%s", group_name));
        if (config == null || config.isEmpty()) {
            List<Map<String, Object>> list = getInstance()
                    .field("parent_id,name_text,name_code,value")
                    .where("group_name", group_name)
                    .where("status", 1)
                    .where("client_enabled", 1)
                    .order("sort_index DESC,create_time DESC")
                    .page(1, 1000)
                    .select();
            if (list.isEmpty()) return new LinkedHashMap<>();

            config = new LinkedHashMap<>();
            for (Map<String, Object> item : list) {
                String name = MapUtil.getString(item, "name_code");
                config.put(name, item.get("value"));
            }
            getInstance().initCache().setMap(String.format("GlobalConfig:%s", group_name), config, 86400000 * 7);
        }
        return config;
    }

    /**
     * 获取指定配置
     *
     * @param name 配置名
     */
    public static String getString(String name) {
        return getString(name, "");
    }

    /**
     * 获取指定配置
     *
     * @param name         配置名
     * @param defaultValue 默认值
     */
    public static String getString(String name, String defaultValue) {
        String value = DataService.getMainCache().getString(String.format("GlobalConfig:%s", name), null);
        if (value == null) {
            Map<String, Object> data = DataService.getMainDB()
                    .name(getInstance().theTableName())
                    .where("name_code", name)
                    .where("status", 1)
                    .find();
            if (data.isEmpty()) return defaultValue;

            value = MapUtil.getString(data, "value");
            DataService.getMainCache().set("GlobalConfig:" + name, value);
        }
        return value;
    }

    /**
     * 获取指定配置
     *
     * @param name         配置名
     * @param defaultValue 默认值
     */
    public static boolean getBool(String name, boolean defaultValue) {
        String obj = getString(name, "");
        if (obj == null || obj == "") return defaultValue;
        if ("0".equals(obj)) return false;
        return true;
    }

    /**
     * 获取指定配置
     *
     * @param name 配置名
     */
    public static int getInt(String name) {
        return getInt(name, 0);
    }

    /**
     * 获取指定配置
     *
     * @param name         配置名
     * @param defaultValue 默认值
     */
    public static int getInt(String name, int defaultValue) {
        String obj = getString(name, "");
        if (obj == null || obj == "") return defaultValue;
        return Integer.valueOf(obj);
    }

    /**
     * 获取指定配置
     *
     * @param name 配置名
     */
    public static long getLong(String name) {
        return getLong(name, 0);
    }

    /**
     * 获取指定配置
     *
     * @param name         配置名
     * @param defaultValue 默认值
     */
    public static long getLong(String name, long defaultValue) {
        String obj = getString(name, "");
        if (obj == null || obj == "") return defaultValue;
        return Long.valueOf(obj);
    }

    /**
     * 获取指定配置
     *
     * @param name 配置名
     */
    public static float getFloat(String name) {
        return getFloat(name, 0.0F);
    }

    /**
     * 获取指定配置
     *
     * @param name         配置名
     * @param defaultValue 默认值
     */
    public static float getFloat(String name, float defaultValue) {
        String obj = getString(name, "");
        if (obj == null || obj == "") return defaultValue;
        return Float.valueOf(obj);
    }

    /**
     * 获取指定配置
     *
     * @param name 配置名
     */
    public static double getDouble(String name) {
        return getDouble(name, 0.0);
    }

    /**
     * 获取指定配置
     *
     * @param name         配置名
     * @param defaultValue 默认值
     */
    public static double getDouble(String name, double defaultValue) {
        String obj = getString(name, "");
        if (obj == null || obj == "") return defaultValue;
        return Double.valueOf(obj);
    }

    /**
     * 获取指定配置
     *
     * @param name 配置名
     */
    public static BigDecimal getBigDecimal(String name) {
        return getBigDecimal(name, new BigDecimal(0));
    }

    /**
     * 获取指定配置
     *
     * @param name         配置名
     * @param defaultValue 默认值
     */
    public static BigDecimal getBigDecimal(String name, BigDecimal defaultValue) {
        return getBigDecimal(name, 18, RoundingMode.HALF_UP, defaultValue);
    }

    /**
     * 获取指定配置
     *
     * @param name         配置名
     * @param defaultValue 默认值
     */
    public static BigDecimal getBigDecimal(String name, int newScale, RoundingMode r, BigDecimal defaultValue) {
        String obj = getString(name, "");
        if (obj == null || obj == "") return defaultValue;
        return new BigDecimal(obj).setScale(newScale, r);
    }
}
