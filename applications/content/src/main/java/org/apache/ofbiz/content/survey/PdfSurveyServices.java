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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.PDComboBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDListBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDRadioButton;
import org.apache.pdfbox.pdmodel.interactive.form.PDTerminalField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;


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
        try (PDDocument pdDocument = loadInputPdf(context, delegator)) {
            if (pdDocument == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ContentPDFInputMissing", locale));
            }
            String surveyName = (String) context.get("surveyName");

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

            PdfFormHelper pdfForm = new PdfFormHelper(pdDocument);
            for (PDTerminalField field : pdfForm.getFormFields()) {
                String fieldName = field.getFullyQualifiedName();
                String value = pdfForm.getValue(field);
                Debug.logInfo("fieldName:" + fieldName + "; field: " + field + "; value: " + value, MODULE);

                GenericValue surveyQuestion = delegator.makeValue("SurveyQuestion", UtilMisc.toMap("question", fieldName));
                String surveyQuestionId = delegator.getNextSeqId("SurveyQuestion");
                surveyQuestion.set("surveyQuestionId", surveyQuestionId);
                surveyQuestion.set("surveyQuestionCategoryId", surveyQuestionCategoryId);

                if (field instanceof PDTextField) {
                    surveyQuestion.set("surveyQuestionTypeId", "TEXT_SHORT");
                } else if (field instanceof PDRadioButton) {
                    surveyQuestion.set("surveyQuestionTypeId", "OPTION");
                } else if (field instanceof PDListBox || field instanceof PDComboBox) {
                    surveyQuestion.set("surveyQuestionTypeId", "OPTION");
                    // TODO: handle these specially with PDChoice.getOptionsDisplayValues (and getOptionsExportValues?)
                } else {
                    surveyQuestion.set("surveyQuestionTypeId", "TEXT_SHORT");
                    Debug.logWarning("Building Survey from PDF, fieldName=[" + fieldName + "]: don't know how to handle field type: "
                            + field.getFieldType() + "; defaulting to short text", MODULE);
                }

                // ==== create a good sequenceNum based on tab order or if no tab order then the page location

                PDAnnotationWidget widget = pdfForm.getPlacedWidget(field);
                Integer tabPage = pdfForm.getPlace(widget)[0];
                Integer tabOrder = pdfForm.getPlace(widget)[1];
                Debug.logInfo("tabPage=" + tabPage + ", tabOrder=" + tabOrder, MODULE);

                // position of the field's first widget: [page, llx, lly, urx, ury]
                PDRectangle rectangle = widget.getRectangle() != null ? widget.getRectangle() : new PDRectangle(0, 0);
                float[] fieldPositions = {tabPage, rectangle.getLowerLeftX(), rectangle.getLowerLeftY(), rectangle.getUpperRightX(),
                    rectangle.getUpperRightY()};
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
                String annotation = field.getAlternateFieldName();

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
            if (UtilValidate.isNotEmpty(contentId)) {
                survey = EntityQuery.use(delegator).from("Survey").where("surveyId", surveyId).queryOne();
                survey.set("acroFormContentId", contentId);
                survey.store();
            }
        } catch (GeneralException | IOException e) {
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
        try (PDDocument pdDocument = loadInputPdf(context, dctx.getDelegator())) {
            if (pdDocument == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ContentPDFInputMissing", locale));
            }
            Delegator delegator = dctx.getDelegator();
            String partyId = (String) context.get("partyId");
            String surveyId = (String) context.get("surveyId");
            surveyResponseId = (String) context.get("surveyResponseId");
            if (UtilValidate.isNotEmpty(surveyResponseId)) {
                GenericValue surveyResponse = EntityQuery.use(delegator).from("SurveyResponse")
                        .where("surveyResponseId", surveyResponseId).queryOne();
                if (surveyResponse != null) {
                    surveyId = surveyResponse.getString("surveyId");
                }
            } else {
                surveyResponseId = delegator.getNextSeqId("SurveyResponse");
                GenericValue surveyResponse = delegator.makeValue("SurveyResponse",
                        UtilMisc.toMap("surveyResponseId", surveyResponseId, "surveyId", surveyId, "partyId", partyId));
                surveyResponse.set("responseDate", UtilDateTime.nowTimestamp());
                surveyResponse.set("lastModifiedDate", UtilDateTime.nowTimestamp());
                surveyResponse.create();
            }

            PdfFormHelper pdfForm = new PdfFormHelper(pdDocument);
            for (PDTerminalField field : pdfForm.getFormFields()) {
                String fieldName = field.getFullyQualifiedName();
                String value = pdfForm.getValue(field);
                GenericValue surveyQuestionAndAppl = EntityQuery.use(delegator).from("SurveyQuestionAndAppl")
                        .where("surveyId", surveyId, "externalFieldRef", fieldName).queryFirst();
                if (surveyQuestionAndAppl == null) {
                    Debug.logInfo("No question found for surveyId:" + surveyId + " and externalFieldRef:" + fieldName,
                            MODULE);
                    continue;
                }

                String surveyQuestionId = (String) surveyQuestionAndAppl.get("surveyQuestionId");
                String surveyQuestionTypeId = (String) surveyQuestionAndAppl.get("surveyQuestionTypeId");
                GenericValue surveyResponseAnswer = delegator.makeValue("SurveyResponseAnswer",
                        UtilMisc.toMap("surveyResponseId", surveyResponseId, "surveyQuestionId", surveyQuestionId,
                                "surveyMultiRespColId", "_NA_"));
                if (surveyQuestionTypeId == null || "TEXT_SHORT".equals(surveyQuestionTypeId)) {
                    surveyResponseAnswer.set("textResponse", value);
                }

                delegator.createOrStore(surveyResponseAnswer);
            }
        } catch (GeneralException | IOException e) {
            Debug.logError(e, "Error generating PDF: " + e.toString(), MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ContentPDFGeneratingError",
                    UtilMisc.toMap("errorString", e.toString()), locale));
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("surveyResponseId", surveyResponseId);
        return results;
    }

    /**
     * @throws GeneralException if getInputByteBuffer fails
     */
    public static Map<String, Object> getAcroFieldsFromPdf(DispatchContext dctx, Map<String, ? extends Object> context) throws GeneralException {
        Map<String, Object> acroFieldMap = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        try (PDDocument pdDocument = loadInputPdf(context, delegator)) {
            if (pdDocument == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ContentPDFInputMissing", locale));
            }
            PdfFormHelper pdfForm = new PdfFormHelper(pdDocument);
            for (PDTerminalField field : pdfForm.getFormFields()) {
                acroFieldMap.put(field.getFullyQualifiedName(), pdfForm.getValue(field));
            }
        } catch (IOException e) {
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
        Locale locale = (Locale) context.get("locale");
        try {
            Map<String, Object> acroFieldMap = UtilGenerics.cast(context.get("acroFieldMap"));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (PDDocument pdDocument = loadInputPdf(context, delegator)) {
                if (pdDocument == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ContentPDFInputMissing", locale));
                }
                PdfFormHelper pdfForm = new PdfFormHelper(pdDocument);
                for (PDTerminalField field : pdfForm.getFormFields()) {
                    String fieldValue;
                    Object obj = acroFieldMap.get(field.getFullyQualifiedName());
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
                        pdfForm.setValue(field, fieldValue);
                    }
                }
                pdfForm.flatten();
                pdDocument.save(baos);
            }
            ByteBuffer outByteBuffer = ByteBuffer.wrap(baos.toByteArray());
            results.put("outByteBuffer", outByteBuffer);
        } catch (IOException | GeneralException e) {
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

            List<String> lines = new ArrayList<>();
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
                lines.add(surveyQuestion.getString("question") + ": " + value);
            }
            ByteBuffer outByteBuffer = ByteBuffer.wrap(writeTextPdf(lines));
            results.put("outByteBuffer", outByteBuffer);
        } catch (GenericEntityException | IOException e) {
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

    /**
     * @return the PDF given by the inputByteBuffer, pdfFileNameIn or contentId parameter, null if none of them is given
     */
    private static PDDocument loadInputPdf(Map<String, ? extends Object> context, Delegator delegator) throws GeneralException, IOException {
        ByteBuffer inputByteBuffer = getInputByteBuffer(context, delegator);
        return inputByteBuffer != null ? Loader.loadPDF(inputByteBuffer.array()) : null;
    }

    /**
     * Writes the lines in a new A4 PDF, in 12 points Helvetica, wrapping long lines and adding pages as needed. Characters
     * that Helvetica can't show are replaced by "?".
     * @param lines the text lines
     * @return the PDF content
     * @throws IOException if the PDF can't be written
     */
    private static byte[] writeTextPdf(List<String> lines) throws IOException {
        PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        float fontSize = 12;
        float leading = 16;
        float margin = 36;
        PDRectangle pageSize = PDRectangle.A4;
        List<String> pdfLines = new ArrayList<>();
        for (String line : lines) {
            for (String paragraph : line.replace('\t', ' ').split("\\R", -1)) {
                pdfLines.addAll(wrapLine(font, fontSize, pageSize.getWidth() - 2 * margin, toShowableText(font, paragraph)));
            }
        }
        int linesPerPage = (int) ((pageSize.getHeight() - 2 * margin) / leading);
        try (PDDocument pdDocument = new PDDocument()) {
            int start = 0;
            do {
                PDPage page = new PDPage(pageSize);
                pdDocument.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(pdDocument, page)) {
                    content.beginText();
                    content.setFont(font, fontSize);
                    content.setLeading(leading);
                    content.newLineAtOffset(margin, pageSize.getHeight() - margin - fontSize);
                    for (String line : pdfLines.subList(start, Math.min(start + linesPerPage, pdfLines.size()))) {
                        content.showText(line);
                        content.newLine();
                    }
                    content.endText();
                }
                start += linesPerPage;
            } while (start < pdfLines.size());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            pdDocument.save(baos);
            return baos.toByteArray();
        }
    }

    private static String toShowableText(PDFont font, String text) {
        StringBuilder showableText = new StringBuilder();
        text.codePoints().forEach(codePoint -> {
            String character = new String(Character.toChars(codePoint));
            try {
                font.encode(character);
                showableText.append(character);
            } catch (IOException | IllegalArgumentException e) {
                showableText.append('?');
            }
        });
        return showableText.toString();
    }

    private static List<String> wrapLine(PDFont font, float fontSize, float width, String text) throws IOException {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ", -1)) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (font.getStringWidth(candidate) / 1000 * fontSize <= width) {
                line.setLength(0);
                line.append(candidate);
                continue;
            }
            if (line.length() > 0) {
                lines.add(line.toString());
                line.setLength(0);
            }
            // a word longer than the line is cut
            for (char character : word.toCharArray()) {
                if (line.length() > 0 && font.getStringWidth(line.toString() + character) / 1000 * fontSize > width) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                line.append(character);
            }
        }
        lines.add(line.toString());
        return lines;
    }
}
