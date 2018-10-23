package com.greatbee.core.utils;

import org.dom4j.Document;
import org.dom4j.Element;

import java.util.*;


/**
 * xml和map相互转换的工具类
 * XmlUtil
 *
 * @author xiaobc
 * @date 18/10/23
 */
public class XmlUtil {

    private static String XML_TAG = "<?xml version='1.0' encoding='UTF-8'?>";


    public static Map<String, Object> Dom2Map(Document doc) {
        Map<String, Object> map = new HashMap<String, Object>();
        if (doc == null)
            return map;
        Element root = doc.getRootElement();
        for (Iterator iterator = root.elementIterator(); iterator.hasNext(); ) {
            Element e = (Element) iterator.next();
            List list = e.elements();
            if (list.size() > 0) {
                map.put(e.getName(), Dom2Map(e));
            } else
                map.put(e.getName(), e.getText());
        }
        return map;
    }

    public static Map Dom2Map(Element e) {
        Map map = new HashMap();
        List list = e.elements();
        if (list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                Element iter = (Element) list.get(i);
                List mapList = new ArrayList();

                if (iter.elements().size() > 0) {
                    Map m = Dom2Map(iter);
                    if (map.get(iter.getName()) != null) {
                        Object obj = map.get(iter.getName());
                        if (!obj.getClass().getName()
                                .equals("java.util.ArrayList")) {
                            mapList = new ArrayList();
                            mapList.add(obj);
                            mapList.add(m);
                        }
                        if (obj.getClass().getName()
                                .equals("java.util.ArrayList")) {
                            mapList = (List) obj;
                            mapList.add(m);
                        }
                        map.put(iter.getName(), mapList);
                    } else
                        map.put(iter.getName(), m);
                } else {
                    if (map.get(iter.getName()) != null) {
                        Object obj = map.get(iter.getName());
                        if (!obj.getClass().getName()
                                .equals("java.util.ArrayList")) {
                            mapList = new ArrayList();
                            mapList.add(obj);
                            mapList.add(iter.getText());
                        }
                        if (obj.getClass().getName()
                                .equals("java.util.ArrayList")) {
                            mapList = (List) obj;
                            mapList.add(iter.getText());
                        }
                        map.put(iter.getName(), mapList);
                    } else
                        map.put(iter.getName(), iter.getText());
                }
            }
        } else
            map.put(e.getName(), e.getText());
        return map;
    }

    /**
     * Map 转 XML
     *
     * @param map
     * @return
     */
    public static String mapToXml(Map map) {
        System.out.println("将Map转成Xml, Map：" + map.toString());
        StringBuffer sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>");
        mapToXml(map, sb);
        sb.append("</root>");
        System.out.println("将Map转成Xml, Xml：" + sb.toString());
        return sb.toString();
    }

    private static void mapToXml(Map map, StringBuffer sb) {
        Set set = map.keySet();
        for (Iterator it = set.iterator(); it.hasNext(); ) {
            String key = (String) it.next();
            Object value = map.get(key);
            if (null == value)
                value = "";
            if (value.getClass().getName().equals("java.util.ArrayList")) {
                ArrayList list = (ArrayList) map.get(key);
                for (int i = 0; i < list.size(); i++) {
                    sb.append("<" + key + ">");
                    Object item = list.get(i);
                    if(isObject(item)){
                        HashMap hm = (HashMap) list.get(i);
                        mapToXml(hm, sb);
                    }else{
                        sb.append(item);
                    }
                    sb.append("</" + key + ">");
                }
            } else {
                if (value instanceof HashMap) {
                    sb.append("<" + key + ">");
                    mapToXml((HashMap) value, sb);
                    sb.append("</" + key + ">");
                } else {
                    sb.append("<" + key + ">" + value + "</" + key + ">");
                }

            }

        }
    }

    /**
     * Checks if is object.
     *
     * @param obj the obj
     * @return true, if is object
     */
    private static boolean isObject(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof String) {
            return false;
        }
        if (obj instanceof Integer) {
            return false;
        }
        if (obj instanceof Double) {
            return false;
        }
        if (obj instanceof Float) {
            return false;
        }
        if (obj instanceof Byte) {
            return false;
        }
        if (obj instanceof Long) {
            return false;
        }
        if (obj instanceof Character) {
            return false;
        }
        if (obj instanceof Short) {
            return false;
        }
        if (obj instanceof Boolean) {
            return false;
        }
        return true;
    }

//    public static void main(String[] args) throws Exception {
//        String message = "<?xml version=\"1.0\" encoding=\"utf-8\" ?> " +
//                "<req>\n" +
//                "   <CustomerCode>客户编码</CustomerCode>\n" +
//                "   <CouponNo>石化兑换券Id</CouponNo>\n" +
//                "   <s>123</s>\n" +
//                "   <bb>11</bb>\n" +
//                "</req>";
//
//        Document doc = DocumentHelper.parseText(message);
//        Map obj = Dom2Map(doc);
//        System.out.println(obj);
//        String e = mapToXml(obj);
//        System.out.println(e);
//    }

}
