package com.evcharge.entity.recharge;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.active.ActivityMovieTicketsEntity;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.entity.user.UserIntegralDetailEntity;
import com.evcharge.enumdata.EBalanceUpdateType;
import com.evcharge.entity.sys.OrderTypeConfig;
import com.evcharge.entity.user.UserConsumeLogEntity;
import com.evcharge.entity.user.UserSummaryEntity;
import com.evcharge.enumdata.ERechargePaymentType;
import com.evcharge.enumdata.EUserIntegralType;
import com.evcharge.libsdk.Hmpay.HmPaymentSDK;
import com.xyzs.entity.BaseEntity;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 充值订单表;
 *
 * @author : Jay
 * @date : 2022-9-26
 */
public class RechargeOrderEntity extends BaseEntity implements Serializable {
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
     * 消耗积分
     */
    public int use_integral;
    /**
     * 支付类型id
     */
    public int paytype_id;
    /**
     * 单价
     */
    public double price;
    /**
     * 数量
     */
    public int amount;
    /**
     * 充值订单配置id
     */
    public long config_id;
    /**
     * 订单编号
     */
    public String ordersn;
    /**
     * 状态;1=未支付 2=已完成 -1=已取消，3=全额退款，4=部分退款
     */
    public int status;
    /**
     * 支付订单号
     */
    public String pay_ordersn;
    /**
     * 商户可结算金额，订单金额-商户手续费
     */
    public BigDecimal settle_amount;
    /**
     * 支付金额
     */
    public double pay_price;
    /**
     * 支付时间
     */
    public long pay_time;
    /**
     * 总价
     */
    public double total_price;
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
    /**
     * 退款状态; 0=无退款, 1=部分退款, 2=全额退款
     */
    public int refund_status;

    //endregion

    /**
     * 获得一个实例
     *
     * @return
     */
    public static RechargeOrderEntity getInstance() {
        return new RechargeOrderEntity();
    }

    /**
     * 创建订单号
     *
     * @param type
     * @return
     */
    public String createOrderSn(String type) {
        String OrderSN;
        switch (type) {
            case "charge":
                OrderSN = String.format("RE%s%s", TimeUtil.toTimeString(TimeUtil.getTimestamp(), "yyyyMMddHHmmss")
                        , common.randomStr(4));
                break;
            case "refund":
                OrderSN = String.format("FU%s%s", TimeUtil.toTimeString(TimeUtil.getTimestamp(), "yyyyMMddHHmmss")
                        , common.randomStr(4));
                break;
            default:
                OrderSN = String.format("OR%s%sSN", TimeUtil.toTimeString(TimeUtil.getTimestamp(), "yyyyMMddHHmmss")
                        , common.randomStr(4));
                break;
        }
        OrderSN = OrderSN.toUpperCase();
        return OrderSN;
    }

    /*
     * 根据订单编号获取订单信息
     *
     * @param orderSn
     * @return
     */
//    public JSONObject getOrderInfoByOrderSn(String orderSn) {
//
////        Map<String, Object> orderInfo = DataService.getMainCache().getJSONObject(String.format("RechargeOrder:%s", orderSn));
//        Map<String, Object> orderInfo = new LinkedHashMap<>();
////        if (orderInfo != null) {
////            return MapUtil.toJSONObject(orderInfo);
////        }
//        orderInfo = this.where("ordersn", orderSn).find();
//        //String s = DataService.getMainDB().theLastSql();
//        if (orderInfo == null) {
//            return new JSONObject();
//        }
//        DataService.getMainCache().setJSONObject(String.format("RechargeOrder:%s", orderSn), MapUtil.toJSONObject(orderInfo));
//        return JSONObject.parseObject(orderInfo.toString());
//    }

