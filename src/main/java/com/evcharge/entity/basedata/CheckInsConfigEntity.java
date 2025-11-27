package com.evcharge.entity.basedata;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 签到配置表;
 *
 * @author : Jay
 * @date : 2024-1-3
 */
public class CheckInsConfigEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 类型id
     */
    public int type_id;
    /**
     * 标题
     */
    public String title;
    /**
     * 排序
     */
    public int sort;
    /**
     * 积分模版id
     */
    public long integral_temp_id;
    /**
     * 状态
     */
    public int status;
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
    public static CheckInsConfigEntity getInstance() {
        return new CheckInsConfigEntity();
    }

    /**
     * 获取签到配置
     * @param sort
     * @return
     */
    public CheckInsConfigEntity getConfigBySort(int sort) {
        return getConfigBySort(sort, 1);
    }

    /**
     * 获取签到配置
     * @param sort
     * @param typeId
     * @return
     */
    public CheckInsConfigEntity getConfigBySort(int sort, int typeId) {
        return this.where("sort", sort)
                .where("type_id", typeId)
                .cache(String.format("BaseData:CheckinConfig:%s",sort),86400*1000)
                .findModel();
    }




}