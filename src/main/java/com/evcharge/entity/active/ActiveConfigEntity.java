package com.evcharge.entity.active;

import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;

import java.io.Serializable;

/**
 * 转盘配置表;
 * @author : Jay
 * @date : 2023-1-10
 */
public class ActiveConfigEntity extends BaseEntity implements Serializable{
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
     * 类型 1=大转盘 2=九宫格 3=老虎机
     */
    public int type ;
    /**
     * 背景图
     */
    public String background ;
    /**
     * 开始时间
     */
    public long start_time ;
    /**
     * 结束时间
     */
    public long end_time ;
    /**
     * 图片
     */
    public String logo ;
    /**
     * 描述
     */
    public String desc ;
    /**
     * 分享海报
     */
    public String share_poster ;
    /**
     * 分享标题
     */
    public String share_title ;
    /**
     * 状态 0=未开始 1=已开始 2=已结束
     */
    public int status ;
    /**
     * 详细配置
     */
    public String config ;
    /**
     * 参与次数
     */
    public int play_limit ;

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
    public static ActiveConfigEntity getInstance() {
        return new ActiveConfigEntity();
    }


    /**
     * 通过id获取配置信息
     * @param configId
     * @return
     */
    public ActiveConfigEntity getConfigById(long configId){
        return this.cache(String.format("LotteryWheel:Config:%s",configId))
                .where("id",configId)
                .findModel();
    }

    /**
     * 通过配置文件删除缓存
     * @param configId
     */
    public void delCacheById(long configId){
        DataService.getMainCache().del(String.format("LotteryWheel:Config:%s",configId));
    }

}