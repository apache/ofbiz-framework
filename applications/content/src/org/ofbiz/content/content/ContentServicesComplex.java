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
package org.ofbiz.content.content;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.SimpleMapProcessor;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;


/**
 * ContentServicesComplex Class
 */
public class ContentServicesComplex {

    public static final String module = ContentServicesComplex.class.getName();


   /*
    * A service that returns a list of ContentAssocDataResourceViewFrom/To views that are
    * associated with the passed in contentId. Other conditions are also applied, including:
    * a list of contentAssocTypeIds or contentTypeIds that the result set views must match.
    * A direction (From or To - case insensitive).
    * From and thru dates or date strings.
    * A mapKey value.
    */
    public static Map getAssocAndContentAndDataResource(DispatchContext dctx, Map context) {

        GenericDelegator delegator = dctx.getDelegator();
        List assocTypes = (List) context.get("assocTypes"); 
        List contentTypes = (List)context.get("contentTypes");
        Timestamp fromDate = (Timestamp)context.get("fromDate");
        Timestamp thruDate = (Timestamp)context.get("thruDate");
        String fromDateStr = (String)context.get("fromDateStr");
        String thruDateStr = (String)context.get("thruDateStr");
        String contentId = (String)context.get("contentId");
        String direction = (String)context.get("direction");
        String mapKey = (String)context.get("mapKey");
        Boolean nullThruDatesOnly = (Boolean)context.get("nullThruDatesOnly");
        Map results = getAssocAndContentAndDataResourceMethod(delegator,
                          contentId, mapKey, direction, fromDate, thruDate,
                          fromDateStr, thruDateStr, assocTypes, contentTypes);
        return results;
    }

