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
package org.apache.ofbiz.content.survey;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.content.data.DataResourceWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;

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

    private static final String MODULE = PdfSurveyServices.class.getName();
    private static final String RESOURCE = "ContentUiLabels";

    /**
     */
    public static Map<String, Object> buildSurveyFromPdf(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
        String surveyId = null;
        try {
            String surveyName = (String) context.get("surveyName");
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ByteBuffer byteBuffer = getInputByteBuffer(context, delegator);
            PdfReader pdfReader = new PdfReader(byteBuffer.array());
            PdfStamper pdfStamper = new PdfStamper(pdfReader, os);
            AcroFields acroFields = pdfStamper.getAcroFields();
            Map<String, Object> acroFieldMap = UtilGenerics.cast(acroFields.getFields());

            String contentId = (String) context.get("contentId");
            GenericValue survey = null;
            surveyId = (String) context.get("surveyId");
            if (UtilValidate.isEmpty(surveyId)) {
                survey = delegator.makeValue("Survey", UtilMisc.toMap("surveyName", surveyName));
                survey.set("surveyId", surveyId);
                survey.set("allowMultiple", "Y");
                survey.set("allowUpdate", "Y");
                survey = delegator.createSetNextSeqId(survey);
                surveyId = survey.getString("surveyId");
            }

            // create a SurveyQuestionCategory to put the questions in
            Map<String, Object> createCategoryResultMap = dispatcher.runSync("createSurveyQuestionCategory",
                    UtilMisc.<String, Object>toMap("description", "From AcroForm in Content [" + contentId + "] for Survey [" + surveyId
                            + "]", "userLogin", userLogin));
            if (ServiceUtil.isError(createCategoryResultMap)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(createCategoryResultMap));
            }
            String surveyQuestionCategoryId = (String) createCategoryResultMap.get("surveyQuestionCategoryId");

            pdfStamper.setFormFlattening(true);
            for (String fieldName : acroFieldMap.keySet()) {
                AcroFields.Item item = acroFields.getFieldItem(fieldName);
                int type = acroFields.getFieldType(fieldName);
                String value = acroFields.getField(fieldName);
                Debug.logInfo("fieldName:" + fieldName + "; item: " + item + "; value: " + value, MODULE);

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
                } else {
                    surveyQuestion.set("surveyQuestionTypeId", "TEXT_SHORT");
                    Debug.logWarning("Building Survey from PDF, fieldName=[" + fieldName + "]: don't know how to handle field type: "
                            + type + "; defaulting to short text", MODULE);
                }

                // ==== create a good sequenceNum based on tab order or if no tab order then the page location

                Integer tabPage = item.getPage(0);
                Integer tabOrder = item.getTabOrder(0);
                Debug.logInfo("tabPage=" + tabPage + ", tabOrder=" + tabOrder, MODULE);

                //array of float  multiple of 5. For each of this groups the values are: [page, llx, lly, urx, ury]
                float[] fieldPositions = acroFields.getFieldPositions(fieldName);
                float fieldPage = fieldPositions[0];
                float fieldLlx = fieldPositions[1];
                float fieldLly = fieldPositions[2];
                float fieldUrx = fieldPositions[3];
                float fieldUry = fieldPositions[4];
                Debug.logInfo("fieldPage=" + fieldPage + ", fieldLlx=" + fieldLlx + ", fieldLly=" + fieldLly + ", fieldUrx="
                        + fieldUrx + ", fieldUry=" + fieldUry, MODULE);

                Long sequenceNum = null;
                if (tabPage != null && tabOrder != null) {
                    sequenceNum = (long) (tabPage * 1000 + tabOrder);
                    Debug.logInfo("tabPage=" + tabPage + ", tabOrder=" + tabOrder + ", sequenceNum=" + sequenceNum, MODULE);
                } else if (fieldPositions.length > 0) {
                    sequenceNum = (long) fieldPage * 10000 + (long) fieldLly * 1000 + (long) fieldLlx;
                    Debug.logInfo("fieldPage=" + fieldPage + ", fieldLlx=" + fieldLlx + ", fieldLly=" + fieldLly + ", fieldUrx="
                            + fieldUrx + ", fieldUry=" + fieldUry + ", sequenceNum=" + sequenceNum, MODULE);
                }

                // TODO: need to find something better to put into these fields...
                String annotation = null;
                for (int k = 0; k < item.size(); ++k) {
                    PdfDictionary dict = item.getWidget(k);

                    // if the "/Type" value is "/Annot", then get the value of "/TU" for the annotation

                    PdfObject typeValue = null;
                    PdfObject tuValue = null;

                    Set<PdfName> dictKeys = UtilGenerics.cast(dict.getKeys());
                    for (PdfName dictKeyName : dictKeys) {
                        PdfObject dictObject = dict.get(dictKeyName);

                        if ("/Type".equals(dictKeyName.toString())) {
                            typeValue = dictObject;
                        } else if ("/TU".equals(dictKeyName.toString())) {
                            tuValue = dictObject;
                        }
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

                GenericValue surveyQuestionAppl = delegator.makeValue("SurveyQuestionAppl",
                        UtilMisc.toMap("surveyId", surveyId, "surveyQuestionId", surveyQuestionId));
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
                survey = EntityQuery.use(delegator).from("Survey").where("surveyId", surveyId).queryOne();
                survey.set("acroFormContentId", contentId);
                survey.store();
            }
        } catch (GeneralException | DocumentException | IOException e) {
            Debug.logError(e, "Error generating PDF: " + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ContentPDFGeneratingError",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("surveyId", surveyId);
        return results;
    }

    /**
     */
    public static Map<String, Object> buildSurveyResponseFromPdf(DispatchContext dctx, Map<String, ? extends Object> context) {
        String surveyResponseId = null;
        Locale locale = (Locale) context.get("locale");
        try {
            Delegator delegator = dctx.getDelegator();
            String partyId = (String) context.get("partyId");
            String surveyId = (String) context.get("surveyId");
            surveyResponseId = (String) context.get("surveyResponseId");
            if (UtilValidate.isNotEmpty(surveyResponseId)) {
                GenericValue surveyResponse = EntityQuery.use(delegator).from("SurveyResponse").where("surveyResponseId",
                        surveyResponseId).queryOne();
                if (surveyResponse != null) {
                    surveyId = surveyResponse.getString("surveyId");
                }
            } else {
                surveyResponseId = delegator.getNextSeqId("SurveyResponse");
                GenericValue surveyResponse = delegator.makeValue("SurveyResponse", UtilMisc.toMap("surveyResponseId",
                        surveyResponseId, "surveyId", surveyId, "partyId", partyId));
                surveyResponse.set("responseDate", UtilDateTime.nowTimestamp());
                surveyResponse.set("lastModifiedDate", UtilDateTime.nowTimestamp());
                surveyResponse.create();
            }

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ByteBuffer byteBuffer = getInputByteBuffer(context, delegator);
            PdfReader r = new PdfReader(byteBuffer.array());
            PdfStamper s = new PdfStamper(r, os);
            AcroFields fs = s.getAcroFields();
            Map<String, Object> hm = UtilGenerics.cast(fs.getFields());
            s.setFormFlattening(true);
            for (String fieldName : hm.keySet()) {
                //AcroFields.Item item = fs.getFieldItem(fieldName);
                String value = fs.getField(fieldName);
                GenericValue surveyQuestionAndAppl = EntityQuery.use(delegator).from("SurveyQuestionAndAppl")
                        .where("surveyId", surveyId,
                                "externalFieldRef", fieldName)
                        .queryFirst();
                if (surveyQuestionAndAppl == null) {
                    Debug.logInfo("No question found for surveyId:" + surveyId + " and externalFieldRef:" + fieldName, MODULE);
                    continue;
                }

                String surveyQuestionId = (String) surveyQuestionAndAppl.get("surveyQuestionId");
                String surveyQuestionTypeId = (String) surveyQuestionAndAppl.get("surveyQuestionTypeId");
                GenericValue surveyResponseAnswer = delegator.makeValue("SurveyResponseAnswer", UtilMisc.toMap("surveyResponseId",
                        surveyResponseId, "surveyQuestionId", surveyQuestionId));
                if (surveyQuestionTypeId == null || "TEXT_SHORT".equals(surveyQuestionTypeId)) {
                    surveyResponseAnswer.set("textResponse", value);
                }

                delegator.create(surveyResponseAnswer);
            }
            s.close();
        } catch (GeneralException | DocumentException | IOException e) {
            Debug.logError(e, "Error generating PDF: " + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ContentPDFGeneratingError",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("surveyResponseId", surveyResponseId);
        return results;
    }

    /**
     */
    public static Map<String, Object> getAcroFieldsFromPdf(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> acroFieldMap = new HashMap<>();
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Delegator delegator = dctx.getDelegator();
            ByteBuffer byteBuffer = getInputByteBuffer(context, delegator);
            PdfReader r = new PdfReader(byteBuffer.array());
            PdfStamper s = new PdfStamper(r, os);
            AcroFields fs = s.getAcroFields();
            Map<String, Object> map = UtilGenerics.cast(fs.getFields());
            s.setFormFlattening(true);

            for (String fieldName : map.keySet()) {
                String parmValue = fs.getField(fieldName);
                acroFieldMap.put(fieldName, parmValue);
            }

        } catch (DocumentException | GeneralException | IOException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("acroFieldMap", acroFieldMap);
        return results;
    }

    /**
     */
    public static Map<String, Object> setAcroFields(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> results = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        try {
            Map<String, Object> acroFieldMap = UtilGenerics.cast(context.get("acroFieldMap"));
            ByteBuffer byteBuffer = getInputByteBuffer(context, delegator);
            PdfReader r = new PdfReader(byteBuffer.array());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfStamper s = new PdfStamper(r, baos);
            AcroFields fs = s.getAcroFields();
            Map<String, Object> map = UtilGenerics.cast(fs.getFields());
            s.setFormFlattening(true);

            for (String fieldName : map.keySet()) {
                String fieldValue = fs.getField(fieldName);
                Object obj = acroFieldMap.get(fieldName);
                if (obj instanceof Date) {
                    Date d = (Date) obj;
                    fieldValue = UtilDateTime.toDateString(d);
                } else if (obj instanceof Long) {
                    Long lg = (Long) obj;
                    fieldValue = lg.toString();
                } else if (obj instanceof Integer) {
                    Integer ii = (Integer) obj;
                    fieldValue = ii.toString();
                } else {
                    fieldValue = (String) obj;
                }

                if (UtilValidate.isNotEmpty(fieldValue)) {
                    fs.setField(fieldName, fieldValue);
                }
            }

            s.close();
            baos.close();
            ByteBuffer outByteBuffer = ByteBuffer.wrap(baos.toByteArray());
            results.put("outByteBuffer", outByteBuffer);
        } catch (DocumentException | IOException | GeneralException e) {
            Debug.logError(e, MODULE);
            results = ServiceUtil.returnError(e.getMessage());
        }
        return results;
    }


    /**
     */
    public static Map<String, Object> buildPdfFromSurveyResponse(DispatchContext dctx, Map<String, ? extends Object> rcontext) {
        Map<String, Object> context = UtilMisc.makeMapWritable(rcontext);
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> results = ServiceUtil.returnSuccess();
        String surveyResponseId = (String) context.get("surveyResponseId");
        String contentId = (String) context.get("contentId");
        String surveyId = null;

        Document document = new Document();
        try {
            if (UtilValidate.isNotEmpty(surveyResponseId)) {
                GenericValue surveyResponse = EntityQuery.use(delegator).from("SurveyResponse").where("surveyResponseId",
                        surveyResponseId).queryOne();
                if (surveyResponse != null) {
                    surveyId = surveyResponse.getString("surveyId");
                }
            }
            if (UtilValidate.isNotEmpty(surveyId) && UtilValidate.isEmpty(contentId)) {
                GenericValue survey = EntityQuery.use(delegator).from("Survey").where("surveyId", surveyId).queryOne();
                if (survey != null) {
                    String acroFormContentId = survey.getString("acroFormContentId");
                    if (UtilValidate.isNotEmpty(acroFormContentId)) {
                        context.put("contentId", acroFormContentId);
                    }
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);

            List<GenericValue> responses = EntityQuery.use(delegator).from("SurveyResponseAnswer").where("surveyResponseId",
                    surveyResponseId).queryList();
            for (GenericValue surveyResponseAnswer : responses) {
                String value = null;
                String surveyQuestionId = (String) surveyResponseAnswer.get("surveyQuestionId");
                GenericValue surveyQuestion = EntityQuery.use(delegator).from("SurveyQuestion").where("surveyQuestionId",
                        surveyQuestionId).queryOne();
                String questionType = surveyQuestion.getString("surveyQuestionTypeId");
                // DEJ20060227 this isn't used, if needed in the future should get from SurveyQuestionAppl.externalFieldRef
                // String fieldName = surveyQuestion.getString("description");
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
                    // not really a question; ignore completely, adding log statement to avoid checkstyle
                    Debug.logInfo("Not really a question; ignore completely. Question type:" + questionType, MODULE);
                } else {
                    value = surveyResponseAnswer.getString("textResponse");
                }
                Chunk chunk = new Chunk(surveyQuestion.getString("question") + ": " + value);
                Paragraph p = new Paragraph(chunk);
                document.add(p);
            }
            ByteBuffer outByteBuffer = ByteBuffer.wrap(baos.toByteArray());
            results.put("outByteBuffer", outByteBuffer);
        } catch (GenericEntityException | DocumentException e) {
            Debug.logError(e, MODULE);
            results = ServiceUtil.returnError(e.getMessage());
        }

        return results;
    }

    /**
     * Returns list of maps with "question" -&gt; SurveyQuestion and "response" -&gt; SurveyResponseAnswer
     */
    public static Map<String, Object> buildSurveyQuestionsAndAnswers(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> results = ServiceUtil.returnSuccess();
        String surveyResponseId = (String) context.get("surveyResponseId");
        List<Object> qAndA = new LinkedList<>();

        try {
            List<GenericValue> responses = EntityQuery.use(delegator).from("SurveyResponseAnswer").where("surveyResponseId",
                    surveyResponseId).queryList();
            for (GenericValue surveyResponseAnswer : responses) {
                String surveyQuestionId = (String) surveyResponseAnswer.get("surveyQuestionId");
                GenericValue surveyQuestion = EntityQuery.use(delegator).from("SurveyQuestion").where("surveyQuestionId",
                        surveyQuestionId).queryOne();
                qAndA.add(UtilMisc.toMap("question", surveyQuestion, "response", surveyResponseAnswer));
            }
            results.put("questionsAndAnswers", qAndA);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            results = ServiceUtil.returnError(e.getMessage());
        }

        return results;
    }

    /**
     */
    public static Map<String, Object> setAcroFieldsFromSurveyResponse(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> results = ServiceUtil.returnSuccess();
        Map<String, Object> acroFieldMap = new HashMap<>();
        String surveyResponseId = (String) context.get("surveyResponseId");
        String acroFormContentId = null;

        try {
            String surveyId = null;
            if (UtilValidate.isNotEmpty(surveyResponseId)) {
                GenericValue surveyResponse = EntityQuery.use(delegator).from("SurveyResponse").where("surveyResponseId",
                        surveyResponseId).queryOne();
                if (surveyResponse != null) {
                    surveyId = surveyResponse.getString("surveyId");
                }
            }

            if (UtilValidate.isNotEmpty(surveyId)) {
                GenericValue survey = EntityQuery.use(delegator).from("Survey").where("surveyId", surveyId).queryOne();
                if (survey != null) {
                    acroFormContentId = survey.getString("acroFormContentId");
                }
            }

            List<GenericValue> responses = EntityQuery.use(delegator).from("SurveyResponseAnswer").where("surveyResponseId",
                    surveyResponseId).queryList();
            for (GenericValue surveyResponseAnswer : responses) {
                String value = null;
                String surveyQuestionId = (String) surveyResponseAnswer.get("surveyQuestionId");

                GenericValue surveyQuestion = EntityQuery.use(delegator).from("SurveyQuestion").where("surveyQuestionId",
                        surveyQuestionId).cache().queryOne();

                GenericValue surveyQuestionAppl = EntityQuery.use(delegator).from("SurveyQuestionAppl")
                        .where("surveyId", surveyId,
                                "surveyQuestionId", surveyQuestionId)
                        .orderBy("-fromDate")
                        .filterByDate().cache().queryFirst();

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
                    // not really a question; ignore completely, adding log to ignore checkstyle issue
                    Debug.logInfo("Not really a question; ignore completely. Question type:" + questionType, MODULE);
                } else {
                    value = surveyResponseAnswer.getString("textResponse");
                }
                acroFieldMap.put(fieldName, value);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        try {
            ModelService modelService = dispatcher.getDispatchContext().getModelService("setAcroFields");
            Map<String, Object> ctx = modelService.makeValid(context, ModelService.IN_PARAM);
            ctx.put("acroFieldMap", acroFieldMap);
            ctx.put("contentId", acroFormContentId);
            Map<String, Object> map = dispatcher.runSync("setAcroFields", ctx);
            if (ServiceUtil.isError(map)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(map));
            }
            String pdfFileNameOut = (String) context.get("pdfFileNameOut");
            ByteBuffer outByteBuffer = (ByteBuffer) map.get("outByteBuffer");
            results.put("outByteBuffer", outByteBuffer);
            if (UtilValidate.isNotEmpty(pdfFileNameOut)) {
                FileOutputStream fos = new FileOutputStream(pdfFileNameOut);
                fos.write(outByteBuffer.array());
                fos.close();
            }
        } catch (IOException | GenericServiceException e) {
            Debug.logError(e, "Error generating PDF: " + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ContentPDFGeneratingError",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        }

        return results;
    }

    public static ByteBuffer getInputByteBuffer(Map<String, ? extends Object> context, Delegator delegator) throws GeneralException {
        ByteBuffer inputByteBuffer = (ByteBuffer) context.get("inputByteBuffer");

        if (inputByteBuffer == null) {
            String pdfFileNameIn = (String) context.get("pdfFileNameIn");
            String contentId = (String) context.get("contentId");
            if (UtilValidate.isNotEmpty(pdfFileNameIn)) {
                try (FileInputStream fis = new FileInputStream(pdfFileNameIn)) {
                    int c;
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    while ((c = fis.read()) != -1) {
                        baos.write(c);
                    }
                    inputByteBuffer = ByteBuffer.wrap(baos.toByteArray());
                } catch (IOException e) {
                    throw(new GeneralException(e.getMessage()));
                }
            } else if (UtilValidate.isNotEmpty(contentId)) {
                try {
                    Locale locale = (Locale) context.get("locale");
                    String https = (String) context.get("https");
                    String webSiteId = (String) context.get("webSiteId");
                    String rootDir = (String) context.get("rootDir");
                    GenericValue content = EntityQuery.use(delegator).from("Content").where("contentId", contentId).cache().queryOne();
                    String dataResourceId = content.getString("dataResourceId");
                    inputByteBuffer = DataResourceWorker.getContentAsByteBuffer(delegator, dataResourceId, https, webSiteId, locale, rootDir);
                } catch (GenericEntityException | IOException e) {
                    throw(new GeneralException(e.getMessage()));
                }
            }
        }
        return inputByteBuffer;
    }
}
