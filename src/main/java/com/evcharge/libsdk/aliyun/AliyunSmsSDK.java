package com.evcharge.libsdk.aliyun;

import com.alibaba.fastjson2.JSONObject;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.Common;
import com.xyzs.utils.LogsUtil;

import java.util.Arrays;

/**
 * 阿里云短信验证码
 */
public class AliyunSmsSDK {
    //region 属性
    /**
     * 授权key
     */
    private String accessKeyId;
    /**
     * 授权密钥
     */
    private String accessKeySecret;
    /**
     * 服务地址
     * Endpoint 请参考 https://api.aliyun.com/product/Dysmsapi
     */
    private String endpoint = "dysmsapi.aliyuncs.com";

    /**
     * 设置RAM授权key
     *
     * @param accessKeyId
     * @return
     */
    public AliyunSmsSDK setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
        return this;
    }

    /**
     * 设置RAM授权密钥
     *
     * @param accessKeySecret
     * @return
     */
    public AliyunSmsSDK setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
        return this;
    }

    /**
     * 设置服务地址，一般情况下不需要设置，使用国际短信才需要去设置
     *
     * @param endpoint
     * @return
     */
    public AliyunSmsSDK setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }
    //endregion

    private static AliyunSmsSDK _this;

    public static AliyunSmsSDK getInstance(String accessKeyId, String accessKeySecret) {
        if (_this == null) {
            _this = new AliyunSmsSDK(accessKeyId, accessKeySecret);
        }
        return _this;
    }

    public AliyunSmsSDK(String accessKeyId, String accessKeySecret) {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
    }

    /**
     * 获得一个短信客户端操作对象
     *
     * @return
     * @throws Exception
     */
    public Client getClient() throws Exception {
        Config config = new Config()
                .setAccessKeyId(this.accessKeyId)
                .setAccessKeySecret(this.accessKeySecret);
        config.endpoint = this.endpoint;
        return new Client(config);
    }

    /**
     * 发送短信验证码
     *
     * @param phoneNumber    手机号码（多个手机号码用英文逗号分隔）
     * @param signName       短信签名
     * @param templateCode   短信模板code
     * @param templateParams 短信模板中的参数和参数值
     * @return
     */
    public boolean send(String phoneNumber, String signName, String templateCode, JSONObject templateParams) {
        try {
            Client client = getClient();
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phoneNumber)
                    .setSignName(signName)
                    .setTemplateCode(templateCode)
                    .setTemplateParam(templateParams.toJSONString());

            // 复制代码运行请自行打印 API 的返回值
            SendSmsResponse response = client.sendSmsWithOptions(request, new com.aliyun.teautil.models.RuntimeOptions());
            LogsUtil.info(this.getClass().getSimpleName(), "发送验证码日志：%s", JSONObject.from(response));
            return true;
        } catch (TeaException error) {
            // 如有需要，请打印 error
            LogsUtil.error(this.getClass().getSimpleName(), "发送验证码失败:%s", Common.assertAsString(error.message));
        } catch (Exception _error) {
            TeaException error = new TeaException(_error.getMessage(), _error);
            // 如有需要，请打印 error
            Common.assertAsString(error.message);
            LogsUtil.error(this.getClass().getSimpleName(), "发送验证码失败:%s", Common.assertAsString(error.message));
        }
        return false;
    }
}
