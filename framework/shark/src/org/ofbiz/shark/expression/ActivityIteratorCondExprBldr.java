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
package org.ofbiz.shark.expression;

import java.sql.Timestamp;

import org.enhydra.shark.api.RootException;
import org.enhydra.shark.api.common.ActivityIteratorExpressionBuilder;
import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.ModelKeyMap;
public class ActivityIteratorCondExprBldr extends BaseEntityCondExprBldr implements ActivityIteratorExpressionBuilder {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    protected boolean addedProcess = false;

    public ActivityIteratorCondExprBldr() {
        this.addEntity("WFAC", org.ofbiz.shark.SharkConstants.WfActivity);
        this.addAllFields("WFAC");
    }

    public void addProcess(String field, String fieldAlias) {
        if (!addedProcess) {
            this.addEntity("WFPR", org.ofbiz.shark.SharkConstants.WfProcess);
            this.addLink("WFAC", "WFAC", false, ModelKeyMap.makeKeyMapList(org.ofbiz.shark.SharkConstants.processId));
        }
        this.addField("WFPR", field, fieldAlias);
    }

    public ActivityIteratorExpressionBuilder and() {
        this.setOr(false);
        return this;
    }

    public ActivityIteratorExpressionBuilder or() {
        this.setOr(true);
        return this;
    }

    public ActivityIteratorExpressionBuilder not() {
        this.setNot(true);
        return this;
    }

    // WfProcess conditions

    public ActivityIteratorExpressionBuilder addPackageIdEquals(String s) {
        this.addProcess(org.ofbiz.shark.SharkConstants.packageId, org.ofbiz.shark.SharkConstants.packageId);
        this.addCondition(new EntityExpr(org.ofbiz.shark.SharkConstants.packageId, isNotSet ? EntityOperator.NOT_EQUAL : EntityOperator.EQUALS, s));
        return this;
    }

    public ActivityIteratorExpressionBuilder addProcessDefIdEquals(String s) {
        this.addProcess(org.ofbiz.shark.SharkConstants.definitionId, "procDefId");
        this.addCondition(new EntityExpr("procDefId", isNotSet ? EntityOperator.NOT_EQUAL : EntityOperator.EQUALS, s));
        return this;
    }

    public ActivityIteratorExpressionBuilder addMgrNameEquals(String s) {
        this.addProcess(org.ofbiz.shark.SharkConstants.mgrName, org.ofbiz.shark.SharkConstants.mgrName);
        this.addCondition(new EntityExpr(org.ofbiz.shark.SharkConstants.mgrName, isNotSet ? EntityOperator.NOT_EQUAL : EntityOperator.EQUALS, s));
        return this;
    }

    public ActivityIteratorExpressionBuilder addVersionEquals(String s) {
        this.addProcess(org.ofbiz.shark.SharkConstants.packageVer, org.ofbiz.shark.SharkConstants.packageVer);
        this.addCondition(new EntityExpr(org.ofbiz.shark.SharkConstants.packageVer, isNotSet ? EntityOperator.NOT_EQUAL : EntityOperator.EQUALS, s));
        return this;
    }

    public ActivityIteratorExpressionBuilder addIsEnabled() {
        return this;
    }

    public ActivityIteratorExpressionBuilder addProcessStateEquals(String s) {
        this.addProcess(org.ofbiz.shark.SharkConstants.packageVer, org.ofbiz.shark.SharkConstants.packageVer);
        this.addCondition(new EntityExpr(org.ofbiz.shark.SharkConstants.packageVer, isNotSet ? EntityOperator.NOT_EQUAL : EntityOperator.EQUALS, s));
        return this;
    }

    public ActivityIteratorExpressionBuilder addProcessStateStartsWith(String s) {
        this.addProcess(org.ofbiz.shark.SharkConstants.currentState, "procState");
        this.addCondition(new EntityExpr("procState", isNotSet ? EntityOperator.NOT_LIKE : EntityOperator.LIKE, s + "%"));
        return this;
    }

