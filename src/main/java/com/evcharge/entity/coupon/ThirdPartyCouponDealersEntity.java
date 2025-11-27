package com.evcharge.entity.coupon;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 第三方券配置;
 * @author : JED
 * @date : 2024-8-14
 */
public class ThirdPartyCouponDealersEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 名称
     */
    public String name ;
    /**
     * 代号
     */
    public String code ;
    /**
     * 配置信息
     */
    public String config ;
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
    public static ThirdPartyCouponDealersEntity getInstance() {
        return new ThirdPartyCouponDealersEntity();
    }


    /**
     * 获取券商信息
     * @param id id
     * @return
     */
    public ThirdPartyCouponDealersEntity getInfoByID(long id){
        return this.cache(String.format("ThirtyParty:Dealers:Info:%s",id),86400*1000)
                .where("id",id).findEntity();
    }




}