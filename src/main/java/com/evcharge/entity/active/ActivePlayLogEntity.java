package com.evcharge.entity.active;

import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 转盘日志表;
 * @author : Jay
 * @date : 2023-1-10
 */
public class ActivePlayLogEntity extends BaseEntity implements Serializable{
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
     * 配置id
     */
    public long config_id ;
    /**
     * 奖品id
     */
    public long reward_id ;
    /**
     * 状态 0=未到账 1=已到账
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
    public static ActivePlayLogEntity getInstance() {
        return new ActivePlayLogEntity();
    }
}