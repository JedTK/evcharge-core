package com.evcharge.entity.cms;

import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * cms内容表;
 * @author : Jay
 * @date : 2023-1-3
 */
public class CmsArticleEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 标题
     */
    public String title ;
    /**
     * 状态 0=不展示 1=展示
     */
    public int status ;
    /**
     * 图片链接
     */
    public String banner ;
    /**
     * 2025-10-21
     * 类型，即将弃用 1-首页banner 2-商城banner,;
     */
    public int type ;
    /**
     * 位置 wxmp_index=微信小程序首页,;
     */
    public String position;
    /**
     * 描述/内容
     */
    public String desc ;
    /**
     * 开始时间
     */
    public long start_time ;
    /**
     * 结束时间
     */
    public long end_time ;
    /**
     * 超链接
     */
    public String link ;
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
    public static CmsArticleEntity getInstance() {
        return new CmsArticleEntity();
    }
}