package com.evcharge.service.Wechat;

import com.alibaba.fastjson2.JSONObject;
import com.evcharge.entity.user.UserEntity;
import com.evcharge.libsdk.wechat.WechatSDK;
import com.evcharge.service.User.UserService;
import com.xyzs.entity.DataService;
import com.xyzs.entity.SyncResult;
import com.xyzs.utils.LogsUtil;
import com.xyzs.utils.common;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WechatService {

    public static void  setUserEntityCache(String token,UserEntity userEntity){
        UserService.setUserEntityCache(token, userEntity);

    }
    /**
     * 解密微信加密数据。
     * @param encryptedData 加密数据
     * @param sessionKey 会话密钥
     * @param iv 加密向量
     * @return 解密后的用户信息JSONObject
     * @throws RuntimeException 如果解密失败
     */
    public JSONObject decryptWechatData(String encryptedData, String sessionKey, String iv) {
      try {
          if (!StringUtils.hasLength(sessionKey)) {
              throw new RuntimeException("session_id已过期，请重新刷新");
          }

          WechatSDK wechatSDK = new WechatSDK();
          SyncResult result = wechatSDK.decryptData(encryptedData, sessionKey, iv);

          if (result.getCode() != 0) {
              throw new RuntimeException("数据解密失败: " + result.getMsg());
          }

          return (JSONObject) result.getData();
      }catch (Exception e){
          e.printStackTrace();
          LogsUtil.error(e, "", "【调试信息】%s.%s 通过API读取用户登录信息失败", this.getClass().getPackageName(), this.getClass().getName());
          return common.apicb(98, "微信授权失败，请稍后再试!");
      }
    }

    /**
     * 根据js_code获取微信小程序的openid和unionid。
     * @param jsCode 微信小程序临时登录凭证
     * @return 包含openid和unionid的JSONObject
     * @throws RuntimeException 如果获取失败
     */
    public JSONObject getSessionKeyAndOpenId(String jsCode) {
        if (!StringUtils.hasLength(jsCode)) {
            throw new IllegalArgumentException("js_code不能为空");
        }

        WechatSDK wechatSDK = new WechatSDK();
        SyncResult result = wechatSDK.updateSessionKey(jsCode);

        if (result.getCode() != 0) {
            throw new RuntimeException("获取session_key失败: " + result.getMsg());
        }

        return (JSONObject) result.getData();
    }


}
