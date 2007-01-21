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

import org.enhydra.shark.api.common.AssignmentIteratorExpressionBuilder;
import org.ofbiz.base.util.Debug;

public class AssignmentIteratorCondExprBldr extends BaseEntityCondExprBldr implements AssignmentIteratorExpressionBuilder {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public AssignmentIteratorCondExprBldr() {
        this.addEntity("WFAS", org.ofbiz.shark.SharkConstants.WfAssignment);
        this.addAllFields("WFAS");
    }

    public AssignmentIteratorExpressionBuilder and() {
        this.setOr(false);
        return this;
    }

    public AssignmentIteratorExpressionBuilder or() {
        this.setOr(true);
        return this;
    }

    public AssignmentIteratorExpressionBuilder not() {
        this.setNot(true);
        return this;
    }

    public AssignmentIteratorExpressionBuilder addUsernameEquals(String s) {
        Debug.logInfo("Call : AssignmentIteratorExpressionBuilder addUsernameEquals(String s)",module); return null;
    }

    public AssignmentIteratorExpressionBuilder addProcessIdEquals(String s) {
        Debug.logInfo("Call : AssignmentIteratorExpressionBuilder addProcessIdEquals(String s)",module); return null;
    }

    public AssignmentIteratorExpressionBuilder addIsAccepted() {
        Debug.logInfo("Call : AssignmentIteratorExpressionBuilder addIsAccepted()",module); return null;
    }

    public AssignmentIteratorExpressionBuilder addPackageIdEquals(String s) {
        Debug.logInfo("Call : AssignmentIteratorExpressionBuilder addPackageIdEquals(String s)",module); return null;
    }

    public AssignmentIteratorExpressionBuilder addPackageVersionEquals(String s) {
        Debug.logInfo("Call : AssignmentIteratorExpressionBuilder addPackageVersionEquals(String s)",module); return null;
    }

    public AssignmentIteratorExpressionBuilder addProcessDefEquals(String s) {
        Debug.logInfo("Call : AssignmentIteratorExpressionBuilder addProcessDefEquals(String s)",module); return null;
    }

    public AssignmentIteratorExpressionBuilder addActivitySetDefEquals(String s) {
        Debug.logInfo("Call : AssignmentIteratorExpressionBuilder addActivitySetDefEquals(String s)",module); return null;
    }

    public AssignmentIteratorExpressionBuilder addActivityDefEquals(String s) {
        Debug.logInfo("Call : AssignmentIteratorExpressionBuilder addActivityDefEquals(String s)",module); return null;
    }

    public AssignmentIteratorExpressionBuilder addProcessDefIdEquals(String arg0) {
        Debug.logInfo("Call : AssignmentIteratorExpressionBuilder addProcessDefIdEquals(String arg0)",module);
        return null;
    }

    public AssignmentIteratorExpressionBuilder addActivitySetDefIdEquals(String arg0) {
        Debug.logInfo("Call : AssignmentIteratorExpressionBuilder addActivitySetDefIdEquals(String arg0)",module);
        return null;
    }

    public AssignmentIteratorExpressionBuilder addActivityDefIdEquals(String arg0) {
        Debug.logInfo("Call : AssignmentIteratorExpressionBuilder addActivityDefIdEquals(String arg0)",module);
        return null;
    }

    public AssignmentIteratorExpressionBuilder addActivityIdEquals(String arg0) {
        Debug.logInfo("Call : AssignmentIteratorExpressionBuilder addActivityIdEquals(String arg0)",module);
        return null;
    }

    public AssignmentIteratorExpressionBuilder addExpression(String arg0) {
        Debug.logInfo("Call : AssignmentIteratorExpressionBuilder addExpression(String arg0)",module);
        return null;
    }

    public AssignmentIteratorExpressionBuilder addExpression(AssignmentIteratorExpressionBuilder arg0) {
        Debug.logInfo("Call : AssignmentIteratorExpressionBuilder addExpression(AssignmentIteratorExpressionBuilder arg0)",module);
        return null;
    }

    public AssignmentIteratorExpressionBuilder setOrderByUsername(boolean arg0) {
        Debug.logInfo("Call : AssignmentIteratorExpressionBuilder setOrderByUsername(boolean arg0)",module);
        return null;
    }

    public AssignmentIteratorExpressionBuilder setOrderByProcessId(boolean arg0) {
        Debug.logInfo("Call : AssignmentIteratorExpressionBuilder setOrderByProcessId(boolean arg0)",module);
        return null;
    }

    public AssignmentIteratorExpressionBuilder setOrderByCreatedTime(boolean arg0) {
        Debug.logInfo("Call : AssignmentIteratorExpressionBuilder setOrderByCreatedTime(boolean arg0)",module);
        return null;
    }

    public AssignmentIteratorExpressionBuilder setOrderByAccepted(boolean arg0) {
        Debug.logInfo("Call : AssignmentIteratorExpressionBuilder setOrderByAccepted(boolean arg0)",module);
        return null;
    }
}
