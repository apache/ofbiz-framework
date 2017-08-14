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

package org.apache.ofbiz.manufacturing.jobshopmgt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.manufacturing.techdata.TechDataServices;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;


/**
 * ProductionRun Object used by the Jobshop management OFBiz components,
 * this object is used to find or updated an existing ProductionRun.
 *
 */
public class ProductionRun {

    public static final String module = ProductionRun.class.getName();
    public static final String resource = "ManufacturingUiLabels";

    protected GenericValue productionRun; // WorkEffort (PROD_ORDER_HEADER)
    protected GenericValue productionRunProduct; // WorkEffortGoodStandard (type: PRUN_PROD_DELIV)
    protected GenericValue productProduced; // Product (from WorkEffortGoodStandard of type: PRUN_PROD_DELIV)
    protected BigDecimal quantity; // the estimatedQuantity

    protected Timestamp estimatedStartDate;
    protected Timestamp estimatedCompletionDate;
    protected String productionRunName;
    protected String description;
    protected GenericValue currentStatus;
    protected List<GenericValue> productionRunComponents;
    protected List<GenericValue> productionRunRoutingTasks;
    protected LocalDispatcher dispatcher;

    /**
     * indicate if quantity or estimatedStartDate has been modified and
     *  estimatedCompletionDate not yet recalculated with recalculateEstimatedCompletionDate() method.
     */
    private boolean updateCompletionDate = false;
    /**
     * indicate if quantity  has been modified, used for store() method to update appropriate entity.
     */
    private boolean quantityIsUpdated = false;

