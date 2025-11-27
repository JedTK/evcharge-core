package com.evcharge.entity.erp;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 合伙人：申领设备订单;
 *
 * @author : JED
 * @date : 2023-1-11
 */
public class PartnerReceiveOrderEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 订单号
     */
    public String OrderSN;
    /**
     * 管理员id
     */
    public long admin_id;
    /**
     * 组织id
     */
    public long organize_id;
    /**
     * 状态：-1-删除，0-草稿，1-待审核，2-审核通过，3-审核不通过，4-订单完成
     */
    public int status;
    /**
     * 备注
     */
    public String remark;
    /**
     * 创建时间戳
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static PartnerReceiveOrderEntity getInstance() {
        return new PartnerReceiveOrderEntity();
    }
}
