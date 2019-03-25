package com.greatbee.core.lego.basic;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.greatbee.base.bean.DBException;
import com.greatbee.base.bean.Data;
import com.greatbee.base.bean.DataList;
import com.greatbee.base.util.CollectionUtil;
import com.greatbee.base.util.DataUtil;
import com.greatbee.base.util.StringUtil;
import com.greatbee.core.ExceptionCode;
import com.greatbee.core.bean.constant.CT;
import com.greatbee.core.bean.constant.DBMT;
import com.greatbee.core.bean.constant.DT;
import com.greatbee.core.bean.constant.IOFT;
import com.greatbee.core.bean.oi.DS;
import com.greatbee.core.bean.oi.Field;
import com.greatbee.core.bean.oi.OI;
import com.greatbee.core.bean.server.APILego;
import com.greatbee.core.bean.server.InputField;
import com.greatbee.core.bean.server.OutputField;
import com.greatbee.core.bean.view.ConnectorTree;
import com.greatbee.core.db.DataManager;
import com.greatbee.core.db.RelationalDataManager;
import com.greatbee.core.lego.Input;
import com.greatbee.core.lego.Lego;
import com.greatbee.core.lego.LegoException;
import com.greatbee.core.lego.Output;
import com.greatbee.core.lego.util.LogUtil;
import com.greatbee.core.manager.TYDriver;
import com.greatbee.core.util.SpringContextUtil;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.greatbee.core.lego.util.BuildConnectorTreeUtils.buildConnectorTree;

/**
 * 批量 添加  更新  删除 先根据条件获取所有满足条件的数据，然后去匹配传过来的list数据，根据id 添加、更新、删除操作
 *  1、根据条件获取列表数据，比如某个物料的属性列表
 *  2、根据传过来的list_Param 判断传过来的数据中 有id的数据与第一步查询的所有数据id做比较，第一步数据中没在list_Param的数据进行删除
 *  3、其他没删除的数据，根据list_Param进行不同操作，如果传了id就更新，如果没传id 就新增
 *
 * <p>
 * 输入：
 * 1. 添加字段列表(多个,ioft=create)
 * 2. 更新字段列表(多个,ioft=update)
 * 3. 条件字段列表(多个,ioft=condition)
 * 输出：
 * 1. 添加后字段列表(多个,ioft = create)
 * 2. 更新后字段列表(多个,ioft = update)
 * 3. Output_Key_UniqueValue添加后的唯一值
 * Date: 2019/1/30
 */
@Component("batchSaveAndDelete")
public class BatchSaveAndDelete implements Lego, ExceptionCode {
    private static final Logger logger = Logger.getLogger(BatchSaveAndDelete.class);

    @Autowired
    private TYDriver tyDriver;

    private static final String Input_Key_List_Param = "list_Param";

    private static final String Output_Key_List_UniqueValue = "list_unique_value";

