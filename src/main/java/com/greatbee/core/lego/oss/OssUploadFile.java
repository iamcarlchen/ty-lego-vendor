package com.greatbee.core.lego.oss;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.PutObjectRequest;
import com.greatbee.base.bean.DBException;
import com.greatbee.base.util.RandomGUIDUtil;
import com.greatbee.base.util.StringUtil;
import com.greatbee.core.bean.server.FileStorage;
import com.greatbee.core.lego.Input;
import com.greatbee.core.lego.LegoException;
import com.greatbee.core.lego.Output;
import com.greatbee.core.lego.util.LegoUtil;
import com.greatbee.core.manager.TYDriver;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * ossUploadFile
 *
 * OSS上传文件
 *
 * input:Input_Key_Oss_Endpoint,Input_Key_Oss_Access_Key_Id,Input_Key_Oss_Access_Key_Secret,Input_Key_Oss_Bucket_Name
 *
 * @author xiaobc
 * @date 18/6/21
 */
@Component("ossUploadFile")
public class OssUploadFile extends OssBase{
    private static final Logger logger = Logger.getLogger(OssUploadFile.class);

    private static final String Input_Key_File_Stream = "file_stream";


    private static final long Lego_Error_No_File_Need_To_Upload = 300027L;
    private static final long Lego_Error_File_Stream_Error = 300028L;

    private static final String Output_Key_File_Serialize_name = "file_serialize_name";
    private static final String Output_Key_File_Original_name = "file_original_name";
    private static final String Output_Key_File_Size = "file_size";
    private static final String Output_Key_File_Type = "file_type";
    private static final String Output_Key_File_Content_Type = "file_content_type";
    private static final String Output_Key_File_Storage = "file_storage";

    @Autowired
    private TYDriver tyDriver;

    @Override
    public void execute(Input input, Output output) throws LegoException {
        //lego 处理逻辑
        String bucketName = input.getInputValue(Input_Key_Oss_Bucket_Name);
        if(StringUtil.isInvalid(bucketName)){
            throw new LegoException("OSS存储空间名称无效",ERROR_LEGO_OSS_Bucket_Name_Null);
        }
        Object objectValue = input.getInputObjectValue(Input_Key_File_Stream);
        if(objectValue == null) {
            throw new LegoException("没有需要上传的文件", Lego_Error_No_File_Need_To_Upload);
        }
        String downloadUrl = input.getInputValue(Input_Key_File_Download_Url);
        Map params = buildTplParams(input);
        String url = LegoUtil.transferInputValue(downloadUrl, params);//附带参数可能需要模板

        //上传文件
        if(objectValue instanceof MultipartFile) {
            //创建oss对象
            OSSClient ossClient = this.createClient(input);

            MultipartFile file = (MultipartFile)objectValue;
            String originalName = file.getOriginalFilename();
            long fileSize = file.getSize();
            String fileType = originalName.split("\\.")[originalName.split("\\.").length - 1];
            String serializeName = RandomGUIDUtil.getRawGUID() + "." + fileType;
            String contentType = file.getContentType();

            //上传文件
            try {
                ossClient.putObject(new PutObjectRequest(bucketName, serializeName, file.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
                throw new LegoException("文件流错误",Lego_Error_File_Stream_Error);
            }

            output.setOutputValue(Output_Key_File_Original_name, originalName);
            output.setOutputValue(Output_Key_File_Serialize_name, serializeName);
            output.setOutputValue(Output_Key_File_Size, Long.valueOf(fileSize));
            output.setOutputValue(Output_Key_File_Type, fileType);
            output.setOutputValue(Output_Key_File_Content_Type, contentType);
            output.setOutputValue(Output_Key_File_Url, url + serializeName);

            FileStorage fileStorage = new FileStorage();
            fileStorage.setContentType(contentType);
            fileStorage.setFileSize(Long.valueOf(fileSize));
            fileStorage.setOiAlias(bucketName);//oiAlias 存 bucketName
            fileStorage.setOriginalName(originalName);
            fileStorage.setSerializeName(serializeName);
            fileStorage.setFileType(fileType);
            fileStorage.setFileUrl(url+serializeName);
            fileStorage.setUploadType("oss");
            try {
                this.tyDriver.getFileStorageManager().add(fileStorage);
            } catch (DBException e) {
                e.printStackTrace();
                throw new LegoException(e.getMessage(),e.getCode());
            }finally {
                this.closeClient(ossClient);
            }
            output.setOutputValue(Output_Key_File_Storage, fileStorage);
            this.closeClient(ossClient);
        }

    }


}
