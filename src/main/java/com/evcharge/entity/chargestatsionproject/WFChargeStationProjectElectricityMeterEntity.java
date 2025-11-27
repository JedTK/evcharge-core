package com.evcharge.entity.chargestatsionproject;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 充电桩项目电表;
 *
 * @author : JED
 * @date : 2023-10-17
 */
public class WFChargeStationProjectElectricityMeterEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 项目ID，自定义生成
     */
    public String projectId;
    /**
     * 电表编号
     */
    public String meterNo;
    /**
     * 最大承载功率
     */
    public double maxPower;
    /**
     * 省（省份）
     */
    public String province;
    /**
     * 市（城市
     */
    public String city;
    /**
     * 区（行政区划）
     */
    public String districts;
    /**
     * 街道/城镇
     */
    public String street;
    /**
     * 城市社区和乡村
     */
    public String communities;
    /**
     * 道路
     */
    public String roads;
    /**
     * 城市和农村地址
     */
    public String addresses;
    /**
     * 经度
     */
    public double lon;
    /**
     * 纬度
     */
    public double lat;
    /**
     * 创建者id
     */
    public long creater_id;
    /**
     * 备注
     */
    public String remark;
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
     *
     * @return
     */
    public static WFChargeStationProjectElectricityMeterEntity getInstance() {
        return new WFChargeStationProjectElectricityMeterEntity();
    }
}
