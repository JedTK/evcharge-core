package com.evcharge.entity.erp;


import com.evcharge.enumdata.EInventoryOpType;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

/**
 * 进销存系统：采购订单;
 *
 * @author : JED
 * @date : 2023-1-9
 */
public class ePurchaseOrderEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 订单号
     */
    public String OrderSN;
    /**
     * 供应商ID
     */
    public long supplier_id;
    /**
     * 采购者ID
     */
    public long buy_admin_id;
    /**
     * 单据时间戳
     */
    public long receiptTime;
    /**
     * 交货时间戳
     */
    public long deliveryTime;
    /**
     * 折扣率
     */
    public BigDecimal discountedRate;
    /**
     * 折扣金额
     */
    public BigDecimal discountedAmount;
    /**
     * 税率
     */
    public BigDecimal taxRate;
    /**
     * 税额
     */
    public BigDecimal taxAmount;
    /**
     * 订单总额
     */
    public double totalAmount;
    /**
     * 应付款金额
     */
    public double payableAmount;
    /**
     * 管理员id
     */
    public long admin_id;
    /**
     * 联系人姓名
     */
    public String contacts;
    /**
     * 联系电话
     */
    public String contactsPhone;
    /**
     * 状态：-1-删除，0-草稿，1-待审核，2-审核不通过，3-审核通过，4-入库完成
     */
    public int status;
    /**
     * 备注
     */
    public String remark;
    /**
     * 物流公司，可选
     */
    public String deliveryCompany;
    /**
     * 物流运单号，可选
     */
    public String deliveryOrderSN;
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
    public static ePurchaseOrderEntity getInstance() {
        return new ePurchaseOrderEntity();
    }

    /**
     * 采购订单入库操作
     *
     * @return
     */
    public SyncResult storageWithOrderSN(String OrderSN, long adminId) {
        ePurchaseOrderEntity purchaseOrderEntity = ePurchaseOrderEntity.getInstance()
                .where("OrderSN", OrderSN)
                .findModel();
        if (purchaseOrderEntity == null || purchaseOrderEntity.id == 0)
            return new SyncResult(2, "请选择正确的订单操作");
        //状态：-1-删除，0-草稿，1-待审核，2-审核通过，3-审核不通过，4-入库完成
        if (purchaseOrderEntity.status == 4) {
            return new SyncResult(9, "此订单已经完成入库");
        }
        if (purchaseOrderEntity.status != 2) {
            return new SyncResult(10, "请先审核订单再操作");
        }

        //归类采购商品要入的仓库
        Map<Long, List<ePurchaseOrderGoodsEntity>> goods_list = new LinkedHashMap<>();

        //读取采购订单商品列表
        List<ePurchaseOrderGoodsEntity> goodsEntityList = ePurchaseOrderGoodsEntity.getInstance()
                .where("order_id", purchaseOrderEntity.id)
                .selectList();

        //归类采购商品要入的仓库
        Iterator it = goodsEntityList.iterator();
        while (it.hasNext()) {
            ePurchaseOrderGoodsEntity orderGoodsEntity = (ePurchaseOrderGoodsEntity) it.next();
            List<ePurchaseOrderGoodsEntity> goodsList;
            if (!goods_list.containsKey(orderGoodsEntity.storehouse_id)) {
                goodsList = new LinkedList<>();
            } else {
                goodsList = goods_list.get(orderGoodsEntity.storehouse_id);
            }
            goodsList.add(orderGoodsEntity);
            goods_list.put(orderGoodsEntity.storehouse_id, goodsList);
        }

        return this.beginTransaction(connection -> {
            try {
                //根据将商品存入不同的仓库
                for (Long storehouse_id : goods_list.keySet()) {
                    //复制采购订单中的商品到入库商品列表中
                    List<ePurchaseOrderGoodsEntity> goodsList = goods_list.get(storehouse_id);

                    List<Map<String, Object>> logsGoodsEntityList = new LinkedList<>();
                    Iterator iterator = goodsList.stream().iterator();
                    while (iterator.hasNext()) {
                        ePurchaseOrderGoodsEntity purchaseOrderGoodsEntity = (ePurchaseOrderGoodsEntity) iterator.next();
                        eStorehouseLogsGoodsEntity logsGoodsEntity = new eStorehouseLogsGoodsEntity();
                        Map<String, Object> goods_data = new LinkedHashMap<>();
                        goods_data.put("spu_code", purchaseOrderGoodsEntity.spu_code);
                        goods_data.put("sku_code", purchaseOrderGoodsEntity.sku_code);
                        goods_data.put("count", purchaseOrderGoodsEntity.count);
                        goods_data.put("price", purchaseOrderGoodsEntity.purchasePrice);
                        goods_data.put("remark", purchaseOrderGoodsEntity.remark);
                        logsGoodsEntityList.add(goods_data);
                    }

                    //更新库存数据
                    SyncResult syncResult = eStorehouseEntity.getInstance()
                            .updateStockTransaction(connection
                                    , storehouse_id
                                    , EInventoryOpType.STORAGE
                                    , logsGoodsEntityList
                                    , adminId
                                    , "采购订单入库"
                                    , OrderSN);

                    if (syncResult.code != 0) return new SyncResult(12, syncResult.msg);
                }
                if (purchaseOrderEntity.updateTransaction(connection, purchaseOrderEntity.id, new LinkedHashMap<>() {{
                    put("status", 4);//状态：-1-删除，0-草稿，1-待审核，2-审核通过，3-审核不通过，4-入库完成
                }}) == 0) return new SyncResult(13, "更新采购订单状态失败");
                return new SyncResult(0, "");
            } catch (Exception e) {
                LogsUtil.error(e, "", "采购订单入库操作发生错误");
            }
            return new SyncResult(1, "操作失败");
        });
    }
}
