package com.greatbee.core.utils;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;
import org.apache.log4j.Logger;

import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;

/**
 * WsUtil
 *
 *  web service client 工具类
 * @author xiaobc
 * @date 18/10/24
 */
public class WsUtil {

    private static Logger logger = Logger.getLogger(WsUtil.class);

    /**
     * web service调用
     * @param url
     * @param xmlData   xml的请求参数  body内容  默认参数是requestXML
     * @param targetNamespace
     * @param soapAction
     * @param method
     * @return
     */
    public static String invokeWsdl(String url,String xmlData,String targetNamespace,String soapAction,String method){
        Service service = new Service();
        try {
            Call call2 = (Call) service.createCall();
            call2.setTargetEndpointAddress(url);
            call2.setUseSOAPAction(true);
            call2.setReturnType(new QName("http://www.w3.org/2001/XMLSchema", "string"));//设置返回参数
            call2.setOperationName(new QName(targetNamespace, method));//设置函数名
            call2.setSOAPActionURI(soapAction);//设置URI
            call2.addParameter(new QName(targetNamespace, "requestXML"), XMLType.XSD_STRING, ParameterMode.IN);  // 这里设置对应参数名称
            String retVal2 = (String) call2.invoke(new Object[] { xmlData });  //调用并带上参数数据
            System.out.println("[invokeWsdl] result = "+retVal2);
            return retVal2;
        } catch(Exception e) {
            e.printStackTrace();
            logger.error("[invokeWsdl] web service 调用失败!"+e.getMessage());
        }
        return null;
    }


    //    测试
    public static void main(String[] args) {
        String encodeParams = "{\"SignKey\":\"hXyRG9Qp2YtNANnthUb2XNvp9hvG+Lvf4Y9Y12EEZwzkIEbz7GScu99u59i8tJEZ7f4YeQ nPGvMB8aW7OH+KqPpwgihXyciWhrhJX/o2qKA1BZeSXcZ+HwWqzLTOVKrc1uyepddZ1198+kL3B5I aF0H6aEvqRhJrtC7pKAU9HDA=\",\"Data\":\"t9tG8IfiymMC9NpKxYGUneyRyZz/2b+XiCkPlkPNW+HruF JAwWbMPGQdz7uq44YoPJkaHUJiKs0uC3vhpBMFQsJsAGr/ws1yKTMaC+mLIP7S5Hv78rGUQkx8ld/ It+ml34gisZc990dmMa7UY3AtawYoiHqBtpNMhI3cjOPWUvNTWhg4hml1JOg8BIoeeL4WePlIPcyb8 +T/MqN93szwHfpDdFN6Mrqreb8fGLUWi8hihI6Q7Da+1hFyKQPGN/48rfvdghhJWqv8K+OH+WVeb uN5/cYCeK9LEGxpDgCvthI=\",\"CustomerCode\":\"0010010000\"}";

        String url = "http://61.140.21.164:65501/GDTest/EOPartnerService.svc?wsdl" ;
        String xmlData= encodeParams;
        Service service2 = new Service();

        try {
            Call call2 = (Call) service2.createCall();
            call2.setTargetEndpointAddress(url);
            call2.setUseSOAPAction(true);
            call2.setReturnType(new QName("http://www.w3.org/2001/XMLSchema", "string"));//设置返回参数
            call2.setOperationName(new QName("http://tempuri.org/", "CouponQuery"));//设置函数名
            call2.setSOAPActionURI("http://tempuri.org/IEOPartnerService/CouponQuery");//设置URI
            call2.addParameter(new QName("http://tempuri.org/", "requestXML"), XMLType.XSD_STRING, ParameterMode.IN);  // 这里设置对应参数名称
            String retVal2 = (String) call2.invoke(new Object[] { xmlData });  //调用并带上参数数据
            System.out.println(retVal2);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
