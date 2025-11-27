package com.evcharge.entity.spusku;


import com.xyzs.entity.BaseEntity;
import com.xyzs.utils.LogsUtil;

import java.io.Serializable;

/**
 * 商品（标准化产品单元）;
 *
 * @author : JED
 * @date : 2023-1-9
 */
public class zSpuEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * spu编码，用于关联其他系统
     */
    public String spu_code;
    /**
     * 商品标题
     */
    public String title;
    /**
     * 商品副标题
     */
    public String subtitle;
    /**
     * 分类ID
     */
    public long category_id;
    /**
     * 品牌ID
     */
    public long brand_id;
    /**
     * 商家编码
     */
    public String merchantCode;
    /**
     * 商品编号,货号
     */
    public String goodsCode;
    /**
     * 商品条形码
     */
    public String goodsBarcode;
    /**
     * 属性JSON格式
     */
    public String attrList;
    /**
     * 计量单位
     */
    public String unitText;
    /**
     * 排序索引
     */
    public int sort_index;
    /**
     * 状态：0=删除，1=正常，2=待审核
     */
    public int status;
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
    public static zSpuEntity getInstance() {
        return new zSpuEntity();
    }

    /**
     * 通过sku编码获取spu商品信息
     *
     * @param sku_code
     * @return
     */
    public zSpuEntity getWithSkuCode(String sku_code) {
        zSkuEntity zSku = zSkuEntity.getInstance().getWithSkuCode(sku_code);
        if (zSku == null || zSku.id == 0) {
            LogsUtil.error("无效sku_code=%s", sku_code);
            return null;
        }
        return this.cache(String.format("BaseData:Spu:%s", zSku.spu_code))
                .where("spu_code", zSku.spu_code)
//                .where("status", 1)
                .findModel();
    }

    /**
     * 根据spuid 获取spu信息
     * @param spuId
     * @return
     */
    public zSpuEntity getInfoWithId (long spuId){
        return this.where("id",spuId)
                .cache(String.format("BaseData:Spu:%s",spuId))
                .findModel();
    }




}
