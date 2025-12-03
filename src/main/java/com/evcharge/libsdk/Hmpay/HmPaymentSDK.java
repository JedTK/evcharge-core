package com.evcharge.libsdk.Hmpay;

import cn.com.sand.hmpay.contants.TradeConstants;
import cn.com.sand.hmpay.enums.*;
import cn.com.sand.hmpay.util.JsonUtil;
import cn.com.sand.hmpay.util.RsaUtil;
import cn.com.sand.hmpay.util.StringUtil;
import cn.com.sand.hmpay.v2.HmpayClient;
import cn.com.sand.hmpay.vo.common.ExtendParams;
import cn.com.sand.hmpay.vo.request.TradeRequest;
import cn.com.sand.hmpay.vo.request.biz.*;
import cn.com.sand.hmpay.vo.request.common.*;
import cn.com.sand.hmpay.vo.response.TradeResponse;
import cn.com.sand.hmpay.vo.response.biz.TradeBizCreateResponse;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.entity.user.UserEntity;
import com.evcharge.enumdata.ERechargePaymentType;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;

public class HmPaymentSDK {

    //    String host = "https://star.sandgate.cn/gateway";
    String host = "https://hmpay.sandpay.com.cn/gateway";
    String url = host + "/api";

    //商户号appid
    String sandAppId = ConfigManager.getString("sand.hm_mid"); // 代理商
    //微信appid
    String WechatAppId = ConfigManager.getString("wechat.appId"); // 商户
    String AlipayAppId= SysGlobalConfigEntity.getString("Alipay:MiniApp:AppID");
    // 平台公钥-验签
    String publicKey = ConfigManager.getString("sand.hm_pub_key");
    // 代理商或商户私钥-签名
    String privateKey = ConfigManager.getString("sand.hm_pri_key");
    String format = "JSON";
    String charset = "UTF-8";
    String signType = TradeApiSignTypeEnums.RSA.name();
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    String version = "1.0";
//    String storeId = "100001"; //不是固定，在衫德河马支付后台查询
    String storeId = ConfigManager.getString("sand.hm_store_id","100001");; //不是固定，在衫德河马支付后台查询
    //随机数
    String nonce = String.valueOf(System.currentTimeMillis());


    String operatorId = "operatorId";
    String terminalId = "terminalId";
    Double totalAmount = 0.02D;
    Double refundAmount = null;
    Double confirmAmount = null;


    // 商户订单号
    String outOrderNo = String.valueOf(System.currentTimeMillis());
    String goodsTag = null;
    Double discountableAmount = 0.01;
    String productCode = "";

    /**
     * （普通）创建订单
     *
     * @param uid
     * @param orderSn
     * @param describe
     * @param totalPrice
     * @param callBackUrl
     * @param param       额外参数
     * @return
     */
//    public SyncResult create(String paySource,long uid, String orderSn, String describe, double totalPrice, String callBackUrl, Map<String, Object> param) {
//        return create(paySource,uid, orderSn, describe, totalPrice, callBackUrl, param);
//        //demo中的方法 testTradeCreate
//        //region 用户信息
//
//    }

    /**
     * （分账）创建订单
     *
     * @param uid
     * @param orderSn
     * @param describe
     * @param totalPrice
     * @param callBackUrl
     * @param param
     * @return
     */
//    public SyncResult shareCreate(long uid, String orderSn, String describe, double totalPrice, String callBackUrl, Map<String, Object> param) {
////        SettleSharingReceiver settleSharingReceiver = new SettleSharingReceiver();
////        settleSharingReceiver.setAccCode("12312");
////        settleSharingReceiver.setAmount(10.0);
////        settleSharingReceiver.setMerchantNo("12312312");
////        settleSharingReceiver.setDescription("分账");
////
////        // 声明一个 Student 类型的数组变量
////        //        Student[] students;
////        // 使用 new 关键字为数组分配内存，并指定数组的长度
////        //        students = new Student[5];
////
////        SettleSharingReceiver[] SettleSharingReceivers;
////
////        SettleSharingReceivers = new SettleSharingReceiver[5];
////
////        return new SyncResult(1, "");
//        return create(uid, orderSn, describe, totalPrice, callBackUrl, param, true);
//    }

