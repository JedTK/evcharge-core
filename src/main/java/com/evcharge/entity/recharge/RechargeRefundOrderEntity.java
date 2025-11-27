package com.evcharge.entity.recharge;


import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.entity.user.UserSummaryEntity;
import com.evcharge.enumdata.ERechargePaymentType;
import com.evcharge.enumdata.ERefundStatus;
import com.evcharge.libsdk.Hmpay.HmPaymentSDK;
import com.evcharge.libsdk.aliyun.AliPaymentSDK;
import com.evcharge.libsdk.wechat.WechatPaySDK;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 充值退款订单;
 *
 * @author : JED
 * @date : 2023-9-20 13:57:15 新增充值退款订单逻辑
 */
public class RechargeRefundOrderEntity extends BaseEntity implements Serializable {
    //region -- 实体类属性 --
    /**
     * id
     */
    public long id;
    /**
     * 用户ID
     */
    public long uid;
    /**
     * 退款订单号，唯一
     */
    public String refund_OrderSN;
    /**
     * 原订单号，用于关联数据
     */
    public String OrderSN;
    /**
     * 支付类型id
     */
    public int paytype_id;
    /**
     * 状态：0-待处理，1-处理中，2-已完成，3-已取消，4-失败，5-部分退款，6-全额退款，7-审核中，8-审核失败
     */
    public int refund_status;
    /**
     * 退款金额
     */
    public double refund_amount;
    /**
     * 退款银行订单号
     */
    public String refund_bank_order_no;
    /**
     * 退款银行流水号
     */
    public String refund_bank_trx_no;
    /**
     * 退款原因
     */
    public String refund_reason;
    /**
     * 是否为测试订单，0=否，1=是
     */
    public int isTest;
    /**
     * 测试ID
     */
    public long testId;
    /**
     * 充电桩ID
     */
    public String CSId;
    /**
     * 组织代码
     */
    public String organize_code;
    /**
     * 设备号
     */
    public String deviceCode;
    /**
     * 设备端口
     */
    public int port;
    /**
     * 创建时间
     */
    public long create_time;
    /**
     * 更新时间
     */
    public long update_time;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static RechargeRefundOrderEntity getInstance() {
        return new RechargeRefundOrderEntity();
    }

    /**
     * 退款操作
     *
     * @param OrderSN       充值订单号
     * @param refund_amount 退款金额，退款金额和充值金额相同为全额退款，退款金额小于充值金额为部分退款，退款金额不能大于充值金额也不能为0
     * @param refund_reason 退款理由
     * @return
     */
    public SyncResult refund(String OrderSN, double refund_amount, String refund_reason) {
        if (!StringUtils.hasLength(OrderSN)) return new SyncResult(2, "请输入订单号");
        if (refund_amount == 0) return new SyncResult(2, "请输入退款金额");

        refund_amount = -Math.abs(refund_amount);

        RechargeOrderEntity orderEntity = RechargeOrderEntity.getInstance()
                .where("ordersn", OrderSN)
                .findEntity();
        if (orderEntity == null || orderEntity.id == 0) return new SyncResult(3, "查询订单数据失败");

        //状态;1=未支付 2=已完成 -1=已取消，3=全额退款，4=部分退款
        switch (orderEntity.status) {
            case -1:
                return new SyncResult(4, "订单已经取消");
            case 1:
                return new SyncResult(5, "订单还没支付");
            case 3://全额退款
                return new SyncResult(6, "订单已全部退款");
        }
        //检查之前的退款金额
        double total_refund_amount = RechargeRefundOrderEntity.getInstance()
                .where("OrderSN", orderEntity.ordersn)
                .where("refund_status", ERefundStatus.Completed.index)
                .sum("refund_amount");
        if (Math.abs(refund_amount + total_refund_amount) > orderEntity.pay_price) {
            return new SyncResult(8, "退款金额不能大于原支付金额");
        }

        String organize_code = orderEntity.organize_code;
        if (!StringUtil.hasLength(organize_code)) {
            organize_code = SysGlobalConfigEntity.getString("System:Organize:Code");
        }

        RechargeRefundOrderEntity refundOrderEntity = RechargeRefundOrderEntity.getInstance();
        refundOrderEntity.uid = orderEntity.uid;
        refundOrderEntity.OrderSN = orderEntity.ordersn;
        refundOrderEntity.paytype_id = orderEntity.paytype_id;
        refundOrderEntity.refund_status = ERefundStatus.Completed.index;
        refundOrderEntity.refund_amount = refund_amount;
        refundOrderEntity.refund_reason = refund_reason;
        refundOrderEntity.isTest = orderEntity.isTest;
        refundOrderEntity.testId = orderEntity.testId;
        refundOrderEntity.CSId = orderEntity.CSId;
        refundOrderEntity.organize_code = organize_code;
        refundOrderEntity.deviceCode = orderEntity.deviceCode;
        refundOrderEntity.port = orderEntity.port;
        refundOrderEntity.create_time = TimeUtil.getTimestamp();
        refundOrderEntity.update_time = TimeUtil.getTimestamp();
        refundOrderEntity.refund_OrderSN = "";
        SyncResult r = new SyncResult(1, "退款失败，缺少支付方式");
        if (orderEntity.paytype_id == ERechargePaymentType.hmpay) {
            /**
             * TODO 接入衫德退款
             */
            HmPaymentSDK hmPaymentSDK = new HmPaymentSDK();
            r = hmPaymentSDK.refund(orderEntity.ordersn, orderEntity.pay_ordersn, refund_amount, refund_reason);
//            if (r.code != 0) {
//                return new SyncResult(1, r.msg);
//            }
            if (r.code != 0) {
                return r;
            }
            JSONObject jsonObject = (JSONObject) r.data;
            refundOrderEntity.refund_bank_order_no = jsonObject.getString("refund_bank_order_no");
            refundOrderEntity.refund_bank_trx_no = jsonObject.getString("refund_bank_trx_no");
        }

        if (orderEntity.paytype_id == ERechargePaymentType.alipay) {
            /**
             * 接入Alipay退款
             */
            AliPaymentSDK aliPaymentSDK = new AliPaymentSDK();
            refundOrderEntity.refund_OrderSN = RechargeOrderEntity.getInstance().createOrderSn("refund");

            r = aliPaymentSDK.refund(orderEntity.ordersn, refundOrderEntity.refund_OrderSN, refund_amount, refund_reason);
//            if (r.code != 0) {
//                return new SyncResult(1, r.msg);
//            }
            if (r.code != 0) {
                return r;
            }
            refundOrderEntity.refund_bank_order_no = "";
            refundOrderEntity.refund_bank_trx_no = "";
        }

        if (orderEntity.paytype_id == ERechargePaymentType.wechat) {
            /**
             * 接入微信支付退款
             */
            WechatPaySDK wechatPaySDK = new WechatPaySDK();
            refundOrderEntity.refund_OrderSN = RechargeOrderEntity.getInstance().createOrderSn("refund");

            r = wechatPaySDK.refund(orderEntity.ordersn, refundOrderEntity.refund_OrderSN, refund_amount, refund_reason);

            if (r.code != 0) {
                return r;
            }
            JSONObject jsonObject = (JSONObject) r.data;
            refundOrderEntity.refund_bank_order_no = jsonObject.getString("refund_bank_order_no");
            refundOrderEntity.refund_bank_trx_no = jsonObject.getString("refund_bank_trx_no");
        }


        //开启事务执行退款
        double finalRefund_amount = refund_amount;
        return beginTransaction(connection -> {
            int noquery = refundOrderEntity.insertTransaction(connection);
            if (noquery == 0) return new SyncResult(8, "退款失败，新增退款订单失败");

            return UserSummaryEntity.getInstance().updateBalanceTransaction(connection,
                    orderEntity.uid
                    , finalRefund_amount
                    , "refund"
                    , "充值退款"
                    , orderEntity.ordersn);
        });
    }

