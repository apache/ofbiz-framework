/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.ofbiz.content.survey;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.content.data.DataResourceWorker;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.ByteWrapper;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfObject;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;


/**
 * PdfSurveyServices Class
 */

public class PdfSurveyServices {
    
    public static final String module = PdfSurveyServices.class.getName();

    /**
     * 
     */
    public static Map buildSurveyFromPdf(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        String surveyId = null;
        try {
            String surveyName = (String) context.get("surveyName");
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ByteWrapper byteWrapper = getInputByteWrapper(context, delegator);
            PdfReader pdfReader = new PdfReader(byteWrapper.getBytes());
            PdfStamper pdfStamper = new PdfStamper(pdfReader, os);
            AcroFields acroFields = pdfStamper.getAcroFields();
            HashMap acroFieldMap = acroFields.getFields();
            
            String contentId = (String) context.get("contentId");
            GenericValue survey = null;
            surveyId = (String) context.get("surveyId");
            if (UtilValidate.isEmpty(surveyId)) {
                surveyId = delegator.getNextSeqId("Survey");
                survey = delegator.makeValue("Survey", UtilMisc.toMap("surveyName", surveyName));
                survey.set("surveyId", surveyId);
                survey.set("allowMultiple", "Y");
                survey.set("allowUpdate", "Y");
                survey.create();
            }
            
            // create a SurveyQuestionCategory to put the questions in
            Map createCategoryResultMap = dispatcher.runSync("createSurveyQuestionCategory", 
                    UtilMisc.toMap("description", "From AcroForm in Content [" + contentId + "] for Survey [" + surveyId + "]", "userLogin", userLogin));
            String surveyQuestionCategoryId = (String) createCategoryResultMap.get("surveyQuestionCategoryId");
            
            pdfStamper.setFormFlattening(true);
            Iterator i = acroFieldMap.keySet().iterator();
            while (i.hasNext()) {
                String fieldName = (String) i.next();
                AcroFields.Item item = acroFields.getFieldItem(fieldName);
                int type = acroFields.getFieldType(fieldName);
                String value = acroFields.getField(fieldName);
                Debug.logInfo("fieldName:" + fieldName + "; item: " + item + "; value: " + value, module);

                GenericValue surveyQuestion = delegator.makeValue("SurveyQuestion", UtilMisc.toMap("question", fieldName));
                String surveyQuestionId = delegator.getNextSeqId("SurveyQuestion");
                surveyQuestion.set("surveyQuestionId", surveyQuestionId);
                surveyQuestion.set("surveyQuestionCategoryId", surveyQuestionCategoryId);

                if (type == AcroFields.FIELD_TYPE_TEXT) {
                    surveyQuestion.set("surveyQuestionTypeId", "TEXT_SHORT");
                } else if (type == AcroFields.FIELD_TYPE_RADIOBUTTON) {
                    surveyQuestion.set("surveyQuestionTypeId", "OPTION");
                } else if (type == AcroFields.FIELD_TYPE_LIST || type == AcroFields.FIELD_TYPE_COMBO) {
                    surveyQuestion.set("surveyQuestionTypeId", "OPTION");
                    // TODO: handle these specially with the acroFields.getListOptionDisplay (and getListOptionExport?)
                    String[] listOptionDisplayArray = acroFields.getListOptionDisplay(fieldName);
                    String[] listOptionExportArray = acroFields.getListOptionExport(fieldName);
                    Debug.logInfo("listOptionDisplayArray: " + listOptionDisplayArray + "; listOptionExportArray: " + listOptionExportArray, module);
                } else {
                    surveyQuestion.set("surveyQuestionTypeId", "TEXT_SHORT");
                    Debug.logWarning("Building Survey from PDF, fieldName=[" + fieldName + "]: don't know how to handle field type: " + type + "; defaulting to short text", module);
                }
                
                // ==== create a good sequenceNum based on tab order or if no tab order then the page location
                
                Integer tabPage = (Integer) item.page.get(0);
                Integer tabOrder = (Integer) item.tabOrder.get(0);
                Debug.logInfo("tabPage=" + tabPage + ", tabOrder=" + tabOrder, module);
                
                //array of float  multiple of 5. For each of this groups the values are: [page, llx, lly, urx, ury]
                float[] fieldPositions = acroFields.getFieldPositions(fieldName);
                float fieldPage = fieldPositions[0];
                float fieldLlx = fieldPositions[1];
                float fieldLly = fieldPositions[2];
                float fieldUrx = fieldPositions[3];
                float fieldUry = fieldPositions[4];
                Debug.logInfo("fieldPage=" + fieldPage + ", fieldLlx=" + fieldLlx + ", fieldLly=" + fieldLly + ", fieldUrx=" + fieldUrx + ", fieldUry=" + fieldUry, module);

                Long sequenceNum = null;
                if (tabPage != null && tabOrder != null) {
                    sequenceNum = new Long(tabPage.intValue() * 1000 + tabOrder.intValue());
                    Debug.logInfo("tabPage=" + tabPage + ", tabOrder=" + tabOrder + ", sequenceNum=" + sequenceNum, module);
                } else if (fieldPositions.length > 0) {
                    sequenceNum = new Long((long) fieldPage * 10000 + (long) fieldLly * 1000 + (long) fieldLlx);
                    Debug.logInfo("fieldPage=" + fieldPage + ", fieldLlx=" + fieldLlx + ", fieldLly=" + fieldLly + ", fieldUrx=" + fieldUrx + ", fieldUry=" + fieldUry + ", sequenceNum=" + sequenceNum, module);
                }
                
                // TODO: need to find something better to put into these fields...
                String annotation = null;
                Iterator widgetIter = item.widgets.iterator();
                while (widgetIter.hasNext()) {
                    PdfDictionary dict = (PdfDictionary) widgetIter.next();
                    
                    // if the "/Type" value is "/Annot", then get the value of "/TU" for the annotation
                    
                    /* Interesting... this doesn't work, I guess we have to iterate to find the stuff...
                    PdfObject typeValue = dict.get(new PdfName("/Type"));
                    if (typeValue != null && "/Annot".equals(typeValue.toString())) {
                        PdfObject tuValue = dict.get(new PdfName("/TU"));
                        annotation = tuValue.toString();
                    }
                    */
                    
                    PdfObject typeValue = null;
                    PdfObject tuValue = null;
                    
                    Set dictKeys = dict.getKeys();
                    Iterator dictKeyIter = dictKeys.iterator();
                    while (dictKeyIter.hasNext()) {
                        PdfName dictKeyName = (PdfName) dictKeyIter.next();
                        PdfObject dictObject = dict.get(dictKeyName);
                        
                        if ("/Type".equals(dictKeyName.toString())) {
                            typeValue = dictObject;
                        } else if ("/TU".equals(dictKeyName.toString())) {
                            tuValue = dictObject;
                        }
                        //Debug.logInfo("AcroForm widget fieldName[" + fieldName + "] dictKey[" + dictKeyName.toString() + "] dictValue[" + dictObject.toString() + "]", module);
                    }
                    if (tuValue != null && typeValue != null && "/Annot".equals(typeValue.toString())) {
                        annotation = tuValue.toString();
                    }
                }
                
                surveyQuestion.set("description", fieldName);
                if (UtilValidate.isNotEmpty(annotation)) {
                    surveyQuestion.set("question", annotation);
                } else {
                    surveyQuestion.set("question", fieldName);
                }

                GenericValue surveyQuestionAppl = delegator.makeValue("SurveyQuestionAppl", UtilMisc.toMap("surveyId", surveyId, "surveyQuestionId", surveyQuestionId));
                surveyQuestionAppl.set("fromDate", nowTimestamp);
                surveyQuestionAppl.set("externalFieldRef", fieldName);

                if (sequenceNum != null) {
                    surveyQuestionAppl.set("sequenceNum", sequenceNum);
                }

                surveyQuestion.create();
                surveyQuestionAppl.create();
            }            
            pdfStamper.close();
            if (UtilValidate.isNotEmpty(contentId)) {
                survey = delegator.findByPrimaryKey("Survey", UtilMisc.toMap("surveyId", surveyId));
                survey.set("acroFormContentId", contentId);
                survey.store();
            }
        } catch (GenericEntityException e) {
            String errMsg = "Error generating PDF: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch(GeneralException e) {
            System.err.println(e.getMessage());
            ServiceUtil.returnError(e.getMessage());
        } catch (Exception e) {
            String errMsg = "Error generating PDF: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        
        Map results = ServiceUtil.returnSuccess();
        results.put("surveyId", surveyId);
        return results;
    }
    
    /**
     * 
     */
    public static Map buildSurveyResponseFromPdf(DispatchContext dctx, Map context) {

        String surveyResponseId = null;
        try {
            
            GenericDelegator delegator = dctx.getDelegator();
            String partyId = (String)context.get("partyId");
            String surveyId = (String)context.get("surveyId");
            //String contentId = (String)context.get("contentId");
            surveyResponseId = (String)context.get("surveyResponseId");
            if (UtilValidate.isNotEmpty(surveyResponseId)) {
                GenericValue surveyResponse = delegator.findByPrimaryKey("SurveyResponse", UtilMisc.toMap("surveyResponseId", surveyResponseId));
                if (surveyResponse != null) {
                    surveyId = surveyResponse.getString("surveyId");
                }
            } else {
                surveyResponseId = delegator.getNextSeqId("SurveyResponse");
                GenericValue surveyResponse = delegator.makeValue("SurveyResponse", UtilMisc.toMap("surveyResponseId", surveyResponseId, "surveyId", surveyId, "partyId", partyId));
                surveyResponse.set("responseDate", UtilDateTime.nowTimestamp());
                surveyResponse.set("lastModifiedDate", UtilDateTime.nowTimestamp());
                surveyResponse.create();
            }
            
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ByteWrapper byteWrapper = getInputByteWrapper(context, delegator);
            PdfReader r = new PdfReader(byteWrapper.getBytes());
            PdfStamper s = new PdfStamper(r,os);
            AcroFields fs = s.getAcroFields();
            HashMap hm = fs.getFields();
            
            
            s.setFormFlattening(true);
            Iterator i = hm.keySet().iterator();
            while (i.hasNext()) {
                String fieldName = (String)i.next();
                //AcroFields.Item item = fs.getFieldItem(fieldName);
                //int type = fs.getFieldType(fieldName);
                String value = fs.getField(fieldName);
                
                List questions = delegator.findByAnd("SurveyQuestionAndAppl", UtilMisc.toMap("surveyId", surveyId, "externalFieldRef", fieldName));
                if (questions.size() == 0 ) {
                    Debug.logInfo("No question found for surveyId:" + surveyId + " and externalFieldRef:" + fieldName, module);
                    continue;
                }
                
                GenericValue surveyQuestionAndAppl = (GenericValue)questions.get(0);
                String surveyQuestionId = (String)surveyQuestionAndAppl.get("surveyQuestionId");
                String surveyQuestionTypeId = (String)surveyQuestionAndAppl.get("surveyQuestionTypeId");
                GenericValue surveyResponseAnswer = delegator.makeValue("SurveyResponseAnswer", UtilMisc.toMap("surveyResponseId", surveyResponseId, "surveyQuestionId", surveyQuestionId));
                if (surveyQuestionTypeId ==null || surveyQuestionTypeId.equals("TEXT_SHORT")) {
                    surveyResponseAnswer.set("textResponse", value);
                }

                delegator.create(surveyResponseAnswer);
            }            
            s.close();
        } catch (GenericEntityException e) {
            String errMsg = "Error generating PDF: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } catch(GeneralException e) {
            System.err.println(e.getMessage());
            ServiceUtil.returnError(e.getMessage());
        } catch (Exception e) {
            String errMsg = "Error generating PDF: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        
        Map results = ServiceUtil.returnSuccess();
        results.put("surveyResponseId", surveyResponseId);
        return results;
    }

    /**
     */
    public static Map getAcroFieldsFromPdf(DispatchContext dctx, Map context) {
        
        Map acroFieldMap = new HashMap();
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            GenericDelegator delegator = dctx.getDelegator();
            ByteWrapper byteWrapper = getInputByteWrapper(context, delegator);
            PdfReader r = new PdfReader(byteWrapper.getBytes());
            PdfStamper s = new PdfStamper(r,os);
            AcroFields fs = s.getAcroFields();
            HashMap map = fs.getFields();
            
            s.setFormFlattening(true);
            
            // Debug code to get the values for setting TDP
    //        String[] sa = fs.getAppearanceStates("TDP");
    //        for (int i=0;i<sa.length;i++)
    //            Debug.log("Appearance="+sa[i]);
            
            Iterator iter = map.keySet().iterator();
            while (iter.hasNext()) {
                String fieldName=(String)iter.next();
                String parmValue = fs.getField(fieldName);
                acroFieldMap.put(fieldName, parmValue);
            }            
                 
        } catch(DocumentException e) {
            System.err.println(e.getMessage());
            ServiceUtil.returnError(e.getMessage());
        } catch(GeneralException e) {
            System.err.println(e.getMessage());
            ServiceUtil.returnError(e.getMessage());
        } catch(IOException ioe) {
            System.err.println(ioe.getMessage());
            ServiceUtil.returnError(ioe.getMessage());
        }
        
    Map results = ServiceUtil.returnSuccess();
    results.put("acroFieldMap", acroFieldMap);
    return results;
    }
    
    /**
     */
    public static Map setAcroFields(DispatchContext dctx, Map context) {
        
        Map results = ServiceUtil.returnSuccess();
        GenericDelegator delegator = dctx.getDelegator();
        try {
            Map acroFieldMap = (Map)context.get("acroFieldMap");
            ByteWrapper byteWrapper = getInputByteWrapper(context, delegator);
            PdfReader r = new PdfReader(byteWrapper.getBytes());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfStamper s = new PdfStamper(r, baos);
            AcroFields fs = s.getAcroFields();
            Map map = fs.getFields();
            
            s.setFormFlattening(true);
            
            // Debug code to get the values for setting TDP
    //      String[] sa = fs.getAppearanceStates("TDP");
    //      for (int i=0;i<sa.length;i++)
    //          Debug.log("Appearance="+sa[i]);
            
            Iterator iter = map.keySet().iterator();
            while (iter.hasNext()) {
                String fieldName=(String)iter.next();
                String fieldValue = fs.getField(fieldName);
                Object obj = acroFieldMap.get(fieldName);
                if (obj instanceof Date) {
                    Date d=(Date)obj;
                    fieldValue=UtilDateTime.toDateString(d);
                } else if (obj instanceof Long) {
                    Long lg=(Long)obj;
                    fieldValue=lg.toString();
                } else if (obj instanceof Integer) {
                    Integer ii=(Integer)obj;
                    fieldValue=ii.toString();
                }   else {
                    fieldValue=(String)obj;
                }
            
                if (UtilValidate.isNotEmpty(fieldValue))
                    fs.setField(fieldName, fieldValue);
            }      
                 
            s.close();
            baos.close();
            ByteWrapper outByteWrapper = new ByteWrapper(baos.toByteArray());
            results.put("outByteWrapper", outByteWrapper);
        } catch(DocumentException e) {
            System.err.println(e.getMessage());
            ServiceUtil.returnError(e.getMessage());
        } catch(GeneralException e) {
            System.err.println(e.getMessage());
            ServiceUtil.returnError(e.getMessage());
        } catch(FileNotFoundException e) {
            System.err.println(e.getMessage());
            ServiceUtil.returnError(e.getMessage());
        } catch(IOException ioe) {
            System.err.println(ioe.getMessage());
            ServiceUtil.returnError(ioe.getMessage());
        } catch(Exception ioe) {
            System.err.println(ioe.getMessage());
            ServiceUtil.returnError(ioe.getMessage());
        }
        
    return results;
    }
    
    
    /**
     */
    public static Map buildPdfFromSurveyResponse(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        //LocalDispatcher dispatcher = dctx.getDispatcher();
        Map results = ServiceUtil.returnSuccess();
        String surveyResponseId = (String)context.get("surveyResponseId");
        String contentId = (String)context.get("contentId");
        String surveyId = null;

        Document document = new Document();
        try {
            if (UtilValidate.isNotEmpty(surveyResponseId)) {
                GenericValue surveyResponse = delegator.findByPrimaryKey("SurveyResponse", UtilMisc.toMap("surveyResponseId", surveyResponseId));
                if (surveyResponse != null) {
                    surveyId = surveyResponse.getString("surveyId");
                }
            }
            if (UtilValidate.isNotEmpty(surveyId) && UtilValidate.isEmpty(contentId)) {
                GenericValue survey = delegator.findByPrimaryKey("Survey", UtilMisc.toMap("surveyId", surveyId));
                if (survey != null) {
                    String acroFormContentId = survey.getString("acroFormContentId");
                    if (UtilValidate.isNotEmpty(acroFormContentId)) {
                        context.put("contentId", acroFormContentId);
                    }
                }
            }
        
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            
            List responses = delegator.findByAnd("SurveyResponseAnswer", UtilMisc.toMap("surveyResponseId", surveyResponseId));
            Iterator iter = responses.iterator();
            while (iter.hasNext()) {
                String value = null;
                GenericValue surveyResponseAnswer = (GenericValue) iter.next();
                String surveyQuestionId = (String) surveyResponseAnswer.get("surveyQuestionId");
                GenericValue surveyQuestion = delegator.findByPrimaryKey("SurveyQuestion", UtilMisc.toMap("surveyQuestionId", surveyQuestionId));
                String questionType = surveyQuestion.getString("surveyQuestionTypeId");
                // DEJ20060227 this isn't used, if needed in the future should get from SurveyQuestionAppl.externalFieldRef String fieldName = surveyQuestion.getString("description");
                if ("OPTION".equals(questionType)) {
                    value = surveyResponseAnswer.getString("surveyOptionSeqId");
                } else if ("BOOLEAN".equals(questionType)) {
                    value = surveyResponseAnswer.getString("booleanResponse");
                } else if ("NUMBER_LONG".equals(questionType) || "NUMBER_CURRENCY".equals(questionType) || "NUMBER_FLOAT".equals(questionType)) {
                    Double num = surveyResponseAnswer.getDouble("numericResponse");
                    if (num != null) {
                        value = num.toString();
                    }
                } else if ("SEPERATOR_LINE".equals(questionType) || "SEPERATOR_TEXT".equals(questionType)) {
                    // not really a question; ingore completely
                } else {
                    value = surveyResponseAnswer.getString("textResponse");
                }
                Chunk chunk = new Chunk(surveyQuestion.getString("question") + ": " + value);
                Paragraph p = new Paragraph(chunk);
                document.add(p);
            }
            ByteWrapper outByteWrapper = new ByteWrapper(baos.toByteArray());
            results.put("outByteWrapper", outByteWrapper);
        } catch (GenericEntityException e) {
            System.err.println(e.getMessage());
            ServiceUtil.returnError(e.getMessage());
        } catch (DocumentException e) {
            System.err.println(e.getMessage());
            ServiceUtil.returnError(e.getMessage());
        }
        
        return results;
    }
    
    /**
     */
    public static Map setAcroFieldsFromSurveyResponse(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map results = ServiceUtil.returnSuccess();
        Map acroFieldMap = new HashMap();
        String surveyResponseId = (String)context.get("surveyResponseId");
        String acroFormContentId = null;
    
        try {
            String surveyId = null;
            if (UtilValidate.isNotEmpty(surveyResponseId)) {
                GenericValue surveyResponse = delegator.findByPrimaryKey("SurveyResponse", UtilMisc.toMap("surveyResponseId", surveyResponseId));
                if (surveyResponse != null) {
                    surveyId = surveyResponse.getString("surveyId");
                }
            }

            if (UtilValidate.isNotEmpty(surveyId)) {
                GenericValue survey = delegator.findByPrimaryKey("Survey", UtilMisc.toMap("surveyId", surveyId));
                if (survey != null) {
                    acroFormContentId = survey.getString("acroFormContentId");
                }
            }
            
            List responses = delegator.findByAnd("SurveyResponseAnswer", UtilMisc.toMap("surveyResponseId", surveyResponseId));
            Iterator iter = responses.iterator();
            while (iter.hasNext()) {
                String value = null;
                GenericValue surveyResponseAnswer = (GenericValue) iter.next();
                String surveyQuestionId = (String) surveyResponseAnswer.get("surveyQuestionId");

                GenericValue surveyQuestion = delegator.findByPrimaryKeyCache("SurveyQuestion", UtilMisc.toMap("surveyQuestionId", surveyQuestionId));
                
                List surveyQuestionApplList = EntityUtil.filterByDate(delegator.findByAndCache("SurveyQuestionAppl", UtilMisc.toMap("surveyId", surveyId, "surveyQuestionId", surveyQuestionId), UtilMisc.toList("-fromDate")), false);
                GenericValue surveyQuestionAppl = EntityUtil.getFirst(surveyQuestionApplList);
                
                String questionType = surveyQuestion.getString("surveyQuestionTypeId");
                String fieldName = surveyQuestionAppl.getString("externalFieldRef");
                if ("OPTION".equals(questionType)) {
                    value = surveyResponseAnswer.getString("surveyOptionSeqId");
                } else if ("BOOLEAN".equals(questionType)) {
                    value = surveyResponseAnswer.getString("booleanResponse");
                } else if ("NUMBER_LONG".equals(questionType) || "NUMBER_CURRENCY".equals(questionType) || "NUMBER_FLOAT".equals(questionType)) {
                    Double num = surveyResponseAnswer.getDouble("numericResponse");
                    if (num != null) {
                        value = num.toString();
                    }
                } else if ("SEPERATOR_LINE".equals(questionType) || "SEPERATOR_TEXT".equals(questionType)) {
                    // not really a question; ingore completely
                } else {
                    value = surveyResponseAnswer.getString("textResponse");
                }
                acroFieldMap.put(fieldName, value);
            }
        } catch (GenericEntityException e) {
            System.err.println(e.getMessage());
            ServiceUtil.returnError(e.getMessage());
        }
        
        try {
            ModelService modelService = dispatcher.getDispatchContext().getModelService("setAcroFields");
            Map ctx = modelService.makeValid(context, "IN");
            ctx.put("acroFieldMap", acroFieldMap);
            ctx.put("contentId", acroFormContentId);
            Map map = dispatcher.runSync("setAcroFields", ctx);
            if (ServiceUtil.isError(map)) {
                String errMsg = ServiceUtil.makeErrorMessage(map, null, null, null, null);
                System.err.println(errMsg);
                ServiceUtil.returnError(errMsg);
            }
            String pdfFileNameOut = (String)context.get("pdfFileNameOut");
            ByteWrapper outByteWrapper = (ByteWrapper)map.get("outByteWrapper");
            results.put("outByteWrapper", outByteWrapper);
            if (UtilValidate.isNotEmpty(pdfFileNameOut)) {
                FileOutputStream fos = new FileOutputStream(pdfFileNameOut);
                fos.write(outByteWrapper.getBytes());
                fos.close();
            }
        } catch(FileNotFoundException e) {
            System.err.println(e.getMessage());
            ServiceUtil.returnError(e.getMessage());
        } catch(IOException e) {
            System.err.println(e.getMessage());
            ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            System.err.println(e.getMessage());
            ServiceUtil.returnError(e.getMessage());
        }
            
    return results;
    }
    
    public static ByteWrapper getInputByteWrapper(Map context, GenericDelegator delegator) throws GeneralException {
        
        ByteWrapper inputByteWrapper = (ByteWrapper)context.get("inputByteWrapper");
        
        if (inputByteWrapper == null) {
            String pdfFileNameIn = (String)context.get("pdfFileNameIn");
            String contentId = (String)context.get("contentId");
            if (UtilValidate.isNotEmpty(pdfFileNameIn)) {
                try {
                    FileInputStream fis = new FileInputStream(pdfFileNameIn);
                    int c;
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    while ((c = fis.read()) != -1) baos.write(c);
                    inputByteWrapper = new ByteWrapper(baos.toByteArray());
                } catch(FileNotFoundException e) {
                    throw(new GeneralException(e.getMessage()));
                } catch(IOException e) {
                    throw(new GeneralException(e.getMessage()));
                }
            } else if (UtilValidate.isNotEmpty(contentId)) {
                try {
                    Locale locale = (Locale)context.get("locale");
                    String https = (String)context.get("https"); 
                    String webSiteId = (String)context.get("webSiteId");
                    String rootDir = (String)context.get("rootDir");
                    GenericValue content = delegator.findByPrimaryKeyCache("Content", UtilMisc.toMap("contentId", contentId));
                    String dataResourceId = content.getString("dataResourceId");
                    inputByteWrapper = DataResourceWorker.getContentAsByteWrapper(delegator, dataResourceId, https, webSiteId, locale, rootDir);
                } catch (GenericEntityException e) {
                    throw(new GeneralException(e.getMessage()));
                } catch (IOException e) {
                    throw(new GeneralException(e.getMessage()));
                }
            }
        }
        return inputByteWrapper;
    }
}
