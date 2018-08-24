package com.greatbee.core.lego.qiniu;

import com.greatbee.base.util.StringUtil;
import com.greatbee.core.ExceptionCode;
import com.greatbee.core.lego.Input;
import com.greatbee.core.lego.Lego;
import com.greatbee.core.lego.LegoException;
import com.greatbee.core.lego.Output;
import com.greatbee.core.lego.system.TYPPC;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;

/**
 * 网络资源转存为本地临时文件 eg: 七牛公有文件转成file对象  方便后面上传到oss
 *
 * @author xiaobc
 * @date 18/7/16
 */
@Component("netResourceToLocalFile")
public class NetResourceToLocalFile implements ExceptionCode, Lego {

    private static final Logger logger = Logger.getLogger(NetResourceToLocalFile.class);

    private static final long ERROR_LEGO_NET_RESOURCE_INVALIDATE = 300054L;
    private static final long ERROR_LEGO_NET_RESOURCE_SAVE_ERROR = 300055L;

    private static final String Input_Key_Net_Resource_Url = "resourceUrl";
    private static final String Input_Key_Net_Resource_FileName = "fileName";

    private static final String Output_Key_Net_Resource_Stream = "net_file_stream";//网络文件流

    private static final String Output_Key_Net_Resource_Content_Type = "contentType";//资源contentType

    @Override
    public void execute(Input input, Output output) throws LegoException {
        String resourceUrl = input.getInputValue(Input_Key_Net_Resource_Url);
        String fileName = input.getInputValue(Input_Key_Net_Resource_FileName);
        if(StringUtil.isInvalid(resourceUrl)){
            throw new LegoException("资源地址无效",ERROR_LEGO_NET_RESOURCE_INVALIDATE);
        }

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
            String locaPath = NetResourceToLocalFile.class.getResource("/").getPath();
            String tmpPath = TYPPC.d("upload.temp.dir");
            if(StringUtil.isValid(tmpPath)){
                File tmpFile = new File(tmpPath);
                if(!tmpFile.getParentFile().exists()){
                    tmpFile.getParentFile().mkdirs();
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

            output.setOutputValue(Output_Key_Net_Resource_Stream,new File(filePath));
            output.setOutputValue(Output_Key_Net_Resource_Content_Type,contentType);

        }  catch (IOException e) {
            e.printStackTrace();
            throw new LegoException("转存网络资源失败",ERROR_LEGO_NET_RESOURCE_SAVE_ERROR);
        }

    }

}
