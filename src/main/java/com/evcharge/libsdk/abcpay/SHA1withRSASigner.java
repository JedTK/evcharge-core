package com.evcharge.libsdk.abcpay;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.io.ByteArrayInputStream;
import java.util.Enumeration;
import org.apache.commons.codec.binary.Base64;

/**
 * SHA1withRSA 非对称加签和验签算法实现
 */
public class SHA1withRSASigner {

    private static final String ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";
    private static final String SHA1withRSA = "SHA1withRSA";

    /**
     * 原始方法 - 使用PKCS12密钥文件进行签名
     *
     * @param merchantPrivateKey         商户私钥（PKCS12格式）
     * @param merchantPrivateKeyPassword 商户私钥密码
     * @param message                    请求报文原始数据
     * @return 加签后的请求报文
     * @throws Exception 签名过程中可能发生异常
     */
    public static String signRequest(byte[] merchantPrivateKey, String merchantPrivateKeyPassword, String message) throws Exception {
        Signature tSignature = Signature.getInstance(SHA1withRSA);
        KeyStore tKeyStore = KeyStore.getInstance("PKCS12");
        tKeyStore.load(new ByteArrayInputStream(merchantPrivateKey), merchantPrivateKeyPassword.toCharArray());

        // 获取密钥别名
        Enumeration<String> aliases = tKeyStore.aliases();
        String tAliases = null;
        while (aliases.hasMoreElements()) {
            tAliases = aliases.nextElement();
            break;
        }

        // 获取私钥
        PrivateKey iMerchantKey = (PrivateKey) tKeyStore.getKey(tAliases, merchantPrivateKeyPassword.toCharArray());
        tSignature.initSign(iMerchantKey);
        tSignature.update(message.getBytes("UTF-8"));

        // 签名并进行Base64编码
        String signatureInBase64 = new String(Base64.encodeBase64(tSignature.sign()), "UTF-8");

        // 构造签名后的完整请求报文
        String signedRequest = "{\"Message\":" + message + "," +
                "\"Signature-Algorithm\":" + "\"" + SIGNATURE_ALGORITHM + "\"" + "," +
                "\"Signature\":" + "\"" + signatureInBase64 + "\"}";
        return signedRequest;
    }

    /**
     * 新增方法 - 使用PrivateKey对象直接进行签名
     * 适用于从KeyLoaderUtil获取的PrivateKey对象
     *
     * @param privateKey 私钥对象
     * @param message    请求报文原始数据
     * @return 加签后的请求报文
     * @throws Exception 签名过程中可能发生异常
     */
    public static String signRequest(PrivateKey privateKey, String message) throws Exception {
        Signature tSignature = Signature.getInstance(SHA1withRSA);
        tSignature.initSign(privateKey);
        tSignature.update(message.getBytes(StandardCharsets.UTF_8));

        // 签名并进行Base64编码
        String signatureInBase64 = new String(Base64.encodeBase64(tSignature.sign()), "UTF-8");

        // 构造签名后的完整请求报文
        String signedRequest = "{\"Message\":" + message + "," +
                "\"Signature-Algorithm\":" + "\"" + SIGNATURE_ALGORITHM + "\"" + "," +
                "\"Signature\":" + "\"" + signatureInBase64 + "\"}";
        return signedRequest;
    }

    /**
     * 计算签名值（不构造完整报文）
     *
     * @param privateKey 私钥对象
     * @param message    要签名的消息
     * @return Base64编码的签名值
     * @throws Exception 签名过程中可能发生异常
     */
    public static String sign(PrivateKey privateKey, String message) throws Exception {
        Signature tSignature = Signature.getInstance(SHA1withRSA);
        tSignature.initSign(privateKey);
        tSignature.update(message.getBytes(StandardCharsets.UTF_8));
        return new String(Base64.encodeBase64(tSignature.sign()), StandardCharsets.UTF_8);
    }

    /**
     * 验证签名
     *
     * @param publicKey 公钥对象
     * @param message   原始消息
     * @param signature Base64编码的签名
     * @return 验证结果
     * @throws Exception 验证过程中可能发生异常
     */
    public static boolean verify(PublicKey publicKey, String message, String signature) throws Exception {
        Signature sig = Signature.getInstance(SHA1withRSA);
        sig.initVerify(publicKey);
        sig.update(message.getBytes(StandardCharsets.UTF_8));
        return sig.verify(Base64.decodeBase64(signature.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * 验证XML格式的响应消息签名
     *
     * @param publicKey 公钥对象
     * @param xmlResponse XML格式的响应消息
     * @return 验证结果
     * @throws Exception 验证过程中可能发生异常
     */
    public static boolean verifyXmlResponse(PublicKey publicKey, String xmlResponse) throws Exception {
        // 此处需要根据XML格式提取Message部分和Signature部分
        // 这里仅为示例，实际应根据XML结构进行解析

        // 假设提取方式如下（实际项目中需要使用合适的XML解析方法）
        int messageStart = xmlResponse.indexOf("<Message>");
        int messageEnd = xmlResponse.indexOf("</Message>") + "</Message>".length();
        String message = xmlResponse.substring(messageStart, messageEnd);

        int signatureStart = xmlResponse.indexOf("<Signature>") + "<Signature>".length();
        int signatureEnd = xmlResponse.indexOf("</Signature>");
        String signature = xmlResponse.substring(signatureStart, signatureEnd);

        // 验证签名
        return verify(publicKey, message, signature);
    }

    /**
     * 使用示例 - 展示如何结合KeyLoaderUtil使用
     */
    public static void main(String[] args) {
        try {
            // 示例1：从PKCS12文件加载私钥并签名
            String keystorePath = "./abckey/keystore.p12";
            String keystorePassword = "password";
            PrivateKey privateKey = KeyLoaderUtil.loadPrivateKeyFromPKCS12(keystorePath, keystorePassword, null);

            // 待签名的数据（JSON格式）
            String jsonMessage = "{\"OrderNo\":\"123456\",\"Amount\":\"100.00\",\"MerchantID\":\"888888\"}";

            // 使用新方法直接使用PrivateKey对象进行签名
            String signedRequest = signRequest(privateKey, jsonMessage);
            System.out.println("签名后的完整请求：");
            System.out.println(signedRequest);

            // 示例2：签名验证
            String publicKeyPath = "./abckey/certificate.cer";
            PublicKey publicKey = KeyLoaderUtil.loadPublicKeyFromCertificate(publicKeyPath);

            // 单独获取签名值
            String signature = sign(privateKey, jsonMessage);
            System.out.println("签名值：" + signature);

            // 验证签名
            boolean verified = verify(publicKey, jsonMessage, signature);
            System.out.println("验证结果：" + (verified ? "签名验证通过" : "签名验证失败"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}