    public ActivityIteratorExpressionBuilder addProcessIdEquals(String s) {
        this.addCondition(new EntityExpr(org.ofbiz.shark.SharkConstants.processId, isNotSet ? EntityOperator.NOT_EQUAL : EntityOperator.EQUALS, s));
        return this;
    }

    public ActivityIteratorExpressionBuilder addProcessNameEquals(String s) {
        this.addProcess(org.ofbiz.shark.SharkConstants.processName, org.ofbiz.shark.SharkConstants.processName);
        this.addCondition(new EntityExpr(org.ofbiz.shark.SharkConstants.processName, isNotSet ? EntityOperator.NOT_EQUAL : EntityOperator.EQUALS, s));
        return this;
    }

    public ActivityIteratorExpressionBuilder addProcessPriorityEquals(int i) {
        this.addProcess(org.ofbiz.shark.SharkConstants.priority, "procPriority");
        this.addCondition(new EntityExpr("procPriority", isNotSet ? EntityOperator.NOT_EQUAL : EntityOperator.EQUALS, new Long(i)));
        return this;
    }

    public ActivityIteratorExpressionBuilder addProcessDescriptionEquals(String s) {
        this.addProcess(org.ofbiz.shark.SharkConstants.description, "procDesc");
        this.addCondition(new EntityExpr("procDesc", isNotSet ? EntityOperator.NOT_EQUAL : EntityOperator.EQUALS, s));
        return this;
    }

    public ActivityIteratorExpressionBuilder addProcessDescriptionContains(String s) {
        this.addProcess(org.ofbiz.shark.SharkConstants.description, "procDesc");
        this.addCondition(new EntityExpr("procDesc", isNotSet ? EntityOperator.NOT_LIKE : EntityOperator.LIKE, "%" + s + "%"));
        return this;
    }

    public ActivityIteratorExpressionBuilder addProcessRequesterIdEquals(String s) {
        return this;
    }

    public ActivityIteratorExpressionBuilder addProcessStartTimeEquals(long l) {
        return this;
    }

    public ActivityIteratorExpressionBuilder addProcessStartTimeBefore(long l) {
        return this;
    }

    public ActivityIteratorExpressionBuilder addProcessStartTimeAfter(long l) {
        return this;
    }

    public ActivityIteratorExpressionBuilder addProcessLastStateTimeEquals(long l) {
        return this;
    }

    public ActivityIteratorExpressionBuilder addProcessLastStateTimeBefore(long l) {
        return this;
    }

    public ActivityIteratorExpressionBuilder addProcessLastStateTimeAfter(long l) {
        return this;
    }

    public ActivityIteratorExpressionBuilder addProcessVariableEquals(String s, Object o) throws RootException {
        return this;
    }

    public ActivityIteratorExpressionBuilder addProcessVariableEquals(String s, String s1) {
        return this;
    }

    public ActivityIteratorExpressionBuilder addProcessVariableEquals(String s, long l) {
        return this;
    }

    public ActivityIteratorExpressionBuilder addProcessVariableGreaterThan(String s, long l) {
        return this;
    }

    public ActivityIteratorExpressionBuilder addProcessVariableLessThan(String s, long l) {
        return this;
    }

    public ActivityIteratorExpressionBuilder addProcessVariableEquals(String s, double v) {
        return this;
    }

    public ActivityIteratorExpressionBuilder addProcessVariableGreaterThan(String s, double v) {
        return this;
    }

    public ActivityIteratorExpressionBuilder addProcessVariableLessThan(String s, double v) {
        return this;
    }

    // WfActivity Conditions

    public ActivityIteratorExpressionBuilder addStateEquals(String s) {
        this.addCondition(new EntityExpr(org.ofbiz.shark.SharkConstants.currentState, isNotSet ? EntityOperator.NOT_EQUAL : EntityOperator.EQUALS, s));
        return this;
    }

    public ActivityIteratorExpressionBuilder addStateStartsWith(String s) {
        this.addCondition(new EntityExpr(org.ofbiz.shark.SharkConstants.currentState, isNotSet ? EntityOperator.NOT_LIKE : EntityOperator.LIKE, s + "%"));
        return this;
    }

