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
package org.ofbiz.order.requirement;

import java.util.*;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.*;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.*;
import org.ofbiz.entity.condition.*;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

/**
 * Requirement Services
 */

public class RequirementServices {

    public static final String module = RequirementServices.class.getName();
    public static final String resource_error = "OrderErrorUiLabels";

    public static final Map getRequirementsForSupplier(DispatchContext ctx, Map context) {
        GenericDelegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        EntityCondition requirementConditions = (EntityCondition) context.get("requirementConditions");
        String partyId = (String) context.get("partyId");
        String unassignedRequirements = (String) context.get("unassignedRequirements");
        String currencyUomId = (String) context.get("currencyUomId");
        try {
            List orderBy = UtilMisc.toList("partyId", "requirementId");
            List conditions = UtilMisc.toList(
                    new EntityExpr("requirementTypeId", EntityOperator.EQUALS, "PRODUCT_REQUIREMENT"),
                    new EntityExpr("statusId", EntityOperator.EQUALS, "REQ_APPROVED"),
                    EntityUtil.getFilterByDateExpr()
                    );
            if (requirementConditions != null) conditions.add(requirementConditions);

            // we're either getting the requirements for a given supplier, unassigned requirements, or requirements for all suppliers
            if (UtilValidate.isNotEmpty(partyId)) {
                conditions.add( new EntityExpr("partyId", EntityOperator.EQUALS, partyId) );
                conditions.add( new EntityExpr("roleTypeId", EntityOperator.EQUALS, "SUPPLIER") );
            } else if (UtilValidate.isNotEmpty(unassignedRequirements)) {
                conditions.add( new EntityExpr("partyId", EntityOperator.EQUALS, null) );
            } else {
                conditions.add( new EntityExpr("roleTypeId", EntityOperator.EQUALS, "SUPPLIER") );
            }
            List requirementAndRoles = delegator.findByAnd("RequirementAndRole", conditions, orderBy);

            // join in fields with extra data about the suppliers and products
            List requirements = FastList.newInstance();
            for (Iterator iter = requirementAndRoles.iterator(); iter.hasNext(); ) {
                GenericValue requirement = (GenericValue) iter.next();
                Map union = FastMap.newInstance();

                // get an available supplier product
                conditions = UtilMisc.toList(
                        new EntityExpr("partyId", EntityOperator.EQUALS, requirement.get("partyId")), 
                        new EntityExpr("productId", EntityOperator.EQUALS, requirement.get("productId")),
                        EntityUtil.getFilterByDateExpr("availableFromDate", "availableThruDate")
                        );
                GenericValue supplierProduct = EntityUtil.getFirst( delegator.findByAnd("SupplierProduct", conditions) );
                if (supplierProduct != null) {
                    union.putAll(supplierProduct.getAllFields());
                }

                // for good identification, get the UPCA type (UPC code)
                GenericValue gid = delegator.findByPrimaryKey("GoodIdentification", UtilMisc.toMap("goodIdentificationTypeId", "UPCA", "productId", requirement.get("productId")));
                if (gid != null) union.put("idValue", gid.get("idValue"));

                // add all the requirement fields last, to overwrite any conflicting fields
                union.putAll(requirement.getAllFields());
                requirements.add(union);
            }

            Map results = ServiceUtil.returnSuccess();
            results.put("requirementsForSupplier", requirements);
            return results;
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error, "OrderEntityExceptionSeeLogs", locale));
        }
    }
}
