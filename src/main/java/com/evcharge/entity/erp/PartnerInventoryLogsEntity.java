package com.evcharge.entity.erp;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 合伙人：库存日志;
 *
 * @author : JED
 * @date : 2023-1-11
 */
public class PartnerInventoryLogsEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 管理员id
     */
    public long admin_id;
    /**
     * 组织id
     */
    public long organize_id;
    /**
     * 类型：1-入库，2-出库
     */
    public int typeId;
    /**
     * (可选)关联订单号
     */
    public String orderSN;
    /**
     * (可选)扩展数据
     */
    public String extraData;
    /**
     * 备注
     */
    public String remark;
    /**
     * 创建时间
     */
    public long create_time;

    //endregion

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static PartnerInventoryLogsEntity getInstance() {
        return new PartnerInventoryLogsEntity();
    }
}
