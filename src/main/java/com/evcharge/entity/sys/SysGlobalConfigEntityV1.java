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

public class SysGlobalConfigEntityV1 extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * ID
     */
    public int id ;
    /**
     * 父级ID
     */
    public long parent_id ;
    /**
     * 配置中文名
     */
    public String name_text ;
    /**
     * 配置名
     */
    public String name_code ;
    /**
     * 配置值
     */
    public String value ;
    /**
     * 状态：0不启用，1激活
     */
    public int status ;
    /**
     * 是否应用于前端，0=否，1是
     */
    public int client_enabled ;
    /**
     * 值的类型
     */
    public String value_type ;
    /**
     * 说明
     */
    public String desc ;
    /**
     * 索引排序
     */
    public long sort_index ;
    /**
     * 创建时间
     */
    public long create_time ;
    /**
     * 更新时间
     */
    public long update_time ;

    //endregion

    private static SysGlobalConfigEntityV1 _this;

    /**
     * 获取一个实例
     *
     * @return
     */
    public static SysGlobalConfigEntityV1 getInstance() {
        if (_this == null) _this = new SysGlobalConfigEntityV1();
        return _this;
    }

    /**
     * 获取所有配置
     *
     * @return
     */
    public Map<String, Object> allConfig() {
        Map<String, Object> config = initCache().getObj("GlobalConfig:ALL");
        if (config == null || config.size() == 0) {
            List<Map<String, Object>> list = this.field("parent_id,name_text,name_code,value")
                    .where("status", 1)
                    .page(1, 1000)
                    .select();
            if (list.size() == 0) return new LinkedHashMap<>();

            config = new LinkedHashMap<>();
            Iterator it = list.iterator();
            while (it.hasNext()) {
                Map<String, Object> item = (Map<String, Object>) it.next();

                String name = MapUtil.getString(item, "name");
                config.put(name, item);
            }
            initCache().setObj("GlobalConfig:ALL", config, 86400000 * 7);
        }
        return config;
    }

    /**
     * 获取指定配置
     *
     * @param name 配置名
     * @return
     */
    public String getString(String name) {
        return getString(name, "");
    }

    /**
     * 获取指定配置
     *
     * @param name         配置名
     * @param defaultValue 默认值
     * @return
     */
    public String getString(String name, String defaultValue) {
        String value = initCache().getString(String.format("GlobalConfig:%s", name), null);
        if (value == null) {
            Map<String, Object> data = DataService.getMainDB()
                    .name(this.theTableName())
                    .where("name_code", name)
                    .where("status", 1)
                    .find();
            if (data.size() == 0) return defaultValue;

            value = MapUtil.getString(data, "value");
            initCache().set("GlobalConfig:" + name, value);
        }
        return value;
    }

    /**
     * 获取指定配置
     *
     * @param name         配置名
     * @param defaultValue 默认值
     * @return
     */
    public boolean getBool(String name, boolean defaultValue) {
        String obj = getString(name, "");
        if (obj == null || obj == "") return defaultValue;
        if ("0".equals(obj)) return false;
        return true;
    }

    /**
     * 获取指定配置
     *
     * @param name 配置名
     * @return
     */
    public int getInt(String name) {
        return getInt(name, 0);
    }

    /**
     * 获取指定配置
     *
     * @param name         配置名
     * @param defaultValue 默认值
     * @return
     */
    public int getInt(String name, int defaultValue) {
        String obj = getString(name, "");
        if (obj == null || obj == "") return defaultValue;
        return Integer.valueOf(obj);
    }

    /**
     * 获取指定配置
     *
     * @param name 配置名
     * @return
     */
    public long getLong(String name) {
        return getLong(name, 0);
    }

    /**
     * 获取指定配置
     *
     * @param name         配置名
     * @param defaultValue 默认值
     * @return
     */
    public long getLong(String name, long defaultValue) {
        String obj = getString(name, "");
        if (obj == null || obj == "") return defaultValue;
        return Long.valueOf(obj);
    }

    /**
     * 获取指定配置
     *
     * @param name 配置名
     * @return
     */
    public float getFloat(String name) {
        return getFloat(name, 0.0F);
    }

    /**
     * 获取指定配置
     *
     * @param name         配置名
     * @param defaultValue 默认值
     * @return
     */
    public float getFloat(String name, float defaultValue) {
        String obj = getString(name, "");
        if (obj == null || obj == "") return defaultValue;
        return Float.valueOf(obj);
    }

    /**
     * 获取指定配置
     *
     * @param name 配置名
     * @return
     */
    public double getDouble(String name) {
        return getDouble(name, 0.0);
    }

    /**
     * 获取指定配置
     *
     * @param name         配置名
     * @param defaultValue 默认值
     * @return
     */
    public double getDouble(String name, double defaultValue) {
        String obj = getString(name, "");
        if (obj == null || obj == "") return defaultValue;
        return Double.valueOf(obj);
    }

    /**
     * 获取指定配置
     *
     * @param name 配置名
     * @return
     */
    public BigDecimal getBigDecimal(String name) {
        return getBigDecimal(name, new BigDecimal(0));
    }

    /**
     * 获取指定配置
     *
     * @param name         配置名
     * @param defaultValue 默认值
     * @return
     */
    public BigDecimal getBigDecimal(String name, BigDecimal defaultValue) {
        return getBigDecimal(name, 18, RoundingMode.HALF_UP, defaultValue);
    }

    /**
     * 获取指定配置
     *
     * @param name         配置名
     * @param defaultValue 默认值
     * @return
     */
    public BigDecimal getBigDecimal(String name, int newScale, RoundingMode r, BigDecimal defaultValue) {
        String obj = getString(name, "");
        if (obj == null || obj == "") return defaultValue;
        return new BigDecimal(obj).setScale(newScale, r);
    }
}
