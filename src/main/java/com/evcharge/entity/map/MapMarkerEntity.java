package com.evcharge.entity.map;

import com.xyzs.entity.BaseEntity;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 地图标记表;
 *
 * @author : Jay
 * @date : 2024-7-5
 */
public class MapMarkerEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 类型
     */
    public int type;
    /**
     * icon
     */
    public String icon;
    /**
     * 标题
     */
    public String title;
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
     * 内容描述
     */
    public String desc;
    /**
     * 经度
     */
    public double lon;
    /**
     * 纬度
     */
    public double lat;
    /**
     * 状态
     */
    public int status;
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
    public static MapMarkerEntity getInstance() {
        return new MapMarkerEntity();
    }


    /**
     * 获取半径五公里范围内的标记点
     * @param lon 经度
     * @param lat 纬度
     * @return list
     */
    public List<Map<String, Object>> getNearMarker(String lon, String lat) {
        return getNearMarker(lon,lat,5);
    }

    /**
     * 获取半径五公里范围内的标记点
     * @param lon 经度
     * @param lat 纬度
     * @return list
     */
    public List<Map<String, Object>> getNearMarker(String lon, String lat, int distance) {

        String field = String.format("id,type,icon,title,lon,lat,status,( 6371 * ACOS(COS(RADIANS(%s)) * COS(RADIANS(lat)) *   COS(RADIANS(lon) - RADIANS(%s)) +  SIN(RADIANS(%s)) * SIN(RADIANS(lat)))) as distance", lat, lon, lat);

        return this
                .cache(String.format("Map:Marker:%s:%s:%s",lon,lat,distance),60*1000)
                .where("distance", "<", distance)
                .field(field)
                .order("distance asc")
                .limit(1)
                .selectList();
    }


}