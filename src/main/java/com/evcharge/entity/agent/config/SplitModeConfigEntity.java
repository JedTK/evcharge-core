package com.evcharge.entity.agent.config;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 分账模式配置表;
 * @author : Jay
 * @date : 2025-2-14
 */
@TargetDB("evcharge_agent")
public class SplitModeConfigEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 模式名称
     */
    public String title ;
    /**
     * 模式类型：B1，B2等等
     */
    public String type ;
    /**
     * 编码 B1，B2等 type字段可能弃用
     */
    public String code ;
    /**
     * 结算模式 T+1/T+30
     */
    public String settlement_model ;
    /**
     * 分账比率
     */
    public BigDecimal split_rate ;
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
    public static SplitModeConfigEntity getInstance() {
        return new SplitModeConfigEntity();
    }
}