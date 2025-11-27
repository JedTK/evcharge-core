package com.evcharge.entity.station.qc;


import com.xyzs.annotation.TargetDB;
import com.xyzs.entity.BaseEntity;
import java.io.Serializable;

/**
 * 消防物料模板表;
 * @author : Jay
 * @date : 2024-9-14
 */
@TargetDB("evcharge_qc")
public class FireSafetyTemplateEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 模板名称
     */
    public String template_name ;
    /**
     * 充电桩形态ID
     */
    public long station_attr ;
    /**
     * 模板描述
     */
    public String description ;
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
    public static FireSafetyTemplateEntity getInstance() {
        return new FireSafetyTemplateEntity();
    }


    /**
     * 获取模版信息
     * @param stationAttr
     * @return
     */
    public FireSafetyTemplateEntity getTemplateInfoByStationAttr(int stationAttr){
      return   FireSafetyTemplateEntity.getInstance()
                .cache(String.format("FireSafety:Template:stationAttr:%s", stationAttr), 86400 * 1000)
                .where("station_attr", stationAttr)
                .findEntity();
    }



}