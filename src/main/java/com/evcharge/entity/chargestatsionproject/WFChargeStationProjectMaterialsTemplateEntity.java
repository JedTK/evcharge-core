package com.evcharge.entity.chargestatsionproject;

import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 充电桩项目物料模型;
 *
 * @author : JED
 * @date : 2023-10-20
 */
public class WFChargeStationProjectMaterialsTemplateEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 模型名
     */
    public String title;
    /**
     * 备注
     */
    public String remark;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static WFChargeStationProjectMaterialsTemplateEntity getInstance() {
        return new WFChargeStationProjectMaterialsTemplateEntity();
    }
}
