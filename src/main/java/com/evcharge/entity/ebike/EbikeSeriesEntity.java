package com.evcharge.entity.ebike;

import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;

import java.io.Serializable;

/**
 * 电动车系列;
 * @author : JED
 * @date : 2022-10-18
 */
public class EbikeSeriesEntity extends BaseEntity implements Serializable{
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id ;
    /**
     * 品牌id
     */
    public long brand_id ;
    /**
     * 系列标题
     */
    public String title ;
    /*
     * 排序
     * */
    public long sort;
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
    public static EbikeSeriesEntity getInstance() {
        return new EbikeSeriesEntity();
    }




    /**
     * 根据系列id获取系列信息
     * @param seriesId
     * @return
     */
    public EbikeSeriesEntity getSeriesById(long seriesId) {

        EbikeSeriesEntity ebikeSeriesEntity = DataService.getMainCache().getObj(String.format("BaseData:Ebike:SeriesInfo%s", seriesId));

        if (ebikeSeriesEntity != null) return ebikeSeriesEntity;

        ebikeSeriesEntity = this.findModel(seriesId);

        if (ebikeSeriesEntity == null) return null;

        DataService.getMainCache().setObj(String.format("BaseData:Ebike:SeriesInfo%s", seriesId), ebikeSeriesEntity, 86400 * 1000 * 7);

        return ebikeSeriesEntity;
    }


}