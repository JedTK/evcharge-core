package com.evcharge.entity.agent.config;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 代理分账模式;
 * @author : Jay
 * @date : 2025-2-17
 */
@TargetDB("evcharge_agent")
public class AgentSplitModeEntity extends BaseEntity implements Serializable{
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
     * 分账模式代码
     */
    public String split_code ;
    /**
     * 分账比率
     */
    public BigDecimal split_rate ;
    /**
     * 开始时间
     */
    public long start_time ;
    /**
     * 结束时间
     */
    public long end_time ;
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
    public static AgentSplitModeEntity getInstance() {
        return new AgentSplitModeEntity();
    }


    /**
     * 根据组织代码获取分账比例
     * @param organize_code
     * @return
     */
    public AgentSplitModeEntity getSplitModeByOrganizeCode(String organize_code){
        return this
                .cache(String.format("Agent:%s:SplitMode",organize_code),86400*1000)
                .where("organize_code",organize_code)
                .where("status",0)
                .findEntity();
    }




}