package com.evcharge.entity.ai;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * OpenAI风格配置;
 *
 * @author : JED
 * @date : 2025-2-21
 */
public class OpenAIConfigEntity extends BaseEntity implements Serializable {
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
     * 配置名称
     */
    public String config_name;
    /**
     * 备注
     */
    public String remark;
    /**
     * api基础Url
     */
    public String base_url;
    /**
     * api秘钥
     */
    public String api_key;
    /**
     * http请求连接超时时间，秒
     */
    public int connect_timeout;
    /**
     * http读取超时时间，秒
     */
    public int read_timeout;
    /**
     * http写入超时时间，秒
     */
    public int write_timeout;
    /**
     * 状态：0-删除，1-正常
     */
    public int status;
    /**
     * 创建时间戳
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     */
    public static OpenAIConfigEntity getInstance() {
        return new OpenAIConfigEntity();
    }
}
