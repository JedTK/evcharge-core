package com.evcharge.entity.erp;


import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.spusku.zSpuEntity;
import com.evcharge.enumdata.EInventoryOpType;
import com.xyzs.database.ISqlDBObject;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.TimeUtil;
import org.springframework.util.StringUtils;


import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 合伙人：库存;
 *
 * @author : JED
 * @date : 2023-1-11
 */
public class PartnerInventoryEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 管理员id
     */
    public long admin_id;
    /**
     * 组织id
     */
    public long organize_id;
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
     * 备注
     */
    public String remark;

    //endregion
    /**
     * 获得一个实例
     * @return
     */

    /**
     * 获得一个实例
     *
     * @return
     */
    public static PartnerInventoryEntity getInstance() {
        return new PartnerInventoryEntity();
    }

    /**
     * 检查仓库商品是否存在数据
     *
     * @param connection 事务
     * @param admin_id   仓库id
     * @param sku_code   sku编码
     * @return
     */
    public SyncResult checkTransaction(Connection connection, long admin_id, String sku_code) throws Exception {
        if (!this.where("admin_id", admin_id)
                .where("sku_code", sku_code).exist()) {
            zSpuEntity zspuEntity = zSpuEntity.getInstance().getWithSkuCode(sku_code);
            if (zspuEntity == null || zspuEntity.id == 0) return new SyncResult(13, "无效的sku编码,error-13");

            PartnerInventoryEntity inventoryEntity = new PartnerInventoryEntity();
            inventoryEntity.admin_id = admin_id;
            inventoryEntity.spu_code = zspuEntity.spu_code;
            inventoryEntity.sku_code = sku_code;
            inventoryEntity.stock = 0;
            inventoryEntity.remark = remark;
            if (inventoryEntity.insertTransaction(connection) > 0) return new SyncResult(0, "");
        } else return new SyncResult(0, "");
        return new SyncResult(40, "合作伙伴新增商品库存数据失败");
    }

    //region 更新库存操作，并自动生成日志信息

    /**
     * 更新库存数据
     *
     * @param connection       事务
     * @param admin_id         管理员
     * @param eInventoryOpType 出入库类型
     * @param goods_list       库存商品
     * @return
     * @throws SQLException
     * @throws IllegalAccessException
     */
    public SyncResult updateStockTransaction(Connection connection
            , long admin_id
            , EInventoryOpType eInventoryOpType
            , List<Map<String, Object>> goods_list
    ) throws Exception {
        return updateStockTransaction(connection, admin_id, eInventoryOpType, goods_list, "", "", "");
    }

    /**
     * 更新库存数据
     *
     * @param connection       事务
     * @param admin_id         管理员
     * @param eInventoryOpType 出入库类型
     * @param goods_list       库存商品
     * @param remark           备注
     * @return
     * @throws SQLException
     * @throws IllegalAccessException
     */
    public SyncResult updateStockTransaction(Connection connection
            , long admin_id
            , EInventoryOpType eInventoryOpType
            , List<Map<String, Object>> goods_list
            , String remark
    ) throws Exception {
        return updateStockTransaction(connection, admin_id, eInventoryOpType, goods_list, remark, "", "");
    }

    /**
     * 更新库存数据
     *
     * @param connection       事务
     * @param admin_id         管理员
     * @param eInventoryOpType 出入库类型
     * @param goods_list       库存商品
     * @param remark           备注
     * @param orderSN          （可选）关联订单号
     * @return
     * @throws SQLException
     * @throws IllegalAccessException
     */
    public SyncResult updateStockTransaction(Connection connection
            , long admin_id
            , EInventoryOpType eInventoryOpType
            , List<Map<String, Object>> goods_list
            , String remark
            , String orderSN
    ) throws Exception {
        return updateStockTransaction(connection, admin_id, eInventoryOpType, goods_list, remark, orderSN, "");
    }

    /**
     * 更新库存数据
     *
     * @param connection       事务
     * @param admin_id         管理员
     * @param eInventoryOpType 出入库类型
     * @param goods_list       库存商品
     * @param remark           备注
     * @param orderSN          （可选）关联订单号
     * @param extraData        （可选）额外参数
     * @return
     * @throws SQLException
     * @throws IllegalAccessException
     */
    public SyncResult updateStockTransaction(Connection connection
            , long admin_id
            , EInventoryOpType eInventoryOpType
            , List<Map<String, Object>> goods_list
            , String remark
            , String orderSN
            , Map<String, Object> extraData
    ) throws Exception {
        return updateStockTransaction(connection, admin_id, eInventoryOpType, goods_list, remark, orderSN, MapUtil.toJSONString(extraData));
    }

    /**
     * 更新库存数据
     *
     * @param connection       事务
     * @param admin_id         管理员
     * @param eInventoryOpType 出入库类型
     * @param goods_list       库存商品
     * @param remark           备注
     * @param orderSN          （可选）关联订单号
     * @param extraData        （可选）额外参数
     * @return
     * @throws SQLException
     * @throws IllegalAccessException
     */
    public SyncResult updateStockTransaction(Connection connection
            , long admin_id
            , EInventoryOpType eInventoryOpType
            , List<Map<String, Object>> goods_list
            , String remark
            , String orderSN
            , JSONObject extraData
    ) throws Exception {
        return updateStockTransaction(connection, admin_id, eInventoryOpType, goods_list, remark, orderSN, extraData.toJSONString());
    }

    /**
     * 更新库存数据
     *
     * @param connection       事务
     * @param admin_id         管理员
     * @param eInventoryOpType 出入库类型
     * @param goods_list       库存商品
     * @param remark           备注
     * @param orderSN          （可选）关联订单号
     * @param extraData        （可选）额外参数
     * @return
     * @throws SQLException
     * @throws IllegalAccessException
     */
    public SyncResult updateStockTransaction(Connection connection
            , long admin_id
            , EInventoryOpType eInventoryOpType
            , List<Map<String, Object>> goods_list
            , String remark
            , String orderSN
            , String extraData
    ) throws Exception {
        //新增操作日志
        PartnerInventoryLogsEntity logsEntity = new PartnerInventoryLogsEntity();
        logsEntity.admin_id = admin_id;
        logsEntity.typeId = eInventoryOpType.index;
        logsEntity.orderSN = orderSN;
        logsEntity.extraData = extraData;
        logsEntity.remark = remark;
        logsEntity.create_time = TimeUtil.getTimestamp();
        logsEntity.id = logsEntity.insertGetIdTransaction(connection);
        if (logsEntity.id == 0) return new SyncResult(10, "新增操作日志失败");

        //新增操作日志商品信息
        Iterator it = goods_list.iterator();
        while (it.hasNext()) {
            Map<String, Object> nd = (Map<String, Object>) it.next();

            PartnerInventoryLogsGoodsEntity logsGoodsEntity = new PartnerInventoryLogsGoodsEntity();
            logsGoodsEntity.logs_id = logsEntity.id;
            logsGoodsEntity.spu_code = MapUtil.getString(nd, "spu_code");
            logsGoodsEntity.sku_code = MapUtil.getString(nd, "sku_code");
            logsGoodsEntity.count = MapUtil.getInt(nd, "count");
            logsGoodsEntity.remark = MapUtil.getString(nd, "remark");

            if (!StringUtils.hasLength(logsGoodsEntity.sku_code)) {
                throw new Exception("PartnerInventoryLogsGoodsEntity sku_code 不能为空");
            }
            if (!StringUtils.hasLength(logsGoodsEntity.spu_code)) {
                zSpuEntity zspuEntity = zSpuEntity.getInstance().getWithSkuCode(logsGoodsEntity.sku_code);
                if (zspuEntity == null || zspuEntity.id == 0) return new SyncResult(13, "无效的sku编码,error-13");
                logsGoodsEntity.spu_code = zspuEntity.spu_code;
            }
            if (logsGoodsEntity.count == 0) continue;
            if (logsGoodsEntity.insertTransaction(connection) == 0) {
                return new SyncResult(11, "新增操作日志商品信息失败");
            }

            SyncResult syncResult = checkTransaction(connection, admin_id, logsGoodsEntity.sku_code);
            if (syncResult.code != 0) return syncResult;

            //获取变化的库存数值
            int stock = logsGoodsEntity.count;
            PartnerInventoryEntity partnerInventoryEntity = PartnerInventoryEntity.getInstance();
            partnerInventoryEntity.where("admin_id", admin_id).where("sku_code", logsGoodsEntity.sku_code);

            //这里判断处理只是担心库存数搞乱正负值
            if (eInventoryOpType == EInventoryOpType.STORAGE) {
                stock = Math.abs(stock);
            } else if (eInventoryOpType == EInventoryOpType.TAKEOUT) {
                stock = -Math.abs(stock);
            }
            if (stock > 0) {
                partnerInventoryEntity.inc("stock", logsGoodsEntity.count);
            } else {
                partnerInventoryEntity.dec("stock", stock);
            }

            int noquery = partnerInventoryEntity.updateTransaction(connection);
            if (noquery == 0) return new SyncResult(12, "更新库存数据失败");
        }

        return new SyncResult(0, "");
    }
    //endregion

    /**
     * 根据仓库id和sku编码获取库存
     *
     * @param connection 事务
     * @param admin_id   合作伙伴id
     * @param sku_code   sku编码
     * @return
     * @throws SQLException
     */
    public int getStockTransaction(Connection connection, long admin_id, String sku_code) throws SQLException {
        PartnerInventoryEntity inventoryEntity = this.field("id,stock")
                .where("admin_id", admin_id)
                .where("sku_code", sku_code)
                .findModelTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.ShareLock);
        if (inventoryEntity == null || inventoryEntity.id == 0) return 0;
        return inventoryEntity.stock;
    }
}