    @Override
    public void execute(Input input, Output output) throws LegoException {
        ApplicationContext wac = SpringContextUtil.getApplicationContext();

        String oiAlias = input.getApiLego().getOiAlias();
        InputField listParam = input.getInputField(Input_Key_List_Param);
        if(listParam.fieldValueToString()==null){
            return;
        }
        JSONArray ja = null;
        try {
           ja= JSONArray.parseArray(listParam.fieldValueToString());
        }catch(Exception e){
            e.printStackTrace();
            throw new LegoException("请求参数必须是有效的jsonString格式",300003);
        }

        //通过 oiAlias获取OI
        OI oi;
        try {
            oi = tyDriver.getTyCacheService().getOIByAlias(oiAlias);
        } catch (DBException e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
            throw new LegoException(e.getMessage(), e, e.getCode());
        }
        //通过oi.dsAlias获取DS
        DS ds;
        try {
            ds = tyDriver.getTyCacheService().getDSByAlias(oi.getDsAlias());
        } catch (DBException e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
            throw new LegoException(e.getMessage(), e, e.getCode());
        }
        //通过DST获取DataManager7
        DBMT dbmt = DBMT.getDBMT(ds.getDst());
        if (dbmt == null) {
            throw new LegoException(ds.getDst() + "数据源不支持", ERROR_DB_DST_NOT_SUPPORT);
        }
        DataManager dataManager = (DataManager) wac.getBean(dbmt.getType());

        //转成OI的Field
        List<List<Field>> createList = new ArrayList<List<Field>>();
        //需要更新的列
        List<UpdateBean> updateBeans = new ArrayList<UpdateBean>();
        //需要删除的列
        List<Field> deleteList = new ArrayList<>();

        //根据条件获取 查询到的数据
        List<Data> queryIdList = queryOldDataIdList(input,oi);
        logger.info("queryIdList="+ JSON.toJSONString(queryIdList));


        //获取需要添加的字段
        List<InputField> inputFields = input.getInputField(IOFT.Create);
        List<InputField> updateInputFields = input.getInputField(IOFT.Update);
        try {
            List<Field> oiFields = tyDriver.getTyCacheService().getFields(oiAlias);
            if (CollectionUtil.isValid(oiFields)) {

                if(CollectionUtil.isValid(queryIdList)){
                    Field pk = null;
                    for(int k=0;k<oiFields.size();k++){
                        if(oiFields.get(k).isPk()){
                            pk = oiFields.get(k);
                            break;
                        }
                    }
                    if(pk!=null){
                        for(int m=0;m<queryIdList.size();m++){
                            String id = queryIdList.get(m).getString("id");
                            boolean updateFlag = false;
                            for (int i = 0; i < ja.size(); i++) {
                                JSONObject jobj = ja.getJSONObject(i);
                                String pkV = null;
                                for (String key : jobj.keySet()) {
                                    if (pk != null && pk.getFieldName().equalsIgnoreCase(key) && StringUtil.isValid(jobj.getString(key))) {
                                        //主键字段
                                        pkV = jobj.getString(key);
                                        break;
                                    }
                                }
                                if(pkV!=null && id!=null && pkV.equalsIgnoreCase(id)){
                                    //参数有这个id 是更新
                                    updateFlag = true;
                                    break;
                                }
                            }
                            if(!updateFlag){
                                //遍历完 没有找到更新id值 说明是删除
                                Field deleteField = (Field) pk.clone();
                                deleteField.setFieldValue(id);
                                deleteList.add(deleteField);
                            }
                        }
                    }
                }

                logger.info("deleteList="+JSON.toJSONString(deleteList));

                for (int i = 0; i < ja.size(); i++) {
                    JSONObject jobj = ja.getJSONObject(i);
                    List<Field> createFieldsList = new ArrayList<Field>();

                    Field pkField = null;//主键字段
                    List<Field> updateFields = new ArrayList<Field>();
                    boolean isUpdate = false;

                    for (Field field : oiFields) {
                        InputField target = null;
                        InputField updatetTrget = null;
                        //创建字段
                        for (InputField inputField : inputFields) {
                            if (inputField.getFieldName().equalsIgnoreCase(field.getFieldName())) {
                                target = inputField;
                                break;
                            }
                        }
                        //更新字段
                        for (InputField inputField : updateInputFields) {
                            if (inputField.getFieldName().equalsIgnoreCase(field.getFieldName())) {
                                updatetTrget = inputField;
                                break;
                            }
                        }
                        //根据主键 更新
                        if (field.isPk()) {
                            pkField = (Field) field.clone();
                        }

                        for (String str : jobj.keySet()) {
                            if (pkField != null && pkField.getFieldName().equalsIgnoreCase(str) && StringUtil.isValid(jobj.getString(str))) {
                                //主键字段
                                pkField.setFieldValue(jobj.getString(str));
                                isUpdate = true;
                            }
                            //创建
                            if (target != null && target.getFieldName().equals(str)) {
                                //如果create字段 名  和 传进来的数组key相同，取其value
                                Field newField = (Field) field.clone();
                                newField.setFieldValue(jobj.getString(str));
                                //设置inputfiled的value，方便后面返回create组的字段值
                                if(StringUtil.isInvalid(jobj.getString(str))&&target.getFieldValue() != null&&!target.getFieldValue().equals("")){
                                    newField.setFieldValue(DataUtil.getString(target.getFieldValue(),""));
                                }
//                                else{
//                                    target.setFieldValue(jobj.getString(str));
//                                }
                                createFieldsList.add(newField);
                            }
                            //更新
                            if (updatetTrget != null && updatetTrget.getFieldName().equals(str)) {
                                //如果update字段 名  和 传进来的数组key相同，取其value
                                Field newField = (Field) field.clone();
                                newField.setFieldValue(jobj.getString(str));
                                //设置inputfiled的value，方便后面返回update组的字段值
                                updatetTrget.setFieldValue(jobj.getString(str));
                                updateFields.add(newField);
                            }
                        }
                        //针对从上一个节点或者http请求获取的数据填充
                        if (target != null && target.getFieldValue() != null&&!target.getFieldValue().equals("")&&!checkRepeat(createFieldsList,field.getFieldName())) {
                            Field newField = (Field) field.clone();
                            newField.setFieldValue(DataUtil.getString(target.getFieldValue(), ""));
                            createFieldsList.add(newField);
                        }
                        if (updatetTrget != null && updatetTrget.getFieldValue() != null&&!updatetTrget.getFieldValue().equals("")&&!checkRepeat(updateFields,field.getFieldName())) {
                            Field newField = (Field) field.clone();
                            newField.setFieldValue(DataUtil.getString(updatetTrget.getFieldValue(), ""));
                            updateFields.add(newField);
                        }

                    }
                    if (CollectionUtil.isValid(createFieldsList) && !isUpdate) {
                        createList.add(createFieldsList);
                    }

                    if (CollectionUtil.isValid(updateFields) && pkField != null && isUpdate) {
                        UpdateBean ub = new UpdateBean();
                        ub.setPkField(pkField);
                        ub.setFields(updateFields);
                        updateBeans.add(ub);
                    }
                }
            }
        } catch (DBException e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
            throw new LegoException(e.getMessage(), e, e.getCode());
        }

        if (CollectionUtil.isInvalid(createList) && CollectionUtil.isInvalid(updateBeans) && CollectionUtil.isInvalid(deleteList)) {
//            throw new LegoException("没有需要添加或者更新的字段", ERROR_LEGO_ADD_NO_FIELDS);
            logger.info("[batchSaveAndDelete] 没有需要添加、更新或者删除的字段");
            return ;
        }

        List<String> uniqueValueList = new ArrayList<String>();
        try {
            //创建   如果是更新就没有返回唯一值
            for (List<Field> fl : createList) {
                String uniqueValue = dataManager.create(oi, fl);
                uniqueValueList.add(uniqueValue);
            }
            //更新
            for (UpdateBean ub : updateBeans) {
                dataManager.update(oi, ub.getFields(), ub.getPkField());
            }

            //删除
            if(CollectionUtil.isValid(deleteList)){
                for (Field field: deleteList){
                    dataManager.delete(oi,field);
                }
            }

        } catch (DBException e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
            throw new LegoException(e, ERROR_LEGO_ADD);
        }

        //把添加后的唯一值写入进Output中
        output.setOutputValue(Output_Key_List_UniqueValue, uniqueValueList);


        if (CollectionUtil.isValid(createList)) {
            //字段组返回
            this.buildOutputFields(IOFT.Create, output, createList);
        }
        if (updateBeans != null && updateBeans.size() > 0) {
            //字段组返回
            this.buildOutputFields(IOFT.Update, output, updateBeans);
        }

        //添加日志
        LogUtil.saveLog(tyDriver, input, output, oi, "batchSaveAndDelete");

    }

