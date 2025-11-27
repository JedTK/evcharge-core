package com.evcharge.libsdk.dahua;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

public class SignatureUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    /**
     * 获取token生成签名
     *
     * @param accessKey       产品对应的 ak
     * @param secretAccessKey 产品对应的 sk
     * @param timestamp       13位标准时间戳 (System.currentTimeMillis())
     * @param nonce           开发者生成的 UUID 或随机字符串
     * @param stringToSign    固定为 "POST" 或接口要求的值
     * @return 签名 sign
     */
    public static String generateSign(String accessKey,
                                      String secretAccessKey,
                                      String timestamp,
                                      String nonce,
                                      String stringToSign) {

        // 1. 拼接 strAuthFactor
        String strAuthFactor = accessKey + timestamp + nonce + stringToSign;

        // 2. 去掉空格和空字符
        strAuthFactor = strAuthFactor.replaceAll("\\s+", "");

        // 3. 计算 HMAC-SHA512 摘要并转大写
        return hmacSha512(strAuthFactor, secretAccessKey).toUpperCase();
    }

    /**
     * 业务API签名生成方法
     *
     * @param accessKey       产品对应的 ak
     * @param appAccessToken  产品对应的 appAccessToken
     * @param timestamp       13位时间戳
     * @param nonce           UUID 或随机字符串
     * @param method          请求方法 (如 POST/GET)
     * @param headerMap       请求头
     * @param requestBody     请求体字符串（JSON）
     * @param secretAccessKey 产品对应的 sk
     * @return 签名 sign
     */
    public static String openSign(String accessKey,
                                      String appAccessToken,
                                      String timestamp,
                                      String nonce,
                                      String method,
                                      Map<String, String> headerMap,
                                      String requestBody,
                                      String secretAccessKey) {
        try {

            if (!StringUtils.hasLength(accessKey) || !StringUtils.hasLength(appAccessToken)
                    || !StringUtils.hasLength(timestamp) || !StringUtils.hasLength(nonce)
                    || !StringUtils.hasLength(method) || !StringUtils.hasLength(secretAccessKey)) {
                throw new RuntimeException("鉴权参数缺失");
            }

            // ====== 拼接 strAuthFactor 的固定部分 ======
            StringBuilder str = new StringBuilder();
            str.append(accessKey);
            str.append(appAccessToken);
            str.append(timestamp);
            str.append(nonce);

            // ====== 构建 stringToSign ======
            List<String> lines = new ArrayList<>();
            lines.add(method); // Method 必须在第一行

            // 1. 请求体处理
            if (StringUtils.hasLength(requestBody)) {
                String body = deleteWhitespace(requestBody);
                lines.add(sha512(body));
            }

            // 2. 处理 Sign-Headers
            String signHeaders = headerMap == null ? "" : headerMap.get("Sign-Headers");
            if (StringUtils.hasLength(signHeaders)) {
                String[] headerNames = signHeaders.split("\\s*:\\s*");
                String headerLine = Arrays.stream(headerNames)
                        .map(String::trim)
                        .filter(it -> it.length() > 0)
                        .map(it -> it + ":" + headerMap.get(it))
                        .collect(Collectors.joining("\n"));
                if (StringUtils.hasLength(headerLine)) {
                    lines.add(headerLine);
                }
            }

            // 拼接 stringToSign
            String stringToSign = String.join("\n", lines);
            str.append(stringToSign);

            // ====== HMAC-SHA512 签名 ======
            return encrypt(str.toString(), secretAccessKey, "HmacSHA512").toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("生成签名失败", e);
        }
    }

    /**
     * HMAC-SHA512 算法实现
     */
    private static String hmacSha512(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(secretKeySpec);
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(bytes);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA512 签名失败", e);
        }
    }

    /**
     * 字节数组转 Hex
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // 示例
//    public static void main(String[] args) {
//        String accessKey = "your-ak";
//        String secretAccessKey = "your-sk";
//        String timestamp = String.valueOf(System.currentTimeMillis()); // 13位时间戳
//        String nonce = "123e4567e89b12d3a456426655440000"; // 示例 UUID
//        String stringToSign = "POST";
//
//        String sign = generateSign(accessKey, secretAccessKey, timestamp, nonce, stringToSign);
//        System.out.println("签名 sign: " + sign);
//    }
    // 去除空格、制表符、换行符、回车符、换页符、垂直制表符
    public static String deleteWhitespace(String input) {
        if (input == null) return null;
        return input.replaceAll("[ \\t\\n\\r\\f\\v\\u000B]", "");
    }

    // 计算 SHA512
    public static String sha512(String data) throws Exception {
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-512");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHexStr(hash);
    }

    // HMAC-SHA512 加密
    public static String encrypt(String input, String key, String algorithm) {
        String cipher = "";
        try {
            byte[] data = key.getBytes(StandardCharsets.UTF_8);
            SecretKey secretKey = new SecretKeySpec(data, algorithm);
            Mac mac = Mac.getInstance(algorithm);
            mac.init(secretKey);
            byte[] text = input.getBytes(StandardCharsets.UTF_8);
            byte[] encryptByte = mac.doFinal(text);
            cipher = bytesToHexStr(encryptByte);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HMAC-SHA512 签名失败", e);
        }
        return cipher;
    }

    // 字节数组转 HEX
    public static String bytesToHexStr(byte[] bytes) {
        StringBuilder hexStr = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            hexStr.append(hex);
        }
        return hexStr.toString();
    }
}
