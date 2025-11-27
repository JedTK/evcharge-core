package com.evcharge.entity.ad;


import com.xyzs.entity.BaseEntity;

import java.io.Serializable;

/**
 * 广告列表;
 *
 * @author : Jay
 * @date : 2023-2-24
 */
public class AdEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 类型id
     */
    public int type_id;
    /**
     * 广告标题
     */
    public String title;
    /**
     * 微信广告位id
     */
    public String wx_ad_id;
    /**
     * 广告代码
     */
    public String ad_code;
    /**
     * 广告时长
     */
    public String duration;
    /**
     * 小程序广告展示页面
     */
    public String show_page;
    /**
     * 跳转地址
     */
    public String jump_url;
    /**
     * 广告图片
     */
    public String ad_image;
    /**
     * 广告描述
     */
    public String desc;
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
    public static AdEntity getInstance() {
        return new AdEntity();
    }

    /**
     * 根据广告id获取广告信息
     *
     * @param id
     * @return
     */
    public AdEntity getAdById(long id) {
        return this.cache(String.format("Ad:info:%s", id), 86400 * 1000)
                .where("id", id)
                .findModel();
    }

    /**
     * 根据微信广告id获取广告信息
     *
     * @param wxAdId
     * @return
     */
    public AdEntity getAdInfoByWxId(String wxAdId) {
        return this.cache(String.format("Ad:WxAdInfo:%s", wxAdId), 86400 * 1000)
                .where("wx_ad_id", wxAdId)
                .findModel();
    }
}