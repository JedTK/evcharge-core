package com.evcharge.libsdk.wechat;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.entity.user.UserEntity;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.exception.ServiceException;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;


import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.Convert;
import com.xyzs.utils.LogsUtil;
import com.wechat.pay.java.core.notification.RequestParam;
import com.xyzs.utils.MapUtil;

import java.util.LinkedHashMap;
import java.util.Map;

public class WechatPaySDK {
    private static String appletAppId;
    /**
     * 回调地址
     */
    public static String baseNotifyUrl;
    /**
     * 商户号
     */
//    public static  String merchantId = "1316627601";
    public static String merchantId;
    /**
     * 商户API私钥路径
     */
//    private static  String privateKeyPath = ConfigManager.getString("wechat.pay.key_path");;
    private static String privateKeyPath;
    /**
     * 商户证书序列号
     */
//    private static  String merchantSerialNumber = "40CC213CF82F0EF842535BBF25D43F642C095A49";
    private static String merchantSerialNumber;
    /**
     * 商户APIV3密钥
     */
//    private static  String apiV3Key = "QSpbmhEq0Zdcy3YIkKMoJ6Lu7ex1U9fa";
    private static String apiV3Key;

    /**
     * 回调地址
     */
    private static String notifyUrl;

//    private static Map<String, RSAAutoCertificateConfig> mMerchantConfig;

    /**
     * 初始化微信支付sdk
     */
    public WechatPaySDK() {
        /*
         * key= wechat.payment.config
         * merchantId->merchant_id #商户号
         * privateKeyPath->private_key_path #私钥地址
         * merchantSerialNumber->merchant_serial_number #商户证书序列号
         * apiV3Key->api_v3_key #商户APIV3密钥
         * notifyUrl->notify_url #回调地址
         * appletAppId->applet_app_id #小程序id
         * */
        String text = SysGlobalConfigEntity.getString("Wechat:payment:config");
        JSONObject jsonObject = JSONObject.parseObject(text);
        merchantId = jsonObject.getString("merchant_id");
        privateKeyPath = jsonObject.getString("private_key_path");
        merchantSerialNumber = jsonObject.getString("merchant_serial_number");
        apiV3Key = jsonObject.getString("api_v3_key");
        notifyUrl = jsonObject.getString("notify_url");
        appletAppId = jsonObject.getString("applet_app_id");
    }

    /**
     * 创建订单
     *
     * @param orderSN     订单编号
     * @param totalAmount 订单金额
     * @param description 订单描述
     * @param uid         用户openid
     * @param callBackUrl 回调地址
     * @return
     */
    public SyncResult create(
            int paySource
            ,long uid
            , String orderSN
            , String description
            , double totalAmount
            , String callBackUrl
    ) {
//int paySource,long uid, String orderSn, String describe, double totalPrice, String callBackUrl, Map<String, Object> param
        int paymentAmount = Convert.toInt(totalAmount * 100);
        Config config = initConfig();

        Map<String, Object> userInfo = UserEntity.getInstance().findSourceUserByID(uid, paySource);
        if (userInfo.isEmpty()) return new SyncResult(1, "用户不存在");

        String openId = MapUtil.getString(userInfo, "open_id");
        String notify_url = String.format("%s%s", notifyUrl, callBackUrl);
        JsapiServiceExtension service = new JsapiServiceExtension.Builder()
                .config(config)
                .signType("RSA") // 不填默认为RSA
                .build();

        PrepayRequest request = new PrepayRequest();
        Amount amount = new Amount();
        amount.setTotal(paymentAmount);
        request.setAmount(amount);
        request.setAppid(appletAppId);
        request.setMchid(merchantId);
        request.setDescription(description);
        request.setNotifyUrl(notify_url);
        request.setOutTradeNo(orderSN);

        Payer payer = new Payer();
        payer.setOpenid(openId);
        request.setPayer(payer);
        // response包含了调起支付所需的所有参数，可直接用于前端调起支付
        PrepayWithRequestPaymentResponse response = service.prepayWithRequestPayment(request);
//        System.out.println(response);
        JSONObject jsonObject=new JSONObject();
        jsonObject.put("appId",response.getAppId());
        jsonObject.put("timeStamp",response.getTimeStamp());
        jsonObject.put("nonceStr",response.getNonceStr());
        jsonObject.put("package",response.getPackageVal());
        jsonObject.put("signType",response.getSignType());
        jsonObject.put("paySign",response.getPaySign());

        return new SyncResult(0, "", jsonObject);

    }


