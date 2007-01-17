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

import java.sql.Timestamp;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.manufacturing.techdata.TechDataServices;


/**
 * ProductionRun Object used by the Jobshop management OFBiz comonents,
 * this object is used to find or updated an existing ProductionRun.
 *
 */
public class ProductionRun {
    
    public static final String module = ProductionRun.class.getName();
    public static final String resource = "ManufacturingUiLabels";
    
    protected GenericValue productionRun; // WorkEffort (PROD_ORDER_HEADER)
    protected GenericValue productionRunProduct; // WorkEffortGoodStandard (type: PRUN_PROD_DELIV)
    protected GenericValue productProduced; // Product (from WorkEffortGoodStandard of type: PRUN_PROD_DELIV)
    protected Double quantity; // the estimatedQuantity
    
    protected Timestamp estimatedStartDate;
    protected Timestamp estimatedCompletionDate;
    protected String productionRunName;
    protected String description;
    protected GenericValue currentStatus;
    protected List productionRunComponents;
    protected List productionRunRoutingTasks;
    protected LocalDispatcher dispatcher;
    
    /**
     * indicate if quantity or estimatedStartDate has been modified and
     *  estimatedCompletionDate not yet recalculated with recalculateEstimatedCompletionDate() methode.
     */
    private boolean updateCompletionDate = false;
    /**
     * indicate if quantity  has been modified, used for store() method to update appropriate entity.
     */
    private boolean quantityIsUpdated = false;
    
