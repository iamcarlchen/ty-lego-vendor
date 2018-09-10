package com.greatbee.core.lego.utils;

import com.alibaba.fastjson.JSONObject;
import com.greatbee.base.util.StringUtil;
import com.greatbee.core.lego.LegoException;
import com.greatbee.core.lego.system.TYPPC;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;

/**
 * VendorUtil
 *
 * @author xiaobc
 * @date 18/9/10
 */
public class VendorUtil {

    private static final Logger logger = Logger.getLogger(VendorUtil.class);

    private static final long ERROR_LEGO_NET_RESOURCE_INVALIDATE = 300054L;
    private static final long ERROR_LEGO_NET_RESOURCE_SAVE_ERROR = 300055L;

    /**
     * 获取上传临时目录
     * @return
     */
    public static String getTmpPath() {
        String locaPath = TYPPC.d("upload.temp.dir");
        if (StringUtil.isInvalid(locaPath)) {
            locaPath = "/";
        }
        File tmpDir = new File(locaPath);
        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }
        return locaPath;
    }

    /**
     * 根据网络地址，保存文件到临时目录
     * @param resourceUrl
     * @param fileName
     * @return
     * @throws LegoException
     */
    public static JSONObject getLocalFileFromNetFile(String resourceUrl,String fileName) throws LegoException {
        JSONObject result = new JSONObject();
        try {
            URL url = new URL(resourceUrl);

            URLConnection uc = url.openConnection();
            //如果文件名没有传，直接从header中获取，4是微信获取文件名的header方式
            if(StringUtil.isInvalid(fileName)){
                fileName = uc.getHeaderField(4);
                if(StringUtil.isInvalid(fileName)){
                    throw new LegoException("资源地址无效,无法获取文件名",ERROR_LEGO_NET_RESOURCE_INVALIDATE);
                }
                fileName = URLDecoder.decode(fileName.substring(fileName.indexOf("filename=") + 9), "UTF-8");
                fileName = fileName.replaceAll("\"", "");//去掉文件名前后的引号
            }
            String contentType = uc.getHeaderField("Content-Type");//获取contentType

            logger.info("[NetResourceToLocalFile] fileName="+fileName);
            BufferedInputStream in = new BufferedInputStream(uc.getInputStream());
            //先将微信媒体文件存到本地
            String locaPath = VendorUtil.class.getResource("/").getPath();
            String tmpPath = TYPPC.d("upload.temp.dir");
            if(StringUtil.isValid(tmpPath)){
                File tmpFile = new File(tmpPath);
                if(!tmpFile.exists()){
                    tmpFile.mkdirs();
                }
                locaPath = tmpPath;
            }

            String filePath = locaPath+fileName;
            logger.info("[NetResourceToLocalFile] filePath="+filePath);
            File file =  new File(filePath);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
            byte[] buffer = null;
            buffer = new byte[2048];
            int length = in.read(buffer);
            while (length != -1) {
                out.write(buffer, 0, length);
                length = in.read(buffer);
            }
            in.close();
            out.close();

            result.put("filePath", filePath);
            result.put("contentType",contentType);

        }  catch (IOException e) {
            e.printStackTrace();
            throw new LegoException("转存网络资源失败",ERROR_LEGO_NET_RESOURCE_SAVE_ERROR);
        }
        return result;
    }

}
