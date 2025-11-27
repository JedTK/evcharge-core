package com.evcharge.libsdk.abcpay;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class ABCPayUtilsSDK {


    /**
     * @description DES加密 CBC模式
     * @param src 待加密的字符
     * @param key 加密的密钥
     * @param keyIv DES加密的偏移量
     * @return
     */
    public static byte[] encryptCBCMode(String src, String key, String keyIv) {
        try {
            DESKeySpec dks = new DESKeySpec(key.getBytes(StandardCharsets.UTF_8));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey securekey = keyFactory.generateSecret(dks);
            IvParameterSpec iv = new IvParameterSpec(keyIv.getBytes(StandardCharsets.UTF_8));
            Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, securekey,iv);
            byte[] bytes = cipher.doFinal(src.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encode(bytes);
        } catch (Exception e) {
            System.out.println("encryptCBCMode 加密失败");
        }
        return null;
    }


    /**
     * @description DES解密 CBC模式
     * @param src 待解密的字符
     * @param key 解密的密钥
     * @param keyIv DES解密的偏移量
     * @return
     */
    public static byte[] decryptCBCMode(String src, String key, String keyIv) {
        try {
            DESKeySpec dks = new DESKeySpec(key.getBytes(StandardCharsets.UTF_8));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey securekey = keyFactory.generateSecret(dks);
            IvParameterSpec iv = new IvParameterSpec(keyIv.getBytes(StandardCharsets.UTF_8));
            Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, securekey,iv );
            return cipher.doFinal(Base64.getDecoder().decode(src.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            System.out.println("decryptCBCMode 解密失败");
        }
        return null;
    }



}
