package com.evcharge.libsdk.sandpay;

import com.alibaba.fastjson2.JSONObject;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.apache.tomcat.util.codec.binary.Base64;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class PaymentSDK {

    //  public static  Logger logger = LoggerFactory.getLogger(DemoBase.class);

    //默认配置的是UTF-8
    public static String encoding = "UTF-8";
    //签名类型,默认01-SHA1+RSA
    public static String signType = "01";
    //版本号 默认 1.0
    public static String version = "1.0";
    //http连接超时时间
    public static int connectTimeout = 300000;
    //http响应超时时间
    public static int readTimeout = 600000;

    /**
     * 创建订单
     *
     * @param order     订单信息
     * @param userInfo  用户信息，通过user实体类获取
     * @param orderType 订单类型
     * @return
     * @throws Exception
     */
    public SyncResult create(JSONObject order, Map<String, Object> userInfo, String orderType) throws Exception {
        JSONObject header = new JSONObject();
        JSONObject body = new JSONObject();
        String reqAddress = "/gateway/api/order/pay";   //接口报文规范中获取
        SDKConfig.getConfig().loadProperties(); //初始化sdk配置文件

        CertUtil.init(SDKConfig.getConfig().getSandCertPath(), SDKConfig.getConfig().getSignCertPath(), SDKConfig.getConfig().getSignCertPwd());//初始化加密插件

        header.put("version", "1.0");            //版本号
        header.put("method", "sandpay.trade.pay");            //接口名称:统一下单
        header.put("mid", SDKConfig.getConfig().getMid());    //商户号
        header.put("accessType", "1");                    //接入类型设置为平台商户接入
//        header.put("plMid", plMid);
        header.put("channelType", "07");                    //渠道类型：07-互联网   08-移动端
        header.put("reqTime", TimeUtil.toTimeString(TimeUtil.getTimestamp(), "yyyyMMddHHmmss"));    //请求时间
        header.put("productId", "00002021");                //产品编码

        body.put("orderCode", order.get("ordersn"));       //商户订单号
        body.put("totalAmount", "000000000001");         //订单金额

//        body.put("totalAmount", String.format("%012d", Math.round(order.getDouble("total_price")*100))); //订单金额
        body.put("subject", "商品购买");                     //订单标题
        body.put("body", "商品购买" + order.get("total_price"));//订单描述
        //  body.put("txnTimeOut", DemoBase.getNextDayTime()); //订单超时时间
        body.put("clientIp", HttpRequestUtil.getIP());       //客户端IP
        body.put("notifyUrl", ConfigManager.getString("sand.notify_url"));   //异步通知地址
        body.put("frontUrl", ConfigManager.getString("sand.front_url"));    //前台通知地址

        JSONObject payExtra = new JSONObject();

        payExtra.put("subAppid", ConfigManager.getString("wechat.appId"));  //商户公众号Appid
        payExtra.put("userId", userInfo.get("open_id"));                      //用户在商户公众号下得唯一标示openid
        body.put("payMode", "sand_wx");  //支付模式
        body.put("payExtra", payExtra.toJSONString());//支付扩展域

        //业务扩展参数
        JSONObject extend = new JSONObject();
        extend.put("orderType", orderType); //在回调获取orderType 根据不同的订单类型 调用不同的回调方法
        body.put("extend", extend.toJSONString());

        JSONObject result = requestServer(header, body, reqAddress); //请求服务器方法
        if (result != null && result.size() > 0) {
            return new SyncResult(0, "创建订单成功", result);
        } else {
            return new SyncResult(1, "创建订单失败");
        }
    }

    /**
     * 检查衫德支付回调信息，已经主动到衫德查询
     *
     * @param sign
     * @param data
     * @param charset
     * @return
     */
    public SyncResult checkSandCallbackData(String sign, String data, String charset) {
        SDKConfig.getConfig().loadProperties(); //初始化sdk配置文件
        LogsUtil.info("", "", "====获取回调信息=====");
        LogsUtil.info("", "", data);
        LogsUtil.info("", "", "====获取签名信息=====");
        LogsUtil.info("", "", sign);
        try {
            CertUtil.init(SDKConfig.getConfig().getSandCertPath(), SDKConfig.getConfig().getSignCertPath(), SDKConfig.getConfig().getSignCertPwd());//初始化加密插件
            boolean valid;

            valid = CryptoUtil.verifyDigitalSign(data.getBytes(charset), Base64.decodeBase64(sign), CertUtil.getPublicKey(), "SHA1WithRSA");

            if (!valid) {
                return new SyncResult(1, "验签失败");
            }

            //验签成功
            JSONObject dataJson = JSONObject.parseObject(data);
            String orderSn = dataJson.getJSONObject("body").getString("orderCode");
            SyncResult sandResult = this.query(orderSn);

            if (sandResult.code != 0) {
                return new SyncResult(1, "主动查询订单，订单信息不存在或订单编号有误。");
            }
            JSONObject sandData = (JSONObject) sandResult.data;
            if (sandData.size() == 0 || !("00".equals(sandData.getString("orderStatus")))) {
                return new SyncResult(1, "主动查询订单，订单状态不正确。");
            }
            return new SyncResult(0, "处理完成", sandData);

        } catch (Exception e) {
            return new SyncResult(1, e.getMessage());
        }

    }


    /**
     * 创建订单
     *
     * @param OrderSN      订单号
     * @param subject      订单标题
     * @param describe     订单描述
     * @param wechatOpenId 微信用户openId
     * @param orderType    订单类型
     * @return
     * @throws Exception
     */
    public SyncResult create(String OrderSN
            , String subject
            , String describe
            , String wechatOpenId
            , String orderType) throws Exception {
        JSONObject header = new JSONObject();
        JSONObject body = new JSONObject();
        String reqAddress = "/gateway/api/order/pay";   //接口报文规范中获取
        SDKConfig.getConfig().loadProperties(); //初始化sdk配置文件

        CertUtil.init(SDKConfig.getConfig().getSandCertPath(), SDKConfig.getConfig().getSignCertPath(), SDKConfig.getConfig().getSignCertPwd());//初始化加密插件

        header.put("version", "1.0");            //版本号
        header.put("method", "sandpay.trade.pay");            //接口名称:统一下单
        header.put("mid", SDKConfig.getConfig().getMid());    //商户号
        header.put("accessType", "1");                    //接入类型设置为平台商户接入
//        header.put("plMid", plMid);
        header.put("channelType", "07");                    //渠道类型：07-互联网   08-移动端
        header.put("reqTime", TimeUtil.toTimeString(TimeUtil.getTimestamp(), "yyyyMMddHHmmss"));    //请求时间
        header.put("productId", "00002021");                //产品编码

        body.put("orderCode", OrderSN);       //商户订单号
        body.put("totalAmount", "000000000001");         //订单金额

//        body.put("totalAmount", String.format("%012d", Math.round(order.getDouble("total_price")*100))); //订单金额
        body.put("subject", subject);                     //订单标题
        body.put("body", describe);//订单描述
        //  body.put("txnTimeOut", DemoBase.getNextDayTime()); //订单超时时间
        body.put("clientIp", HttpRequestUtil.getIP());       //客户端IP
        body.put("notifyUrl", ConfigManager.getString("sand.notify_url"));   //异步通知地址
        body.put("frontUrl", ConfigManager.getString("sand.front_url"));    //前台通知地址

        JSONObject payExtra = new JSONObject();

        payExtra.put("subAppid", ConfigManager.getString("wechat.appId"));  //商户公众号Appid
        payExtra.put("userId", wechatOpenId);//用户在商户公众号下得唯一标示openid
        body.put("payMode", "sand_wx");  //支付模式
        body.put("payExtra", payExtra.toJSONString());//支付扩展域

        //业务扩展参数
        JSONObject extend = new JSONObject();
        extend.put("orderType", orderType); //在回调获取orderType 根据不同的订单类型 调用不同的回调方法
        body.put("extend", extend.toJSONString());

        JSONObject result = requestServer(header, body, reqAddress); //请求服务器方法
        if (result != null && result.size() > 0) {
            return new SyncResult(0, "创建订单成功", result);
        } else {
            return new SyncResult(1, "创建订单失败");
        }
    }

    /**
     * 主动查询订单状态
     *
     * @param order_sn
     * @return
     */
    public SyncResult query(String order_sn) {
        SDKConfig.getConfig().loadProperties(); //初始化sdk配置文件
        String reqAddress = "/gw/api/order/query";   //接口报文规范中获取
        JSONObject header = new JSONObject();
        JSONObject body = new JSONObject();
        header.put("version", "1.0");                          //版本号
        header.put("method", "sandpay.trade.query");            //接口名称:统一下单
        header.put("mid", SDKConfig.getConfig().getMid());    //商户号
        header.put("accessType", "1");                    //接入类型设置为平台商户接入

        header.put("channelType", "07");                    //渠道类型：07-互联网   08-移动端
        header.put("reqTime", TimeUtil.toTimeString(TimeUtil.getTimestamp(), "yyyyMMddHHmmss"));    //请求时间
        header.put("productId", "00002000");                //产品编码
        body.put("orderCode", order_sn);
        body.put("extend", "");
        JSONObject result = requestServer(header, body, reqAddress);
        if (result != null) {
            return new SyncResult(0, "订单查询成功", result);
        } else {
            return new SyncResult(1, "订单查询失败");
        }
    }

    /**
     * 发送请求
     *
     * @param header
     * @param body
     * @param reqAddr
     * @return
     */
    public JSONObject requestServer(JSONObject header, JSONObject body, String reqAddr) {
        Map<String, String> reqMap = new HashMap<String, String>();
//        JSONObject reqMap = new JSONObject();
        JSONObject reqJson = new JSONObject();
        reqJson.put("head", header);
        reqJson.put("body", body);
        String reqStr = reqJson.toJSONString();
        String reqSign;
        // 签名
        try {
            reqSign = new String(Base64.encodeBase64(CryptoUtil.digitalSign(reqStr.getBytes(encoding), CertUtil.getPrivateKey(), "SHA1WithRSA")));
        } catch (Exception e) {
            // logger.error(e.getMessage());
            return null;
        }
        //整体报文格式
        reqMap.put("charset", encoding);
        reqMap.put("data", reqStr);
        reqMap.put("signType", signType);
        reqMap.put("sign", reqSign);
        reqMap.put("extend", "");
//        String requestURl=SDKConfig.getConfig().getUrl() + reqAddr;
//        System.out.println(requestURl);
        String result;
        try {
            result = HttpUtil.sendPost(SDKConfig.getConfig().getUrl() + reqAddr, httpBuildQuery(reqMap), new LinkedHashMap<>(), "application/x-www-form-urlencoded;charset=" + encoding);
            result = URLDecoder.decode(result, encoding);
        } catch (Exception e) {
            //    logger.error(e.getMessage());
            LogsUtil.error(e, "", "请求衫德接口失败");
            return null;
        }

        Map<String, String> respMap = SDKUtil.convertResultStringToMap(result);
        String respData = respMap.get("data");
        String respSign = respMap.get("sign");

        // 验证签名
        boolean valid;
        try {
//            PublicKey publicKey = CertUtil.getPublicKey();
//            valid = CryptoUtil.verifyDigitalSign(respData.getBytes(encoding), Base64.decodeBase64(respSign), CertUtil.getPublicKey(), "SHA1WithRSA");
            valid = CryptoUtil.verifyDigitalSign(respData.getBytes(encoding), Base64.decodeBase64(respSign), CertUtil.getPublicKey(), "SHA1WithRSA");
            if (!valid) {
                LogsUtil.error("", "verify sign fail.");
                return null;
            }
            JSONObject respJson = JSONObject.parseObject(respData);
            if (respJson != null) {
                LogsUtil.info("", "响应码：[" + respJson.getJSONObject("head").getString("respCode") + "]");
                LogsUtil.info("", "响应描述：[" + respJson.getJSONObject("head").getString("respMsg") + "]");
//                LogsUtil.info("","响应报文：\n" + JSONObject.toJSONString(respJson, true));
                LogsUtil.info("", "响应报文：\n" + respJson.toJSONString());
                JSONObject respBody = respJson.getJSONObject("body");
                return respBody;
            } else {
//                logger.error("服务器请求异常！！！");
                LogsUtil.error("", "服务器请求异常.");
                return null;
            }
        } catch (Exception e) {
//            logger.error(e.getMessage());
            LogsUtil.error("", e.getMessage());
            return null;
        }
    }

    /**
     * 构建url格式
     *
     * @param array
     * @return
     */
    public static String httpBuildQuery(Map<String, String> array) {
        String reString = null;
        //遍历数组形成akey=avalue&bkey=bvalue&ckey=cvalue形式的的字符串
        Iterator it = array.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = (Map.Entry) it.next();
            String key = entry.getKey();
            String value = entry.getValue();
            reString += key + "=" + value + "&";
        }
        reString = reString.substring(0, reString.length() - 1);
        //将得到的字符串进行处理得到目标格式的字符串
        try {
            reString = java.net.URLEncoder.encode(reString, "utf-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        reString = reString.replace("%3D", "=").replace("%26", "&");
        return reString;
    }

}