    /**
     * 查询订单
     *
     * @param orderSn 订单编号
     * @return
     */
    public SyncResult query(String orderSn) {
        Config config = initConfig();
        QueryOrderByOutTradeNoRequest queryRequest = new QueryOrderByOutTradeNoRequest();
        queryRequest.setMchid(merchantId);
        queryRequest.setOutTradeNo(orderSn);
        JsapiService service = new JsapiService.Builder().config(config).build();
        try {
            Transaction result = service.queryOrderByOutTradeNo(queryRequest);
            System.out.println(result.getTradeState());

//            if(!("SUCCESS").equals(result.getTradeState())){
//                return new SyncResult(1, result.getTradeStateDesc());
//            }
//            {
//                "amount": {
//                "currency": "CNY",
//                        "payer_currency": "CNY",
//                        "payer_total": 1,
//                        "total": 1
//            },
//                "appid": "wxe485dd4954a7bba6",
//                    "attach": "",
//                    "bank_type": "OTHERS",
//                    "mchid": "1639057580",
//                    "out_trade_no": "RE202406201410474VRJ",
//                    "payer": {
//                "openid": "o4C6w4kEgQ07PNE9_eAsx9ByXW-o"
//            },
//                "promotion_detail": [],
//                "success_time": "2024-06-20T14:11:01+08:00",
//                    "trade_state": "SUCCESS",
//                    "trade_state_desc": "支付成功",
//                    "trade_type": "JSAPI",
//                    "transaction_id": "4200002223202406203215499007"
//            }
            JSONObject jsonObject=new JSONObject();

            jsonObject.put("amount",result.getAmount());
            jsonObject.put("appid",result.getAppid());
            jsonObject.put("attach",result.getAttach());
            jsonObject.put("bank_type",result.getBankType());
            jsonObject.put("mchid",result.getMchid());
            jsonObject.put("out_trade_no",result.getOutTradeNo());
            jsonObject.put("payer",result.getPayer());
            jsonObject.put("promotion_detail",result.getPromotionDetail());
            jsonObject.put("success_time",result.getSuccessTime());
            jsonObject.put("trade_state",result.getTradeState());
            jsonObject.put("trade_state_desc",result.getTradeStateDesc());
            jsonObject.put("trade_type",result.getTradeType());
            jsonObject.put("transaction_id",result.getTransactionId());
            return new SyncResult(0, "success", jsonObject);
        } catch (ServiceException e) {
            // API返回失败, 例如ORDER_NOT_EXISTS
            System.out.printf("code=[%s], message=[%s]\n", e.getErrorCode(), e.getErrorMessage());
            System.out.printf("reponse body=[%s]\n", e.getResponseBody());
            return new SyncResult(1, "error", e.getResponseBody());
        }
    }


    /**
     * 退款接口
     *
     * @param orderSn     订单编号
     * @param totalAmount 退款金额
     * @param reason      退款原因
     * @return
     */
    public SyncResult refund(String orderSn,String refundOrderSn,  double totalAmount, String reason) {
        Config config = initConfig();
        long refundAmount = Math.abs(Convert.toInt(totalAmount * 100));
        RefundService service = new RefundService.Builder().config(config).build();
        CreateRequest request = new CreateRequest();
        AmountReq amount = new AmountReq();
        amount.setRefund(refundAmount);
        amount.setTotal(refundAmount);
        amount.setCurrency("CNY");
        request.setOutTradeNo(orderSn);
        request.setOutRefundNo(refundOrderSn);
        request.setAmount(amount);
        request.setReason(reason);

        try {
            Refund result = service.create(request);
//            System.out.println(result.);
//            Map<String, Object> refundData = new LinkedHashMap<>();
            System.out.println(result);

            JSONObject refundInfo=new JSONObject();

//            refundId: 50310110232024062003540241356
//            outRefundNo: FU20240620151953ZJQ8
//            transactionId: 4200002208202406208127471990
//            outTradeNo: RE20240620144530B8HK
//            userReceivedAccount: 支付用户零钱
//            successTime: null
//            createTime: 2024-06-20T15:20:00+08:00

            refundInfo.put("refund_bank_order_no",result.getRefundId());
            refundInfo.put("refund_bank_trx_no",result.getTransactionId());

            return new SyncResult(0, "success", refundInfo);
        } catch (ServiceException e) {
            // API返回失败, 例如ORDER_NOT_EXISTS
            System.out.printf("code=[%s], message=[%s]\n", e.getErrorCode(), e.getErrorMessage());
            System.out.printf("reponse body=[%s]\n", e.getResponseBody());
            return new SyncResult(1, "error", e.getResponseBody());
        }
    }

    /**
     * 初始化配置
     *
     * @return
     */
    public RSAAutoCertificateConfig initConfig() {
//        if (mMerchantConfig == null) mMerchantConfig = new LinkedHashMap<>();
        String _privateKeyPath = String.format("%s%s", System.getProperty("user.dir"), privateKeyPath);
        //        mMerchantConfig.put(merchantId, config);
        return new RSAAutoCertificateConfig.Builder()
                .merchantId(merchantId)
                .privateKeyFromPath(_privateKeyPath)
                .merchantSerialNumber(merchantSerialNumber)
                .apiV3Key(apiV3Key)
                .build();

//        if (!mMerchantConfig.containsKey(merchantId)) {
//            String _privateKeyPath = String.format("%s%s", System.getProperty("user.dir"), privateKeyPath);
//            RSAAutoCertificateConfig config = new RSAAutoCertificateConfig.Builder()
//                    .merchantId(merchantId)
//                    .privateKeyFromPath(_privateKeyPath)
//                    .merchantSerialNumber(merchantSerialNumber)
//                    .apiV3Key(apiV3Key)
//                    .build();
//            mMerchantConfig.put(merchantId, config);
//            return config;
//        }
//        return mMerchantConfig.get(merchantId);
    }


    /**
     * 验签
     *
     * @param signature     签名
     * @param nonce         随机字符串
     * @param timestamp     时间戳
     * @param serial
     * @param signatureType 签名类型
     * @param requestBody   响应结构体
     * @return
     */
    public SyncResult verifySign(String signature, String nonce, String timestamp, String serial, String signatureType, String requestBody) {
        try {
            // 构造 RequestParam
            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(serial)
                    .nonce(nonce)
                    .signature(signature)
                    .timestamp(timestamp)
                    .signType(signatureType)// 若未设置signType，默认值为 WECHATPAY2-SHA256-RSA2048
                    .body(requestBody)
                    .build();
            NotificationConfig config = initConfig();
            NotificationParser parser = new NotificationParser(config);
            JSONObject decryptObject = parser.parse(requestParam, JSONObject.class);
            return new SyncResult(0, "", decryptObject);
        } catch (Exception e) {
            LogsUtil.error(e, "", "微信支付，JsAPIv3版本验签发生错误");
        }


        return new SyncResult(1, "");
    }


}
