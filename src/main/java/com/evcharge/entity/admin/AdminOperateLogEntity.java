package com.evcharge.entity.admin;


import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.TimeUtil;

import java.io.Serializable;

/**
 * 管理员操作记录;
 *
 * @author : Jay
 * @date : 2025-8-11
 */
public class AdminOperateLogEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 管理员id
     */
    public long admin_id;
    /**
     * 路由
     */
    public String path;
    /**
     * 内容
     */
    public String content;
    /**
     * ip
     */
    public String ip ;
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
    public static AdminOperateLogEntity getInstance() {
        return new AdminOperateLogEntity();
    }

    /**
     * 创建日志
     * @param adminId 管理员iid
     * @param path 路径
     * @param content 内容
     * @param ip ip
     */
    public void createLog(long adminId, String path, String content,String ip) {
        this.admin_id = adminId;
        this.path = path;
        this.content = content;
        this.ip = ip;
        this.create_time = TimeUtil.getTimestamp();
        this.insert();
    }


}