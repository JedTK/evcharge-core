package com.evcharge.strategy.ConsumeCenter.Payment.PaymentCallback;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;
import com.evcharge.entity.payment.PaymentCallbackLogEntity;
import com.evcharge.entity.user.UserIntegralDetailEntity;
import com.evcharge.enumdata.EUserIntegralType;
import com.evcharge.libsdk.wechat.WechatPaySDK;
import com.evcharge.service.ConsumeCenter.ConsumeOrdersService;
import com.evcharge.strategy.ConsumeCenter.Payment.Fulfillment.FulfillmentService;
import com.evcharge.strategy.ConsumeCenter.Payment.Fulfillment.FulfillmentServiceFactory;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.HttpRequestUtil;
import com.xyzs.utils.JsonUtil;
import com.xyzs.utils.LogsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class WechatPayMPCallbackServiceImpl implements PaymentCallbackService {
    @Autowired
    private FulfillmentServiceFactory fulfillmentServiceFactory;

    @Autowired
    private ConsumeOrdersService consumeOrdersService;

    @Override
    public String getChannelCode() {
        return "wechat_mp"; // 支付宝回调渠道编码
    }

    @Override
    public String process(HttpServletRequest request) {
        String requestBody = "";
        try {
            requestBody = getBody(request);
        } catch (Exception e) {
            LogsUtil.error(this.getClass().getName(), "【微信支付】获取requestBody失败，失败原因=" + e.getMessage());
            return "error";
        }

        JSONObject cb = new JSONObject();
        System.out.println("接收充值微信支付回调：" + requestBody);

        String signature = HttpRequestUtil.getHeader("Wechatpay-Signature");
        String nonce = HttpRequestUtil.getHeader("Wechatpay-Nonce");
        String timestamp = HttpRequestUtil.getHeader("Wechatpay-Timestamp");
        String serial = HttpRequestUtil.getHeader("Wechatpay-Serial");
        String signatureType = HttpRequestUtil.getHeader("Wechatpay-Signature-Type");

        String message = "";
//        message += String.format("&applet_app_id=%s", applet_app_id);
        message += String.format("&signature=%s", signature);
        message += String.format("&nonce=%s", nonce);
        message += String.format("&timestamp=%s", timestamp);
        message += String.format("&serial=%s", serial);
        message += String.format("&signature_Type=%s", signatureType);
        message += String.format("&requestBody=%s", requestBody);
        //   PaymentCallbackLogEntity logEntity = PaymentCallbackLogEntity.getInstance().add("", message, 3, "UserAskVIPCardOrder");
        WechatPaySDK wechatPaySDK = new WechatPaySDK();
        SyncResult jsAPIv3Result = wechatPaySDK.verifySign(signature, nonce, timestamp, serial, signatureType, requestBody);
        if (jsAPIv3Result.code != 0) return "FAIL";
        JSONObject decryptObject = (JSONObject) jsAPIv3Result.data;
        String trade_state = JsonUtil.getString(decryptObject, "trade_state");
        if (!"SUCCESS".equalsIgnoreCase(trade_state)) {
            LogsUtil.error("", "微信支付回调失败，%s", message);
            return "FAIL";
        }
        //订单号，根据支付情况修改参数
        String orderSn = JsonUtil.getString(decryptObject, "out_trade_no");

        //添加到支付记录
        PaymentCallbackLogEntity.getInstance().addLog(orderSn, decryptObject.toJSONString());

        /**
         * TODO 更新订单信息
         */
        ConsumeOrdersEntity consumeOrdersEntity = consumeOrdersService.findByOrderSn(orderSn);
        if(consumeOrdersEntity == null) {
            return "error";
        }
        JSONObject amount = decryptObject.getJSONObject("amount");
        int totalAmount = amount.getIntValue("total");
        double payPrice = new BigDecimal(totalAmount)
                .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP)
                .doubleValue();

        SyncResult result= consumeOrdersService.paySuccess(
                orderSn
                ,decryptObject.getString("transaction_id")
                , BigDecimal.valueOf(payPrice)
                ,BigDecimal.valueOf(payPrice)
        );
        if(result.code != 0) return "error";
        //如果有消耗积分
        if (consumeOrdersEntity.use_integral > 0) {
            UserIntegralDetailEntity.getInstance().decrIntegral(consumeOrdersEntity.uid
                    , -consumeOrdersEntity.use_integral
                    , EUserIntegralType.Recharge_Deduct //目前只有充值使用积分 其他地方暂时没有使用积分情况，这个需要注意
                    , consumeOrdersEntity.id
                    , "充值抵扣积分"
            );
        }
        FulfillmentService fulfillmentService = fulfillmentServiceFactory.getService(consumeOrdersEntity.product_type);
        fulfillmentService.processFulfillment(consumeOrdersEntity);

        return "";
    }

    public static String getBody(HttpServletRequest request) throws IOException {

        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(request.getInputStream());
            bufferedReader = new BufferedReader(inputStreamReader);
            char[] charBuffer = new char[128];
            int bytesRead;
            while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                stringBuilder.append(charBuffer, 0, bytesRead);
            }
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }
        return stringBuilder.toString();
    }
}
