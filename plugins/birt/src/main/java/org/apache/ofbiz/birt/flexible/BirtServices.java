/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.apache.ofbiz.birt.flexible;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.xml.sax.SAXException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.birt.BirtWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.framework.Platform;
import org.eclipse.birt.report.engine.api.script.IReportContext;
import org.eclipse.birt.report.model.api.DesignConfig;
import org.eclipse.birt.report.model.api.DesignElementHandle;
import org.eclipse.birt.report.model.api.DesignFileException;
import org.eclipse.birt.report.model.api.IDesignEngine;
import org.eclipse.birt.report.model.api.IDesignEngineFactory;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.birt.report.model.api.SessionHandle;
import org.eclipse.birt.report.model.api.SimpleMasterPageHandle;
import org.eclipse.birt.report.model.api.SlotHandle;
import org.eclipse.birt.report.model.api.VariableElementHandle;
import org.eclipse.birt.report.model.api.activity.SemanticException;
import org.eclipse.birt.report.model.elements.SimpleMasterPage;

import com.ibm.icu.util.ULocale;
import org.w3c.dom.Document;


/**
 * Birt Services
 */

public class BirtServices {

    public static final String module = BirtServices.class.getName();
    public static final String resource = "BirtUiLabels";
    public static final String resource_error = "BirtErrorUiLabels";
    public static final String resourceProduct = "BirtUiLabels";

    /**
     * Instanciate a new Flexible report, using the data given in parameters and <code>ReportDesignGenerator</code> class.
     */
    public static Map<String, Object> createFlexibleReport(DispatchContext dctx, Map<String, Object> context) {
        ReportDesignGenerator rptGenerator;
        try {
            rptGenerator = new ReportDesignGenerator(context, dctx);
        } catch (Exception e1) {
            e1.printStackTrace();
            return ServiceUtil.returnError(e1.getMessage());
        }
        try {
            rptGenerator.buildReport();
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    @Deprecated
    public static Map<String, Object> prepareFlexibleReportOptionFieldsFromEntity(DispatchContext dctx, Map<String, Object> context) {
        String entityViewName = (String) context.get("entityViewName");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        List<String> listMultiFields = new ArrayList<String>();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> result = new HashMap<String, Object>();
        Locale locale = (Locale) context.get("locale");

        ModelEntity modelEntity = delegator.getModelEntity(entityViewName);
        List<String> listFieldsEntity = modelEntity.getAllFieldNames();

        for (String field : listFieldsEntity) {
            listMultiFields.add(field);
            ModelField mField = modelEntity.getField(field);
            String fieldType = mField.getType();
            String birtType = null;
            try {
                Map<String, Object> convertRes = dispatcher.runSync("convertFieldTypeToBirtType", UtilMisc.toMap("fieldType", fieldType, "userLogin", userLogin));
                birtType = (String) convertRes.get("birtType");
                if (UtilValidate.isEmpty(birtType)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "BirtErrorConversionFieldToBirtFailed", locale));
                }
            } catch (GenericServiceException e) {
                e.printStackTrace();
            }
            // make more general when report forms have been made so too.
            if (birtType.equalsIgnoreCase("date-time") || birtType.equalsIgnoreCase("date") || birtType.equalsIgnoreCase("time")) {
                listMultiFields.add(field + "_fld0_op");
                listMultiFields.add(field + "_fld0_value");
                listMultiFields.add(field + "_fld1_op");
                listMultiFields.add(field + "_fld1_value");
            }
        }
        result.put("listMultiFields", listMultiFields);
        return result;
    }

