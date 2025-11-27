package com.evcharge.entity.user;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 用户投诉记录;
 * @author : JED
 * @date : 2023-10-13
 */
public class UserComplaintLogEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 用户ID
     */
    public long uid ;
    /**
     * 订单类型
     */
    public int type ;
    /**
     * 订单编号
     */
    public String ordersn ;
    /**
     * 投诉时间
     */
    public long complaint_date ;
    /**
     * 投诉原因
     */
    public String complaint_reason ;
    /**
     * 备注
     */
    public String memo ;
    /**
     * 状态
     */
    public int status ;
    /**
     * 创建时间
     */
    public long create_time ;
    /**
     * 更新时间
     */
    public long update_time ;

    //endregion
    /**
     * 获得一个实例
     * @return
     */
    public static UserComplaintLogEntity getInstance() {
        return new UserComplaintLogEntity();
    }
}