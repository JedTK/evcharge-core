package com.evcharge.entity.agent.agent;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 代理映射表;
 * @author : Jay
 * @date : 2025-3-4
 */
@TargetDB("evcharge_agent")
public class AgentToOrganizeEntity extends BaseEntity implements Serializable{
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
     * 税务主体 1=一般纳税人 2=小规模纳税人
     */
    public int tax_subject ;
    /**
     * 税率
     */
    public BigDecimal tax_rate ;
    /**
     * 银行手续费
     */
    public BigDecimal bank_handling_fee ;
    /**
     * 状态 0=启用 1=弃用
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
    public static AgentToOrganizeEntity getInstance() {
        return new AgentToOrganizeEntity();
    }
}