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

import org.enhydra.shark.api.common.ProcessMgrIteratorExpressionBuilder;
import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;

public class ProcessMgrIteratorCondExprBldr extends BaseEntityCondExprBldr implements ProcessMgrIteratorExpressionBuilder {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public ProcessMgrIteratorCondExprBldr() {
        this.addEntity("WFPM", org.ofbiz.shark.SharkConstants.WfProcessMgr);
        this.addAllFields("WFPM");
    }

    public ProcessMgrIteratorExpressionBuilder and() {
        this.setOr(false);
        return this;
    }

    public ProcessMgrIteratorExpressionBuilder or() {
        this.setOr(true);
        return this;
    }

    public ProcessMgrIteratorExpressionBuilder not() {
        this.setNot(true);
        return this;
    }

    public ProcessMgrIteratorExpressionBuilder addPackageIdEquals(String s) {
        this.addCondition(new EntityExpr(org.ofbiz.shark.SharkConstants.packageId, isNotSet ? EntityOperator.NOT_EQUAL : EntityOperator.EQUALS, s));
        return this;
    }

    public ProcessMgrIteratorExpressionBuilder addProcessDefIdEquals(String s) {
        this.addCondition(new EntityExpr(org.ofbiz.shark.SharkConstants.definitionId, isNotSet ? EntityOperator.NOT_EQUAL : EntityOperator.EQUALS, s));
        return this;
    }

    public ProcessMgrIteratorExpressionBuilder addNameEquals(String s) {
        this.addCondition(new EntityExpr(org.ofbiz.shark.SharkConstants.mgrName, isNotSet ? EntityOperator.NOT_EQUAL : EntityOperator.EQUALS, s));
        return this;
    }

    public ProcessMgrIteratorExpressionBuilder addVersionEquals(String s) {
        this.addCondition(new EntityExpr(org.ofbiz.shark.SharkConstants.packageVer, isNotSet ? EntityOperator.NOT_EQUAL : EntityOperator.EQUALS, s));
        return this;
    }

    public ProcessMgrIteratorExpressionBuilder addIsEnabled() {
        this.addCondition(new EntityExpr(org.ofbiz.shark.SharkConstants.currentState, isNotSet ? EntityOperator.NOT_EQUAL : EntityOperator.EQUALS, new Long(0)));
        return this;
    }

    public ProcessMgrIteratorExpressionBuilder addExpression(String s) {
        ProcessMgrIteratorExpressionBuilder builder = (ProcessMgrIteratorExpressionBuilder) BaseEntityCondExprBldr.getBuilder(s);
        if (builder != null) {
            return this.addExpression(builder);
        } else {
            return this;
        }
    }

    public ProcessMgrIteratorExpressionBuilder addExpression(ProcessMgrIteratorExpressionBuilder builder) {
        if (!(builder instanceof BaseEntityCondExprBldr)) {
            throw new UnsupportedOperationException("Unsupported implementation");
        } else {
            this.addCondition(((BaseEntityCondExprBldr) builder).getCondition());
        }
        return this;
    }

    public ProcessMgrIteratorExpressionBuilder addCreatedTimeEquals(long arg0) {
        Debug.logInfo("Call : ProcessMgrIteratorExpressionBuilder addCreatedTimeEquals(long arg0)",module);
        return null;
    }

    public ProcessMgrIteratorExpressionBuilder addCreatedTimeBefore(long arg0) {
        Debug.logInfo("Call : ProcessMgrIteratorExpressionBuilder addCreatedTimeBefore(long arg0)",module);
        return null;
    }

    public ProcessMgrIteratorExpressionBuilder addCreatedTimeAfter(long arg0) {
        Debug.logInfo("Call : ProcessMgrIteratorExpressionBuilder addCreatedTimeAfter(long arg0)",module);
        return null;
    }

    public ProcessMgrIteratorExpressionBuilder setOrderByPackageId(boolean arg0) {
        Debug.logInfo("Call : ProcessMgrIteratorExpressionBuilder setOrderByPackageId(boolean arg0)",module);
        return null;
    }

    public ProcessMgrIteratorExpressionBuilder setOrderByProcessDefId(boolean arg0) {
        Debug.logInfo("Call : ProcessMgrIteratorExpressionBuilder setOrderByProcessDefId(boolean arg0)",module);
        return null;
    }

    public ProcessMgrIteratorExpressionBuilder setOrderByName(boolean arg0) {
        Debug.logInfo("Call : ProcessMgrIteratorExpressionBuilder setOrderByName(boolean arg0)",module);
        return null;
    }

    public ProcessMgrIteratorExpressionBuilder setOrderByVersion(boolean arg0) {
        Debug.logInfo("Call : ProcessMgrIteratorExpressionBuilder setOrderByVersion(boolean arg0)",module);
        return null;
    }

    public ProcessMgrIteratorExpressionBuilder setOrderByCreatedTime(boolean arg0) {
        Debug.logInfo("Call : ProcessMgrIteratorExpressionBuilder setOrderByCreatedTime(boolean arg0)",module);
        return null;
    }

    public ProcessMgrIteratorExpressionBuilder setOrderByEnabled(boolean arg0) {
        Debug.logInfo("Call : ProcessMgrIteratorExpressionBuilder setOrderByEnabled(boolean arg0)",module);
        return null;
    }
}
