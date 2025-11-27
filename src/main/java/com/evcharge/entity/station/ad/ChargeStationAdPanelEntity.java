package com.evcharge.entity.station.ad;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 充电桩广告位;
 * @author : Jay
 * @date : 2024-3-27
 */
public class ChargeStationAdPanelEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id ;
    /**
     * 充电站id
     */
    public long cs_id ;
    /**
     * nfc编码
     */
    public String nfc_code ;
    /**
     * 广告位编号
     */
    public String ad_no ;
    /**
     * 广告位分类 1商圈/2主干道/3住宅区/4写字楼
     */
    public int ad_type ;
    /**
     * 广告位属性 1品牌/2商业/3公益/4定制
     */
    public int ad_attr ;
    /**
     * 广告位置
     */
    public int ad_location ;
    /**
     * 图例id
     */
    public long example_id ;
    /**
     * 备注
     */
    public String memo ;
    /**
     * 状态
     */
    public int status ;
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
    public static ChargeStationAdPanelEntity getInstance() {
        return new ChargeStationAdPanelEntity();
    }
}