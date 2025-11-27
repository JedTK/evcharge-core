package com.evcharge.entity.station;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 充电桩额外信息;
 * @author : JED
 * @date : 2024-9-9
 */
public class ChargeStationExtraInfoEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 平台代码
     */
    public String platform_code ;
    /**
     * 组织代码
     */
    public String organize_code ;
    /**
     * 站点id
     */
    public String CSId ;
    /**
     * 站点编号
     */
    public String station_number ;
    /**
     * 合作伙伴
     */
    public String partner ;
    /**
     * 是否购买保险 0-未购买保险 1=已购买保险
     */
    public int is_insurance ;
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
     */
    public static ChargeStationExtraInfoEntity getInstance() {
        return new ChargeStationExtraInfoEntity();
    }
}