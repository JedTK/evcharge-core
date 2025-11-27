package com.evcharge.entity.station.ad;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 充电桩广告类型;
 * @author : Jay
 * @date : 2024-3-27
 */
public class ChargeStationAdCategoryEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 标题
     */
    public String title ;
    /**
     * 状态
     */
    public int status ;
    /**
     * 创建时间
     */
    public long create_time ;
    /**
     * 更新时间
     */
    public long update_time ;

    //endregion
    /**
     * 获得一个实例
     * @return
     */
    public static ChargeStationAdCategoryEntity getInstance() {
        return new ChargeStationAdCategoryEntity();
    }
}