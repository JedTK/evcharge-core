package com.evcharge.entity.mktchannels;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 历史记录;
 * @author : Jay
 * @date : 2025-4-17
 */
@TargetDB("evcharge_mktchannels")
public class ContactHistoryEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 对接记录id 关联ContactRecord表
     */
    public long record_id ;
    /**
     * 内容
     */
    public String content ;
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
    public static ContactHistoryEntity getInstance() {
        return new ContactHistoryEntity();
    }
}