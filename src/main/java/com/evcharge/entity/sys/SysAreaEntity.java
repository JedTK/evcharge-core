package com.evcharge.entity.sys;


import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.common;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 地区设置;
 *
 * @author : JED
 * @date : 2023-1-4
 */
@TargetDB("evcharge_rbac")
public class SysAreaEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 自增列
     */
    public int area_id;
    /**
     * 区代码
     */
    public String area_code;
    /**
     * 父级市代码
     */
    public String city_code;
    /**
     * 市名称
     */
    public String area_name;
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

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static SysAreaEntity getInstance() {
        return new SysAreaEntity();
    }

    /**
     * 根据城市编码和行政区域名，查询行政区域信息
     *
     * @param city_code 城市编码
     * @return
     */
    public List<SysAreaEntity> getWithCityCode(String city_code) {
        return this.cache(String.format("BaseData:SysCity:%s", city_code))
                .where("city_code", city_code)
                .selectList();
    }

    /**
     * 根据城市编码查询行政区域代码集合
     *
     * @param city_codes 城市编码,多个值用英文逗号隔开
     * @return
     */
    public String[] getListWithCityCodes(String city_codes) {
        return this.cache(String.format("BaseData:SysCity:%s:AreaCodes", common.md5(city_codes)))
                .whereIn("city_code", city_codes)
                .selectForStringArray("area_code");
    }

    /**
     * 根据城市编码和行政区域名，查询行政区域信息
     *
     * @param area_name 区域名
     * @param city_code 城市编码
     * @return
     */
    public SysAreaEntity getWithAreaName(String area_name, String city_code) {
        return this.cache(String.format("BaseData:SysCity:%s:%s", city_code, area_name))
                .where("area_name", area_name)
                .findEntity();
    }

    /**
     * 通过行政区域代码查询信息
     *
     * @param area_code 行政区域代码
     */
    public SysAreaEntity getWithCode(String area_code) {
        return this.cache(String.format("BaseData:SysArea:%s", area_code))
                .where("area_code", area_code)
                .findEntity();
    }

    /**
     * 本地化（生成json数据）
     *
     * @return
     */
    public JSONArray localize(String city_code) {
        JSONArray array = new JSONArray();
        List<SysAreaEntity> list = this.getWithCityCode(city_code);
        for (SysAreaEntity entity : list) {
            JSONObject json = new JSONObject();
            json.put("value", entity.area_code);
            json.put("text", entity.area_name);

            JSONArray children = SysStreetEntity.getInstance().localize(entity.area_code);
            json.put("children", children);
            array.add(json);
        }
        return array;
    }
}
