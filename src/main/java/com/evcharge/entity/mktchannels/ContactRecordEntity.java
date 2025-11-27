package com.evcharge.entity.mktchannels;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 对接记录;
 * @author : Jay
 * @date : 2025-4-17
 */
@TargetDB("evcharge_mktchannels")
public class ContactRecordEntity extends BaseEntity implements Serializable{
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
     * 省
     */
    public String province_code ;
    /**
     * 市
     */
    public String city_code ;
    /**
     * 区
     */
    public String district_code ;
    /**
     * 街道
     */
    public String street_code ;
    /**
     * 地图图片
     */
    public String map_image ;
    /**
     * 商务id
     */
    public String business_id ;
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
     * 状态 0=未对接 1=对接中 2=已接入 -1=受阻碍
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
    public static ContactRecordEntity getInstance() {
        return new ContactRecordEntity();
    }
}