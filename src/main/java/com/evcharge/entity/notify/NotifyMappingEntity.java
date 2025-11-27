package com.evcharge.entity.notify;


import com.evcharge.enumdata.ENotifyType;
import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;

import java.io.Serializable;
import java.util.List;

/**
 * 通知映射;
 *
 * @author : JED
 * @date : 2024-11-1
 */
@TargetDB("evcharge_notify")
public class NotifyMappingEntity extends BaseEntity implements Serializable {
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
    /**
     * 映射通知配置代码
     */
    public String config_code;
    /**
     * 描述
     */
    public String description;

    //endregion

    /**
     * 获得一个实例
     */
    public static NotifyMappingEntity getInstance() {
        return new NotifyMappingEntity();
    }

    /**
     * 通过配置映射表找到对应的配置通知
     *
     * @param unique_code        设备序列号/站点编码等等
     * @param origin_config_code 源配置代码
     * @param notifyType
     * @return
     */
    public List<NotifyConfigEntity> getList(String unique_code, String origin_config_code, ENotifyType notifyType) {
        int existTag = DataService.getMainCache().getInt(String.format("Notify:Mapping:%s_%s", unique_code, origin_config_code), -1);
        if (existTag == 0) return null;
        if (existTag == -1 && !this.where("unique_code", unique_code)
                .where("origin_config_code", origin_config_code)
                .exist()) {
            DataService.getMainCache().set(String.format("Notify:Mapping:%s_%s", unique_code, origin_config_code), 0);
            return null;
        }

        NotifyConfigEntity configEntity = new NotifyConfigEntity();
        configEntity.field("c.*")
                .alias("c")
                .cache(String.format("Notify:Mapping:Config:%s_%s", unique_code, origin_config_code))
                .join(theTableName(), "m", "c.config_code = m.config_code")
                .where("m.unique_code", unique_code)
                .where("m.origin_config_code", origin_config_code);
        if (notifyType != ENotifyType.NONE) {
            configEntity.where("c.type_id", notifyType.getIndex());
        }
        DataService.getMainCache().set(String.format("Notify:Mapping:%s_%s", unique_code, origin_config_code), 1);
        return configEntity.selectList();
    }
}
