package com.binance.mgs.nft.utils;


import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class RSAUtils {


    private static final String KEY_ALGORITHM = "RSA";

    public static KeyPair getKeyPair(){
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            return keyPair;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static PublicKey getPublicKey(String publicKey) throws Exception {
        byte[] bytesPublic = Base64.decodeBase64(publicKey);
        KeyFactory rsa = KeyFactory.getInstance(KEY_ALGORITHM);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(bytesPublic);
        return rsa.generatePublic(keySpec);
    }

    private static PrivateKey getPrivateKey(String publicKey) throws Exception {
        byte[] bytesPublic = Base64.decodeBase64(publicKey);
        KeyFactory rsa = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(bytesPublic);
        return rsa.generatePrivate(keySpec);
    }

    public static String encodeByPublicKey(String context, PublicKey publicKey) throws Exception {
        Cipher instance = Cipher.getInstance(KEY_ALGORITHM);
        instance.init(Cipher.ENCRYPT_MODE,publicKey);
        byte[] bytes = instance.doFinal(context.getBytes());
        return new String(Base64.encodeBase64(bytes));
    }

    public static String encodeContext(String context,String publicKey) throws Exception {
        return encodeByPublicKey(context,getPublicKey(publicKey));
    }

    public static String decodeContext(String context,String privateKey) throws Exception {
        return decodeByPrivateKey(context,getPrivateKey(privateKey));
    }


    public static String decodeByPrivateKey(String context, PrivateKey key) throws Exception {
        byte[] data = Base64.decodeBase64(context);
        Cipher instance = Cipher.getInstance(KEY_ALGORITHM);
        instance.init(Cipher.DECRYPT_MODE,key);
        return new String(instance.doFinal(data));
    }

    public static void main(String[] args) throws Exception {

        for (int i = 0; i < 1; i++) {
            String message = "userId=10898921&code=1i3qzt7m&timestamp=1655150085426&subActivityCode=576155141394550784";
            String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgkHKb6iyXDjP/MOOR7wcos9qMeO7OanJBi8zoah0SZNEVjVbmmFfYn8xm+B5YBgtyrFK8+BPtks24e9oJb8DH+MF1o+LReG7I59w2YhMhDYduEX/sKczRl5grNoNAX1mFL0G0Se8ynkpOkBkGes0a6sMvP+B0G1sdktPPx88w9UbSodJhw4v23sq82XAwtbvgZ5f/Dex8JAIOKvKhHkETnwTGtdsRkMIaf8b/iVT7M7T0GQEdXVKA/ABo/vG0qXqqBCqIveKjaFN+vsV/bjMH/1YO78hLCjUppuVsOJoUBDXSMMkIF00wOVwXxw4hSEu8G956HcjZhS6eqg/oCtTVwIDAQAB";
            String publicResults = RSAUtils.encodeByPublicKey(message,RSAUtils.getPublicKey(publicKey));
            System.out.println("publicResults " + i +" = " + publicResults);
            String privateKey = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCCQcpvqLJcOM/8w45HvByiz2ox47s5qckGLzOhqHRJk0RWNVuaYV9ifzGb4HlgGC3KsUrz4E+2Szbh72glvwMf4wXWj4tF4bsjn3DZiEyENh24Rf+wpzNGXmCs2g0BfWYUvQbRJ7zKeSk6QGQZ6zRrqwy8/4HQbWx2S08/HzzD1RtKh0mHDi/beyrzZcDC1u+Bnl/8N7HwkAg4q8qEeQROfBMa12xGQwhp/xv+JVPsztPQZAR1dUoD8AGj+8bSpeqoEKoi94qNoU36+xX9uMwf/Vg7vyEsKNSmm5Ww4mhQENdIwyQgXTTA5XBfHDiFIS7wb3nodyNmFLp6qD+gK1NXAgMBAAECggEAAuJIBB7dDBOp7zO5M7djfutOs5oSLB2pOLzUzNB4+qQLEEmQJKPhQ8IDLCtVJJ6EbQdt3GZr/WI+7dOqH6PSAuO43l5BPCPaS9ic3AQbhZXZJJpQJe4dwYIXa9xMC2tmVjE1NG5HzMfP9N02GijN+VBJMOoLSr0ReLEEKSac5s0Jxb6zfwxKhAxJ1uQpJfaOpt2Yo9tnue7OxOQrnj5NTkVTzxdOScyY8UwYAWrxApROLJ+4ludymxt9SVoiqyFJMVhbl3ObB/Uj7zlneoEQjDjM9bLg5lQytHZi3iZo7xyoDqTwcYUc4VApyjgF+YtT8rh7zYpQ3+pgy/O9YLRl0QKBgQDmuHNDiQ6hZ0IWeorlwVFjaR0bbge3Eo4UCVwg7fxVdxmmy1rpqzEul8TQTIuaEVrOINnlVFW2l1PtXKS0qgYDmKmAbzgOjFstF0uAxsnIMLgLKAqeMtxK6xgK8TBbI8eOHg3D+utIwyKJ9rNqDduTie6h9KA2zECnH8C5SwH+HwKBgQCQh2iq0nUixZd9apBlIPppcNpGl7TnwM1uJq1BxiMi1M2AhrBQTf1uN6soUUdvIqzgS4WsVu2OYuOj67LPmQTDfIQbzDyqIVqdCCWqwVXdbB8o/0SMpBDmELamADhMxa0diRllW6/0Yn7HpECL2gTyXzGIuy8+Ehvu88ZMNXGTyQKBgFS2fuPaK/wJTNOyFNO9QmPs0Voj8UM/1dj3gtM4boD25P1AB1Zqm/lOkl4k7NEZ9CxhFYBFkd8j+xXZAUSwdNrXL81PiNaWpFePCRL0alxNvxWhkxx48jez0DUcT7P3FCtTT5yYwdEKjOD5KvESu3+Vkn/2sOjN4CM83mdqagXjAoGAWeI8r/APNT7ZhgAeKSanVaf/t+NleLQpjpWzLrLA60qZO5OIV4kJUeCBK6PQ30cbaKrPSW0OdHz/wdQ18nHhyonHx1nvaIcxyXNlqZpvgjNZ9a87vJPUhqBiVz7PxL8zeKjpCGZLOZt/6T03f0JpzSpyrexr5xhwEt28t2yNZDECgYAHQYqKYdDc+zznGktgutOksUuCUKRvSA+NrG5AqMd6W69fjkgtK6oC+NBML0zJVgAE93MOfmhEPzix25w86g6Pq7rITTyU1tVkXUxiNkF8K/ZiuYacraU+QURgOuOGvXy7EzgWG8+zMuCXjq+WPPb+f+tTwq8q0cI+mi7Z4HcqKg==";
            System.out.println("privateKey = " + privateKey);
            String context = RSAUtils.decodeContext(publicResults, privateKey);
            System.out.println(context);
        }


    }
}
