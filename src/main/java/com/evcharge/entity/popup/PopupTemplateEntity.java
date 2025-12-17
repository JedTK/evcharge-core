package com.evcharge.entity.popup;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 弹窗内容模板 - 实体类 ;弹窗内容模板，标题、内容、按钮等文案
 *
 * @date : 2025-12-12
 */
@TargetDB("evcharge_notify")
public class PopupTemplateEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * 主键,;
     */
    public long id;
    /**
     * 模板编码，唯一,;
     */
    public String template_code;
    /**
     * 模板名称，便于运营识别,;
     */
    public String template_name;
    /**
     * 标题模板，支持 ${var} 占位符,;
     */
    public String title;
    /**
     * 副标题模板（可选），支持 ${var},;
     */
    public String sub_title;
    /**
     * 正文模板，支持 ${var} 占位符，建议使用简单文本或富文本,;
     */
    public String content;
    /**
     * 确认按钮 JSON 模板配置，支持 ${var}，为空表示无确认按钮,;
     */
    public String button_confirm_json;
    /**
     * 关闭按钮 JSON 模板配置，支持 ${var}，为空表示无关闭按钮,;
     */
    public String button_close_json;
    /**
     * 变量说明 JSON，用于描述各 ${var} 的含义、类型、示例,;
     */
    public String vars_schema_json;
    /**
     * 状态：0-停用，1-启用,;
     */
    public byte status;
    /**
     * 备注,;
     */
    public String remark;
    /**
     * 语言，如 zh-CN / en-US,;
     */
    public String lang;
    /**
     * 创建时间戳,;
     */
    public long create_time;
    /**
     * 更新时间戳,;
     */
    public long update_time;
    //endregion

    /**
     * 获得一个实例
     */
    public static PopupTemplateEntity getInstance() {
        return new PopupTemplateEntity();
    }
}