  /**
     * （普通）创建订单
     *
     * @param uid         用户id
     * @param orderSn     订单号
     * @param describe    描述
     * @param totalPrice  总价
     * @param callBackUrl 回调方法 示例 /charge/sandCallBack
     * @return
     */
    public SyncResult create(int paySource,long uid, String orderSn, String describe, double totalPrice, String callBackUrl) {
        return create(paySource,uid, orderSn, describe, totalPrice, callBackUrl, null);
    }

    /**
     * （核心）创建订单
     *
     * @param uid
     * @param orderSn
     * @param describe
     * @param totalPrice
     * @param callBackUrl
     * @param param
     * @param paySource 支付渠道 wechat/alipay
     * @return
     */
    public SyncResult create(int paySource,long uid, String orderSn, String describe, double totalPrice, String callBackUrl, Map<String, Object> param) {
        //demo中的方法 testTradeCreate
        //region 用户信息

        Map<String, Object> userInfo = UserEntity.getInstance().findSourceUserByID(uid,paySource);
        if (userInfo.isEmpty()) return new SyncResult(1, "用户不存在");

        String buyerId = MapUtil.getString(userInfo, "open_id");
        //endregion

        //region 回调域名
        String notifyUrl = String.format("%s%s", ConfigManager.getString("consume.notify_url"), callBackUrl);
        //  notifyUrl = notifyUrl.replaceAll("//", "/");
        //endregion
        System.out.println("notifyUrl+" + notifyUrl);
        String appID=WechatAppId;
        String payWay=TradeBizPayWayEnums.WECHAT.name();
        if(paySource== ERechargePaymentType.hm_alipay){
            payWay = TradeBizPayWayEnums.ALIPAY.name();
            appID=AlipayAppId;
        }


        String payType = TradeBizPayTypeEnums.JSAPI.name();

        String method = TradeApiMethodEnums.TRADE_CREATE.getMethod();

        String createTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        //三分钟后超时
        String expireTime = LocalDateTime.now().plusMinutes(3).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        String extendParams = "";
        if (param != null) {
            extendParams = JSONObject.toJSONString(param);
        }
        try {
            String bizContent = TradeCreateRequest
                    .builder()
                    .payType(payType)
                    .buyerId(buyerId)
                    .merAppId(appID)
                    .merBuyerId(buyerId)
                    .body(describe)
                    .createIp(HttpRequestUtil.getIP()) //获取ip
                    .createTime(createTime)
                    .expireTime(expireTime)
                    .storeId(storeId)
                    .notifyUrl(notifyUrl)
                    .outOrderNo(orderSn)
                    .payWay(payWay)
                    .totalAmount(totalPrice)
                    //req_reserved
                    .reqReserved(extendParams)
                    .discountInfo(DisCountInfo.builder().build())
                    .build()
                    .toJsonString();

            SyncResult r = execute(method, bizContent);
            if (r.code != 0) {
                return new SyncResult(r.code, r.msg);
            }

            TradeResponse tradeResponse = (TradeResponse) r.data;
            //解析响应，获取payData
            String data = tradeResponse.getData();
            TradeBizCreateResponse tradeBizCreateResponse = JsonUtil.fromJson(data, TradeBizCreateResponse.class);
            String payData = tradeBizCreateResponse.getPayData();
            // System.out.println(payData);
            return new SyncResult(0, "success", JSONObject.parseObject(payData));
        }catch (Exception e){
            LogsUtil.error(this.getClass().getName(),e.getMessage());
            return new SyncResult(1,e.getMessage());
        }

    }

