package com.evcharge.entity.sys;


import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.common;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 城市设置;
 *
 * @author : JED
 * @date : 2023-1-4
 */
@TargetDB("evcharge_rbac")
public class SysCityEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 自增列
     */
    public int city_id;
    /**
     * 市代码
     */
    public String city_code;
    /**
     * 市名称
     */
    public String city_name;
    /**
     * 简称
     */
    public String short_name;
    /**
     * 省代码
     */
    public String province_code;
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
    public static SysCityEntity getInstance() {
        return new SysCityEntity();
    }

    /**
     * 根据省份编码和城市名，查询城市信息
     *
     * @param province_code 省份编码
     * @return
     */
    public List<SysCityEntity> getWithProvinceCode(String province_code) {
        return this.cache(String.format("BaseData:SysProvince:%s", province_code))
                .where("province_code", province_code)
                .selectList();
    }

    /**
     * 根据省代码查询城市代码集合
     *
     * @param province_codes 省代码,多个值用英文逗号隔开
     * @return
     */
    public String[] getListWithProvinceCode(String province_codes) {
        return this.cache(String.format("BaseData:SysProvince:%s:CityCodes", common.md5(province_codes)))
                .whereIn("province_code", province_codes)
                .selectForStringArray("city_code");
    }

    /**
     * 根据省份编码和城市名，查询城市信息
     *
     * @param city_name     城市名
     * @param province_code 省份编码
     * @return
     */
    public SysCityEntity getWithCityName(String city_name, String province_code) {
        return this.cache(String.format("BaseData:SysProvince:%s:%s", province_code, city_name))
                .where("city_name", city_name)
                .findEntity();
    }

    /**
     * 通过城市代码查询信息
     *
     * @param code 城市代码
     */
    public SysCityEntity getWithCode(String code) {
        return this.cache(String.format("BaseData:SysCity:%s", code))
                .where("city_code", code)
                .findEntity();
    }

    /**
     * 本地化（生成json数据）
     *
     * @return
     */
    @RequestMapping("localize")
    public JSONArray localize(String province_code) {
        JSONArray array = new JSONArray();
        List<SysCityEntity> list = this.getWithProvinceCode(province_code);
        for (SysCityEntity entity : list) {
            JSONObject json = new JSONObject();
            json.put("value", entity.city_code);
            json.put("text", entity.city_name);

            JSONArray children = SysAreaEntity.getInstance().localize(entity.city_code);
            json.put("children", children);
            array.add(json);
        }
        return array;
    }
}
