package com.evcharge.entity.erp;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 进销存系统：仓库盘点;
 *
 * @author : JED
 * @date : 2023-1-9
 */
public class eStorehouseInventoryEntity extends BaseEntity implements Serializable {
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
     * 仓库id
     */
    public long storehouse_id;
    /**
     * 盘点人
     */
    public long op_admin_id;
    /**
     * 盘点时间戳
     */
    public long op_time;
    /**
     * 状态：-1-删除，0-草稿，1-待审核，2-审核通过，3=审核不通过
     */
    public int status;
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
    public static eStorehouseInventoryEntity getInstance() {
        return new eStorehouseInventoryEntity();
    }
}
