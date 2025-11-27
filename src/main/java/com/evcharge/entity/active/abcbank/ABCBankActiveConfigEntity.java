package com.evcharge.entity.active.abcbank;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 农行活动配置;
 * @author : Jay
 * @date : 2025-3-21
 */
public class ABCBankActiveConfigEntity extends BaseEntity implements Serializable{
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
     * 优惠券代码，可以多个，用','隔开 关联ThirdPartyCouponConfig表 的mapping_id
     */
    public String coupon_code ;
    /**
     * 限制数量
     */
    public int limit_count ;
    /**
     * 领取数量
     */
    public int receive_count ;
    /**
     * 开始时间
     */
    public long start_time ;
    /**
     * 结束时间
     */
    public long end_time ;
    /**
     * 价格
     */
    public BigDecimal price ;
    /**
     * 状态 0=启动 1=禁用
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
    public static ABCBankActiveConfigEntity getInstance() {
        return new ABCBankActiveConfigEntity();
    }
}
