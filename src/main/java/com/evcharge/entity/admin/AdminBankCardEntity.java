package com.evcharge.entity.admin;

import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 管理员银行卡;
 *
 * @author : JED
 * @date : 2022-11-21
 */
public class AdminBankCardEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 管理员Id
     */
    public long admin_id;
    /**
     * 收款人姓名
     */
    public String fullName;
    /**
     * 银行卡卡号
     */
    public String bankCardNumber;
    /**
     * 关联的银行ID
     */
    public long bank_id;
    /**
     * 关联的银行支行ID
     */
    public long bank_branch_id;
    /**
     * 是否默认：0=否，1=是
     */
    public int isDefault;
    /**
     * 创建时间戳
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static AdminBankCardEntity getInstance() {
        return new AdminBankCardEntity();
    }

    /**
     * 读取默认的银行卡信息
     *
     * @param admin_id
     * @return
     */
    public AdminBankCardEntity getDefault(long admin_id) {
        return AdminBankCardEntity.getInstance()
                .cache(String.format("Admin:BankCard:%s:Default", admin_id))
                .where("admin_id", admin_id)
                .where("isDefault", 1)
                .findModel();
    }

    /**
     * 通过银行卡卡号读取信息
     *
     * @param bankCardNumber 银行卡卡号
     * @return
     */
    public AdminBankCardEntity getWithBankCardNumber(long bankCardNumber) {
        return AdminBankCardEntity.getInstance()
                .cache(String.format("Admin:BankCard:%s", bankCardNumber))
                .where("bankCardNumber", bankCardNumber)
                .findModel();
    }
}
