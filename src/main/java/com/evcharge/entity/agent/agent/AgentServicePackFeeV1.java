package com.evcharge.entity.agent.agent;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 代理收益(费用)明细表;
 * @author : Jay
 * @date : 2025-2-14
 */
@Deprecated
public class AgentServicePackFeeV1 extends BaseEntity implements Serializable{
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
     * 提现订单
     */
    public String withdraw_order ;
    /**
     * 服务包编码
     */
    public String service_code ;
    /**
     * 日期 yyyy-mm
     */
    public String month ;
    /**
     * 日期时间戳
     */
    public long month_time ;
    /**
     * 金额
     */
    public BigDecimal amount ;
    /**
     * 状态 1=未提现 2=提现申请中  3=已提现
     */
    public int status ;
    /**
     * 备注
     */
    public String remark ;
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
    public static AgentServicePackFeeV1 getInstance() {
        return new AgentServicePackFeeV1();
    }
}