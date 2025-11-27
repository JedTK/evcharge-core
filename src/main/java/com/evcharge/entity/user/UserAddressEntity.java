package com.evcharge.entity.user;

import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 用户地址表;
 * @author : Jay
 * @date : 2022-12-22
 */
public class UserAddressEntity extends BaseEntity implements Serializable{
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
    public String name ;
    /**
     * 手机号码
     */
    public String phone ;
    /**
     * 邮政编码
     */
    public String postal_code ;
    /**
     * 省份
     */
    public String province ;
    /**
     * 城市
     */
    public String city ;
    /**
     * 地区
     */
    public String area ;
    /**
     * 详细地址
     */
    public String details ;
    /**
     * 是否默认
     */
    public int is_default ;
    /**
     *
     */
    public String md5 ;
    /**
     * 创建IP
     */
    public String ip ;
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
    public static UserAddressEntity getInstance() {
        return new UserAddressEntity();
    }
}