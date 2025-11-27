package com.evcharge.entity.megadata;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 充电桩数据;
 *
 * @author : JED
 * @date : 2023-8-21
 */
public class MDChargeStaionEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 分配的充电桩唯一编码
     */
    public String cs_code;
    /**
     * 自定义充电桩编码
     */
    public String custom_cs_code;
    /**
     * 名称
     */
    public String name;
    /**
     * 状态：0=删除，1=正常，2=在建
     */
    public int status;
    /**
     * 省（省份）
     */
    public String province;
    /**
     * 省份代码
     */
    public String province_code;
    /**
     * 市（城市
     */
    public String city;
    /**
     * 市代码
     */
    public String city_code;
    /**
     * 区（行政区划）
     */
    public String district;
    /**
     * 区代码
     */
    public String district_code;
    /**
     * 街道/城镇
     */
    public String street;
    /**
     * 街道代码
     */
    public String street_code;
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
    public String address;
    /**
     * 经度
     */
    public double lon;
    /**
     * 纬度
     */
    public double lat;
    /**
     * 充电位总数
     */
    public int deviceSocketCount;
    /**
     * 充电位使用中数量
     */
    public int deviceSocketUsingCount;
    /**
     * 充电位空闲中数量
     */
    public int deviceSocketIdleCount;
    /**
     * 充电位占用中数量
     */
    public int deviceSocketOccupiedCount;
    /**
     * 充电位异常数量
     */
    public int deviceSocketErrorCount;
    /**
     * 总充电次数
     */
    public long chargingTotalCount;
    /**
     * 本月充电次数
     */
    public long chargingMonthCount;
    /**
     * 昨日充电次数
     */
    public long chargingYesterdayCount;
    /**
     * 累计充电用户数
     */
    public long chargingUserCount;
    /**
     * 监视器设备数
     */
    public int monitorTotalCount;
    /**
     * 监视器在线数
     */
    public int monitorOnlineCount;
    /**
     * 充电桩使用率
     */
    public double useRate;
    /**
     * 组织代码，所属组织
     */
    public String organize_code;
    /**
     * 正式上线时间
     */
    public long online_time;
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
    public static MDChargeStaionEntity getInstance() {
        return new MDChargeStaionEntity();
    }
}