    /**
     * 开启自我修复任务
     */
    public void repairTask() {
        ThreadUtil.getInstance().execute("充值退款订单 数据修复", () -> {
            long id = 0;
            do {
                Map<String, Object> data = RechargeOrderEntity.getInstance()
                        .field("id,ordersn")
                        .where("id", ">", id)
                        .whereIn("status", "3,4")
                        .find();
                if (data == null || data.size() == 0) {
                    id = -1;
                    break;
                }
                id = MapUtil.getLong(data, "id", -1);
                String OrderSN = MapUtil.getString(data, "ordersn");
                repair(OrderSN);
            } while (id != -1);
            LogsUtil.info("充值退款订单 数据修复", "任务已结束");
        });
    }

    /**
     * 修复订单
     */
    public SyncResult repair(String OrderSN) {
        RechargeOrderEntity orderEntity = RechargeOrderEntity.getInstance()
                .where("ordersn", OrderSN)
                .findModel();
        if (orderEntity == null || orderEntity.id == 0) {
            LogsUtil.info("充值退款订单 数据修复", "[%s] 查无订单数据,无法进行修复", OrderSN);
            return new SyncResult(2, "无订单数据");
        }

        //状态;1=未支付 2=已完成 -1=已取消，3=全额退款，4=部分退款
        switch (orderEntity.status) {
            case -1:
            case 1:
            case 2:
                LogsUtil.info("充值退款订单 数据修复", "[%s] 订单无退款操作，无需修复", OrderSN);
                return new SyncResult(3, "无退款操作");
            case 3: //全额退款
            case 4: //部分退款
                break;
        }

        String refun_OrderSN = "";
        String refund_bank_order_no = "";
        String refund_bank_trx_no = "";

        double refund_amount = orderEntity.refund_amount > 0 ? orderEntity.refund_amount : orderEntity.total_price;
        refund_amount = -Math.abs(refund_amount);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("uid", orderEntity.uid);
        data.put("refund_OrderSN", refun_OrderSN);
        data.put("OrderSN", orderEntity.ordersn);
        data.put("paytype_id", orderEntity.paytype_id);
        data.put("refund_status", ERefundStatus.Completed.index);
        data.put("refund_amount", refund_amount);
        data.put("refund_bank_order_no", refund_bank_order_no);
        data.put("refund_bank_trx_no", refund_bank_trx_no);
        data.put("isTest", orderEntity.isTest);
        data.put("testId", orderEntity.testId);
        data.put("CSId", orderEntity.CSId);
        data.put("deviceCode", orderEntity.deviceCode);
        data.put("port", orderEntity.port);
        data.put("create_time", orderEntity.update_time);
//        data.put("update_time", TimeUtil.getTimestamp());

        RechargeRefundOrderEntity refundOrderEntity = new RechargeRefundOrderEntity();
        if (refundOrderEntity.where("OrderSN", OrderSN).exist()) {
            //表示已经存在，则更新数据
            refundOrderEntity.where("OrderSN", OrderSN).update(data);
        } else {
            refundOrderEntity.insert(data);
        }

        orderEntity.update(orderEntity.id, new LinkedHashMap<>() {{
            put("refund_status", orderEntity.status == 4 ? 1 : 2);//退款状态; 0=无退款, 1=部分退款, 2=全额退款
        }});

        LogsUtil.info("充值退款订单 数据修复", "[%s] 已修复", OrderSN);
        return new SyncResult(0, "已修复");
    }
}
