package com.evcharge.entity.agent.config;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 代理服务包配置表;
 * @author : Jay
 * @date : 2025-2-17
 */
@TargetDB("evcharge_agent")
public class AgentServicePackConfigEntity extends BaseEntity implements Serializable{
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
     * 单价
     */
    public BigDecimal price ;
    /**
     * 周期时间 根据服务包的计费周期来计算，比如服务包是月度巡检，cycle_count=12，则产生12个月的月度巡检费用
     */
    public Integer cycle_count ;
    /**
     * 比率
     */
    public BigDecimal rate ;
    /**
     * 备注
     */
    public String remark ;
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
    public static AgentServicePackConfigEntity getInstance() {
        return new AgentServicePackConfigEntity();
    }
}