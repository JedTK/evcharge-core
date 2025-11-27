package com.evcharge.libsdk.aliyun;

import com.aliyun.ocr_api20210707.models.RecognizeInvoiceResponse;
import com.aliyun.ocr_api20210707.models.RecognizeInvoiceResponseBody;
import com.evcharge.entity.sys.SysGlobalConfigEntity;

public class AliYunOcrSDK {


    // 阿里云访问密钥ID
    private String accessKeyId;
    // 阿里云访问密钥密钥
    private String accessKeySecret;

    public AliYunOcrSDK() {
        String accessKeyId = SysGlobalConfigEntity.getString("Aliyun.OSS.File.AccessKeyId");
        String accessKeySecret = SysGlobalConfigEntity.getString("Aliyun.OSS.File.AccessKeySecret");

        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;

    }


    public AliYunOcrSDK setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
        return this;
    }


    public AliYunOcrSDK setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
        return this;
    }

    /**
     * <b>description</b> :
     * <p>使用AK&amp;SK初始化账号Client</p>
     *
     * @return Client
     * @throws Exception
     */
    private com.aliyun.ocr_api20210707.Client createClient() throws Exception {

        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config()
                // 必填，请确保代码运行环境设置了环境变量 ALIBABA_CLOUD_ACCESS_KEY_ID。
                .setAccessKeyId(this.accessKeyId)
                // 必填，请确保代码运行环境设置了环境变量 ALIBABA_CLOUD_ACCESS_KEY_SECRET。
                .setAccessKeySecret(this.accessKeySecret);
        // Endpoint 请参考 https://api.aliyun.com/product/ocr-api
        config.endpoint = "ocr-api.cn-hangzhou.aliyuncs.com";
        return new com.aliyun.ocr_api20210707.Client(config);
    }

    /**
     * 发票识别
     *
     * @param url String
     */
    public String recognizeInvoiceScan(String url) {
        try {
            com.aliyun.ocr_api20210707.Client client = this.createClient();
            com.aliyun.ocr_api20210707.models.RecognizeInvoiceRequest recognizeInvoiceRequest = new com.aliyun.ocr_api20210707.models.RecognizeInvoiceRequest()
                    .setUrl(url);
            com.aliyun.teautil.models.RuntimeOptions runtime = new com.aliyun.teautil.models.RuntimeOptions();
            // 复制代码运行请自行打印 API 的返回值
            RecognizeInvoiceResponse response = client.recognizeInvoiceWithOptions(recognizeInvoiceRequest, runtime);
            RecognizeInvoiceResponseBody body = response.getBody();
            return body.data;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }

    }

}
