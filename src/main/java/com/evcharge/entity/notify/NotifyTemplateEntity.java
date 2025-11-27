package com.evcharge.entity.notify;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 通知模板：通知方式可能会有不同的模板，特别是短信、电话;
 *
 * @author : JED
 * @date : 2024-9-25
 */
@TargetDB("evcharge_notify")
public class NotifyTemplateEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 通知模板
     */
    public String template_code;
    /**
     * 通知方式
     */
    public String method_code;
    /**
     * 描述
     */
    public String description;
    /**
     * 模板内容，可以包含占位符
     */
    public String content;
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
    public static NotifyTemplateEntity getInstance() {
        return new NotifyTemplateEntity();
    }

    /**
     * 获取通知模板
     *
     * @param template_code
     * @return
     */
    public NotifyTemplateEntity getWithCode(String template_code) {
        return this.cache(String.format("Notify:Template:%s", template_code))
                .where("template_code", template_code)
                .findEntity();
    }
}
