package com.evcharge.libsdk.aliyun;

import com.alibaba.fastjson2.JSONObject;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeCreateModel;
import com.alipay.api.domain.ExtendParams;
import com.alipay.api.request.AlipayTradeCreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeCreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.evcharge.entity.user.UserEntity;
import com.evcharge.enumdata.EUserRegType;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.ConfigManager;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.MapUtil;

import java.util.Map;

public class AliPaymentSDK {

    /**
     * 创建订单
     *
     * @param uid 用户uid
     * @param orderSn 订单编号
     * @param describe 描述
     * @param totalPrice 总价
     * @param callBackUrl 回调地址
     * @param param 额外参数
     * @return
     */
    public SyncResult create(long uid, String orderSn, String describe, double totalPrice, String callBackUrl, Map<String, Object> param) {
        try {
            Map<String, Object> userInfo = UserEntity.getInstance().findSourceUserByID(uid, EUserRegType.alipayId);
            if (userInfo.isEmpty()) return new SyncResult(1, "用户不存在");
            String buyerId = MapUtil.getString(userInfo, "open_id");
            AlipayClient alipayClient = new DefaultAlipayClient(AliPayConfig.getConfig());
            // 构造请求参数以调用接口
            AlipayTradeCreateRequest request = new AlipayTradeCreateRequest();
            //设置回传地址
            String notifyUrl = String.format("%s%s", ConfigManager.getString("consume.notify_url"), callBackUrl);
            System.out.println("notifyUrl=" + notifyUrl);
            request.setNotifyUrl(notifyUrl);
            AlipayTradeCreateModel model = new AlipayTradeCreateModel();
            // 设置商户订单号
            model.setOutTradeNo(orderSn);
            // 设置产品码
            model.setProductCode("JSAPI_PAY");
            // 设置小程序支付中
            model.setOpAppId(AliPayConfig.getAppId());
            // 设置订单总金额
            model.setTotalAmount(String.format("%s", totalPrice));
            // 设置订单标题
            model.setSubject(describe);
            // 设置订单附加信息
            model.setBody(describe);
            // 设置买家支付宝用户openid
            model.setBuyerOpenId(buyerId);
            // 设置商户门店编号
            //model.setStoreId("NJ_001");
            request.setBizModel(model);
            System.out.println("request=" + request);
            AlipayTradeCreateResponse response = alipayClient.certificateExecute(request);
            if (response.isSuccess()) {
                System.out.println("调用成功");
                System.out.println(response.getBody());
                String body = response.getBody();
                JSONObject json = JSONObject.parse(body);
                JSONObject info = json.getJSONObject("alipay_trade_create_response");

//                {
//                    "alipay_trade_create_response": {
//                    "code": "10000",
//                            "msg": "Success",
//                            "trade_no": "2015042321001004720200028594",
//                            "out_trade_no": "20150423001001"
//                },
//                    "sign": "ERITJKEIJKJHKKKKKKKHJEREEEEEEEEEEE"
//                }
                return new SyncResult(0, "success", info);
            } else {
                System.out.println("调用失败");
                // sdk版本是"4.38.0.ALL"及以上,可以参考下面的示例获取诊断链接
                // String diagnosisUrl = DiagnosisUtils.getDiagnosisUrl(response);
                // System.out.println(diagnosisUrl);
                return new SyncResult(1, "发起支付失败");
            }
        } catch (Exception e) {
            LogsUtil.error(this.getClass().getName(), e.getMessage());
            return new SyncResult(1, e.getMessage());
        }
    }

    /**
     * 查询订单
     *
     * @param orderSn
     * @return
     */
    public SyncResult query(String orderSn) {
        try {
            AlipayClient alipayClient = new DefaultAlipayClient(AliPayConfig.getConfig());
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", orderSn);
            request.setBizContent(bizContent.toString());
            AlipayTradeQueryResponse response = alipayClient.certificateExecute(request);
            if (response.isSuccess()) {
                System.out.println("调用成功");
                String body = response.getBody();
                JSONObject json = JSONObject.parse(body);
                JSONObject info = json.getJSONObject("alipay_trade_query_response");
                return new SyncResult(0, "success", info);
            } else {
                System.out.println("调用失败");
                return new SyncResult(1, "调用失败");
            }
        } catch (Exception e) {
            LogsUtil.error(this.getClass().getName(), e.getMessage());
            return new SyncResult(1, e.getMessage());
        }
    }

    /**
     * 退款接口
     *
     * @param orderSn 商户订单号
     * @param refundOrderSn 退款订单号
     * @param totalAmount 退款金额
     * @param reason 退款原因
     * @return
     */
    public SyncResult refund(String orderSn, String refundOrderSn, double totalAmount, String reason) {

        try {
            AlipayClient alipayClient = new DefaultAlipayClient(AliPayConfig.getConfig());
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", orderSn);
            bizContent.put("refund_amount", Math.abs(totalAmount));
            bizContent.put("out_request_no", refundOrderSn);
            bizContent.put("refund_reason", reason);
            //// 返回参数选项，按需传入
            //JSONArray queryOptions = new JSONArray();
            //queryOptions.add("refund_detail_item_list");
            //bizContent.put("query_options", queryOptions);

            request.setBizContent(bizContent.toString());
            AlipayTradeRefundResponse response = alipayClient.certificateExecute(request);
            if (response.isSuccess()) {
                String body = response.getBody();
                JSONObject json = JSONObject.parse(body);
                JSONObject info = json.getJSONObject("alipay_trade_refund_response");

                if(!"Success".equals(info.getString("msg"))){
                    return new SyncResult(1,info.getString("msg"));
                }
                return new SyncResult(0, "success", info);
            } else {
                System.out.println("调用失败");
                return new SyncResult(1, "调用失败");
            }

        } catch (Exception e) {
            LogsUtil.error(this.getClass().getName(), e.getMessage());
            return new SyncResult(1, e.getMessage());
        }
    }


}
