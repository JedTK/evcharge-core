package com.evcharge.entity.erp;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 进销存系统：供应商;
 *
 * @author : JED
 * @date : 2023-1-9
 */
public class eSupplierCompanyEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 供应商名称
     */
    public String name;
    /**
     * 拼音
     */
    public String pinyin;
    /**
     * 联系人姓名
     */
    public String contacts;
    /**
     * 联系电话
     */
    public String contactsPhone;
    /**
     * 省
     */
    public String province;
    /**
     * 市
     */
    public String city;
    /**
     * 区
     */
    public String district;
    /**
     * 街道，可能为空字串
     */
    public String street;
    /**
     * 门牌，可能为空字串
     */
    public String street_number;
    /**
     * 排序索引
     */
    public int sort_index;
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
    public static eSupplierCompanyEntity getInstance() {
        return new eSupplierCompanyEntity();
    }
}
