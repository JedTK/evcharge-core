package com.evcharge.DBSyncer.task.consume_order;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.DBSyncer.core.DBSyncTableField;
import com.evcharge.DBSyncer.core.DBSyncTask;
import com.evcharge.DBSyncer.core.DBSyncTaskTrigger;
import com.evcharge.DBSyncer.core.IDataSyncListener;
import com.evcharge.entity.consumecenter.order.ConsumeOrderItemsEntity;
import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;
import com.evcharge.entity.consumecenter.product.ConsumeProductsEntity;
import com.evcharge.entity.recharge.RechargeConfigEntity;
import com.evcharge.entity.recharge.RechargeOrderEntity;
import com.evcharge.enumdata.ERechargePaymentType;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.ISyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.MapUtil;
import com.xyzs.utils.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 同步 充值订单到 消费订单（新的订单系统）
 */
public class SyncReChargeOrderTask extends DBSyncTaskTrigger {
    /**
     * 获取实例（标准构造模式）
     */
    public static SyncReChargeOrderTask getInstance() {
        return new SyncReChargeOrderTask();
    }

    /**
     * 构造函数：初始化同步任务配置，包括主从表字段映射等
     */
    public SyncReChargeOrderTask() {
        this.TAG = "充值订单同步触发器";
        task = new DBSyncTask();

        task.mainDBName = "";
        task.mainTableName = RechargeOrderEntity.getInstance().theTableName();

        BaseEntity entity = new ConsumeOrdersEntity();
        task.slaveDBName = entity.theDBName();
        task.slaveTableName = entity.theTableName();

        task.MainPrimaryKey = "ordersn";
        task.SlavePrimaryKey = "order_sn";
        this.WHERE_EXPRESS = String.format("status = 2 AND %s", this.WHERE_EXPRESS);

        // 字段映射配置：主表字段 -> 子表字段
        task.FieldMapping = new ArrayList<>() {{
            add(new DBSyncTableField("uid", "uid"));
            add(new DBSyncTableField("ordersn", "order_sn"));
            add(new DBSyncTableField("total_price", "total_price"));
            add(new DBSyncTableField("pay_price", "order_price"));
            add(new DBSyncTableField("use_integral", "use_integral"));
            add(new DBSyncTableField("pay_price", "pay_price"));
            add(new DBSyncTableField("pay_ordersn", "pay_order_sn"));
            add(new DBSyncTableField("status", "payment_status"));
            add(new DBSyncTableField("pay_time", "pay_time"));
            add(new DBSyncTableField("settle_amount", "settle_amount"));
            add(new DBSyncTableField("refund_status", "refund_status"));
            add(new DBSyncTableField("CSId", "CSId"));
            add(new DBSyncTableField("deviceCode", "device_code"));
            add(new DBSyncTableField("port", "port"));
            add(new DBSyncTableField("organize_code", "organize_code"));
            add(new DBSyncTableField("organize_code", "platform_code"));
            add(new DBSyncTableField("create_time", "create_time"));
            add(new DBSyncTableField("update_time", "update_time"));

            add(new DBSyncTableField("paytype_id", ""));
            add(new DBSyncTableField("config_id", ""));
            add(new DBSyncTableField("amount", ""));
            add(new DBSyncTableField("price", ""));
        }};
    }

    /**
     * 批量同步
     *
     * @param params 参数对象，包含必要的同步条件、时间范围、分页信息等
     * @param useMQ  是否通过 MQ 推送任务执行：
     *               - true：推送到消息队列，由消费者异步执行；
     *               - false：在当前线程直接执行。
     * @return
     */
    @Override
    public ISyncResult batch(JSONObject params, boolean useMQ) {
        useMQ = false; // 暂时不支持MQ同步
        return super.batch(params, useMQ, dataSyncListener);
    }

