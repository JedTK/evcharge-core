package com.evcharge.service.Active;

import com.abc.pay.client.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.active.abcbank.ABCBankActiveConfigEntity;
import com.evcharge.entity.active.abcbank.ABCBankActiveOrderEntity;
import com.evcharge.entity.user.coupon.UserThirdPartyCouponEntity;
import com.evcharge.libsdk.abcpay.ABCPaySDK;
import com.evcharge.libsdk.abcpay.KeyLoaderUtil;
import com.evcharge.libsdk.abcpay.SHA1withRSASigner;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;

import java.math.BigDecimal;
import java.security.PrivateKey;
import java.util.LinkedHashMap;
import java.util.Map;

import com.abc.pay.client.ebus.common.EBusMerchantCommonRequest;

public class ABCBankActiveService {
    public static ABCBankActiveService getInstance() {
        return new ABCBankActiveService();
    }


    /**
     * 创建订单号
     *
     * @return String
     */
    public String createOrderSn(long configId) {
        String orderSN;
        orderSN = String.format("%s%s%s", "ABCBANK", TimeUtil.toTimeString(TimeUtil.getTimestamp(), "yyyyMMddHHmmss")
                , common.randomInt(1000, 9999));

        if (configId == 1) {
            orderSN = String.format("%s01%s%s", "ABC", TimeUtil.toTimeString(TimeUtil.getTimestamp(), "yyyyMMddHHmmss")
                    , common.randomInt(1000, 9999));
            orderSN = orderSN.toUpperCase();
        }

        if (configId == 2) {
            orderSN = String.format("%s02%s%s", "ABC", TimeUtil.toTimeString(TimeUtil.getTimestamp(), "yyyyMMddHHmmss")
                    , common.randomInt(1000, 9999));
            orderSN = orderSN.toUpperCase();
        }
        return orderSN;
    }
    /**
     * 创建订单号
     *
     * @return String
     */
    public String createRefundOrderSn() {
        String orderSN;

        orderSN = String.format("%s%s%s", "ABCBANKReFund", TimeUtil.toTimeString(TimeUtil.getTimestamp(), "yyyyMMddHHmmss")
                , common.randomInt(1000, 9999));
        orderSN = orderSN.toUpperCase();
        return orderSN;
    }

    public JSONObject createOrder(String orderSn, String productName, BigDecimal price, String callBackUrl) {
        String notifyUrl = String.format("%s%s", ConfigManager.getString("sand.hm_notify_url"), callBackUrl);

        EBusMerchantCommonRequest eBusMerchantCommonRequest = new EBusMerchantCommonRequest();
        //必填项
        eBusMerchantCommonRequest.dicRequest.put("PaymentType", "A");
        eBusMerchantCommonRequest.dicRequest.put("PaymentLinkType", "1");
        eBusMerchantCommonRequest.dicRequest.put("ReceiveAccount", "");
        eBusMerchantCommonRequest.dicRequest.put("ReceiveAccName", "");
        eBusMerchantCommonRequest.dicRequest.put("NotifyType", "1");
        eBusMerchantCommonRequest.dicRequest.put("ResultNotifyURL", notifyUrl);
        eBusMerchantCommonRequest.dicRequest.put("TrxType", "PayReq");
        JSONArray orderItems = new JSONArray();

        orderItems.add(new JSONObject() {{
            put("ProductName", productName);
        }});
        ;
        JSONObject order = new JSONObject();
        order.put("PayTypeID", "ImmediatePay");
        order.put("OrderDate", TimeUtil.toTimeString("YYYY/MM/dd"));
        order.put("OrderTime", TimeUtil.toTimeString("HH:mm:ss"));
        order.put("OrderNo", orderSn);
        order.put("CurrencyCode", "156");
        order.put("OrderAmount", String.valueOf(price));
        order.put("CommodityType", "0202");
        order.put("BuyIP", HttpRequestUtil.getIP());
        order.put("OrderItems", orderItems.toString());
        //yyyyMMddHHmmss

        eBusMerchantCommonRequest.dicRequest.put("Order", order);
        JSON responseJson = eBusMerchantCommonRequest.postRequest();
        String jsonString = responseJson.getIJsonString();
        System.out.println(jsonString);
        JSONObject content = JSONObject.parseObject(jsonString);
        if (content == null) return common.apicb(1, "解析JSON失败，content内容为" + jsonString);
        String tokenId = ABCPaySDK.getTokenID(content);
        if (tokenId == null) return common.apicb(1, "获取tokenid失败");
        String jumpUrl = ABCPaySDK.createJumpUrl(tokenId);
        System.out.println("jumpUrl=" + jumpUrl);
        if (jumpUrl == null) return common.apicb(1, "获取跳转链接失败");
        Map<String, Object> cbData = new LinkedHashMap<>();
        cbData.put("jump_url", jumpUrl);
        cbData.put("ordersn", orderSn);

        return common.apicb(0, "success", cbData);
    }