    /**
     * 根据条件组装搜索
     * @param input
     * @param oi
     * @return
     * @throws LegoException
     */
    private List queryOldDataIdList(Input input,OI oi) throws LegoException {
        List<String> result = null;

        APILego queryApiLego = new APILego();
        queryApiLego.setOiAlias(oi.getAlias());
        queryApiLego.setLegoAlias("list");
        List<InputField> inputFields = input.getInputField(IOFT.Condition);
        InputField idIf = new InputField();
        idIf.setAlias("id");
        idIf.setFieldName("id");//查询id
        idIf.setCt(CT.EQ.getName());
        idIf.setIft(IOFT.Read.getType());
        idIf.setDt(DT.String.getType());
        inputFields.add(idIf);

        Input queryInput = new Input(input.getRequest(),input.getResponse());
        queryInput.setApiLego(queryApiLego);
        queryInput.setInputFields(inputFields);
        ConnectorTree root = buildConnectorTree(tyDriver, queryInput);
        RelationalDataManager dataManager = (RelationalDataManager) SpringContextUtil.getBean(DBMT.Mysql.getType());
        try {
            DataList dataList = dataManager.list(root);
            result = dataList.getList();
        } catch (DBException e) {
            e.printStackTrace();
            throw new LegoException(e.getMessage(),e.getCode());
        }
        return result;
    }

    /**
     * 校验list中是否存在某个字段
     * @param fields
     * @param fieldName
     * @return
     */
    private boolean checkRepeat(List<Field> fields,String fieldName){
        for(Field f:fields){
            if(f.getFieldName().equalsIgnoreCase(fieldName)){
                return true;
            }
        }
        return false;
    }

    /**
     * 构建list和page的多字段返回参数
     *
     * @param output
     * @param list
     */
    private void buildOutputFields(IOFT ioft, Output output, List list) {
        List<OutputField> outputFields = output.getOutputField(ioft);
        if (CollectionUtil.isValid(outputFields)) {
            for (OutputField outputField : outputFields) {
                List datas = new ArrayList();
                for (int i = 0; i < list.size(); i++) {
                    List<Field> row = null;
                    if (list.get(i) instanceof UpdateBean) {
                        row = ((UpdateBean) list.get(i)).getFields();
                    } else {
                        row = (List<Field>) list.get(i);
                    }
                    for (Field f : row) {
                        if (f.getFieldName().equalsIgnoreCase(outputField.getFieldName())) {
                            String outputFieldValue = f.getFieldValue();
                            datas.add(outputFieldValue);
                            break;
                        }
                    }
                }
                outputField.setFieldValue(datas);
            }
        }
    }

    class UpdateBean {
        private Field pkField;//主键字段
        private List<Field> fields;//需要更新的字段

        public Field getPkField() {
            return pkField;
        }

        public void setPkField(Field pkField) {
            this.pkField = pkField;
        }

        public List<Field> getFields() {
            return fields;
        }

        public void setFields(List<Field> fields) {
            this.fields = fields;
        }
    }
}
