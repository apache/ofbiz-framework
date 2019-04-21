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
package org.apache.ofbiz.content.content;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.SimpleMapProcessor;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;


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
    public static Map<String, Object> getAssocAndContentAndDataResource(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        List<String> assocTypes = UtilGenerics.checkList(context.get("assocTypes"));
        List<String> contentTypes = UtilGenerics.checkList(context.get("contentTypes"));
        Timestamp fromDate = (Timestamp)context.get("fromDate");
        Timestamp thruDate = (Timestamp)context.get("thruDate");
        String fromDateStr = (String)context.get("fromDateStr");
        String thruDateStr = (String)context.get("thruDateStr");
        String contentId = (String)context.get("contentId");
        String direction = (String)context.get("direction");
        String mapKey = (String)context.get("mapKey");
        Map<String, Object> results = getAssocAndContentAndDataResourceMethod(delegator, contentId, mapKey, direction, fromDate, thruDate, fromDateStr, thruDateStr, assocTypes, contentTypes);
        return results;
    }

    public static Map<String, Object> getAssocAndContentAndDataResourceMethod(Delegator delegator, String contentId, String mapKey, String direction, Timestamp fromDate, Timestamp thruDate, String fromDateStr, String thruDateStr, List<String> assocTypes, List<String> contentTypes) {
        List<EntityCondition> exprList = new LinkedList<>();
        EntityExpr joinExpr = null;
        String viewName = null;
        if (mapKey != null) {
            EntityExpr mapKeyExpr = EntityCondition.makeCondition("caMapKey", EntityOperator.EQUALS, mapKey);
            exprList.add(mapKeyExpr);
        }
        if (direction != null && "From".equalsIgnoreCase(direction)) {
            joinExpr = EntityCondition.makeCondition("caContentIdTo", EntityOperator.EQUALS, contentId);
            viewName = "ContentAssocDataResourceViewFrom";
        } else {
            joinExpr = EntityCondition.makeCondition("caContentId", EntityOperator.EQUALS, contentId);
            viewName = "ContentAssocDataResourceViewTo";
        }
        exprList.add(joinExpr);
        if (UtilValidate.isNotEmpty(assocTypes)) {
            exprList.add(EntityCondition.makeCondition("caContentAssocTypeId", EntityOperator.IN, assocTypes));
        }
        if (UtilValidate.isNotEmpty(contentTypes)) {
            exprList.add(EntityCondition.makeCondition("contentTypeId", EntityOperator.IN, contentTypes));
        }

        if (fromDate == null && fromDateStr != null) {
            fromDate = UtilDateTime.toTimestamp(fromDateStr);
        }
        if (thruDate == null && thruDateStr != null) {
            thruDate = UtilDateTime.toTimestamp(thruDateStr);
        }

        if (fromDate != null) {
            EntityExpr fromExpr = EntityCondition.makeCondition("caFromDate", EntityOperator.LESS_THAN, fromDate);
            exprList.add(fromExpr);
        }
        if (thruDate != null) {
            List<EntityExpr> thruList = new LinkedList<>();

            EntityExpr thruExpr = EntityCondition.makeCondition("caThruDate", EntityOperator.LESS_THAN, thruDate);
            thruList.add(thruExpr);
            EntityExpr thruExpr2 = EntityCondition.makeCondition("caThruDate", EntityOperator.EQUALS, null);
            thruList.add(thruExpr2);
            EntityConditionList<EntityExpr> thruExprList = EntityCondition.makeCondition(thruList, EntityOperator.OR);
            exprList.add(thruExprList);
        } else if (fromDate != null) {
            List<EntityExpr> thruList = new LinkedList<>();

            EntityExpr thruExpr = EntityCondition.makeCondition("caThruDate", EntityOperator.GREATER_THAN, fromDate);
            thruList.add(thruExpr);
            EntityExpr thruExpr2 = EntityCondition.makeCondition("caThruDate", EntityOperator.EQUALS, null);
            thruList.add(thruExpr2);
            EntityConditionList<EntityExpr> thruExprList = EntityCondition.makeCondition(thruList, EntityOperator.OR);
            exprList.add(thruExprList);
        }
        EntityConditionList<EntityCondition> assocExprList = EntityCondition.makeCondition(exprList, EntityOperator.AND);
        List<GenericValue> relatedAssocs = null;
        try {
            relatedAssocs = EntityQuery.use(delegator).from(viewName).where(assocExprList).orderBy("caFromDate").queryList();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
        for (int i=0; i < relatedAssocs.size(); i++) {
            GenericValue a = relatedAssocs.get(i);
            if (Debug.verboseOn()) Debug.logVerbose(" contentId:" + a.get("contentId") + " To:" + a.get("caContentIdTo") + " fromDate:" + a.get("caFromDate") + " thruDate:" + a.get("caThruDate") + " AssocTypeId:" + a.get("caContentAssocTypeId"), null);
        }
        Map<String, Object> results = new HashMap<>();
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
    public static Map<String, Object> getAssocAndContentAndDataResourceCache(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        List<String> assocTypes = UtilGenerics.checkList(context.get("assocTypes"));
        String assocTypesString = (String)context.get("assocTypesString");
        if (UtilValidate.isNotEmpty(assocTypesString)) {
            List<String> lst = StringUtil.split(assocTypesString, "|");
            if (assocTypes == null) {
                assocTypes = new LinkedList<>();
            }
            assocTypes.addAll(lst);
        }
        List<String> contentTypes = UtilGenerics.checkList(context.get("contentTypes"));
        String contentTypesString = (String)context.get("contentTypesString");
        if (UtilValidate.isNotEmpty(contentTypesString)) {
            List<String> lst = StringUtil.split(contentTypesString, "|");
            if (contentTypes == null) {
                contentTypes = new LinkedList<>();
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
        Map<String, Object> results = null;
        try {
            results = getAssocAndContentAndDataResourceCacheMethod(delegator, contentId, mapKey, direction, fromDate, fromDateStr, assocTypes, contentTypes, nullThruDatesOnly, contentAssocPredicateId, null);
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (MiniLangException e2) {
            return ServiceUtil.returnError(e2.getMessage());
        }
        return results;
    }

    public static Map<String, Object> getAssocAndContentAndDataResourceCacheMethod(Delegator delegator, String contentId, String mapKey, String direction,
                          Timestamp fromDate, String fromDateStr, List<String> assocTypes, List<String> contentTypes, Boolean nullThruDatesOnly, String contentAssocPredicateId, String orderBy) throws GenericEntityException, MiniLangException {
        EntityExpr joinExpr = null;
        String viewName = null;
        String contentFieldName = null;
        if (direction != null && "From".equalsIgnoreCase(direction)) {
            contentFieldName = "caContentIdTo";
            joinExpr = EntityCondition.makeCondition("caContentIdTo", EntityOperator.EQUALS, contentId);
        } else {
            contentFieldName = "caContentId";
            joinExpr = EntityCondition.makeCondition("contentId", EntityOperator.EQUALS, contentId);
        }
        if (direction != null && "From".equalsIgnoreCase(direction)) {
            viewName = "ContentAssocDataResourceViewFrom";
        } else {
            viewName = "ContentAssocDataResourceViewTo";
        }
        List<EntityCondition> conditionList = new ArrayList<>();
        conditionList.add(joinExpr);
        if (UtilValidate.isNotEmpty(mapKey)) {
            String mapKeyValue = "is null".equalsIgnoreCase(mapKey) ? null : mapKey;
            conditionList.add(EntityCondition.makeCondition("caMapKey", mapKeyValue));
        }
        if (UtilValidate.isNotEmpty(contentAssocPredicateId)) {
            String contentAssocPredicateIdValue = "is null".equalsIgnoreCase(contentAssocPredicateId) ? null : contentAssocPredicateId;
            conditionList.add(EntityCondition.makeCondition("caMapKey", contentAssocPredicateIdValue));
        }
        if (nullThruDatesOnly != null && nullThruDatesOnly) {
            conditionList.add(EntityCondition.makeCondition("caThruDate", null));
        }

        if (UtilValidate.isNotEmpty(assocTypes)) {
            conditionList.add(EntityCondition.makeCondition("caContentAssocTypeId", EntityOperator.IN, assocTypes));
        }

        List<GenericValue> contentAssocsTypeFiltered = EntityQuery.use(delegator).from(viewName)
                .where(conditionList).orderBy("caSequenceNum", "-caFromDate").cache().queryList();

        String assocRelationName = null;
        if (direction != null && "To".equalsIgnoreCase(direction)) {
            assocRelationName = "ToContent";
        } else {
            assocRelationName = "FromContent";
        }

        GenericValue contentAssocDataResourceView = null;
        GenericValue content = null;
        GenericValue dataResource = null;
        List<GenericValue> contentAssocDataResourceList = new LinkedList<>();
        Locale locale = Locale.getDefault(); // TODO: this needs to be passed in
        try{
        for (GenericValue contentAssocView : contentAssocsTypeFiltered) {
            GenericValue contentAssoc = EntityQuery.use(delegator).from("ContentAssoc").where(UtilMisc.toMap("contentId", contentAssocView.getString("contentId"),
                    "contentIdTo", contentAssocView.getString(contentFieldName), "contentAssocTypeId", contentAssocView.getString("caContentAssocTypeId"), 
                    "fromDate", contentAssocView.getTimestamp("caFromDate"))).queryOne();
            content = contentAssoc.getRelatedOne(assocRelationName, true);
            if (UtilValidate.isNotEmpty(contentTypes)) {
                String contentTypeId = (String)content.get("contentTypeId");
                if (contentTypes.contains(contentTypeId)) {
                    contentAssocDataResourceView = delegator.makeValue(viewName);
                    contentAssocDataResourceView.setAllFields(content, true, null, null);
                }
            } else {
                contentAssocDataResourceView = delegator.makeValue(viewName);
                contentAssocDataResourceView.setAllFields(content, true, null, null);
            }
            SimpleMapProcessor.runSimpleMapProcessor("component://content/minilang/ContentManagementMapProcessors.xml", "contentAssocOut", contentAssoc, contentAssocDataResourceView, new LinkedList<>(), locale);
            String dataResourceId = content.getString("dataResourceId");
            if (UtilValidate.isNotEmpty(dataResourceId))
                dataResource = content.getRelatedOne("DataResource", true);
            if (dataResource != null) {
                SimpleMapProcessor.runSimpleMapProcessor("component://content/minilang/ContentManagementMapProcessors.xml", "dataResourceOut", dataResource, contentAssocDataResourceView, new LinkedList<>(), locale);
            }
            contentAssocDataResourceList.add(contentAssocDataResourceView);
        }
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
        
        if (UtilValidate.isNotEmpty(orderBy)) {
            List<String> orderByList = StringUtil.split(orderBy, "|");
           contentAssocDataResourceList = EntityUtil.orderBy(contentAssocDataResourceList, orderByList);
        }
        Map<String, Object> results = new HashMap<>();
        results.put("entityList", contentAssocDataResourceList);
        if (UtilValidate.isNotEmpty(contentAssocDataResourceList)) {
            results.put("view", contentAssocDataResourceList.get(0));
        }
        return results;
    }
}
