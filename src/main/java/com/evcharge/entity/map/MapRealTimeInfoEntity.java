package com.evcharge.entity.map;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 实时信息;
 * @author : Jay
 * @date : 2024-7-5
 */
public class MapRealTimeInfoEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 用户ID
     */
    public long uid ;
    /**
     * 类型
     */
    public String type ;
    /**
     * 省
     */
    public String province ;
    /**
     * 城市
     */
    public String city ;
    /**
     * 区域
     */
    public String district ;
    /**
     * 街道
     */
    public String street ;
    /**
     * 地址
     */
    public String address ;
    /**
     * 经度
     */
    public double lon ;
    /**
     * 纬度
     */
    public double lat ;

    /**
     * 描述
     */
    public String desc ;
    /**
     * 图片链接
     */
    public String img_url ;
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
    public static MapRealTimeInfoEntity getInstance() {
        return new MapRealTimeInfoEntity();
    }
}