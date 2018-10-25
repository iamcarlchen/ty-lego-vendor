package com.greatbee.core.lego.crypt;

import com.greatbee.core.ExceptionCode;
import com.greatbee.core.lego.Input;
import com.greatbee.core.lego.Lego;
import com.greatbee.core.lego.LegoException;
import com.greatbee.core.lego.Output;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * FyEnCrypt fy 接口加密
 *
 * @author xiaobc
 * @date 18/6/21
 */
@Component("fyEnCrypt")
public class FyEnCrypt implements ExceptionCode, Lego{
    private static final Logger logger = Logger.getLogger(FyEnCrypt.class);

    private static final String Input_Key_Static_3DESKey="static3DesKey";//静态的3deskey
    private static final String Input_Key_Rsa_Public_Key="rsaPublicKey";//对方 rsa 公钥
    private static final String Input_Key_Req_Data = "reqData";//请求对象   通过对象重组合并成接口需要的对象，  会将data转成xml字符串

    @Override
    public void execute(Input input, Output output) throws LegoException {
        //lego 处理逻辑


    }
}
