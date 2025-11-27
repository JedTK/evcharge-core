package com.evcharge.entity.station;


import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.MapUtil;

import java.io.Serializable;
import java.util.Map;

/**
 * 充电桩合作方类型;
 *
 * @author : JED
 * @date : 2025-1-15
 */
public class CSPartnerTypeEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 合作方类型名
     */
    public String partner_type_name;
    /**
     * 合作方类型代码
     */
    public String partner_type_code;
    /**
     * 备注
     */
    public String remark;
    /**
     * 上级合作方类型代码
     */
    public String parent_code;
    /**
     * 状态：0-删除，1-正常
     */
    public int status;
    /**
     * 创建时间戳
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     */
    public static CSPartnerTypeEntity getInstance() {
        return new CSPartnerTypeEntity();
    }

    /**
     * 获取合作模式名字
     *
     * @param partner_type_code 合作模式代码
     * @return 运营模型名字
     */
    public String getTypeName(String partner_type_code) {
        return getTypeName(partner_type_code, true);
    }

    /**
     * 获取合作模式名字
     *
     * @param partner_type_code 合作模式代码
     * @param inCache      优先从缓存中获取
     * @return 运营模型名字
     */
    public String getTypeName(String partner_type_code, boolean inCache) {
        this.field("id,partner_type_name,partner_type_code");
        if (inCache) this.cache(String.format("BaseData:CSPartnerType:%s:TypeName", partner_type_code));
        Map<String, Object> data = this
                .where("partner_type_code", partner_type_code)
                .find();
        if (data.isEmpty()) return "";
        return MapUtil.getString(data, "partner_type_name");
    }
}