    public ProductionRun(String productionRunId, GenericDelegator delegator, LocalDispatcher dispatcher) {
        try {
            if (! UtilValidate.isEmpty(productionRunId)) {
                this.dispatcher = dispatcher;
                GenericValue workEffort = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", productionRunId));
                if (workEffort != null) {
                    // If this is a task, get the parent production run
                    if (workEffort.getString("workEffortTypeId") != null && "PROD_ORDER_TASK".equals(workEffort.getString("workEffortTypeId"))) {
                        workEffort = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffort.getString("workEffortParentId")));
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
    public boolean exist(){
        return productionRun != null;
    }
    
    /**
     * get the ProductionRun GenericValue .
     * @return the ProductionRun GenericValue
     **/
    public GenericValue getGenericValue(){
        return productionRun;
    }
    /**
     * store  the modified ProductionRun object in the database.
     *     <li>store the the productionRun header
     *     <li> the productProduced related data
     *     <li> the listRoutingTask related data
     *     <li> the productComponent list related data
     * @return true if success false otherwise
     **/
    public boolean store(){
        if (exist()){
            if (updateCompletionDate){
                this.estimatedCompletionDate = recalculateEstimatedCompletionDate();
            }
            productionRun.set("estimatedStartDate",this.estimatedStartDate);
            productionRun.set("estimatedCompletionDate",this.estimatedCompletionDate);
            productionRun.set("workEffortName",this.productionRunName);
            productionRun.set("description",this.description);
            try {
                productionRun.store();
                if (quantityIsUpdated) {
                    productionRunProduct.set("estimatedQuantity",this.quantity);
                    productionRunProduct.store();
                    quantityIsUpdated = false;
                }
                if (productionRunRoutingTasks != null) {
                    for (Iterator iter = productionRunRoutingTasks.iterator(); iter.hasNext();){
                        GenericValue routingTask = (GenericValue) iter.next();
                        routingTask.store();
                    }
                }
                if (productionRunComponents != null) {
                    for (Iterator iter = productionRunComponents.iterator(); iter.hasNext();){
                        GenericValue component = (GenericValue) iter.next();
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
    public GenericValue getProductProduced(){
        if (exist()) {
            if (productProduced == null) {
                try {
                    List productionRunProducts = productionRun.getRelated("WorkEffortGoodStandard", UtilMisc.toMap("workEffortGoodStdTypeId", "PRUN_PROD_DELIV"),null);
                    this.productionRunProduct = EntityUtil.getFirst(productionRunProducts);
                    quantity = productionRunProduct.getDouble("estimatedQuantity");
                    productProduced = productionRunProduct.getRelatedOneCache("Product");
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
    public Double getQuantity(){
        if (exist()) {
            if (quantity == null)  getProductProduced();
            return quantity;
        }
        else return null;
    }
    /**
     * set the quantity property and recalculated the productComponent quantity.
     * @return
     **/
    public void setQuantity(Double newQuantity) {
        if (quantity == null) getProductProduced();
        double previousQuantity = quantity.doubleValue(), componentQuantity;
        this.quantity = newQuantity;
        this.quantityIsUpdated = true;
        this.updateCompletionDate = true;
        if (productionRunComponents == null) getProductionRunComponents();
        for (Iterator iter = productionRunComponents.iterator(); iter.hasNext();){
            GenericValue component = (GenericValue) iter.next();
            componentQuantity = component.getDouble("estimatedQuantity").doubleValue();
            component.set("estimatedQuantity", new Double(componentQuantity / previousQuantity * newQuantity.doubleValue()));
        }
    }
    /**
     * get the estimatedStartDate property.
     * @return the estimatedStartDate property
     **/
    public Timestamp getEstimatedStartDate(){
        return (exist()? this.estimatedStartDate: null);
    }
    /**
     * set the estimatedStartDate property.
     * @return
     **/
    public void setEstimatedStartDate(Timestamp estimatedStartDate){
        this.estimatedStartDate = estimatedStartDate;
        this.updateCompletionDate = true;
    }
    /**
     * get the estimatedCompletionDate property.
     * @return the estimatedCompletionDate property
     **/
    public Timestamp getEstimatedCompletionDate(){
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
     * @return
     **/
    public void setEstimatedCompletionDate(Timestamp estimatedCompletionDate){
        this.estimatedCompletionDate = estimatedCompletionDate;
    }
    /**
     * recalculated  the estimatedCompletionDate property.
     *     Use the quantity and the estimatedStartDate properties as entries parameters.
     *     <br/>read the listRoutingTask and for each recalculated and update the estimatedStart and endDate in the object.
     *     <br/> no store in the database is done.
     * @param priority give the routingTask start point to recalculated
     * @return the estimatedCompletionDate calculated
     **/
    public Timestamp recalculateEstimatedCompletionDate(Long priority, Timestamp startDate){
        if (exist()) {
            getProductionRunRoutingTasks();
            if (quantity == null) getQuantity();
            Timestamp endDate=null;
            for (Iterator iter=productionRunRoutingTasks.iterator(); iter.hasNext();){
                GenericValue routingTask = (GenericValue) iter.next();
                if (priority.compareTo(routingTask.getLong("priority")) <= 0){
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
     * call recalculateEstimatedCompletionDate(0,estimatedStartDate), so recalculated for all the routingtask.
     */
    public Timestamp recalculateEstimatedCompletionDate(){
        this.updateCompletionDate = false;
        return recalculateEstimatedCompletionDate(new Long(0), estimatedStartDate);
    }
    /**
     * get the productionRunName property.
     * @return the productionRunName property
     **/
    public String getProductionRunName(){
        if (exist()) return this.productionRunName;
        else return null;
    }
    public  void setProductionRunName(String name){
        this.productionRunName = name;
    }
    /**
     * get the description property.
     * @return the description property
     **/
    public String getDescription(){
        if (exist()) return productionRun.getString("description");
        else return null;
    }
    public void setDescription(String description){
        this.description = description;
    }
    /**
     * get the GenericValue currentStatus.
     * @return the currentStatus related object
     **/
    public GenericValue getCurrentStatus(){
        if (exist()) {
            if (currentStatus == null) {
                try {
                    currentStatus = productionRun.getRelatedOneCache("StatusItem");
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
    public List getProductionRunComponents(){
        if (exist()) {
            if (productionRunComponents == null) {
                if (productionRunRoutingTasks == null)  this.getProductionRunRoutingTasks();
                if (productionRunRoutingTasks != null) {
                    try {
                        productionRunComponents = new LinkedList();
                        GenericValue routingTask;
                        for (Iterator iter=productionRunRoutingTasks.iterator(); iter.hasNext();) {
                            routingTask = (GenericValue)iter.next();
                            productionRunComponents.addAll(routingTask.getRelated("WorkEffortGoodStandard", UtilMisc.toMap("workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED"),null));
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
    public List getProductionRunRoutingTasks(){
        if (exist()) {
            if (productionRunRoutingTasks == null) {
                try {
                    productionRunRoutingTasks = productionRun.getRelated("ChildWorkEffort",UtilMisc.toMap("workEffortTypeId","PROD_ORDER_TASK"),UtilMisc.toList("priority"));
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
    public GenericValue getLastProductionRunRoutingTask(){
        if (exist()) {
            if (productionRunRoutingTasks == null) {
                try {
                    productionRunRoutingTasks = productionRun.getRelated("ChildWorkEffort",UtilMisc.toMap("workEffortTypeId","PROD_ORDER_TASK"),UtilMisc.toList("priority"));
                } catch (GenericEntityException e) {
                    Debug.logWarning(e.getMessage(), module);
                }
            }
            return (GenericValue)(productionRunRoutingTasks != null && productionRunRoutingTasks.size() > 0? productionRunRoutingTasks.get(productionRunRoutingTasks.size() - 1): null);
        }
        return null;
    }

    /**
     * clear list of all the productionRunRoutingTasks to force re-reading at the next need.
     * This methode is used when the routingTasks ordering is changed.
     * @return
     **/
    public void clearRoutingTasksList(){
        this.productionRunRoutingTasks = null;
    }
    
    /*
     * FIXME: the three getEstimatedTaskTime(...) methods will be removed and
     * implemented in the "getEstimatedTaskTime" service.
     */
    public static long getEstimatedTaskTime(GenericValue task, double quantity, LocalDispatcher dispatcher) {
        return getEstimatedTaskTime(task, new Double(quantity), dispatcher);
    }
    public static long getEstimatedTaskTime(GenericValue task, Double quantity, LocalDispatcher dispatcher) {
        return getEstimatedTaskTime(task, quantity, null, null, dispatcher);
    }
    public static long getEstimatedTaskTime(GenericValue task, Double quantity, String productId, String routingId, LocalDispatcher dispatcher) {
        if (quantity == null) {
            quantity = new Double(1);
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
        // TODO
        if (task.get("estimateCalcMethod") != null) {
            String serviceName = null;
            try {
                GenericValue genericService = task.getRelatedOne("CustomMethod");
                if (genericService != null && genericService.getString("customMethodName") != null) {
                    serviceName = genericService.getString("customMethodName");
                    // call the service
                    // and put the value in totalTaskTime
                    Map estimateCalcServiceMap = UtilMisc.toMap("workEffort", task, "quantity", quantity, "productId", productId, "routingId", routingId);
                    Map serviceContext = UtilMisc.toMap("arguments", estimateCalcServiceMap);
                    // serviceContext.put("userLogin", userLogin);
                    Map resultService = dispatcher.runSync(serviceName, serviceContext);
                    totalTaskTime = ((Double)resultService.get("totalTime")).doubleValue();
                }
            } catch(Exception exc) {
                Debug.logError(exc, "Problem calling the customMethod service " + serviceName);
            }
        }
        
        return (long) totalTaskTime;
    }

}