    public ActivityIteratorExpressionBuilder addIdEquals(String s) {
        this.addCondition(new EntityExpr(org.ofbiz.shark.SharkConstants.activityId, isNotSet ? EntityOperator.NOT_EQUAL : EntityOperator.EQUALS, s));
        return this;
    }

    public ActivityIteratorExpressionBuilder addNameEquals(String s) {
        this.addCondition(new EntityExpr(org.ofbiz.shark.SharkConstants.activityName, isNotSet ? EntityOperator.NOT_EQUAL : EntityOperator.EQUALS, s));
        return this;
    }

    public ActivityIteratorExpressionBuilder addPriorityEquals(int i) {
        this.addCondition(new EntityExpr(org.ofbiz.shark.SharkConstants.priority, isNotSet ? EntityOperator.NOT_EQUAL : EntityOperator.EQUALS, new Long(i)));
        return this;
    }

    public ActivityIteratorExpressionBuilder addDescriptionEquals(String s) {
        this.addCondition(new EntityExpr(org.ofbiz.shark.SharkConstants.description, isNotSet ? EntityOperator.NOT_EQUAL : EntityOperator.EQUALS, s));
        return this;
    }

    public ActivityIteratorExpressionBuilder addDescriptionContains(String s) {
        this.addCondition(new EntityExpr(org.ofbiz.shark.SharkConstants.description, isNotSet ? EntityOperator.NOT_LIKE : EntityOperator.LIKE, "%" + s + "%"));
        return this;
    }

    public ActivityIteratorExpressionBuilder addActivatedTimeEquals(long l) {
        this.addCondition(new EntityExpr(org.ofbiz.shark.SharkConstants.activatedTime, isNotSet ? EntityOperator.NOT_EQUAL : EntityOperator.EQUALS, new Timestamp(l)));
        return this;
    }

    public ActivityIteratorExpressionBuilder addActivatedTimeBefore(long l) {
        this.addCondition(new EntityExpr(org.ofbiz.shark.SharkConstants.activatedTime, isNotSet ? EntityOperator.LESS_THAN : EntityOperator.GREATER_THAN_EQUAL_TO, new Timestamp(l)));
        return this;
    }

    public ActivityIteratorExpressionBuilder addActivatedTimeAfter(long l) {
        this.addCondition(new EntityExpr(org.ofbiz.shark.SharkConstants.activatedTime, isNotSet ? EntityOperator.GREATER_THAN : EntityOperator.LESS_THAN_EQUAL_TO, new Timestamp(l)));
        return this;
    }

    public ActivityIteratorExpressionBuilder addLastStateTimeEquals(long l) {
        this.addCondition(new EntityExpr(org.ofbiz.shark.SharkConstants.lastStateTime, isNotSet ? EntityOperator.NOT_EQUAL : EntityOperator.EQUALS, new Timestamp(l)));
        return this;
    }

    public ActivityIteratorExpressionBuilder addLastStateTimeBefore(long l) {
        this.addCondition(new EntityExpr(org.ofbiz.shark.SharkConstants.lastStateTime, isNotSet ? EntityOperator.LESS_THAN : EntityOperator.GREATER_THAN_EQUAL_TO, new Timestamp(l)));
        return this;
    }

    public ActivityIteratorExpressionBuilder addLastStateTimeAfter(long l) {
        this.addCondition(new EntityExpr(org.ofbiz.shark.SharkConstants.lastStateTime, isNotSet ? EntityOperator.NOT_EQUAL : EntityOperator.EQUALS, new Timestamp(l)));
        return this;
    }

    public ActivityIteratorExpressionBuilder addAcceptedTimeEquals(long l) {
        this.addCondition(new EntityExpr(org.ofbiz.shark.SharkConstants.acceptedTime, isNotSet ? EntityOperator.NOT_EQUAL : EntityOperator.EQUALS, new Timestamp(l)));
        return this;
    }

    public ActivityIteratorExpressionBuilder addAcceptedTimeBefore(long l) {
        this.addCondition(new EntityExpr(org.ofbiz.shark.SharkConstants.acceptedTime, isNotSet ? EntityOperator.LESS_THAN : EntityOperator.GREATER_THAN_EQUAL_TO, new Timestamp(l)));
        return this;
    }

