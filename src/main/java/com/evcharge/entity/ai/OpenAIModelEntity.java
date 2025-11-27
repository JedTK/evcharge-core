package com.evcharge.entity.ai;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * OpenAI模型;
 *
 * @author : JED
 * @date : 2025-2-21
 */
public class OpenAIModelEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 配置编码
     */
    public String config_code;
    /**
     * 模型名
     */
    public String model_name;
    /**
     * 默认模型：0-否，1-是
     */
    public int is_default;
    /**
     * 创建时间戳
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     */
    public static OpenAIModelEntity getInstance() {
        return new OpenAIModelEntity();
    }
}
