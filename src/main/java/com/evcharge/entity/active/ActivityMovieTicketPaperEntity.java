package com.evcharge.entity.active;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 电影票实体票;
 * @author : Jay
 * @date : 2024-7-31
 */
public class ActivityMovieTicketPaperEntity extends BaseEntity implements Serializable{
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
     * 电影票id
     */
    public long movie_id ;
    /**
     * cdkey
     */
    public String cd_key ;
    /**
     * 省
     */
    public String province ;
    /**
     * 市
     */
    public String city ;
    /**
     * 区
     */
    public String district ;
    /**
     * 电影院
     */
    public String cinema ;
    /**
     * 电影厅
     */
    public String hall_number ;
    /**
     * 座位
     */
    public String seats ;
    /**
     * 电影日期
     */
    public long movie_date ;
    /**
     * 开始时间
     */
    public long start_time ;
    /**
     * 状态 0-未领取 1-已领取
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
    public static ActivityMovieTicketPaperEntity getInstance() {
        return new ActivityMovieTicketPaperEntity();
    }






}