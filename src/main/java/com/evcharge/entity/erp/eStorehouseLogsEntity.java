package com.evcharge.entity.erp;

import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 进销存系统：仓库出入库记录;
 *
 * @author : JED
 * @date : 2023-1-10
 */
public class eStorehouseLogsEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 仓库id
     */
    public long storehouse_id;
    /**
     * 类型：1-入库，2-出库，3-盘点
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
    public static eStorehouseLogsEntity getInstance() {
        return new eStorehouseLogsEntity();
    }
}
