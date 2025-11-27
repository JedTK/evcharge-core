package com.evcharge.libsdk.wechat;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 微信消息订阅模版表;
 * @author : Jay
 * @date : 2024-1-9
 */
public class WechatSubscribeTmplEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 运行类型：充电结束，签到提醒
     */
    public int run_type ;
    /**
     * 标题
     */
    public String title ;
    /**
     * 关键词
     */
    public String keyword ;
    /**
     * 模版id
     */
    public String tmpl_id ;
    /**
     * 类目：共享服务，签到提醒
     */
    public String cate_name ;
    /**
     * 类型id 1=一次性订阅	2=长期订阅
     */
    public int type_id ;
    /**
     * 任务执行时间(corn表达式)
     */
    public String task_runtime ;
    /**
     * 数据字段
     */
    public String data_field ;
    /**
     * 备注
     */
    public String memo ;
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
    public static WechatSubscribeTmplEntity getInstance() {
        return new WechatSubscribeTmplEntity();
    }





}