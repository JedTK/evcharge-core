package com.evcharge.entity.chargecard;


import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.active.integral.ActiveIntegralTempV1Entity;
import com.evcharge.entity.sys.OrderTypeConfig;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.entity.user.UserConsumeLogEntity;
import com.evcharge.entity.user.UserEntity;
import com.evcharge.entity.user.UserIntegralDetailEntity;
import com.evcharge.enumdata.ERechargePaymentType;
import com.evcharge.enumdata.EUserIntegralType;
import com.evcharge.libsdk.Hmpay.HmPaymentSDK;
import com.xyzs.database.ISqlDBObject;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;

import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户购买充电卡记录;
 *
 * @author : JED
 * @date : 2022-10-11
 */
public class UserChargeCardOrderEntity extends BaseEntity implements Serializable {
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
     * 订单号
     */
    public String OrderSN;
    /**
     * 状态，0=等待支付，1=支付成功，2=全额退款，3=部分退款
     */
    public int status;
    /**
     * 充电卡商品ID(待删除)
     */
    @Deprecated
    public long cardConfigId;
    /**
     * spu编码
     */
    public String spu_code;
    /**
     * 支付金额
     */
    public double totalAmount;
    /**
     * 支付类型id
     */
    public int payTypeId;
    /**
     * 支付订单号
     */
    public String payOrderSN;
    /**
     * 支付金额
     */
    public double payAmount;
    /**
     * 支付时间
     */
    public long pay_time;
    /**
     * 商户可结算金额，订单金额-商户手续费
     */
    public BigDecimal settle_amount;
    /**
     * 分享码
     */
    public String sharecode;
    /**
     * 创建时间
     */
    public long create_time;
    /**
     * 更新时间
     */
    public long update_time;
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
     * 退款金额
     */
    public double refund_amount;
    /**
     * 退款时间
     */
    public long refund_time;

    //endregion

    /**
     * 获得一个实例
     */
    public static UserChargeCardOrderEntity getInstance() {
        return new UserChargeCardOrderEntity();
    }

    /**
     * 订单号创建
     */
    public String orderSNCreate(String HEAD, String END) {
        return String.format("%s%s%s%s", HEAD, TimeUtil.toTimeString(TimeUtil.getTimestamp(), "yyyyMMddHHmmss"), common.randomInt(100, 999), END);
    }

    /**
     * 创建订单
     *
     * @param uid           用户ID
     * @param OrderSN       订单号
     * @param cardConfigId  充电卡配置ID
     * @param totalAmount   支付金额
     * @param payTypeId     支付类型
     * @param CSId          （可选）充电桩ID
     * @param organize_code (可选)组织代码
     * @param deviceCode    （可选）充电设备
     * @param port          （可选）充电设备端口
     */
    public SyncResult create(long uid
            , String OrderSN
            , long cardConfigId
            , double totalAmount
            , int payTypeId
            , String CSId
            , String organize_code
            , String deviceCode
            , int port
            , String shareCode
    ) {
        this.OrderSN = OrderSN;
        this.uid = uid;
        this.status = 0;//状态，0=等待支付，1=支付成功，2=全额退款，3=部分退款
        this.cardConfigId = cardConfigId;
        this.payTypeId = payTypeId;
        this.totalAmount = totalAmount;
        this.create_time = TimeUtil.getTimestamp();
        this.update_time = TimeUtil.getTimestamp();
        this.CSId = CSId;
        this.organize_code = organize_code;
        this.deviceCode = deviceCode;
        this.port = port;
        this.sharecode = shareCode;

        this.id = this.insertGetId();
        if (this.id > 0) return new SyncResult(0, "", this);
        return new SyncResult(1, "创建订单失败");
    }

    /**
     * 结单
     *
     * @param OrderSN 订单号
     */
    public SyncResult finish(Connection connection, String OrderSN) throws SQLException {
        UserChargeCardOrderEntity logEntity = UserChargeCardOrderEntity.getInstance()
                .where("OrderSN", OrderSN)
                .findModelTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.ShareLock);
        if (logEntity == null || logEntity.id == 0) return new SyncResult(10, "查询不到订单信息");

