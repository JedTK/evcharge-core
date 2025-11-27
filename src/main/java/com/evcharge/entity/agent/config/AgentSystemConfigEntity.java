package com.evcharge.entity.agent.config;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
/**
 * 系统配置文件表;null
 *
 * @author : Jay
 * @date : 2025-11-17
 */
public class AgentSystemConfigEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 编号,;
     */
    public long id;
    /**
     * 标题,;
     */
    public String title;
    /**
     * 配置代码,;
     */
    public String config_code;
    /**
     * 配置值类型,;
     */
    public String value_type;
    /**
     * 配置值,;
     */
    public String config_value;
    /**
     * 配置说明,;
     */
    public String description;
    /**
     * 状态,;
     */
    public int status;
    /**
     * 排序,;
     */
    public int sort;
    /**
     * 创建时间,;
     */
    public long create_time;
    /**
     * 更新时间,;
     */
    public long update_time;
    //endregion
    /**
     * 获得一个实例
     */
    public static AgentSystemConfigEntity getInstance() {
        return new AgentSystemConfigEntity();
    }
}