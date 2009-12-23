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
package org.ofbiz.manufacturing.jobshopmgt;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.common.CommonWorkers;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericPK;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.manufacturing.bom.BOMTree;
import org.ofbiz.manufacturing.bom.BOMNode;
import org.ofbiz.manufacturing.techdata.TechDataServices;
import org.ofbiz.product.config.ProductConfigWrapper;
import org.ofbiz.product.config.ProductConfigWrapper.ConfigOption;
import org.ofbiz.product.product.ProductWorker;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

/**
 * Services for Production Run maintenance
 *
 */
public class ProductionRunServices {

    public static final String module = ProductionRunServices.class.getName();
    public static final String resource = "ManufacturingUiLabels";

    private static BigDecimal ZERO = BigDecimal.ZERO;
    private static BigDecimal ONE = BigDecimal.ONE;
    private static int decimals = -1;
    private static int rounding = -1;
    static {
        decimals = UtilNumber.getBigDecimalScale("order.decimals");
        rounding = UtilNumber.getBigDecimalRoundingMode("order.rounding");
        // set zero to the proper scale
        ZERO = ZERO.setScale(decimals);
        ONE = ONE.setScale(decimals);
    }

    /**
     * Cancels a ProductionRun.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map cancelProductionRun(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String productionRunId = (String) context.get("productionRunId");

        ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
        if (!productionRun.exist()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunNotExists", locale));
        }
        String currentStatusId = productionRun.getGenericValue().getString("currentStatusId");

        // PRUN_CREATED, PRUN_DOC_PRINTED --> PRUN_CANCELLED
        if (currentStatusId.equals("PRUN_CREATED") || currentStatusId.equals("PRUN_DOC_PRINTED") || currentStatusId.equals("PRUN_SCHEDULED")) {
            try {
                // First of all, make sure that there aren't production runs that depend on this one.
                List mandatoryWorkEfforts = new ArrayList();
                ProductionRunHelper.getLinkedProductionRuns(delegator, dispatcher, productionRunId, mandatoryWorkEfforts);
                for (int i = 1; i < mandatoryWorkEfforts.size(); i++) {
                    GenericValue mandatoryWorkEffort = ((ProductionRun)mandatoryWorkEfforts.get(i)).getGenericValue();
                    if (!(mandatoryWorkEffort.getString("currentStatusId").equals("PRUN_CANCELLED"))) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusNotChangedMandatoryProductionRunFound", locale));
                    }
                }
                Map serviceContext = new HashMap();
                // change the production run (header) status to PRUN_CANCELLED
                serviceContext.put("workEffortId", productionRunId);
                serviceContext.put("currentStatusId", "PRUN_CANCELLED");
                serviceContext.put("userLogin", userLogin);
                Map resultService = null;
                resultService = dispatcher.runSync("updateWorkEffort", serviceContext);
                // Cancel the product promised
                List products = delegator.findByAnd("WorkEffortGoodStandard", UtilMisc.toMap("workEffortId", productionRunId, "workEffortGoodStdTypeId", "PRUN_PROD_DELIV", "statusId", "WEGS_CREATED"));
                if (!UtilValidate.isEmpty(products)) {
                    Iterator productsIt = products.iterator();
                    while (productsIt.hasNext()) {
                        GenericValue product = (GenericValue)productsIt.next();
                        product.set("statusId", "WEGS_CANCELLED");
                        product.store();
                    }
                }

                // change the tasks status to PRUN_CANCELLED
                List tasks = productionRun.getProductionRunRoutingTasks();
                GenericValue oneTask = null;
                String taskId = null;
                for (int i = 0; i < tasks.size(); i++) {
                    oneTask = (GenericValue)tasks.get(i);
                    taskId = oneTask.getString("workEffortId");
                    serviceContext.clear();
                    serviceContext.put("workEffortId", taskId);
                    serviceContext.put("currentStatusId", "PRUN_CANCELLED");
                    serviceContext.put("userLogin", userLogin);
                    resultService = dispatcher.runSync("updateWorkEffort", serviceContext);
                    // cancel all the components
                    List components = delegator.findByAnd("WorkEffortGoodStandard", UtilMisc.toMap("workEffortId", taskId, "workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED", "statusId", "WEGS_CREATED"));
                    if (!UtilValidate.isEmpty(components)) {
                        Iterator componentsIt = components.iterator();
                        while (componentsIt.hasNext()) {
                            GenericValue component = (GenericValue)componentsIt.next();
                            component.set("statusId", "WEGS_CANCELLED");
                            component.store();
                        }
                    }
                }
            } catch (Exception e) {
                Debug.logError(e, "Problem calling the updateWorkEffort service", module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusNotChanged", locale));
            }
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusChanged",UtilMisc.toMap("newStatusId", "PRUN_DOC_PRINTED"), locale));
            return result;
        }
        return ServiceUtil.returnError("Cannot cancel productionRun, not in a valid status");
    }

    /**
     * Creates a Production Run.
     *  <li> check if routing - product link exist
     *  <li> check if product have a Bill Of Material
     *  <li> check if routing have routingTask
     *  <li> create the workEffort for ProductionRun
     *  <li> create the WorkEffortGoodStandard for link between ProductionRun and the product it will produce
     *  <li> for each valid routingTask of the routing create a workeffort-task
     *  <li> for the first routingTask, create for all the valid productIdTo with no associateRoutingTask  a WorkEffortGoodStandard
     *  <li> for each valid routingTask of the routing and valid productIdTo associate with this RoutingTask create a WorkEffortGoodStandard
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters, productId, routingId, pRQuantity, startDate, workEffortName, description
     * @return Map with the result of the service, the output parameters.
     */
    public static Map createProductionRun(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        /* TODO: security management  and finishing cleaning (ex copy from PartyServices.java)
        if (!security.hasEntityPermission(secEntity, secOperation, userLogin)) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, "You do not have permission to perform this operation for this party");
            return partyId;
        }
         */
        // Mandatory input fields
        String productId = (String) context.get("productId");
        Timestamp  startDate =  (Timestamp) context.get("startDate");
        BigDecimal pRQuantity = (BigDecimal) context.get("pRQuantity");
        String facilityId = (String) context.get("facilityId");
        // Optional input fields
        String workEffortId = (String) context.get("routingId");
        String workEffortName = (String) context.get("workEffortName");
        String description = (String) context.get("description");

        GenericValue routing = null;
        GenericValue product = null;
        List workEffortProducts = null;
        List productBoms = null;
        List routingTaskAssocs = null;
        try {
            // Find the product
            product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));
            if (product == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductNotExist", locale));
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // -------------------
        // Routing and routing tasks
        // -------------------
        // Select the product's routing
        try {
            Map routingInMap = UtilMisc.toMap("productId", productId, "applicableDate", startDate, "userLogin", userLogin);
            if (workEffortId != null) {
                routingInMap.put("workEffortId", workEffortId);
            }
            Map routingOutMap = dispatcher.runSync("getProductRouting", routingInMap);
            routing = (GenericValue)routingOutMap.get("routing");
            routingTaskAssocs = (List)routingOutMap.get("tasks");
        } catch (GenericServiceException gse) {
            Debug.logWarning(gse.getMessage(), module);
        }
        if (routing == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductRoutingNotExist", locale));
        }
        if (UtilValidate.isEmpty(routingTaskAssocs)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingRoutingHasNoRoutingTask", locale));
        }

        // -------------------
        // Components
        // -------------------
        // The components are retrieved using the getManufacturingComponents service
        // (that performs a bom breakdown and if needed runs the configurator).
        List components = null;
        Map serviceContext = new HashMap();
        serviceContext.put("productId", productId); // the product that we want to manufacture
        serviceContext.put("quantity", pRQuantity); // the quantity that we want to manufacture
        serviceContext.put("userLogin", userLogin);
        Map resultService = null;
        try {
            resultService = dispatcher.runSync("getManufacturingComponents", serviceContext);
            components = (List)resultService.get("components"); // a list of objects representing the product's components
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the getManufacturingComponents service", module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // ProductionRun header creation,
        if (workEffortName == null) {
            String prdName = UtilValidate.isNotEmpty(product.getString("productName"))? product.getString("productName"): product.getString("productId");
            String wefName = UtilValidate.isNotEmpty(routing.getString("workEffortName"))? routing.getString("workEffortName"): routing.getString("workEffortId");
            workEffortName =  prdName + "-" + wefName;
        }

        serviceContext.clear();
        serviceContext.put("workEffortTypeId", "PROD_ORDER_HEADER");
        serviceContext.put("workEffortPurposeTypeId", "WEPT_PRODUCTION_RUN");
        serviceContext.put("currentStatusId", "PRUN_CREATED");
        serviceContext.put("workEffortName", workEffortName);
        serviceContext.put("description",description);
        serviceContext.put("facilityId", facilityId);
        serviceContext.put("estimatedStartDate",startDate);
        serviceContext.put("quantityToProduce", pRQuantity);
        serviceContext.put("userLogin", userLogin);
        try {
            resultService = dispatcher.runSync("createWorkEffort", serviceContext);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the createWorkEffort service", module);
            return ServiceUtil.returnError(e.getMessage());
        }
        String productionRunId = (String) resultService.get("workEffortId");
        if (Debug.infoOn()) {
            Debug.logInfo("ProductionRun created: " + productionRunId, module);
        }

        // ProductionRun,  product will be produce creation = WorkEffortGoodStandard for the productId
        serviceContext.clear();
        serviceContext.put("workEffortId", productionRunId);
        serviceContext.put("productId", productId);
        serviceContext.put("workEffortGoodStdTypeId", "PRUN_PROD_DELIV");
        serviceContext.put("statusId", "WEGS_CREATED");
        serviceContext.put("estimatedQuantity", pRQuantity);
        serviceContext.put("fromDate", startDate);
        serviceContext.put("userLogin", userLogin);
        try {
            resultService = dispatcher.runSync("createWorkEffortGoodStandard", serviceContext);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the createWorkEffortGoodStandard service", module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // Multi creation (like clone) ProductionRunTask and GoodAssoc
        Iterator  rt = routingTaskAssocs.iterator();
        boolean first = true;
        while (rt.hasNext()) {
            GenericValue routingTaskAssoc = (GenericValue) rt.next();

            if (EntityUtil.isValueActive(routingTaskAssoc, startDate)) {
                GenericValue routingTask = null;
                try {
                    routingTask = routingTaskAssoc.getRelatedOne("ToWorkEffort");
                } catch (GenericEntityException e) {
                    Debug.logError(e.getMessage(),  module);
                }
                // Calculate the estimatedCompletionDate
                long totalTime = ProductionRun.getEstimatedTaskTime(routingTask, pRQuantity, dispatcher);
                Timestamp endDate = TechDataServices.addForward(TechDataServices.getTechDataCalendar(routingTask),startDate, totalTime);

                serviceContext.clear();
                serviceContext.put("priority", routingTaskAssoc.get("sequenceNum"));
                serviceContext.put("workEffortPurposeTypeId", routingTask.get("workEffortPurposeTypeId"));
                serviceContext.put("workEffortName",routingTask.get("workEffortName"));
                serviceContext.put("description",routingTask.get("description"));
                serviceContext.put("fixedAssetId",routingTask.get("fixedAssetId"));
                serviceContext.put("workEffortTypeId", "PROD_ORDER_TASK");
                serviceContext.put("currentStatusId","PRUN_CREATED");
                serviceContext.put("workEffortParentId", productionRunId);
                serviceContext.put("facilityId", facilityId);
                serviceContext.put("estimatedStartDate",startDate);
                serviceContext.put("estimatedCompletionDate",endDate);
                serviceContext.put("estimatedSetupMillis", routingTask.get("estimatedSetupMillis"));
                serviceContext.put("estimatedMilliSeconds", routingTask.get("estimatedMilliSeconds"));
                serviceContext.put("quantityToProduce", pRQuantity);
                serviceContext.put("userLogin", userLogin);
                resultService = null;
                try {
                    resultService = dispatcher.runSync("createWorkEffort", serviceContext);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem calling the createWorkEffort service", module);
                }
                String productionRunTaskId = (String) resultService.get("workEffortId");
                if (Debug.infoOn()) Debug.logInfo("ProductionRunTaskId created: " + productionRunTaskId, module);
                // The newly created production run task is associated to the routing task
                // to keep track of the template used to generate it.
                serviceContext.clear();
                serviceContext.put("userLogin", userLogin);
                serviceContext.put("workEffortIdFrom", routingTask.getString("workEffortId"));
                serviceContext.put("workEffortIdTo", productionRunTaskId);
                serviceContext.put("workEffortAssocTypeId", "WORK_EFF_TEMPLATE");
                try {
                    resultService = dispatcher.runSync("createWorkEffortAssoc", serviceContext);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem calling the createWorkEffortAssoc service", module);
                }
                // copy date valid WorkEffortPartyAssignments from the routing task to the run task
                List workEffortPartyAssignments = null;
                try {
                    workEffortPartyAssignments = EntityUtil.filterByDate(delegator.findByAnd("WorkEffortPartyAssignment",
                            UtilMisc.toMap("workEffortId", routingTaskAssoc.getString("workEffortIdTo"))));
                } catch (GenericEntityException e) {
                    Debug.logError(e.getMessage(),  module);
                }
                if (workEffortPartyAssignments != null) {
                    Iterator i = workEffortPartyAssignments.iterator();
                    while (i.hasNext()) {
                        GenericValue workEffortPartyAssignment = (GenericValue) i.next();
                        Map partyToWorkEffort = UtilMisc.toMap(
                                "workEffortId",  productionRunTaskId,
                                "partyId",  workEffortPartyAssignment.getString("partyId"),
                                "roleTypeId",  workEffortPartyAssignment.getString("roleTypeId"),
                                "fromDate",  workEffortPartyAssignment.getTimestamp("fromDate"),
                                "statusId",  workEffortPartyAssignment.getString("statusId"),
                                "userLogin", userLogin
                       );
                        try {
                            resultService = dispatcher.runSync("assignPartyToWorkEffort", partyToWorkEffort);
                        } catch (GenericServiceException e) {
                            Debug.logError(e, "Problem calling the assignPartyToWorkEffort service", module);
                        }
                        if (Debug.infoOn()) Debug.logInfo("ProductionRunPartyassigment for party: " + workEffortPartyAssignment.get("partyId") + " created", module);
                    }
                }

                // Now we iterate thru the components returned by the getManufacturingComponents service
                // TODO: if in the BOM a routingWorkEffortId is specified, but the task is not in the routing
                //       the component is not added to the production run.
                Iterator  pb = components.iterator();
                while (pb.hasNext()) {
                    // The components variable contains a list of BOMNodes:
                    // each node represents a product (component).
                    BOMNode node = (BOMNode) pb.next();
                    GenericValue productBom = node.getProductAssoc();
                    if ((productBom.getString("routingWorkEffortId") == null && first) || (productBom.getString("routingWorkEffortId") != null && productBom.getString("routingWorkEffortId").equals(routingTask.getString("workEffortId")))) {
                        serviceContext.clear();
                        serviceContext.put("workEffortId", productionRunTaskId);
                        // Here we get the ProductAssoc record from the BOMNode
                        // object to be sure to use the
                        // right component (possibly configured).
                        serviceContext.put("productId", node.getProduct().get("productId"));
                        serviceContext.put("workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED");
                        serviceContext.put("statusId", "WEGS_CREATED");
                        serviceContext.put("fromDate", productBom.get("fromDate"));
                        // Here we use the getQuantity method to get the quantity already
                        // computed by the getManufacturingComponents service
                        serviceContext.put("estimatedQuantity", node.getQuantity());
                        serviceContext.put("userLogin", userLogin);
                        resultService = null;
                        try {
                            resultService = dispatcher.runSync("createWorkEffortGoodStandard", serviceContext);
                        } catch (GenericServiceException e) {
                            Debug.logError(e, "Problem calling the createWorkEffortGoodStandard service", module);
                        }
                        if (Debug.infoOn()) Debug.logInfo("ProductLink created for productId: " + productBom.getString("productIdTo"), module);
                    }
                }
                first = false;
                startDate = endDate;
            }
        }

        // update the estimatedCompletionDate field for the productionRun
        serviceContext.clear();
        serviceContext.put("workEffortId",productionRunId);
        serviceContext.put("estimatedCompletionDate",startDate);
        serviceContext.put("userLogin", userLogin);
        resultService = null;
        try {
            resultService = dispatcher.runSync("updateWorkEffort", serviceContext);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the updateWorkEffort service", module);
        }
        result.put("productionRunId", productionRunId);
        result.put("estimatedCompletionDate", startDate);
        result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(resource, "ManufacturingProductionRunCreated",UtilMisc.toMap("productionRunId", productionRunId), locale));
        return result;
    }
    /**
     * Update a Production Run.
     *  <li> update field and after recalculate the entire ProductionRun data (routingTask and productComponent)
     *  <li> create the WorkEffortGoodStandard for link between ProductionRun and the product it will produce
     *  <li> for each valid routingTask of the routing create a workeffort-task
     *  <li> for the first routingTask, create for all the valid productIdTo with no associateRoutingTask  a WorkEffortGoodStandard
     *  <li> for each valid routingTask of the routing and valid productIdTo associate with this RoutingTask create a WorkEffortGoodStandard
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters, productId, routingId, quantity, estimatedStartDate, workEffortName, description
     * @return Map with the result of the service, the output parameters.
     */
    public static Map updateProductionRun(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String productionRunId = (String) context.get("productionRunId");

        if (!UtilValidate.isEmpty(productionRunId)) {
            ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
            if (productionRun.exist()) {

                if (!"PRUN_CREATED".equals(productionRun.getGenericValue().getString("currentStatusId")) &&
                      !"PRUN_SCHEDULED".equals(productionRun.getGenericValue().getString("currentStatusId"))) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunPrinted", locale));
                }

                BigDecimal quantity = (BigDecimal) context.get("quantity");
                if (quantity != null &&  quantity.compareTo(productionRun.getQuantity()) != 0) {
                    productionRun.setQuantity(quantity);
                }

                Timestamp  estimatedStartDate =  (Timestamp) context.get("estimatedStartDate");
                if (estimatedStartDate != null && ! estimatedStartDate.equals(productionRun.getEstimatedStartDate())) {
                    productionRun.setEstimatedStartDate(estimatedStartDate);
                }

                String  workEffortName = (String) context.get("workEffortName");
                if (workEffortName != null) {
                    productionRun.setProductionRunName(workEffortName);
                }

                String  description = (String) context.get("description");
                if (description != null) {
                    productionRun.setDescription(description);
                }

                String  facilityId = (String) context.get("facilityId");
                if (facilityId != null) {
                    productionRun.getGenericValue().set("facilityId", facilityId);
                }

                boolean updateEstimatedOrderDates = productionRun.isUpdateCompletionDate();
                if (productionRun.store()) {
                    if (updateEstimatedOrderDates && "PRUN_SCHEDULED".equals(productionRun.getGenericValue().getString("currentStatusId"))) {
                        try {
                            Map resultService = dispatcher.runSync("setEstimatedDeliveryDates",
                                                                   UtilMisc.toMap("userLogin", userLogin));
                        } catch (GenericServiceException e) {
                            Debug.logError(e, "Problem calling the setEstimatedDeliveryDates service", module);
                            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunNotUpdated", locale));
                        }
                    }
                    return ServiceUtil.returnSuccess();
                } else {
                    Debug.logError("productionRun.store() fail for productionRunId ="+productionRunId,module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunNotUpdated", locale));
                }
            }
            Debug.logError("no productionRun for productionRunId ="+productionRunId,module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunNotUpdated", locale));
        }
        Debug.logError("service updateProductionRun call with productionRunId empty",module);
        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunNotUpdated", locale));
    }

    public static Map changeProductionRunStatus(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String productionRunId = (String) context.get("productionRunId");
        String statusId = (String) context.get("statusId");

        ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
        if (!productionRun.exist()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunNotExists", locale));
        }
        String currentStatusId = productionRun.getGenericValue().getString("currentStatusId");

        if (currentStatusId.equals(statusId)) {
            result.put("newStatusId", currentStatusId);
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusChanged",UtilMisc.toMap("newStatusId", currentStatusId), locale));
            return result;
        }

        // PRUN_CREATED --> PRUN_SCHEDULED
        if (currentStatusId.equals("PRUN_CREATED") && (statusId != null && statusId.equals("PRUN_SCHEDULED"))) {
            // change the production run status to PRUN_SCHEDULED
            Map serviceContext = new HashMap();
            serviceContext.clear();
            serviceContext.put("workEffortId", productionRunId);
            serviceContext.put("currentStatusId", statusId);
            serviceContext.put("userLogin", userLogin);
            Map resultService = null;
            try {
                resultService = dispatcher.runSync("updateWorkEffort", serviceContext);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem calling the updateWorkEffort service", module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusNotChanged", locale));
            }
            // change the production run tasks status to PRUN_CLOSED
            Iterator tasks = productionRun.getProductionRunRoutingTasks().iterator();
            while (tasks.hasNext()) {
                GenericValue task = (GenericValue)tasks.next();
                serviceContext.clear();
                serviceContext.put("workEffortId", task.getString("workEffortId"));
                serviceContext.put("currentStatusId", statusId);
                serviceContext.put("userLogin", userLogin);
                resultService = null;
                try {
                    resultService = dispatcher.runSync("updateWorkEffort", serviceContext);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem calling the updateWorkEffort service", module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusNotChanged", locale));
                }
            }
            result.put("newStatusId", statusId);
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusChanged",UtilMisc.toMap("newStatusId", "PRUN_CLOSED"), locale));
            return result;
        }

        // PRUN_CREATED or PRON_SCHEDULED --> PRUN_DOC_PRINTED
        if ((currentStatusId.equals("PRUN_CREATED") || currentStatusId.equals("PRUN_SCHEDULED")) && (statusId == null || statusId.equals("PRUN_DOC_PRINTED"))) {
            // change only the production run (header) status to PRUN_DOC_PRINTED
            Map serviceContext = new HashMap();
            serviceContext.clear();
            serviceContext.put("workEffortId", productionRunId);
            serviceContext.put("currentStatusId", "PRUN_DOC_PRINTED");
            serviceContext.put("userLogin", userLogin);
            Map resultService = null;
            try {
                resultService = dispatcher.runSync("updateWorkEffort", serviceContext);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem calling the updateWorkEffort service", module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusNotChanged", locale));
            }
            result.put("newStatusId", "PRUN_DOC_PRINTED");
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusChanged",UtilMisc.toMap("newStatusId", "PRUN_DOC_PRINTED"), locale));
            return result;
        }

        // PRUN_DOC_PRINTED --> PRUN_RUNNING
        // this should be called only when the first task is started
        if (currentStatusId.equals("PRUN_DOC_PRINTED") && (statusId == null || statusId.equals("PRUN_RUNNING"))) {
            // change only the production run (header) status to PRUN_RUNNING
            // First check if there are production runs with precedence not still completed
            try {
                List mandatoryWorkEfforts = EntityUtil.filterByDate(delegator.findByAnd("WorkEffortAssoc", UtilMisc.toMap("workEffortIdTo", productionRunId, "workEffortAssocTypeId", "WORK_EFF_PRECEDENCY")));
                for (int i = 0; i < mandatoryWorkEfforts.size(); i++) {
                    GenericValue mandatoryWorkEffortAssoc = (GenericValue)mandatoryWorkEfforts.get(i);
                    GenericValue mandatoryWorkEffort = mandatoryWorkEffortAssoc.getRelatedOne("FromWorkEffort");
                    if (!(mandatoryWorkEffort.getString("currentStatusId").equals("PRUN_COMPLETED") ||
                         mandatoryWorkEffort.getString("currentStatusId").equals("PRUN_CLOSED"))) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusNotChangedMandatoryProductionRunNotCompleted", locale));
                    }
                }
            } catch (GenericEntityException gee) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusNotChanged", locale));
            }

            Map serviceContext = new HashMap();
            serviceContext.clear();
            serviceContext.put("workEffortId", productionRunId);
            serviceContext.put("currentStatusId", "PRUN_RUNNING");
            serviceContext.put("actualStartDate", UtilDateTime.nowTimestamp());
            serviceContext.put("userLogin", userLogin);
            Map resultService = null;
            try {
                resultService = dispatcher.runSync("updateWorkEffort", serviceContext);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem calling the updateWorkEffort service", module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusNotChanged", locale));
            }
            result.put("newStatusId", "PRUN_RUNNING");
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusChanged",UtilMisc.toMap("newStatusId", "PRUN_DOC_PRINTED"), locale));
            return result;
        }

        // PRUN_RUNNING --> PRUN_COMPLETED
        // this should be called only when the last task is completed
        if (currentStatusId.equals("PRUN_RUNNING") && (statusId == null || statusId.equals("PRUN_COMPLETED"))) {
            // change only the production run (header) status to PRUN_COMPLETED
            Map serviceContext = new HashMap();
            serviceContext.clear();
            serviceContext.put("workEffortId", productionRunId);
            serviceContext.put("currentStatusId", "PRUN_COMPLETED");
            serviceContext.put("actualCompletionDate", UtilDateTime.nowTimestamp());
            serviceContext.put("userLogin", userLogin);
            Map resultService = null;
            try {
                resultService = dispatcher.runSync("updateWorkEffort", serviceContext);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem calling the updateWorkEffort service", module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusNotChanged", locale));
            }
            result.put("newStatusId", "PRUN_COMPLETED");
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusChanged",UtilMisc.toMap("newStatusId", "PRUN_DOC_PRINTED"), locale));
            return result;
        }

        // PRUN_COMPLETED --> PRUN_CLOSED
        if (currentStatusId.equals("PRUN_COMPLETED") && (statusId == null || statusId.equals("PRUN_CLOSED"))) {
            // change the production run status to PRUN_CLOSED
            Map serviceContext = new HashMap();
            serviceContext.clear();
            serviceContext.put("workEffortId", productionRunId);
            serviceContext.put("currentStatusId", "PRUN_CLOSED");
            serviceContext.put("userLogin", userLogin);
            Map resultService = null;
            try {
                resultService = dispatcher.runSync("updateWorkEffort", serviceContext);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem calling the updateWorkEffort service", module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusNotChanged", locale));
            }
            // change the production run tasks status to PRUN_CLOSED
            Iterator tasks = productionRun.getProductionRunRoutingTasks().iterator();
            while (tasks.hasNext()) {
                GenericValue task = (GenericValue)tasks.next();
                serviceContext.clear();
                serviceContext.put("workEffortId", task.getString("workEffortId"));
                serviceContext.put("currentStatusId", "PRUN_CLOSED");
                serviceContext.put("userLogin", userLogin);
                resultService = null;
                try {
                    resultService = dispatcher.runSync("updateWorkEffort", serviceContext);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem calling the updateWorkEffort service", module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusNotChanged", locale));
                }
            }
            result.put("newStatusId", "PRUN_CLOSED");
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusChanged",UtilMisc.toMap("newStatusId", "PRUN_CLOSED"), locale));
            return result;
        }
        result.put("newStatusId", currentStatusId);
        result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusChanged",UtilMisc.toMap("newStatusId", currentStatusId), locale));
        return result;
    }

    public static Map changeProductionRunTaskStatus(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String productionRunId = (String) context.get("productionRunId");
        String taskId = (String) context.get("workEffortId");
        String statusId = (String) context.get("statusId");
        Boolean issueAllComponents = (Boolean) context.get("issueAllComponents");
        if (issueAllComponents == null) {
            issueAllComponents = Boolean.FALSE;
        }

        ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
        if (!productionRun.exist()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunNotExists", locale));
        }
        List tasks = productionRun.getProductionRunRoutingTasks();
        GenericValue theTask = null;
        GenericValue oneTask = null;
        boolean allTaskCompleted = true;
        boolean allPrecTaskCompletedOrRunning = true;
        for (int i = 0; i < tasks.size(); i++) {
            oneTask = (GenericValue)tasks.get(i);
            if (oneTask.getString("workEffortId").equals(taskId)) {
                theTask = oneTask;
            } else {
                if (theTask == null && allPrecTaskCompletedOrRunning && (!oneTask.getString("currentStatusId").equals("PRUN_COMPLETED") && !oneTask.getString("currentStatusId").equals("PRUN_RUNNING"))) {
                    allPrecTaskCompletedOrRunning = false;
                }
                if (allTaskCompleted && !oneTask.getString("currentStatusId").equals("PRUN_COMPLETED")) {
                    allTaskCompleted = false;
                }
            }
        }
        if (theTask == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunTaskNotExists", locale));
        }

        String currentStatusId = theTask.getString("currentStatusId");
        String oldStatusId = theTask.getString("currentStatusId"); // pass back old status for secas to check

        if (statusId != null && currentStatusId.equals(statusId)) {
            result.put("oldStatusId", oldStatusId);
            result.put("newStatusId", currentStatusId);
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(resource, "ManufacturingProductionRunTaskStatusChanged",UtilMisc.toMap("newStatusId", currentStatusId), locale));
            return result;
        }

        // PRUN_CREATED or PRUN_SCHEDULED --> PRUN_RUNNING
        // this should be called only when the first task is started
        if ((currentStatusId.equals("PRUN_CREATED") || currentStatusId.equals("PRUN_SCHEDULED")) && (statusId == null || statusId.equals("PRUN_RUNNING"))) {
            // change the production run task status to PRUN_RUNNING
            // if necessary change the production run (header) status to PRUN_RUNNING
            if (!allPrecTaskCompletedOrRunning) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunTaskCannotStartPrevTasksNotCompleted", locale));
            }
            if (productionRun.getGenericValue().getString("currentStatusId").equals("PRUN_CREATED")) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunTaskCannotStartDocsNotPrinted", locale));
            }
            Map serviceContext = new HashMap();
            serviceContext.clear();
            serviceContext.put("workEffortId", taskId);
            serviceContext.put("currentStatusId", "PRUN_RUNNING");
            serviceContext.put("actualStartDate", UtilDateTime.nowTimestamp());
            serviceContext.put("userLogin", userLogin);
            Map resultService = null;
            try {
                resultService = dispatcher.runSync("updateWorkEffort", serviceContext);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem calling the updateWorkEffort service", module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusNotChanged", locale));
            }
            if (!productionRun.getGenericValue().getString("currentStatusId").equals("PRUN_RUNNING")) {
                serviceContext.clear();
                serviceContext.put("productionRunId", productionRunId);
                serviceContext.put("statusId", "PRUN_RUNNING");
                serviceContext.put("userLogin", userLogin);
                resultService = null;
                try {
                    resultService = dispatcher.runSync("changeProductionRunStatus", serviceContext);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem calling the changeProductionRunStatus service", module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusNotChanged", locale));
                }
            }
            result.put("oldStatusId", oldStatusId);
            result.put("newStatusId", "PRUN_RUNNING");
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusChanged",UtilMisc.toMap("newStatusId", "PRUN_DOC_PRINTED"), locale));
            return result;
        }

        // PRUN_RUNNING --> PRUN_COMPLETED
        // this should be called only when the last task is completed
        if (currentStatusId.equals("PRUN_RUNNING") && (statusId == null || statusId.equals("PRUN_COMPLETED"))) {
            Map serviceContext = new HashMap();
            Map resultService = null;
            if (issueAllComponents.booleanValue()) {
                // Issue all the components, if this task needs components and they still need to be issued
                try {
                    List inventoryAssigned = delegator.findByAnd("WorkEffortInventoryAssign", UtilMisc.toMap("workEffortId", taskId));
                    if (UtilValidate.isEmpty(inventoryAssigned)) {
                        serviceContext.clear();
                        serviceContext.put("workEffortId", taskId);
                        serviceContext.put("userLogin", userLogin);
                        resultService = dispatcher.runSync("issueProductionRunTask", serviceContext);
                    }
                } catch (Exception e) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusNotChanged", locale));
                }
            }
            // change only the production run task status to PRUN_COMPLETED
            serviceContext.clear();
            serviceContext.put("workEffortId", taskId);
            serviceContext.put("currentStatusId", "PRUN_COMPLETED");
            serviceContext.put("actualCompletionDate", UtilDateTime.nowTimestamp());
            BigDecimal quantityToProduce = theTask.getBigDecimal("quantityToProduce");
            if (quantityToProduce == null) {
                quantityToProduce = BigDecimal.ZERO;
            }
            BigDecimal quantityProduced = theTask.getBigDecimal("quantityProduced");
            if (quantityProduced == null) {
                quantityProduced = BigDecimal.ZERO;
            }
            BigDecimal quantityRejected = theTask.getBigDecimal("quantityRejected");
            if (quantityRejected == null) {
                quantityRejected = BigDecimal.ZERO;
            }
            BigDecimal totalQuantity = quantityProduced.add(quantityRejected);
            BigDecimal diffQuantity = quantityToProduce.subtract(totalQuantity);
            if (diffQuantity.compareTo(BigDecimal.ZERO) > 0) {
                quantityProduced = quantityProduced.add(diffQuantity);
            }
            serviceContext.put("quantityProduced", quantityProduced);
            if (theTask.get("actualSetupMillis") == null) {
                serviceContext.put("actualSetupMillis", theTask.get("estimatedSetupMillis"));
            }
            if (theTask.get("actualMilliSeconds") == null) {
                Double autoMillis = null;
                if (theTask.get("estimatedMilliSeconds") != null) {
                    autoMillis = Double.valueOf(quantityProduced.doubleValue() * theTask.getDouble("estimatedMilliSeconds"));
                }
                serviceContext.put("actualMilliSeconds", autoMillis);
            }
            serviceContext.put("userLogin", userLogin);
            try {
                resultService = dispatcher.runSync("updateWorkEffort", serviceContext);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem calling the updateWorkEffort service", module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusNotChanged", locale));
            }
            // Calculate and store the production run task actual costs
            serviceContext.clear();
            serviceContext.put("productionRunTaskId", taskId);
            serviceContext.put("userLogin", userLogin);
            resultService = null;
            try {
                resultService = dispatcher.runSync("createProductionRunTaskCosts", serviceContext);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem calling the createProductionRunTaskCosts service", module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusNotChanged", locale));
            }
            // If this is the last task, then the production run is marked as 'completed'
            if (allTaskCompleted) {
                serviceContext.clear();
                serviceContext.put("productionRunId", productionRunId);
                serviceContext.put("statusId", "PRUN_COMPLETED");
                serviceContext.put("userLogin", userLogin);
                resultService = null;
                try {
                    resultService = dispatcher.runSync("changeProductionRunStatus", serviceContext);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem calling the updateWorkEffort service", module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusNotChanged", locale));
                }
                // and compute the overhead costs associated to the finished product
                try {
                    // get the currency
                    GenericValue facility = productionRun.getGenericValue().getRelatedOne("Facility");
                    Map outputMap = dispatcher.runSync("getPartyAccountingPreferences", UtilMisc.<String, Object>toMap("userLogin", userLogin, "organizationPartyId", facility.getString("ownerPartyId")));
                    Map partyAccountingPreference = (Map)outputMap.get("partyAccountingPreference");
                    if (partyAccountingPreference == null) {
                        return ServiceUtil.returnError("Unable to find a currency for production run costs");
                    }
                    outputMap = dispatcher.runSync("getProductionRunCost", UtilMisc.<String, Object>toMap("userLogin", userLogin, "workEffortId", productionRunId));

                    BigDecimal totalCost = (BigDecimal)outputMap.get("totalCost");
                    if (totalCost == null) {
                        totalCost = ZERO;
                    }

                    List productCostComponentCalcs = delegator.findByAnd("ProductCostComponentCalc", UtilMisc.toMap("productId", productionRun.getProductProduced().getString("productId")), UtilMisc.toList("sequenceNum"));
                    for (int i = 0; i < productCostComponentCalcs.size(); i++) {
                        GenericValue productCostComponentCalc = (GenericValue)productCostComponentCalcs.get(i);
                        GenericValue costComponentCalc = productCostComponentCalc.getRelatedOne("CostComponentCalc");
                        GenericValue customMethod = costComponentCalc.getRelatedOne("CustomMethod");
                        if (customMethod == null) {
                            // TODO: not supported for CostComponentCalc entries directly associated to a product
                            Debug.logWarning("Unable to create cost component for cost component calc with id [" + costComponentCalc.getString("costComponentCalcId") + "] because customMethod is not set", module);
                        } else {
                            Map costMethodResult = dispatcher.runSync(customMethod.getString("customMethodName"), UtilMisc.toMap("productCostComponentCalc", productCostComponentCalc,
                                                                                                                                 "costComponentCalc", costComponentCalc,
                                                                                                                                 "costComponentTypePrefix", "ACTUAL", 
                                                                                                                                 "baseCost", totalCost,
                                                                                                                                 "currencyUomId", (String)partyAccountingPreference.get("baseCurrencyUomId"),
                                                                                                                                 "userLogin", userLogin));
                            BigDecimal productCostAdjustment = (BigDecimal)costMethodResult.get("productCostAdjustment");
                            totalCost = totalCost.add(productCostAdjustment);
                            Map inMap = UtilMisc.toMap("userLogin", userLogin, "workEffortId", productionRunId);
                            inMap.put("costComponentTypeId", "ACTUAL_" + productCostComponentCalc.getString("costComponentTypeId"));
                            inMap.put("costUomId", (String)partyAccountingPreference.get("baseCurrencyUomId"));
                            inMap.put("cost", productCostAdjustment);
                            dispatcher.runSync("createCostComponent", inMap);
                        }
                    }
                } catch(Exception e) {
                    return ServiceUtil.returnError("Unable to compute overhead costs for production run: " + e.getMessage());
                }
            }

            result.put("oldStatusId", oldStatusId);
            result.put("newStatusId", "PRUN_COMPLETED");
            result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusChanged",UtilMisc.toMap("newStatusId", "PRUN_DOC_PRINTED"), locale));
            return result;
        }
        result.put("oldStatusId", oldStatusId);
        result.put("newStatusId", currentStatusId);
        result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(resource, "ManufacturingProductionRunTaskStatusChanged",UtilMisc.toMap("newStatusId", currentStatusId), locale));
        return result;
    }

    public static Map getWorkEffortCosts(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        Delegator delegator = ctx.getDelegator();
        String workEffortId = (String)context.get("workEffortId");
        try {
            GenericValue workEffort = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
            if (workEffort == null) {
                return ServiceUtil.returnError("Cannot find work effort [" + workEffortId + "]");
            }
            // Get all the valid CostComponents entries
            List costComponents = EntityUtil.filterByDate(delegator.findByAnd("CostComponent",
                                                          UtilMisc.toMap("workEffortId", workEffortId)));
            result.put("costComponents", costComponents);
            Iterator costComponentsIt = costComponents.iterator();
            // TODO: before doing these totals we should convert the cost components' costs to the
            //       base currency uom of the owner of the facility in which the task is running
            BigDecimal totalCost = ZERO;
            BigDecimal totalCostNoMaterials = ZERO;
            while (costComponentsIt.hasNext()) {
                GenericValue costComponent = (GenericValue)costComponentsIt.next();
                BigDecimal cost = costComponent.getBigDecimal("cost");
                totalCost = totalCost.add(cost);
                if (!"ACTUAL_MAT_COST".equals(costComponent.getString("costComponentTypeId"))) {
                    totalCostNoMaterials = totalCostNoMaterials.add(cost);
                }
            }
            result.put("totalCost", totalCost);
            result.put("totalCostNoMaterials", totalCostNoMaterials);
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError("Cannot retrieve costs for work effort [" + workEffortId + "]: " + gee.getMessage());
        }
        return result;
    }

    public static Map getProductionRunCost(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String workEffortId = (String)context.get("workEffortId");
        try {
            List tasks = delegator.findByAnd("WorkEffort", UtilMisc.toMap("workEffortParentId", workEffortId), UtilMisc.toList("workEffortId"));
            Iterator tasksIt = tasks.iterator();
            BigDecimal totalCost = ZERO;
            while (tasksIt.hasNext()) {
                GenericValue task = (GenericValue)tasksIt.next();
                Map outputMap = dispatcher.runSync("getWorkEffortCosts", UtilMisc.<String, Object>toMap("userLogin", userLogin, "workEffortId", task.getString("workEffortId")));
                BigDecimal taskCost = (BigDecimal)outputMap.get("totalCost");
                totalCost = totalCost.add(taskCost);
            }
            result.put("totalCost", totalCost);
        } catch (Exception exc) {
            return ServiceUtil.returnError("Cannot retrieve costs for production run [" + workEffortId + "]: " + exc.getMessage());
        }
        return result;
    }

    public static Map createProductionRunTaskCosts(DispatchContext ctx, Map context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // this is the id of the actual (real) production run task
        String productionRunTaskId = (String)context.get("productionRunTaskId");
        try {
            GenericValue workEffort = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", productionRunTaskId));
            if (UtilValidate.isEmpty(workEffort)) {
                return ServiceUtil.returnError("Cannot find the production run task with id [" + productionRunTaskId + "]");
            }
            double actualTotalMilliSeconds = 0.0;
            Double actualSetupMillis = workEffort.getDouble("actualSetupMillis");
            Double actualMilliSeconds = workEffort.getDouble("actualMilliSeconds");
            if (actualSetupMillis == null) {
                actualSetupMillis = new Double(0.0);
            }
            if (actualMilliSeconds == null) {
                actualMilliSeconds = new Double(0.0);
            }
            actualTotalMilliSeconds += actualSetupMillis.doubleValue();
            actualTotalMilliSeconds += actualMilliSeconds.doubleValue();
            // Get the template (aka routing task) of the work effort
            GenericValue routingTaskAssoc = EntityUtil.getFirst(EntityUtil.filterByDate(delegator.findByAnd("WorkEffortAssoc",
                                                            UtilMisc.toMap("workEffortIdTo", productionRunTaskId,
                                                                           "workEffortAssocTypeId", "WORK_EFF_TEMPLATE"))));
            GenericValue routingTask = null;
            if (UtilValidate.isNotEmpty(routingTaskAssoc)) {
                routingTask = routingTaskAssoc.getRelatedOne("FromWorkEffort");
            }
            List workEffortCostCalcs = null;
            if (UtilValidate.isEmpty(routingTask)) {
                // there is no template, try to get the cost entries for the actual production run task, if any
                workEffortCostCalcs = delegator.findByAnd("WorkEffortCostCalc", UtilMisc.toMap("workEffortId", productionRunTaskId));
            } else {
                workEffortCostCalcs = delegator.findByAnd("WorkEffortCostCalc", UtilMisc.toMap("workEffortId", routingTask.getString("workEffortId")));
            }
            // Get all the valid CostComponentCalc entries
            workEffortCostCalcs = EntityUtil.filterByDate(workEffortCostCalcs);
            Iterator workEffortCostCalcsIt = workEffortCostCalcs.iterator();
            while (workEffortCostCalcsIt.hasNext()) {
                GenericValue workEffortCostCalc = (GenericValue)workEffortCostCalcsIt.next();
                GenericValue costComponentCalc = workEffortCostCalc.getRelatedOne("CostComponentCalc");
                GenericValue customMethod = costComponentCalc.getRelatedOne("CustomMethod");
                if (UtilValidate.isEmpty(customMethod) || UtilValidate.isEmpty(customMethod.getString("customMethodName"))) {
                    // compute the total time
                    double totalTime = actualTotalMilliSeconds;
                    if (costComponentCalc.get("perMilliSecond") != null) {
                        long perMilliSecond = costComponentCalc.getLong("perMilliSecond").longValue();
                        if (perMilliSecond != 0) {
                            totalTime = totalTime / perMilliSecond;
                        }
                    }
                    // compute the cost
                    BigDecimal fixedCost = costComponentCalc.getBigDecimal("fixedCost");
                    BigDecimal variableCost = costComponentCalc.getBigDecimal("variableCost");
                    if (fixedCost == null) {
                        fixedCost = BigDecimal.ZERO;
                    }
                    if (variableCost == null) {
                        variableCost = BigDecimal.ZERO;
                    }
                    BigDecimal totalCost = fixedCost.add(variableCost.multiply(BigDecimal.valueOf(totalTime))).setScale(decimals, rounding);
                    // store the cost
                    Map inMap = UtilMisc.toMap("userLogin", userLogin, "workEffortId", productionRunTaskId);
                    inMap.put("costComponentTypeId", "ACTUAL_" + workEffortCostCalc.getString("costComponentTypeId"));
                    inMap.put("costComponentCalcId", costComponentCalc.getString("costComponentCalcId"));
                    inMap.put("costUomId", costComponentCalc.getString("currencyUomId"));
                    inMap.put("cost", totalCost);
                    dispatcher.runSync("createCostComponent", inMap);
                } else {
                    // use the custom method (aka formula) to compute the costs
                    Map inMap = UtilMisc.toMap("userLogin", userLogin, "workEffort", workEffort);
                    inMap.put("workEffortCostCalc", workEffortCostCalc);
                    inMap.put("costComponentCalc", costComponentCalc);
                    dispatcher.runSync(customMethod.getString("customMethodName"), inMap);
                }
            }
            // Now get the cost information associated to the fixed asset and compute the costs
            GenericValue fixedAsset = workEffort.getRelatedOne("FixedAsset");
            if (UtilValidate.isEmpty(fixedAsset) && UtilValidate.isNotEmpty(routingTask)) {
                fixedAsset = routingTask.getRelatedOne("FixedAsset");
            }
            if (UtilValidate.isNotEmpty(fixedAsset)) {
                List setupCosts = fixedAsset.getRelatedByAnd("FixedAssetStdCost", UtilMisc.toMap("fixedAssetStdCostTypeId", "SETUP_COST"));
                GenericValue setupCost = EntityUtil.getFirst(EntityUtil.filterByDate(setupCosts));
                List usageCosts = fixedAsset.getRelatedByAnd("FixedAssetStdCost", UtilMisc.toMap("fixedAssetStdCostTypeId", "USAGE_COST"));
                GenericValue usageCost = EntityUtil.getFirst(EntityUtil.filterByDate(usageCosts));
                if (UtilValidate.isNotEmpty(setupCost) || UtilValidate.isNotEmpty(usageCost)) {
                    String currencyUomId = (setupCost != null? setupCost.getString("amountUomId"): usageCost.getString("amountUomId"));
                    BigDecimal setupCostAmount = ZERO;
                    if (setupCost != null) {
                        setupCostAmount = setupCost.getBigDecimal("amount").multiply(BigDecimal.valueOf(actualSetupMillis.doubleValue()));
                    }
                    BigDecimal usageCostAmount = ZERO;
                    if (usageCost != null) {
                        usageCostAmount = usageCost.getBigDecimal("amount").multiply(BigDecimal.valueOf(actualMilliSeconds.doubleValue()));
                    }
                    BigDecimal fixedAssetCost = setupCostAmount.add(usageCostAmount).setScale(decimals, rounding);
                    fixedAssetCost = fixedAssetCost.divide(BigDecimal.valueOf(3600000), decimals, rounding);
                    // store the cost
                    Map inMap = UtilMisc.toMap("userLogin", userLogin, "workEffortId", productionRunTaskId);
                    inMap.put("costComponentTypeId", "ACTUAL_ROUTE_COST");
                    inMap.put("costUomId", currencyUomId);
                    inMap.put("cost", fixedAssetCost);
                    inMap.put("fixedAssetId", fixedAsset.get("fixedAssetId"));
                    dispatcher.runSync("createCostComponent", inMap);
                }
            }
        } catch (Exception e) {
            return ServiceUtil.returnError("Unable to create routing costs for the production run task [" + productionRunTaskId + "]: " + e.getMessage());
        }
        // materials costs: these are the costs derived from the materials used by the production run task
        try {
            Iterator inventoryAssignIt = delegator.findByAnd("WorkEffortAndInventoryAssign", UtilMisc.toMap("workEffortId", productionRunTaskId)).iterator();
            Map materialsCostByCurrency = FastMap.newInstance();
            while (inventoryAssignIt.hasNext()) {
                GenericValue inventoryConsumed = (GenericValue)inventoryAssignIt.next();
                BigDecimal quantity = inventoryConsumed.getBigDecimal("quantity");
                BigDecimal unitCost = inventoryConsumed.getBigDecimal("unitCost");
                if (UtilValidate.isEmpty(unitCost) || UtilValidate.isEmpty(quantity)) {
                    continue;
                }
                String currencyUomId = inventoryConsumed.getString("currencyUomId");
                if (!materialsCostByCurrency.containsKey(currencyUomId)) {
                    materialsCostByCurrency.put(currencyUomId, BigDecimal.ZERO);
                }
                BigDecimal materialsCost = (BigDecimal)materialsCostByCurrency.get(currencyUomId);
                materialsCost = materialsCost.add(unitCost.multiply(quantity)).setScale(decimals, rounding);
                materialsCostByCurrency.put(currencyUomId, materialsCost);
            }
            Iterator currencyIt = materialsCostByCurrency.keySet().iterator();
            while (currencyIt.hasNext()) {
                String currencyUomId = (String)currencyIt.next();
                BigDecimal materialsCost = (BigDecimal)materialsCostByCurrency.get(currencyUomId);
                Map inMap = UtilMisc.toMap("userLogin", userLogin, "workEffortId", productionRunTaskId);
                inMap.put("costComponentTypeId", "ACTUAL_MAT_COST");
                inMap.put("costUomId", currencyUomId);
                inMap.put("cost", materialsCost);
                dispatcher.runSync("createCostComponent", inMap);
            }
        } catch (Exception e) {
            return ServiceUtil.returnError("Unable to create materials costs for the production run task [" + productionRunTaskId + "]: " + e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * check if field for routingTask update are correct and if need recalculated data in Production Run.
     *  Check<ul>
     *  <li> if estimatedStartDate is not before Production Run estimatedStartDate.</ul>
     *  <li> if there is not a another routingTask with the same priority
     *  If priority or estimatedStartDate has changed recalculated data for routingTask after that one.
     * <br/> update the productionRun
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters, productId, routingId, priority, estimatedStartDate, estimatedSetupMillis, estimatedMilliSeconds
     * @return Map with the result of the service, the output parameters, estimatedCompletionDate.
     */
    public static Map checkUpdatePrunRoutingTask(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String productionRunId = (String) context.get("productionRunId");
        String routingTaskId = (String) context.get("routingTaskId");
        if (! UtilValidate.isEmpty(productionRunId) && ! UtilValidate.isEmpty(routingTaskId)) {
            ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
            if (productionRun.exist()) {

                if (!"PRUN_CREATED".equals(productionRun.getGenericValue().getString("currentStatusId")) &&
                      !"PRUN_SCHEDULED".equals(productionRun.getGenericValue().getString("currentStatusId"))) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunPrinted", locale));
                }

                Timestamp estimatedStartDate = (Timestamp) context.get("estimatedStartDate");
                Timestamp pRestimatedStartDate = productionRun.getEstimatedStartDate();
                if (pRestimatedStartDate.after(estimatedStartDate)) {
                    try {
                        Map resultService = dispatcher.runSync("updateProductionRun", UtilMisc.toMap("productionRunId", productionRunId, "estimatedStartDate", estimatedStartDate, "userLogin", userLogin));
                    } catch (GenericServiceException e) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingRoutingTaskStartDateBeforePRun", locale));
                    }
                }

                Long priority = (Long) context.get("priority");
                List pRRoutingTasks = productionRun.getProductionRunRoutingTasks();
                boolean first = true;
                for (Iterator iter=pRRoutingTasks.iterator();iter.hasNext();) {
                    GenericValue routingTask = (GenericValue) iter.next();
                    if (priority.equals(routingTask.get("priority")) && ! routingTaskId.equals(routingTask.get("workEffortId")))
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingRoutingTaskSeqIdAlreadyExist", locale));
                    if (routingTaskId.equals(routingTask.get("workEffortId"))) {
                        routingTask.set("estimatedSetupMillis", context.get("estimatedSetupMillis"));
                        routingTask.set("estimatedMilliSeconds", context.get("estimatedMilliSeconds"));
                        if (first) {    // for the first routingTask the estimatedStartDate update imply estimatedStartDate productonRun update
                            if (! estimatedStartDate.equals(pRestimatedStartDate)) {
                                productionRun.setEstimatedStartDate(estimatedStartDate);
                            }
                        }
                        // the priority has been changed
                        if (! priority.equals(routingTask.get("priority"))) {
                            routingTask.set("priority", priority);
                            // update the routingTask List and re-read it to be able to have it sorted with the new value
                            if (! productionRun.store()) {
                                Debug.logError("productionRun.store(), in routingTask.priority update, fail for productionRunId ="+productionRunId,module);
                                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunNotUpdated", locale));
                            }
                            productionRun.clearRoutingTasksList();
                        }
                    }
                    if (first) first = false;
                }
                productionRun.setEstimatedCompletionDate(productionRun.recalculateEstimatedCompletionDate(priority, estimatedStartDate));

                if (productionRun.store()) {
                    return ServiceUtil.returnSuccess();
                } else {
                    Debug.logError("productionRun.store() fail for productionRunId ="+productionRunId,module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunNotUpdated", locale));
                }
            }
            Debug.logError("no productionRun for productionRunId ="+productionRunId,module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunNotUpdated", locale));
        }
        Debug.logError("service updateProductionRun call with productionRunId empty",module);
        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunNotUpdated", locale));
    }

    public static Map addProductionRunComponent(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Timestamp now = UtilDateTime.nowTimestamp();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        // Mandatory input fields
        String productionRunId = (String)context.get("productionRunId");
        String productId = (String)context.get("productId");
        BigDecimal quantity = (BigDecimal) context.get("estimatedQuantity");
        // Optional input fields
        String workEffortId = (String)context.get("workEffortId");

        ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
        List tasks = productionRun.getProductionRunRoutingTasks();
        if (UtilValidate.isEmpty(tasks)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunTaskNotExists", locale));
        }

        if (!"PRUN_CREATED".equals(productionRun.getGenericValue().getString("currentStatusId")) &&
              !"PRUN_SCHEDULED".equals(productionRun.getGenericValue().getString("currentStatusId"))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunPrinted", locale));
        }

        if (workEffortId != null) {
            boolean found = false;
            for (int i = 0; i < tasks.size(); i++) {
                GenericValue oneTask = (GenericValue)tasks.get(i);
                if (oneTask.getString("workEffortId").equals(workEffortId)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunTaskNotExists", locale));
            }
        } else {
            workEffortId = EntityUtil.getFirst(tasks).getString("workEffortId");
        }

        try {
            // Find the product
            GenericValue product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));
            if (product == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductNotExist", locale));
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            return ServiceUtil.returnError(e.getMessage());
        }
        Map serviceContext = new HashMap();
        serviceContext.clear();
        serviceContext.put("workEffortId", workEffortId);
        serviceContext.put("productId", productId);
        serviceContext.put("workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED");
        serviceContext.put("statusId", "WEGS_CREATED");
        serviceContext.put("fromDate", now);
        serviceContext.put("estimatedQuantity", quantity);
        serviceContext.put("userLogin", userLogin);
        Map resultService = null;
        try {
            resultService = dispatcher.runSync("createWorkEffortGoodStandard", serviceContext);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the createWorkEffortGoodStandard service", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunComponentNotAdded", locale));
        }
        result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(resource, "ManufacturingProductionRunComponentAdded",UtilMisc.toMap("productionRunId", productionRunId), locale));
        return result;
    }

    public static Map updateProductionRunComponent(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        // Mandatory input fields
        String productionRunId = (String)context.get("productionRunId");
        String productId = (String)context.get("productId");
        // Optional input fields
        String workEffortId = (String)context.get("workEffortId"); // the production run task
        BigDecimal quantity = (BigDecimal) context.get("estimatedQuantity");

        ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
        List components = productionRun.getProductionRunComponents();
        if (UtilValidate.isEmpty(components)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunComponentNotExists", locale));
        }

        if (!"PRUN_CREATED".equals(productionRun.getGenericValue().getString("currentStatusId")) &&
              !"PRUN_SCHEDULED".equals(productionRun.getGenericValue().getString("currentStatusId"))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunPrinted", locale));
        }

        boolean found = false;
        GenericValue theComponent = null;
        for (int i = 0; i < components.size(); i++) {
            theComponent = (GenericValue)components.get(i);
            if (theComponent.getString("productId").equals(productId)) {
                if (workEffortId != null) {
                    if (theComponent.getString("workEffortId").equals(workEffortId)) {
                        found = true;
                        break;
                    }
                } else {
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunTaskNotExists", locale));
        }

        try {
            // Find the product
            GenericValue product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));
            if (product == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductNotExist", locale));
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            return ServiceUtil.returnError(e.getMessage());
        }
        Map serviceContext = new HashMap();
        serviceContext.clear();
        serviceContext.put("workEffortId", theComponent.getString("workEffortId"));
        serviceContext.put("workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED");
        serviceContext.put("productId", productId);
        serviceContext.put("fromDate", theComponent.getTimestamp("fromDate"));
        if (quantity != null) {
            serviceContext.put("estimatedQuantity", quantity);
        }
        serviceContext.put("userLogin", userLogin);
        Map resultService = null;
        try {
            resultService = dispatcher.runSync("updateWorkEffortGoodStandard", serviceContext);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the updateWorkEffortGoodStandard service", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunComponentNotAdded", locale));
        }
        result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(resource, "ManufacturingProductionRunComponentUpdated",UtilMisc.toMap("productionRunId", productionRunId), locale));
        return result;
    }

    public static Map addProductionRunRoutingTask(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        // Mandatory input fields
        String productionRunId = (String)context.get("productionRunId");
        String routingTaskId = (String)context.get("routingTaskId");
        Long priority = (Long)context.get("priority");

        // Optional input fields
        String workEffortName = (String)context.get("workEffortName");
        String description = (String)context.get("description");
        Timestamp estimatedStartDate = (Timestamp)context.get("estimatedStartDate");
        Timestamp estimatedCompletionDate = (Timestamp)context.get("estimatedCompletionDate");
        Double estimatedSetupMillis = (Double)context.get("estimatedSetupMillis");
        Double estimatedMilliSeconds = (Double)context.get("estimatedMilliSeconds");

        // The production run is loaded
        ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
        BigDecimal pRQuantity = productionRun.getQuantity();
        if (pRQuantity == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunTaskNotExists", locale));
        }

        if (!"PRUN_CREATED".equals(productionRun.getGenericValue().getString("currentStatusId")) &&
              !"PRUN_SCHEDULED".equals(productionRun.getGenericValue().getString("currentStatusId"))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunPrinted", locale));
        }

        if (estimatedStartDate != null) {
            Timestamp pRestimatedStartDate = productionRun.getEstimatedStartDate();
            if (pRestimatedStartDate.after(estimatedStartDate)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingRoutingTaskStartDateBeforePRun", locale));
            }
        }

        // The routing task is loaded
        GenericValue routingTask = null;
        try {
            routingTask = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", routingTaskId));
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(),  module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingRoutingTaskNotExists", locale));
        }
        if (routingTask == null) {
            Debug.logError("Routing task: " + routingTaskId + " is null.",  module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingRoutingTaskNotExists", locale));
        }

        if (workEffortName == null) {
            workEffortName = (String)routingTask.get("workEffortName");
        }
        if (description == null) {
            description = (String)routingTask.get("description");
        }
        if (estimatedSetupMillis == null) {
            estimatedSetupMillis = (Double)routingTask.get("estimatedSetupMillis");
        }
        if (estimatedMilliSeconds == null) {
            estimatedMilliSeconds = (Double)routingTask.get("estimatedMilliSeconds");
        }
        if (estimatedStartDate == null) {
            estimatedStartDate = productionRun.getEstimatedStartDate();
        }
        if (estimatedCompletionDate == null) {
            // Calculate the estimatedCompletionDate
            long totalTime = ProductionRun.getEstimatedTaskTime(routingTask, pRQuantity, dispatcher);
            estimatedCompletionDate = TechDataServices.addForward(TechDataServices.getTechDataCalendar(routingTask), estimatedStartDate, totalTime);
        }
        Map serviceContext = new HashMap();
        serviceContext.clear();
        serviceContext.put("priority", priority);
        serviceContext.put("workEffortPurposeTypeId", routingTask.get("workEffortPurposeTypeId"));
        serviceContext.put("workEffortName", workEffortName);
        serviceContext.put("description", description);
        serviceContext.put("fixedAssetId", routingTask.get("fixedAssetId"));
        serviceContext.put("workEffortTypeId", "PROD_ORDER_TASK");
        serviceContext.put("currentStatusId","PRUN_CREATED");
        serviceContext.put("workEffortParentId", productionRunId);
        serviceContext.put("facilityId", productionRun.getGenericValue().getString("facilityId"));
        serviceContext.put("estimatedStartDate", estimatedStartDate);
        serviceContext.put("estimatedCompletionDate", estimatedCompletionDate);
        serviceContext.put("estimatedSetupMillis", estimatedSetupMillis);
        serviceContext.put("estimatedMilliSeconds", estimatedMilliSeconds);
        serviceContext.put("quantityToProduce", pRQuantity);
        serviceContext.put("userLogin", userLogin);
        Map resultService = null;
        try {
            resultService = dispatcher.runSync("createWorkEffort", serviceContext);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the createWorkEffort service", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingAddProductionRunRoutingTaskNotCreated", locale));
        }
        String productionRunTaskId = (String) resultService.get("workEffortId");
        if (Debug.infoOn()) Debug.logInfo("ProductionRunTaskId created: " + productionRunTaskId, module);


        productionRun.setEstimatedCompletionDate(productionRun.recalculateEstimatedCompletionDate());
        if (!productionRun.store()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingAddProductionRunRoutingTaskNotCreated", locale));
        }

        // copy date valid WorkEffortPartyAssignments from the routing task to the run task
        List workEffortPartyAssignments = null;
        try {
            workEffortPartyAssignments = EntityUtil.filterByDate(delegator.findByAnd("WorkEffortPartyAssignment", UtilMisc.toMap("workEffortId", routingTaskId)));
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(),  module);
        }
        if (workEffortPartyAssignments != null) {
            Iterator i = workEffortPartyAssignments.iterator();
            while (i.hasNext()) {
                GenericValue workEffortPartyAssignment = (GenericValue) i.next();
                Map partyToWorkEffort = UtilMisc.toMap(
                        "workEffortId",  productionRunTaskId,
                        "partyId",  workEffortPartyAssignment.getString("partyId"),
                        "roleTypeId",  workEffortPartyAssignment.getString("roleTypeId"),
                        "fromDate",  workEffortPartyAssignment.getTimestamp("fromDate"),
                        "statusId",  workEffortPartyAssignment.getString("statusId"),
                        "userLogin", userLogin
               );
                try {
                    resultService = dispatcher.runSync("assignPartyToWorkEffort", partyToWorkEffort);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem calling the assignPartyToWorkEffort service", module);
                }
                if (Debug.infoOn()) Debug.logInfo("ProductionRunPartyassigment for party: " + workEffortPartyAssignment.get("partyId") + " created", module);
            }
        }

        result.put("routingTaskId", productionRunTaskId);
        result.put("estimatedStartDate", estimatedStartDate);
        result.put("estimatedCompletionDate", estimatedCompletionDate);
        return result;
    }

    public static Map productionRunProduce(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        // Mandatory input fields
        String productionRunId = (String)context.get("workEffortId");

        // Optional input fields
        BigDecimal quantity = (BigDecimal) context.get("quantity");
        String inventoryItemTypeId = (String)context.get("inventoryItemTypeId");
        String lotId = (String)context.get("lotId");
        Boolean createLotIfNeeded = (Boolean)context.get("createLotIfNeeded");
        Boolean autoCreateLot = (Boolean)context.get("autoCreateLot");

        // The default is non-serialized inventory item
        if (UtilValidate.isEmpty(inventoryItemTypeId)) {
            inventoryItemTypeId = "NON_SERIAL_INV_ITEM";
        }
        // The default is to create a lot if the lotId is given, but the lot doesn't exist
        if (createLotIfNeeded == null) {
            createLotIfNeeded = Boolean.TRUE;
        }
        if (autoCreateLot == null) {
            autoCreateLot = Boolean.FALSE;
        }

        List inventoryItemIds = new ArrayList();
        result.put("inventoryItemIds", inventoryItemIds);
        // The production run is loaded
        ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
        // The last task is loaded
        GenericValue lastTask = productionRun.getLastProductionRunRoutingTask();
        if (lastTask == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunTaskNotExists", locale));
        }
        if ("WIP".equals(productionRun.getProductProduced().getString("productTypeId"))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductIsWIP", locale));
        }
        BigDecimal quantityProduced = productionRun.getGenericValue().getBigDecimal("quantityProduced");

        if (quantityProduced == null) {
            quantityProduced = BigDecimal.ZERO;
        }
        BigDecimal quantityDeclared = lastTask.getBigDecimal("quantityProduced");

        if (quantityDeclared == null) {
            quantityDeclared = BigDecimal.ZERO;
        }
        // If the quantity already produced is not lower than the quantity declared, no inventory is created.
        BigDecimal maxQuantity = quantityDeclared.subtract(quantityProduced);

        if (maxQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return result;
        }

        // If quantity was not passed, the max quantity is used
        if (quantity == null) {
            quantity = maxQuantity;
        }
        //
        if (quantity.compareTo(maxQuantity) > 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunProductProducedNotStillAvailable", locale));
        }

        if (lotId == null && autoCreateLot.booleanValue()) {
            lotId = delegator.getNextSeqId("Lot");
            createLotIfNeeded = Boolean.TRUE;
        }
        if (UtilValidate.isNotEmpty(lotId)) {
            try {
                // Find the lot
                GenericValue lot = delegator.findByPrimaryKey("Lot", UtilMisc.toMap("lotId", lotId));
                if (lot == null) {
                    if (createLotIfNeeded.booleanValue()) {
                        lot = delegator.makeValue("Lot", UtilMisc.toMap("lotId", lotId, "creationDate", UtilDateTime.nowDate()));
                        lot.create();
                    } else {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingLotNotExists", locale));
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), module);
                return ServiceUtil.returnError(e.getMessage());
            }
        }

        GenericValue orderItem = null;
        try {
            // Find the related order item (if exists)
            List orderItems = productionRun.getGenericValue().getRelated("WorkOrderItemFulfillment");
            orderItem = EntityUtil.getFirst(orderItems);
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            return ServiceUtil.returnError(e.getMessage());
        }
        // the inventory item unit cost is the product's standard cost
        BigDecimal unitCost = ZERO;
        try {
            // get the currency
            GenericValue facility = productionRun.getGenericValue().getRelatedOne("Facility");
            Map outputMap = dispatcher.runSync("getPartyAccountingPreferences", UtilMisc.<String, Object>toMap("userLogin", userLogin, "organizationPartyId", facility.getString("ownerPartyId")));
            Map partyAccountingPreference = (Map)outputMap.get("partyAccountingPreference");
            if (partyAccountingPreference == null) {
                return ServiceUtil.returnError("Unable to find a currency for production run costs");
            }
            outputMap = dispatcher.runSync("getProductCost", UtilMisc.<String, Object>toMap("userLogin", userLogin, "productId", productionRun.getProductProduced().getString("productId"), "currencyUomId", (String)partyAccountingPreference.get("baseCurrencyUomId"), "costComponentTypePrefix", "EST_STD"));
            unitCost = (BigDecimal)outputMap.get("productCost");
            if (unitCost == null) {
                unitCost = ZERO;
            }

        } catch (Exception e) {
            Debug.logWarning(e.getMessage(), module);
            return ServiceUtil.returnError(e.getMessage());
        }

        if ("SERIALIZED_INV_ITEM".equals(inventoryItemTypeId)) {
            try {
                int numOfItems = quantity.intValue();
                for (int i = 0; i < numOfItems; i++) {
                    Map serviceContext = UtilMisc.toMap("productId", productionRun.getProductProduced().getString("productId"),
                                                        "inventoryItemTypeId", "SERIALIZED_INV_ITEM",
                                                        "statusId", "INV_AVAILABLE");
                    serviceContext.put("facilityId", productionRun.getGenericValue().getString("facilityId"));
                    serviceContext.put("datetimeReceived", UtilDateTime.nowTimestamp());
                    serviceContext.put("comments", "Created by production run " + productionRunId);
                    if (unitCost.compareTo(ZERO) != 0) {
                        serviceContext.put("unitCost", unitCost);
                    }
                    //serviceContext.put("serialNumber", productionRunId);
                    serviceContext.put("lotId", lotId);
                    serviceContext.put("userLogin", userLogin);
                    Map resultService = dispatcher.runSync("createInventoryItem", serviceContext);
                    String inventoryItemId = (String)resultService.get("inventoryItemId");
                    inventoryItemIds.add(inventoryItemId);
                    serviceContext.clear();
                    serviceContext.put("inventoryItemId", inventoryItemId);
                    serviceContext.put("workEffortId", productionRunId);
                    serviceContext.put("availableToPromiseDiff", BigDecimal.ONE);
                    serviceContext.put("quantityOnHandDiff", BigDecimal.ONE);
                    serviceContext.put("userLogin", userLogin);
                    resultService = dispatcher.runSync("createInventoryItemDetail", serviceContext);
                    serviceContext.clear();
                    serviceContext.put("userLogin", userLogin);
                    serviceContext.put("workEffortId", productionRunId);
                    serviceContext.put("inventoryItemId", inventoryItemId);
                    resultService = dispatcher.runSync("createWorkEffortInventoryProduced", serviceContext);
                    // Recompute reservations
                    serviceContext = new HashMap();
                    serviceContext.put("inventoryItemId", inventoryItemId);
                    serviceContext.put("userLogin", userLogin);
                    resultService = dispatcher.runSync("balanceInventoryItems", serviceContext);
                }
            } catch (Exception exc) {
                return ServiceUtil.returnError(exc.getMessage());
            }
        } else {
            try {
                Map serviceContext = UtilMisc.toMap("productId", productionRun.getProductProduced().getString("productId"),
                                                    "inventoryItemTypeId", "NON_SERIAL_INV_ITEM");
                serviceContext.put("facilityId", productionRun.getGenericValue().getString("facilityId"));
                serviceContext.put("datetimeReceived", UtilDateTime.nowTimestamp());
                serviceContext.put("comments", "Created by production run " + productionRunId);
                serviceContext.put("lotId", lotId);
                if (unitCost.compareTo(ZERO) != 0) {
                    serviceContext.put("unitCost", unitCost);
                }
                serviceContext.put("userLogin", userLogin);
                Map resultService = dispatcher.runSync("createInventoryItem", serviceContext);
                String inventoryItemId = (String)resultService.get("inventoryItemId");
                inventoryItemIds.add(inventoryItemId);
                serviceContext.clear();
                serviceContext.put("inventoryItemId", inventoryItemId);
                serviceContext.put("workEffortId", productionRunId);
                serviceContext.put("availableToPromiseDiff", quantity);
                serviceContext.put("quantityOnHandDiff", quantity);
                serviceContext.put("userLogin", userLogin);
                resultService = dispatcher.runSync("createInventoryItemDetail", serviceContext);
                serviceContext.clear();
                serviceContext.put("userLogin", userLogin);
                serviceContext.put("workEffortId", productionRunId);
                serviceContext.put("inventoryItemId", inventoryItemId);
                resultService = dispatcher.runSync("createWorkEffortInventoryProduced", serviceContext);
                // Recompute reservations
                serviceContext = new HashMap();
                serviceContext.put("inventoryItemId", inventoryItemId);
                serviceContext.put("userLogin", userLogin);
                if (orderItem != null) {
                    // the reservations of this order item are privileged reservations
                    serviceContext.put("priorityOrderId", orderItem.getString("orderId"));
                    serviceContext.put("priorityOrderItemSeqId", orderItem.getString("orderItemSeqId"));
                }
                resultService = dispatcher.runSync("balanceInventoryItems", serviceContext);
            } catch (Exception exc) {
                return ServiceUtil.returnError(exc.getMessage());
            }
        }
        // Now the production run's quantityProduced is updated
        Map serviceContext = UtilMisc.toMap("workEffortId", productionRunId);
        serviceContext.put("quantityProduced", quantityProduced.add(quantity));
        serviceContext.put("actualCompletionDate", UtilDateTime.nowTimestamp());
        serviceContext.put("userLogin", userLogin);
        try {
            Map resultService = dispatcher.runSync("updateWorkEffort", serviceContext);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the updateWorkEffort service", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusNotChanged", locale));
        }

        result.put("quantity", quantity);
        return result;
    }

    public static Map productionRunDeclareAndProduce(DispatchContext ctx, Map context) {
        Map result = FastMap.newInstance();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        // Mandatory input fields
        String productionRunId = (String)context.get("workEffortId");

        // Optional input fields
        BigDecimal quantity = (BigDecimal)context.get("quantity");
        Map componentsLocationMap = (Map)context.get("componentsLocationMap");

        // The production run is loaded
        ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);

        BigDecimal quantityProduced = productionRun.getGenericValue().getBigDecimal("quantityProduced");
        BigDecimal quantityToProduce = productionRun.getGenericValue().getBigDecimal("quantityToProduce");
        if (quantityProduced == null) {
            quantityProduced = BigDecimal.ZERO;
        }
        if (quantityToProduce == null) {
            quantityToProduce = BigDecimal.ZERO;
        }
        BigDecimal minimumQuantityProducedByTask = quantityProduced.add(quantity);
        if (minimumQuantityProducedByTask.compareTo(quantityToProduce) > 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingQuantityProducedIsHigherThanQuantityDeclared", locale));
        }

        List tasks = productionRun.getProductionRunRoutingTasks();
        for (int i = 0; i < tasks.size(); i++) {
            GenericValue oneTask = (GenericValue)tasks.get(i);
            String taskId = oneTask.getString("workEffortId");
            if ("PRUN_RUNNING".equals(oneTask.getString("currentStatusId"))) {
                BigDecimal quantityDeclared = oneTask.getBigDecimal("quantityProduced");
                if (quantityDeclared == null) {
                    quantityDeclared = BigDecimal.ZERO;
                }
                if (minimumQuantityProducedByTask.compareTo(quantityDeclared) > 0) {
                    try {
                        Map serviceContext = UtilMisc.toMap("productionRunId", productionRunId, "productionRunTaskId", taskId);
                        serviceContext.put("addQuantityProduced", minimumQuantityProducedByTask.subtract(quantityDeclared));
                        serviceContext.put("issueRequiredComponents", Boolean.TRUE);
                        serviceContext.put("componentsLocationMap", componentsLocationMap);
                        serviceContext.put("userLogin", userLogin);
                        Map resultService = dispatcher.runSync("updateProductionRunTask", serviceContext);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, "Problem calling the changeProductionRunTaskStatus service", module);
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusNotChanged", locale));
                    }
                }
            }
        }
        try {
            Map inputMap = FastMap.newInstance();
            inputMap.putAll(context);
            inputMap.remove("componentsLocationMap");
            result = dispatcher.runSync("productionRunProduce", inputMap);
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the changeProductionRunTaskStatus service", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusNotChanged", locale));
        }
        return result;
    }

    public static Map productionRunTaskProduce(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        // Mandatory input fields
        String productionRunTaskId = (String)context.get("workEffortId");
        String productId = (String)context.get("productId");
        BigDecimal quantity = (BigDecimal)context.get("quantity");

        // Optional input fields
        String facilityId = (String)context.get("facilityId");
        String currencyUomId = (String)context.get("currencyUomId");
        BigDecimal unitCost = (BigDecimal)context.get("unitCost");
        String inventoryItemTypeId = (String)context.get("inventoryItemTypeId");

        // The default is non-serialized inventory item
        if (UtilValidate.isEmpty(inventoryItemTypeId)) {
            inventoryItemTypeId = "NON_SERIAL_INV_ITEM";
        }

        if (facilityId == null) {
            // The production run is loaded
            ProductionRun productionRun = new ProductionRun(productionRunTaskId, delegator, dispatcher);
            facilityId = productionRun.getGenericValue().getString("facilityId");
        }
        List inventoryItemIds = new ArrayList();
        if ("SERIALIZED_INV_ITEM".equals(inventoryItemTypeId)) {
            try {
                int numOfItems = quantity.intValue();
                for (int i = 0; i < numOfItems; i++) {
                    Map serviceContext = UtilMisc.toMap("productId", productId,
                                                        "inventoryItemTypeId", "SERIALIZED_INV_ITEM",
                                                        "statusId", "INV_AVAILABLE");
                    serviceContext.put("facilityId", facilityId);
                    serviceContext.put("datetimeReceived", UtilDateTime.nowDate());
                    serviceContext.put("comments", "Created by production run task " + productionRunTaskId);
                    if (unitCost != null) {
                        serviceContext.put("unitCost", unitCost);
                        serviceContext.put("currencyUomId", currencyUomId);
                    }
                    serviceContext.put("userLogin", userLogin);
                    Map resultService = dispatcher.runSync("createInventoryItem", serviceContext);
                    String inventoryItemId = (String)resultService.get("inventoryItemId");
                    serviceContext.clear();
                    serviceContext.put("inventoryItemId", inventoryItemId);
                    serviceContext.put("workEffortId", productionRunTaskId);
                    serviceContext.put("availableToPromiseDiff", BigDecimal.ONE);
                    serviceContext.put("quantityOnHandDiff", BigDecimal.ONE);
                    serviceContext.put("userLogin", userLogin);
                    resultService = dispatcher.runSync("createInventoryItemDetail", serviceContext);
                    serviceContext.clear();
                    serviceContext.put("userLogin", userLogin);
                    serviceContext.put("workEffortId", productionRunTaskId);
                    serviceContext.put("inventoryItemId", inventoryItemId);
                    resultService = dispatcher.runSync("createWorkEffortInventoryProduced", serviceContext);
                    inventoryItemIds.add(inventoryItemId);
                    // Recompute reservations
                    serviceContext = new HashMap();
                    serviceContext.put("inventoryItemId", inventoryItemId);
                    serviceContext.put("userLogin", userLogin);
                    resultService = dispatcher.runSync("balanceInventoryItems", serviceContext);
                }
            } catch (Exception exc) {
                return ServiceUtil.returnError(exc.getMessage());
            }
        } else {
            try {
                Map serviceContext = UtilMisc.toMap("productId", productId,
                                                    "inventoryItemTypeId", "NON_SERIAL_INV_ITEM");
                serviceContext.put("facilityId", facilityId);
                serviceContext.put("datetimeReceived", UtilDateTime.nowTimestamp());
                serviceContext.put("comments", "Created by production run task " + productionRunTaskId);
                if (unitCost != null) {
                    serviceContext.put("unitCost", unitCost);
                    serviceContext.put("currencyUomId", currencyUomId);
                }
                serviceContext.put("userLogin", userLogin);
                Map resultService = dispatcher.runSync("createInventoryItem", serviceContext);
                String inventoryItemId = (String)resultService.get("inventoryItemId");

                serviceContext.clear();
                serviceContext.put("inventoryItemId", inventoryItemId);
                serviceContext.put("workEffortId", productionRunTaskId);
                serviceContext.put("availableToPromiseDiff", quantity);
                serviceContext.put("quantityOnHandDiff", quantity);
                serviceContext.put("userLogin", userLogin);
                resultService = dispatcher.runSync("createInventoryItemDetail", serviceContext);
                serviceContext.clear();
                serviceContext.put("userLogin", userLogin);
                serviceContext.put("workEffortId", productionRunTaskId);
                serviceContext.put("inventoryItemId", inventoryItemId);
                resultService = dispatcher.runSync("createWorkEffortInventoryProduced", serviceContext);
                inventoryItemIds.add(inventoryItemId);
                // Recompute reservations
                serviceContext = new HashMap();
                serviceContext.put("inventoryItemId", inventoryItemId);
                serviceContext.put("userLogin", userLogin);
                resultService = dispatcher.runSync("balanceInventoryItems", serviceContext);
            } catch (Exception exc) {
                return ServiceUtil.returnError(exc.getMessage());
            }
        }
        result.put("inventoryItemIds", inventoryItemIds);
        return result;
    }

    public static Map productionRunTaskReturnMaterial(DispatchContext ctx, Map context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        // Mandatory input fields
        String productionRunTaskId = (String)context.get("workEffortId");
        String productId = (String)context.get("productId");
        // Optional input fields
        BigDecimal quantity = (BigDecimal)context.get("quantity");
        if (quantity == null || quantity.compareTo(ZERO) == 0) {
            return ServiceUtil.returnSuccess();
        }
        // Verify how many items of the given productId
        // are currently assigned to this task.
        // If less than passed quantity then return an error message.
        try {
            Iterator issuances = (delegator.findByAnd("WorkEffortAndInventoryAssign", UtilMisc.toMap("workEffortId", productionRunTaskId, "productId", productId))).iterator();
            BigDecimal totalIssued = BigDecimal.ZERO;
            while (issuances.hasNext()) {
                GenericValue issuance = (GenericValue)issuances.next();
                BigDecimal issued = issuance.getBigDecimal("quantity");
                if (issued != null) {
                    totalIssued = totalIssued.add(issued);
                }
            }
            Iterator returns = (delegator.findByAnd("WorkEffortAndInventoryProduced", UtilMisc.toMap("workEffortId", productionRunTaskId, "productId", productId))).iterator();
            BigDecimal totalReturned = BigDecimal.ZERO;
            while (returns.hasNext()) {
                GenericValue returned = (GenericValue)returns.next();
                GenericValue returnDetail = EntityUtil.getFirst(delegator.findByAnd("InventoryItemDetail", UtilMisc.toMap("inventoryItemId", returned.getString("inventoryItemId")), UtilMisc.toList("inventoryItemDetailSeqId")));
                if (returnDetail != null) {
                    BigDecimal qtyReturned = returnDetail.getBigDecimal("quantityOnHandDiff");
                    if (qtyReturned != null) {
                        totalReturned = totalReturned.add(qtyReturned);
                    }
                }
            }
            if (quantity.compareTo(totalIssued.subtract(totalReturned)) > 0) {
                return ServiceUtil.returnError("Production Run Task with id [" + productionRunTaskId + "] cannot return more items [" + quantity + "] than the ones currently allocated [" + (totalIssued.subtract(totalReturned)) + "]");
            }
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError(gee.getMessage());
        }
        String inventoryItemTypeId = (String)context.get("inventoryItemTypeId");

        // TODO: if the task is not running, then return an error message.

        try {
            Map inventoryResult = dispatcher.runSync("productionRunTaskProduce", UtilMisc.<String, Object>toMap("workEffortId", productionRunTaskId,
                                                                                                "productId", productId,
                                                                                                "quantity", quantity,
                                                                                                "inventoryItemTypeId", inventoryItemTypeId,
                                                                                                "userLogin", userLogin));
            if (ServiceUtil.isError(inventoryResult)) {
                return ServiceUtil.returnError("Error calling productionRunTaskProduce: " + ServiceUtil.getErrorMessage(inventoryResult));
            }
        } catch (GenericServiceException exc) {
            return ServiceUtil.returnError(exc.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map updateProductionRunTask(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        // Mandatory input fields
        String productionRunId = (String)context.get("productionRunId");
        String workEffortId = (String)context.get("productionRunTaskId");
        String partyId = (String)context.get("partyId");
        if (UtilValidate.isEmpty(partyId)) {
            partyId = userLogin.getString("partyId");
        }

        // Optional input fields
        Timestamp fromDate = (Timestamp)context.get("fromDate");
        Timestamp toDate = (Timestamp)context.get("toDate");
        BigDecimal addQuantityProduced = (BigDecimal)context.get("addQuantityProduced");
        BigDecimal addQuantityRejected = (BigDecimal)context.get("addQuantityRejected");
        BigDecimal addSetupTime = (BigDecimal)context.get("addSetupTime");
        BigDecimal addTaskTime = (BigDecimal)context.get("addTaskTime");
        String comments = (String)context.get("comments");
        Boolean issueRequiredComponents = (Boolean)context.get("issueRequiredComponents");
        Map componentsLocationMap = (Map)context.get("componentsLocationMap");

        if (issueRequiredComponents == null) {
            issueRequiredComponents = Boolean.FALSE;
        }
        if (fromDate == null) {
            fromDate = UtilDateTime.nowTimestamp();
        }
        if (toDate == null) {
            toDate = UtilDateTime.nowTimestamp();
        }
        if (addQuantityProduced == null) {
            addQuantityProduced = BigDecimal.ZERO;
        }
        if (addQuantityRejected == null) {
            addQuantityRejected = BigDecimal.ZERO;
        }
        if (comments == null) {
            comments = "";
        }

        ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
        if (!productionRun.exist()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunNotExists", locale));
        }
        List tasks = productionRun.getProductionRunRoutingTasks();
        GenericValue theTask = null;
        GenericValue oneTask = null;
        for (int i = 0; i < tasks.size(); i++) {
            oneTask = (GenericValue)tasks.get(i);
            if (oneTask.getString("workEffortId").equals(workEffortId)) {
                theTask = oneTask;
                break;
            }
        }
        if (theTask == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunTaskNotExists", locale));
        }

        String currentStatusId = theTask.getString("currentStatusId");

        if (!currentStatusId.equals("PRUN_RUNNING")) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunTaskNotRunning", locale));
        }

        BigDecimal quantityProduced = theTask.getBigDecimal("quantityProduced");
        if (quantityProduced == null) {
            quantityProduced = BigDecimal.ZERO;
        }
        BigDecimal quantityRejected = theTask.getBigDecimal("quantityRejected");
        if (quantityRejected == null) {
            quantityRejected = BigDecimal.ZERO;
        }
        BigDecimal totalQuantityProduced = quantityProduced.add(addQuantityProduced);
        BigDecimal totalQuantityRejected = quantityRejected.add(addQuantityRejected);

        if (issueRequiredComponents.booleanValue() && addQuantityProduced.compareTo(ZERO) > 0) {
            BigDecimal quantityToProduce = theTask.getBigDecimal("quantityToProduce");
            if (quantityToProduce == null) {
                quantityToProduce = BigDecimal.ZERO;
            }
            if (quantityToProduce.compareTo(ZERO) > 0) {
                try {
                    List<GenericValue> components = theTask.getRelated("WorkEffortGoodStandard");
                    for (GenericValue component : components) {
                        BigDecimal totalRequiredMaterialQuantity = component.getBigDecimal("estimatedQuantity").multiply(totalQuantityProduced).divide(quantityToProduce, rounding);
                        // now get the units that have been already issued and subtract them
                        List<GenericValue> issuances = delegator.findByAnd("WorkEffortAndInventoryAssign", UtilMisc.toMap("workEffortId", workEffortId, "productId", component.getString("productId")));
                        BigDecimal totalIssued = BigDecimal.ZERO;
                        for (GenericValue issuance : issuances) {
                            BigDecimal issued = issuance.getBigDecimal("quantity");
                            if (issued != null) {
                                totalIssued = totalIssued.add(issued);
                            }
                        }
                        BigDecimal requiredQuantity = totalRequiredMaterialQuantity.subtract(totalIssued);
                        if (requiredQuantity.compareTo(ZERO) > 0) {
                            GenericPK key = component.getPrimaryKey();
                            Map componentsLocation = null;
                            if (componentsLocationMap != null) {
                                componentsLocation = (Map)componentsLocationMap.get(key);
                            }
                            Map serviceContext = UtilMisc.toMap("workEffortId", workEffortId, "productId", component.getString("productId"), "fromDate", component.getTimestamp("fromDate"));
                            serviceContext.put("quantity", requiredQuantity);
                            if (componentsLocation != null) {
                                serviceContext.put("locationSeqId", (String)componentsLocation.get("locationSeqId"));
                                serviceContext.put("secondaryLocationSeqId", (String)componentsLocation.get("secondaryLocationSeqId"));
                                serviceContext.put("failIfItemsAreNotAvailable", (String)componentsLocation.get("failIfItemsAreNotAvailable"));
                            }
                            serviceContext.put("userLogin", userLogin);
                            Map resultService = dispatcher.runSync("issueProductionRunTaskComponent", serviceContext);
                        }
                    }
                } catch (GenericEntityException gee) {

                } catch (GenericServiceException gee) {

                }
            }
        }

        // Create a new TimeEntry
        try {
            /*
            String timeEntryId = delegator.getNextSeqId("TimeEntry");
            Map timeEntryFields = UtilMisc.toMap("timeEntryId", timeEntryId,
                                                 "workEffortId", workEffortId);
            Double totalTime = Double.valueOf(addSetupTime.doubleValue() + addTaskTime.doubleValue());
            timeEntryFields.put("partyId", partyId);
            timeEntryFields.put("fromDate", fromDate);
            timeEntryFields.put("thruDate", toDate);
            timeEntryFields.put("hours", totalTime); // FIXME
            //timeEntryFields.put("setupTime", addSetupTime); // FIXME
            //timeEntryFields.put("quantityProduced", addQuantityProduced); // FIXME
            //timeEntryFields.put("quantityRejected", addQuantityRejected); // FIXME
            timeEntryFields.put("comments", comments);
            GenericValue timeEntry = delegator.makeValue("TimeEntry", timeEntryFields);
            timeEntry.create();
            */
            Map serviceContext = new HashMap();
            serviceContext.clear();
            serviceContext.put("workEffortId", workEffortId);
            if (addTaskTime != null) {
                Double actualMilliSeconds = theTask.getDouble("actualMilliSeconds");
                if (actualMilliSeconds == null) {
                    actualMilliSeconds = Double.valueOf(0);
                }
                serviceContext.put("actualMilliSeconds", Double.valueOf(actualMilliSeconds.doubleValue() + addTaskTime.doubleValue()));
            }
            if (addSetupTime != null) {
                Double actualSetupMillis = theTask.getDouble("actualSetupMillis");
                if (actualSetupMillis == null) {
                    actualSetupMillis = Double.valueOf(0);
                }
                serviceContext.put("actualSetupMillis", Double.valueOf(actualSetupMillis.doubleValue() + addSetupTime.doubleValue()));
            }
            serviceContext.put("quantityProduced", totalQuantityProduced);
            serviceContext.put("quantityRejected", totalQuantityRejected);
            serviceContext.put("userLogin", userLogin);
            Map resultService = dispatcher.runSync("updateWorkEffort", serviceContext);
        } catch (Exception exc) {
            return ServiceUtil.returnError(exc.getMessage());
        }

        return result;
    }

    public static Map approveRequirement(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        // Mandatory input fields
        String requirementId = (String)context.get("requirementId");
        GenericValue requirement = null;
        try {
            requirement = delegator.findByPrimaryKey("Requirement", UtilMisc.toMap("requirementId", requirementId));
        } catch (GenericEntityException gee) {
        }

        if (requirement == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingRequirementNotExists", locale));
        }
        try {
            result = dispatcher.runSync("updateRequirement", UtilMisc.<String, Object>toMap("requirementId", requirementId, "statusId", "REQ_APPROVED", "requirementTypeId", requirement.getString("requirementTypeId"), "userLogin", userLogin));
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingRequirementNotUpdated", locale));
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map createProductionRunFromRequirement(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        // Mandatory input fields
        String requirementId = (String)context.get("requirementId");
        // Optional input fields
        BigDecimal quantity = (BigDecimal)context.get("quantity");

        GenericValue requirement = null;
        try {
            requirement = delegator.findByPrimaryKey("Requirement", UtilMisc.toMap("requirementId", requirementId));
        } catch (GenericEntityException gee) {
        }
        if (requirement == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingRequirementNotExists", locale));
        }
        if (!"INTERNAL_REQUIREMENT".equals(requirement.getString("requirementTypeId"))) {
            return ServiceUtil.returnSuccess();
        }

        if (quantity == null) {
            quantity = requirement.getBigDecimal("quantity");
        }
        Map serviceContext = new HashMap();
        serviceContext.clear();
        serviceContext.put("productId", requirement.getString("productId"));
        serviceContext.put("quantity", quantity);
        serviceContext.put("startDate", requirement.getTimestamp("requirementStartDate"));
        serviceContext.put("facilityId", requirement.getString("facilityId"));
        String workEffortName = null;
        if (requirement.getString("description") != null) {
            workEffortName = requirement.getString("description");
            if (workEffortName.length() > 50) {
                workEffortName = workEffortName.substring(0, 50);
            }
        } else {
            workEffortName = "Created from requirement " + requirement.getString("requirementId");
        }
        serviceContext.put("workEffortName", workEffortName);
        serviceContext.put("userLogin", userLogin);
        Map resultService = null;
        try {
            resultService = dispatcher.runSync("createProductionRunsForProductBom", serviceContext);
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunNotCreated", locale) + ": " + e.getMessage());
        }
        if (ServiceUtil.isError(resultService)) {
            return ServiceUtil.returnError(ServiceUtil.getErrorMessage(resultService));
        }

        String productionRunId = (String)resultService.get("productionRunId");
        result.put("productionRunId", productionRunId);

        result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(resource, "ManufacturingProductionRunCreated",UtilMisc.toMap("productionRunId", productionRunId), locale));
        return result;
    }

    public static Map createProductionRunFromConfiguration(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        // Mandatory input fields
        String facilityId = (String)context.get("facilityId");
        // Optional input fields
        String configId = (String)context.get("configId");
        ProductConfigWrapper config = (ProductConfigWrapper)context.get("config");
        BigDecimal quantity = (BigDecimal)context.get("quantity");
        String orderId = (String)context.get("orderId");
        String orderItemSeqId = (String)context.get("orderItemSeqId");

        if (config == null && configId == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingConfigurationNotAvailable", locale));
        }
        if (config == null && configId != null) {
            // TODO: load the configuration
            return ServiceUtil.returnError("Operation not yet implemented");
        }
        if (!config.isCompleted()) {
            return ServiceUtil.returnError("ProductConfigurationNotValid");
        }
        if (quantity == null) {
            quantity = BigDecimal.ONE;
        }
        String instanceProductId = null;
        try {
            instanceProductId = ProductWorker.getAggregatedInstanceId(delegator, config.getProduct().getString("productId"), config.getConfigId());
        } catch (Exception e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        Map serviceContext = new HashMap();
        serviceContext.clear();
        serviceContext.put("productId", instanceProductId);
        serviceContext.put("pRQuantity", quantity);
        serviceContext.put("startDate", UtilDateTime.nowTimestamp());
        serviceContext.put("facilityId", facilityId);
        //serviceContext.put("workEffortName", "");
        serviceContext.put("userLogin", userLogin);
        Map resultService = null;
        try {
            resultService = dispatcher.runSync("createProductionRun", serviceContext);
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunNotCreated", locale));
        }
        String productionRunId = (String)resultService.get("productionRunId");
        result.put("productionRunId", productionRunId);

        Iterator options = config.getSelectedOptions().iterator();
        HashMap components = new HashMap();
        while (options.hasNext()) {
            ConfigOption co = (ConfigOption)options.next();
            //components.addAll(co.getComponents());
            Iterator selComponents = co.getComponents().iterator();
            while (selComponents.hasNext()) {
                BigDecimal componentQuantity = null;
                GenericValue selComponent = (GenericValue)selComponents.next();
                if (selComponent.get("quantity") != null) {
                    componentQuantity = selComponent.getBigDecimal("quantity");
                }
                if (componentQuantity == null) {
                    componentQuantity = BigDecimal.ONE;
                }
                String componentProductId = selComponent.getString("productId");
                if (co.isVirtualComponent(selComponent)) {
                    Map componentOptions = co.getComponentOptions();
                    if (UtilValidate.isNotEmpty(componentOptions) && UtilValidate.isNotEmpty(componentOptions.get(componentProductId))) {
                        componentProductId = (String)componentOptions.get(componentProductId);
                    }
                }
                componentQuantity = quantity.multiply(componentQuantity);
                if (components.containsKey(componentProductId)) {
                    BigDecimal totalQuantity = (BigDecimal)components.get(componentProductId);
                    componentQuantity = totalQuantity.add(componentQuantity);
                }
                
                // check if a bom exists
                List bomList = null;
                try {
                    bomList = delegator.findByAnd("ProductAssoc", UtilMisc.toMap("productId", componentProductId, "productAssocTypeId", "MANUF_COMPONENT"));
                    bomList = EntityUtil.filterByDate(bomList, UtilDateTime.nowTimestamp());
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError("try to get BOM list from productAssoc");
                }
                // if so create a mandatory predecessor to this production run
                if (UtilValidate.isNotEmpty(bomList)) {
                    serviceContext.clear();
                    serviceContext.put("productId", componentProductId);
                    serviceContext.put("quantity", componentQuantity);
                    serviceContext.put("startDate", UtilDateTime.nowTimestamp());
                    serviceContext.put("facilityId", facilityId);
                    serviceContext.put("userLogin", userLogin);
                    resultService = null;
                    try {
                        resultService = dispatcher.runSync("createProductionRunsForProductBom", serviceContext);
                        GenericValue workEffortPreDecessor = delegator.makeValue("WorkEffortAssoc", UtilMisc.toMap(
                                "workEffortIdTo", productionRunId, "workEffortIdFrom", resultService.get("productionRunId"),
                                "workEffortAssocTypeId", "WORK_EFF_PRECEDENCY", "fromDate", UtilDateTime.nowTimestamp()));
                        workEffortPreDecessor.create();
                    } catch (GenericServiceException e) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunNotCreated", locale));
                    } catch (GenericEntityException e) {
                        return ServiceUtil.returnError("try to create workeffort assoc");
                    }
                    
                } else {
                    components.put(componentProductId, componentQuantity);
                }

                //  create production run notes from comments
                String comments = co.getComments();
                if (UtilValidate.isNotEmpty(comments)) {
                    resultService.clear();
                    serviceContext.clear();
                    serviceContext.put("workEffortId", productionRunId);
                    serviceContext.put("internalNote", "Y");
                    serviceContext.put("noteInfo", comments);
                    serviceContext.put("noteName", co.getDescription());
                    serviceContext.put("userLogin", userLogin);
                    serviceContext.put("noteParty", userLogin.getString("partyId"));
                    try {
                        resultService = dispatcher.runSync("createWorkEffortNote", serviceContext);
                    } catch (GenericServiceException e) {
                        Debug.logWarning(e.getMessage(), module);
                        return ServiceUtil.returnError(e.getMessage());
                    }
                }
            }
        }

        Iterator componentsIt = components.entrySet().iterator();
        while (componentsIt.hasNext()) {
            Map.Entry component = (Map.Entry)componentsIt.next();
            String productId = (String)component.getKey();
            BigDecimal componentQuantity = (BigDecimal)component.getValue();
            if (componentQuantity == null) {
                componentQuantity = BigDecimal.ONE;
            }
            resultService = null;
            serviceContext = new HashMap();
            serviceContext.put("productionRunId", productionRunId);
            serviceContext.put("productId", productId);
            serviceContext.put("estimatedQuantity", componentQuantity);
            serviceContext.put("userLogin", userLogin);
            try {
                resultService = dispatcher.runSync("addProductionRunComponent", serviceContext);
            } catch (GenericServiceException e) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunNotCreated", locale));
            }
        }
        try {
            if (productionRunId != null && orderId != null && orderItemSeqId != null) {
                delegator.create("WorkOrderItemFulfillment", UtilMisc.toMap("workEffortId", productionRunId, "orderId", orderId, "orderItemSeqId", orderItemSeqId));
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingRequirementNotDeleted", locale));
        }

        result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(resource, "ManufacturingProductionRunCreated",UtilMisc.toMap("productionRunId", productionRunId), locale));
        return result;
    }

    public static Map createProductionRunForMktgPkg(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        // Mandatory input fields
        String facilityId = (String)context.get("facilityId");
        String orderId = (String)context.get("orderId");
        String orderItemSeqId = (String)context.get("orderItemSeqId");

        // Check if the order is to be immediately fulfilled, in which case the inventory
        // hasn't been reserved and ATP not yet decreased
        boolean isImmediatelyFulfilled = false;
        try {
            GenericValue order = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
            GenericValue productStore = delegator.getRelatedOne("ProductStore", order);
            isImmediatelyFulfilled = "Y".equals(productStore.getString("isImmediatelyFulfilled"));
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Error creating a production run for marketing package for order [" + orderId + " " + orderItemSeqId + "]: " + e.getMessage());
        }

        GenericValue orderItem = null;
        try {
            orderItem = delegator.findByPrimaryKey("OrderItem", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId));
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Error creating a production run for marketing package for order [" + orderId + " " + orderItemSeqId + "]: " + e.getMessage());
        }
        if (orderItem == null) {
            return ServiceUtil.returnError("Error creating a production run for marketing package for order [" + orderId + " " + orderItemSeqId + "]: order item not found.");
        }
        if (orderItem.get("quantity") == null) {
            Debug.logWarning("No quantity found for orderItem [" + orderItem +"], skipping production run of this marketing package", module);
            return ServiceUtil.returnSuccess();
        }

        try {
            // first figure out how much of this product we already have in stock (ATP)
            BigDecimal existingAtp = BigDecimal.ZERO;
            Map tmpResults = dispatcher.runSync("getInventoryAvailableByFacility", UtilMisc.<String, Object>toMap("productId", orderItem.getString("productId"), "facilityId", facilityId, "userLogin", userLogin));
            if (tmpResults.get("availableToPromiseTotal") != null) {
                existingAtp = (BigDecimal) tmpResults.get("availableToPromiseTotal");
            }
            // if the order is immediately fulfilled, adjust the atp to compensate for it not reserved
            if (isImmediatelyFulfilled) {
                existingAtp = existingAtp.subtract(orderItem.getBigDecimal("quantity"));
            }

            if (Debug.verboseOn()) { Debug.logVerbose("Order item [" + orderItem + "] Existing ATP = [" + existingAtp + "]", module); }
            // we only need to produce more marketing packages if there isn't enough in stock.
            if (existingAtp.compareTo(ZERO) < 0) {
                // how many should we produce?  If there already is some inventory, then just produce enough to bring ATP back up to zero.
                BigDecimal qtyRequired = BigDecimal.ZERO.subtract(existingAtp);
                // ok so that's how many we WANT to produce, but let's check how many we can actually produce based on the available components
                Map serviceContext = new HashMap();
                serviceContext.put("productId", orderItem.getString("productId"));
                serviceContext.put("facilityId", facilityId);
                serviceContext.put("userLogin", userLogin);
                Map resultService = dispatcher.runSync("getMktgPackagesAvailable", serviceContext);
                BigDecimal mktgPackagesAvailable = (BigDecimal) resultService.get("availableToPromiseTotal");

                BigDecimal qtyToProduce = qtyRequired.min(mktgPackagesAvailable);

                if (qtyToProduce.compareTo(ZERO) > 0) {
                    if (Debug.verboseOn()) { Debug.logVerbose("Required quantity (all orders) = [" + qtyRequired + "] quantity to produce = [" + qtyToProduce + "]", module); }

                    serviceContext.put("pRQuantity", qtyToProduce);
                    serviceContext.put("startDate", UtilDateTime.nowTimestamp());
                    //serviceContext.put("workEffortName", "");

                    resultService = dispatcher.runSync("createProductionRun", serviceContext);

                    String productionRunId = (String)resultService.get("productionRunId");
                    result.put("productionRunId", productionRunId);
                    
                    try {
                        delegator.create("WorkOrderItemFulfillment", UtilMisc.toMap("workEffortId", productionRunId, "orderId", orderId, "orderItemSeqId", orderItemSeqId));
                    } catch (GenericEntityException e) {
                       return ServiceUtil.returnError("Error creating a production run for marketing package for order [" + orderId + " " + orderItemSeqId + "]: " + e.getMessage());
                   }

                    try {
                        serviceContext.clear();
                        serviceContext.put("productionRunId", productionRunId);
                        serviceContext.put("statusId", "PRUN_COMPLETED");
                        serviceContext.put("userLogin", userLogin);
                        resultService = dispatcher.runSync("quickChangeProductionRunStatus", serviceContext);
                        serviceContext.clear();
                        serviceContext.put("workEffortId", productionRunId);
                        serviceContext.put("userLogin", userLogin);
                        resultService = dispatcher.runSync("productionRunProduce", serviceContext);
                    } catch (GenericServiceException e) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunNotCreated", locale));
                    }

                    result.put(ModelService.SUCCESS_MESSAGE, UtilProperties.getMessage(resource, "ManufacturingProductionRunCreated", UtilMisc.toMap("productionRunId", productionRunId), locale));
                    return result;
                } else {
                    if (Debug.verboseOn()) { Debug.logVerbose("There are not enough components available to produce any marketing packages [" + orderItem.getString("productId") + "]", module); }
                    return ServiceUtil.returnSuccess();
                }
            } else {
                if (Debug.verboseOn()) { Debug.logVerbose("No marketing packages need to be produced - ATP is [" + existingAtp + "]", module); }
                return ServiceUtil.returnSuccess();
            }
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunNotCreated", locale));
        }
    }

    public static Map createProductionRunsForOrder(DispatchContext dctx, Map context) {

        Map result = new HashMap();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue)context.get("userLogin");

        String orderId = (String) context.get("orderId");

        String shipmentId = (String) context.get("shipmentId");
        String orderItemSeqId = (String) context.get("orderItemSeqId");
        BigDecimal quantity = (BigDecimal) context.get("quantity");
        String fromDateStr = (String) context.get("fromDate");

        BigDecimal amount = null;
        Date fromDate = null;
        if (UtilValidate.isNotEmpty(fromDateStr)) {
            try {
                fromDate = Timestamp.valueOf(fromDateStr);
            } catch (Exception e) {
            }
        }
        if (fromDate == null) {
            fromDate = new Date();
        }

        List orderItems = null;

        if (orderItemSeqId != null) {
            try {
                GenericValue orderItem = delegator.findByPrimaryKey("OrderItem", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId));
                if (orderItem == null) {
                    return ServiceUtil.returnError("OrderItem [" + orderItemSeqId + "] not found.");
                }
                if (quantity != null) {
                    orderItem.set("quantity", quantity);
                }
                orderItems = UtilMisc.toList(orderItem);
            } catch (GenericEntityException gee) {
                return ServiceUtil.returnError("Error reading the OrderItem: " + gee.getMessage());
            }
        } else {
            try {
                orderItems = delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId));
                if (orderItems == null) {
                    return ServiceUtil.returnError("OrderItems for order [" + orderId + "] not found.");
                }
            } catch (GenericEntityException gee) {
                return ServiceUtil.returnError("Error reading the OrderItems: " + gee.getMessage());
            }
        }
        for (int i = 0; i < orderItems.size(); i++) {
            GenericValue orderItem = (GenericValue)orderItems.get(i);
            if (orderItem.get("productId") == null) {
                continue;
            }
            if (orderItem.get("quantity") != null) {
                quantity = orderItem.getBigDecimal("quantity");
            } else {
                continue;
            }
            try {
                List existingProductionRuns = delegator.findByAndCache("WorkOrderItemFulfillment", UtilMisc.toMap("orderId", orderItem.getString("orderId"), "orderItemSeqId", orderItem.getString("orderItemSeqId")));
                if (UtilValidate.isNotEmpty(existingProductionRuns)) {
                    Debug.logWarning("Production Run for order item [" + orderItem.getString("orderId") + "/" + orderItem.getString("orderItemSeqId") + "] already exists.", module);
                    continue;
                }
            } catch (GenericEntityException gee) {
                return ServiceUtil.returnError("Error reading the WorkOrderItemFulfillment: " + gee.getMessage());
            }
            if (orderItem.get("selectedAmount") != null) {
                amount = orderItem.getBigDecimal("selectedAmount");
            }
            if (amount == null) {
                amount = BigDecimal.ZERO;
            }
            try {
                ArrayList components = new ArrayList();
                BOMTree tree = new BOMTree(orderItem.getString("productId"), "MANUF_COMPONENT", fromDate, BOMTree.EXPLOSION_MANUFACTURING, delegator, dispatcher, userLogin);
                tree.setRootQuantity(quantity);
                tree.setRootAmount(amount);
                tree.print(components);
                tree.createManufacturingOrders(null, fromDate, null, null, null, orderId, orderItem.getString("orderItemSeqId"), shipmentId, userLogin);
            } catch (GenericEntityException gee) {
                return ServiceUtil.returnError("Error creating bill of materials tree: " + gee.getMessage());
            }
        }
        ArrayList productionRuns = new ArrayList();
        result.put("productionRuns" , productionRuns);
        return result;
    }

    public static Map createProductionRunsForProductBom(DispatchContext dctx, Map context) {
        Map result = new HashMap();
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin =(GenericValue)context.get("userLogin");

        String productId = (String)context.get("productId");
        Timestamp startDate = (Timestamp)context.get("startDate");
        BigDecimal quantity = (BigDecimal)context.get("quantity");
        String facilityId = (String)context.get("facilityId");
        String workEffortName = (String)context.get("workEffortName");
        String description = (String)context.get("description");
        String routingId = (String)context.get("routingId");
        String workEffortId = null;
        if (quantity == null) {
            quantity = BigDecimal.ONE;
        }
        try {
            ArrayList components = new ArrayList();
            BOMTree tree = new BOMTree(productId, "MANUF_COMPONENT", startDate, BOMTree.EXPLOSION_MANUFACTURING, delegator, dispatcher, userLogin);
            tree.setRootQuantity(quantity);
            tree.setRootAmount(BigDecimal.ZERO);
            tree.print(components);
            workEffortId = tree.createManufacturingOrders(facilityId, startDate, workEffortName, description, routingId, null, null, null, userLogin);
        } catch (GenericEntityException gee) {
            return ServiceUtil.returnError("Error creating bill of materials tree: " + gee.getMessage());
        }
        if (workEffortId == null) {
            return ServiceUtil.returnError("No production run is required for product with id [" + productId +"] in date [" + startDate + "]; please verify the validity dates of the bill of materials and routing.");
        }
        ArrayList productionRuns = new ArrayList();
        result.put("productionRuns" , productionRuns);
        result.put("productionRunId" , workEffortId);
        return result;
    }

    /**
     * Quick runs a ProductionRun task to the completed status, also issuing components
     * if necessary.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map quickRunProductionRunTask(DispatchContext ctx, Map context) {
        Map result = ServiceUtil.returnSuccess();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String productionRunId = (String) context.get("productionRunId");
        String taskId = (String) context.get("taskId");

        try {
            Map serviceContext = new HashMap();
            Map resultService = null;
            GenericValue task = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", taskId));
            String currentStatusId = task.getString("currentStatusId");
            String prevStatusId = "";
            while (!"PRUN_COMPLETED".equals(currentStatusId)) {
                serviceContext.put("productionRunId", productionRunId);
                serviceContext.put("workEffortId", taskId);
                serviceContext.put("issueAllComponents", Boolean.TRUE);
                serviceContext.put("userLogin", userLogin);
                resultService = dispatcher.runSync("changeProductionRunTaskStatus", serviceContext);
                currentStatusId = (String)resultService.get("newStatusId");
                if (currentStatusId.equals(prevStatusId)) {
                    return ServiceUtil.returnError("Unable to progress from status [" + prevStatusId + "] for task [" + taskId + "]");
                } else {
                    prevStatusId = currentStatusId;
                }
                serviceContext.clear();
            }
        } catch (Exception e) {
            Debug.logError(e, "Problem calling the changeProductionRunTaskStatus service", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusNotChanged", locale));
        }
        return result;
    }

    /**
     * Quick runs all the tasks of a ProductionRun to the completed status,
     * also issuing components if necessary.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map quickRunAllProductionRunTasks(DispatchContext ctx, Map context) {
        Map result = ServiceUtil.returnSuccess();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String productionRunId = (String) context.get("productionRunId");

        ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
        if (!productionRun.exist()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunNotExists", locale));
        }
        List tasks = productionRun.getProductionRunRoutingTasks();
        GenericValue oneTask = null;
        String taskId = null;
        for (int i = 0; i < tasks.size(); i++) {
            oneTask = (GenericValue)tasks.get(i);
            taskId = oneTask.getString("workEffortId");
            try {
                Map serviceContext = null;
                Map resultService = null;
                serviceContext = new HashMap();
                serviceContext.put("productionRunId", productionRunId);
                serviceContext.put("taskId", taskId);
                serviceContext.put("userLogin", userLogin);
                resultService = dispatcher.runSync("quickRunProductionRunTask", serviceContext);
            } catch (GenericServiceException e) {
                Debug.logError(e, "Problem calling the quickRunProductionRunTask service", module);
                return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusNotChanged", locale));
            }
        }
        return result;
    }

    public static Map quickStartAllProductionRunTasks(DispatchContext ctx, Map context) {
        Map result = ServiceUtil.returnSuccess();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String productionRunId = (String) context.get("productionRunId");

        ProductionRun productionRun = new ProductionRun(productionRunId, delegator, dispatcher);
        if (!productionRun.exist()) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunNotExists", locale));
        }
        List tasks = productionRun.getProductionRunRoutingTasks();
        GenericValue oneTask = null;
        String taskId = null;
        for (int i = 0; i < tasks.size(); i++) {
            oneTask = (GenericValue)tasks.get(i);
            taskId = oneTask.getString("workEffortId");
            if ("PRUN_CREATED".equals(oneTask.getString("currentStatusId"))) {
                try {
                    Map serviceContext = UtilMisc.toMap("productionRunId", productionRunId, "workEffortId", taskId);
                    serviceContext.put("statusId", "PRUN_RUNNING");
                    serviceContext.put("issueAllComponents", Boolean.FALSE);
                    serviceContext.put("userLogin", userLogin);
                    Map resultService = dispatcher.runSync("changeProductionRunTaskStatus", serviceContext);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problem calling the changeProductionRunTaskStatus service", module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusNotChanged", locale));
                }
            }
        }
        return result;
    }

    /**
     * Quick moves a ProductionRun to the passed in status, performing all
     * the needed tasks in the way.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map quickChangeProductionRunStatus(DispatchContext ctx, Map context) {
        Map result = ServiceUtil.returnSuccess();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String productionRunId = (String) context.get("productionRunId");
        String statusId = (String) context.get("statusId");
        String startAllTasks = (String) context.get("startAllTasks");

        try {
            Map serviceContext = null;
            Map resultService = null;
            // Change the task status to running
            serviceContext = new HashMap();
            if (statusId.equals("PRUN_DOC_PRINTED") ||
                    statusId.equals("PRUN_RUNNING") ||
                    statusId.equals("PRUN_COMPLETED") ||
                    statusId.equals("PRUN_CLOSED")) {
                serviceContext.put("productionRunId", productionRunId);
                serviceContext.put("statusId", "PRUN_DOC_PRINTED");
                serviceContext.put("userLogin", userLogin);
                resultService = dispatcher.runSync("changeProductionRunStatus", serviceContext);
            }
            if (statusId.equals("PRUN_RUNNING") && "Y".equals(startAllTasks)) {
                serviceContext.clear();
                serviceContext.put("productionRunId", productionRunId);
                serviceContext.put("userLogin", userLogin);
                resultService = dispatcher.runSync("quickStartAllProductionRunTasks", serviceContext);
            }
            if (statusId.equals("PRUN_COMPLETED") ||
                       statusId.equals("PRUN_CLOSED")) {
                serviceContext.clear();
                serviceContext.put("productionRunId", productionRunId);
                serviceContext.put("userLogin", userLogin);
                resultService = dispatcher.runSync("quickRunAllProductionRunTasks", serviceContext);
            }
            if (statusId.equals("PRUN_CLOSED")) {
                // Put in warehouse the products manufactured
                serviceContext.clear();
                serviceContext.put("workEffortId", productionRunId);
                serviceContext.put("userLogin", userLogin);
                resultService = dispatcher.runSync("productionRunProduce", serviceContext);
                serviceContext.clear();
                serviceContext.put("productionRunId", productionRunId);
                serviceContext.put("statusId", "PRUN_CLOSED");
                serviceContext.put("userLogin", userLogin);
                resultService = dispatcher.runSync("changeProductionRunStatus", serviceContext);
            } else {
                serviceContext.put("productionRunId", productionRunId);
                serviceContext.put("statusId", statusId);
                serviceContext.put("userLogin", userLogin);
                resultService = dispatcher.runSync("changeProductionRunStatus", serviceContext);
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the changeProductionRunStatus service", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionRunStatusNotChanged", locale));
        }
        return result;
    }

    /**
     * Given a productId and an optional date, returns the total qty
     * of productId reserved by production runs.
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input parameters.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map getProductionRunTotResQty(DispatchContext ctx, Map context) {
        Map result = ServiceUtil.returnSuccess();
        Delegator delegator = ctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        String productId = (String) context.get("productId");
        Timestamp startDate = (Timestamp) context.get("startDate");
        if (startDate == null) {
            startDate = UtilDateTime.nowTimestamp();
        }
        BigDecimal totQty = BigDecimal.ZERO;
        try {
            List findOutgoingProductionRunsConds = new LinkedList();

            findOutgoingProductionRunsConds.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId));
            findOutgoingProductionRunsConds.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "WEGS_CREATED"));
            findOutgoingProductionRunsConds.add(EntityCondition.makeCondition("estimatedStartDate", EntityOperator.LESS_THAN_EQUAL_TO, startDate));

            List findOutgoingProductionRunsStatusConds = new LinkedList();
            findOutgoingProductionRunsStatusConds.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.EQUALS, "PRUN_CREATED"));
            findOutgoingProductionRunsStatusConds.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.EQUALS, "PRUN_SCHEDULED"));
            findOutgoingProductionRunsStatusConds.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.EQUALS, "PRUN_DOC_PRINTED"));
            findOutgoingProductionRunsStatusConds.add(EntityCondition.makeCondition("currentStatusId", EntityOperator.EQUALS, "PRUN_RUNNING"));
            findOutgoingProductionRunsConds.add(EntityCondition.makeCondition(findOutgoingProductionRunsStatusConds, EntityOperator.OR));

            List outgoingProductionRuns = delegator.findList("WorkEffortAndGoods", EntityCondition.makeCondition(findOutgoingProductionRunsConds, EntityOperator.AND), null, UtilMisc.toList("-estimatedStartDate"), null, false);
            if (outgoingProductionRuns != null) {
                for (int i = 0; i < outgoingProductionRuns.size(); i++) {
                    GenericValue outgoingProductionRun = (GenericValue)outgoingProductionRuns.get(i);
                    BigDecimal qty = outgoingProductionRun.getBigDecimal("estimatedQuantity");
                    qty = qty != null ? qty : BigDecimal.ZERO;
                    totQty = totQty.add(qty);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem calling the getProductionRunTotResQty service", module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource, "ManufacturingProductionResQtyCalc", locale));
        }
        result.put("reservedQuantity", totQty);
        return result;
    }

    public static Map checkDecomposeInventoryItem(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String inventoryItemId = (String)context.get("inventoryItemId");
        /*
        BigDecimal quantity = (BigDecimal)context.get("quantityAccepted");
        if (quantity != null && quantity.BigDecimalValue() == 0) {
            return ServiceUtil.returnSuccess();
        }
         */
        try {
            GenericValue inventoryItem = delegator.findByPrimaryKey("InventoryItem", UtilMisc.toMap("inventoryItemId", inventoryItemId));
            if (inventoryItem == null) {
                return ServiceUtil.returnError("Error: inventory item with id [" + inventoryItemId + "] not found.");
            }
            if (inventoryItem.get("availableToPromiseTotal") != null && inventoryItem.getBigDecimal("availableToPromiseTotal").compareTo(ZERO) <= 0) {
                return ServiceUtil.returnSuccess();
            }
            GenericValue product = inventoryItem.getRelatedOne("Product");
            if (product == null) {
                return ServiceUtil.returnError("Error: product with id [" + inventoryItem.get("productId") + "] not found.");
            }
            if (CommonWorkers.hasParentType(delegator, "ProductType", "productTypeId", product.getString("productTypeId"), "parentTypeId", "MARKETING_PKG_AUTO")) {
                Map serviceContext = UtilMisc.toMap("inventoryItemId", inventoryItemId,
                                                    "userLogin", userLogin);
                /*
                if (quantity != null) {
                    serviceContext.put("quantity", quantity);
                }
                 */
                dispatcher.runSync("decomposeInventoryItem", serviceContext);
            }
        } catch (Exception e) {
            Debug.logError(e, "Problem calling the checkDecomposeInventoryItem service", module);
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map decomposeInventoryItem(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        Timestamp now = UtilDateTime.nowTimestamp();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        // Mandatory input fields
        String inventoryItemId = (String)context.get("inventoryItemId");
        BigDecimal quantity = (BigDecimal)context.get("quantity");
        List inventoryItemIds = new ArrayList();
        try {
            GenericValue inventoryItem = delegator.findByPrimaryKey("InventoryItem", UtilMisc.toMap("inventoryItemId", inventoryItemId));
            if (inventoryItem == null) {
                return ServiceUtil.returnError("Error decomposing inventory item: inventory item with id [" + inventoryItemId + "] not found.");
            }
            // the work effort (disassemble order) is created
            Map serviceContext = UtilMisc.toMap("workEffortTypeId", "TASK",
                                 "workEffortPurposeTypeId", "WEPT_PRODUCTION_RUN",
                                 "currentStatusId", "CAL_COMPLETED");
            serviceContext.put("workEffortName", "Decomposing product [" + inventoryItem.getString("productId") + "] inventory item [" + inventoryItem.getString("inventoryItemId") + "]");
            serviceContext.put("facilityId", inventoryItem.getString("facilityId"));
            serviceContext.put("estimatedStartDate", now);
            serviceContext.put("userLogin", userLogin);
            Map resultService = dispatcher.runSync("createWorkEffort", serviceContext);
            String workEffortId = (String)resultService.get("workEffortId");
            // the inventory (marketing package) is issued
            serviceContext.clear();
            serviceContext = UtilMisc.toMap("inventoryItem", inventoryItem,
                                 "workEffortId", workEffortId,
                                 "userLogin", userLogin);
            if (quantity != null) {
                serviceContext.put("quantity", quantity);
            }
            resultService = dispatcher.runSync("issueInventoryItemToWorkEffort", serviceContext);
            BigDecimal issuedQuantity = (BigDecimal)resultService.get("quantityIssued");
            if (issuedQuantity.compareTo(ZERO) == 0) {
                return ServiceUtil.returnError("Error decomposing inventory item: no marketing packages found in inventory item [" + inventoryItem.getString("inventoryItemId") + "].");
            }
            // get the package's unit cost to compute a cost coefficient ratio which is the marketing package's actual unit cost divided by its standard cost
            // this ratio will be used to determine the cost of the marketing package components when they are returned to inventory
            serviceContext.clear();
            serviceContext = UtilMisc.toMap("productId", inventoryItem.getString("productId"),
                                 "currencyUomId", inventoryItem.getString("currencyUomId"),
                                 "costComponentTypePrefix", "EST_STD",
                                 "userLogin", userLogin);
            resultService = dispatcher.runSync("getProductCost", serviceContext);
            BigDecimal packageCost = (BigDecimal)resultService.get("productCost");
            BigDecimal inventoryItemCost = inventoryItem.getBigDecimal("unitCost");
            BigDecimal costCoefficient = null;
            if (packageCost == null || packageCost.compareTo(ZERO) == 0 || inventoryItemCost == null) {
                // if the actual cost of the item (marketing package) that we are decomposing is not available, or
                // if the standard cost of the marketing package is not available then
                // the cost coefficient ratio is set to 1.0:
                // this means that the unit costs of the inventory items of the components
                // will be equal to the components' standard costs
                costCoefficient = BigDecimal.ONE;
            } else {
                costCoefficient = inventoryItemCost.divide(packageCost, 10, rounding);
            }

            // the components are retrieved
            serviceContext.clear();
            serviceContext = UtilMisc.toMap("productId", inventoryItem.getString("productId"),
                                 "quantity", issuedQuantity,
                                 "userLogin", userLogin);
            resultService = dispatcher.runSync("getManufacturingComponents", serviceContext);
            List components = (List)resultService.get("componentsMap");
            if (components == null || components.isEmpty()) {
                return ServiceUtil.returnError("Error decomposing inventory item: no components found for marketing package [" + inventoryItem.getString("productId") + "].");
            }
            Iterator componentsIt = components.iterator();
            while (componentsIt.hasNext()) {
                Map component = (Map)componentsIt.next();
                // get the component's standard cost
                serviceContext.clear();
                serviceContext = UtilMisc.toMap("productId", ((GenericValue)component.get("product")).getString("productId"),
                                     "currencyUomId", inventoryItem.getString("currencyUomId"),
                                     "costComponentTypePrefix", "EST_STD",
                                     "userLogin", userLogin);
                resultService = dispatcher.runSync("getProductCost", serviceContext);
                BigDecimal componentCost = (BigDecimal)resultService.get("productCost");

                // return the component to inventory at its standard cost multiplied by the cost coefficient from above
                BigDecimal componentInventoryItemCost = costCoefficient.multiply(componentCost);
                serviceContext.clear();
                serviceContext = UtilMisc.toMap("productId", ((GenericValue)component.get("product")).getString("productId"),
                                     "quantity", component.get("quantity"),
                                     "facilityId", inventoryItem.getString("facilityId"),
                                     "unitCost", componentInventoryItemCost,
                                     "userLogin", userLogin);
                serviceContext.put("workEffortId", workEffortId);
                resultService = dispatcher.runSync("productionRunTaskProduce", serviceContext);
                List newInventoryItemIds = (List)resultService.get("inventoryItemIds");
                inventoryItemIds.addAll(newInventoryItemIds);
            }
            // the components are put in warehouse
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem calling the createWorkEffort service", module);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            Debug.logError(e, "Problem calling the createWorkEffort service", module);
            return ServiceUtil.returnError(e.getMessage());
        }
        result.put("inventoryItemIds", inventoryItemIds);
        return result;
    }

    public static Map setEstimatedDeliveryDates(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        Delegator delegator = ctx.getDelegator();
        Timestamp now = UtilDateTime.nowTimestamp();

        Map products = FastMap.newInstance();

        try {
            List resultList = delegator.findByAnd("WorkEffortAndGoods", UtilMisc.toMap("workEffortGoodStdTypeId", "PRUN_PROD_DELIV",
                                                                                  "statusId", "WEGS_CREATED",
                                                                                  "workEffortTypeId", "PROD_ORDER_HEADER"));
            Iterator iteratorResult = resultList.iterator();
            while (iteratorResult.hasNext()) {
                GenericValue genericResult = (GenericValue) iteratorResult.next();
                if ("PRUN_CLOSED".equals(genericResult.getString("currentStatusId")) ||
                    "PRUN_CREATED".equals(genericResult.getString("currentStatusId"))) {
                    continue;
                }
                BigDecimal qtyToProduce = genericResult.getBigDecimal("quantityToProduce");
                if (qtyToProduce == null) {
                    qtyToProduce = BigDecimal.ZERO;
                }
                BigDecimal qtyProduced = genericResult.getBigDecimal("quantityProduced");
                if (qtyProduced == null) {
                    qtyProduced = BigDecimal.ZERO;
                }
                if (qtyProduced.compareTo(qtyToProduce) >= 0) {
                    continue;
                }
                BigDecimal qtyDiff = qtyToProduce.subtract(qtyProduced);
                String productId =  genericResult.getString("productId");
                Timestamp estimatedShipDate = genericResult.getTimestamp("estimatedCompletionDate");
                if (estimatedShipDate == null) {
                    estimatedShipDate = now;
                }
                if (!products.containsKey(productId)) {
                    products.put(productId, new TreeMap());
                }
                TreeMap productMap = (TreeMap)products.get(productId);
                if (!productMap.containsKey(estimatedShipDate)) {
                    productMap.put(estimatedShipDate, UtilMisc.toMap("remainingQty", BigDecimal.ZERO, "reservations", FastList.newInstance()));
                }
                Map dateMap = (Map)productMap.get(estimatedShipDate);
                BigDecimal remainingQty = (BigDecimal)dateMap.get("remainingQty");
                //List reservations = (List)dateMap.get("reservations");
                remainingQty = remainingQty.add(qtyDiff);
                dateMap.put("remainingQty", remainingQty);
            }

            // Approved purchase orders
            resultList = delegator.findByAnd("OrderHeaderAndItems", UtilMisc.toMap("orderTypeId", "PURCHASE_ORDER",
                                                                                   "itemStatusId", "ITEM_APPROVED"), UtilMisc.toList("orderId"));
            iteratorResult = resultList.iterator();
            String orderId = null;
            GenericValue orderDeliverySchedule = null;
            while (iteratorResult.hasNext()) {
                GenericValue genericResult = (GenericValue) iteratorResult.next();
                String newOrderId =  genericResult.getString("orderId");
                if (!newOrderId.equals(orderId)) {
                    orderDeliverySchedule = null;
                    orderId = newOrderId;
                    try {
                        orderDeliverySchedule = delegator.findByPrimaryKey("OrderDeliverySchedule", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", "_NA_"));
                    } catch (GenericEntityException e) {
                    }
                }
                String productId =  genericResult.getString("productId");
                BigDecimal orderQuantity = genericResult.getBigDecimal("quantity");
                GenericValue orderItemDeliverySchedule = null;
                try {
                    orderItemDeliverySchedule = delegator.findByPrimaryKey("OrderDeliverySchedule", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", genericResult.getString("orderItemSeqId")));
                } catch (GenericEntityException e) {
                }
                Timestamp estimatedShipDate = null;
                if (orderItemDeliverySchedule != null && orderItemDeliverySchedule.get("estimatedReadyDate") != null) {
                    estimatedShipDate = orderItemDeliverySchedule.getTimestamp("estimatedReadyDate");
                } else if (orderDeliverySchedule != null && orderDeliverySchedule.get("estimatedReadyDate") != null) {
                    estimatedShipDate = orderDeliverySchedule.getTimestamp("estimatedReadyDate");
                } else {
                    estimatedShipDate = genericResult.getTimestamp("estimatedDeliveryDate");
                }
                if (estimatedShipDate == null) {
                    estimatedShipDate = now;
                }
                if (!products.containsKey(productId)) {
                    products.put(productId, new TreeMap());
                }
                TreeMap productMap = (TreeMap)products.get(productId);
                if (!productMap.containsKey(estimatedShipDate)) {
                    productMap.put(estimatedShipDate, UtilMisc.toMap("remainingQty", BigDecimal.ZERO, "reservations", FastList.newInstance()));
                }
                Map dateMap = (Map)productMap.get(estimatedShipDate);
                BigDecimal remainingQty = (BigDecimal)dateMap.get("remainingQty");
                //List reservations = (List)dateMap.get("reservations");
                remainingQty = remainingQty.add(orderQuantity);
                dateMap.put("remainingQty", remainingQty);
            }

            // backorders
            List backordersCondList = FastList.newInstance();
            backordersCondList.add(EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.NOT_EQUAL, null));
            backordersCondList.add(EntityCondition.makeCondition("quantityNotAvailable", EntityOperator.GREATER_THAN, BigDecimal.ZERO));
            //backordersCondList.add(EntityCondition.makeCondition(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ITEM_CREATED"), EntityOperator.OR, EntityCondition.makeCondition("statusId", EntityOperator.LESS_THAN, "ITEM_APPROVED")));

            List backorders = delegator.findList("OrderItemAndShipGrpInvResAndItem", EntityCondition.makeCondition(backordersCondList, EntityOperator.AND), null, UtilMisc.toList("shipBeforeDate"), null, false);
            Iterator backordersIt = backorders.iterator();
            while (backordersIt.hasNext()) {
                GenericValue genericResult = (GenericValue) backordersIt.next();
                String productId = genericResult.getString("productId");
                GenericValue orderItemShipGroup = delegator.findByPrimaryKey("OrderItemShipGroup", UtilMisc.toMap("orderId", genericResult.get("orderId"),
                                                                                                                  "shipGroupSeqId", genericResult.get("shipGroupSeqId")));
                Timestamp requiredByDate = orderItemShipGroup.getTimestamp("shipByDate");

                BigDecimal quantityNotAvailable = genericResult.getBigDecimal("quantityNotAvailable");
                BigDecimal quantityNotAvailableRem = quantityNotAvailable;
                if (requiredByDate == null) {
                    // If shipByDate is not set, 'now' is assumed.
                    requiredByDate = now;
                }
                if (!products.containsKey(productId)) {
                    continue;
                }
                TreeMap productMap = (TreeMap)products.get(productId);
                SortedMap subsetMap = productMap.headMap(requiredByDate);
                // iterate and 'reserve'
                Iterator subsetMapKeysIt = subsetMap.keySet().iterator();
                while (subsetMapKeysIt.hasNext()) {
                    Timestamp currentDate = (Timestamp)subsetMapKeysIt.next();
                    Map currentDateMap = (Map) subsetMap.get(currentDate);
                    //List reservations = (List)currentDateMap.get("reservations");
                    BigDecimal remainingQty = (BigDecimal)currentDateMap.get("remainingQty");
                    if (remainingQty.compareTo(ZERO) == 0) {
                        continue;
                    }
                    if (remainingQty.compareTo(quantityNotAvailableRem) >= 0) {
                        remainingQty = remainingQty.subtract(quantityNotAvailableRem);
                        currentDateMap.put("remainingQty", remainingQty);
                        GenericValue orderItemShipGrpInvRes = delegator.findByPrimaryKey("OrderItemShipGrpInvRes", UtilMisc.toMap("orderId", genericResult.getString("orderId"),
                                                                                                                                  "shipGroupSeqId", genericResult.getString("shipGroupSeqId"),
                                                                                                                                  "orderItemSeqId", genericResult.getString("orderItemSeqId"),
                                                                                                                                  "inventoryItemId", genericResult.getString("inventoryItemId")));
                        orderItemShipGrpInvRes.set("promisedDatetime", currentDate);
                        orderItemShipGrpInvRes.store();
                        // TODO: set the reservation
                        break;
                    } else {
                        quantityNotAvailableRem = quantityNotAvailableRem.subtract(remainingQty);
                        remainingQty = BigDecimal.ZERO;
                        currentDateMap.put("remainingQty", remainingQty);
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error", module);
            return ServiceUtil.returnError("Problem running the setEstimatedDeliveryDates service");
        }
        return ServiceUtil.returnSuccess();
    }
}