    /**
     * 根据订单信息查询支付信息
     *
     * @param orderSn
     * @return
     */
    public SyncResult query(String orderSn) {

        String method = TradeApiMethodEnums.TRADE_QUERY.getMethod();
        String bizContent = TradeQueryRequest
                .builder()
                .outOrderNo(orderSn)
                .platTrxNo(null)
                .bankOrderNo(null)
                .build()
                .toJsonString();

        SyncResult res = execute(method, bizContent);

        if (res.code != 0) {
            return new SyncResult(res.code, res.msg);
        }
        TradeResponse tradeResponse = (TradeResponse) res.data;
        //解析响应，获取payData
        String data = tradeResponse.getData();
//        String payResult = tradeBizCreateResponse.getReqReserved();
        return new SyncResult(0, "success", JSONObject.parseObject(data));
    }

    /**
     * 关闭订单
     * @return
     */
    public SyncResult close() {

        return new SyncResult(1, "");
    }

    /**
     * 取消订单
     * @return
     */
    public SyncResult cancel() {

        return new SyncResult(1, "");
    }

    /**
     * 订单退款
     *
     * @param orderSn     退款订单
     * @param bankOrderNo 退款银行流水号
     * @param totalAmount 退款金额
     * @return
     */
    public SyncResult refund(String orderSn, String bankOrderNo, double totalAmount) {

//        String oldOutOrderNo = null;
//        String platTrxNo = "66618122xxxxxxxx551280309251";
        String method = TradeApiMethodEnums.TRADE_REFUND.getMethod();

        String bizContent = TradeRefundRequest
                .builder()
                .outOrderNo(orderSn)
//                .platTrxNo(payOrderSn)
                .bankOrderNo(bankOrderNo)
                .refundAmount(totalAmount)
                .refundRequestNo(outOrderNo)
                .refundReason("退款理由-正常退款")
                .storeId(storeId)
                .terminalId(terminalId)
                .operatorId(operatorId)
                .build()
                .toJsonString();

        SyncResult r = execute(method, bizContent);
        if (r.code != 0) {
            return new SyncResult(r.code, r.msg);
        }
        return new SyncResult(0, "success");
    }

    /**
     * 订单退款
     *
     * @param orderSn     退款订单
     * @param bankOrderNo 退款银行流水号
     * @param totalAmount 退款金额
     * @param reason      退款理由
     * @return
     */
    public SyncResult refund(String orderSn, String bankOrderNo, double totalAmount, String reason) {
        totalAmount = Math.abs(totalAmount);
//        String oldOutOrderNo = null;
//        String platTrxNo = "66618122xxxxxxxx551280309251";
        String method = TradeApiMethodEnums.TRADE_REFUND.getMethod();

        String bizContent = TradeRefundRequest
                .builder()
                .outOrderNo(orderSn)
//                .platTrxNo(payOrderSn)
                .bankOrderNo(bankOrderNo)
                .refundAmount(totalAmount)
                .refundRequestNo(outOrderNo)
                .refundReason(reason)
                .storeId(storeId)
                .terminalId(terminalId)
                .operatorId(operatorId)
                .build()
                .toJsonString();

        SyncResult r = execute(method, bizContent);
        if (r.code != 0) {
            return new SyncResult(r.code, r.msg);
        }
        TradeResponse tradeResponse = (TradeResponse) r.data;
        //解析响应，获取payData
        String data = tradeResponse.getData();
        LogsUtil.info("refund", String.format("订单退款，订单号：%s，退款金额：%s,接口返回：%s", orderSn, totalAmount, data));
        return new SyncResult(0, "success", JSONObject.parseObject(data));
    }