    /**
     * 同步一次/实际同步一条数据的执行函数
     *
     * @param params 参数对象，可包含筛选条件、同步目标、数据源配置等
     * @return
     */
    @Override
    public ISyncResult once(JSONObject params) {
        return super.once(params, dataSyncListener);
    }

    /**
     * 数据同步监听器，同步前回调，同步后回调
     */
    private final IDataSyncListener dataSyncListener = new IDataSyncListener() {

        @Override
        public void onBeforeSyncRecord(Map<String, Object> mainRecord, Map<String, Object> syncRecord) {
            if (mainRecord == null || mainRecord.isEmpty() || syncRecord == null || syncRecord.isEmpty()) return;

            int paytype_id = MapUtil.getInt(mainRecord, "paytype_id");

            syncRecord.put("product_type", "recharge"); // 产品类型：充值
            syncRecord.put("discount_price", 0); // 优惠金额，后续任务补全
            syncRecord.put("coupon_id", 0); // 优惠券ID，后续任务补全
            syncRecord.put("memo", "");

            // 支付方式映射
            switch (paytype_id) {
                case ERechargePaymentType.wechat:
                    syncRecord.put("payment_type", "WechatPay_MP"); // 微信小程序支付
                    break;
                case ERechargePaymentType.wechat_alipay:
                    syncRecord.put("payment_type", "WechatPay_H5"); // 微信H5支付
                    break;
                case ERechargePaymentType.alipay:
                    syncRecord.put("payment_type", "ALIPAY_MP"); // 支付宝支付
                    break;
                case ERechargePaymentType.hmpay:
                    syncRecord.put("payment_type", "HmPay_MP"); // 杉德小程序支付
                    break;
                default:
                    syncRecord.put("payment_type", paytype_id); // 兜底
                    break;
            }
        }

        @Override
        public void onAfterSyncRecord(Map<String, Object> mainRecord, Map<String, Object> syncRecord) {
            String OrderSN = MapUtil.getString(mainRecord, task.MainPrimaryKey);
            if (StringUtil.isEmpty(OrderSN)) return;

            // 查询已经同步的订单
            Map<String, Object> consumeOrderData = ConsumeOrdersEntity.getInstance()
                    .field("id")
                    .where("order_sn", OrderSN)
                    .find();
            if (consumeOrderData == null || consumeOrderData.isEmpty()) {
                LogsUtil.warn(TAG, "缺少[%s]订单数据", OrderSN);
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("order_id", MapUtil.getLong(consumeOrderData, "id"));
            data.put("order_sn", OrderSN);

            long product_id = 0;
            // region remark - 查询旧表充值配置
            long config_id = MapUtil.getLong(mainRecord, "config_id");
            Map<String, Object> config_data = RechargeConfigEntity.getInstance()
                    .cache(String.format("Recharge:Config:%s", config_id))
                    .where("id", config_id)
                    .find();
            if (config_data != null && !config_data.isEmpty()) {
                product_id = MapUtil.getLong(config_data, "product_id");
                data.put("product_id", product_id);
            }
            // endregion

            // region remark - 查询产品详情
            if (product_id > 0) {
                ConsumeProductsEntity consumeProductsEntity = ConsumeProductsEntity.getInstance()
                        .cache(String.format("Consume:Products:%s", product_id))
                        .where("id", product_id)
                        .findEntity();
                if (consumeProductsEntity != null) {
                    data.put("title", String.format("%s-%s", consumeProductsEntity.title, consumeProductsEntity.subtitle));
                }
            }
            // endregion

            int quantity = MapUtil.getInt(mainRecord, "amount");
            data.put("quantity", quantity);
            data.put("price", MapUtil.getDouble(mainRecord, "price"));

            if (ConsumeOrderItemsEntity.getInstance()
                    .where("order_sn", OrderSN)
                    .exist()) {
                ConsumeOrderItemsEntity.getInstance()
                        .where("order_sn", OrderSN)
                        .update(data);
            } else ConsumeOrderItemsEntity.getInstance().insert(data);
        }
    };
}