    public ActivityIteratorExpressionBuilder addAcceptedTimeAfter(long l) {
        this.addCondition(new EntityExpr(org.ofbiz.shark.SharkConstants.acceptedTime, isNotSet ? EntityOperator.GREATER_THAN : EntityOperator.LESS_THAN_EQUAL_TO, new Timestamp(l)));
        return this;
    }

    public ActivityIteratorExpressionBuilder addVariableEquals(String s, Object o) throws RootException {
        if (o != null) {
            if (o instanceof String) {
                return addVariableEquals(s, (String) o);
            } else if (o instanceof Number) {
                if (o instanceof Double) {
                    return addVariableEquals(s, ((Double) o).doubleValue());
                } else {
                    return addVariableEquals(s, ((Long) o).longValue());
                }
            } else {
                throw new RootException("Unable to compare database blobs!");
            }
        }
        return this;
    }

    public ActivityIteratorExpressionBuilder addVariableEquals(String s, String s1)
    {
        Debug.logInfo("Call : ActivityIteratorExpressionBuilder addVariableEquals(String s, String s1)", module);
        return this;
    }

    public ActivityIteratorExpressionBuilder addVariableEquals(String s, long l)
    {
        Debug.logInfo("Call : ActivityIteratorExpressionBuilder addVariableEquals(String s, long l)", module);
        return this;
    }

    public ActivityIteratorExpressionBuilder addVariableGreaterThan(String s, long l)
    {
        Debug.logInfo("Call : ActivityIteratorExpressionBuilder addVariableGreaterThan(String s, long l)",module);
        return this;
    }

    public ActivityIteratorExpressionBuilder addVariableLessThan(String s, long l)
    {
        Debug.logInfo("Call : ActivityIteratorExpressionBuilder addVariableLessThan(String s, long l)",module);
        return this;
    }

    public ActivityIteratorExpressionBuilder addVariableEquals(String s, double v)
    {
        Debug.logInfo("Call : ActivityIteratorExpressionBuilder addVariableEquals(String s, double v)",module);
        return this;
    }

    public ActivityIteratorExpressionBuilder addVariableGreaterThan(String s, double v)
    {
        Debug.logInfo("Call : ActivityIteratorExpressionBuilder addVariableGreaterThan(String s, double v)",module);
        return this;
    }

    public ActivityIteratorExpressionBuilder addVariableLessThan(String s, double v)
    {
        Debug.logInfo("Call : ActivityIteratorExpressionBuilder addVariableLessThan(String s, double v)",module);
        return this;
    }

    public ActivityIteratorExpressionBuilder addActivitySetDefId(String s)
    {
        Debug.logInfo("Call : ActivityIteratorExpressionBuilder addActivitySetDefId(String s)",module);
        return this;
    }

    public ActivityIteratorExpressionBuilder addDefinitionId(String s)
    {
        Debug.logInfo("Call : ActivityIteratorExpressionBuilder addDefinitionId(String s)",module);
        return this;
    }

    public ActivityIteratorExpressionBuilder addIsAccepted()
    {
        Debug.logInfo("Call : ActivityIteratorExpressionBuilder addIsAccepted()",module);
        return this;
    }

    public ActivityIteratorExpressionBuilder addResourceUsername(String s)
    {
        Debug.logInfo("Call : ActivityIteratorExpressionBuilder addResourceUsername(String s)",module);
        return this;
    }

    public ActivityIteratorExpressionBuilder addExpression(String s)
    {
        Debug.logInfo("Call : ActivityIteratorExpressionBuilder addExpression(String s)",module);
        return this;
    }

    public ActivityIteratorExpressionBuilder addExpression(ActivityIteratorExpressionBuilder eieb)
    {
        Debug.logInfo("Call : ActivityIteratorExpressionBuilder addExpression(ActivityIteratorExpressionBuilder eieb)",module);
        return this;
    }

    public ActivityIteratorExpressionBuilder addIsMgrEnabled()
    {
        Debug.logInfo("Call : ActivityIteratorExpressionBuilder addIsMgrEnabled()",module);
        return null;
    }

