package com.evcharge.entity.basedata;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 渠道管理;
 * @author : Jay
 * @date : 2024-2-28
 */
public class ChannelManagementEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * 主键ID
     */

    public long id ;
    /**
     * 渠道类型
     */
    public int channel_type ;
    /**
     * 姓名
     */
    public String name ;
    /**
     * 手机号
     */
    public String phone_number ;
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
     * 街道
     */
    public String street ;
    /**
     * 公司名
     */
    public String company_name ;
    /**
     * 开户名
     */
    public String account_name ;
    /**
     * 银行账号
     */
    public String bank_account ;
    /**
     * 开户行
     */
    public String bank ;
    /**
     * 税号
     */
    public String tax_number ;
    /**
     * 结账日期
     */
    public long settlement_date ;
    /**
     * 备注
     */
    public String remark ;
    /**
     * 创建人
     */
    public long creator_id ;
    /**
     * 创建日期
     */
    public long create_time ;

    //endregion
    /**
     * 获得一个实例
     * @return
     */
    public static ChannelManagementEntity getInstance() {
        return new ChannelManagementEntity();
    }
}