    /**
     * 根据订单编号获取订单信息
     *
     * @param orderSn
     * @return
     */
    public RechargeOrderEntity getOrderInfoByOrderSn(String orderSn) {

//        Map<String, Object> orderInfo = DataService.getMainCache().getJSONObject(String.format("RechargeOrder:%s", orderSn));
        Map<String, Object> orderInfo = new LinkedHashMap<>();
//        if (orderInfo != null) {
//            return MapUtil.toJSONObject(orderInfo);
//        }
        return this.cache(String.format("RechargeOrder:%s", orderSn))
                .where("ordersn", orderSn).findEntity();
        //String s = DataService.getMainDB().theLastSql();
//        if (orderInfo == null) {
//            return new JSONObject();
//        }
//        DataService.getMainCache().setJSONObject(String.format("RechargeOrder:%s", orderSn), MapUtil.toJSONObject(orderInfo));
//        return JSONObject.parseObject(orderInfo.toString());
    }


    /**
     * 检查订单支付状态
     * @param sandData
     * @return
     */
//    public static boolean checkOrderPaySuccess(JSONObject sandData){
//
//        if (!("SUCCESS").equals(sandData.getString("sub_code"))) {
//            LogsUtil.info("", "回调信息 状态为%s，订单编号=%s", sandData.getString("sub_code"), orderSn);
//            return false;
//        }
//
//        return true;
//    }


