package com.evcharge.entity.spusku;


import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.utils.MapUtil;

import java.io.Serializable;
import java.util.Map;

/**
 * 商品库存 1个SPU对应多个SKU（库存量单位）;
 *
 * @author : JED
 * @date : 2023-1-6
 */
public class zSkuEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * sku编码，用于关联其他系统
     */
    public String sku_code;
    /**
     * SKU标题
     */
    public String title;
    /**
     * spu ID
     */
    public long spu_id;
    /**
     * spu编码，用于关联其他系统
     */
    public String spu_code;
    /**
     * sku分类ID
     */
    public long sku_type_id;
    /**
     * 单价
     */
    public double sellPrice;
    /**
     * 计量单位
     */
    public String unitText;
    /**
     * 库存数量
     */
    public int stock;
    /**
     * 状态：0-删除，1-正常，2-下架
     */
    public int status;
    /**
     * 排序索引
     */
    public int sort_index;
    /**
     * 创建时间戳
     */
    public long create_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static zSkuEntity getInstance() {
        return new zSkuEntity();
    }

    /**
     * 根据SKU编码查询SKU信息
     *
     * @param sku_code
     * @return
     */
    public zSkuEntity getWithSkuCode(String sku_code) {
        return this.cache(String.format("BaseData:Sku:%s", sku_code))
                .where("sku_code", sku_code)
//                .where("status", 1)
                .findModel();
    }

    /**
     * 根据sku编码获取商品详情
     *
     * @param sku_code sku编码
     * @return
     */
    public Map<String, Object> getDetaisWithSkuCode(String sku_code) {
        Map<String, Object> data = DataService.getMainCache().getMap(String.format("BaseData:Sku:%s:Detail", sku_code));
        if (data != null && data.size() > 0) return data;

        data = this.field("b.id,a.spu_code,b.sku_code,a.title,a.subtitle,a.category_id,a.brand_id,a.merchantCode,a.goodsCode,a.goodsBarcode,a.attrList,a.unitText,b.sellPrice,b.sku_type_id")
                .alias("b")
                .join(zSpuEntity.getInstance().theTableName(), "a", "a.id = b.spu_id")
                .where("b.sku_code", sku_code)
                .where("b.status", 1)
                .order("b.sort_index DESC,b.create_time,b.id")
                .find();

        long category_id = MapUtil.getLong(data, "category_id");
        data.put("category_name", zSpuCategoryEntity.getInstance().getNameWithId(category_id));

        long brand_id = MapUtil.getLong(data, "brand_id");
        data.put("brand_name", zSpuBrandEntity.getInstance().getNameWithId(brand_id));

        long sku_type_id = MapUtil.getLong(data, "sku_type_id");
        data.put("sku_type_title", zSkuTypeEntity.getInstance().getTitleWithId(sku_type_id));

        DataService.getMainCache().setMap(String.format("BaseData:Sku:%s:Detail", sku_code), data);
        return data;
    }
}