        //如果状态为支付成功，表示已经发货了
        if (logEntity.status == 1) return new SyncResult(0, "");

        Map<String, Object> set_data = new HashMap<>();
        set_data.put("status", 1);//状态，0=等待支付，1=支付成功
        set_data.put("update_time", TimeUtil.getTimestamp());
        if (this.updateTransaction(connection, logEntity.id, set_data) == 0) return new SyncResult(11, "结单失败");

        return UserChargeCardEntity.getInstance().addTransaction(connection, logEntity.uid, logEntity.cardConfigId);
    }

    /**
     * 河马支付结单
     *
     * @param OrderSN  订单号
     * @param sandData 支付信息
     */
    public SyncResult finish(Connection connection, String OrderSN, JSONObject sandData) {
        try {
            UserChargeCardOrderEntity orderEntity = UserChargeCardOrderEntity.getInstance()
                    .where("OrderSN", OrderSN)
                    .findModelTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.ShareLock);

            if (orderEntity == null || orderEntity.id == 0) return new SyncResult(10, "查询不到订单信息");

            //写入消费表
            Map<String, Object> consumeData = new LinkedHashMap<>();
            consumeData.put("uid", orderEntity.uid);
            consumeData.put("paytype_id", ERechargePaymentType.hmpay);
            consumeData.put("pay_order_code", sandData.get("bank_order_no"));
            consumeData.put("pay_time", TimeUtil.getTimestamp());
            consumeData.put("pay_price", sandData.getDouble("total_amount"));
            consumeData.put("order_type", OrderTypeConfig.chargeCard);
            consumeData.put("create_time", TimeUtil.getTimestamp());
            consumeData.put("bank_serial", sandData.get("bank_trx_no"));
            consumeData.put("ordersn", OrderSN);
            consumeData.put("content", sandData.toJSONString());
            consumeData.put("notify_url", "/ChargeCard/hm/callback");
            consumeData.put("ip", HttpRequestUtil.getIP());
            consumeData.put("status", 1);
            UserConsumeLogEntity.getInstance().insertGetIdTransaction(connection, consumeData);
            //如果状态为支付成功，表示已经发货了
            if (orderEntity.status == 1) return new SyncResult(0, "");

            Map<String, Object> set_data = new HashMap<>();
            set_data.put("status", 1);//状态，0=等待支付，1=支付成功
            set_data.put("payOrderSN", sandData.get("bank_order_no"));
            set_data.put("payAmount", sandData.get("total_amount"));
            set_data.put("settle_amount", sandData.getBigDecimal("settle_amount"));

            set_data.put("pay_time", TimeUtil.getTimestamp());
            set_data.put("update_time", TimeUtil.getTimestamp());
            if (this.updateTransaction(connection, orderEntity.id, set_data) == 0) {
                return new SyncResult(11, "结单失败");
            }
            //处理分享码 如果存在分享码
            System.out.println("sharecode=" + orderEntity.sharecode);
            if (StringUtils.hasLength(orderEntity.sharecode)) {
                UserEntity shareUserEntity = UserEntity.getInstance().findUserByShareCode(orderEntity.sharecode);
                System.out.println("shareUserEntity=" + shareUserEntity);
                if (shareUserEntity != null && shareUserEntity.id != orderEntity.uid) {
                    long integralTempId = SysGlobalConfigEntity.getLong("ChargeCard:Share:IntegralTemp");
                    ActiveIntegralTempV1Entity activeIntegralTempV1Entity = ActiveIntegralTempV1Entity.getInstance().getRuleById(integralTempId);
                    if (activeIntegralTempV1Entity != null) {
                        UserIntegralDetailEntity.getInstance().receiveActiveIntegral(
                                shareUserEntity.id
                                , activeIntegralTempV1Entity.rule_key
                                , EUserIntegralType.ChargeCard_Share
                                , orderEntity.id
                                , "分享购买优惠卡获得积分"
                        );
                    }

                }
            }
            return UserChargeCardEntity.getInstance().purchaseCallback(orderEntity.uid, orderEntity.CSId, orderEntity.cardConfigId, orderEntity.OrderSN, "");
        } catch (Exception e) {
            return new SyncResult(1, "结单失败");
        }
    }


    /**
     * 微信支付回调信息
     */
    public SyncResult wechatFinish(Connection connection, String OrderSN, JSONObject wxData) {
        try {
            UserChargeCardOrderEntity orderEntity = UserChargeCardOrderEntity.getInstance()
                    .where("OrderSN", OrderSN)
                    .findModelTransaction(connection, ISqlDBObject.ROWLOCK_TYPE.ShareLock);

            if (orderEntity == null || orderEntity.id == 0) return new SyncResult(10, "查询不到订单信息");
            //写入消费表
            Map<String, Object> consumeData = new LinkedHashMap<>();
            JSONObject amount = wxData.getJSONObject("amount");
            int totalAmount = amount.getIntValue("total");
            double payPrice = new BigDecimal(totalAmount)
                    .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP)
                    .doubleValue();
            consumeData.put("uid", orderEntity.uid);
            consumeData.put("paytype_id", ERechargePaymentType.wechat);
            consumeData.put("pay_order_code", wxData.get("transaction_id"));
            consumeData.put("pay_time", TimeUtil.getTimestamp());
            consumeData.put("pay_price", payPrice);
            consumeData.put("order_type", OrderTypeConfig.recharge);
            consumeData.put("create_time", TimeUtil.getTimestamp());
            consumeData.put("bank_serial", "");
            consumeData.put("ordersn", orderEntity.OrderSN);
            consumeData.put("content", wxData.toJSONString());
            consumeData.put("notify_url", "/ChargeCard/wechat/callback");
            consumeData.put("ip", HttpRequestUtil.getIP());
            consumeData.put("status", 1);

            //如果状态为支付成功，表示已经发货了
            if (orderEntity.status == 1) return new SyncResult(0, "");

            Map<String, Object> set_data = new HashMap<>();
            set_data.put("status", 1);//状态，0=等待支付，1=支付成功
            set_data.put("payOrderSN", wxData.get("transaction_id"));
            set_data.put("payAmount", payPrice);
            set_data.put("settle_amount", payPrice);

            set_data.put("pay_time", TimeUtil.getTimestamp());
            set_data.put("update_time", TimeUtil.getTimestamp());
            if (this.updateTransaction(connection, orderEntity.id, set_data) == 0) {
                return new SyncResult(11, "结单失败");
            }

            //处理分享码 如果存在分享码
            System.out.println("sharecode=" + orderEntity.sharecode);
            if (StringUtils.hasLength(orderEntity.sharecode)) {
                UserEntity shareUserEntity = UserEntity.getInstance().findUserByShareCode(orderEntity.sharecode);
                System.out.println("shareUserEntity=" + shareUserEntity);
                if (shareUserEntity != null && shareUserEntity.id != orderEntity.uid) {
                    long integralTempId = SysGlobalConfigEntity.getLong("ChargeCard:Share:IntegralTemp");
                    ActiveIntegralTempV1Entity activeIntegralTempV1Entity = ActiveIntegralTempV1Entity.getInstance().getRuleById(integralTempId);
                    if (activeIntegralTempV1Entity != null) {
                        UserIntegralDetailEntity.getInstance().receiveActiveIntegral(
                                shareUserEntity.id
                                , activeIntegralTempV1Entity.rule_key
                                , EUserIntegralType.ChargeCard_Share
                                , orderEntity.id
                                , "分享购买优惠卡获得积分"
                        );
                    }
                }
            }
            return UserChargeCardEntity.getInstance().purchaseCallback(orderEntity.uid, orderEntity.CSId, orderEntity.cardConfigId, orderEntity.OrderSN, "");
        } catch (Exception e) {
            return new SyncResult(1, "结单失败");
        }

    }


    /**
     * 退款操作
     *
     * @param OrderSN       充值订单号
     * @param cardNumber    充电卡号码，设置失效
     * @param refund_amount 退款金额，退款金额和充值金额相同为全额退款，退款金额小于充值金额为部分退款，退款金额不能大于充值金额也不能为0
     * @param refund_time   退款时间，0时自动默认当前时间
     */
    @Deprecated
    public SyncResult refund(String OrderSN, String cardNumber, double refund_amount, long refund_time) {
        if (!StringUtils.hasLength(OrderSN)) return new SyncResult(2, "请输入订单号");
        if (!StringUtils.hasLength(cardNumber)) return new SyncResult(2, "请输入充电卡卡号");
        if (refund_amount == 0) return new SyncResult(2, "请输入退款金额");
        if (refund_amount < 0) refund_amount = Math.abs(refund_amount);
        if (refund_time == 0) refund_time = TimeUtil.getTimestamp();

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

        Map<String, Object> set_data = new LinkedHashMap<>();
        //处理部分退款可能出现多次操作的情景
        if (orderEntity.status == 3) {
            //已经退款的额度+本次退款的金额 > 支付的金额
            if (orderEntity.refund_amount + refund_amount > orderEntity.totalAmount) {
                return new SyncResult(8, "所有退款的金额不能大于充值金额");
            }

            //已经退款的额度+本次退款的金额 = 支付的金额  表示已经全额退款
            if (orderEntity.refund_amount + refund_amount == orderEntity.totalAmount) {
                set_data.put("status", 3);//状态;1=未支付 2=已完成 -1=已取消，3=全额退款，4=部分退款
            }
            //已经退款的额度+本次退款的金额 < 支付的金额  表示依然还是部分退款
            else {
                set_data.put("status", 4);//状态;1=未支付 2=已完成 -1=已取消，3=全额退款，4=部分退款
            }
        }
        //正常退款流程
        else {
            if (refund_amount == orderEntity.totalAmount) {
                set_data.put("status", 2);//状态，0=等待支付，1=支付成功，2=全额退款，3=部分退款
            } else if (refund_amount < orderEntity.totalAmount) {
                set_data.put("status", 3);//状态，0=等待支付，1=支付成功，2=全额退款，3=部分退款
            } else return new SyncResult(6, "退款金额不能大于充值金额也不能为0");
        }

        set_data.put("refund_amount", orderEntity.refund_amount + refund_amount);
        set_data.put("refund_time", refund_time);

        UserChargeCardEntity chargeCardEntity = UserChargeCardEntity.getInstance()
                .where("cardNumber", cardNumber)
                .findModel();
        if (chargeCardEntity == null || chargeCardEntity.id == 0) {
            return new SyncResult(7, "不存在此充电卡，请确保充电卡卡号是否输入错误");
        }

        if (orderEntity.uid != chargeCardEntity.uid) return new SyncResult(10, "订单号和充电卡用户不匹配");
        /**
         * TODO 接入衫德退款
         */
        HmPaymentSDK hmPaymentSDK = new HmPaymentSDK();
        SyncResult r = hmPaymentSDK.refund(orderEntity.OrderSN, orderEntity.payOrderSN, refund_amount, "充电卡退款");
        if (r.code != 0) {
            return new SyncResult(1, r.msg);
        }
        //开启事务执行退款
        return beginTransaction(connection -> {
            int noquery = orderEntity.where("ordersn", OrderSN).update(set_data);
            if (noquery == 0) return new SyncResult(8, "退款失败，更新订单数据失败");

            noquery = chargeCardEntity.where("cardNumber", cardNumber).updateTransaction(connection, new LinkedHashMap<>() {{
                put("end_time", chargeCardEntity.start_time);
            }});
            DataService.getMainCache().del(String.format("User:%s:ChargeCard:Valid", orderEntity.uid));
            if (noquery == 0) return new SyncResult(9, "更新充电卡失败");
            return new SyncResult(0, "");
        });
    }
}
