package com.imokkkk.util;

import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * <h1>非常简单的AES加密，只需要传入秘钥即可进行对称加密，而且该类可以供前端后端一起使用。</h1>
 *
 * @author hmilyylimh
 *         ^_^
 * @version 0.0.1
 *         ^_^
 * @date 2021-11-12
 *
 */
public class AesUtils {

    private static final String KEY_AES = "AES";
    private static final String HEX_CHARS = "0123456789ABCDEF";

    /**
     * 通过AES算法加密
     * @param content 需要加密的原始报文
     * @param privateSecret 密钥
     * @return
     * @throws Exception
     */
    public static String encrypt(String content, String privateSecret) {
        try {
            SecretKeySpec secretKeySpec = getSecretKeySpec(privateSecret);
            Cipher cipher = Cipher.getInstance(KEY_AES);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

            byte[] encrypted = cipher.doFinal(content.getBytes());
            return bytes2HexStr(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Aes encrypt failed: " + e.getMessage(), e);
        }
    }

    /**
     * 通过AES算法解密
     * @param content 需要解密得字符串
     * @param privateSecret 密钥
     * @return
     * @throws Exception
     */
    public static String decrypt(String content, String privateSecret) {
        try {
            SecretKeySpec secretKeySpec = getSecretKeySpec(privateSecret);

            Cipher cipher = Cipher.getInstance(KEY_AES);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);

            byte[] encryptedBytes = hexStr2Bytes(content);
            byte[] decryptBytes = cipher.doFinal(encryptedBytes);

            return new String(decryptBytes);
        } catch (Exception e) {
            throw new RuntimeException("Aes decrypt failed: " + e.getMessage(), e);
        }
    }

    private static SecretKeySpec getSecretKeySpec(String key){
        byte[] raw = hexStr2Bytes(key);
        return new SecretKeySpec(raw, KEY_AES);
    }

    public static String bytes2HexStr(byte[] src){

        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < src.length; i++) {

            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }

            stringBuilder.append(hv);
        }

        return stringBuilder.toString();
    }

    public static byte[] hexStr2Bytes(String hexString) {

        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];

        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }

        return d;
    }

    private static byte charToByte(char c) {
        return (byte) HEX_CHARS.indexOf(c);
    }

    public static void main(String[] args) {
        String str = "hello 你好";
        String aesSecret = bytes2HexStr("BC1O8qSfEXPzwB1c".getBytes(StandardCharsets.UTF_8));
        System.out.println(aesSecret);
        String encrypt = encrypt(str, aesSecret);
        System.out.println(encrypt);
        System.out.println(decrypt(encrypt, aesSecret));
    }
}
