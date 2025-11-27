package com.evcharge.entity.map;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 地图路线表;
 * @author : Jay
 * @date : 2024-7-5
 */
public class MapRouteEntity extends BaseEntity implements Serializable{
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
     * 类型 限行，监控等
     */
    public String type ;
    /**
     * 经纬度数组
     */
    public String points ;
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
     * 线的颜色
     */
    public String color ;
    /**
     * 彩虹线
     */
    public String color_list ;
    /**
     * 线的宽度
     */
    public int width ;
    /**
     * 是否虚线 1=是 0=否
     */
    public int dotted_line ;
    /**
     * 带箭头的线
     */
    public int arrow_line ;
    /**
     * 更换箭头图标
     */
    public String arrow_icon_path ;
    /**
     * 线的边框颜色
     */
    public String border_color ;
    /**
     * 线的厚度
     */
    public int border_width ;
    /**
     * 压盖关系 默认为 abovelabels
     */
    public String level ;
    /**
     * 文字样式 折线上文本样式
     */
    public String text_style ;
    /**
     * 分段文本 Array<SegmentText> 折线上文本内容和位置
     */
    public String segment_texts ;
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
    public static MapRouteEntity getInstance() {
        return new MapRouteEntity();
    }


    /**
     * 获取半径五公里范围内的标记点
     * @param lon 经度
     * @param lat 纬度
     * @return list
     */
    public List<Map<String, Object>> getNearRoute(String lon, String lat) {
        return getNearRoute(lon,lat,5);
    }

    /**
     * 获取半径五公里范围内的标记点
     * @param lon 经度
     * @param lat 纬度
     * @return list
     */
    public List<Map<String, Object>> getNearRoute(String lon, String lat, int distance) {

        String field = String.format("id,title,type,points,lon,lat,status,color,width,dotted_line,arrow_line,arrow_icon_path,border_color,border_width,level,text_style,segment_texts,( 6371 * ACOS(COS(RADIANS(%s)) * COS(RADIANS(lat)) *   COS(RADIANS(lon) - RADIANS(%s)) +  SIN(RADIANS(%s)) * SIN(RADIANS(lat)))) as distance", lat, lon, lat);

        return this
                .where("distance", "<", distance)
                .cache(String.format("Map:Route:%s:%s:%s",lon,lat,distance),60*1000)
                .field(field)
                .order("distance asc")
                .limit(1)
                .selectList();
    }


}