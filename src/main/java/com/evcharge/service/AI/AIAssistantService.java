package com.evcharge.service.AI;

import com.evcharge.entity.ai.AIAssistantEntity;
import com.evcharge.entity.ai.OpenAIConfigEntity;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.libsdk.openai.IOpenAI;
import com.evcharge.libsdk.openai.OpenAI;
import com.xyzs.utils.ConfigManager;
import com.xyzs.utils.LogsUtil;

/**
 * AI助手
 */
public class AIAssistantService {

    public static AIAssistantService getInstance() {
        return new AIAssistantService();
    }

    /**
     * 获取OpenAI
     * @param code AI助手编码
     * @return IOpenAI风格的AI类
     */
    public IOpenAI getOpenAI(String code) {
        OpenAIConfigEntity configEntity = AIAssistantService.getInstance().getDefaultOpenAIConfigByAssistantCode(code);
        if (configEntity == null) return null;

        return new OpenAI(configEntity.base_url, configEntity.api_key)
                .setConnectTimeout(configEntity.connect_timeout)
                .setReadTimeout(configEntity.read_timeout)
                .setWriteTimeout(configEntity.write_timeout);
    }

    /**
     * 获得AI助手
     *
     * @param code 助手编码
     */
    public AIAssistantEntity getAIAssistant(String code) {
        return AIAssistantEntity.getInstance()
                .cache(String.format("AI:Assistant:%s", code))
                .where("code", code)
                .where("status", 1)
                .findEntity();
    }

    /**
     * 获取默认OpenAI配置
     *
     * @param code 助手编码
     */
    public OpenAIConfigEntity getDefaultOpenAIConfigByAssistantCode(String code) {
        AIAssistantEntity assistantEntity = getAIAssistant(code);
        if (assistantEntity == null) {
            LogsUtil.warn(this.getClass().getSimpleName(), "[%s] - AI助手不存在或已关闭", code);
            return null;
        }

        OpenAIConfigEntity configEntity = OpenAIConfigService.getInstance().getConfig(assistantEntity.config_code);
        if (configEntity == null) {
            LogsUtil.warn(this.getClass().getSimpleName(), "[%s] - OpenAI配置不存在或已关闭", assistantEntity.config_code);
            return null;
        }
        return configEntity;
    }
}
