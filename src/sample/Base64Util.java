package sample;

import java.util.Base64;

public class Base64Util {
     //加密
    public static String EncodeBase64(byte[] data)
    {
        Base64.Encoder encoder = Base64.getEncoder();
        String str = encoder.encodeToString(data);
        return str;
    }

     //解密
    public static String DecodeBase64(String str) {
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] ResultBase = decoder.decode(str);
        String str2=new String(ResultBase);
        return str2;
    }

}
