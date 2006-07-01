/*
 * $Id: ProcessMgrIteratorCondExprBldr.java 7426 2006-04-26 23:35:58Z jonesde $
 *
 * Copyright 2004-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.ofbiz.shark.expression;

import org.enhydra.shark.api.common.ProcessMgrIteratorExpressionBuilder;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;

/**
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @since      3.3
 */
public class ProcessMgrIteratorCondExprBldr extends BaseEntityCondExprBldr implements ProcessMgrIteratorExpressionBuilder {

    public ProcessMgrIteratorCondExprBldr() {
        this.addEntity("WFPM", "WfProcessMgr");
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
        this.addCondition(new EntityExpr("packageId", isNotSet ? EntityOperator.NOT_EQUAL : EntityOperator.EQUALS, s));
        return this;
    }

    public ProcessMgrIteratorExpressionBuilder addProcessDefIdEquals(String s) {
        this.addCondition(new EntityExpr("definitionId", isNotSet ? EntityOperator.NOT_EQUAL : EntityOperator.EQUALS, s));
        return this;
    }

    public ProcessMgrIteratorExpressionBuilder addNameEquals(String s) {
        this.addCondition(new EntityExpr("mgrName", isNotSet ? EntityOperator.NOT_EQUAL : EntityOperator.EQUALS, s));
        return this;
    }

    public ProcessMgrIteratorExpressionBuilder addVersionEquals(String s) {
        this.addCondition(new EntityExpr("packageVer", isNotSet ? EntityOperator.NOT_EQUAL : EntityOperator.EQUALS, s));
        return this;
    }

    public ProcessMgrIteratorExpressionBuilder addIsEnabled() {
        this.addCondition(new EntityExpr("currentState", isNotSet ? EntityOperator.NOT_EQUAL : EntityOperator.EQUALS, new Long(0)));
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
}
