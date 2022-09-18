package com.example.xiaoxiguaxilie.util;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CipherUtils {

    private static char sHexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};


    /**
     * md5加密
     *
     * @param source
     * @return
     */
    public static String md5(String source) {
        try {
            byte[] bytes = source.getBytes();
            // 获得MD5摘要算法的 MessageDigest 对象
            MessageDigest md = MessageDigest.getInstance("MD5");
            // 使用指定的字节更新摘要
            md.update(bytes);
            // 获得密文
            byte[] mdBytes = md.digest();
            // 把密文转换成十六进制的字符串形式
            int length = mdBytes.length;
            char[] chars = new char[length * 2];
            int k = 0;
            for (int i = 0; i < length; i++) {
                byte byte0 = mdBytes[i];
                chars[k++] = sHexDigits[byte0 >>> 4 & 0xf];
                chars[k++] = sHexDigits[byte0 & 0xf];
            }
            return new String(chars);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * AES/CBC/PKCS5Padding 加密
     *
     * @param content    :待加密的内容.
     * @param secret_key :用于生成密钥的 key，自定义即可，加密与解密必须使用同一个，如果不一致，则抛出异常
     * @param vector_key 用于生成算法参数规范的 key，自定义即可，加密与解密必须使用同一个，如果不一致，解密的内容可能会造成与源内容不一致.
     *                   <p>
     *                   1、secret_key、vector_key: AES 时必须是 16 个字节，DES 时必须是 8 字节.
     *                   2、secret_key、vector_key 值不建议使用中文，如果是中文，注意一个汉字是3个字节。
     *                   </p>
     * @return 返回 Cipher 加密后的数据，对加密后的字节数组用 Base64 进行编码转成了可视字符串，如 7giH2bqIMH3kDMIg8gq0nY
     * @throws Exception
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String encrypt(String content, String secret_key, String vector_key) throws Exception {
        //实例化 Cipher 对象。使用：AES-高级加密标准算法、CBC-有向量模式、PKCS5Padding-填充方案:（加密内容不足8位时用余位数补足8位）
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        //使用 SecretKeySpec(byte[] key, String algorithm) 创建密钥. 算法要与 Cipher.getInstance 保持一致.
        SecretKey secretKey = new SecretKeySpec(secret_key.getBytes(), "AES");
        /**
         * init(int opMode,Key key,AlgorithmParameterSpec params)：初始化 Cipher，
         * 1、Cipher.ENCRYPT_MODE 表示加密模式
         * 2、key 表示加密密钥
         * 3、params 表示算法参数规范，使用 CBC 有向量模式时，必须传入,如果是 ECB-无向量模式,那么可以不传
         * 4、所有参数规范都必须实现 {@link AlgorithmParameterSpec} 接口.
         */
        IvParameterSpec parameterSpec = new IvParameterSpec(vector_key.getBytes());
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
        /**
         * byte[] doFinal(byte[] content)：对 content 完成加密操作，如果 cipher.init 初始化时使用的解密模式，则此时是解密操作.
         * 返回的是加密后的字节数组，如果直接 new String(byte[] bytes) 是会乱码的，可以借助 BASE64 转为可视字符串，或者转成 16 进制字符
         */
        byte[] encrypted = cipher.doFinal(content.getBytes());
        //对字节数组内容进行编码，转为可视字符串，这样方便存储和转换.
        String base64Encode = Base64.getEncoder().encodeToString(encrypted);
        return base64Encode;
    }


    /**
     * AES解密
     * @param content
     * @param secret_key
     * @param iv
     * @return
     * @throws Exception
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String decrypt(String content, String secret_key, String iv) throws Exception {

        SecretKeySpec skeySpec = new SecretKeySpec(hex2byte(secret_key), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(hex2byte(iv)));

        byte[] encrypted = cipher.doFinal(Base64.getDecoder().decode(content));
        return new String(encrypted);
    }



    /**
     * 将hex字符串转换成字节数组
     * 注意前端只要是用了 CryptoJS.enc.Hex.parse() 输出，就一定需要这个方法
     **/
    private static byte[] hex2byte(String inputString) {
        if (inputString == null || inputString.length() < 2) {
            return new byte[0];
        }
        inputString = inputString.toLowerCase();
        int l = inputString.length() / 2;
        byte[] result = new byte[l];
        for (int i = 0; i < l; ++i) {
            String tmp = inputString.substring(2 * i, 2 * i + 2);
            result[i] = (byte) (Integer.parseInt(tmp, 16) & 0xFF);
        }
        return result;
    }



    /**
     *  如果密钥不足16位，那么就补足
     * @param keyBytes
     * @return
     */
    public static byte[] get16Byte(byte[] keyBytes){
        int base = 16;
        if (keyBytes.length % base != 0) {
            int groups = keyBytes.length / base + (keyBytes.length % base != 0 ? 1 : 0);
            byte[] temp = new byte[groups * base];
            Arrays.fill(temp, (byte) 0);
            System.arraycopy(keyBytes, 0, temp, 0, keyBytes.length);
            keyBytes = temp;
        }
        return keyBytes;
    }
}