    public ProductionRun(String productionRunId, Delegator delegator, LocalDispatcher dispatcher) {
        try {
            if (! UtilValidate.isEmpty(productionRunId)) {
                this.dispatcher = dispatcher;
                GenericValue workEffort = EntityQuery.use(delegator).from("WorkEffort").where("workEffortId", productionRunId).queryOne();
                if (workEffort != null) {
                    // If this is a task, get the parent production run
                    if (workEffort.getString("workEffortTypeId") != null && "PROD_ORDER_TASK".equals(workEffort.getString("workEffortTypeId"))) {
                        workEffort = EntityQuery.use(delegator).from("WorkEffort").where("workEffortId", workEffort.getString("workEffortParentId")).queryOne();
                    }
                }
                this.productionRun = workEffort;
                if (exist()) {
                    this.estimatedStartDate = productionRun.getTimestamp("estimatedStartDate");
                    this.estimatedCompletionDate = productionRun.getTimestamp("estimatedCompletionDate");
                    this.productionRunName = productionRun.getString("workEffortName");
                    this.description = productionRun.getString("description");
                }
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
        }
    }

    /**
     * test if the productionRun exist.
     * @return true if it exist false otherwise.
     **/
    public boolean exist() {
        return productionRun != null;
    }

    /**
     * get the ProductionRun GenericValue .
     * @return the ProductionRun GenericValue
     **/
    public GenericValue getGenericValue() {
        return productionRun;
    }
    /**
     * Store the modified ProductionRun object in the database.
     * <ul>
     *     <li>store the the productionRun header</li>
     *     <li> the productProduced related data</li>
     *     <li> the listRoutingTask related data</li>
     *     <li> the productComponent list related data</li>
     * </ul>
     * @return true if success false otherwise
     **/
    public boolean store() {
        if (exist()) {
            if (updateCompletionDate) {
                this.estimatedCompletionDate = recalculateEstimatedCompletionDate();
            }
            productionRun.set("estimatedStartDate",this.estimatedStartDate);
            productionRun.set("estimatedCompletionDate",this.estimatedCompletionDate);
            productionRun.set("workEffortName",this.productionRunName);
            productionRun.set("description",this.description);
            try {
                if (quantityIsUpdated) {
                    productionRun.set("quantityToProduce",(BigDecimal) this.quantity);
                    productionRunProduct.set("estimatedQuantity",this.quantity.doubleValue());
                    productionRunProduct.store();
                    quantityIsUpdated = false;
                }
                productionRun.store();
                if (productionRunRoutingTasks != null) {
                    for (Iterator<GenericValue> iter = productionRunRoutingTasks.iterator(); iter.hasNext();) {
                        GenericValue routingTask = iter.next();
                        routingTask.store();
                    }
                }
                if (productionRunComponents != null) {
                    for (Iterator<GenericValue> iter = productionRunComponents.iterator(); iter.hasNext();) {
                        GenericValue component = iter.next();
                        component.store();
                    }
                }
            } catch (GenericEntityException e) {
                Debug.logWarning(e.getMessage(), module);
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * get the Product GenericValue corresponding to the productProduced.
     *     In the same time this method read the quantity property from SGBD
     * @return the productProduced related object
     **/
    public GenericValue getProductProduced() {
        if (exist()) {
            if (productProduced == null) {
                try {
                    List<GenericValue> productionRunProducts = productionRun.getRelated("WorkEffortGoodStandard", UtilMisc.toMap("workEffortGoodStdTypeId", "PRUN_PROD_DELIV"), null, false);
                    this.productionRunProduct = EntityUtil.getFirst(productionRunProducts);
                    quantity = productionRunProduct.getBigDecimal("estimatedQuantity");
                    productProduced = productionRunProduct.getRelatedOne("Product", true);
                } catch (GenericEntityException e) {
                    Debug.logWarning(e.getMessage(), module);
                }
            }
            return productProduced;
        }
        return null;
    }

    /**
     * get the quantity property.
     * @return the quantity property
     **/
    public BigDecimal getQuantity() {
        if (exist()) {
            if (quantity == null)  getProductProduced();
            return quantity;
        }
        else return null;
    }
    /**
     * set the quantity property and recalculated the productComponent quantity.
     * @param newQuantity the new quantity to be set
     **/
    public void setQuantity(BigDecimal newQuantity) {
        if (quantity == null) getProductProduced();
        BigDecimal previousQuantity = quantity, componentQuantity;
        this.quantity = newQuantity;
        this.quantityIsUpdated = true;
        this.updateCompletionDate = true;
        if (productionRunComponents == null) getProductionRunComponents();
        for (Iterator<GenericValue> iter = productionRunComponents.iterator(); iter.hasNext();) {
            GenericValue component = iter.next();
            componentQuantity = component.getBigDecimal("estimatedQuantity");
            component.set("estimatedQuantity", componentQuantity.divide(previousQuantity, 10, RoundingMode.HALF_UP).multiply(newQuantity).doubleValue());
        }
    }
    /**
     * get the estimatedStartDate property.
     * @return the estimatedStartDate property
     **/
    public Timestamp getEstimatedStartDate() {
        return (exist()? this.estimatedStartDate: null);
    }
    /**
     * set the estimatedStartDate property.
     * @param estimatedStartDate set the estimatedStartDate property
     **/
    public void setEstimatedStartDate(Timestamp estimatedStartDate) {
        this.estimatedStartDate = estimatedStartDate;
        this.updateCompletionDate = true;
    }
    /**
     * get the estimatedCompletionDate property.
     * @return the estimatedCompletionDate property
     **/
    public Timestamp getEstimatedCompletionDate() {
        if (exist()) {
            if (updateCompletionDate) {
                this.estimatedCompletionDate = recalculateEstimatedCompletionDate();
            }
            return this.estimatedCompletionDate;
        }
        else return null;
    }
    /**
     * set the estimatedCompletionDate property without any control or calculation.
     * usage productionRun.setEstimatedCompletionDate(productionRun.recalculateEstimatedCompletionDate(priority);
     * @param estimatedCompletionDate set the estimatedCompletionDate property
     **/
    public void setEstimatedCompletionDate(Timestamp estimatedCompletionDate) {
        this.estimatedCompletionDate = estimatedCompletionDate;
    }
    /**
     * Recalculate the estimatedCompletionDate property.
     * Use the quantity and the estimatedStartDate properties as entries parameters.
     * Read the listRoutingTask and for each recalculated and update the estimatedStart and endDate in the object.
     * No store in the database is done.
     * @param priority give the routingTask start point to recalculated
     * @return the estimatedCompletionDate calculated
     **/
    public Timestamp recalculateEstimatedCompletionDate(Long priority, Timestamp startDate) {
        if (exist()) {
            getProductionRunRoutingTasks();
            if (quantity == null) getQuantity();
            Timestamp endDate=null;
            for (Iterator<GenericValue> iter = productionRunRoutingTasks.iterator(); iter.hasNext();) {
                GenericValue routingTask = iter.next();
                if (priority.compareTo(routingTask.getLong("priority")) <= 0) {
                    // Calculate the estimatedCompletionDate
                    long totalTime = ProductionRun.getEstimatedTaskTime(routingTask, quantity, dispatcher);
                    endDate = TechDataServices.addForward(TechDataServices.getTechDataCalendar(routingTask),startDate, totalTime);
                    // update the routingTask
                    routingTask.set("estimatedStartDate",startDate);
                    routingTask.set("estimatedCompletionDate",endDate);
                    startDate = endDate;
                }
            }
            return endDate;
        } else {
            return null;
        }
    }
    /**
     * call recalculateEstimatedCompletionDate(0,estimatedStartDate), so recalculated for all the routing tasks.
     */
    public Timestamp recalculateEstimatedCompletionDate() {
        this.updateCompletionDate = false;
        return recalculateEstimatedCompletionDate(Long.valueOf(0), estimatedStartDate);
    }
    /**
     * get the productionRunName property.
     * @return the productionRunName property
     **/
    public String getProductionRunName() {
        if (exist()) return this.productionRunName;
        else return null;
    }
    public  void setProductionRunName(String name) {
        this.productionRunName = name;
    }
    /**
     * get the description property.
     * @return the description property
     **/
    public String getDescription() {
        if (exist()) return productionRun.getString("description");
        else return null;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    /**
     * get the GenericValue currentStatus.
     * @return the currentStatus related object
     **/
    public GenericValue getCurrentStatus() {
        if (exist()) {
            if (currentStatus == null) {
                try {
                    currentStatus = productionRun.getRelatedOne("CurrentStatusItem", true);
                } catch (GenericEntityException e) {
                    Debug.logWarning(e.getMessage(), module);
                }
            }
            return currentStatus;
        }
        return null;
    }
    /**
     * get the list of all the productionRunComponents as a list of GenericValue.
     * @return the productionRunComponents related object
     **/
    public List<GenericValue> getProductionRunComponents() {
        if (exist()) {
            if (productionRunComponents == null) {
                if (productionRunRoutingTasks == null)  this.getProductionRunRoutingTasks();
                if (productionRunRoutingTasks != null) {
                    try {
                        productionRunComponents = new LinkedList<GenericValue>();
                        GenericValue routingTask;
                        for (Iterator<GenericValue> iter = productionRunRoutingTasks.iterator(); iter.hasNext();) {
                            routingTask = iter.next();
                            productionRunComponents.addAll(routingTask.getRelated("WorkEffortGoodStandard", UtilMisc.toMap("workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED"),null, false));
                        }
                    } catch (GenericEntityException e) {
                        Debug.logWarning(e.getMessage(), module);
                    }
                }
            }
            return productionRunComponents;
        }
        return null;
    }
    /**
     * get the list of all the productionRunRoutingTasks as a list of GenericValue.
     * @return the productionRunRoutingTasks related object
     **/
    public List<GenericValue> getProductionRunRoutingTasks() {
        if (exist()) {
            if (productionRunRoutingTasks == null) {
                try {
                    productionRunRoutingTasks = productionRun.getRelated("ChildWorkEffort",UtilMisc.toMap("workEffortTypeId","PROD_ORDER_TASK"),UtilMisc.toList("priority"), false);
                } catch (GenericEntityException e) {
                    Debug.logWarning(e.getMessage(), module);
                }
            }
            return productionRunRoutingTasks;
        }
        return null;
    }

    /**
     * get the list of all the productionRunRoutingTasks as a list of GenericValue.
     * @return the productionRunRoutingTasks related object
     **/
    public GenericValue getLastProductionRunRoutingTask() {
        if (exist()) {
            if (productionRunRoutingTasks == null) {
                try {
                    productionRunRoutingTasks = productionRun.getRelated("ChildWorkEffort",UtilMisc.toMap("workEffortTypeId","PROD_ORDER_TASK"),UtilMisc.toList("priority"), false);
                } catch (GenericEntityException e) {
                    Debug.logWarning(e.getMessage(), module);
                }
            }
            return (UtilValidate.isNotEmpty(productionRunRoutingTasks) ? productionRunRoutingTasks.get(productionRunRoutingTasks.size() - 1): null);
        }
        return null;
    }

    /**
     * clear list of all the productionRunRoutingTasks to force re-reading at the next need.
     * This method is used when the routingTasks ordering is changed.
     **/
    public void clearRoutingTasksList() {
        this.productionRunRoutingTasks = null;
    }

    /*
     * FIXME: the two getEstimatedTaskTime(...) methods will be removed and
     * implemented in the "getEstimatedTaskTime" service.
     */
    public static long getEstimatedTaskTime(GenericValue task, BigDecimal quantity, LocalDispatcher dispatcher) {
        return getEstimatedTaskTime(task, quantity, null, null, dispatcher);
    }
    public static long getEstimatedTaskTime(GenericValue task, BigDecimal quantity, String productId, String routingId, LocalDispatcher dispatcher) {
        if (quantity == null) {
            quantity = BigDecimal.ONE;
        }
        if (task == null) return 0;
        double setupTime = 0;
        double taskTime = 1;
        double totalTaskTime = 0;
        if (task.get("estimatedSetupMillis") != null) {
            setupTime = task.getDouble("estimatedSetupMillis").doubleValue();
        }
        if (task.get("estimatedMilliSeconds") != null) {
            taskTime = task.getDouble("estimatedMilliSeconds").doubleValue();
        }
        totalTaskTime = (setupTime + taskTime * quantity.doubleValue());
        
        if (task.get("estimateCalcMethod") != null) {
            String serviceName = null;
            try {
                GenericValue genericService = task.getRelatedOne("CustomMethod", false);
                if (genericService != null && genericService.getString("customMethodName") != null) {
                    serviceName = genericService.getString("customMethodName");
                    // call the service
                    // and put the value in totalTaskTime
                    Map<String, Object> estimateCalcServiceMap = UtilMisc.<String, Object>toMap("workEffort", task, "quantity", quantity, "productId", productId, "routingId", routingId);
                    Map<String, Object> serviceContext = UtilMisc.<String, Object>toMap("arguments", estimateCalcServiceMap);
                    Map<String, Object> resultService = dispatcher.runSync(serviceName, serviceContext);
                    totalTaskTime = ((BigDecimal)resultService.get("totalTime")).doubleValue();
                }
            } catch (GenericServiceException exc) {
                Debug.logError(exc, "Problem calling the customMethod service " + serviceName);
            } catch (Exception exc) {
                Debug.logError(exc, "Problem calling the customMethod service " + serviceName);
            }
        }

        return (long) totalTaskTime;
    }

    public boolean isUpdateCompletionDate() {
        return updateCompletionDate;
    }
}
