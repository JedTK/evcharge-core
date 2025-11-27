package com.evcharge.entity.chargestatsionproject;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 充电桩项目物料模型项目;
 *
 * @author : JED
 * @date : 2023-10-20
 */
public class WFChargeStationProjectMaterialsTemplateItemEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 模板ID
     */
    public String templateId;
    /**
     * 物料
     */
    public String title;
    /**
     * 数量
     */
    public int quantity;
    /**
     * 总价格
     */
    public BigDecimal totalPrice;
    /**
     * 平均价格
     */
    public BigDecimal avgPrice;
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
    public static WFChargeStationProjectMaterialsTemplateItemEntity getInstance() {
        return new WFChargeStationProjectMaterialsTemplateItemEntity();
    }
}
