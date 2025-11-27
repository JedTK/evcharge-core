package com.evcharge.entity.user;

import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 用户观看广告记录;
 * @author : Jay
 * @date : 2023-2-24
 */
public class UserWatchAdLogEntity extends BaseEntity implements Serializable{
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
     * 广告id
     */
    public long ad_id ;
    /**
     * 状态
     */
    public int status ;
    /**
     * 创建日期
     */
    public String create_date ;
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
    public static UserWatchAdLogEntity getInstance() {
        return new UserWatchAdLogEntity();
    }
}