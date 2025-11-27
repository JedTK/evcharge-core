package com.evcharge.entity.station;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 分润模式;
 * @author : Jay
 * @date : 2024-3-11
 */
public class ChargeStationProfitsModeEntity extends BaseEntity implements Serializable{
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
     * 收入类型 1=金额 2=比率 3=金额+比率
     */
    public int income_type ;
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
    public static ChargeStationProfitsModeEntity getInstance() {
        return new ChargeStationProfitsModeEntity();
    }
}