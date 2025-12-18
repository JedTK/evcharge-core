package com.evcharge.service.popup;

import com.evcharge.entity.popup.PopupTemplateEntity;

/**
 * 弹窗消息模版
 */
public class PopupTemplateService {

    private final static String TAG = "Popup消息模版";

    private volatile static PopupTemplateService instance;

    public static PopupTemplateService getInstance() {
        if (instance == null) {
            synchronized (PopupTemplateService.class) {
                if (instance == null) {
                    instance = new PopupTemplateService();
                }
            }
        }
        return instance;
    }

    /**
     * 通过弹窗代码查询配置
     *
     * @param template_code 模板编码
     * @return 弹窗配置
     */
    public PopupTemplateEntity getByCode(String template_code) {
        return getByCode(template_code, true);
    }

    /**
     * 通过弹窗代码查询配置
     *
     * @param template_code 模板编码
     * @param inCache    是否有限从缓存中获取
     * @return 弹窗配置
     */
    public PopupTemplateEntity getByCode(String template_code, boolean inCache) {
        PopupTemplateEntity entity = PopupTemplateEntity.getInstance();
        if (inCache) entity.cache(String.format("BaseData:PopupTemplate:%s", template_code));
        return entity.where("template_code", template_code)
                .findEntity();
    }
}
