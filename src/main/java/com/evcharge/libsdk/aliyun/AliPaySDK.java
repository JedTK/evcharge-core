package com.evcharge.libsdk.aliyun;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipayEncrypt;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipaySystemOauthTokenRequest;
import com.alipay.api.request.AlipayUserInfoShareRequest;
import com.alipay.api.response.AlipaySystemOauthTokenResponse;
import com.alipay.api.response.AlipayUserInfoShareResponse;
import com.evcharge.entity.sys.SysGlobalConfigEntity;
import com.evcharge.entity.user.UserEntity;
import com.evcharge.entity.user.UserSourceInfoEntity;
import com.evcharge.entity.user.UserSummaryEntity;
import com.evcharge.enumdata.EUserRegType;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.*;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class AliPaySDK {


    public static String publicKey = "";
    public static String privateKey = "";
    public static String appId = "";

    public AliPaySDK() {
        //alipay.public_key
//        try {
//            publicKey = AlipaySignature.getAlipayPublicKey(ConfigManager.getString("alipay.public_key"));
//            privateKey = SysGlobalConfigEntity.getString("Alipay:MiniApp:Private:Key");
////            privateKey = AlipaySignature.getAlipayPublicKey(ConfigManager.getString("alipay.private_key"));
//            appId = SysGlobalConfigEntity.getString("Alipay:MiniApp:AppID");
//        } catch (Exception e) {
//            System.out.println(e.getMessage());
//        }
    }
//    public static AlipayConfig getAlipayConfig() {
//        AlipayConfig alipayConfig = new AlipayConfig();
//        alipayConfig.setServerUrl("https://openapi.alipay.com/gateway.do");
////        alipayConfig.setAppId(appId);
////        alipayConfig.setPrivateKey(privateKey);
////        alipayConfig.setFormat("json");
////        alipayConfig.setAlipayPublicKey(publicKey);
////        alipayConfig.setCharset("UTF-8");
////        alipayConfig.setSignType("RSA2");
//        //设置网关地址
//        alipayConfig.setServerUrl("https://openapi.alipay.com/gateway.do");
//        //设置应用ID
//        alipayConfig.setAppId(SysGlobalConfigEntity.getString("Alipay:MiniApp:AppID"));
//        //设置应用私钥
//        alipayConfig.setPrivateKey(SysGlobalConfigEntity.getString("Alipay:MiniApp:Private:Key"));
//        //设置请求格式，固定值json
//        alipayConfig.setFormat("json");
//        //设置字符集
//        alipayConfig.setCharset("UTF-8");
//        //设置签名类型
//        alipayConfig.setSignType("RSA2");
//        //设置应用公钥证书路径
//        alipayConfig.setAppCertPath(ConfigManager.getString("alipay.mini_app.public_key"));
//        //设置支付宝公钥证书路径
//        alipayConfig.setAlipayPublicCertPath(ConfigManager.getString("alipay.public_key"));
//        //设置支付宝根证书路径
//        alipayConfig.setRootCertPath(ConfigManager.getString("alipay.root_key"));
//
//        return alipayConfig;
//    }
    /**
     * 获取openid
     *
     * @param authCode
     * @return
     */
    public SyncResult getAccessToken(String authCode) {

        try {
            // 初始化SDK
            AlipayClient alipayClient = new DefaultAlipayClient(AliPayConfig.getConfig());
            // 构造请求参数以调用接口
            AlipaySystemOauthTokenRequest request = new AlipaySystemOauthTokenRequest();

            // 设置刷新令牌
            //request.setRefreshToken("201208134b203fe6c11548bcabd8da5bb087a83b");

            // 设置授权码
            request.setCode(authCode);

            // 设置授权方式
            request.setGrantType("authorization_code");

            AlipaySystemOauthTokenResponse response = alipayClient.certificateExecute(request);
            if (response.isSuccess()) {
                System.out.println("调用成功");
                String body = response.getBody();
                JSONObject json = JSONObject.parse(body);
                JSONObject authInfo = json.getJSONObject("alipay_system_oauth_token_response");
                String token = authInfo.getString("access_token");
                String openId = authInfo.getString("open_id");
//                DataService.getMainCache().set("AliPay:access_token",token,1800*1000);
//                return openId;
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("open_id", openId);
                data.put("access_token", token);

                return new SyncResult(0, "success", data);
            } else {
                System.out.println("调用失败");
                // sdk版本是"4.38.0.ALL"及以上,可以参考下面的示例获取诊断链接
//                 String diagnosisUrl = DiagnosisUtils.getDiagnosisUrl(response);
//                 System.out.println(diagnosisUrl);
                LogsUtil.error("alipay", String.format("初始化assess_token失败，失败原因：%s", response.getMsg()));
                return new SyncResult(1, response.getMsg());
            }
        } catch (Exception e) {
            LogsUtil.error("alipay", String.format("初始化assess_token失败，失败原因：%s", e.getMessage()));
            return new SyncResult(1, e.getMessage());
        }
    }
    /**
     * 获取用户信息
     *
     * @param accessToken
     * @return
     */
    public SyncResult getUserInfo(String accessToken) {
        try {
            // 初始化SDK
            AlipayClient alipayClient = new DefaultAlipayClient(AliPayConfig.getConfig());
            AlipayUserInfoShareRequest request = new AlipayUserInfoShareRequest();
            AlipayUserInfoShareResponse response = alipayClient.certificateExecute(request, accessToken);
            if (response.isSuccess()) {
                System.out.println("调用成功");
                String body = response.getBody();
                JSONObject json = JSONObject.parse(body);
//                {
//                    "alipay_user_info_share_response": {
//                    "code": "10000",
//                            "msg": "Success",
//                            "user_id": "2088102104794936",
//                            "avatar": "http://tfsimg.alipay.com/images/partner/T1uIxXXbpXXXXXXXX",
//                            "city": "安庆",
//                            "nick_name": "支付宝小二",
//                            "province": "安徽省",
//                            "gender": "F"
//                },
//                    "sign": "ERITJKEIJKJHKKKKKKKHJEREEEEEEEEEEE"
//                }
                JSONObject info = json.getJSONObject("alipay_user_info_share_response");
                return new SyncResult(0, "success", info);
            } else {
                System.out.println("调用失败");
                // sdk版本是"4.38.0.ALL"及以上,可以参考下面的示例获取诊断链接
//                 String diagnosisUrl = DiagnosisUtils.getDiagnosisUrl(response);
//                 System.out.println(diagnosisUrl);
                LogsUtil.error("alipay", String.format("初始化assess_token失败，失败原因：%s", response.getMsg()));
                return new SyncResult(1, response.getMsg());
            }
        } catch (Exception e) {
            LogsUtil.error("alipay", String.format("初始化assess_token失败，失败原因：%s", e.getMessage()));
            return new SyncResult(1, e.getMessage());
        }
    }
    /**
     * 创建用户
     *
     * @param accessToken
     * @return
     */
    public SyncResult createUser(String openId, String accessToken) {
        return createUser(openId, accessToken, null, null, null, 0);
    }
    public SyncResult createUser(String openId, String accessToken, String deviceCode, String devicePort, String deviceIndex, long csId) {
        SyncResult result = getUserInfo(accessToken);
        if (result.code != 0) return new SyncResult(result.code, result.msg);
        Map<String, Object> userInfo = (Map<String, Object>) result.data;
        String nickname="";
        //用户信息
        if(StringUtils.hasLength(MapUtil.getString(userInfo,"nick_name"))){
            nickname=MapUtil.getString(userInfo,"nick_name");
        }else{
            nickname=String.format("元气充%s",common.randomInt(100000,999999));
        }
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("nickname", nickname);
        user.put("avatar", MapUtil.getString(userInfo,"avatar_url"));
        user.put("create_time", TimeUtil.getTimestamp());
        user.put("status", 0);
        user.put("reg_id", EUserRegType.alipayId);
        user.put("device_code", deviceCode);
        user.put("device_port", devicePort);
        user.put("device_index", deviceIndex);
        user.put("cs_id", csId);
        user.put("create_ip", HttpRequestUtil.getIP());
        Map<String, Object> data = new LinkedHashMap<>();

        //  {
//                    "code": "10000",
//                            "msg": "Success",
//                            "user_id": "2088102104794936",
//                            "avatar": "http://tfsimg.alipay.com/images/partner/T1uIxXXbpXXXXXXXX",
//                            "city": "安庆",
//                            "nick_name": "支付宝小二",
//                            "province": "安徽省",
//                            "gender": "F"
//                },
        data.put("reg_id", EUserRegType.alipayId);
        data.put("open_id", openId);
        data.put("nickname", MapUtil.getString(userInfo, "nick_name"));
        if ("F".equals(MapUtil.getString(userInfo, "gender"))) {
            data.put("gender", 1);
        }

        data.put("city", MapUtil.getString(userInfo, "city"));
        data.put("province", MapUtil.getString(userInfo, "province"));
        data.put("create_time", TimeUtil.getTimestamp());
        data.put("device_code", deviceCode);
        data.put("device_port", devicePort);
        data.put("device_index", deviceIndex);
        //检查openid
        if (UserSourceInfoEntity.getInstance().where("open_id", openId).count() > 0) {
            return new SyncResult(1, "该openid已被注册");
        }

        SyncResult syncResult = DataService.getMainDB().beginTransaction(connection -> {
            long uid = DataService.getMainDB().name("User").insertGetIdTransaction(connection, user);
            if (uid == 0) return new SyncResult(1, "创建用户失败，请稍后再试!");
            //2024-06-06 新增用户注册程序
            UserEntity.getInstance().updateRegisterInfo(uid,deviceCode);
            //写入用户信息表
            data.put("uid", uid);
            if (DataService.getMainDB().name("UserSourceInfo").insertTransaction(connection, data) == 0) {
                return new SyncResult(1, "新增失败");
            }
            //region 2024-02-14添加
            UserSummaryEntity.getInstance().initSummary(uid);
            //endregion
            //判断是否存在分享用户数据
            return new SyncResult(0, "", new HashMap<>() {{
                put("uid", uid);
            }});
        });
        if (syncResult.code != 0) return new SyncResult(syncResult.code, result.msg);
        long uid = MapUtil.getLong((Map<String, Object>) syncResult.data, "uid");
        String token = String.valueOf(UserEntity.createToken(uid));
        UserEntity userEntity = new UserEntity();
        userEntity = userEntity.findUserByOpenID(openId);
        //用户注册成功，返回token，写入redis
        DataService.getMainCache().setObj(String.format("User:Info:%s",  uid), userEntity, 86400 * 1000 * 7);
//            userEntity.setUserCache(uid);
        DataService.getMainCache().set(String.format("User:Token:%s", token), uid, 86400 * 1000 * 7);
        Map<String, Object> cbData = new LinkedHashMap<>();
        cbData.put("token", token);
        cbData.put("uid", uid);
        return new SyncResult(0, "授权成功", cbData);
    }
    /**
     * 获取手机号
     *
     * @return
     */
    public SyncResult getPhoneInfo(String response) {
        //   String response = "小程序前端返回的加密信息";
//        //1. 获取验签和解密所需要的参数
        JSONObject openapiResult = JSON.parseObject(response);
        String signType = "RSA2";
        String charset = "UTF-8";
        String encryptType = "AES";
        String sign = openapiResult.getString("sign");
        String content = openapiResult.getString("response");
        String decryptKey = SysGlobalConfigEntity.getString("Alipay:MiniApp:Verify:Key");
        //判断是否为加密内容
        boolean isDataEncrypted = !content.startsWith("{");
        boolean signCheckPass = false;
        System.out.println("response"+response);
        //2. 验签
        boolean verify = verifyData(response);
        if (!verify) {
            return new SyncResult(1, "验签失败");
        }
        //3. 解密
        String plainData = null;
        if (isDataEncrypted) {
            try {
                plainData = AlipayEncrypt.decryptContent(content, encryptType, decryptKey, charset);
                System.out.println("plainData="+plainData);
            } catch (AlipayApiException e) {
                //解密异常, 记录日志
                return new SyncResult(1, "解密异常");
            }
        } else {
            plainData = content;
        }
        return new SyncResult(0, "success", plainData);
    }
    public boolean verifyData(String response) {
//        String resultInfo =convertJsonToQueryString(response);
//                //回调的待验签字符串
//        //签名方式
//        String sign_type = "RSA2";
//        //对待签名字符串数据通过&进行拆分
//        String[] temp = resultInfo.split("&");
//        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
//        //把拆分数据放在map集合内
//        for (int i = 0; i < temp.length; i++) {
//            String[] arr = temp[i].split("=", 2); //通过"="号分割成2个数据
//            String[] tempAagin = new String[arr.length]; //再开辟一个数组用来接收分割后的数据
//            for (int j = 0; j < arr.length; j++) {
//                tempAagin[j] = arr[j];
//            }
//            map.put(tempAagin[0], tempAagin[1]);
//        }
//        System.out.println(map);
        //验签方法
//        try {
//            //编码格式
//            String charset = "utf-8";
//            //支付宝公钥证书
//            String alipayPublicCertPath = ConfigManager.getString("alipay.public_key");;
////            String alipayPublicCertPath = AlipaySignature.getAlipayPublicKey(ConfigManager.getString("alipay.public_key"));;
//            // TODO 验签成功后
//            return AlipaySignature.rsaCertCheckV1(map, alipayPublicCertPath, charset, sign_type);
//        } catch (Exception e) {
//            LogsUtil.error(this.getClass().getName(), e.getMessage());
//            return false;
//        }
        //1. 获取验签和解密所需要的参数
        JSONObject openapiResult = JSON.parseObject(response);
        String signType = "RSA2";
        String charset = "UTF-8";
        String sign = openapiResult.getString("sign");
        String content = openapiResult.getString("response");
        if(content==null){
            if(openapiResult.containsKey("sign")){
                openapiResult.remove("sign");
            }
            if(openapiResult.containsKey("sign_type")){
                openapiResult.remove("sign_type");
            }
            content=sortJsonObject(openapiResult).toJSONString();
        }
        //判断是否为加密内容
        boolean isDataEncrypted = !content.startsWith("{");
        boolean signCheckPass = false;
        //2. 验签
        String signContent = content;
//        支付宝公钥
        if (isDataEncrypted) {
            signContent = "\"" + signContent + "\"";
        }
        try {
            String signVeriKey = AlipaySignature.getAlipayPublicKey(AliPayConfig.getAlipayPublicCertPath());
            signCheckPass = AlipaySignature.rsaCheck(signContent, sign, signVeriKey, charset, signType);
        } catch (AlipayApiException e) {
            // 验签异常, 日志
            LogsUtil.error(this.getClass().getName(),e.getMessage());
        }
        if (!signCheckPass) {
            //验签不通过（异常或者报文被篡改），终止流程（不需要做解密）
            return false;
        } else {
            System.out.println("验签成功");
            return true;
        }
    }
//    public static String convertJsonToQueryString(String json) {
//        JSONObject jsonObject = JSONObject.parseObject(json);
//        StringBuilder queryString = new StringBuilder();
//
//        for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
//            if (queryString.length() > 0) {
//                queryString.append("&");
//            }
//            queryString.append(entry.getKey())
//                    .append("=")
//                    .append(JSONObject.toJSONString(entry.getValue()).replaceAll("^\"|\"$", ""));
//        }
//
//        return queryString.toString();
//    }

    /**
     * 获取post请求路由参数
     * @param request
     * @return json——object字符串
     */
    public String getUrlParams(HttpServletRequest request){
        try {
            JSONObject jsonObject = new JSONObject();

            // 获取所有请求参数的名称
            Enumeration<String> parameterNames = request.getParameterNames();

            while (parameterNames.hasMoreElements()) {
                String paramName = parameterNames.nextElement();
                // 对于每个参数，获取其值并放入JSONObject
                String paramValue = request.getParameter(paramName);
                jsonObject.put(paramName, paramValue);
            }

            // 将JSONObject转换为字符串
            return jsonObject.toJSONString();
        }catch (Exception e){
            LogsUtil.error(this.getClass().getName(),e.getMessage());
            return "";
        }
    }

    public  JSONObject sortJsonObject(JSONObject jsonObject) {
        // 提取所有键
        List<String> keys = new ArrayList<>(jsonObject.keySet());
        // 按字典顺序对键进行排序
        Collections.sort(keys);

        // 创建一个新的 JSONObject，按照排序后的键顺序添加键值对
        JSONObject sortedJsonObject = new JSONObject();
        for (String key : keys) {
            sortedJsonObject.put(key, jsonObject.get(key));
        }

        // 返回排序后的 JSONObject 字符串
        return sortedJsonObject;
    }
}
