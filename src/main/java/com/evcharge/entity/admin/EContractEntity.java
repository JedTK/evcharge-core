package com.evcharge.entity.admin;

import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 合同表;
 *
 * @author : JED
 * @date : 2022-12-21
 */
public class EContractEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 用户ID
     */
    public long uid;
    /**
     * 合同标题
     */
    public String title;
    /**
     * 合同url
     */
    public String url;
    /**
     * 创建时间
     */
    public long create_time;
    /**
     * 更新时间
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static EContractEntity getInstance() {
        return new EContractEntity();
    }
}