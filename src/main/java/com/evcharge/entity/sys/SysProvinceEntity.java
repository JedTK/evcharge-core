package com.evcharge.entity.sys;


import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 省份设置;
 *
 * @author : JED
 * @date : 2023-1-4
 */
@TargetDB("evcharge_rbac")
public class SysProvinceEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 自增列
     */
    public int province_id;
    /**
     * 省份代码
     */
    public String province_code;
    /**
     * 省份名称
     */
    public String province_name;
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
    public static SysProvinceEntity getInstance() {
        return new SysProvinceEntity();
    }

    /**
     * 根据省份查询省份信息
     *
     * @param province_name 省份
     */
    public SysProvinceEntity getWithProvinceName(String province_name) {
        return this.cache(String.format("BaseData:SysProvince:%s", province_name))
                .where("province_name", province_name)
                .findEntity();
    }

    /**
     * 通过省份代码查询信息
     *
     * @param code 省份代码
     */
    public SysProvinceEntity getWithCode(String code) {
        return this.cache(String.format("BaseData:SysProvince:%s", code))
                .where("province_code", code)
                .findEntity();
    }

    /**
     * 本地化（生成json数据）
     */
    public JSONArray localize() {
        return localize(this.selectList());
    }

    /**
     * 本地化（生成json数据）
     */
    public JSONArray localize(List<SysProvinceEntity> list) {
        JSONArray array = new JSONArray();
        for (SysProvinceEntity entity : list) {
            JSONObject json = new JSONObject();
            json.put("value", entity.province_code);
            json.put("text", entity.province_name);

            JSONArray children = SysCityEntity.getInstance().localize(entity.province_code);
            json.put("children", children);
            array.add(json);
        }
        return array;
    }
}
