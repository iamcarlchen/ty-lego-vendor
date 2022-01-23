package com.greatbee.core.utils.rsaJavaTransferNet;

import org.apache.commons.codec.binary.Base64;

import java.io.IOException;

/**
 * Base64Helper
 *
 * @author xiaobc
 * @date 18/10/25
 */
public class Base64Helper {

    public static String encode(byte[] byteArray) {
        //sun.misc.BASE64Encoder base64Encoder = new sun.misc.BASE64Encoder();
        //return base64Encoder.encode(byteArray);
        return Base64.encodeBase64String(byteArray);
    }

    public static byte[] decode(String base64EncodedString) {
//        sun.misc.BASE64Decoder base64Decoder = new sun.misc.BASE64Decoder();
//            return base64Decoder.decodeBuffer(base64EncodedString);
        return Base64.decodeBase64(base64EncodedString);
    }
}