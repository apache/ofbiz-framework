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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.content.data.DataResourceWorker;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.ByteWrapper;
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
    
    /** 
     * 
     * @param request
     * @param response
     * @return
     * 
     * Creates the topmost Content entity of a Composite Document tree.
     * Also creates an "empty" Composite Document Instance Content entity.
     * Creates ContentRevision/Item records for each, as well.
     */

    public static Map persistRootCompDoc(DispatchContext dctx, Map context) {
        Map result = new HashMap();
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale)context.get("locale");
        GenericValue userLogin = (GenericValue)context.get("userLogin");
        String contentId = (String)context.get("contentId");
        //String instanceContentId = null;
        
        boolean contentExists = true;
        if (UtilValidate.isEmpty(contentId)) {
            contentExists = false;
        } else {
            try {
                GenericValue val = delegator.findByPrimaryKey("Content", UtilMisc.toMap("contentId", contentId));
                if (val == null)  contentExists = false;
            } catch(GenericEntityException e) {
                Debug.logError(e, "Error running serviceName persistContentAndAssoc", module);
                String errMsg = UtilProperties.getMessage(CoreEvents.err_resource, "coreEvents.error_modelservice_for_srv_name", locale);
                return ServiceUtil.returnError(errMsg);
           }
        }
        
        ModelService modelService = null;
        try {
            modelService = dispatcher.getDispatchContext().getModelService("persistContentAndAssoc");
        } catch (GenericServiceException e) {
            String errMsg = "Error getting model service for serviceName, 'persistContentAndAssoc'. " + e.toString();
            Debug.logError(errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        Map persistMap = modelService.makeValid(context, ModelService.IN_PARAM);
        persistMap.put("userLogin", userLogin);
        try {
            Map persistContentResult = dispatcher.runSync("persistContentAndAssoc", persistMap);
            if (ServiceUtil.isError(persistContentResult)) {
                //Debug.logError("Error running service 'persistContentAndAssoc'. " + ServiceUtil.getErrorMessage(persistContentResult), module);
                return ServiceUtil.returnError("Error saving content information: ", null, null, persistContentResult);
            }

            contentId = (String) persistContentResult.get("contentId");
            result.putAll(persistContentResult);
            //request.setAttribute("contentId", contentId);
            // Update ContentRevision and ContentRevisonItem

            Map contentRevisionMap = new HashMap();
            contentRevisionMap.put("itemContentId", contentId);
            contentRevisionMap.put("contentId", contentId);
            contentRevisionMap.put("userLogin", userLogin);

            Map persistRevResult = dispatcher.runSync("persistContentRevisionAndItem", contentRevisionMap);
            if (ServiceUtil.isError(persistRevResult)) {
                //Debug.logError("Error running service 'persistContentRevisionAndItem'. " + ServiceUtil.getErrorMessage(persistRevResult), module);
                return ServiceUtil.returnError("Error saving revision information: ", null, null, persistRevResult);
            }

            result.putAll(persistRevResult);
            return result;
        } catch(GenericServiceException e) {
            String errMsg = "Error running serviceName, 'persistContentAndAssoc'. " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
    }

    public static Map renderCompDocPdf(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        
        Locale locale = (Locale) context.get("locale");
        String rootDir = (String) context.get("rootDir");
        String webSiteId = (String) context.get("webSiteId");
        String https = (String) context.get("https");
        
        GenericDelegator delegator = dctx.getDelegator();
        
        String contentId = (String) context.get("contentId");
        String contentRevisionSeqId = (String) context.get("contentRevisionSeqId");
        String oooHost = (String) context.get("oooHost");
        String oooPort = (String) context.get("oooPort");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        
        try {   
            Timestamp nowTimestamp = UtilDateTime.nowTimestamp();
            List exprList = new ArrayList();
            exprList.add(new EntityExpr("contentIdTo", EntityOperator.EQUALS, contentId));
            exprList.add(new EntityExpr("rootRevisionContentId", EntityOperator.EQUALS, contentId));
            if (UtilValidate.isNotEmpty(contentRevisionSeqId)) {
                exprList.add(new EntityExpr("contentRevisionSeqId", EntityOperator.LESS_THAN_EQUAL_TO, contentRevisionSeqId));
            }
            exprList.add(new EntityExpr("contentAssocTypeId", EntityOperator.EQUALS, "COMPDOC_PART"));
            exprList.add(new EntityExpr("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, nowTimestamp));

            List thruList = new ArrayList();
            thruList.add(new EntityExpr("thruDate", EntityOperator.EQUALS, null));
            thruList.add(new EntityExpr("thruDate", EntityOperator.GREATER_THAN, nowTimestamp));
            exprList.add(new EntityConditionList(thruList, EntityOperator.OR));

            EntityConditionList conditionList = new EntityConditionList(exprList, EntityOperator.AND);
            
            String [] fields = {"rootRevisionContentId", "itemContentId", "maxRevisionSeqId", "contentId", "dataResourceId", "contentIdTo", "contentAssocTypeId", "fromDate", "sequenceNum"};
            List selectFields = UtilMisc.toListArray(fields);
            List orderByFields = UtilMisc.toList("sequenceNum");
            List compDocParts = delegator.findByCondition("ContentAssocRevisionItemView", conditionList, selectFields, orderByFields);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document();
            document.setPageSize(PageSize.LETTER);    
            //Rectangle rect = document.getPageSize();
            //PdfWriter writer = PdfWriter.getInstance(document, baos);
            PdfCopy writer = new PdfCopy(document, baos);
            document.open();
            Iterator iter = compDocParts.iterator();
            int pgCnt =0;
            while (iter.hasNext()) {
                GenericValue contentAssocRevisionItemView = (GenericValue)iter.next();
                //String thisContentId = contentAssocRevisionItemView.getString("contentId");
                //String thisContentRevisionSeqId = contentAssocRevisionItemView.getString("maxRevisionSeqId");
                String thisDataResourceId = contentAssocRevisionItemView.getString("dataResourceId");
                GenericValue dataResource = delegator.findByPrimaryKey("DataResource", UtilMisc.toMap("dataResourceId", thisDataResourceId));
                String inputMimeType = null;
                if(dataResource != null) {
                    inputMimeType = dataResource.getString("mimeTypeId");
                }
                byte [] inputByteArray = null;
                PdfReader reader = null;
                if (inputMimeType != null && inputMimeType.equals("application/pdf")) {
                    ByteWrapper byteWrapper = DataResourceWorker.getContentAsByteWrapper(delegator, thisDataResourceId, https, webSiteId, locale, rootDir);
                    inputByteArray = byteWrapper.getBytes();
                    reader = new PdfReader(inputByteArray);
                } else if (inputMimeType != null && inputMimeType.equals("text/html")) {
                    ByteWrapper byteWrapper = DataResourceWorker.getContentAsByteWrapper(delegator, thisDataResourceId, https, webSiteId, locale, rootDir);
                    inputByteArray = byteWrapper.getBytes();
                    String s = new String(inputByteArray);
                    Debug.logInfo("text/html string:" + s, module);
                    continue;
                } else if (inputMimeType != null && inputMimeType.equals("application/vnd.ofbiz.survey.response")) {
                    String surveyResponseId = dataResource.getString("relatedDetailId");
                    String surveyId = null;
                    String acroFormContentId = null;
                    GenericValue surveyResponse = null;
                    if (UtilValidate.isNotEmpty(surveyResponseId)) {
                        surveyResponse = delegator.findByPrimaryKey("SurveyResponse", UtilMisc.toMap("surveyResponseId", surveyResponseId));
                        if (surveyResponse != null) {
                            surveyId = surveyResponse.getString("surveyId");
                        }
                    }
                    if (UtilValidate.isNotEmpty(surveyId)) {
                        GenericValue survey = delegator.findByPrimaryKey("Survey", UtilMisc.toMap("surveyId", surveyId));
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
                            Map survey2PdfResults = dispatcher.runSync("buildPdfFromSurveyResponse", UtilMisc.toMap("surveyResponseId", surveyId));
                            if (ServiceUtil.isError(survey2PdfResults)) {
                                return ServiceUtil.returnError("Error building PDF from SurveyResponse: ", null, null, survey2PdfResults);
                            }

                            ByteWrapper outByteWrapper = (ByteWrapper)survey2PdfResults.get("outByteWrapper");
                            inputByteArray = outByteWrapper.getBytes();
                            reader = new PdfReader(inputByteArray);
                        } else {
                            // Fill in acroForm
                            Map survey2AcroFieldResults = dispatcher.runSync("setAcroFieldsFromSurveyResponse", UtilMisc.toMap("surveyResponseId", surveyResponseId));
                            if (ServiceUtil.isError(survey2AcroFieldResults)) {
                                return ServiceUtil.returnError("Error setting AcroFields from SurveyResponse: ", null, null, survey2AcroFieldResults);
                            }

                            ByteWrapper outByteWrapper = (ByteWrapper) survey2AcroFieldResults.get("outByteWrapper");
                            inputByteArray = outByteWrapper.getBytes();
                            reader = new PdfReader(inputByteArray);
                        }
                    }
                } else {
                    ByteWrapper inByteWrapper = DataResourceWorker.getContentAsByteWrapper(delegator, thisDataResourceId, https, webSiteId, locale, rootDir);

                    Map convertInMap = UtilMisc.toMap("userLogin", userLogin, "inByteWrapper", inByteWrapper, "inputMimeType", inputMimeType, "outputMimeType", "application/pdf");
                    if (UtilValidate.isNotEmpty(oooHost)) convertInMap.put("oooHost", oooHost);
                    if (UtilValidate.isNotEmpty(oooPort)) convertInMap.put("oooPort", oooPort);

                    Map convertResult = dispatcher.runSync("convertDocumentByteWrapper", convertInMap);
                    
                    if (ServiceUtil.isError(convertResult)) {
                        return ServiceUtil.returnError("Error in Open", null, null, convertResult);
                    }

                    ByteWrapper outByteWrapper = (ByteWrapper) convertResult.get("outByteWrapper");
                    inputByteArray = outByteWrapper.getBytes();
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
            ByteWrapper outByteWrapper = new ByteWrapper(baos.toByteArray());

            Map results = ServiceUtil.returnSuccess();
            results.put("outByteWrapper", outByteWrapper);
            return results;
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.toString());
        } catch (IOException e) {
            Debug.logError(e, "Error in CompDoc operation: ", module);
            return ServiceUtil.returnError(e.toString());
        } catch(Exception e) {
            Debug.logError(e, "Error in CompDoc operation: ", module);
            return ServiceUtil.returnError(e.toString());
        }
    }

    public static Map renderContentPdf(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map results = ServiceUtil.returnSuccess();
        String dataResourceId = null;
        
        Locale locale = (Locale) context.get("locale");
        String rootDir = (String) context.get("rootDir");
        String webSiteId = (String) context.get("webSiteId");
        String https = (String) context.get("https");
        
        GenericDelegator delegator = dctx.getDelegator();
        
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
                GenericValue content = delegator.findByPrimaryKeyCache("Content", UtilMisc.toMap("contentId", contentId));
                dataResourceId = content.getString("dataResourceId");
                Debug.logInfo("SCVH(0b)- dataResourceId:" + dataResourceId, module);
                dataResource = delegator.findByPrimaryKey("DataResource", UtilMisc.toMap("dataResourceId", dataResourceId));
             } else {
                GenericValue contentRevisionItem = delegator.findByPrimaryKeyCache("ContentRevisionItem", UtilMisc.toMap("contentId", contentId, "itemContentId", contentId, "contentRevisionSeqId", contentRevisionSeqId));
                if (contentRevisionItem == null) {
                    throw new ViewHandlerException("ContentRevisionItem record not found for contentId=" + contentId
                                                   + ", contentRevisionSeqId=" + contentRevisionSeqId + ", itemContentId=" + contentId);
                }
                Debug.logInfo("SCVH(1)- contentRevisionItem:" + contentRevisionItem, module);
                Debug.logInfo("SCVH(2)-contentId=" + contentId
                        + ", contentRevisionSeqId=" + contentRevisionSeqId + ", itemContentId=" + contentId, module);
                dataResourceId = contentRevisionItem.getString("newDataResourceId");
                Debug.logInfo("SCVH(3)- dataResourceId:" + dataResourceId, module);
                dataResource = delegator.findByPrimaryKey("DataResource", UtilMisc.toMap("dataResourceId", dataResourceId));
            }
            String inputMimeType = null;
            if(dataResource != null) {
                inputMimeType = dataResource.getString("mimeTypeId");
            }
            byte [] inputByteArray = null;
            if (inputMimeType != null && inputMimeType.equals("application/pdf")) {
                ByteWrapper byteWrapper = DataResourceWorker.getContentAsByteWrapper(delegator, dataResourceId, https, webSiteId, locale, rootDir);
                inputByteArray = byteWrapper.getBytes();
            } else if (inputMimeType != null && inputMimeType.equals("text/html")) {
                ByteWrapper byteWrapper = DataResourceWorker.getContentAsByteWrapper(delegator, dataResourceId, https, webSiteId, locale, rootDir);
                inputByteArray = byteWrapper.getBytes();
                String s = new String(inputByteArray);
                Debug.logInfo("text/html string:" + s, module);
            } else if (inputMimeType != null && inputMimeType.equals("application/vnd.ofbiz.survey.response")) {
                String surveyResponseId = dataResource.getString("relatedDetailId");
                String surveyId = null;
                String acroFormContentId = null;
                GenericValue surveyResponse = null;
                if (UtilValidate.isNotEmpty(surveyResponseId)) {
                    surveyResponse = delegator.findByPrimaryKey("SurveyResponse", UtilMisc.toMap("surveyResponseId", surveyResponseId));
                    if (surveyResponse != null) {
                        surveyId = surveyResponse.getString("surveyId");
                    }
                }
                if (UtilValidate.isNotEmpty(surveyId)) {
                    GenericValue survey = delegator.findByPrimaryKey("Survey", UtilMisc.toMap("surveyId", surveyId));
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
                        Map survey2PdfResults = dispatcher.runSync("buildPdfFromSurveyResponse", UtilMisc.toMap("surveyResponseId", surveyResponseId));
                        if (ServiceUtil.isError(survey2PdfResults)) {
                            return ServiceUtil.returnError("Error building PDF from SurveyResponse: ", null, null, survey2PdfResults);
                        }

                        ByteWrapper outByteWrapper = (ByteWrapper)survey2PdfResults.get("outByteWrapper");
                        inputByteArray = outByteWrapper.getBytes();
                    } else {
                        // Fill in acroForm
                        Map survey2AcroFieldResults = dispatcher.runSync("setAcroFieldsFromSurveyResponse", UtilMisc.toMap("surveyResponseId", surveyResponseId));
                        if (ServiceUtil.isError(survey2AcroFieldResults)) {
                            return ServiceUtil.returnError("Error setting AcroFields from SurveyResponse: ", null, null, survey2AcroFieldResults);
                        }

                        ByteWrapper outByteWrapper = (ByteWrapper) survey2AcroFieldResults.get("outByteWrapper");
                        inputByteArray = outByteWrapper.getBytes();
                    }
                }
            } else {
                ByteWrapper inByteWrapper = DataResourceWorker.getContentAsByteWrapper(delegator, dataResourceId, https, webSiteId, locale, rootDir);
                
                Map convertInMap = UtilMisc.toMap("userLogin", userLogin, "inByteWrapper", inByteWrapper, 
                        "inputMimeType", inputMimeType, "outputMimeType", "application/pdf");
                if (UtilValidate.isNotEmpty(oooHost)) convertInMap.put("oooHost", oooHost);
                if (UtilValidate.isNotEmpty(oooPort)) convertInMap.put("oooPort", oooPort);

                Map convertResult = dispatcher.runSync("convertDocumentByteWrapper", convertInMap);
                
                if (ServiceUtil.isError(convertResult)) {
                    return ServiceUtil.returnError("Error in Open", null, null, convertResult);
                }

                ByteWrapper outByteWrapper = (ByteWrapper) convertResult.get("outByteWrapper");
                inputByteArray = outByteWrapper.getBytes();
            }
            
            ByteWrapper outByteWrapper = new ByteWrapper(inputByteArray);
            results.put("outByteWrapper", outByteWrapper);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.toString());
        } catch (IOException e) {
            Debug.logError(e, "Error in PDF generation: ", module);
            return ServiceUtil.returnError(e.toString());
        } catch(Exception e) {
            Debug.logError(e, "Error in PDF generation: ", module);
            return ServiceUtil.returnError(e.toString());
        }
        return results;
    }
}
