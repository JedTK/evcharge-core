package com.evcharge.entity.station.ad;


import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 广告位与nfc关联表;
 * @author : JED
 * @date : 2024-3-27
 */
public class ChargeStationAdPanelToNFCEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * nfc编码
     */
    public String nfc_code ;
    /**
     * 广告位id
     */
    public long ad_panel_id ;
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
    public static ChargeStationAdPanelToNFCEntity getInstance() {
        return new ChargeStationAdPanelToNFCEntity();
    }
}