    /**
     * 衫德回调
     *
     * @param orderSn
     * @param sandData
     */
    public static String sandCallBack(String orderSn, JSONObject sandData) {
        //获取订单信息
//        JSONObject orderInfo = RechargeOrderEntity.getInstance().getOrderInfoByOrderSn(orderSn);
        RechargeOrderEntity orderInfo = RechargeOrderEntity.getInstance().getOrderInfoByOrderSn(orderSn);
        //获取用户id
        long userId = orderInfo.uid;
        //获取配置数据
        Map<String, Object> configInfo = RechargeConfigEntity.getInstance()
                .getInfo(orderInfo.config_id);

        //获取余额
        double balance = Double.parseDouble(configInfo.get("balance").toString());

        if (orderInfo == null) {
            LogsUtil.info("", "充值订单，订单信息不存在，订单编号=%s", orderSn);
            return "respCode=000001";
        }
        if (orderInfo.status == 2) {
            LogsUtil.info("", "订单状态不正确，当前订单状态为已支付，订单编号=%s", orderSn);
            return "respCode=000001";
        }
        if (orderInfo.status == -1) {
            LogsUtil.info("", "订单状态不正确，当前订单状态为已取消，订单编号=%s", orderSn);
            return "respCode=000001";
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", 2); //支付成功
        data.put("pay_ordersn", sandData.get("payOrderCode")); //更新订单
        data.put("update_time", TimeUtil.getTimestamp());
        data.put("pay_time", TimeUtil.getTimestamp());
        Double payPrice = sandData.getDouble("buyerPayAmount") / 100;
        data.put("pay_price", payPrice);
        //更新用户余额
        DataService.getMainDB().name("RechargeOrder")
                .where("ordersn", orderSn)
                .update(data);

        //写入消费表
        Map<String, Object> consumeData = new LinkedHashMap<>();

        consumeData.put("uid", orderInfo.uid);
        consumeData.put("paytype_id", 1);
        consumeData.put("pay_order_code", sandData.get("payOrderCode"));
        consumeData.put("pay_time", TimeUtil.getTimestamp());
        consumeData.put("pay_price", payPrice);
        consumeData.put("order_type", OrderTypeConfig.recharge);
        consumeData.put("create_time", TimeUtil.getTimestamp());
        consumeData.put("bank_serial", sandData.get("bankserial"));
        consumeData.put("ordersn", orderSn);
        consumeData.put("content", sandData.toJSONString());
        consumeData.put("notify_url", "");
        consumeData.put("ip", HttpRequestUtil.getIP());

        UserConsumeLogEntity.getInstance().insert(consumeData);

        UserSummaryEntity.getInstance().updateBalance(userId, balance, EBalanceUpdateType.recharge, "余额充值");

        return "respCode=000000";
    }

    /**
     * 河马支付回调
     *
     * @param orderSn
     * @param sandData
     * @return
     */
    public String HmCallback(String orderSn, JSONObject sandData) {

        //获取订单信息
        RechargeOrderEntity orderInfo = RechargeOrderEntity
                .getInstance()
                .getOrderInfoByOrderSn(orderSn);

        //获取配置数据
        Map<String, Object> configInfo = RechargeConfigEntity.getInstance()
                .getInfo(orderInfo.config_id);

        //获取余额
        double balance = Double.parseDouble(configInfo.get("balance").toString());
        System.out.printf("ordersn=%s,recharge_balance=%f%n", orderSn, balance);
        return HmCallback(orderSn,0, sandData);
    }

    /**
     * 河马支付回调
     * @param orderSn 订单编号
     * @param isMovieCallBack 是否为电影券活动回调
     * @param sandData
     * @return
     */
    public String HmCallback(String orderSn, int isMovieCallBack, JSONObject sandData) {
        //        {
//               "bank_order_no": "HMP1809266450549890110215171", //银行订单号，平台送给渠道的商户订单号
//                "bank_trx_no": "4200000186201809268150013529", //银行流水号，渠道返回给平台的流水号，订单成功情况下存在
//                "buyer_id": "oUpF8uMv_S50DhHpWF8yaUSteOVo", //买家ID，付款方唯一标识
//                "is_refund": "true", //是否有退款 true/false
//                "out_order_no": "1537930941182", //商户订单号，商户下当天唯一
//                "plat_trx_no": "6661809266450549890101826563", //平台交易流水号，平台唯一，在订单非异常情况下必填
//                "refund_success_amount": 0.01, //成功退款金额，单位元
//                "sub_code": "SUCCESS",
//                "sub_msg": "交易成功",
//                "success_time": "20180808111211", //支付成功时间，格式yyyyMMddHHmmss
//                "buyer_pay_amount": "0.01" //买家付款金额，买家实际付款的金额，订单金额-优惠金额
//        }
        //获取订单信息
        RechargeOrderEntity orderInfo = RechargeOrderEntity
                .getInstance()
                .getOrderInfoByOrderSn(orderSn);

        if (orderInfo == null) {
            LogsUtil.info("", "充值订单，订单信息不存在，订单编号=%s", orderSn);
            return "error";
        }
        //获取充值配置
        RechargeConfigEntity rechargeConfigEntity = RechargeConfigEntity.getInstance().getInfoById(orderInfo.config_id);
        double balance = 0;
        //获取用户id
        long userId = orderInfo.uid;
        //获取余额
        //double balance = Double.parseDouble(configInfo.get("balance").toString());
        if (orderInfo.status == 2) {
            LogsUtil.info("", "订单状态不正确，当前订单状态为已支付，订单编号=%s", orderSn);
            return "error";
        }
        if (orderInfo.status == -1) {
            LogsUtil.info("", "订单状态不正确，当前订单状态为已取消，订单编号=%s", orderSn);
            return "error";
        }

        if (!("SUCCESS").equals(sandData.getString("sub_code"))) {
            LogsUtil.info("", "回调信息 状态为%s，订单编号=%s", sandData.getString("sub_code"), orderSn);
            return "error";
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", 2); //支付成功
        data.put("pay_ordersn", sandData.get("bank_order_no")); //更新订单
        data.put("update_time", TimeUtil.getTimestamp());
        data.put("pay_time", TimeUtil.getTimestamp());
        data.put("pay_price", sandData.getDouble("total_amount"));
        data.put("settle_amount", sandData.getBigDecimal("settle_amount"));
        //更新用户余额
        DataService.getMainDB().name("RechargeOrder")
                .where("ordersn", orderSn)
                .update(data);

        //写入消费表
        Map<String, Object> consumeData = new LinkedHashMap<>();

        consumeData.put("uid", orderInfo.uid);
        consumeData.put("paytype_id", ERechargePaymentType.hmpay);
        consumeData.put("pay_order_code", sandData.get("bank_order_no"));
        consumeData.put("pay_time", TimeUtil.getTimestamp());
        consumeData.put("pay_price", sandData.getDouble("total_amount"));
        consumeData.put("order_type", OrderTypeConfig.recharge);
        consumeData.put("create_time", TimeUtil.getTimestamp());
        consumeData.put("bank_serial", sandData.get("bank_trx_no"));
        consumeData.put("ordersn", orderSn);
        consumeData.put("content", sandData.toJSONString());
        consumeData.put("notify_url", "/recharge/hm/callback");
        consumeData.put("ip", HttpRequestUtil.getIP());
        consumeData.put("status", 1);

        UserConsumeLogEntity.getInstance().insert(consumeData);
        //如果有消耗积分
        if (orderInfo.use_integral > 0) {
            UserIntegralDetailEntity.getInstance().decrIntegral(orderInfo.uid
                    , -orderInfo.use_integral
                    , EUserIntegralType.Recharge_Deduct
                    , orderInfo.id
                    , "充值抵扣积分"
            );
        }
        //如果电影票活动有开启
        if (SysGlobalConfigEntity.getInt("Activity:MovieTickets:Recharge:Enable") == 1) {

            if(isMovieCallBack==1){
                ActivityMovieTicketsEntity.getInstance().sendCdKey(orderSn);
            }

            if(SysGlobalConfigEntity.getInt("Activity:MovieTickets:Random:Enable")==1){
                String str = ActivityMovieTicketsEntity.getInstance().randomCallback(orderSn);
            }
        }

        //UserSummaryEntity.getInstance().updateBalance(userId, balance, EBalanceUpdateType.recharge, "余额充值", orderSn);
        SyncResult r = DataService.getMainDB().beginTransaction(connection -> {
            UserSummaryEntity.getInstance().updateBalanceTransaction(connection, userId, rechargeConfigEntity.balance, EBalanceUpdateType.recharge, "余额充值", orderSn);
            //如果有充值奖励
            if (rechargeConfigEntity.reward_balance > 0) {
                UserSummaryEntity.getInstance().updateBalanceTransaction(connection, userId, rechargeConfigEntity.reward_balance, rechargeConfigEntity.reward_sub_title, rechargeConfigEntity.reward_title, orderSn);
            }
            return new SyncResult(0, "success");
        });
        if (r.code > 0) {
            return "error";
        }
        DataService.getMainCache().del(String.format("RechargeOrder:%s", orderSn));
        return "SUCCESS";
    }

    /**
     * 微信支付回调
     *
     * @param orderSn
     * @param wxData
     * @return
     */
    public String wechatCallBack(String orderSn, JSONObject wxData) {
        System.out.println("微信支付回调，解密信息:" + wxData.toString());
        RechargeOrderEntity orderInfo = RechargeOrderEntity
                .getInstance()
                .getOrderInfoByOrderSn(orderSn);

        if (orderInfo == null) {
            LogsUtil.info("", "充值订单，订单信息不存在，订单编号=%s", orderSn);
            return "FAIL";
        }
        RechargeConfigEntity rechargeConfigEntity = RechargeConfigEntity.getInstance().getInfoById(orderInfo.config_id);

        double balance = 0;
        //获取用户id
        long userId = orderInfo.uid;

        if (orderInfo.status == 2) {
            LogsUtil.info("", "订单状态不正确，当前订单状态为已支付，订单编号=%s", orderSn);
            return "FAIL";
        }
        if (orderInfo.status == -1) {
            LogsUtil.info("", "订单状态不正确，当前订单状态为已取消，订单编号=%s", orderSn);
            return "FAIL";
        }
        String tradeState = wxData.getString("trade_state");
        if (!("SUCCESS").equals(tradeState)) {
            LogsUtil.info("", "回调信息 状态为%s，订单编号=%s", wxData.getString("sub_code"), orderSn);
            return "error";
        }

        Map<String, Object> data = new LinkedHashMap<>();
        JSONObject amount = wxData.getJSONObject("amount");
        int totalAmount = amount.getIntValue("total");
        double payPrice = new BigDecimal(totalAmount)
                .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP)
                .doubleValue();
        data.put("status", 2); //支付成功
        data.put("pay_ordersn", wxData.get("transaction_id")); //更新订单
        data.put("update_time", TimeUtil.getTimestamp());
        data.put("pay_time", TimeUtil.getTimestamp());
        data.put("pay_price", payPrice);
//        data.put("settle_amount", wxData.getBigDecimal("settle_amount"));
        data.put("settle_amount", payPrice);

        //更新用户余额
        RechargeOrderEntity.getInstance()
                .where("ordersn", orderSn)
                .update(data);

        //写入消费表
        Map<String, Object> consumeData = new LinkedHashMap<>();

        consumeData.put("uid", orderInfo.uid);
        consumeData.put("paytype_id", ERechargePaymentType.wechat);
        consumeData.put("pay_order_code", wxData.get("transaction_id"));
        consumeData.put("pay_time", TimeUtil.getTimestamp());
        consumeData.put("pay_price", payPrice);
        consumeData.put("order_type", OrderTypeConfig.recharge);
        consumeData.put("create_time", TimeUtil.getTimestamp());
        consumeData.put("bank_serial", "");
        consumeData.put("ordersn", orderSn);
        consumeData.put("content", wxData.toJSONString());
        consumeData.put("notify_url", "/recharge/wechat/callback");
        consumeData.put("ip", HttpRequestUtil.getIP());
        consumeData.put("status", 1);

        UserConsumeLogEntity.getInstance().insert(consumeData);
        //如果有消耗积分
        if (orderInfo.use_integral > 0) {
            UserIntegralDetailEntity.getInstance().decrIntegral(orderInfo.uid
                    , -orderInfo.use_integral
                    , EUserIntegralType.Recharge_Deduct
                    , orderInfo.id
                    , "充值抵扣积分"
            );
        }


        SyncResult r = DataService.getMainDB().beginTransaction(connection -> {
            UserSummaryEntity.getInstance().updateBalanceTransaction(connection, userId, rechargeConfigEntity.balance, EBalanceUpdateType.recharge, "余额充值", orderSn);
            //如果有充值奖励
            if (rechargeConfigEntity.reward_balance > 0) {
                UserSummaryEntity.getInstance().updateBalanceTransaction(connection, userId, rechargeConfigEntity.reward_balance, rechargeConfigEntity.reward_sub_title, rechargeConfigEntity.reward_title, orderSn);
            }
            return new SyncResult(0, "success");
        });
        if (r.code > 0) {
            return "error";
        }
        DataService.getMainCache().del(String.format("RechargeOrder:%s", orderSn));
        return "SUCCESS";
    }

    /**
     * 支付宝支付回调
     *
     * @param orderSn
     * @param alipayData
     */
    public void alipayCallback(String orderSn, JSONObject alipayData) {
        //获取配置数据

        RechargeOrderEntity orderInfo = RechargeOrderEntity
                .getInstance()
                .getOrderInfoByOrderSn(orderSn);
        Map<String, Object> configInfo = RechargeConfigEntity.getInstance()
                .getInfo(orderInfo.config_id);
        double balance = Double.parseDouble(configInfo.get("balance").toString());
        //获取充值配置
        RechargeConfigEntity rechargeConfigEntity = RechargeConfigEntity.getInstance().getInfoById(orderInfo.config_id);

        if (orderInfo == null) {
            LogsUtil.info("", "充值订单，订单信息不存在，订单编号=%s", orderSn);
            System.out.println("error");
            return;
        }
        //获取用户id
        long userId = orderInfo.uid;
        if (orderInfo.status == 2) {
            LogsUtil.info("", "订单状态不正确，当前订单状态为已支付，订单编号=%s", orderSn);
            System.out.println("success");
            return;
        }
        if (orderInfo.status == -1) {
            LogsUtil.info("", "订单状态不正确，当前订单状态为已取消，订单编号=%s", orderSn);
            System.out.println("error");
            return;
        }
        //判断交易状态
        String tradeStatus = alipayData.getString("trade_status");
        if (!"TRADE_SUCCESS".equals(tradeStatus)) {
            LogsUtil.info("", String.format("订单状态不正确，订单交易状态为%s，订单编号=%s", tradeStatus, orderSn));

            System.out.println("error");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", 2); //支付成功
        data.put("pay_ordersn", alipayData.get("trade_no")); //更新订单
        data.put("pay_time", TimeUtil.getTimestamp());
        data.put("pay_price", alipayData.getDouble("total_amount"));
        data.put("settle_amount", alipayData.getBigDecimal("total_amount"));
        data.put("update_time", TimeUtil.getTimestamp());
        //更新用户余额
        DataService.getMainDB().name("RechargeOrder")
                .where("ordersn", orderSn)
                .update(data);

        UserConsumeLogEntity userConsumeLogEntity = new UserConsumeLogEntity();
        userConsumeLogEntity.uid = orderInfo.uid;
        userConsumeLogEntity.paytype_id = String.format("%s", ERechargePaymentType.alipay);
        userConsumeLogEntity.pay_order_code = alipayData.getString("trade_no");
        userConsumeLogEntity.pay_time = TimeUtil.getTimestamp();
        userConsumeLogEntity.order_type = OrderTypeConfig.recharge;
        userConsumeLogEntity.create_time = TimeUtil.getTimestamp();
        userConsumeLogEntity.bank_serial = "";
        userConsumeLogEntity.ordersn = orderSn;
        userConsumeLogEntity.content = alipayData.toString();
        userConsumeLogEntity.notify_url = "/recharge/alipay/callback";
        userConsumeLogEntity.ip = HttpRequestUtil.getIP();
        userConsumeLogEntity.status = 1;
        userConsumeLogEntity.insert();
        //如果有消耗积分
        if (orderInfo.use_integral > 0) {
            UserIntegralDetailEntity.getInstance().decrIntegral(orderInfo.uid
                    , -orderInfo.use_integral
                    , EUserIntegralType.Recharge_Deduct
                    , orderInfo.id
                    , "充值抵扣积分"
            );
        }
//        DataService.getMainCache().del(String.format("RechargeOrder:%s", orderSn));
//        UserSummaryEntity.getInstance().updateBalance(userId, balance, EBalanceUpdateType.recharge, "余额充值", orderSn);

        SyncResult r = DataService.getMainDB().beginTransaction(connection -> {
            UserSummaryEntity.getInstance().updateBalanceTransaction(connection, userId, rechargeConfigEntity.balance, EBalanceUpdateType.recharge, "余额充值", orderSn);
            //如果有充值奖励
            if (rechargeConfigEntity.reward_balance > 0) {
                UserSummaryEntity.getInstance().updateBalanceTransaction(connection, userId, rechargeConfigEntity.reward_balance, rechargeConfigEntity.reward_sub_title, rechargeConfigEntity.reward_title, orderSn);
            }
            return new SyncResult(0, "success");
        });
        if (r.code > 0) {
            System.out.println("error");
        }
        DataService.getMainCache().del(String.format("RechargeOrder:%s", orderSn));

        System.out.println("success");
    }

    /**
     * 取消订单
     *
     * @param orderSn
     * @return
     */
    public SyncResult cancelOrder(String orderSn) {
        return cancelOrder(orderSn, 0);
//        RechargeOrderEntity orderInfo = getOrderInfoByOrderSn(orderSn);
//        long movieId = SysGlobalConfigEntity.getLong("Activity:MovieTickets");
//
//        if (orderInfo == null) {
//            return new SyncResult(2, "订单不存在");
//        }
//
//        if (orderInfo.status == -1) {
//            return new SyncResult(3, "订单已取消");
//        }
//        if (orderInfo.status == -3) {
//            return new SyncResult(3, "订单已取消");
//        }
//        JSONObject data = new JSONObject();
//
//        data.put("status", -1);
//        data.put("update_time", TimeUtil.getTimestamp());
//        int r = this.where("id", orderInfo.id).update(data);
//        if (r != 0) {
//            if(movieId!=0){
//                ActivityMovieTicketsEntity.incrMovieStock();
////                DataService.getMainCache().incr(String.format("Activity:Movie:Tickets:Stock:%s", movieId), 1);
//                //addStockCount();
//            }
//            //删除缓存
//            DataService.getMainCache().del(String.format("RechargeOrder:%s", orderSn));
//            return new SyncResult(0, "订单取消成功");
//        } else {
//            return new SyncResult(1, "订单取消失败");
//        }
    }

    /**
     * 取消订单
     * @param orderSn
     * @param orderType
     * @return
     */
    public SyncResult cancelOrder(String orderSn, int orderType) {
        RechargeOrderEntity orderInfo = getOrderInfoByOrderSn(orderSn);
        long movieId = SysGlobalConfigEntity.getLong("Activity:MovieTickets");

        if (orderInfo == null) {
            return new SyncResult(2, "订单不存在");
        }

        if (orderInfo.status == -1) {
            return new SyncResult(3, "订单已取消");
        }

        if (orderInfo.status == 2) {
            return new SyncResult(3, "订单已完成支付");
        }

        if (orderInfo.status == -3) {
            return new SyncResult(3, "订单已取消");
        }
        JSONObject data = new JSONObject();

        data.put("status", -1);
        data.put("update_time", TimeUtil.getTimestamp());
        int r = this.where("id", orderInfo.id).update(data);
        if (r != 0) {
            if (movieId != 0) {
                if (orderType == 1) {
                    ActivityMovieTicketsEntity.incrMovieStock(movieId);
                }
            }
            //删除缓存
            DataService.getMainCache().del(String.format("RechargeOrder:%s", orderSn));
            return new SyncResult(0, "订单取消成功");
        } else {
            return new SyncResult(1, "订单取消失败");
        }
    }

    /**
     * @param OrderSN       充值订单号
     * @param refund_amount 退款金额，退款金额和充值金额相同为全额退款，退款金额小于充值金额为部分退款，退款金额不能大于充值金额也不能为0
     * @param refund_time   退款时间，0时自动默认当前时间
     * @return
     */
    @Deprecated
    public SyncResult refund(String OrderSN, double refund_amount, long refund_time) {
        return refund(OrderSN, refund_amount, refund_time, "");
    }
    /**
     * 退款操作
     *
     * @param OrderSN       充值订单号
     * @param refund_amount 退款金额，退款金额和充值金额相同为全额退款，退款金额小于充值金额为部分退款，退款金额不能大于充值金额也不能为0
     * @param refund_time   退款时间，0时自动默认当前时间
     * @param reason        退款理由
     * @return
     */
    @Deprecated
    public SyncResult refund(String OrderSN, double refund_amount, long refund_time, String reason) {
        if (!StringUtils.hasLength(OrderSN)) return new SyncResult(2, "请输入订单号");
        if (refund_amount == 0) return new SyncResult(2, "请输入退款金额");
        if (refund_amount < 0) refund_amount = Math.abs(refund_amount);
        if (refund_time == 0) refund_time = TimeUtil.getTimestamp();

        RechargeOrderEntity orderEntity = RechargeOrderEntity.getInstance()
                .where("ordersn", OrderSN)
                .findModel();
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

        Map<String, Object> set_data = new LinkedHashMap<>();
        //处理部分退款可能出现多次操作的情景
        if (orderEntity.status == 4) {
            //已经退款的额度+本次退款的金额 > 支付的金额
            if (orderEntity.refund_amount + refund_amount > orderEntity.pay_price) {
                return new SyncResult(8, "所有退款的金额不能大于充值金额");
            }

            //已经退款的额度+本次退款的金额 = 支付的金额  表示已经全额退款
            if (orderEntity.refund_amount + refund_amount == orderEntity.pay_price) {
                set_data.put("status", 3);//状态;1=未支付 2=已完成 -1=已取消，3=全额退款，4=部分退款
            }
            //已经退款的额度+本次退款的金额 < 支付的金额  表示依然还是部分退款
            else {
                set_data.put("status", 4);//状态;1=未支付 2=已完成 -1=已取消，3=全额退款，4=部分退款
            }
        }
        //正常退款流程
        else {
            if (refund_amount == orderEntity.pay_price) {
                set_data.put("status", 3);//状态;1=未支付 2=已完成 -1=已取消，3=全额退款，4=部分退款
            } else if (refund_amount < orderEntity.pay_price) {
                set_data.put("status", 4);//状态;1=未支付 2=已完成 -1=已取消，3=全额退款，4=部分退款
            } else return new SyncResult(7, "退款金额不能大于充值金额也不能为0");
        }

        //总的退款金额
        set_data.put("refund_amount", orderEntity.refund_amount + refund_amount);
        set_data.put("refund_time", refund_time);
        /**
         * TODO 接入衫德退款
         */
        HmPaymentSDK hmPaymentSDK = new HmPaymentSDK();
        SyncResult r = hmPaymentSDK.refund(orderEntity.ordersn, orderEntity.pay_ordersn, refund_amount, reason);

        if (r.code != 0) {
            return new SyncResult(1, r.msg);
        }
        //开启事务执行退款
        double finalRefund_amount = refund_amount;
        return beginTransaction(connection -> {
            int noquery = orderEntity.where("ordersn", OrderSN).update(set_data);
            if (noquery == 0) return new SyncResult(8, "退款失败，更新订单数据失败");

            return UserSummaryEntity.getInstance().updateBalanceTransaction(connection,
                    orderEntity.uid
                    , -finalRefund_amount
                    , "refund"
                    , "充值退款"
                    , orderEntity.ordersn);
        });
    }
    private static AtomicInteger stock = new AtomicInteger(0);
    public static void initStock() {
        long movieId = SysGlobalConfigEntity.getLong("Activity:MovieTickets");
        stock.set(ActivityMovieTicketsEntity.getInstance().getStock(movieId));
    }
    public void mqSubService() {
//        String configType = ConfigManager.getString("config.type");
//        String groupName = String.format("EvCharge_Movie_%s", configType);
//        long stock = ActivityMovieTicketsEntity.getInstance().getStock(SysGlobalConfigEntity.getLong("Activity:MovieTickets"));
//        RocketMQGlobal.getRocketMQ().subscribe((MessageListenerOrderly) (list, consumeOrderlyContext) -> {
//            MessageExt msg = list.get(0);
////            for (MessageExt msg : list) {
//            try {
////                if(getStockCount().intValue()==-99){
////                    initStock();
////                }
//                System.out.println("stock=" + stock);
//                JSONObject json = JSONObject.parseObject(new String(msg.getBody(), StandardCharsets.UTF_8));
//                RechargeOrderEntity rechargeOrderEntity = json.getObject("RechargeOrder", RechargeOrderEntity.class);
//                if (stock <= 0) {
//                    rechargeOrderEntity.status = -3;
//                    rechargeOrderEntity.update_time = TimeUtil.getTimestamp();
//                    rechargeOrderEntity.insert();
//                    return ConsumeOrderlyStatus.SUCCESS;
//                }
//
//                System.out.println("获取消息队列信息" + json);
//                rechargeOrderEntity.createOrder();
//            } catch (Exception e) {
//                LogsUtil.error(e, "", "RocketMQ消费者处理程序发生错误");
//            }
////            }
//            return ConsumeOrderlyStatus.SUCCESS;
//        }, groupName, String.format("EvCharge_Movie_%s", configType), "sendAsyncWithMQ");


    }
    /**
     * 创建订单
     *
     * @return
     */
    public SyncResult createOrder() {
        this.id = this.insertGetId();
        if (this.id == 0) {
            //是否需要加库存

            return new SyncResult(4001, "更新订单发生错误，请稍后再试");
        }
        ;
//        subStockCount();

        return new SyncResult(0, "success");
    }
    /**
     * 获取在线数量
     *
     * @return
     */
    public static synchronized AtomicInteger getStockCount() {
        return stock;
    }
    /**
     * 新增在线数量
     */
    public static synchronized void addStockCount() {
        stock.getAndIncrement();
    }
    /**
     * 减少在线数量
     */
    public static synchronized void subStockCount() {
        stock.getAndDecrement();
    }


}
