package com.evcharge.entity.active;

import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 分享日志;
 * @author : Jay
 * @date : 2023-1-10
 */
public class ActiveShareLogEntity extends BaseEntity implements Serializable{
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
    public static ActiveShareLogEntity getInstance() {
        return new ActiveShareLogEntity();
    }
}