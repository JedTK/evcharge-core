package com.evcharge.libsdk.aliyun;

import com.alipay.api.AlipayConfig;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.xyzs.utils.ConfigManager;

public class AliPayConfig {

    //设置网关地址
    private static String serverUrl = "https://openapi.alipay.com/gateway.do";
    //设置应用ID
    private static String appId = SysGlobalConfigEntity.getString("Alipay:MiniApp:AppID");
    //设置应用私钥
    private static String privateKey=SysGlobalConfigEntity.getString("Alipay:MiniApp:Private:Key");

    private static String appCertPath=ConfigManager.getString("alipay.mini_app.public_key").replace("user.dir.path:", System.getProperty("user.dir"));
    private static String alipayPublicCertPath=ConfigManager.getString("alipay.public_key").replace("user.dir.path:", System.getProperty("user.dir"));
    private static String rootCertPath=ConfigManager.getString("alipay.root_key").replace("user.dir.path:", System.getProperty("user.dir"));


    public static String getAppId(){
        return appId;
    }
    public static String getAlipayPublicCertPath(){
        return alipayPublicCertPath;
    }

    public static AlipayConfig getConfig() {
        AlipayConfig alipayConfig = new AlipayConfig();
        //设置网关地址
        alipayConfig.setServerUrl(serverUrl);
        //设置应用ID
        alipayConfig.setAppId(appId);
        //设置应用私钥
        alipayConfig.setPrivateKey(privateKey);
        //设置请求格式，固定值json
        alipayConfig.setFormat("json");
        //设置字符集
        alipayConfig.setCharset("UTF-8");
        //设置签名类型
        alipayConfig.setSignType("RSA2");
        //设置应用公钥证书路径
        alipayConfig.setAppCertPath(appCertPath);
        //设置支付宝公钥证书路径
        System.out.println(alipayPublicCertPath);
        alipayConfig.setAlipayPublicCertPath(alipayPublicCertPath);
        //设置支付宝根证书路径
        alipayConfig.setRootCertPath(rootCertPath);

        return alipayConfig;
    }
}
