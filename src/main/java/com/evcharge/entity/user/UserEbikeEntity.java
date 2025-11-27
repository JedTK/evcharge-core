package com.evcharge.entity.user;

import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.MapUtil;

import java.io.Serializable;
import java.util.Map;

/**
 * 用户电动车表;
 *
 * @author : Jay
 * @date : 2022-10-18
 */
public class UserEbikeEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 用户ID
     */
    public long uid;
    /**
     * 品牌id
     */
    public long brand_id;
    /**
     * 系列id
     */
    public String series_title;
    /**
     * 电车型号
     */
    public String model_title;
    /**
     * 省份
     */
    public long province_code;
    /**
     * 是否默认 1=默认 0=否
     */
    public int is_default;
    /**
     * 地区
     */
    public long city_code;
    /**
     * 车牌
     */
    public String ebike_number;
    /**
     * 车牌图片
     */
    public String ebike_number_pic;
    /**
     * 充电器允许最大充电功率
     */
    public double charger_max_power;
    /**
     * 购买时间
     */
    public long buy_time ;
    /**
     * 备注
     */
    public String memo;
    /**
     * 是否删除
     */
    public int is_del;
    /*
     * 缺失数据
     * */
    public String empty_info;
    /**
     * 创建时间
     */
    public long create_time;

    /**
     * 更新时间
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static UserEbikeEntity getInstance() {
        return new UserEbikeEntity();
    }

    /**
     * 获取用户默认的车辆信息
     *
     * @param uid
     * @return
     */
    public UserEbikeEntity getDefaultWithUid(long uid) {
        return this.cache(String.format("User:%s:Ebike:Default", uid))
                .where("uid", uid)
                .where("is_del",0)
                .order("is_default DESC,create_time DESC")
                .findEntity();
    }

    /**
     * 获取爱车中最大充电功率
     *
     * @param uid
     * @return
     */
    public double getChargerMaxPowerWithUid(long uid) {
        Map<String, Object> data = this
                .cache(String.format("User:%s:Ebike:TopChargerMaxPower", uid))
                .field("id,charger_max_power")
                .where("uid", uid)
                .order("charger_max_power DESC")
                .find();
        if (data.isEmpty()) return 0.0;
        return MapUtil.getDouble(data, "charger_max_power");
    }
}