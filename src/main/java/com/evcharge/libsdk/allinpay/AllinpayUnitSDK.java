package com.evcharge.libsdk.allinpay;

import com.alibaba.fastjson2.JSONObject;
//import com.allinpay.sdk.OpenClient;
//import com.allinpay.sdk.bean.BizParameter;
//import com.allinpay.sdk.bean.OpenConfig;
//import com.allinpay.sdk.bean.OpenResponse;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.TimeUtil;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

/**
 * 通联支付中间插件SDK，此类只是整合到平台的中间类，实际是调用了com.allinpay.sdk  jar文件实现
 */
public class AllinpayUnitSDK {

    private final static String APIURL = "http://test.allinpay.com/open/gateway";
    private final static String APPID = SysGlobalConfigEntity.getString("allinpay:appid");
    private final static String SECRETKEY = SysGlobalConfigEntity.getString("allinpay:secretKey");
    private final static String PRIVATE_KEY_PATH = SysGlobalConfigEntity.getString("allinpay:privateKey:path");
    private final static String PRIVATE_CERT_PASSWORD = SysGlobalConfigEntity.getString("allinpay:privateKey:password");
    private final static String PUBLIC_KEY_PATH = SysGlobalConfigEntity.getString("allinpay:publicKey:path");
//    private final static OpenConfig OPEN_CONFIG = new OpenConfig(APIURL, APPID, SECRETKEY, PRIVATE_KEY_PATH, PRIVATE_CERT_PASSWORD, PUBLIC_KEY_PATH);

    public static AllinpayUnitSDK getInstance() {
        return new AllinpayUnitSDK();
    }

    public SyncResult createPaymentTest() {
//        final OpenClient client = new OpenClient(OPEN_CONFIG);
//
//        String orderExpireDatetime = TimeUtil.toTimeString(TimeUtil.getTimestamp() + 1 * 3600 * 1000);
//        BizParameter param = new BizParameter();
//        // 实名付（单笔）
//        final HashMap<String, Object> realnamePay = new HashMap<>();
//        realnamePay.put("bankCardNo", client.encrypt("6228480402637874214"));
//        realnamePay.put("amount", 100000);
//
//        // 分组装支付方式
//        final HashMap<String, Object> payMethod = new HashMap<>();
//        payMethod.put("REALNAMEPAY", realnamePay);
//
//        param.put("bizUserId", "test0002");
//        param.put("bizOrderNo", System.currentTimeMillis() + "cz");
//        param.put("accountSetNo", "200001");
//        param.put("amount", 100000L);
//        param.put("fee", 0L);
//        param.put("validateType", 0L);
//        param.put("frontUrl", "http://ceshi.allinpay.com");
//        param.put("backUrl", "http://ceshi.allinpay.com");
//        param.put("orderExpireDatetime", orderExpireDatetime);
//        param.put("payMethod", JSONObject.toJSON(payMethod));
//        param.put("goodsName", "computer");
//        param.put("industryCode", "1010");
//        param.put("industryName", "保险代理");
//        param.put("source", 2L);
//        param.put("summary", "deposit");
//        param.put("extendInfo", "this is extendInfo");
//        try {
//            final OpenResponse response = client.execute("allinpay.yunst.orderService.depositApply", param);
//            if ("OK".equals(response.getSubCode())) {
//                System.out.println(response.getData());
//            }
//        } catch (final Exception e) {
//            e.printStackTrace();
//        }
        return new SyncResult(1, "");
    }
}
