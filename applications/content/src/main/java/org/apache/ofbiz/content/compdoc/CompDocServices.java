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
package org.apache.ofbiz.content.compdoc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.content.data.DataResourceWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.webapp.event.CoreEvents;
import org.apache.ofbiz.webapp.view.ViewHandlerException;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;

/**
 * CompDocEvents Class
 */

public class CompDocServices {
    public static final String module = CompDocServices.class.getName();
    public static final String resource = "ContentUiLabels";
    
    /**
     *
     * Creates the topmost Content entity of a Composite Document tree.
     * Also creates an "empty" Composite Document Instance Content entity.
     * Creates ContentRevision/Item records for each, as well.
     * @param dctx the dispatch context
     * @param context the context
     * @return Creates the topmost Content entity of a Composite Document tree
     */

    public static Map<String, Object> persistRootCompDoc(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale)context.get("locale");
        GenericValue userLogin = (GenericValue)context.get("userLogin");
        String contentId = (String)context.get("contentId");

        if (UtilValidate.isNotEmpty(contentId)) {
            try {
                EntityQuery.use(delegator).from("Content").where("contentId", contentId).queryOne();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error running serviceName persistContentAndAssoc", module);
                return ServiceUtil.returnError(UtilProperties.getMessage(CoreEvents.err_resource, "ContentNoContentFound", UtilMisc.toMap("contentId", contentId), locale));
           }
        }

