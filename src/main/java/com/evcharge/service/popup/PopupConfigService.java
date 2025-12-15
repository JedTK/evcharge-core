package com.evcharge.service.popup;

import com.evcharge.entity.popup.PopupConfigEntity;

/**
 * 弹出窗口配置;(PopupConfig)表服务接口
 *
 * @author : JED
 * @date : 2025-12-12
 */
public class PopupConfigService {

    private final static String TAG = "Popup配置";

    private volatile static PopupConfigService instance;

    public static PopupConfigService getInstance() {
        if (instance == null) {
            synchronized (PopupConfigService.class) {
                if (instance == null) {
                    instance = new PopupConfigService();
                }
            }
        }
        return instance;
    }

    /**
     * 通过弹窗代码查询配置
     *
     * @param popup_code 弹窗编码
     * @return 弹窗配置
     */
    public PopupConfigEntity getByCode(String popup_code) {
        return getByCode(popup_code, true);
    }

    /**
     * 通过弹窗代码查询配置
     *
     * @param popup_code 弹窗编码
     * @param inCache    是否有限从缓存中获取
     * @return 弹窗配置
     */
    public PopupConfigEntity getByCode(String popup_code, boolean inCache) {
        PopupConfigEntity configEntity = PopupConfigEntity.getInstance();
        if (inCache) configEntity.cache(String.format("BaseData:PopupConfig:%s", popup_code));
        return configEntity.where("popup_code", popup_code)
                .findEntity();
    }
}
