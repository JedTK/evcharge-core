package com.evcharge.service.AI;

import com.evcharge.entity.ai.OpenAIConfigEntity;

public class OpenAIConfigService {

    public static OpenAIConfigService getInstance() {
        return new OpenAIConfigService();
    }

    /**
     * 获得AI助手
     *
     * @param config_code 配置编码
     */
    public OpenAIConfigEntity getConfig(String config_code) {
        return OpenAIConfigEntity.getInstance()
                .cache(String.format("AI:OpenAIConfig:%s", config_code))
                .where("config_code", config_code)
                .where("status", 1)
                .findEntity();
    }
}
