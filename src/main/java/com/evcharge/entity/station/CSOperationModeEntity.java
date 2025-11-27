package com.evcharge.entity.station;


import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.MapUtil;

import java.io.Serializable;
import java.util.Map;

/**
 * 充电桩运营模式;
 *
 * @author : JED
 * @date : 2025-1-15
 */
public class CSOperationModeEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 运营模式
     */
    public String op_mode_name;
    /**
     * 运营模式代码
     */
    public String op_mode_code;
    /**
     * 备注
     */
    public String remark;
    /**
     * 上级运营代码
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
    public static CSOperationModeEntity getInstance() {
        return new CSOperationModeEntity();
    }

    /**
     * 获取运营模式名字
     *
     * @param op_mode_code 运营模式代码
     * @return 运营模型名字
     */
    public String getModeName(String op_mode_code) {
        return getModeName(op_mode_code, true);
    }

    /**
     * 获取运营模式名字
     *
     * @param op_mode_code 运营模式代码
     * @param inCache      优先从缓存中获取
     * @return 运营模型名字
     */
    public String getModeName(String op_mode_code, boolean inCache) {
        this.field("id,op_mode_name,op_mode_code");
        if (inCache) this.cache(String.format("BaseData:CSOperationMode:%s:ModeName", op_mode_code));
        Map<String, Object> data = this
                .where("op_mode_code", op_mode_code)
                .find();
        if (data.isEmpty()) return "";
        return MapUtil.getString(data, "op_mode_name");
    }
}
