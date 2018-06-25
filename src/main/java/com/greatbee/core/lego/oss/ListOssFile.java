package com.greatbee.core.lego.oss;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.greatbee.base.util.DataUtil;
import com.greatbee.base.util.StringUtil;
import com.greatbee.core.lego.Input;
import com.greatbee.core.lego.LegoException;
import com.greatbee.core.lego.Output;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CreateOssStorage
 *
 * 创建oss存储空间 lego
 * 同一用户创建的存储空间总数不能超过30个
 * 存储空间一旦创建成功，名称、所处地域、存储类型不能修改。
 * 单个存储空间的容量不限制。
 * @author xiaobc
 * @date 18/6/21
 */
@Component("listOssFile")
public class ListOssFile extends OssBase{
    private static final Logger logger = Logger.getLogger(ListOssFile.class);

    private static final String Input_Key_Oss_Page = "page";
    private static final String Input_Key_Oss_PageSize = "pageSize";
    private static final String Input_Key_Oss_KeyPrefix = "keyPrefix";

    @Override
    public void execute(Input input, Output output) throws LegoException {
        //lego 处理逻辑
        String bucketName = input.getInputValue(Input_Key_Oss_Bucket_Name);
        if(StringUtil.isInvalid(bucketName)){
            throw new LegoException("OSS存储空间名称无效",ERROR_LEGO_OSS_Bucket_Name_Null);
        }
        //分页列举文件
        int page = DataUtil.getInt(input.getInputValue(Input_Key_Oss_Page), 1);
        int pageSize = DataUtil.getInt(input.getInputValue(Input_Key_Oss_PageSize), 100);
        if(page <= 0) {
            page = 1;
        }

        if(pageSize <= 0) {
            pageSize = 1;
        }
        //搜索前缀
        String keyPrefix = input.getInputValue(Input_Key_Oss_KeyPrefix);

        OSSClient ossClient = this.createClient(input);

        String nextMarker = null;
        ObjectListing objectListing;
        do {
            objectListing = ossClient.listObjects(new ListObjectsRequest(bucketName).
                    withPrefix(keyPrefix).withMarker(nextMarker).withMaxKeys(pageSize));
            List<OSSObjectSummary> sums = objectListing.getObjectSummaries();

            for (OSSObjectSummary s : sums) {
                //TODO 转换成data_page方式
                System.out.println("\t" + s.getKey());
            }
            nextMarker = objectListing.getNextMarker();
        } while (objectListing.isTruncated());

        //关闭ossClient
        this.closeClient(ossClient);

    }


}
