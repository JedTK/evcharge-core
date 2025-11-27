package com.evcharge.DBSyncer.task.consume_order;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.DBSyncer.core.DBSyncTableField;
import com.evcharge.DBSyncer.core.DBSyncTask;
import com.evcharge.DBSyncer.core.DBSyncTaskTrigger;
import com.evcharge.DBSyncer.core.IDataSyncListener;
import com.evcharge.entity.chargecard.UserChargeCardRefundOrderEntity;
import com.evcharge.entity.consumecenter.order.ConsumeOrderRefundsEntity;
import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;
import com.evcharge.entity.recharge.RechargeRefundOrderEntity;
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
public class SyncChargeCardRefundOrderTask extends DBSyncTaskTrigger {
    /**
     * 获取实例（标准构造模式）
     */
    public static SyncChargeCardRefundOrderTask getInstance() {
        return new SyncChargeCardRefundOrderTask();
    }

    /**
     * 构造函数：初始化同步任务配置，包括主从表字段映射等
     */
    public SyncChargeCardRefundOrderTask() {
        this.TAG = "充值订单同步触发器";
        task = new DBSyncTask();

        task.mainDBName = "";
        task.mainTableName = UserChargeCardRefundOrderEntity.getInstance().theTableName();

        BaseEntity entity = new ConsumeOrderRefundsEntity();
        task.slaveDBName = entity.theDBName();
        task.slaveTableName = entity.theTableName();

        task.MainPrimaryKey = "refund_OrderSN";
        task.SlavePrimaryKey = "refund_order_sn";
        this.WHERE_EXPRESS = String.format("%s", this.WHERE_EXPRESS);

        // 字段映射配置：主表字段 -> 子表字段
        task.FieldMapping = new ArrayList<>() {{
            add(new DBSyncTableField("uid", "uid"));
            add(new DBSyncTableField("OrderSN", "order_sn"));
            add(new DBSyncTableField("refund_OrderSN", "refund_order_sn"));
            add(new DBSyncTableField("refund_amount", "refund_amount"));
            add(new DBSyncTableField("refund_reason", "refund_reason"));
            add(new DBSyncTableField("refund_bank_order_no", "refund_bank_order_no"));
            add(new DBSyncTableField("refund_bank_trx_no", "refund_bank_trx_no"));
            add(new DBSyncTableField("create_time", "create_time"));
            add(new DBSyncTableField("update_time", "update_time"));

            add(new DBSyncTableField("refund_status", ""));
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

            int refund_status = MapUtil.getInt(mainRecord, "refund_status");
            // 退款状态
            switch (refund_status) {
                case 0:
                    syncRecord.put("status", "PENDING"); // 待处理
                    break;
                case 1:
                    syncRecord.put("status", "PENDING"); // 处理中
                    break;
                case 2:
                    syncRecord.put("status", "SUCCESS"); // 已完成
                    break;
                case 3:
                    syncRecord.put("status", "FAILED"); // 已取消
                    break;
                case 4:
                    syncRecord.put("status", "FAILED"); // 失败
                    break;
//                case 5:
//                    syncRecord.put("status", refund_status); // 部分退款
//                    break;
//                case 6:
//                    syncRecord.put("status", refund_status); // 全额退款
//                    break;
//                case 7:
//                    syncRecord.put("status", refund_status); // 审核中
//                    break;
//                case 8:
//                    syncRecord.put("status", refund_status); // 审核失败
//                    break;
                default:
                    syncRecord.put("status", refund_status); // 审核失败
                    break;
            }
        }

        @Override
        public void onAfterSyncRecord(Map<String, Object> mainRecord, Map<String, Object> syncRecord) {
            String order_sn = MapUtil.getString(mainRecord, "OrderSN");
            String refund_order_sn = MapUtil.getString(mainRecord, "refund_OrderSN");
            if (StringUtil.isEmpty(order_sn)) return;

            // 查询已经同步的订单
            Map<String, Object> orderData = ConsumeOrdersEntity.getInstance()
                    .field("id")
                    .where("order_sn", order_sn)
                    .find();
            if (orderData == null || orderData.isEmpty()) {
                LogsUtil.warn(TAG, "缺少[%s]订单数据", refund_order_sn);
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("order_id", MapUtil.getLong(orderData, "id"));
            if (ConsumeOrderRefundsEntity.getInstance()
                    .where("refund_order_sn", refund_order_sn)
                    .exist()) {
                ConsumeOrderRefundsEntity.getInstance()
                        .where("refund_order_sn", refund_order_sn)
                        .update(data);
            } else ConsumeOrderRefundsEntity.getInstance().insert(data);
        }
    };
}
