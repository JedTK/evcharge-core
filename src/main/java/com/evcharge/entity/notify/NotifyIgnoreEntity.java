package com.evcharge.entity.notify;

import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 通知忽略配置，特定站点/设备不需要通知;
 *
 * @author : JED
 * @date : 2024-9-25
 */
@TargetDB("evcharge_notify")
public class NotifyIgnoreEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 唯一编码：{前缀}.{站点编码/设备序列号/其他}
     */
    public String unique_code;
    /**
     * 原通知配置代码
     */
    public String origin_config_code;
    //endregion

    /**
     * 获得一个实例
     */
    public static NotifyIgnoreEntity getInstance() {
        return new NotifyIgnoreEntity();
    }

    /**
     * 是否忽略
     *
     * @param unique_code        设备序列号/站点ID等等能用于识别的唯一码
     * @param origin_config_code 源配置代码
     * @return
     */
    public boolean ignore(String unique_code, String origin_config_code) {
        int v = initCache().getInt(String.format("Notify:Ignore:exist:%s_%s", unique_code, origin_config_code), -1);
        if (v == -1) {
            boolean ignore = this
                    .cache(String.format("Notify:Ignore:%s_%s", unique_code, origin_config_code))
                    .where("unique_code", unique_code)
                    .where("origin_config_code", origin_config_code)
                    .exist();
            initCache().set(String.format("Notify:Ignore:exist:%s_%s", unique_code, origin_config_code), ignore ? 1 : 0);
            return ignore;
        }
        return v == 1;
    }
}
