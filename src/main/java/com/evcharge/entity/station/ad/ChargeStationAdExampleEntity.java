package com.evcharge.entity.station.ad;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 充电桩图例;
 * @author : Jay
 * @date : 2024-3-27
 */
public class ChargeStationAdExampleEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 类型id
     */
    public long cate_id ;
    /**
     * 用户ID
     */
    public String content ;
    /**
     * 尺寸
     */
    public String example_size ;
    /**
     * 封面图
     */
    public String cover_pic ;
    /**
     * 资源路径
     */
    public String resource_url ;
    /**
     * 材质
     */
    public String material ;
    /**
     * 备注
     */
    public String memo ;
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
    public static ChargeStationAdExampleEntity getInstance() {
        return new ChargeStationAdExampleEntity();
    }
}