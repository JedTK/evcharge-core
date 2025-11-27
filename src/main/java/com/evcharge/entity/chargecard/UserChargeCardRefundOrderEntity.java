package com.evcharge.entity.chargecard;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.recharge.RechargeOrderEntity;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.enumdata.ERechargePaymentType;
import com.evcharge.enumdata.ERefundStatus;
import com.evcharge.libsdk.Hmpay.HmPaymentSDK;
import com.evcharge.libsdk.wechat.WechatPaySDK;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户充电卡退款订单;
 *
 * @author : JED
 * @date : 2023-9-20
 */
public class UserChargeCardRefundOrderEntity extends BaseEntity implements Serializable {
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
     */
    public static UserChargeCardRefundOrderEntity getInstance() {
        return new UserChargeCardRefundOrderEntity();
    }

    /**
     * 退款操作
     *
     * @param OrderSN       充值订单号
     * @param cardNumber    充电卡号码，设置失效
     * @param refund_amount 退款金额，退款金额和充值金额相同为全额退款，退款金额小于充值金额为部分退款，退款金额不能大于充值金额也不能为0
     * @param refund_reason 退款原因
     */
    public SyncResult refund(String OrderSN, String cardNumber, double refund_amount, String refund_reason) {
        if (!StringUtils.hasLength(OrderSN)) return new SyncResult(2, "请输入订单号");
        if (!StringUtils.hasLength(cardNumber)) return new SyncResult(2, "请输入充电卡卡号");
        if (refund_amount == 0) return new SyncResult(2, "请输入退款金额");

        refund_amount = -Math.abs(refund_amount);

        UserChargeCardOrderEntity orderEntity = UserChargeCardOrderEntity.getInstance()
                .where("OrderSN", OrderSN)
                .findModel();
        if (orderEntity == null || orderEntity.id == 0) return new SyncResult(3, "查询订单数据失败");
        //状态，0=等待支付，1=支付成功，2=全额退款，3=部分退款
        switch (orderEntity.status) {
            case 0:
                return new SyncResult(4, "订单还没支付");
            case 2://全额退款
                return new SyncResult(5, "订单已退款");
        }

        //检查之前的退款金额
        double total_refund_amount = UserChargeCardRefundOrderEntity.getInstance()
                .where("OrderSN", orderEntity.OrderSN)
                .where("refund_status", ERefundStatus.Completed.index)
                .sum("refund_amount");
        if (Math.abs(refund_amount + total_refund_amount) > orderEntity.payAmount) {
            return new SyncResult(8, "退款金额不能大于原支付金额");
        }

        String organize_code = orderEntity.organize_code;
        if (!StringUtil.hasLength(organize_code)) {
            organize_code = SysGlobalConfigEntity.getString("System:Organize:Code");
        }

        UserChargeCardRefundOrderEntity refundOrderEntity = UserChargeCardRefundOrderEntity.getInstance();
        refundOrderEntity.uid = orderEntity.uid;
        refundOrderEntity.OrderSN = orderEntity.OrderSN;
        refundOrderEntity.paytype_id = orderEntity.payTypeId;
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

        UserChargeCardEntity chargeCardEntity = UserChargeCardEntity.getInstance()
                .where("cardNumber", cardNumber)
                .findModel();
        if (chargeCardEntity == null || chargeCardEntity.id == 0) {
            return new SyncResult(7, "不存在此充电卡，请确保充电卡卡号是否输入错误");
        }

        if (orderEntity.uid != chargeCardEntity.uid) return new SyncResult(10, "订单号和充电卡用户不匹配");

        /**
         * 2024-06-20 修改退款功能
         */
        if (orderEntity.payTypeId == ERechargePaymentType.hmpay) {
            /**
             * TODO 接入衫德退款
             */
            HmPaymentSDK hmPaymentSDK = new HmPaymentSDK();
            SyncResult r = hmPaymentSDK.refund(orderEntity.OrderSN, orderEntity.payOrderSN, refund_amount, refund_reason);
            if (r.code != 0) {
                return new SyncResult(1, r.msg);
            }
            JSONObject jsonObject = (JSONObject) r.data;
            refundOrderEntity.refund_OrderSN = "";
            refundOrderEntity.refund_bank_order_no = jsonObject.getString("refund_bank_order_no");
            refundOrderEntity.refund_bank_trx_no = jsonObject.getString("refund_bank_trx_no");
        }


        if (orderEntity.payTypeId == ERechargePaymentType.wechat) {
            WechatPaySDK wechatPaySDK = new WechatPaySDK();
            refundOrderEntity.refund_OrderSN = RechargeOrderEntity.getInstance().createOrderSn("refund");

            SyncResult r = wechatPaySDK.refund(orderEntity.OrderSN, refundOrderEntity.refund_OrderSN, refund_amount, refund_reason);

            if (r.code != 0) {
                return r;
            }
            JSONObject jsonObject = (JSONObject) r.data;
            refundOrderEntity.refund_bank_order_no = jsonObject.getString("refund_bank_order_no");
            refundOrderEntity.refund_bank_trx_no = jsonObject.getString("refund_bank_trx_no");

        }


        //开启事务执行退款
        return beginTransaction(connection -> {
            int noquery = refundOrderEntity.insertTransaction(connection);
            if (noquery == 0) return new SyncResult(8, "退款失败，新增退款订单失败");

            noquery = chargeCardEntity.where("cardNumber", cardNumber).updateTransaction(connection, new LinkedHashMap<>() {{
                put("end_time", chargeCardEntity.start_time);
            }});
            DataService.getMainCache().del(String.format("User:%s:ChargeCard:Valid", orderEntity.uid));
            if (noquery == 0) return new SyncResult(9, "更新充电卡失败");
            return new SyncResult(0, "");
        });
    }

