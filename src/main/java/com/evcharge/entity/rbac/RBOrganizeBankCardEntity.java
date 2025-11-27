package com.evcharge.entity.rbac;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 组织对公帐号;
 * @author : Jay
 * @date : 2025-3-11
 */
public class RBOrganizeBankCardEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 组织代码
     */
    public String organize_code ;
    /**
     * 公司名称
     */
    public String company_name ;
    /**
     * 注册地址
     */
    public String registered_address ;
    /**
     * 预留手机号
     */
    public String phone_num ;
    /**
     * 银行卡卡号
     */
    public String bank_card_number ;
    /**
     * 银行id
     */
    public long bank_id ;
    /**
     * 支行名称
     */
    public String bank_branch_name ;
    /**
     * 关联支行id
     */
    public long bank_branch_id ;
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
    public static RBOrganizeBankCardEntity getInstance() {
        return new RBOrganizeBankCardEntity();
    }
}