package com.imokkkk.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * <h1><a href="https://time.geekbang.org/column/intro/100312101">极客专栏地址：https://time.geekbang.org/column/intro/100312101</a></h1><br/><h1><a href="https://gitee.com/ylimhhmily/GeekDubbo3Tutorial">案例代码地址：https://gitee.com/ylimhhmily/GeekDubbo3Tutorial</a></h1>
 *
 * @author hmilyylimh
 * ^_^
 * @version 0.0.1
 * ^_^
 * @date 2022-11-22
 */
public class SignUtils {

    public static String addSign(Object[] parameters, String privateKey) {
        try {
            Object[] includeMetadata = new Object[parameters.length];
            System.arraycopy(parameters, 0, includeMetadata, 0, parameters.length);
            byte[] bytes = toByteArray(includeMetadata);

            return RsaUtils.sign(privateKey.getBytes(StandardCharsets.UTF_8), bytes);
        } catch (Exception e) {
            // ignore error
            return null;
        }
    }

    public static boolean verifySign(Object[] parameters, String publicKey, String signText) {
        try {
            Object[] includeMetadata = new Object[parameters.length];
            System.arraycopy(parameters, 0, includeMetadata, 0, parameters.length);
            byte[] bytes = toByteArray(includeMetadata);

            return RsaUtils.verify(publicKey.getBytes(StandardCharsets.UTF_8), bytes, signText);
        } catch (Exception e) {
            // ignore error
            return false;
        }
    }

    private static byte[] toByteArray(Object[] parameters) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(parameters);
            out.flush();
            return bos.toByteArray();
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }
    }

}
