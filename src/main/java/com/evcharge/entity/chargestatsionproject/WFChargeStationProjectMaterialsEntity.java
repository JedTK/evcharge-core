package com.evcharge.entity.chargestatsionproject;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 充电桩项目物料;
 *
 * @author : JED
 * @date : 2023-10-17
 */
public class WFChargeStationProjectMaterialsEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 项目ID，自定义生成
     */
    public String projectId;
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
    public static WFChargeStationProjectMaterialsEntity getInstance() {
        return new WFChargeStationProjectMaterialsEntity();
    }
}
