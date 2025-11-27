package com.evcharge.entity.notify;


import com.evcharge.enumdata.ENotifyType;
import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.List;

/**
 * 通知配置：系统通知、消防通知、站点离线通知、站点异常通知、自定义;
 *
 * @author : JED
 * @date : 2024-9-25
 */
@TargetDB("evcharge_notify")
public class NotifyConfigEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 通知配置:{前缀}.{通知编码/站点编码/设备序列号}
     */
    public String config_code;
    /**
     * 通知标题
     */
    public String title;
    /**
     * 优先级别：1-10
     */
    public int priority;
    /**
     * 通知等级：0-4，0-NONE,1=紧急，2=重要，3=普通，4=信息
     */
    public int level;
    /**
     * 通知类型：1-WECHATCORPBOT,2=SMS,3=VOICE
     */
    public int type_id;
    /**
     * 通知方式
     */
    public String method_code;
    /**
     * 通知模板
     */
    public String template_code;
    /**
     * 接受者,[all_admin]/[auto]/手机号码1,手机号码2
     */
    public String accept_list;
    /**
     * 状态：0-关闭，1-启动
     */
    public int status;
    /**
     * 互斥：0-否，1-是，主要针对特定设备或站点互斥
     */
    public int is_mutex;
    /**
     * 配置，格式：JSONArray
     */
    public String config;
    /**
     * 通知描述
     */
    public String description;
    /**
     * 重试最大次数
     */
    public int retry_max_count;
    /**
     * 重试间隔，毫秒级
     */
    public int retry_timeout_ms;
    /**
     * 节流超时时间,毫秒
     */
    public int throttling_timeout_ms;
    /**
     * 平台代码
     */
    public String platform_code;
    /**
     * 组织代码
     */
    public String organize_code;

    //endregion

    /**
     * 获得一个实例
     */
    public static NotifyConfigEntity getInstance() {
        return new NotifyConfigEntity();
    }

    /**
     * 查询通知配置列表
     *
     * @param config_code
     * @return
     */
    public List<NotifyConfigEntity> getList(String config_code) {
        return this.cache(String.format("Notify:Config:%s", config_code))
                .where("config_code", config_code)
                .where("status", 1)// 状态：0-关闭，1-启动
                .page(1, 100)
                .order("priority")
                .selectList();
    }

    /**
     * 查询通知配置列表
     *
     * @param config_code   通知配置代码
     * @param organize_code 组织代码
     * @return
     */
    public List<NotifyConfigEntity> getList(String config_code, String organize_code, ENotifyType notifyType) {
        this.where("config_code", config_code);
        if (notifyType == ENotifyType.NONE) {
            this.cache(String.format("Notify:Config:%s", config_code));
        } else {
            this.where("type_id", notifyType.getIndex());
        }
        if (StringUtils.hasLength(organize_code)) {
            this.where("organize_code", organize_code);
        }

        return this.where("status", 1)// 状态：0-关闭，1-启动
                .page(1, 100)
                .order("priority")
                .selectList();
    }
}
