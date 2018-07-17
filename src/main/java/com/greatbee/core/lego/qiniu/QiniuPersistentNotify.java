package com.greatbee.core.lego.qiniu;

import com.greatbee.core.ExceptionCode;
import com.greatbee.core.lego.Input;
import com.greatbee.core.lego.Lego;
import com.greatbee.core.lego.LegoException;
import com.greatbee.core.lego.Output;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 七牛数据处理的回调lego
 *
 * @author xiaobc
 * @date 18/7/16
 */
@Component("qiniuPersistentNotify")
public class QiniuPersistentNotify implements ExceptionCode, Lego {

    private static final Logger logger = Logger.getLogger(QiniuPersistentNotify.class);

    private static final long ERROR_LEGO_NET_RESOURCE_SAVE_ERROR = 300055L;

    private static final String Output_Key_Net_Resource_File_Url = "net_file_url";//网络资源地址

    @Override
    public void execute(Input input, Output output) throws LegoException {
        //返回网络资源就好了
        HttpServletRequest request = input.getRequest();
        try {
            request.getInputStream();
            String line="";
            BufferedReader br=new BufferedReader(new InputStreamReader(
                    request.getInputStream()));
            StringBuilder sb = new StringBuilder();
            while((line = br.readLine())!=null){
                sb.append(line);
            }
            logger.info("===============================end！");
            logger.info(sb.toString());


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