    /**
     * Perform find data on given view/entity and return these into birt compatible format.
     * This service is meant to be used as default for View/entity report design
     *
     */
    public static Map<String, Object> callPerformFindFromBirt(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        IReportContext reportContext = (IReportContext) context.get("reportContext");
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String entityViewName = (String) reportContext.getParameterValue("modelElementName");
        Map<String, Object> inputFields = (Map<String, Object>) reportContext.getParameterValue("parameters");
        Map<String, Object> resultPerformFind = new HashMap<String, Object>();
        Map<String, Object> resultToBirt = null;
        List<GenericValue> list = null;

        if (UtilValidate.isEmpty(entityViewName)) {
            entityViewName = (String) inputFields.get("modelElementName");
            if (UtilValidate.isEmpty(entityViewName)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "BirtErrorEntityViewNotFound", locale));
            }
        }

        try {
            resultPerformFind = dispatcher.runSync("performFind", UtilMisc.<String, Object>toMap("entityName", entityViewName, "inputFields", inputFields, "userLogin", userLogin, "noConditionFind", "Y", "locale", locale));
            if (ServiceUtil.isError(resultPerformFind)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "BirtErrorRunningPerformFind", locale));
            }
        } catch (GenericServiceException e) {
            e.printStackTrace();
            return ServiceUtil.returnError(e.getMessage());
        }

        EntityListIterator listIt = (EntityListIterator) resultPerformFind.get("listIt");
        try {
            if (UtilValidate.isNotEmpty(listIt)) {
                list = listIt.getCompleteList();
                listIt.close();
            } else {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "BirtErrorRunningPerformFind", locale));
            }
        } catch (GenericEntityException e) {
            e.printStackTrace();
        }
        resultToBirt = ServiceUtil.returnSuccess();
        resultToBirt.put("records", list);
        return resultToBirt;
    }

    /**
     * Analyse given master and create report design from its data
     * Two cases are implemented :
     * <ul>
     *     <li>Entity : data retieval is based on a simple view/entity</li>
     *     <li>Service : data retrieval is based on service</li>
     * </ul>
     */
    public static Map<String, Object> createFlexibleReportFromMaster(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");

        String reportName = (String) context.get("reportName");
        String masterContentId = (String) context.get("contentId");
        String description = (String) context.get("description");
        String writeFilters = (String) context.get("writeFilters");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        GenericValue masterContentAttribute = null;
        try {
            EntityCondition entityCondition = EntityCondition.makeCondition("contentId", masterContentId);
            masterContentAttribute = EntityQuery.use(delegator).from("ContentAttribute").where(entityCondition).queryFirst();
        } catch (GenericEntityException e) {
            e.printStackTrace();
            return ServiceUtil.returnError(e.getMessage());
        }

        if (masterContentAttribute == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "BirtErrorNoAttributeFound", locale));
        }
        String attrName = masterContentAttribute.getString("attrName");
        String reportContentId;
        if (attrName.equalsIgnoreCase("Entity")) {
            String entityViewName = masterContentAttribute.getString("attrValue");
                ModelEntity modelEntity = delegator.getModelEntity(entityViewName);
                if (modelEntity == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "BirtErrorEntityViewNotExist", locale) + " " + entityViewName);
                }
            try {
                Map<String, Object> resultContent = dispatcher.runSync("createFlexibleReportFromMasterEntityWorkflow", UtilMisc.toMap("entityViewName", entityViewName, "reportName", reportName, "description", description, "writeFilters", writeFilters, "masterContentId", masterContentId, "userLogin", userLogin, "locale", locale));
                if(ServiceUtil.isError(resultContent)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(resultContent));
                }
                reportContentId = (String) resultContent.get("contentId");
            } catch (GenericServiceException e) {
                e.printStackTrace();
                return ServiceUtil.returnError(e.getMessage());
            }
        } else if (attrName.equalsIgnoreCase("Service")) {
            String serviceName = masterContentAttribute.getString("attrValue");
            try {
                Map<String, Object> resultContent = dispatcher.runSync("createFlexibleReportFromMasterServiceWorkflow", UtilMisc.toMap("serviceName", serviceName, "reportName", reportName, "description", description, "writeFilters", writeFilters, "masterContentId", masterContentId, "userLogin", userLogin, "locale", locale));
                if (ServiceUtil.isError(resultContent)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(resultContent));
                }
                reportContentId = (String) resultContent.get("contentId");
            } catch (GenericServiceException e) {
                e.printStackTrace();
                return ServiceUtil.returnError(e.getMessage());
            }
        } else {
            // could create other workflows. WebService? Does it need to be independent from Service workflow?
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "BirtErrorCannotDetermineDataSource", locale));
        }

        // prepare report form to display to allow override
        String textForm;
        Map<String, Object> resultFormDisplay;
        try {
            resultFormDisplay = dispatcher.runSync("prepareFlexibleReportSearchFormToEdit", UtilMisc.toMap("reportContentId", reportContentId, "userLogin", userLogin, "locale", locale));
            textForm = (String) resultFormDisplay.get("textForm");
        } catch (GenericServiceException e) {
            e.printStackTrace();
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "BirtErrorCreatingDefaultSearchForm", locale).concat(": ").concat(e.getMessage()));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "BirtFlexibleReportSuccessfullyGenerated", locale).concat(" ").concat(reportName));
        result.put("textForm", textForm);
        result.put("reportContentId", reportContentId);
        return result;
    }

    // I'm not a big fan of how I did the createFormForDisplay / overrideReportForm. Could probably be improved using a proper formForReport object or something similar.

    /**
     * Update search form of a report design
     */
    public static Map<String, Object> overrideReportForm(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String reportContentId = (String) context.get("reportContentId");
        String overrideFilters = (String) context.get("overrideFilters");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // safety check : do not accept "${groovy", "${bsh" and "javascript"
        String overideFiltersNoWhiteSpace = overrideFilters.replaceAll("\\s", "");
        if (overideFiltersNoWhiteSpace.contains("${groovy:") || overideFiltersNoWhiteSpace.contains("${bsh:") || overideFiltersNoWhiteSpace.contains("javascript:")) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "BirtErrorUnauthorisedCharacter", locale));
        }

        try {
            GenericValue content = EntityQuery.use(delegator).from("Content").where("contentId", reportContentId).queryOne();
            String dataResourceId = content.getString("dataResourceId");
            StringBuffer newForm = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?> <forms xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://ofbiz.apache.org/dtds/widget-form.xsd\">");
            newForm.append(overrideFilters);
            newForm.append("</forms>");
            Document xmlForm = UtilXml.readXmlDocument(newForm.toString());
            dispatcher.runSync("updateElectronicTextForm", UtilMisc.toMap("dataResourceId", dataResourceId, "textData", UtilXml.writeXmlDocument(xmlForm), "userLogin", userLogin, "locale", locale));
        } catch (GeneralException | SAXException | ParserConfigurationException | IOException e) {
            e.printStackTrace();
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "BirtSearchFormSuccessfullyOverridde", locale));
    }

    /**
     * Create report design from View/Entity master report
     */
    public static Map<String, Object> createFlexibleReportFromMasterEntityWorkflow(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String writeFilters = (String) context.get("writeFilters");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String entityViewName = (String) context.get("entityViewName");

        ModelEntity modelEntity = delegator.getModelEntity(entityViewName);
        String contentId = null;
        Map<String, Object> result = ServiceUtil.returnSuccess();
        try {
            Map<String, Object> resultMapsForGeneration = dispatcher.runSync("prepareFlexibleReportFieldsFromEntity", UtilMisc.toMap("modelEntity", modelEntity, "userLogin", userLogin, "locale", locale));
            if (ServiceUtil.isError(resultMapsForGeneration)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(resultMapsForGeneration));
            }
            Map<String, String> dataMap = (Map<String, String>) resultMapsForGeneration.get("dataMap");
            Map<String, String> fieldDisplayLabels = null;
            if (UtilValidate.isNotEmpty(resultMapsForGeneration.get("fieldDisplayLabels"))) {
                fieldDisplayLabels = (Map<String, String>) resultMapsForGeneration.get("fieldDisplayLabels");
            }
            Map<String, String> filterMap = null;
            if (UtilValidate.isNotEmpty(resultMapsForGeneration.get("filterMap"))) {
                filterMap = (Map<String, String>) resultMapsForGeneration.get("filterMap");
            }
            Map<String, String> filterDisplayLabels = null;
            if (UtilValidate.isNotEmpty(resultMapsForGeneration.get("filterDisplayLabels"))) {
                filterDisplayLabels = (Map<String, String>) resultMapsForGeneration.get("filterDisplayLabels");
            }
            contentId = BirtWorker.recordReportContent(delegator, dispatcher, context);
            // callPerformFindFromBirt is the customMethod for Entity workflow
            String rptDesignFileName = BirtUtil.resolveRptDesignFilePathFromContent(delegator, contentId);
            GenericValue content = EntityQuery.use(delegator).from("Content").where("contentId", contentId).queryOne();
            String customMethodId = content.getString("customMethodId");
            if (UtilValidate.isEmpty(customMethodId)) customMethodId = "CM_FB_PERFORM_FIND";
            GenericValue customMethod = EntityQuery.use(delegator).from("CustomMethod").where("customMethodId", customMethodId).cache().queryOne();
            if (customMethod == null) {
                return ServiceUtil.returnError("CustomMethod not exist : " + customMethodId); //todo labelise
            }
            result = dispatcher.runSync("createFlexibleReport", UtilMisc.toMap(
                    "locale", locale,
                    "dataMap", dataMap,
                    "userLogin", userLogin,
                    "filterMap", filterMap,
                    "serviceName", customMethod.get("customMethodName"),
                    "writeFilters", writeFilters,
                    "rptDesignName", rptDesignFileName,
                    "fieldDisplayLabels", fieldDisplayLabels,
                    "filterDisplayLabels", filterDisplayLabels));
            if (ServiceUtil.isError(result)) {
                return ServiceUtil.returnError(ServiceUtil.getErrorMessage(result));
            }
        } catch (GeneralException e) {
            e.printStackTrace();
            return ServiceUtil.returnError(e.getMessage());
        }
        result.put("contentId", contentId);
        return result;
    }

    /**
     * Create report design from service master report
     */
    public static Map<String, Object> createFlexibleReportFromMasterServiceWorkflow(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        String writeFilters = (String) context.get("writeFilters");
        String serviceName = (String) context.get("serviceName");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String masterContentId = (String) context.get("masterContentId");
        String contentId = null;
        Map<String, Object> result = ServiceUtil.returnSuccess();

        try {
            GenericValue masterContent = EntityQuery.use(delegator).from("Content").where("contentId", masterContentId).cache().queryOne();
            String customMethodId = masterContent.getString("customMethodId");
            if (UtilValidate.isEmpty(customMethodId)) {
                throw new GeneralException("The master content " + masterContentId + " haven't a customMethod");
            }
            GenericValue customMethod = EntityQuery.use(delegator).from("CustomMethod").where("customMethodId", customMethodId).cache().queryOne();
            if (customMethod == null) {
                return ServiceUtil.returnError("CustomMethod not exist : " + customMethodId); //todo labelise
            }
            String customMethodName = (String) customMethod.getString("customMethodName");
            if ("default".equalsIgnoreCase(serviceName)) {
                serviceName = customMethodName + "PrepareFields";
            }
            try {
                ModelService modelService = dctx.getModelService(serviceName);
            } catch (GenericServiceException e) {
                return ServiceUtil.returnError("No service define with name " + serviceName); //TODO labelise
            }
            contentId = BirtWorker.recordReportContent(delegator, dispatcher, context);
            String rptDesignFileName = BirtUtil.resolveRptDesignFilePathFromContent(delegator, contentId);
            Map<String, Object> resultService = dispatcher.runSync(serviceName, UtilMisc.toMap("locale", locale, "userLogin", userLogin));
            Map<String, String> dataMap = (Map<String, String>) resultService.get("dataMap");
            Map<String, String> filterMap = (Map<String, String>) resultService.get("filterMap");
            Map<String, String> fieldDisplayLabels = (Map<String, String>) resultService.get("fieldDisplayLabels");
            Map<String, String> filterDisplayLabels = (Map<String, String>) resultService.get("filterDisplayLabels");
            Map<String, Object> resultGeneration = dispatcher.runSync("createFlexibleReport", UtilMisc.toMap(
                    "locale", locale,
                    "dataMap", dataMap,
                    "userLogin", userLogin,
                    "filterMap", filterMap,
                    "serviceName", customMethodName,
                    "writeFilters", writeFilters,
                    "rptDesignName", rptDesignFileName,
                    "fieldDisplayLabels", fieldDisplayLabels,
                    "filterDisplayLabels", filterDisplayLabels));
            if (ServiceUtil.isError(resultGeneration)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "BirtErrorCreatingFlexibleReport", locale));
            }
        } catch (GeneralException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        result.put("contentId", contentId);
        return result;
    }

    /**
     * Define which data fields and its label, filter fields and label that will be supported by the View/Entity report design
     */
    public static Map<String, Object> prepareFlexibleReportFieldsFromEntity(DispatchContext dctx, Map<String, Object> context) {
        Locale locale = (Locale) context.get("locale");
        ModelEntity modelEntity = (ModelEntity) context.get("modelEntity");

        Map<String, String> dataMap = new HashMap<String, String>();
        Map<String, String> fieldDisplayLabels = new HashMap<String, String>();
        LinkedHashMap<String, String> filterMap = new LinkedHashMap<String, String>();
        LinkedHashMap<String, String> filterDisplayLabels = new LinkedHashMap<String, String>();

        List<String> listEntityFields = modelEntity.getAllFieldNames();
        Map<Object, Object> uiLabelMap = new HashMap<Object, Object>();
        final String[] resourceGlob = {"OrderUiLabels", "ProductUiLabels", "PartyUiLabels", "ContentUiLabels", "AccountingUiLabels", "CommonUiLabels", "BirtUiLabels"};
        for (String res : resourceGlob) {
            uiLabelMap.putAll(UtilProperties.getProperties(res, locale));
        }

        List<String> excludeFields = modelEntity.getAutomaticFieldNames();
        for (String field : listEntityFields) {
            ModelField mField = modelEntity.getField(field);
            //ignore stamps fields
            if (excludeFields.contains(mField.getName())) continue;
            dataMap.put(field, mField.getType());

            String localizedName = null;
            String interpretedFieldName = null;
            FlexibleStringExpander.getInstance(mField.getDescription()).expandString(context);
            String titleFieldName = "FormFieldTitle_".concat(field);
            localizedName = (String) uiLabelMap.get(titleFieldName);
            if (UtilValidate.isEmpty(localizedName) || localizedName.equals(titleFieldName)) {
                interpretedFieldName = FlexibleStringExpander.getInstance(field).expandString(context);
                fieldDisplayLabels.put(field, interpretedFieldName);
            } else {
                fieldDisplayLabels.put(field, localizedName);
            }

            List<String> fieldTypeWithRangeList = UtilMisc.toList("date", "date-time", "time", "floating-point", "currency-amount", "numeric");
            if (fieldTypeWithRangeList.contains(mField.getType())) {
                filterMap.put(field.concat("_fld0_value"), mField.getType());
                filterMap.put(field.concat("_fld0_op"), "short-varchar");
                filterMap.put(field.concat("_fld1_value"), mField.getType());
                filterMap.put(field.concat("_fld1_op"), "short-varchar");
                filterDisplayLabels.put(field.concat("_fld0_value"), fieldDisplayLabels.get(field).concat(UtilProperties.getMessage(resource, "BirtFindFieldOptionValue0", locale)));
                filterDisplayLabels.put(field.concat("_fld0_op"), fieldDisplayLabels.get(field).concat(UtilProperties.getMessage(resource, "BirtFindFieldOptionValue0", locale).concat(UtilProperties.getMessage(resource, "BirtFindCompareOperator", locale))));
                filterDisplayLabels.put(field.concat("_fld1_value"), fieldDisplayLabels.get(field).concat(UtilProperties.getMessage(resource, "BirtFindFieldOptionValue1", locale)));
                filterDisplayLabels.put(field.concat("_fld1_op"), fieldDisplayLabels.get(field).concat(UtilProperties.getMessage(resource, "BirtFindFieldOptionValue1", locale).concat(UtilProperties.getMessage(resource, "BirtFindCompareOperator", locale))));
            } else { // remaining types need 4 fields (fld0-1_op-value)
                filterMap.put(field, mField.getType());
                filterMap.put(field.concat("_op"), "short-varchar");
                filterDisplayLabels.put(field, fieldDisplayLabels.get(field));
                filterDisplayLabels.put(field.concat("_op"), fieldDisplayLabels.get(field).concat(UtilProperties.getMessage(resource, "BirtFindCompareOperator", locale)));
            }
        }
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("dataMap", dataMap);
        if (UtilValidate.isNotEmpty(fieldDisplayLabels)) {
            result.put("fieldDisplayLabels", fieldDisplayLabels);
        }
        if (UtilValidate.isNotEmpty(filterMap)) {
            result.put("filterMap", filterMap);
        }
        if (UtilValidate.isNotEmpty(filterDisplayLabels)) {
            result.put("filterDisplayLabels", filterDisplayLabels);
        }
        return result;
    }

    /**
     * Prepare and return search form of a report design
     */
    public static Map<String, Object> createFormForDisplay(DispatchContext dctx, Map<String, Object> context) {
        String reportContentId = (String) context.get("reportContentId");
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = ServiceUtil.returnSuccess();

        String textData = null;
        try {
            GenericValue content = EntityQuery.use(delegator).from("Content").where("contentId", reportContentId).cache().queryOne();
            String dataResourceId = content.getString("dataResourceId");
            GenericValue electronicText = EntityQuery.use(delegator).from("ElectronicText").where("dataResourceId", dataResourceId).cache().queryOne();
            textData = electronicText.getString("textData");
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        if (Debug.infoOn()) Debug.logInfo(textData, module);
        textData = textData.substring(textData.indexOf("<form "), textData.length());
        if (textData.contains("</form>")) {
            textData = textData.substring(0, textData.indexOf("</form>") + 7);
        } else {
            textData = textData.substring(0, textData.indexOf("/>") + 2);
        }
        textData = StringUtil.replaceString(textData, "$", "&#36;");
        result.put("textForm", textData);
        return result;
    }

    /**
     * delete all non-master report design
     */
    public static Map<String, Object> deleteAllReports(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        List<String> listContentId = null;
        List<GenericValue> listContent = null;
        EntityCondition entityConditionContent = EntityCondition.makeCondition("contentTypeId", "FLEXIBLE_REPORT");
        try {
            listContent = EntityQuery.use(delegator).from("Content").where(entityConditionContent).select("contentId").queryList();
        } catch (GenericEntityException e) {
            e.printStackTrace();
            return ServiceUtil.returnError(e.getMessage());
        }
        if (UtilValidate.isEmpty(listContent)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "BirtErrorNoFlexibleReportToDelete", locale));
        }
        listContentId = EntityUtil.getFieldListFromEntityList(listContent, "contentId", true);

        try {
            for (String contentId : listContentId) {
                Map<String, Object> returnMap = dispatcher.runSync("deleteFlexibleReport", UtilMisc.toMap("contentId", contentId, "userLogin", userLogin, "locale", locale));
                ServiceUtil.isError(returnMap);
            }
        } catch (GenericServiceException e) {
            e.printStackTrace();
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "BirtFlexibleReportsSuccessfullyDeleted", locale));
    }

    /**
     * Delete a flexible report design
     */
    public static Map<String, Object> deleteFlexibleReport(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String contentId = (String) context.get("contentId");

        List<GenericValue> listContentRpt = null;
        List<GenericValue> listRptDesignFileGV = null;
        String contentIdRpt;
        try {
            listContentRpt = EntityQuery.use(delegator).from("ContentAssoc").where("contentId", contentId).select("contentIdTo").queryList();
            contentIdRpt = listContentRpt.get(0).getString("contentIdTo");
            List<EntityExpr> listConditions = UtilMisc.toList(EntityCondition.makeCondition("contentTypeId", EntityOperator.EQUALS, "RPTDESIGN"), EntityCondition.makeCondition("contentId", EntityOperator.EQUALS, contentIdRpt));
            EntityConditionList<EntityExpr> ecl = EntityCondition.makeCondition(listConditions, EntityOperator.AND);
            listRptDesignFileGV = EntityQuery.use(delegator).from("ContentDataResourceView").where(ecl).select("drObjectInfo").queryList();
        } catch (GenericEntityException e1) {
            e1.printStackTrace();
            return ServiceUtil.returnError(e1.getMessage());
        }
        if (listRptDesignFileGV.size() > 1) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "BirtErrorUnexpectedNumberReportToDelete", locale));
        }
        List<String> listRptDesignFile = EntityUtil.getFieldListFromEntityList(listRptDesignFileGV, "drObjectInfo", false);
        String rptfileName = listRptDesignFile.get(0);
        Path path = Paths.get(rptfileName);
        try {
            if (! Files.deleteIfExists(path)) {
                ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "BirtErrorCannotLocateReportFile", locale));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ServiceUtil.returnError(e.getMessage());
        }
        try {
            delegator.removeByAnd("ContentAttribute", UtilMisc.toMap("contentId", contentId));
            dispatcher.runSync("removeContentAndRelated", UtilMisc.toMap("contentId", contentId, "userLogin", userLogin, "locale", locale));
            dispatcher.runSync("removeContentAndRelated", UtilMisc.toMap("contentId", contentIdRpt, "userLogin", userLogin, "locale", locale));
        } catch (GenericServiceException e) {
            e.printStackTrace();
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericEntityException e) {
            e.printStackTrace();
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess(UtilProperties.getMessage(resource, "BirtFlexibleReportSuccessfullyDeleted", locale));
    }

    /**
     * Update birt rptdesign file from uploaded one.
     * <p>This will update only STYLES, BODY, MASTERPAGE AND CUBES from existing rptdesign with uploaded ones.</p>
     *
     */
    public static Map<String, Object> uploadRptDesign(DispatchContext dctx, Map<String, Object> context) {
        String dataResourceId = (String) context.get("dataResourceIdRpt");
        Locale locale = (Locale) context.get("locale");
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = null;
        List<String> listSuccessMessage = new ArrayList<String>();

        // the idea is to allow only design to be uploaded. We use the stored file and add the new design from the uploaded file within.
        DesignConfig config = new DesignConfig();
        IDesignEngine engine = null;
        try {
            Platform.startup();
            IDesignEngineFactory factory = (IDesignEngineFactory) Platform.createFactoryObject(IDesignEngineFactory.EXTENSION_DESIGN_ENGINE_FACTORY);
            engine = factory.createDesignEngine(config);
        } catch (Exception e) {
            e.printStackTrace();
        }
        SessionHandle session = engine.newSessionHandle(ULocale.forLocale(locale));

        // get old file to restore dataset and datasource
        ByteBuffer newRptDesignBytes = (ByteBuffer) context.get("uploadRptDesign");
        if (newRptDesignBytes == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "BirtErrorCannotFindUploadedFile", locale));
        }

        GenericValue dataResource = null;
        try {
            dataResource = EntityQuery.use(delegator).from("DataResource").where("dataResourceId", dataResourceId).queryOne();
        } catch (GenericEntityException e1) {
            e1.printStackTrace();
            return ServiceUtil.returnError(e1.getMessage());
        }
        String rptDesignName = dataResource.getString("objectInfo");
        // start Birt API platfrom
        try {
            Platform.startup();
        } catch (BirtException e) {
            e.printStackTrace();
            return ServiceUtil.returnError("Cannot start Birt platform");
        }

        // get database design
        ReportDesignHandle designStored;
        try {
            designStored = session.openDesign(rptDesignName);
        } catch (DesignFileException e) {
            e.printStackTrace();
            return ServiceUtil.returnError(e.getMessage());
        }

        // check if design stored already has a body and delete it to avoid conflicts (taking into account only newly designed body)
        if (UtilValidate.isNotEmpty(designStored.getBody())) {
            SlotHandle bodyStored = designStored.getBody();

            Iterator<DesignElementHandle> iter = bodyStored.iterator();
            while (iter.hasNext()) {
                try {
                    iter.remove();
                } catch (Exception e) {
                    e.printStackTrace();
                    return ServiceUtil.returnError(e.getMessage());
                }
            }
        }

        // NEED TO COPY STYLES, BODY, MASTERPAGE AND CUBES; existing elements (in case I missed one):
        //[styles, parameters, dataSources, dataSets, pageSetup, components, body, scratchPad, templateParameterDefinitions, cubes, themes]
        // get user design
        String nameTempRpt = rptDesignName.substring(0, rptDesignName.lastIndexOf('.')).concat("_TEMP_.rptdesign");
        File file = new File(nameTempRpt);
        RandomAccessFile out;
        ReportDesignHandle designFromUser;
        try {
            out = new RandomAccessFile(file, "rw");
            out.write(newRptDesignBytes.array());
            out.close();
            designFromUser = session.openDesign(nameTempRpt);
            // user file is deleted straight away to prevent the use of the report as script entry (security)
            Path path = Paths.get(nameTempRpt);
            Files.deleteIfExists(path);
        } catch (Exception e) {
            e.printStackTrace();
            return ServiceUtil.returnError(e.getMessage());
        }

        //copy cube
        SlotHandle cubesFromUser = designFromUser.getCubes();
        Iterator<DesignElementHandle> iterCube = cubesFromUser.iterator();

        while (iterCube.hasNext()) {
            DesignElementHandle item = (DesignElementHandle) iterCube.next();
            DesignElementHandle copy = item.copy().getHandle(item.getModule());
            try {
                designStored.getCubes().add(copy);
            } catch (Exception e) {
                e.printStackTrace();
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        // copy body
        SlotHandle bodyFromUser = designFromUser.getBody();
        Iterator<DesignElementHandle> iter = bodyFromUser.iterator();

        while (iter.hasNext()) {
            DesignElementHandle item = (DesignElementHandle) iter.next();
            DesignElementHandle copy = item.copy().getHandle(item.getModule());
            try {
                designStored.getBody().add(copy);
            } catch (Exception e) {
                e.printStackTrace();
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        // deleting simple master page from design stored
        try {
            List<DesignElementHandle> listMasterPagesStored = designStored.getMasterPages().getContents();
            for (Object masterPage : listMasterPagesStored) {
                if (masterPage instanceof SimpleMasterPageHandle) {
                    designStored.getMasterPages().drop((DesignElementHandle) masterPage);
                }
            }

            // adding simple master page => tous ces casts et autres instanceof... c'est laid, mais c'est tellement galère que quand je trouve une solution qui marche... :s
            List<DesignElementHandle> listMasterPages = designFromUser.getMasterPages().getContents();
            for (DesignElementHandle masterPage : listMasterPages) {
                if (masterPage instanceof SimpleMasterPageHandle) {
                    designStored.getMasterPages().add((SimpleMasterPage) ((SimpleMasterPageHandle) masterPage).copy());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ServiceUtil.returnError(e.getMessage());
        }

        // page variables
        List<VariableElementHandle> pageVariablesUser = designFromUser.getPageVariables();
        for (VariableElementHandle pageVariable : pageVariablesUser) {
            try {
                designStored.setPageVariable(pageVariable.getName(), pageVariable.getPropertyBindingExpression(pageVariable.getName()));
            } catch (SemanticException e) {
                e.printStackTrace();
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        // copy styles
        SlotHandle stylesFromUser = designFromUser.getStyles();
        SlotHandle stylesStored = designStored.getStyles();

        // getting style names from stored report
        List<String> listStyleNames = new ArrayList<String>();
        Iterator<DesignElementHandle> iterStored = stylesStored.iterator();
        while (iterStored.hasNext()) {
            DesignElementHandle item = (DesignElementHandle) iterStored.next();
            listStyleNames.add(item.getName());
        }

        Iterator<DesignElementHandle> iterUser = stylesFromUser.iterator();

        // adding to styles those which are not already present
        while (iterUser.hasNext()) {
            DesignElementHandle item = (DesignElementHandle) iterUser.next();
            if (! listStyleNames.contains(item.getName())) {
                DesignElementHandle copy = item.copy().getHandle(item.getModule());
                try {
                    designStored.getStyles().add(copy);
                } catch (Exception e) {
                    e.printStackTrace();
                    return ServiceUtil.returnError(e.getMessage());
                }
            }
        }

        try {
            designStored.saveAs(rptDesignName);
        } catch (IOException e) {
            e.printStackTrace();
            return ServiceUtil.returnError(e.getMessage());
        }
        designFromUser.close();
        designStored.close();
        if (Debug.infoOn()) Debug.logInfo("####### Design uploaded: ".concat(rptDesignName), module);

        // should we as a secondary safety precaution delete any file finishing with _TEMP_.rptdesign?
        listSuccessMessage.add(UtilProperties.getMessage(resource, "BirtFlexibleRptDesignSuccessfullyUploaded", locale));
        result = ServiceUtil.returnSuccess(listSuccessMessage);
        return result;
    }

}