package com.evcharge.entity.ai;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * AI助手;
 *
 * @author : JED
 * @date : 2025-2-21
 */
public class AIAssistantEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 助手编码
     */
    public String code;
    /**
     * 助手名称
     */
    public String name;
    /**
     * 角色设定，一般为：system
     */
    public String role_name;
    /**
     * 角色内容
     */
    public String role_content;
    /**
     * 默认参数JSON格式
     */
    public String default_params;
    /**
     * 状态：0-删除，1-正常
     */
    public int status;
    /**
     * 默认使用OpenAI配置
     */
    public String config_code;
    /**
     * 默认使用模型名
     */
    public String model_name;
    /**
     * 创建时间戳
     */
    public long create_time;
    /**
     * 更新时间戳
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     */
    public static AIAssistantEntity getInstance() {
        return new AIAssistantEntity();
    }
}
