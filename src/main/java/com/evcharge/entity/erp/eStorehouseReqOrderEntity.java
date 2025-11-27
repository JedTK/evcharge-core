package com.evcharge.entity.erp;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 进销存系统：仓库调拨订单;
 *
 * @author : JED
 * @date : 2023-1-9
 */
public class eStorehouseReqOrderEntity extends BaseEntity implements Serializable {
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
     * 出库仓库id
     */
    public long storehouse_id_out;
    /**
     * 入库仓库id
     */
    public long storehouse_id_in;
    /**
     * 状态：-1-删除，0-草稿，1-待审核，2-审核通过
     */
    public int status;
    /**
     * 调拨时间
     */
    public long ReqTime;
    /**
     * 物流公司，可选
     */
    public String deliveryCompany;
    /**
     * 物流运单号，可选
     */
    public String deliveryOrderSN;
    /**
     * 备注
     */
    public String remark;
    /**
     * 管理员id
     */
    public long admin_id;
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
    public static eStorehouseReqOrderEntity getInstance() {
        return new eStorehouseReqOrderEntity();
    }
}