    public ActivityIteratorExpressionBuilder addProcessCreatedTimeEquals(long arg0)
    {
        Debug.logInfo("Call : ActivityIteratorExpressionBuilder addProcessCreatedTimeEquals(long arg0)",module);
        return null;
    }

    public ActivityIteratorExpressionBuilder addProcessCreatedTimeBefore(long arg0)
    {
        Debug.logInfo("Call : ActivityIteratorExpressionBuilder addProcessCreatedTimeBefore(long arg0)",module);
        return null;
    }

    public ActivityIteratorExpressionBuilder addProcessCreatedTimeAfter(long arg0)
    {
        Debug.logInfo("Call : ActivityIteratorExpressionBuilder addProcessCreatedTimeAfter(long arg0)", module);
        return null;
    }

    public ActivityIteratorExpressionBuilder setOrderById(boolean arg0)
    {
        Debug.logInfo("Call : ActivityIteratorExpressionBuilder setOrderById(boolean arg0)",module);
        return null;
    }

    public ActivityIteratorExpressionBuilder setOrderByActivitySetDefId(boolean arg0)
    {
        Debug.logInfo("Call : ActivityIteratorExpressionBuilder setOrderByActivitySetDefId(boolean arg0)",module);
        return null;
    }

    public ActivityIteratorExpressionBuilder setOrderByDefinitionId(boolean arg0)
    {
        Debug.logInfo("Call : ActivityIteratorExpressionBuilder setOrderByDefinitionId(boolean arg0)",module);
        return null;
    }

    public ActivityIteratorExpressionBuilder setOrderByProcessId(boolean arg0)
    {
        Debug.logInfo("Call : ActivityIteratorExpressionBuilder setOrderByProcessId(boolean arg0)",module);
        return null;
    }

    public ActivityIteratorExpressionBuilder setOrderByResourceUsername(boolean arg0)
    {
        Debug.logInfo("Call : ActivityIteratorExpressionBuilder setOrderByResourceUsername(boolean arg0)",module);
        return null;
    }

    public ActivityIteratorExpressionBuilder setOrderByProcessDefName(boolean arg0)
    {
        Debug.logInfo("Call : ActivityIteratorExpressionBuilder setOrderByProcessDefName(boolean arg0)",module);
        return null;
    }

    public ActivityIteratorExpressionBuilder setOrderByState(boolean arg0)
    {
        Debug.logInfo("Call : ActivityIteratorExpressionBuilder setOrderByState(boolean arg0)",module);
        return null;
    }

    public ActivityIteratorExpressionBuilder setOrderByPerformer(boolean arg0)
    {
        Debug.logInfo("Call : ActivityIteratorExpressionBuilder setOrderByPerformer(boolean arg0)",module);
        return null;
    }

    public ActivityIteratorExpressionBuilder setOrderByPriority(boolean arg0)
    {
        Debug.logInfo("Call : ActivityIteratorExpressionBuilder setOrderByPriority(boolean arg0)", module);
        return null;
    }

    public ActivityIteratorExpressionBuilder setOrderByName(boolean arg0)
    {
        Debug.logInfo("Call : ActivityIteratorExpressionBuilder setOrderByName(boolean arg0)",module);
        return null;
    }

    public ActivityIteratorExpressionBuilder setOrderByActivatedTime(boolean arg0)
    {
        Debug.logInfo("Call : ActivityIteratorExpressionBuilder setOrderByActivatedTime(boolean arg0)",module);
        return null;
    }

    public ActivityIteratorExpressionBuilder setOrderByAcceptedTime(boolean arg0)
    {
        Debug.logInfo("Call : ActivityIteratorExpressionBuilder setOrderByAcceptedTime(boolean arg0)",module);
        return null;
    }

    public ActivityIteratorExpressionBuilder setOrderByLastStateTime(boolean arg0)
    {
        Debug.logInfo("Call : ActivityIteratorExpressionBuilder setOrderByLastStateTime(boolean arg0)",module);
        return null;
    }
}
