package com.evcharge.libsdk.abcpay;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.io.FileReader;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Enumeration;

import org.apache.commons.codec.binary.Base64;

/**
 * 从文件加载RSA密钥对的工具类
 */
public class KeyLoaderUtil {

    /**
     * 从PKCS12/PFX格式的密钥库文件加载私钥
     *
     * @param keystorePath 密钥库文件路径
     * @param keystorePassword 密钥库密码
     * @param alias 密钥别名，如果为null则使用第一个找到的别名
     * @return 私钥对象
     */
    public static PrivateKey loadPrivateKeyFromPKCS12(String keystorePath, String keystorePassword, String alias) throws Exception {
        File keystoreFile = new File(keystorePath);
        try (FileInputStream fis = new FileInputStream(keystoreFile)) {
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            keystore.load(fis, keystorePassword.toCharArray());

            // 如果未指定别名，则使用第一个找到的别名
            String keyAlias = alias;
            if (keyAlias == null) {
                Enumeration<String> aliases = keystore.aliases();
                if (aliases.hasMoreElements()) {
                    keyAlias = aliases.nextElement();
                } else {
                    throw new Exception("密钥库中没有找到密钥");
                }
            }

            // 检查指定别名是否存在
            if (!keystore.isKeyEntry(keyAlias)) {
                throw new Exception("指定的别名 '" + keyAlias + "' 在密钥库中不存在或不是密钥条目");
            }

            // 加载私钥
            return (PrivateKey) keystore.getKey(keyAlias, keystorePassword.toCharArray());
        }
    }

    /**
     * 从PKCS12/PFX格式的密钥库文件加载公钥证书
     *
     * @param keystorePath 密钥库文件路径
     * @param keystorePassword 密钥库密码
     * @param alias 密钥别名，如果为null则使用第一个找到的别名
     * @return 公钥对象
     */
    public static PublicKey loadPublicKeyFromPKCS12(String keystorePath, String keystorePassword, String alias) throws Exception {
        File keystoreFile = new File(keystorePath);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(keystoreFile);
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            keystore.load(fis, keystorePassword.toCharArray());

            // 如果未指定别名，则使用第一个找到的别名
            String keyAlias = alias;
            if (keyAlias == null) {
                Enumeration<String> aliases = keystore.aliases();
                if (aliases.hasMoreElements()) {
                    keyAlias = aliases.nextElement();
                } else {
                    throw new Exception("密钥库中没有找到密钥");
                }
            }

            // 检查指定别名是否存在
            if (!keystore.isKeyEntry(keyAlias)) {
                throw new Exception("指定的别名 '" + keyAlias + "' 在密钥库中不存在或不是密钥条目");
            }

            // 获取证书并提取公钥
            Certificate cert = keystore.getCertificate(keyAlias);
            return cert.getPublicKey();
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }

    /**
     * 从X.509证书文件加载公钥
     *
     * @param certPath 证书文件路径（.cer, .crt, .pem等）
     * @return 公钥对象
     */
    public static PublicKey loadPublicKeyFromCertificate(String certPath) throws Exception {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(certPath);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate cert = cf.generateCertificate(fis);
            return cert.getPublicKey();
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }

    /**
     * 从PEM格式文件加载RSA私钥
     *
     * @param privateKeyPath 私钥文件路径
     * @return 私钥对象
     */
    public static PrivateKey loadPrivateKeyFromPEM(String privateKeyPath) throws Exception {
        File keyFile = new File(privateKeyPath);
        FileReader fr = null;
        try {
            fr = new FileReader(keyFile);
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[1024];
            int read;
            while ((read = fr.read(buffer)) != -1) {
                sb.append(buffer, 0, read);
            }

            String privateKeyPEM = sb.toString();

            // 移除PEM头尾和换行符
            privateKeyPEM = privateKeyPEM
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            // 解码Base64
            byte[] encoded = Base64.decodeBase64(privateKeyPEM);

            // 创建私钥规范
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            return keyFactory.generatePrivate(keySpec);
        } finally {
            if (fr != null) {
                fr.close();
            }
        }
    }

    /**
     * 从PEM格式文件加载RSA公钥
     *
     * @param publicKeyPath 公钥文件路径
     * @return 公钥对象
     */
    public static PublicKey loadPublicKeyFromPEM(String publicKeyPath) throws Exception {
        File keyFile = new File(publicKeyPath);
        FileReader fr = null;
        try {
            fr = new FileReader(keyFile);
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[1024];
            int read;
            while ((read = fr.read(buffer)) != -1) {
                sb.append(buffer, 0, read);
            }

            String publicKeyPEM = sb.toString();

            // 移除PEM头尾和换行符
            publicKeyPEM = publicKeyPEM
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            // 解码Base64
            byte[] encoded = Base64.decodeBase64(publicKeyPEM);

            // 创建公钥规范
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            return keyFactory.generatePublic(keySpec);
        } finally {
            if (fr != null) {
                fr.close();
            }
        }
    }

    /**
     * 使用示例
     */
    public static void main(String[] args) {
        try {
            // 从PKCS12/PFX文件加载密钥对
            String keystorePath = "./abckey/keystore.p12";
            String keystorePassword = "password";
            PrivateKey privateKey = loadPrivateKeyFromPKCS12(keystorePath, keystorePassword, null);
            PublicKey publicKey = loadPublicKeyFromPKCS12(keystorePath, keystorePassword, null);
            System.out.println("成功从PKCS12文件加载密钥对");

            // 从证书文件加载公钥
            String certPath = "./abckey/certificate.cer";
            PublicKey publicKeyFromCert = loadPublicKeyFromCertificate(certPath);
            System.out.println("成功从证书文件加载公钥");

            // 从PEM文件加载密钥对
            String privateKeyPEM = "./abckey/private_key.pem";
            String publicKeyPEM = "./abckey/public_key.pem";
            PrivateKey privateKeyFromPEM = loadPrivateKeyFromPEM(privateKeyPEM);
            PublicKey publicKeyFromPEM = loadPublicKeyFromPEM(publicKeyPEM);
            System.out.println("成功从PEM文件加载密钥对");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}