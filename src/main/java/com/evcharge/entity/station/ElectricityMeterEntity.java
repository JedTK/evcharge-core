package com.evcharge.entity.station;


import com.evcharge.entity.device.DeviceEntity;
import com.evcharge.entity.station.bill.EMeterToCStationEntity;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.utils.MapUtil;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 电表 关联充电桩;
 *
 * @author : JED
 * @date : 2022-12-27
 */
public class ElectricityMeterEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 父级id
     */
    public long parent_id ;
    /**
     * 充电站ID
     */
    public String CSId;
    /**
     * 电表图片
     */
    public String meter_img;
    /**
     * 报电证明
     */
    public String proof_img ;
    /**
     * 电表属性 1=自营电表 2=物业电表
     */
    public int meter_attr;
    /**
     * 电表类型 1=两相电 2=三相电
     */
    public int meter_type;
    /**
     * 电表名
     */
    public String title;
    /**
     * 省
     */
    public String province;
    /**
     * 市
     */
    public String city;
    /**
     * 区
     */
    public String district;
    /**
     * 街道
     */
    public String street;
    /**
     * 电表地址
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
     * 电表编号 用于查询电费
     */
    public String meterNo;
    /**
     * 电表物理号
     */
    public String meter_code;
    /**
     * 最大承载功率
     */
    public double maxPower;
    /**
     * 当前使用中的功率（参考值）
     */
    public double usePower;
    /**
     * 状态0关闭1启用
     */
    public int status;
    /**
     * 平台代码
     */
    public String platform_code;
    /**
     * 组织代码
     */
    public String organize_code;
    /**
     * 创建时间
     */
    public long create_time;


    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ElectricityMeterEntity getInstance() {
        return new ElectricityMeterEntity();
    }

    /**
     * 获取绑定的设备id列表
     *
     * @param meterId 电表ID
     * @return
     */
    public List<Long> getBindDeviceIdList(long meterId) {
        List<Long> deviceIdList = DataService.getMainCache().getList(String.format("ChargeStation:ElectricMeter:%s:Device", meterId));
        if (deviceIdList == null || deviceIdList.isEmpty()) {
            List<Map<String, Object>> deviceList = DeviceEntity.getInstance()
                    .field("id")
                    .where("meterId", meterId)
                    .select();
            if (deviceList.isEmpty()) return new LinkedList<>();

            deviceIdList = new LinkedList<>();
            Iterator it = deviceList.iterator();
            while (it.hasNext()) {
                Map<String, Object> nd = (Map<String, Object>) it.next();
                deviceIdList.add(MapUtil.getLong(nd, "id"));
            }
            if (!deviceIdList.isEmpty()) {
                DataService.getMainCache().getList(String.format("ChargeStation:ElectricMeter:%s:Device", meterId));
            }
        }
        return deviceIdList;
    }

    /**
     * 获取电表信息
     *
     * @param meterNo 电表编号
     * @return
     */
    public ElectricityMeterEntity getWithMeterNo(String meterNo) {
        return getWithMeterNo(meterNo, true);
    }

    /**
     * 获取电表信息
     *
     * @param meterNo 电表编号
     * @param inCache 优先从缓存中获得
     * @return
     */
    public ElectricityMeterEntity getWithMeterNo(String meterNo, boolean inCache) {
        ElectricityMeterEntity meterEntity = new ElectricityMeterEntity();
        if (inCache) meterEntity.cache(String.format("ElectricityMeter:%s:Detail", meterNo));
        meterEntity = meterEntity.where("meterNo", meterNo)
                .findEntity();
        return meterEntity;
    }

    /**
     * 获取电表绑定的充电桩ID列表
     *
     * @param meterId 电表ID
     * @return
     */
    public String[] getBindCSIdList(long meterId) {
        return EMeterToCStationEntity.getInstance()
                .where("meter_id", meterId)
                .where("status", 1)
                .selectForStringArray("cs_id");
    }

    /**
     * 通过站点编码获取电表信息
     *
     * @param CSId
     * @return
     */
    public ElectricityMeterEntity getByCSId(String CSId) {
        return this.cache(String.format("BaseData:ChargeStationMeter:%s", CSId))
                .field("m.*")
                .alias("m")
                .join(EMeterToCStationEntity.getInstance().theTableName(), "mc", "m.id = mc.meter_id")
                .where("cs_id", CSId)
                .findEntity();
    }
}
