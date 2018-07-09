package com.greatbee.core.lego.wx.util;

import com.alibaba.fastjson.JSONObject;
import com.greatbee.base.bean.DBException;
import com.greatbee.base.util.StringUtil;
import com.greatbee.core.lego.LegoException;
import com.greatbee.core.util.HttpClientUtil;
import org.apache.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.UUID;

/**
 * WxUtil jssdk
 *
 * @author xiaobc
 * @date 18/7/9
 */
public class WxUtil {

    private static final Logger logger = Logger.getLogger(WxUtil.class);

    protected static final long ERROR_LEFO_WX_Sign_Error = 300031L;

    private static final String WX_Open_Api_Host = "https://api.weixin.qq.com";

    private static String access_token;
    private static long lastUpdateToken = 0;
    //    private long updateInterval = 1000 * 7200;
    private static long updateInterval = 1000 * 1000;

    /**
     * ************************************ 下面是 jssdk 接口
     */

    /**
     * controller层调用：@RequestMapping(value = "/getSignature")
     * <p/>
     * 获取signature
     *
     * @return pipelineContext
     */
    public static JSONObject getSignature(String signUrl,String appId,String appSecret) throws DBException {
        //get ticket
        String jsapi_ticket = _getTicket(appId,appSecret);
        logger.info("[getSignature]jsapi_ticket" + jsapi_ticket);
        //get sign
        String nonce_str = _createNonceStr();
        String timestamp = _createTimestamp();
        String string1;
        String signature = "";
        //注意这里参数名必须全部小写，且必须有序
        string1 = "jsapi_ticket=" + jsapi_ticket +
                "&noncestr=" + nonce_str +
                "&timestamp=" + timestamp +
                "&url=" + signUrl;
        logger.info("[getSignature]signUrl=" + signUrl);

        try {
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(string1.getBytes("UTF-8"));
            //hex
            signature = byteToHex(crypt.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            logger.error(e);
            logger.error(e.getMessage());
            logger.error(e.toString());
            throw new DBException(e.getMessage(),ERROR_LEFO_WX_Sign_Error);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            logger.error(e);
            logger.error(e.getMessage());
            logger.error(e.toString());
            throw new DBException(e.getMessage(),ERROR_LEFO_WX_Sign_Error);
        }

        JSONObject sign = new JSONObject();
        sign.put("url", signUrl);
        sign.put("jsapi_ticket", jsapi_ticket);
        sign.put("nonceStr", nonce_str);
        sign.put("timestamp", timestamp);
        sign.put("signature", signature);
        sign.put("appId", appId);
        logger.info("[getSignature]signature=" + sign.toString());
        return sign;

    }

    /**
     * 获取ticket
     *
     * @return
     * @throws Exception
     */
    private static String _getTicket(String appId,String appSecret) throws DBException {
        String url = WX_Open_Api_Host + "/cgi-bin/ticket/getticket?access_token=" + getAccessTokenString(appId,appSecret)+"&type=jsapi";
        //get ticket
        String httpResponse = HttpClientUtil.get(url, null).getResponseBody();
        logger.info(httpResponse);
        if (StringUtil.isInvalid(httpResponse)) {
            logger.error("输出jsapi：" +"[SimpleWxEnterpriseManager][_getTicket][error] httpResponse is invalid!");
        }
        JSONObject jsonObject = JSONObject.parseObject(httpResponse);
        if (jsonObject.containsKey("ticket")) {
            return jsonObject.getString("ticket");
        } else {
            logger.error("[SimpleWxEnterpriseManager][_getTicket][error] httpResponse is invalid!");
            return null;
        }
    }

    /**
     * 获取accessToken的url
     *
     * @return
     */
    public static String getAccessTokenUrl(String appId,String appSecret) {
        StringBuilder urlBuilder = new StringBuilder(WX_Open_Api_Host);
        urlBuilder.append("/cgi-bin/token?");
        urlBuilder.append("grant_type=client_credential");
        urlBuilder.append("&appid=").append(appId);
        urlBuilder.append("&secret=").append(appSecret);
        return urlBuilder.toString();
    }

    /**
     * 只需要access_token有值就可以了，不需要返回，获取access_token值
     *
     * @throws LegoException
     */
    public static void initAccessToken(String appId,String appSecret) throws DBException {
        String url = getAccessTokenUrl(appId,appSecret);
        String httpResponse = HttpClientUtil.get(url,null).getResponseBody();
        if (StringUtil.isInvalid(httpResponse)) {
            logger.error("[SimpleWxEnterpriseManager][initAccessToken][error] httpResponse is invalid!");
        }
        JSONObject jsonObject = JSONObject.parseObject(httpResponse);
        if (jsonObject.containsKey("access_token")) {
            access_token = jsonObject.getString("access_token");
            //更新lastupdate时间戳,以免暴力请求导致sdk不能使用
            lastUpdateToken = System.currentTimeMillis();
            System.out.println(access_token);
        }
    }


    /**
     * 获取accesstoken的值，添加点击频率校验，以免暴力请求导致sdk不能使用
     *
     * @return
     * @throws Exception
     */
    public static String getAccessTokenString(String appId,String appSecret) throws DBException {
        if (accessTokenInvalid()) {
            initAccessToken(appId,appSecret);
        }
        return access_token;
    }
    /**
     * 校验，以免暴力请求导致sdk不能使用
     *
     * @return
     */
    private static boolean accessTokenInvalid() {
        if (StringUtil.isInvalid(access_token)) {
            return true;
        } else if (lastUpdateToken + updateInterval < System.currentTimeMillis()) {
            return true;
        }
        return false;
    }


    /**
     * 创建随机数
     *
     * @return
     */
    private static String _createNonceStr() {
        return UUID.randomUUID().toString();
    }

    /**
     * 创建当前时间戳
     *
     * @return
     */
    private static String _createTimestamp() {
        return Long.toString(System.currentTimeMillis() / 1000);
    }

    /**
     * 微信消息需要转码
     *
     * @param hash
     * @return
     */
    private static String byteToHex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }


}
