package com.evcharge.entity.wechat;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 微信企业机器人池;
 *
 * @author : JED
 * @date : 2024-8-27
 */
@Deprecated
public class WechatCorpBotPoolEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 组名
     */
    public String group_name;
    /**
     * webhook网址
     */
    public String webhook_url;
    /**
     * (可选)昵称
     */
    public String nick_name;

    //endregion

    /**
     * 获得一个实例
     */
    public static WechatCorpBotPoolEntity getInstance() {
        return new WechatCorpBotPoolEntity();
    }
}
