package com.evcharge.entity.sys;


import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.common;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 街道设置;
 *
 * @author : JED
 * @date : 2023-1-4
 */
public class SysStreetEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 自增列
     */
    public int street_id;
    /**
     * 街道代码
     */
    public String street_code;
    /**
     * 父级区代码
     */
    public String area_code;
    /**
     * 街道名称
     */
    public String street_name;
    /**
     * 简称
     */
    public String short_name;
    /**
     * 经度
     */
    public String lng;
    /**
     * 纬度
     */
    public String lat;
    /**
     * 排序
     */
    public int sort;
    /**
     * 创建时间
     */
    public LocalDateTime create_time;
    /**
     * 修改时间
     */
    public LocalDateTime update_time;
    /**
     * 备注
     */
    public String memo;
    /**
     * 状态
     */
    public int status;
    /**
     * 租户ID
     */
    public String tenant_code;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static SysStreetEntity getInstance() {
        return new SysStreetEntity();
    }

    /**
     * 根据城市编码和行政区域名，查询行政区域信息
     *
     * @param area_code 行政区域编码
     * @return
     */
    public List<SysStreetEntity> getWithAreaCode(String area_code) {
        return this.cache(String.format("BaseData:SysArea:%s", area_code))
                .where("area_code", area_code)
                .selectList();
    }

    /**
     * 根据城市编码和行政区域名，查询行政区域信息
     *
     * @param street_name 街道名
     * @param area_code   行政区域编码
     * @return
     */
    public SysStreetEntity getWithStreetName(String street_name, String area_code) {
        return this.cache(String.format("BaseData:SysArea:%s:%s", area_code, street_name))
                .where("area_code", area_code)
                .where("street_name", street_name)
                .findEntity();
    }

    /**
     * 根据 省、市、区、街道 中文信息 获取街道信息
     *
     * @param province_name 省 中文
     * @param city_name     市 中文
     * @param district_name 区 中文
     * @param street_name   街道 中文
     * @return
     */
    public SysStreetEntity getStreetFromNames(String province_name, String city_name, String district_name, String street_name) {
        SysProvinceEntity provinceEntity = SysProvinceEntity.getInstance().getWithProvinceName(province_name);
        if (provinceEntity == null) return null;

        SysCityEntity cityEntity = SysCityEntity.getInstance().getWithCityName(city_name, provinceEntity.province_code);
        if (cityEntity == null) return null;

        SysAreaEntity areaEntity = SysAreaEntity.getInstance().getWithAreaName(district_name, cityEntity.city_code);
        if (areaEntity == null) return null;

        return getWithStreetName(street_name, areaEntity.area_code);
    }

    /**
     * 通过街道代码查询信息
     *
     * @param street_code 街道代码
     */
    public SysStreetEntity getWithCode(String street_code) {
        try {
            return this.cache(String.format("BaseData:SysStreet:%s", street_code))
                    .where("street_code", street_code)
                    .findEntity();
        } catch (Exception e) {
            LogsUtil.error(e, this.getClass().getSimpleName(), "无法获取街道：%s", street_code);
        }
        return null;
    }

    /**
     * 根据省代码查询街道代码列表
     *
     * @param province_codes 市代码,多个值用英文逗号隔开
     * @return 街道代码列表
     */
    public String[] getListWithProvinceCodes(String province_codes) {
        String[] city_code_list = SysCityEntity.getInstance().getListWithProvinceCode(province_codes);
        StringBuilder city_codes = new StringBuilder();
        for (String s : city_code_list) {
            city_codes.append(s).append(",");
        }
        if (city_codes.length() > 0) city_codes.deleteCharAt(city_codes.length() - 1);
        String[] area_code_list = SysAreaEntity.getInstance().getListWithCityCodes(city_codes.toString());
        return this.cache(String.format("BaseData:SysProvince:%s:Streets", common.md5(province_codes)))
                .whereIn("area_code", area_code_list)
                .selectForStringArray("street_code");
    }

    /**
     * 根据市代码查询街道代码列表
     *
     * @param city_codes 市代码，多个值用英文逗号隔开
     * @return 街道代码列表
     */
    public String[] getListWithCityCodes(String city_codes) {
        return this.cache(String.format("BaseData:SysCity:%s:StreetCodes", common.md5(city_codes)))
                .whereIn("area_code", SysAreaEntity.getInstance().getListWithCityCodes(city_codes))
                .selectForStringArray("street_code");
    }

    /**
     * 根据行政区域代码查询街道代码列表
     *
     * @param area_codes 行政区域代码，多个值用英文逗号隔开
     * @return 街道代码列表
     */
    public String[] getListWithAreaCodes(String area_codes) {
        return this.cache(String.format("BaseData:SysArea:%s:StreetCodes", common.md5(area_codes)))
                .whereIn("area_code", area_codes)
                .selectForStringArray("street_code");
    }


    /**
     * 本地化（生成json数据）
     *
     * @return
     */
    public JSONArray localize(String area_code) {
        JSONArray array = new JSONArray();
        List<SysStreetEntity> list = this.getWithAreaCode(area_code);
        for (SysStreetEntity entity : list) {
            JSONObject json = new JSONObject();
            json.put("value", entity.street_code);
            json.put("text", entity.street_name);
            array.add(json);
        }
        return array;
    }
}
