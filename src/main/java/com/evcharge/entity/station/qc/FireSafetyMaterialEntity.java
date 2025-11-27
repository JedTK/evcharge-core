package com.evcharge.entity.station.qc;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.util.List;

/**
 * 消防物料表;
 *
 * @author : Jay
 * @date : 2024-9-14
 */
@TargetDB("evcharge_qc")
public class FireSafetyMaterialEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 模版id
     */
    public long template_id;
    /**
     * 父级id
     */
    public long parent_id;
    /**
     * 物料名称
     */
    public String material_name;
    /**
     * 物料描述
     */
    public String description;
    /**
     * 计量单位
     */
    public String unit;
    /**
     * 值类型 int,string,bool
     */
    public String value_type;
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
    public static FireSafetyMaterialEntity getInstance() {
        return new FireSafetyMaterialEntity();
    }

    /**
     * @param stationAttr 充电桩属性 对应模版 一对一关系
     * @return
     */
    public List<FireSafetyMaterialEntity> getMaterialByStationAttr(int stationAttr) {

        FireSafetyTemplateEntity fireSafetyTemplateEntity = FireSafetyTemplateEntity.getInstance()
                .cache(String.format("FireSafety:Template:stationAttr:%s", stationAttr), 86400 * 1000)
                .where("station_attr", stationAttr)
                .findEntity();

        if (fireSafetyTemplateEntity == null) {
            return null;
        }
        return getMaterialByTemplateId(fireSafetyTemplateEntity.id);
    }

    /**
     * 根据模版id获取物料明细
     */
    public List<FireSafetyMaterialEntity> getMaterialByTemplateId(long templateId) {
        return this
                .cache(String.format("FireSafety:Material:TemplateId:%s", templateId), 86400 * 1000)
                .where("template_id", templateId)
                .selectList();
    }


}