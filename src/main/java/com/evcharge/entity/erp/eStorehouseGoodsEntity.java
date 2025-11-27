package com.evcharge.entity.erp;


import com.evcharge.entity.spusku.zSpuEntity;
import com.xyzs.database.ISqlDBObject;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 进销存系统：仓库SKU商品;
 *
 * @author : JED
 * @date : 2023-1-9
 */
public class eStorehouseGoodsEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 仓库id
     */
    public long storehouse_id;
    /**
     * spu编码
     */
    public String spu_code;
    /**
     * sku编码
     */
    public String sku_code;
    /**
     * 库存数量
     */
    public int stock;
    /**
     * 商品编号,货号，用于查找货物存放位置
     */
    public String goodsCode;
    /**
     * 备注
     */
    public String remark;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static eStorehouseGoodsEntity getInstance() {
        return new eStorehouseGoodsEntity();
    }

    /**
     * 检查仓库商品是否存在数据
     *
     * @param connection    事务
     * @param storehouse_id 仓库id
     * @param sku_code      sku编码
     * @return
     */
    public SyncResult checkTransaction(Connection connection, long storehouse_id, String sku_code) throws SQLException, IllegalAccessException {
        if (!eStorehouseGoodsEntity.getInstance()
                .where("storehouse_id", storehouse_id)
                .where("sku_code", sku_code).exist()) {
            zSpuEntity zspuEntity = zSpuEntity.getInstance().getWithSkuCode(sku_code);
            if (zspuEntity == null || zspuEntity.id == 0) return new SyncResult(13, "无效的sku编码,error-13");

            eStorehouseGoodsEntity goodsEntity = new eStorehouseGoodsEntity();
            goodsEntity.storehouse_id = storehouse_id;
            goodsEntity.spu_code = zspuEntity.spu_code;
            goodsEntity.sku_code = sku_code;
            goodsEntity.stock = 0;
            goodsEntity.goodsCode = "";
            goodsEntity.remark = "";
            if (goodsEntity.insertTransaction(connection) > 0) return new SyncResult(0, "");
        } else return new SyncResult(0, "");
        return new SyncResult(1, "仓库新增商品信息失败");
    }

    /**
     * 根据仓库id和sku编码获取库存
     *
     * @param connection    事务
     * @param storehouse_id 仓库id
     * @param sku_code      sku编码
     * @return
     * @throws SQLException
     */
    public int getStockTransaction(Connection connection, long storehouse_id, String sku_code) throws SQLException {
        eStorehouseGoodsEntity goodsEntity = eStorehouseGoodsEntity.getInstance()
                .field("id,stock")
                .where("storehouse_id", storehouse_id)
                .where("sku_code", sku_code)
                .findModelTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.ShareLock);
        if (goodsEntity == null || goodsEntity.id == 0) return 0;
        return goodsEntity.stock;
    }
}
