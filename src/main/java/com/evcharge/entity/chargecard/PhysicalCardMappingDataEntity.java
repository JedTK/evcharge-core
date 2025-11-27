package com.evcharge.entity.chargecard;

import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 实体卡数据映射数据;
 *
 * @author : JED
 */
public class PhysicalCardMappingDataEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * UUID原始值（16进制）
     */
    public String uuid;
    /**
     * UUID-10进制
     */
    public String uuid_decimal;
    /**
     * 物理卡号
     */
    public String card_number;
    /**
     * 卡配置spu编码
     */
    public String spu_code;

    //endregion

    /**
     * 获得一个实例
     */
    public static PhysicalCardMappingDataEntity getInstance() {
        return new PhysicalCardMappingDataEntity();
    }
}