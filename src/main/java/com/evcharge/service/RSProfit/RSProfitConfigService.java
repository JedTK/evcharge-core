package com.evcharge.service.RSProfit;

import com.evcharge.entity.RSProfit.RSProfitConfigEntity;

/**
 * 分润配置-业务逻辑层
 */
public class RSProfitConfigService {
    /**
     * 获得一个实例
     */
    public static RSProfitConfigService getInstance() {
        return new RSProfitConfigService();
    }

    /**
     * 获取 配置信息
     *
     * @param config_id 配置ID
     * @return 分润配置
     */
    public RSProfitConfigEntity getConfigWithId(long config_id) {
        return getConfigWithId(config_id, true);
    }

    /**
     * 获取 配置信息
     *
     * @param config_id 配置ID
     * @param inCache   是否优先从缓存中获取数据
     * @return 分润配置
     */
    public RSProfitConfigEntity getConfigWithId(long config_id, boolean inCache) {
        RSProfitConfigEntity configEntity = RSProfitConfigEntity.getInstance();
        if (inCache) configEntity.cache(String.format("BaseData:RSProfitConfig:%s", config_id));
        return configEntity.where("id", config_id).findEntity();
    }

    /**
     * 通过渠道联系手机号查询站点ID列表
     *
     * @param channel_phone 渠道手机号码
     * @param inCache       优先从缓存中获取
     */
    public String[] getCSIdList(String channel_phone, boolean inCache) {
        RSProfitConfigEntity configEntity = new RSProfitConfigEntity();
        if (inCache) configEntity.cache(String.format("RSProfit:%s:CSIdList", channel_phone));
        return configEntity.field("cs_id")
                .where("channel_phone", channel_phone)
                .group("cs_id")
                .selectForStringArray("cs_id");
    }
}
