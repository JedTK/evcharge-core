package com.evcharge.entity.user.disabled;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 残疾用户;
 * @author : Jay
 * @date : 2024-1-11
 */
public class DisabledUsersEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 手机号码
     */
    public String phone ;
    /**
     * 等级id
     */
    public long level_id ;
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
    public static DisabledUsersEntity getInstance() {
        return new DisabledUsersEntity();
    }

    /**
     *
     * @param phone
     * @return
     */
    public DisabledUsersEntity getInfoByInfo(String phone){
        return this.where("phone",phone)
                .cache(String.format("User:%s:DisableInfo",phone),86400*1000)
                .findModel();
    }




}