    public static Map getAssocAndContentAndDataResourceMethod(GenericDelegator delegator, String contentId, String mapKey, String direction, Timestamp fromDate, Timestamp thruDate, String fromDateStr, String thruDateStr, List assocTypes, List contentTypes) {

        List exprList = new ArrayList();
        EntityExpr joinExpr = null;
        EntityExpr expr = null;
        String viewName = null;
        if (mapKey != null) {
            EntityExpr mapKeyExpr = new EntityExpr("caMapKey", EntityOperator.EQUALS, mapKey);
            exprList.add(mapKeyExpr);
        }
        if (direction != null && direction.equalsIgnoreCase("From")) {
            joinExpr = new EntityExpr("caContentIdTo", EntityOperator.EQUALS, contentId);
            viewName = "ContentAssocDataResourceViewFrom";
        } else {
            joinExpr = new EntityExpr("caContentId", EntityOperator.EQUALS, contentId);
            viewName = "ContentAssocDataResourceViewTo";
        }
        exprList.add(joinExpr);
        if (assocTypes != null && assocTypes.size() > 0) {
            List exprListOr = new ArrayList();
            Iterator it = assocTypes.iterator();
            while (it.hasNext()) {
                String assocType = (String)it.next();
                expr = new EntityExpr("caContentAssocTypeId", EntityOperator.EQUALS, assocType);
                exprListOr.add(expr);
            }
            EntityConditionList assocExprList = new EntityConditionList(exprListOr, EntityOperator.OR);

            exprList.add(assocExprList);
        }
        if (contentTypes != null && contentTypes.size() > 0) {
            List exprListOr = new ArrayList();
            Iterator it = contentTypes.iterator();
            while (it.hasNext()) {
                String contentType = (String)it.next();
                expr = new EntityExpr("contentTypeId", 
                                  EntityOperator.EQUALS, contentType);
                exprListOr.add(expr);
            }
            EntityConditionList contentExprList = new EntityConditionList(exprListOr, EntityOperator.OR);
            exprList.add(contentExprList);
        }

        if (fromDate == null && fromDateStr != null) {
            fromDate = UtilDateTime.toTimestamp(fromDateStr);
        }
        if (thruDate == null && thruDateStr != null) {
            thruDate = UtilDateTime.toTimestamp(thruDateStr);
        }

        if (fromDate != null) {
            EntityExpr fromExpr = new EntityExpr("caFromDate", EntityOperator.LESS_THAN, fromDate);
            exprList.add(fromExpr);
        }
        if (thruDate != null) {
            List thruList = new ArrayList();
            //thruDate = UtilDateTime.getDayStart(thruDate, daysLater);

            EntityExpr thruExpr = new EntityExpr("caThruDate", EntityOperator.LESS_THAN, thruDate);
            thruList.add(thruExpr);
            EntityExpr thruExpr2 = new EntityExpr("caThruDate", EntityOperator.EQUALS, null);
            thruList.add(thruExpr2);
            EntityConditionList thruExprList = new EntityConditionList(thruList, EntityOperator.OR);
            exprList.add(thruExprList);
        } else if (fromDate != null) {
            List thruList = new ArrayList();

            EntityExpr thruExpr = new EntityExpr("caThruDate", EntityOperator.GREATER_THAN, fromDate);
            thruList.add(thruExpr);
            EntityExpr thruExpr2 = new EntityExpr("caThruDate", EntityOperator.EQUALS, null);
            thruList.add(thruExpr2);
            EntityConditionList thruExprList = new EntityConditionList(thruList, EntityOperator.OR);
            exprList.add(thruExprList);
        }
        EntityConditionList assocExprList = new EntityConditionList(exprList, EntityOperator.AND);
        List relatedAssocs = null;
        try {
            //relatedAssocs = delegator.findByCondition(viewName, joinExpr, 
            relatedAssocs = delegator.findByCondition(viewName, assocExprList, 
                                  new ArrayList(),UtilMisc.toList("caFromDate"));
        } catch(GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        for (int i=0; i < relatedAssocs.size(); i++) {
            GenericValue a = (GenericValue)relatedAssocs.get(i);
                Debug.logVerbose(" contentId:" + a.get("contentId")
                         + " To:" + a.get("caContentIdTo")
                         + " fromDate:" + a.get("caFromDate")
                         + " thruDate:" + a.get("caThruDate")
                         + " AssocTypeId:" + a.get("caContentAssocTypeId")
                         ,null);

        }
        HashMap results = new HashMap();
        results.put("entityList", relatedAssocs);
        return results;
    }

   /*
    * A service that returns a list of ContentAssocDataResourceViewFrom/To views that are
    * associated with the passed in contentId. Other conditions are also applied, including:
    * a list of contentAssocTypeIds or contentTypeIds that the result set views must match.
    * A direction (From or To - case insensitive).
    * From and thru dates or date strings.
    * A mapKey value.
    */
    public static Map getAssocAndContentAndDataResourceCache(DispatchContext dctx, Map context) {

        GenericDelegator delegator = dctx.getDelegator();
        List assocTypes = (List) context.get("assocTypes"); 
        String assocTypesString = (String)context.get("assocTypesString");
        if (UtilValidate.isNotEmpty(assocTypesString)) {
            List lst = StringUtil.split(assocTypesString, "|");
            if (assocTypes == null) {
                assocTypes = new ArrayList();   
            }
            assocTypes.addAll(lst);
        }
        List contentTypes = (List)context.get("contentTypes");
        String contentTypesString = (String)context.get("contentTypesString");
        if (UtilValidate.isNotEmpty(contentTypesString)) {
            List lst = StringUtil.split(contentTypesString, "|");
            if (contentTypes == null) {
                contentTypes = new ArrayList();   
            }
            contentTypes.addAll(lst);
        }
        Timestamp fromDate = (Timestamp)context.get("fromDate");
        String fromDateStr = (String)context.get("fromDateStr");
        String contentId = (String)context.get("contentId");
        String direction = (String)context.get("direction");
        String mapKey = (String)context.get("mapKey");
        String contentAssocPredicateId = (String)context.get("contentAssocPredicateId");
        Boolean nullThruDatesOnly = (Boolean)context.get("nullThruDatesOnly");
        Map results = null;
        try {
            results = getAssocAndContentAndDataResourceCacheMethod(delegator,
                          contentId, mapKey, direction, fromDate, 
                          fromDateStr, assocTypes, contentTypes, nullThruDatesOnly, contentAssocPredicateId);
        } catch(GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch(MiniLangException e2) {
            return ServiceUtil.returnError(e2.getMessage());
        }
        return results;
    }


    public static Map getAssocAndContentAndDataResourceCacheMethod(GenericDelegator delegator, String contentId, String mapKey, String direction, 
                          Timestamp fromDate, String fromDateStr, List assocTypes, List contentTypes, Boolean nullThruDatesOnly, String contentAssocPredicateId) throws GenericEntityException, MiniLangException {
            Map results = getAssocAndContentAndDataResourceCacheMethod(delegator,
                          contentId, mapKey, direction, fromDate, fromDateStr, assocTypes, 
                          contentTypes, nullThruDatesOnly, contentAssocPredicateId, null);
            return results;
    }

    public static Map getAssocAndContentAndDataResourceCacheMethod(GenericDelegator delegator, String contentId, String mapKey, String direction, 
                          Timestamp fromDate, String fromDateStr, List assocTypes, List contentTypes, Boolean nullThruDatesOnly, String contentAssocPredicateId, String orderBy) throws GenericEntityException, MiniLangException {

        //List exprList = new ArrayList();
        //EntityExpr joinExpr = null;
        //EntityExpr expr = null;
        String viewName = null;
        GenericValue contentAssoc = null;
        String contentFieldName = null;
        if (direction != null && direction.equalsIgnoreCase("From")) {
            contentFieldName = "contentIdTo";
        } else {
            contentFieldName = "contentId";
        }
        if (direction != null && direction.equalsIgnoreCase("From")) {
            viewName = "ContentAssocDataResourceViewFrom";
        } else {
            viewName = "ContentAssocDataResourceViewTo";
        }
            //if (Debug.infoOn()) Debug.logInfo("in getAssocAndContent...Cache, assocTypes:" + assocTypes, module);
        Map fieldMap = UtilMisc.toMap(contentFieldName, contentId);
        if (assocTypes != null && assocTypes.size() == 1) {
            fieldMap.putAll(UtilMisc.toMap("contentAssocTypeId", assocTypes.get(0)));
        }
        if (UtilValidate.isNotEmpty(mapKey)) {
            if (mapKey.equalsIgnoreCase("is null"))
                fieldMap.putAll(UtilMisc.toMap("mapKey", null));
            else
                fieldMap.putAll(UtilMisc.toMap("mapKey", mapKey));
        }
        if (UtilValidate.isNotEmpty(contentAssocPredicateId)) {
            if (contentAssocPredicateId.equalsIgnoreCase("is null"))
                fieldMap.putAll(UtilMisc.toMap("contentAssocPredicateId", null));
            else
                fieldMap.putAll(UtilMisc.toMap("contentAssocPredicateId", contentAssocPredicateId));
        }
        if (nullThruDatesOnly != null && nullThruDatesOnly.booleanValue()) {
            fieldMap.putAll(UtilMisc.toMap("thruDate", null));
        }
        List contentAssocsUnfiltered = null;
        
            //if (Debug.infoOn()) Debug.logInfo("in getAssocAndContent...Cache, fieldMap:" + fieldMap, module);
        contentAssocsUnfiltered = delegator.findByAndCache("ContentAssoc", fieldMap, UtilMisc.toList("-fromDate"));

            //if (Debug.infoOn()) Debug.logInfo("in getAssocAndContent...Cache, contentAssocsUnfiltered:" + contentAssocsUnfiltered, module);
        if (fromDate == null && fromDateStr != null) {
            fromDate = UtilDateTime.toTimestamp(fromDateStr);
        }
        List contentAssocsDateFiltered2 = EntityUtil.filterByDate(contentAssocsUnfiltered, fromDate);
        List contentAssocsDateFiltered = EntityUtil.orderBy(contentAssocsDateFiltered2, UtilMisc.toList("sequenceNum", "fromDate DESC"));

        String contentAssocTypeId = null;
        List contentAssocsTypeFiltered = new ArrayList();
        if (assocTypes != null && assocTypes.size() > 1) {
            Iterator it = contentAssocsDateFiltered.iterator();
            while (it.hasNext()) {
                contentAssoc = (GenericValue)it.next();
                contentAssocTypeId = (String)contentAssoc.get("contentAssocTypeId");
                if (assocTypes.contains(contentAssocTypeId)) {
                    contentAssocsTypeFiltered.add(contentAssoc);
                }
            }
        } else {
            contentAssocsTypeFiltered = contentAssocsDateFiltered;
        }

        String assocRelationName = null;
        if (direction != null && direction.equalsIgnoreCase("To")) {
            assocRelationName = "ToContent";
        } else {
            assocRelationName = "FromContent";
        }

        GenericValue contentAssocDataResourceView = null;
        GenericValue content = null;
        GenericValue dataResource = null;
        List contentAssocDataResourceList = new ArrayList();
        Locale locale = Locale.getDefault(); // TODO: this needs to be passed in
        Iterator it = contentAssocsTypeFiltered.iterator();
        while (it.hasNext()) {
            contentAssoc = (GenericValue)it.next();
            content = contentAssoc.getRelatedOneCache(assocRelationName);
            if (contentTypes != null && contentTypes.size() > 0) {
                String contentTypeId = (String)content.get("contentTypeId");
                if (contentTypes.contains(contentTypeId)) {
                    contentAssocDataResourceView = delegator.makeValue(viewName, null);
                    contentAssocDataResourceView.setAllFields(content, true, null, null);
                }
            } else {
                contentAssocDataResourceView = delegator.makeValue(viewName, null);
                contentAssocDataResourceView.setAllFields(content, true, null, null);
            }
            SimpleMapProcessor.runSimpleMapProcessor("org/ofbiz/content/ContentManagementMapProcessors.xml", "contentAssocOut", contentAssoc, contentAssocDataResourceView, new ArrayList(), locale);
            //if (Debug.infoOn()) Debug.logInfo("contentAssoc:" + contentAssoc, module);
            //contentAssocDataResourceView.setAllFields(contentAssoc, false, null, null);
            String dataResourceId = content.getString("dataResourceId");
            if (UtilValidate.isNotEmpty(dataResourceId))
                dataResource = content.getRelatedOneCache("DataResource");
            //if (Debug.infoOn()) Debug.logInfo("dataResource:" + dataResource, module);
            //if (Debug.infoOn()) Debug.logInfo("contentAssocDataResourceView:" + contentAssocDataResourceView, module);
            if (dataResource != null) {
                //contentAssocDataResourceView.setAllFields(dataResource, false, null, null);
                SimpleMapProcessor.runSimpleMapProcessor("org/ofbiz/content/ContentManagementMapProcessors.xml", "dataResourceOut", dataResource, contentAssocDataResourceView, new ArrayList(), locale);
            }
            //if (Debug.infoOn()) Debug.logInfo("contentAssocDataResourceView:" + contentAssocDataResourceView, module);
            contentAssocDataResourceList.add(contentAssocDataResourceView);
        }

        List orderByList = null;
        if (UtilValidate.isNotEmpty(orderBy)) {
           orderByList = StringUtil.split(orderBy, "|");
           contentAssocDataResourceList = EntityUtil.orderBy(contentAssocDataResourceList, orderByList);
        }
        HashMap results = new HashMap();
        results.put("entityList", contentAssocDataResourceList);
        if (contentAssocDataResourceList != null && contentAssocDataResourceList.size() > 0) {
            results.put("view", contentAssocDataResourceList.get(0));
        }
        return results;
    }

/*
    public static Map getSubContentAndDataResource(GenericDelegator delegator, String contentId, String direction, Timestamp fromDate,  String assocType, String contentType, String orderBy) throws GenericEntityException {

        List exprList = new ArrayList();
        EntityExpr joinExpr = null;
        EntityExpr expr = null;
        String viewName = null;
        GenericValue contentAssoc = null;
        String contentFieldName = null;
        if (direction != null && direction.equalsIgnoreCase("From")) {
            viewName = "ContentAssocDataResourceViewFrom";
            contentFieldName = "contentIdTo";
            joinExpr = new EntityExpr("caContentIdTo", EntityOperator.EQUALS, contentId);
        } else {
            viewName = "ContentAssocDataResourceViewTo";
            contentFieldName = "contentId";
            joinExpr = new EntityExpr("caContentId", EntityOperator.EQUALS, contentId);
        }
        exprList.add(joinExpr);

        if (UtilValidate.isNotEmpty(assocType)) {
            expr = new EntityExpr("caContentAssocTypeId", EntityOperator.EQUALS, assocType);
            exprList.add(expr);
        }

        if (UtilValidate.isNotEmpty(contentType)) {
            expr = new EntityExpr("caContentTypeId", EntityOperator.EQUALS, contentType);
            exprList.add(expr);
        }

        List orderByList = null;
        if (UtilValidate.isNotEmpty(orderBy)) {
           orderByList = StringUtil.split(orderBy, "|");
           contentAssocDataResourceList = EntityUtil.orderBy(contentAssocDataResourceList, orderByList);
        }
        HashMap results = new HashMap();
        results.put("entityList", contentAssocDataResourceList);
        return results;
    }
*/
}
