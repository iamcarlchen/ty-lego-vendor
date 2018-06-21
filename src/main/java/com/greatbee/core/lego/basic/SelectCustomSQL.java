package com.greatbee.core.lego.basic;

import com.greatbee.core.ExceptionCode;
import com.greatbee.core.lego.Input;
import com.greatbee.core.lego.Lego;
import com.greatbee.core.lego.LegoException;
import com.greatbee.core.lego.Output;
import com.greatbee.core.manager.TYDriver;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * SelectCustomSQL
 *
 * 自定义查询SQL lego
 *
 * @author xiaobc
 * @date 18/6/21
 */
@Component("selectCustomSQL")
public class SelectCustomSQL implements ExceptionCode, Lego{
    private static final Logger logger = Logger.getLogger(SelectCustomSQL.class);
    @Autowired
    private TYDriver tyDriver;


    @Override
    public void execute(Input input, Output output) throws LegoException {
        //lego 处理逻辑
        
    }

}
