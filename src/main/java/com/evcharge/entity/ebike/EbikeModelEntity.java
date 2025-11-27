package com.evcharge.entity.ebike;

import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 电动车型号;
 *
 * @author : JED
 * @date : 2022-10-18
 */
public class EbikeModelEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 品牌id
     */
    public long brand_id;
    /**
     * 系列id
     */
    public long series_id;
    /**
     * 标题
     */
    public String title;
    /**
     * 车身图
     */
    public String ebike_pic;
    /**
     * 充电功率
     */
    public int charge_power;
    /**
     * 售价
     */
    public BigDecimal sale_price;
    /**
     * 上市日期
     */
    public String sale_date;
    /**
     * 电池类型
     */
    public String battery_type;
    /**
     * 充电电压
     */
    public String charge_voltage;
    /*
     * 排序
     * */
    public long sort;
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
    public static EbikeModelEntity getInstance() {
        return new EbikeModelEntity();
    }


    /**
     * 根据车型id获取系列信息
     *
     * @param modelId
     * @return
     */
    public EbikeModelEntity getEbikeModelWithId(long modelId) {
        return this.cache(String.format("BaseData:EbikeModel:%s:Details", modelId), 7 * 86400 * 1000).findModel(modelId);
    }
}
