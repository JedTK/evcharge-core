package com.evcharge.entity.ebike;

import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;

import java.io.Serializable;

/**
 * 电动车品牌表;
 *
 * @author : JED
 * @date : 2022-10-18
 */
public class EbikeBrandEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */

    public long id;
    /**
     * 图标
     */
    public String icon;
    /**
     * 标题
     */
    public String title;
    /*
    * 排序
    * */
    public long sort;
    /**
     * 创建时间
     */
    public long createTime;
    /**
     * 更新时间
     */
    public long updateTime;


    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static EbikeBrandEntity getInstance() {
        return new EbikeBrandEntity();
    }

    /**
     * 根据品牌id获取品牌信息
     * @param brandId
     * @return
     */
    public EbikeBrandEntity getBrandById(long brandId) {

        EbikeBrandEntity ebikeBrandEntity = DataService.getMainCache().getObj(String.format("BaseData:Ebike:BrandInfo%s", brandId));

        if (ebikeBrandEntity != null) return ebikeBrandEntity;

        ebikeBrandEntity = this.findModel(brandId);

        if (ebikeBrandEntity == null) return null;

        DataService.getMainCache().setObj(String.format("BaseData:Ebike:BrandInfo%s", brandId), ebikeBrandEntity, 86400 * 1000 * 7);

        return ebikeBrandEntity;
    }


}