    /**
     * 开启自我修复任务
     */
    public void repairTask() {
        ThreadUtil.getInstance().execute("充电卡退款订单 数据修复", () -> {
            long id = 0;
            do {
                Map<String, Object> data = UserChargeCardOrderEntity.getInstance()
                        .field("id,OrderSN")
                        .where("id", ">", id)
                        .whereIn("status", "2,3")
                        .find();
                if (data == null || data.size() == 0) {
                    id = -1;
                    break;
                }
                id = MapUtil.getLong(data, "id", -1);
                String OrderSN = MapUtil.getString(data, "OrderSN");
                repair(OrderSN);
            } while (id != -1);
            LogsUtil.info("充电卡退款订单 数据修复", "任务已结束");
        });
    }

    /**
     * 修复订单
     */
    public SyncResult repair(String OrderSN) {
        UserChargeCardOrderEntity orderEntity = UserChargeCardOrderEntity.getInstance()
                .where("OrderSN", OrderSN)
                .findEntity();
        if (orderEntity == null || orderEntity.id == 0) {
            LogsUtil.info("充电卡退款订单 数据修复", "[%s] 查无订单数据,无法进行修复", OrderSN);
            return new SyncResult(2, "无订单数据");
        }

        //状态，0=等待支付，1=支付成功，2=全额退款，3=部分退款
        switch (orderEntity.status) {
            case 0:
            case 1:
                LogsUtil.info("充电卡退款订单 数据修复", "[%s] 订单无退款操作，无需修复", OrderSN);
                return new SyncResult(3, "无退款操作");
            case 2: //全额退款
            case 3: //部分退款
                break;
        }

        String refund_OrderSN = "";
        String refund_bank_order_no = "";
        String refund_bank_trx_no = "";

        double refund_amount = orderEntity.refund_amount > 0 ? orderEntity.refund_amount : orderEntity.totalAmount;
        refund_amount = -Math.abs(refund_amount);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("uid", orderEntity.uid);
        data.put("refund_OrderSN", refund_OrderSN);
        data.put("OrderSN", orderEntity.OrderSN);
        data.put("paytype_id", orderEntity.payTypeId);
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

        UserChargeCardRefundOrderEntity refunOrderEntity = new UserChargeCardRefundOrderEntity();
        if (refunOrderEntity.where("OrderSN", OrderSN).exist()) {
            //表示已经存在，则更新数据
            refunOrderEntity.where("OrderSN", OrderSN).update(data);
        } else {
            refunOrderEntity.insert(data);
        }

        orderEntity.update(orderEntity.id, new LinkedHashMap<>() {{
            put("refund_status", orderEntity.status == 3 ? 1 : 2);//退款状态; 0=无退款, 1=部分退款, 2=全额退款
        }});

        LogsUtil.info("充电卡退款订单 数据修复", "[%s] 已修复", OrderSN);
        return new SyncResult(0, "已修复");
    }
}