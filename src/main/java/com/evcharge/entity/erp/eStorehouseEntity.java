package com.evcharge.entity.erp;


import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.spusku.zSpuEntity;
import com.evcharge.enumdata.EInventoryOpType;
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
 * 进销存系统：仓库;
 *
 * @author : JED
 * @date : 2023-1-9
 */
public class eStorehouseEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 仓库名
     */
    public String name;
    /**
     * 拼音
     */
    public String pinyin;
    /**
     * 联系人姓名
     */
    public String contacts;
    /**
     * 联系电话
     */
    public String contactsPhone;
    /**
     * 省
     */
    public String province;
    /**
     * 市
     */
    public String city;
    /**
     * 区
     */
    public String district;
    /**
     * 街道，可能为空字串
     */
    public String street;
    /**
     * 门牌，可能为空字串
     */
    public String street_number;
    /**
     * 排序索引
     */
    public int sort_index;
    /**
     * 管理员id
     */
    public long admin_id;
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
    public static eStorehouseEntity getInstance() {
        return new eStorehouseEntity();
    }

    //region 更新仓库库存数据，自动增加出入库日志

    /**
     * 更新库存数据
     *
     * @param connection       事务
     * @param storehouse_id    仓库id
     * @param eInventoryOpType 出入库类型
     * @param goods_list       库存商品
     * @param admin_id         操作管理员
     * @return
     * @throws SQLException
     * @throws IllegalAccessException
     */
    public SyncResult updateStockTransaction(Connection connection
            , long storehouse_id
            , EInventoryOpType eInventoryOpType
            , List<Map<String, Object>> goods_list
            , long admin_id
    ) throws Exception {
        return updateStockTransaction(connection, storehouse_id, eInventoryOpType, goods_list, admin_id, "", "", "");
    }

    /**
     * 更新库存数据
     *
     * @param connection       事务
     * @param storehouse_id    仓库id
     * @param eInventoryOpType 出入库类型
     * @param goods_list       库存商品
     * @param admin_id         操作管理员
     * @param remark           备注
     * @return
     * @throws SQLException
     * @throws IllegalAccessException
     */
    public SyncResult updateStockTransaction(Connection connection
            , long storehouse_id
            , EInventoryOpType eInventoryOpType
            , List<Map<String, Object>> goods_list
            , long admin_id
            , String remark
    ) throws Exception {
        return updateStockTransaction(connection, storehouse_id, eInventoryOpType, goods_list, admin_id, remark, "", "");
    }

    /**
     * 更新库存数据
     *
     * @param connection       事务
     * @param storehouse_id    仓库id
     * @param eInventoryOpType 出入库类型
     * @param goods_list       库存商品
     * @param admin_id         操作管理员
     * @param remark           备注
     * @param orderSN          （可选）关联订单号
     * @return
     * @throws SQLException
     * @throws IllegalAccessException
     */
    public SyncResult updateStockTransaction(Connection connection
            , long storehouse_id
            , EInventoryOpType eInventoryOpType
            , List<Map<String, Object>> goods_list
            , long admin_id
            , String remark
            , String orderSN
    ) throws Exception {
        return updateStockTransaction(connection, storehouse_id, eInventoryOpType, goods_list, admin_id, remark, orderSN, "");
    }

    /**
     * 更新库存数据
     *
     * @param connection       事务
     * @param storehouse_id    仓库id
     * @param eInventoryOpType 出入库类型
     * @param goods_list       库存商品
     * @param admin_id         操作管理员
     * @param remark           备注
     * @param orderSN          （可选）关联订单号
     * @param extraData        （可选）额外参数
     * @return
     * @throws SQLException
     * @throws IllegalAccessException
     */
    public SyncResult updateStockTransaction(Connection connection
            , long storehouse_id
            , EInventoryOpType eInventoryOpType
            , List<Map<String, Object>> goods_list
            , long admin_id
            , String remark
            , String orderSN
            , Map<String, Object> extraData
    ) throws Exception {
        return updateStockTransaction(connection, storehouse_id, eInventoryOpType, goods_list, admin_id, remark, orderSN, MapUtil.toJSONString(extraData));
    }

    /**
     * 更新库存数据
     *
     * @param connection       事务
     * @param storehouse_id    仓库id
     * @param eInventoryOpType 出入库类型
     * @param goods_list       库存商品
     * @param admin_id         操作管理员
     * @param remark           备注
     * @param orderSN          （可选）关联订单号
     * @param extraData        （可选）额外参数
     * @return
     * @throws SQLException
     * @throws IllegalAccessException
     */
    public SyncResult updateStockTransaction(Connection connection
            , long storehouse_id
            , EInventoryOpType eInventoryOpType
            , List<Map<String, Object>> goods_list
            , long admin_id
            , String remark
            , String orderSN
            , JSONObject extraData
    ) throws Exception {
        return updateStockTransaction(connection, storehouse_id, eInventoryOpType, goods_list, admin_id, remark, orderSN, extraData.toJSONString());
    }

    /**
     * 更新库存数据
     *
     * @param connection       事务
     * @param storehouse_id    仓库id
     * @param eInventoryOpType 出入库类型
     * @param goods_list       库存商品
     * @param admin_id         操作管理员
     * @param remark           备注
     * @param orderSN          （可选）关联订单号
     * @param extraData        （可选）额外参数
     * @return
     * @throws SQLException
     * @throws IllegalAccessException
     */
    public SyncResult updateStockTransaction(Connection connection
            , long storehouse_id
            , EInventoryOpType eInventoryOpType
            , List<Map<String, Object>> goods_list
            , long admin_id
            , String remark
            , String orderSN
            , String extraData
    ) throws Exception {
        //检查仓库是否存在
        eStorehouseEntity storehouseEntity = this.cache(String.format("Storehouse:%s:Detail", storehouse_id)).findModel(storehouse_id);
        if (storehouseEntity == null || storehouseEntity.id == 0) return new SyncResult(2, "不存在此仓库，请检查仓库id");

        eStorehouseLogsEntity logsEntity = new eStorehouseLogsEntity();
        logsEntity.storehouse_id = storehouse_id;
        logsEntity.typeId = eInventoryOpType.index;
        logsEntity.remark = remark;
        logsEntity.orderSN = orderSN;
        logsEntity.extraData = extraData;
        logsEntity.admin_id = admin_id;
        logsEntity.create_time = TimeUtil.getTimestamp();
        logsEntity.id = logsEntity.insertGetIdTransaction(connection);
        if (logsEntity.id == 0) return new SyncResult(10, "新增仓库操作日志失败");

        Iterator it = goods_list.iterator();
        while (it.hasNext()) {
            Map<String, Object> nd = (Map<String, Object>) it.next();

            eStorehouseLogsGoodsEntity logsGoodsEntity = new eStorehouseLogsGoodsEntity();
            logsGoodsEntity.logs_id = logsEntity.id;
            logsGoodsEntity.spu_code = MapUtil.getString(nd, "spu_code");
            logsGoodsEntity.sku_code = MapUtil.getString(nd, "sku_code");
            logsGoodsEntity.count = MapUtil.getInt(nd, "count");
            logsGoodsEntity.price = MapUtil.getDouble(nd, "price", 0.0);
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
                return new SyncResult(11, "新增仓库操作日志商品信息失败");
            }

            eStorehouseGoodsEntity goodsEntity = eStorehouseGoodsEntity.getInstance();
            SyncResult checkResult = goodsEntity.checkTransaction(connection, storehouse_id, logsGoodsEntity.sku_code);
            if (checkResult.code != 0) {
                return checkResult;
            }
            goodsEntity.where("storehouse_id", storehouse_id).where("sku_code", logsGoodsEntity.sku_code);

            //获取变化的库存数值
            int stock = logsGoodsEntity.count;
            //这里判断处理只是担心库存数搞乱正负值
            if (eInventoryOpType == EInventoryOpType.STORAGE) {
                stock = Math.abs(stock);
            } else if (eInventoryOpType == EInventoryOpType.TAKEOUT) {
                stock = -Math.abs(stock);
            }
            if (stock > 0) {
                goodsEntity.inc("stock", logsGoodsEntity.count);
            } else {
                goodsEntity.dec("stock", stock);
            }

            int noquery = goodsEntity.updateTransaction(connection);
            if (noquery == 0) return new SyncResult(12, "更新库存数据失败");
        }

        return new SyncResult(0, "");
    }
    //endregion
}
