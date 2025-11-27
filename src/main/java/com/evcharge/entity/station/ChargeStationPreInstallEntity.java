package com.evcharge.entity.station;


import com.evcharge.entity.sys.SysAreaEntity;
import com.evcharge.entity.sys.SysCityEntity;
import com.evcharge.entity.sys.SysProvinceEntity;
import com.evcharge.entity.sys.SysStreetEntity;
import com.evcharge.enumdata.ECacheTime;
import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.common;

import java.io.Serializable;
import java.util.Map;

/**
 * 充电桩预安装;
 *
 * @author : JED
 * @date : 2022-11-23
 */
public class ChargeStationPreInstallEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 项目ID
     */
    public String projectId;
    /**
     * 标题
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
     * 街道，可能为空字串
     */
    public String street;
    /**
     * 城市社区/乡村
     */
    public String communities;
    /**
     * 路
     */
    public String roads;
    /**
     * 具体地址，门牌
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
     * //状态：0=取消，1=待审核，2=审核通过，3=领取设备中，4=待安装，5=项目竣工
     */
    public int status;
    /**
     * 联系单位
     */
    public String contactsUnitName;
    /**
     * 联系人
     */
    public String contactsName;
    /**
     * 联系电话
     */
    public String contactsPhone;
    /**
     * 预计设备数量
     */
    public int deviceCount;
    /**
     * 施工结构：0-未知，1-雨棚，2-钢架
     */
    public int constructionType;
    /**
     * 施工备注
     */
    public String constructionRemark;
    /**
     * 项目经理人姓名
     */
    public String pmName;
    /**
     * 项目经理人联系电话
     */
    public String pmPhone;
    /**
     * 管理员id
     */
    public long admin_id;
    /**
     * 组织id
     */
    public long organize_id;
    /**
     * 创建时间戳
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static ChargeStationPreInstallEntity getInstance() {
        return new ChargeStationPreInstallEntity();
    }

    /**
     * 根据地址生成项目ID
     *
     * @param province 省份
     * @param city     城市
     * @param area     区域
     * @param street   街道
     * @return
     */
    public String createProjectIdWithAddress(String province, String city, String area, String street) {
        String projectId = "[province][city][area][street][randomInt]";

        Map<String, Object> sysProvinceData = SysProvinceEntity.getInstance()
                .field("province_id,province_code,province_name")
                .cache(String.format("Sys:Address:Simple:%s", province), ECacheTime.YEAR)
                .where("province_name", province)
                .find();
        if (sysProvinceData.size() > 0) {
            projectId = projectId.replace("[province]", MapUtil.getString(sysProvinceData, "province_code").substring(0, 2));
        }

        Map<String, Object> sysCityData = SysCityEntity.getInstance()
                .field("city_id,city_code,city_name")
                .cache(String.format("Sys:Address:Simple:%s:%s", province, city), ECacheTime.YEAR)
                .where("city_name", city)
                .find();
        if (sysCityData.size() > 0) {
            projectId = projectId.replace("[city]", MapUtil.getString(sysCityData, "city_code").substring(0, 4));
        }

        Map<String, Object> sysAreaData = SysAreaEntity.getInstance()
                .field("area_id,area_code,area_name")
                .cache(String.format("Sys:Address:Simple:%s:%s:%s", province, city, area), ECacheTime.YEAR)
                .where("area_name", area)
                .find();
        if (sysAreaData.size() > 0) {
            projectId = projectId.replace("[area]", String.format("%s", MapUtil.getString(sysAreaData, "area_id")));
        }

        Map<String, Object> sysStreetData = SysStreetEntity.getInstance()
                .field("street_id,street_code,street_name")
                .cache(String.format("Sys:Address:Simple:%s:%s:%s:%s", province, city, area, street), ECacheTime.YEAR)
                .where("street_name", street)
                .find();
        if (sysStreetData.size() > 0) {
            projectId = projectId.replace("[street]", String.format("%s", MapUtil.getString(sysStreetData, "street_id")));
        }

        //将未知的补上00
        projectId = projectId.replace("[province]", "00")
                .replace("[city]", "0000")
                .replace("[area]", "0000")
                .replace("[street]", "00000")
                .replace("[randomInt]", String.format("%s", common.randomInt(10000, 99999)));
        return projectId;
    }
}
