package com.evcharge.strategy.ConsumeCenter.Payment.PaymentCallback;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.consumecenter.order.ConsumeOrdersEntity;
import com.evcharge.entity.payment.PaymentCallbackLogEntity;
import com.evcharge.entity.user.UserIntegralDetailEntity;
import com.evcharge.enumdata.EUserIntegralType;
import com.evcharge.libsdk.Hmpay.HmPaymentSDK;
import com.evcharge.service.ConsumeCenter.ConsumeOrdersService;
import com.evcharge.strategy.ConsumeCenter.Payment.Fulfillment.FulfillmentService;
import com.evcharge.strategy.ConsumeCenter.Payment.Fulfillment.FulfillmentServiceFactory;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.HttpRequestUtil;
import com.xyzs.utils.LogsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

@Service
public class HmPayMPCallbackServiceImpl implements PaymentCallbackService {

    @Autowired
    private FulfillmentServiceFactory fulfillmentServiceFactory;
    @Autowired
    private ConsumeOrdersService consumeOrdersService;

    @Override
    public String getChannelCode() {
        return "hmpay_mp"; // 支付宝回调渠道编码
    }

    @Override
    public String process(HttpServletRequest request) {
        String outOrderNo = HttpRequestUtil.getString("out_order_no"); //商户订单号
        System.out.println(request.getQueryString());
        LogsUtil.info("", request.getQueryString());
        String content = request.getQueryString();
        try {
            HmPaymentSDK hmPaymentSDK = new HmPaymentSDK();
            SyncResult r = hmPaymentSDK.checkNotifyContent(content);
            if (r.code != 0) { //验签失败
                LogsUtil.error("", r.msg);
                return "error";
            }
            //主动查询
            SyncResult query = hmPaymentSDK.query(outOrderNo);
            if (query.code != 0) {
                LogsUtil.info("", "[支付回调],河马支付回调失败，失败原因=%s", query.msg);
                return "error";
            }
            JSONObject body = (JSONObject) query.data;
            //添加到支付记录
            PaymentCallbackLogEntity.getInstance().addLog(outOrderNo, body.toJSONString());
            /**
             * TODO 更新订单信息
             */


            //
            ConsumeOrdersEntity consumeOrdersEntity = consumeOrdersService.findByOrderSn(outOrderNo);
            if(consumeOrdersEntity == null) {
                return "error";
            }
            SyncResult result= consumeOrdersService.paySuccess(
                    outOrderNo
                    ,body.getString("bank_order_no")
                    ,body.getBigDecimal("total_amount")
                    ,body.getBigDecimal("settle_amount")
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
            LogsUtil.error("", "[支付回调],河马支付回调失败，失败原因=%s", e.getMessage());
            LogsUtil.error(e, "", "[支付回调],河马支付回调失败");
            return "error";
        }
    }
}
