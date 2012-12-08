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
package org.ofbiz.content.compdoc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.content.data.DataResourceWorker;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.webapp.event.CoreEvents;
import org.ofbiz.webapp.view.ViewHandlerException;

import com.lowagie.text.Document;
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
        Map<String, Object> result = FastMap.newInstance();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale)context.get("locale");
        GenericValue userLogin = (GenericValue)context.get("userLogin");
        String contentId = (String)context.get("contentId");
        //String instanceContentId = null;

        if (UtilValidate.isNotEmpty(contentId)) {
            try {
                delegator.findOne("Content", UtilMisc.toMap("contentId", contentId), false);
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
            //request.setAttribute("contentId", contentId);
            // Update ContentRevision and ContentRevisonItem

            Map<String, Object> contentRevisionMap = FastMap.newInstance();
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
        String oooHost = (String) context.get("oooHost");
        String oooPort = (String) context.get("oooPort");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        try {
            Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
            List<EntityCondition> exprList = FastList.newInstance();
            exprList.add(EntityCondition.makeCondition("contentIdTo", EntityOperator.EQUALS, contentId));
            exprList.add(EntityCondition.makeCondition("rootRevisionContentId", EntityOperator.EQUALS, contentId));
            if (UtilValidate.isNotEmpty(contentRevisionSeqId)) {
                exprList.add(EntityCondition.makeCondition("contentRevisionSeqId", EntityOperator.LESS_THAN_EQUAL_TO, contentRevisionSeqId));
            }
            exprList.add(EntityCondition.makeCondition("contentAssocTypeId", EntityOperator.EQUALS, "COMPDOC_PART"));
            exprList.add(EntityCondition.makeCondition("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, nowTimestamp));

            List<EntityCondition> thruList = FastList.newInstance();
            thruList.add(EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null));
            thruList.add(EntityCondition.makeCondition("thruDate", EntityOperator.GREATER_THAN, nowTimestamp));
            exprList.add(EntityCondition.makeCondition(thruList, EntityOperator.OR));

            EntityConditionList<EntityCondition> conditionList = EntityCondition.makeCondition(exprList, EntityOperator.AND);

            String [] fields = {"rootRevisionContentId", "itemContentId", "maxRevisionSeqId", "contentId", "dataResourceId", "contentIdTo", "contentAssocTypeId", "fromDate", "sequenceNum"};
            Set<String> selectFields = UtilMisc.toSetArray(fields);
            List<String> orderByFields = UtilMisc.toList("sequenceNum");
            List<GenericValue> compDocParts = delegator.findList("ContentAssocRevisionItemView", conditionList, selectFields, orderByFields, null, false);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document();
            document.setPageSize(PageSize.LETTER);
            //Rectangle rect = document.getPageSize();
            //PdfWriter writer = PdfWriter.getInstance(document, baos);
            PdfCopy writer = new PdfCopy(document, baos);
            document.open();
            int pgCnt =0;
            for(GenericValue contentAssocRevisionItemView : compDocParts) {
                //String thisContentId = contentAssocRevisionItemView.getString("contentId");
                //String thisContentRevisionSeqId = contentAssocRevisionItemView.getString("maxRevisionSeqId");
                String thisDataResourceId = contentAssocRevisionItemView.getString("dataResourceId");
                GenericValue dataResource = delegator.findOne("DataResource", UtilMisc.toMap("dataResourceId", thisDataResourceId), false);
                String inputMimeType = null;
                if (dataResource != null) {
                    inputMimeType = dataResource.getString("mimeTypeId");
                }
                byte [] inputByteArray = null;
                PdfReader reader = null;
                if (inputMimeType != null && inputMimeType.equals("application/pdf")) {
                    ByteBuffer byteBuffer = DataResourceWorker.getContentAsByteBuffer(delegator, thisDataResourceId, https, webSiteId, locale, rootDir);
                    inputByteArray = byteBuffer.array();
                    reader = new PdfReader(inputByteArray);
                } else if (inputMimeType != null && inputMimeType.equals("text/html")) {
                    ByteBuffer byteBuffer = DataResourceWorker.getContentAsByteBuffer(delegator, thisDataResourceId, https, webSiteId, locale, rootDir);
                    inputByteArray = byteBuffer.array();
                    String s = new String(inputByteArray);
                    Debug.logInfo("text/html string:" + s, module);
                    continue;
                } else if (inputMimeType != null && inputMimeType.equals("application/vnd.ofbiz.survey.response")) {
                    String surveyResponseId = dataResource.getString("relatedDetailId");
                    String surveyId = null;
                    String acroFormContentId = null;
                    GenericValue surveyResponse = null;
                    if (UtilValidate.isNotEmpty(surveyResponseId)) {
                        surveyResponse = delegator.findOne("SurveyResponse", UtilMisc.toMap("surveyResponseId", surveyResponseId), false);
                        if (surveyResponse != null) {
                            surveyId = surveyResponse.getString("surveyId");
                        }
                    }
                    if (UtilValidate.isNotEmpty(surveyId)) {
                        GenericValue survey = delegator.findOne("Survey", UtilMisc.toMap("surveyId", surveyId), false);
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
                    ByteBuffer inByteBuffer = DataResourceWorker.getContentAsByteBuffer(delegator, thisDataResourceId, https, webSiteId, locale, rootDir);

                    Map<String, Object> convertInMap = UtilMisc.<String, Object>toMap("userLogin", userLogin, "inByteBuffer", inByteBuffer, "inputMimeType", inputMimeType, "outputMimeType", "application/pdf");
                    if (UtilValidate.isNotEmpty(oooHost)) convertInMap.put("oooHost", oooHost);
                    if (UtilValidate.isNotEmpty(oooPort)) convertInMap.put("oooPort", oooPort);

                    Map<String, Object> convertResult = dispatcher.runSync("convertDocumentByteBuffer", convertInMap);

                    if (ServiceUtil.isError(convertResult)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentConvertingDocumentByteBuffer", locale), null, null, convertResult);
                    }

                    ByteBuffer outByteBuffer = (ByteBuffer) convertResult.get("outByteBuffer");
                    inputByteArray = outByteBuffer.array();
                    reader = new PdfReader(inputByteArray);
                }
                if (reader != null) {
                    int n = reader.getNumberOfPages();
                    for (int i=0; i < n; i++) {
                        PdfImportedPage pg = writer.getImportedPage(reader, i + 1);
                        //cb.addTemplate(pg, left, height * pgCnt);
                        writer.addPage(pg);
                        pgCnt++;
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
        } catch (IOException e) {
            Debug.logError(e, "Error in CompDoc operation: ", module);
            return ServiceUtil.returnError(e.toString());
        } catch (Exception e) {
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
        String oooHost = (String) context.get("oooHost");
        String oooPort = (String) context.get("oooPort");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        try {
            //Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
            //ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document();
            document.setPageSize(PageSize.LETTER);
            //Rectangle rect = document.getPageSize();
            //PdfCopy writer = new PdfCopy(document, baos);
            document.open();

            GenericValue dataResource = null;
            if (UtilValidate.isEmpty(contentRevisionSeqId)) {
                GenericValue content = delegator.findOne("Content", UtilMisc.toMap("contentId", contentId), true);
                dataResourceId = content.getString("dataResourceId");
                Debug.logInfo("SCVH(0b)- dataResourceId:" + dataResourceId, module);
                dataResource = delegator.findOne("DataResource", UtilMisc.toMap("dataResourceId", dataResourceId), false);
             } else {
                GenericValue contentRevisionItem = delegator.findOne("ContentRevisionItem", UtilMisc.toMap("contentId", contentId, "itemContentId", contentId, "contentRevisionSeqId", contentRevisionSeqId), true);
                if (contentRevisionItem == null) {
                    throw new ViewHandlerException("ContentRevisionItem record not found for contentId=" + contentId
                                                   + ", contentRevisionSeqId=" + contentRevisionSeqId + ", itemContentId=" + contentId);
                }
                Debug.logInfo("SCVH(1)- contentRevisionItem:" + contentRevisionItem, module);
                Debug.logInfo("SCVH(2)-contentId=" + contentId
                        + ", contentRevisionSeqId=" + contentRevisionSeqId + ", itemContentId=" + contentId, module);
                dataResourceId = contentRevisionItem.getString("newDataResourceId");
                Debug.logInfo("SCVH(3)- dataResourceId:" + dataResourceId, module);
                dataResource = delegator.findOne("DataResource", UtilMisc.toMap("dataResourceId", dataResourceId), false);
            }
            String inputMimeType = null;
            if (dataResource != null) {
                inputMimeType = dataResource.getString("mimeTypeId");
            }
            byte [] inputByteArray = null;
            if (inputMimeType != null && inputMimeType.equals("application/pdf")) {
                ByteBuffer byteBuffer = DataResourceWorker.getContentAsByteBuffer(delegator, dataResourceId, https, webSiteId, locale, rootDir);
                inputByteArray = byteBuffer.array();
            } else if (inputMimeType != null && inputMimeType.equals("text/html")) {
                ByteBuffer byteBuffer = DataResourceWorker.getContentAsByteBuffer(delegator, dataResourceId, https, webSiteId, locale, rootDir);
                inputByteArray = byteBuffer.array();
                String s = new String(inputByteArray);
                Debug.logInfo("text/html string:" + s, module);
            } else if (inputMimeType != null && inputMimeType.equals("application/vnd.ofbiz.survey.response")) {
                String surveyResponseId = dataResource.getString("relatedDetailId");
                String surveyId = null;
                String acroFormContentId = null;
                GenericValue surveyResponse = null;
                if (UtilValidate.isNotEmpty(surveyResponseId)) {
                    surveyResponse = delegator.findOne("SurveyResponse", UtilMisc.toMap("surveyResponseId", surveyResponseId), false);
                    if (surveyResponse != null) {
                        surveyId = surveyResponse.getString("surveyId");
                    }
                }
                if (UtilValidate.isNotEmpty(surveyId)) {
                    GenericValue survey = delegator.findOne("Survey", UtilMisc.toMap("surveyId", surveyId), false);
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
                ByteBuffer inByteBuffer = DataResourceWorker.getContentAsByteBuffer(delegator, dataResourceId, https, webSiteId, locale, rootDir);

                Map<String, Object> convertInMap = UtilMisc.<String, Object>toMap("userLogin", userLogin, "inByteBuffer", inByteBuffer,
                        "inputMimeType", inputMimeType, "outputMimeType", "application/pdf");
                if (UtilValidate.isNotEmpty(oooHost)) convertInMap.put("oooHost", oooHost);
                if (UtilValidate.isNotEmpty(oooPort)) convertInMap.put("oooPort", oooPort);

                Map<String, Object> convertResult = dispatcher.runSync("convertDocumentByteBuffer", convertInMap);

                if (ServiceUtil.isError(convertResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ContentConvertingDocumentByteBuffer", locale), null, null, convertResult);
                }

                ByteBuffer outByteBuffer = (ByteBuffer) convertResult.get("outByteBuffer");
                inputByteArray = outByteBuffer.array();
            }

            ByteBuffer outByteBuffer = ByteBuffer.wrap(inputByteArray);
            results.put("outByteBuffer", outByteBuffer);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.toString());
        } catch (IOException e) {
            Debug.logError(e, "Error in PDF generation: ", module);
            return ServiceUtil.returnError(e.toString());
        } catch (Exception e) {
            Debug.logError(e, "Error in PDF generation: ", module);
            return ServiceUtil.returnError(e.toString());
        }
        return results;
    }
}
