package com.imokkkk.util;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * <h1><a href="https://time.geekbang.org/column/intro/100312101">极客专栏地址：https://time.geekbang.org/column/intro/100312101</a></h1><br/><h1><a href="https://gitee.com/ylimhhmily/GeekDubbo3Tutorial">案例代码地址：https://gitee.com/ylimhhmily/GeekDubbo3Tutorial</a></h1>
 *
 * @author hmilyylimh
 * ^_^
 * @version 0.0.1
 * ^_^
 * @date 2022-11-22
 */
public class RsaUtils {

    private static final String KEY_ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    public static String sign(byte[] privateKey, String plainText){
        return sign(privateKey, plainText, SIGNATURE_ALGORITHM, StandardCharsets.UTF_8);
    }
    public static String sign(String privateKey,
                              String plainText){
        return sign(privateKey.getBytes(StandardCharsets.UTF_8), plainText);
    }

    public static String sign(byte[] privateKey,
                              String plainText,
                              String signTransform,
                              Charset charset){
        try {
            PKCS8EncodedKeySpec priPKCS8 = new PKCS8EncodedKeySpec(Base64.decodeBase64(privateKey));
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            PrivateKey priKey = keyFactory.generatePrivate(priPKCS8);

            Signature sign = Signature.getInstance(signTransform);
            sign.initSign(priKey);
            sign.update(plainText.getBytes(charset));
            return Hex.encodeHexString(sign.sign());
        } catch (Exception e) {
            throw new RuntimeException("sign error.", e);
        }
    }

    public static String sign(byte[] privateKey,
                              byte[] plainTextBytes){
        return sign2(privateKey, plainTextBytes, SIGNATURE_ALGORITHM);
    }

    public static String sign2(byte[] privateKey,
                               byte[] plainTextBytes,
                               String signTransform){
        try {
            PKCS8EncodedKeySpec priPKCS8 = new PKCS8EncodedKeySpec(Base64.decodeBase64(privateKey));
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            PrivateKey priKey = keyFactory.generatePrivate(priPKCS8);

            Signature sign = Signature.getInstance(signTransform);
            sign.initSign(priKey);
            sign.update(plainTextBytes);
            return Hex.encodeHexString(sign.sign());
        } catch (Exception e) {
            throw new RuntimeException("sign error.", e);
        }
    }

    public static boolean verify(byte[] publicKey, String plainText, String signText){
        return verify(publicKey, plainText, signText, SIGNATURE_ALGORITHM, StandardCharsets.UTF_8);
    }

    public static boolean verify(byte[] publicKey,
                                 String plainText,
                                 String signText,
                                 String signTransform,
                                 Charset charset){
        try {
            X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(Base64.decodeBase64(publicKey));
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            PublicKey pubKey = keyFactory.generatePublic(x509EncodedKeySpec);

            byte[] signed = Hex.decodeHex(signText.toCharArray());
            Signature signature = Signature.getInstance(signTransform);
            signature.initVerify(pubKey);
            signature.update(plainText.getBytes(charset));
            return signature.verify(signed);
        } catch (Throwable e) {
            throw new RuntimeException("verify error.", e);
        }
    }

    public static boolean verify(byte[] publicKey,
                                 byte[] plainTextBytes,
                                 String signText) {
        return verify2(publicKey, plainTextBytes, signText, SIGNATURE_ALGORITHM);
    }

    public static boolean verify2(byte[] publicKey,
                                  byte[] plainTextBytes,
                                  String signText,
                                  String signTransform) {
        try {
            X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(Base64.decodeBase64(publicKey));
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            PublicKey pubKey = keyFactory.generatePublic(x509EncodedKeySpec);

            byte[] signed = Hex.decodeHex(signText.toCharArray());
            Signature signature = Signature.getInstance(signTransform);
            signature.initVerify(pubKey);
            signature.update(plainTextBytes);
            return signature.verify(signed);
        } catch (Throwable e) {
            throw new RuntimeException("verify error.", e);
        }
    }

    public static boolean verify(String publicKey, String plainText, String signText){
        return verify(publicKey.getBytes(StandardCharsets.UTF_8), plainText, signText);
    }

    public static KeyPair getKeyPair(){
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            keyPairGenerator.initialize(KEY_SIZE);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("getKeyPair error.", e);
        }
    }
    public static PublicPrivateInfo getBase64KeyPair(){
        KeyPair keyPair = getKeyPair();

        PublicPrivateInfo publicPrivateInfo = new PublicPrivateInfo();
        publicPrivateInfo.setPublicKey(Base64.encodeBase64String(keyPair.getPublic().getEncoded()));
        publicPrivateInfo.setPrivateKey(Base64.encodeBase64String(keyPair.getPrivate().getEncoded()));
        return publicPrivateInfo;
    }
    @Setter
    @Getter
    public static class PublicPrivateInfo {
        private String publicKey;
        private String privateKey;
    }

    public static void main(String[] args) {
        PublicPrivateInfo publicPrivateInfo = getBase64KeyPair();
        String publicKey = publicPrivateInfo.getPublicKey();
        String privateKey = publicPrivateInfo.getPrivateKey();
        System.out.println("publicKey=" + publicKey);
        System.out.println("privateKey=" + privateKey);

        String reqData = "hello 你好";
        String signed = sign(privateKey, reqData);
        System.out.println("signed=" + signed);
        System.out.println("verified=" + verify(publicKey, reqData, signed));
    }
}
