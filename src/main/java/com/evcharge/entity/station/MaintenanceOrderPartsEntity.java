package com.evcharge.entity.station;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 维修零件;
 *
 * @author : JED
 * @date : 2023-7-10
 */
public class MaintenanceOrderPartsEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 零件类型：1-充电设备，2-其他
     */
    public int partType;
    /**
     * 维修记录订单id
     */
    public long orderId;
    /**
     * 其他零件名称，记录不是充电设备的零件
     */
    public String partName;
    /**
     * 其他零件规格，
     */
    public String partSpec;
    /**
     * 数量
     */
    public int count;
    /**
     * 备注
     */
    public String remark;
    /**
     * 状态：0-等待审核，1-待领取，2-待安装，3-安装成功
     */
    public int status;
    /**
     * 申请人管理员id
     */
    public long request_admin_id;
    /**
     * 创建时间戳
     */
    public long create_time;
    /**
     * 更新时间戳
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static MaintenanceOrderPartsEntity getInstance() {
        return new MaintenanceOrderPartsEntity();
    }
}