        ModelService modelService = null;
        try {
            modelService = dispatcher.getDispatchContext().getModelService("persistContentAndAssoc");
        } catch (GenericServiceException e) {
            Debug.logError("Error getting model service for serviceName, 'persistContentAndAssoc'. " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(CoreEvents.err_resource, "coreEvents.error_modelservice_for_srv_name", locale));
        }
        Map<String, Object> persistMap = modelService.makeValid(context, ModelService.IN_PARAM);
        persistMap.put("userLogin", userLogin);
        try {
            Map<String, Object> persistContentResult = dispatcher.runSync("persistContentAndAssoc", persistMap);
            if (ServiceUtil.isError(persistContentResult)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentContentCreatingError", UtilMisc.toMap("serviceName", "persistContentAndAssoc"), locale), null, null, persistContentResult);
            }

            contentId = (String) persistContentResult.get("contentId");
            result.putAll(persistContentResult);

            Map<String, Object> contentRevisionMap = new HashMap<>();
            contentRevisionMap.put("itemContentId", contentId);
            contentRevisionMap.put("contentId", contentId);
            contentRevisionMap.put("userLogin", userLogin);

            Map<String, Object> persistRevResult = dispatcher.runSync("persistContentRevisionAndItem", contentRevisionMap);
            if (ServiceUtil.isError(persistRevResult)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentContentCreatingError", UtilMisc.toMap("serviceName", "persistContentRevisionAndItem"), locale), null, null, persistRevResult);
            }

            result.putAll(persistRevResult);
            return result;
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error running serviceName, 'persistContentAndAssoc'. " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentContentCreatingError", UtilMisc.toMap("serviceName", "persistContentAndAssoc"), locale) + e.toString());
        }
    }

    public static Map<String, Object> renderCompDocPdf(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();

        Locale locale = (Locale) context.get("locale");
        String rootDir = (String) context.get("rootDir");
        String webSiteId = (String) context.get("webSiteId");
        String https = (String) context.get("https");

        Delegator delegator = dctx.getDelegator();

        String contentId = (String) context.get("contentId");
        String contentRevisionSeqId = (String) context.get("contentRevisionSeqId");

        try {
            List<EntityCondition> exprList = new LinkedList<>();
            exprList.add(EntityCondition.makeCondition("contentIdTo", EntityOperator.EQUALS, contentId));
            exprList.add(EntityCondition.makeCondition("contentAssocTypeId", EntityOperator.EQUALS, "COMPDOC_PART"));
            exprList.add(EntityCondition.makeCondition("rootRevisionContentId", EntityOperator.EQUALS, contentId));
            if (UtilValidate.isNotEmpty(contentRevisionSeqId)) {
                exprList.add(EntityCondition.makeCondition("contentRevisionSeqId", EntityOperator.LESS_THAN_EQUAL_TO, contentRevisionSeqId));
            }

            List<GenericValue> compDocParts = EntityQuery.use(delegator)
                    .select("rootRevisionContentId", "itemContentId", "maxRevisionSeqId", "contentId", "dataResourceId", "contentIdTo", "contentAssocTypeId", "fromDate", "sequenceNum")
                    .from("ContentAssocRevisionItemView")
                    .where(exprList)
                    .orderBy("sequenceNum").filterByDate().queryList();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document();
            document.setPageSize(PageSize.LETTER);
            PdfCopy writer = new PdfCopy(document, baos);
            document.open();
            for (GenericValue contentAssocRevisionItemView : compDocParts) {
                String thisDataResourceId = contentAssocRevisionItemView.getString("dataResourceId");
                GenericValue dataResource = EntityQuery.use(delegator).from("DataResource").where("dataResourceId", thisDataResourceId).queryOne();
                String inputMimeType = null;
                if (dataResource != null) {
                    inputMimeType = dataResource.getString("mimeTypeId");
                }
                byte [] inputByteArray = null;
                PdfReader reader = null;
                if (inputMimeType != null && "application/pdf".equals(inputMimeType)) {
                    ByteBuffer byteBuffer = DataResourceWorker.getContentAsByteBuffer(delegator, thisDataResourceId, https, webSiteId, locale, rootDir);
                    inputByteArray = byteBuffer.array();
                    reader = new PdfReader(inputByteArray);
                } else if (inputMimeType != null && "text/html".equals(inputMimeType)) {
                    ByteBuffer byteBuffer = DataResourceWorker.getContentAsByteBuffer(delegator, thisDataResourceId, https, webSiteId, locale, rootDir);
                    inputByteArray = byteBuffer.array();
                    String s = new String(inputByteArray, "UTF-8");
                    Debug.logInfo("text/html string:" + s, module);
                    continue;
                } else if (inputMimeType != null && "application/vnd.ofbiz.survey.response".equals(inputMimeType)) {
                    String surveyResponseId = dataResource.getString("relatedDetailId");
                    String surveyId = null;
                    String acroFormContentId = null;
                    GenericValue surveyResponse = null;
                    if (UtilValidate.isNotEmpty(surveyResponseId)) {
                        surveyResponse = EntityQuery.use(delegator).from("SurveyResponse").where("surveyResponseId", surveyResponseId).queryOne();
                        if (surveyResponse != null) {
                            surveyId = surveyResponse.getString("surveyId");
                        }
                    }
                    if (UtilValidate.isNotEmpty(surveyId)) {
                        GenericValue survey = EntityQuery.use(delegator).from("Survey").where("surveyId", surveyId).queryOne();
                        if (survey != null) {
                            acroFormContentId = survey.getString("acroFormContentId");
                            if (UtilValidate.isNotEmpty(acroFormContentId)) {
                                // TODO: is something supposed to be done here?
                            }
                        }
                    }
                    if (surveyResponse != null) {
                        if (UtilValidate.isEmpty(acroFormContentId)) {
                            // Create AcroForm PDF
                            Map<String, Object> survey2PdfResults = dispatcher.runSync("buildPdfFromSurveyResponse", UtilMisc.toMap("surveyResponseId", surveyId));
                            if (ServiceUtil.isError(survey2PdfResults)) {
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentSurveyErrorBuildingPDF", locale), null, null, survey2PdfResults);
                            }

                            ByteBuffer outByteBuffer = (ByteBuffer) survey2PdfResults.get("outByteBuffer");
                            inputByteArray = outByteBuffer.array();
                            reader = new PdfReader(inputByteArray);
                        } else {
                            // Fill in acroForm
                            Map<String, Object> survey2AcroFieldResults = dispatcher.runSync("setAcroFieldsFromSurveyResponse", UtilMisc.toMap("surveyResponseId", surveyResponseId));
                            if (ServiceUtil.isError(survey2AcroFieldResults)) {
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentSurveyErrorSettingAcroFields", locale), null, null, survey2AcroFieldResults);
                            }

                            ByteBuffer outByteBuffer = (ByteBuffer) survey2AcroFieldResults.get("outByteBuffer");
                            inputByteArray = outByteBuffer.array();
                            reader = new PdfReader(inputByteArray);
                        }
                    }
                } else {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentMimeTypeNotSupported", locale));
                }
                if (reader != null) {
                    int n = reader.getNumberOfPages();
                    for (int i=0; i < n; i++) {
                        PdfImportedPage pg = writer.getImportedPage(reader, i + 1);
                        writer.addPage(pg);
                    }
                }
            }
            document.close();
            ByteBuffer outByteBuffer = ByteBuffer.wrap(baos.toByteArray());

            Map<String, Object> results = ServiceUtil.returnSuccess();
            results.put("outByteBuffer", outByteBuffer);
            return results;
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.toString());
        } catch (IOException | DocumentException | GeneralException e) {
            Debug.logError(e, "Error in CompDoc operation: ", module);
            return ServiceUtil.returnError(e.toString());
        }
    }

    public static Map<String, Object> renderContentPdf(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> results = ServiceUtil.returnSuccess();
        String dataResourceId = null;

        Locale locale = (Locale) context.get("locale");
        String rootDir = (String) context.get("rootDir");
        String webSiteId = (String) context.get("webSiteId");
        String https = (String) context.get("https");

        Delegator delegator = dctx.getDelegator();

        String contentId = (String) context.get("contentId");
        String contentRevisionSeqId = (String) context.get("contentRevisionSeqId");

        try {
            Document document = new Document();
            document.setPageSize(PageSize.LETTER);
            document.open();

            GenericValue dataResource = null;
            if (UtilValidate.isEmpty(contentRevisionSeqId)) {
                GenericValue content = EntityQuery.use(delegator).from("Content").where("contentId", contentId).cache().queryOne();
                dataResourceId = content.getString("dataResourceId");
                Debug.logInfo("SCVH(0b)- dataResourceId:" + dataResourceId, module);
                dataResource = EntityQuery.use(delegator).from("DataResource").where("dataResourceId", dataResourceId).queryOne();
             } else {
                GenericValue contentRevisionItem = EntityQuery.use(delegator).from("ContentRevisionItem").where("contentId", contentId, "itemContentId", contentId, "contentRevisionSeqId", contentRevisionSeqId).cache().queryOne();
                if (contentRevisionItem == null) {
                    throw new ViewHandlerException("ContentRevisionItem record not found for contentId=" + contentId
                                                   + ", contentRevisionSeqId=" + contentRevisionSeqId + ", itemContentId=" + contentId);
                }
                Debug.logInfo("SCVH(1)- contentRevisionItem:" + contentRevisionItem, module);
                Debug.logInfo("SCVH(2)-contentId=" + contentId
                        + ", contentRevisionSeqId=" + contentRevisionSeqId + ", itemContentId=" + contentId, module);
                dataResourceId = contentRevisionItem.getString("newDataResourceId");
                Debug.logInfo("SCVH(3)- dataResourceId:" + dataResourceId, module);
                dataResource = EntityQuery.use(delegator).from("DataResource").where("dataResourceId", dataResourceId).queryOne();
            }
            String inputMimeType = null;
            if (dataResource != null) {
                inputMimeType = dataResource.getString("mimeTypeId");
            }
            byte [] inputByteArray = null;
            if (inputMimeType != null && "application/pdf".equals(inputMimeType)) {
                ByteBuffer byteBuffer = DataResourceWorker.getContentAsByteBuffer(delegator, dataResourceId, https, webSiteId, locale, rootDir);
                inputByteArray = byteBuffer.array();
            } else if (inputMimeType != null && "text/html".equals(inputMimeType)) {
                ByteBuffer byteBuffer = DataResourceWorker.getContentAsByteBuffer(delegator, dataResourceId, https, webSiteId, locale, rootDir);
                inputByteArray = byteBuffer.array();
                String s = new String(inputByteArray, "UTF-8");
                Debug.logInfo("text/html string:" + s, module);
            } else if (inputMimeType != null && "application/vnd.ofbiz.survey.response".equals(inputMimeType)) {
                String surveyResponseId = dataResource.getString("relatedDetailId");
                String surveyId = null;
                String acroFormContentId = null;
                GenericValue surveyResponse = null;
                if (UtilValidate.isNotEmpty(surveyResponseId)) {
                    surveyResponse = EntityQuery.use(delegator).from("SurveyResponse").where("surveyResponseId", surveyResponseId).queryOne();
                    if (surveyResponse != null) {
                        surveyId = surveyResponse.getString("surveyId");
                    }
                }
                if (UtilValidate.isNotEmpty(surveyId)) {
                    GenericValue survey = EntityQuery.use(delegator).from("Survey").where("surveyId", surveyId).queryOne();
                    if (survey != null) {
                        acroFormContentId = survey.getString("acroFormContentId");
                        if (UtilValidate.isNotEmpty(acroFormContentId)) {
                            // TODO: is something supposed to be done here?
                        }
                    }
                }

                if (surveyResponse != null) {
                    if (UtilValidate.isEmpty(acroFormContentId)) {
                        // Create AcroForm PDF
                        Map<String, Object> survey2PdfResults = dispatcher.runSync("buildPdfFromSurveyResponse", UtilMisc.toMap("surveyResponseId", surveyResponseId));
                        if (ServiceUtil.isError(survey2PdfResults)) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentSurveyErrorBuildingPDF", locale), null, null, survey2PdfResults);
                        }

                        ByteBuffer outByteBuffer = (ByteBuffer)survey2PdfResults.get("outByteBuffer");
                        inputByteArray = outByteBuffer.array();
                    } else {
                        // Fill in acroForm
                        Map<String, Object> survey2AcroFieldResults = dispatcher.runSync("setAcroFieldsFromSurveyResponse", UtilMisc.toMap("surveyResponseId", surveyResponseId));
                        if (ServiceUtil.isError(survey2AcroFieldResults)) {
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentSurveyErrorSettingAcroFields", locale), null, null, survey2AcroFieldResults);
                        }

                        ByteBuffer outByteBuffer = (ByteBuffer) survey2AcroFieldResults.get("outByteBuffer");
                        inputByteArray = outByteBuffer.array();
                    }
                }
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentMimeTypeNotSupported", locale));
            }

            if (inputByteArray == null) {
                Debug.logError("Error in PDF generation: ", module);
                return ServiceUtil.returnError("The array used to create outByteBuffer is still declared null");
            }
            ByteBuffer outByteBuffer = ByteBuffer.wrap(inputByteArray);
            results.put("outByteBuffer", outByteBuffer);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.toString());
        } catch (IOException | GeneralException e) {
            Debug.logError(e, "Error in PDF generation: ", module);
            return ServiceUtil.returnError(e.toString());
        }
        return results;
    }
}
