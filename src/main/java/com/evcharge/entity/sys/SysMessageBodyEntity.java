package com.evcharge.entity.sys;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 站内消息详情;
 *
 * @author : JED
 * @date : 2022-11-10
 */
public class SysMessageBodyEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 标题
     */
    public String title;
    /**
     * 内容
     */
    public String content;
    /**
     * 消息类型：1=系统通知
     */
    public int typeId;
    /**
     * 路径数据
     */
    public String path;
    /**
     * 创建时间
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static SysMessageBodyEntity getInstance() {
        return new SysMessageBodyEntity();
    }
}
