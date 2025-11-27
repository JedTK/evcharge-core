package com.evcharge.entity.agent.config;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 服务包配置表;
 * @author : Jay
 * @date : 2025-2-14
 */
@TargetDB("evcharge_agent")
public class ServicePackConfigEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 标题
     */
    public String title ;
    /**
     * 编码
     */
    public String service_code ;
    /**
     * 单价
     */
    public BigDecimal price ;
    /**
     * 比例
     */
    public BigDecimal rate ;
    /**
     * 计费单位 station站点 socket插座
     */
    public String billing_unit ;
    /**
     * 计费周期:day-按日， month-按月 quarter-按季 year-按年
     */
    public String billing_cycle ;
    /**
     * 周期数量,如:12个月,4个季度
     */
    public Integer cycle_count ;
    /**
     * 有效期限制：0-无期限
     */
    public int expired_stint ;
    /**
     * 状态 0-禁用 1-启用
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
    public static ServicePackConfigEntity getInstance() {
        return new ServicePackConfigEntity();
    }

    /**
     * 根据服务编码获取信息
     * @param serviceCode String
     * @return
     */
    public ServicePackConfigEntity getConfigByCode(String serviceCode){
        return this
                .cache(String.format("Config:ServicePackConfig:%s",serviceCode),86400*1000)
                .where("service_code",serviceCode)
                .findEntity();
    }


}