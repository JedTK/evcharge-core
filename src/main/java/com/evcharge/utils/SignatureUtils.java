package com.evcharge.utils;

import com.alibaba.fastjson2.JSONObject;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.TreeMap;

/**
 * 签名工具类
 * 用于生成和验证API请求签名，确保数据传输的安全性和完整性。
 * 主要功能包括生成随机密钥、计算请求参数签名、验证签名有效性等。
 */
public class SignatureUtils {

    /**
     * 生成随机密钥
     * 使用安全随机数生成器创建指定长度的随机字符串作为密钥
     *
     * @param length 要生成的密钥长度
     * @return 生成的随机密钥字符串
     */
    public static String generateSecretKey(int length) {
        // 定义可用于生成密钥的字符集（大小写字母和数字）
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        // 使用SecureRandom而非Random以提高安全性
        SecureRandom random = new SecureRandom();
        // 随机选择字符组成密钥
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * 生成签名
     * 根据请求参数和密钥生成签名，签名算法流程：
     * 1. 将所有参数按键名升序排序
     * 2. 构建参数查询字符串（key1=value1&key2=value2...）
     * 3. 在查询字符串末尾拼接密钥
     * 4. 对拼接后的字符串进行MD5加密
     * 5. 将MD5加密结果转为大写
     *
     * @param params 请求参数，JSON格式
     * @param secretKey 密钥字符串
     * @return 计算得到的签名（MD5加密后的大写字符串）
     */
    public static String generateSignature(JSONObject params, String secretKey) {
        // 按键名升序排序参数
        TreeMap<String, Object> sortedParams = new TreeMap<>();
        for (String key : params.keySet()) {
            sortedParams.put(key, params.get(key));
        }

        // 构建参数查询字符串
        StringBuilder queryString = new StringBuilder();
        for (Map.Entry<String, Object> entry : sortedParams.entrySet()) {
            if (queryString.length() > 0) {
                queryString.append("&");
            }
            queryString.append(entry.getKey()).append("=").append(entry.getValue());
        }

        // 在查询字符串末尾拼接密钥
        queryString.append(secretKey);
        System.out.printf("queryString=%s \n", queryString);

        // 对拼接后的字符串进行MD5加密
        String md5 = md5(queryString.toString());

        // 将MD5加密结果转为大写并返回
        return md5.toUpperCase();
    }

    /**
     * 验证签名方法
     * 比较传入的签名与根据参数重新计算的签名是否一致
     *
     * @param params 请求参数，JSON格式
     * @param signature 待验证的签名
     * @param secretKey 密钥字符串
     * @return 签名验证结果，true表示验证通过，false表示验证失败
     */
    public static boolean verifySignature(JSONObject params, String signature, String secretKey) {
        // 根据参数和密钥重新计算签名
        String generatedSignature = generateSignature(params, secretKey);
        System.out.printf("generatedSignature=%s \n", generatedSignature);
        System.out.printf("secretKey=%s  \n", secretKey);
        System.out.printf("signature=%s  \n", signature);
        return generatedSignature.equals(signature);
    }

    /**
     * MD5加密辅助方法
     * 对输入字符串进行MD5加密，并返回16进制表示的字符串
     *
     * @param input 待加密的字符串
     * @return 加密后的MD5值（16进制小写字符串）
     */
    private static String md5(String input) {
        try {
            // 获取MD5算法实例
            MessageDigest md = MessageDigest.getInstance("MD5");
            // 计算MD5摘要
            byte[] messageDigest = md.digest(input.getBytes());

            // 将字节数组转换为16进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                // 将每个字节转换为16进制，并确保是两位数
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) {
                    hexString.append('0'); // 补齐前导零
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}