    /**
     * 退款订单查询
     *
     * @param outOrderNo    商户订单号
     * @param refundOrderSn 原商户订单号
     * @return
     */
    public SyncResult refundQuery(String outOrderNo, String refundOrderSn) {
        String method = TradeApiMethodEnums.TRADE_REFUND_QUERY.getMethod();
        String bizContent = TradeRefundQueryRequest
                .builder()
                .outOrderNo(outOrderNo)
                .platTrxNo(null)
                .bankOrderNo(null)
                .refundRequestNo(refundOrderSn)
                .build()
                .toJsonString();
        SyncResult res = execute(method, bizContent);
        if (res.code != 0) {
            return new SyncResult(res.code, res.msg);
        }
        TradeResponse tradeResponse = (TradeResponse) res.data;
        //解析响应，获取payData
        String data = tradeResponse.getData();
//        String payResult = tradeBizCreateResponse.getReqReserved();
        return new SyncResult(0, "success", JSONObject.parseObject(data));
    }

    /**
     * 检查回调内容
     *
     * @param content
     * @return
     */
    public SyncResult checkNotifyContent(String content) {

        try {
            String[] pairs = StringUtil.tokenizeToStringArray(content, "&");
            Map<String, String> result = new LinkedHashMap<>((pairs.length));
            for (String pair : pairs) {
                int idx = pair.indexOf('=');
                if (idx == -1) {
                    result.put(URLDecoder.decode(pair, "UTF-8"), null);
                } else {
                    String name = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                    String value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                    result.put(name, value);
                }
            }
//            if (result.containsKey("sign")) {
//                String sign = result.get("sign");
//                if (sign != null) {
//                    sign = sign.trim();                   // 去首尾空白
//                    sign = sign.replace(" ", "+");        // 恢复 URL 替换
//                    sign = sign.replaceAll("[^A-Za-z0-9+/=]", ""); // 清理非法字符
//                    result.put("sign", sign);
//                    System.out.println("sign cleaned: [" + sign + "]");
//                    System.out.println("sign length: " + sign.length());
//                }
//            }

            System.out.println(result);
            boolean checkResultSign = RsaUtil.rsaCheckV2(result, publicKey, TradeConstants.CHARSET, "RSA");
            if (!checkResultSign) {
                //todo 响应验签失败 逻辑处理
                System.out.println("响应验签失败");
                return new SyncResult(1, String.format("[支付回调]检查支付回调信息失败，验签失败。"));
            }
//            System.out.println(checkResultSign);
            return new SyncResult(0, "success");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return new SyncResult(1, String.format("[支付回调]检查支付回调信息失败，失败原因：%s", e.getMessage()));
        }


    }

    /**
     * 执行
     *
     * @param method
     * @param bizContent
     * @return
     */
    private SyncResult execute(String method, String bizContent) {
        TradeRequest tradeRequest = buildTradeRequest(method, bizContent);
        //todo 打印请求日志
        System.out.println(JsonUtil.toJson(tradeRequest));

        TradeResponse tradeResponse = HmpayClient.execute(url, tradeRequest);
        try {
            //todo 打印响应日志
            System.out.println(JsonUtil.toJson(tradeResponse));

            boolean checkResultSign = RsaUtil.rsaCheckV2(tradeResponse.toMap(), publicKey, TradeConstants.CHARSET);
            if (!checkResultSign) {
                //todo 响应验签失败 逻辑处理
                System.out.println("响应验签失败");
                return new SyncResult(1, "响应验签失败");
            }

            return new SyncResult(0, "验签成功", tradeResponse);
        } catch (Exception e) {
            return new SyncResult(1, String.format("响应验签失败，失败原因：%s", e.getMessage()));
        }

    }

    /**
     * 创建交易请求数据
     *
     * @param method
     * @param bizContent
     * @return
     */
    private TradeRequest buildTradeRequest(String method, String bizContent) {
        return TradeRequest.builder()
                .appId(sandAppId)
//                .subAppId(subAppId)
                .format(format)
                .charset(charset)
                .method(method)
                .signType(signType)
                .timestamp(timestamp)
                .nonce(nonce)
                .version(version)
                .bizContent(bizContent)
                .build()
                .buildSign(privateKey);
    }
}