    public SyncResult refund(String orderSn, String refundOrderSn, BigDecimal refundAmount) {
        EBusMerchantCommonRequest eBusMerchantCommonRequest = new EBusMerchantCommonRequest();
        eBusMerchantCommonRequest.dicRequest.put("TrxType", "Refund"); //交易码
        eBusMerchantCommonRequest.dicRequest.put("OrderDate", TimeUtil.toTimeString("YYYY/MM/dd")); //退款日期
        eBusMerchantCommonRequest.dicRequest.put("OrderTime", TimeUtil.toTimeString("HH:mm:ss")); //退款时间
        eBusMerchantCommonRequest.dicRequest.put("OrderNo", orderSn); //支付原订单号
        eBusMerchantCommonRequest.dicRequest.put("NewOrderNo", refundOrderSn); //退款订单号，商户上送
        eBusMerchantCommonRequest.dicRequest.put("CurrencyCode", "156"); //交易币种，上送156
        eBusMerchantCommonRequest.dicRequest.put("TrxAmount", String.valueOf(refundAmount)); //退款金额，最多精确小数点后两位

        JSON responseJson = eBusMerchantCommonRequest.postRequest();
        String jsonString = responseJson.getIJsonString();
        System.out.println(jsonString);

        JSONObject content = JSONObject.parseObject(jsonString);
        if (content == null) return new SyncResult(1, "解析JSON失败，content内容为" + jsonString);

        JSONObject message = content.getJSONObject("MSG").getJSONObject("Message").getJSONObject("TrxResponse");
        if (message == null) return new SyncResult(1, "解析JSON失败，message为null");

        if(!message.getString("ReturnCode").equals("0000")){
            return new SyncResult(1,message.getString("ErrorMessage"));
        }

        return new SyncResult(0, "success", message);
    }


    public void callback(ABCBankActiveOrderEntity abcBankActiveOrderEntity, JSONObject trxResponse, JSONObject jsonObj) {
        ABCBankActiveConfigEntity abcBankActiveConfigEntity = ABCBankActiveConfigEntity.getInstance()
                .where("id", abcBankActiveOrderEntity.config_id)
                .findEntity();
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("callback_content", jsonObj.toString().replaceAll("[^\u0000-\uFFFF]", "?"));
        data.put("callback_time", TimeUtil.getTimestamp());
        data.put("status", 2);
        data.put("pay_price", trxResponse.getBigDecimal("Amount"));
        data.put("update_time", TimeUtil.getTimestamp());

        ABCBankActiveOrderEntity.getInstance().where("id", abcBankActiveOrderEntity.id).update(data);

        UserThirdPartyCouponEntity.getInstance().sendABCBankCoupon(abcBankActiveOrderEntity.uid, abcBankActiveOrderEntity.ordersn, abcBankActiveConfigEntity.coupon_code);

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("receive_count", abcBankActiveConfigEntity.receive_count + 1);
        config.put("update_time", TimeUtil.getTimestamp());
        ABCBankActiveConfigEntity.getInstance().where("id", abcBankActiveConfigEntity.id).update(config);
        //计算派券数量
//        int count=couponList.size();
//        ABCBankActiveConfigEntity.getInstance().addReviceCouont(config,count);

    }


    public void cancel(ABCBankActiveOrderEntity abcBankActiveOrderEntity, JSONObject jsonObj) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("callback_content", jsonObj.toString().replaceAll("[^\u0000-\uFFFF]", "?"));
        data.put("callback_time", TimeUtil.getTimestamp());
        data.put("status", -1);
        data.put("update_time", TimeUtil.getTimestamp());
        ABCBankActiveOrderEntity.getInstance().where("id", abcBankActiveOrderEntity.id).update(data);
    }


}
