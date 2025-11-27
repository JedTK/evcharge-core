package com.evcharge.entity.mktchannels;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 渠道信息;
 * @author : Jay
 * @date : 2025-4-17
 */
@TargetDB("evcharge_mktchannels")
public class ChannelInfoEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 省
     */
    public String province ;
    /**
     * 市
     */
    public String city ;
    /**
     * 区
     */
    public String district ;
    /**
     * 街道
     */
    public String street ;
    /**
     * 地址
     */
    public String address ;
    /**
     * 渠道名称
     */
    public String name ;
    /**
     * 商务名称
     */
    public long business_id ;
    /**
     * 渠道等级
     */
    public String channel_level ;
    /**
     * 渠道类型
     */
    public String channel_type ;
    /**
     * 联系人
     */
    public String contact ;
    /**
     * 手机号码
     */
    public String phone ;
    /**
     * 职位
     */
    public String position ;
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
    public static ChannelInfoEntity getInstance() {
        return new ChannelInfoEntity();
    }
}