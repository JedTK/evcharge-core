package com.evcharge.entity.agent.agent;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 站点服务包费用明细表;
 * @author : Jay
 * @date : 2025-2-14
 */
@TargetDB("evcharge_agent")
public class AgentStationServicePackFeeEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 组织代码
     */
    public String organize_code ;
    /**
     * 提现订单号
     */
    public String withdraw_order_sn ;
    /**
     * 站点id
     */
    public String CSId ;
    /**
     * 服务包编码
     */
    public String service_code ;
    /**
     * 开始时间
     */
    public long start_time ;
    /**
     * 结束时间
     */
    public long end_time ;
    /**
     * 金额
     */
    public BigDecimal amount ;
    /**
     * 备注
     */
    public String remark ;
    /**
     * 状态 0=未扣费 1=冻结中 2=已扣费
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
    public static AgentStationServicePackFeeEntity getInstance() {
        return new AgentStationServicePackFeeEntity();
    }
}