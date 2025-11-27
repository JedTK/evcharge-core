package com.evcharge.entity.chargestatsionproject;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 分润信息配置;
 * @author : Jay
 * @date : 2024-3-1
 */
public class WFChargeStationProfitsInfoEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 项目id
     */
    public String project_id ;
    /**
     * 分润角色 中间人/举荐人(推荐人)
     */
    public int profit_role ;
    /**
     * 分润类型
     */
    public int profit_type ;
    /**
     * 渠道用户ID
     */
    public long channel_id ;
    /**
     * 分润模式 1=月结电位分润、2=手动电量分润、3=实时电量分润、4=实时消费金额(计次充电、内购卡金额)百分比分润
     */
    public int profit_mode ;
    /**
     * 收益单价，按单价收益时用
     */
    public double income_price ;
    /**
     * 收益比率，按百分比收益时用
     */
    public double income_ratio ;
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
    public static WFChargeStationProfitsInfoEntity getInstance() {
        return new WFChargeStationProfitsInfoEntity();
    }
}
