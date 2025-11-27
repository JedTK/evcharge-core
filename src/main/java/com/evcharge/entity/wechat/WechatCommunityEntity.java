package com.evcharge.entity.wechat;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 微信社群表;
 * @author : JED
 * @date : 2023-9-12
 */
public class WechatCommunityEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 群名
     */
    public String title ;
    /**
     * 群二维码
     */
    public String group_qrcode ;
    /**
     * 按钮链接
     */
    public String button_url ;
    /**
     * 经度
     */
    public String lon ;
    /**
     * 纬度
     */
    public String lat ;
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
    public static WechatCommunityEntity getInstance() {
        return new WechatCommunityEntity();
    }
}