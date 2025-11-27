package com.evcharge.strategy.ConsumeCenter.Payment.PaymentCallback;

import com.alibaba.fastjson2.JSONObject;
import com.alipay.api.internal.util.AlipaySignature;
import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;
import com.evcharge.entity.payment.PaymentCallbackLogEntity;
import com.evcharge.entity.user.UserIntegralDetailEntity;
import com.evcharge.enumdata.EUserIntegralType;
import com.evcharge.libsdk.aliyun.AliPayConfig;
import com.evcharge.libsdk.aliyun.AliPaymentSDK;
import com.evcharge.service.ConsumeCenter.ConsumeOrdersService;
import com.evcharge.strategy.ConsumeCenter.Payment.Fulfillment.FulfillmentService;
import com.evcharge.strategy.ConsumeCenter.Payment.Fulfillment.FulfillmentServiceFactory;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.HttpRequestUtil;
import com.xyzs.utils.LogsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AlipayMPCallbackServiceImpl implements PaymentCallbackService {
    @Autowired
    private FulfillmentServiceFactory fulfillmentServiceFactory;
    @Autowired
    private ConsumeOrdersService consumeOrdersService;

    @Override
    public String getChannelCode() {
        return "alipay_mp"; // 支付宝回调渠道编码
    }



    @Override
    public String process(HttpServletRequest request) {
        String outOrderNo = HttpRequestUtil.getString("out_trade_no"); //商户订单号
        if (!StringUtils.hasLength(outOrderNo)) {
            System.out.println("error");
            return "";
        }
        System.out.println("outOrderNo=" + outOrderNo);
        try {
            AliPaymentSDK aliPaymentSDK = new AliPaymentSDK();
            //获取支付宝POST过来反馈信息
            Map<String, String> params = new LinkedHashMap<>();
            Map<String, String[]> requestParams = request.getParameterMap();
            for (String name : requestParams.keySet()) {
                String[] values = (String[]) requestParams.get(name);
                String valueStr = "";
                for (int i = 0; i < values.length; i++) {
                    valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
                }
                //乱码解决，这段代码在出现乱码时使用。
                //valueStr = new String(valueStr.getBytes("ISO-8859-1"), "utf-8");
                params.put(name, valueStr);
            }
            String charset = "UTF-8";
            System.out.println(params.toString());
            boolean flag = AlipaySignature.rsaCertCheckV1(params, AliPayConfig.getAlipayPublicCertPath(), charset, "RSA2");
            if (!flag) {
                LogsUtil.error(this.getClass().getName(), "验签失败");
                System.out.println("error");
                return "";
            }
            //主动查询
            SyncResult query = aliPaymentSDK.query(outOrderNo);
            if (query.code != 0) {
                LogsUtil.info(this.getClass().getName(), "[支付回调],支付宝支付回调失败，失败原因=%s", query.msg);
                System.out.println("error");
            }
            LogsUtil.info(this.getClass().getName(), "[支付回调],验签成功！");
            JSONObject body = (JSONObject) query.data;
            /**
             * 添加到支付记录
             */
            PaymentCallbackLogEntity.getInstance().addLog(outOrderNo, body.toJSONString());
            /**
             * TODO 整合到订单系统
             */
            ConsumeOrdersEntity consumeOrdersEntity = consumeOrdersService.findByOrderSn(outOrderNo);
            if(consumeOrdersEntity == null) {
                return "error";
            }

            SyncResult result= consumeOrdersService.paySuccess(
                    outOrderNo
                    ,body.getString("trade_no")
                    ,body.getBigDecimal("total_amount")
                    ,body.getBigDecimal("total_amount")
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
            return "success";

        } catch (Exception e) {
            LogsUtil.error(this.getClass().getName(), "[支付回调],支付宝支付回调失败，失败原因=%s", e.getMessage());
            LogsUtil.error(e, this.getClass().getName(), "[支付回调],支付宝支付回调失败");
            System.out.println("error");
            return "";
        }
    }

}
