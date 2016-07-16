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
package org.apache.ofbiz.webpos.search;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityFunction;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.DynamicViewEntity;
import org.apache.ofbiz.entity.model.ModelKeyMap;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

public class WebPosSearch {

    public static final String module = WebPosSearch.class.getName();
    
    public static Map<String, Object> findProducts(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String searchByProductIdValue = (String) context.get("searchByProductIdValue");
        String searchByProductName = (String) context.get("searchByProductName");
        String searchByProductDescription = (String) context.get("searchByProductDescription");
        String goodIdentificationTypeId = (String) context.get("goodIdentificationTypeId");
        Map<String, Object> result = ServiceUtil.returnSuccess();
        
        List<EntityCondition> andExprs = new LinkedList<EntityCondition>();
        EntityCondition mainCond = null;
        String entityName = "Product";
        
        // search by product name
        if (UtilValidate.isNotEmpty(searchByProductName)) {
            searchByProductName = searchByProductName.toUpperCase().trim();
            andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("productName"), EntityOperator.LIKE, EntityFunction.UPPER("%" + searchByProductName + "%")));
        }
        // search by description
        if (UtilValidate.isNotEmpty(searchByProductDescription)) {
            searchByProductDescription = searchByProductDescription.toUpperCase().trim();
            andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("description"), EntityOperator.LIKE, EntityFunction.UPPER("%" + searchByProductDescription + "%")));
        }
        // search by good identification
        if (UtilValidate.isNotEmpty(searchByProductIdValue)) {
            entityName = "GoodIdentificationAndProduct";
            searchByProductIdValue = searchByProductIdValue.toUpperCase().trim();
            andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("idValue"), EntityOperator.EQUALS, searchByProductIdValue));
            if (UtilValidate.isNotEmpty(goodIdentificationTypeId)) {
                andExprs.add(EntityCondition.makeCondition("goodIdentificationTypeId", EntityOperator.EQUALS, goodIdentificationTypeId));
            }
        }
        mainCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
        List<GenericValue> products = null;
        try {
            products = EntityQuery.use(delegator).from(entityName).where(mainCond).orderBy("productName", "description").queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        result.put("productsList", products);
        return result;
    }
    
    public static Map<String, Object> findParties(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String searchByPartyLastName = (String) context.get("searchByPartyLastName");
        String searchByPartyFirstName = (String) context.get("searchByPartyFirstName");
        String searchByPartyIdValue = (String) context.get("searchByPartyIdValue");
        String partyIdentificationTypeId = (String) context.get("partyIdentificationTypeId");
        String billingLocation = (String) context.get("billingLocation");
        String shippingLocation = (String) context.get("shippingLocation");
        Map<String, Object> result = ServiceUtil.returnSuccess();
        
        List<EntityCondition> andExprs = new LinkedList<EntityCondition>();
        List<EntityCondition> orExprs = new LinkedList<EntityCondition>();
        EntityCondition mainCond = null;
        List<String> orderBy = new LinkedList<String>();
        
        // default view settings
        DynamicViewEntity dynamicView = new DynamicViewEntity();
        dynamicView.addMemberEntity("PT", "Party");
        dynamicView.addAlias("PT", "partyId");
        dynamicView.addAlias("PT", "statusId");
        dynamicView.addAlias("PT", "partyTypeId");
        dynamicView.addMemberEntity("PI", "PartyIdentification");
        dynamicView.addAlias("PI", "partyIdentificationTypeId");
        dynamicView.addAlias("PI", "idValue");
        dynamicView.addViewLink("PT", "PI", Boolean.TRUE, ModelKeyMap.makeKeyMapList("partyId"));
        dynamicView.addMemberEntity("PER", "Person");
        dynamicView.addAlias("PER", "lastName");
        dynamicView.addAlias("PER", "firstName");
        dynamicView.addViewLink("PT", "PER", Boolean.TRUE, ModelKeyMap.makeKeyMapList("partyId"));
        dynamicView.addMemberEntity("PCP", "PartyContactMechPurpose");
        dynamicView.addAlias("PCP", "contactMechId");
        dynamicView.addAlias("PCP", "contactMechPurposeTypeId");
        dynamicView.addAlias("PCP", "fromDate");
        dynamicView.addAlias("PCP", "thruDate");
        dynamicView.addViewLink("PT", "PCP", Boolean.TRUE, ModelKeyMap.makeKeyMapList("partyId"));
        dynamicView.addMemberEntity("CM", "ContactMech");
        dynamicView.addAlias("CM", "contactMechId");
        dynamicView.addAlias("CM", "contactMechTypeId");
        dynamicView.addAlias("CM", "infoString");
        dynamicView.addViewLink("PCP", "CM", Boolean.TRUE, ModelKeyMap.makeKeyMapList("contactMechId"));
        dynamicView.addMemberEntity("PA", "PostalAddress");
        dynamicView.addAlias("PA", "address1");
        dynamicView.addAlias("PA", "city");
        dynamicView.addAlias("PA", "postalCode");
        dynamicView.addAlias("PA", "countryGeoId");
        dynamicView.addAlias("PA", "stateProvinceGeoId");
        dynamicView.addViewLink("CM", "PA", Boolean.TRUE, ModelKeyMap.makeKeyMapList("contactMechId"));
        
        if (UtilValidate.isNotEmpty(billingLocation) && "Y".equalsIgnoreCase(billingLocation)) {
            orExprs.add(EntityCondition.makeCondition("contactMechPurposeTypeId", EntityOperator.EQUALS, "BILLING_LOCATION"));
        }
        
        if (UtilValidate.isNotEmpty(shippingLocation) && "Y".equalsIgnoreCase(shippingLocation)) {
            orExprs.add(EntityCondition.makeCondition("contactMechPurposeTypeId", EntityOperator.EQUALS, "SHIPPING_LOCATION"));
        }
        
        if (orExprs.size() > 0) {
            andExprs.add(EntityCondition.makeCondition(orExprs, EntityOperator.OR));
        }
        andExprs.add(EntityCondition.makeCondition("partyTypeId", EntityOperator.EQUALS, "PERSON"));
        andExprs.add(EntityCondition.makeCondition("contactMechTypeId", EntityOperator.EQUALS, "POSTAL_ADDRESS"));
        
        mainCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
        
        orderBy.add("lastName");
        orderBy.add("firstName");
        
        // search by last name
        if (UtilValidate.isNotEmpty(searchByPartyLastName)) {
            searchByPartyLastName = searchByPartyLastName.toUpperCase().trim();
            andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("lastName"), EntityOperator.LIKE, EntityFunction.UPPER("%" + searchByPartyLastName + "%")));
        }
        // search by first name
        if (UtilValidate.isNotEmpty(searchByPartyFirstName)) {
            searchByPartyFirstName = searchByPartyFirstName.toUpperCase().trim();
            andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("firstName"), EntityOperator.LIKE, EntityFunction.UPPER("%" + searchByPartyFirstName + "%")));
        }
        // search by party identification
        if (UtilValidate.isNotEmpty(searchByPartyIdValue)) {
            searchByPartyIdValue = searchByPartyIdValue.toUpperCase().trim();
            andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("idValue"), EntityOperator.EQUALS, searchByPartyIdValue));
            if (UtilValidate.isNotEmpty(partyIdentificationTypeId)) {
                andExprs.add(EntityCondition.makeCondition("partyIdentificationTypeId", EntityOperator.EQUALS, partyIdentificationTypeId));
            }
        }
        mainCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
        List<GenericValue> parties = null;
        try {
            EntityListIterator pli = delegator.findListIteratorByCondition(dynamicView, mainCond, null, null, orderBy, null);
            parties = EntityUtil.filterByDate(pli.getCompleteList(), true);
            pli.close();
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        result.put("partiesList", parties);
        return result;
    }
}