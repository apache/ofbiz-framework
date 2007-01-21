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

import org.enhydra.shark.api.common.ProcessIteratorExpressionBuilder;
import org.enhydra.shark.api.RootException;
import org.ofbiz.base.util.Debug;

public class ProcessIteratorCondExprBldr extends BaseEntityCondExprBldr implements ProcessIteratorExpressionBuilder {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public ProcessIteratorExpressionBuilder and() {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder and()",module); return null;
    }

    public ProcessIteratorExpressionBuilder or() {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder or()",module); return null;
    }

    public ProcessIteratorExpressionBuilder not() {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder not() ",module); return null;
    }

    public ProcessIteratorExpressionBuilder addPackageIdEquals(String s) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addPackageIdEquals(String s)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addProcessDefIdEquals(String s) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addProcessDefIdEquals(String s)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addMgrNameEquals(String s) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addMgrNameEquals(String s)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addVersionEquals(String s) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addVersionEquals(String s)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addIsEnabled() {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addIsEnabled()",module); return null;
    }

    public ProcessIteratorExpressionBuilder addStateEquals(String s) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addStateEquals(String s)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addStateStartsWith(String s) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addStateStartsWith(String s)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addIdEquals(String s) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addIdEquals(String s)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addNameEquals(String s) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addNameEquals(String s)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addPriorityEquals(int i) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addPriorityEquals(int i)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addDescriptionEquals(String s) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addDescriptionEquals(String s)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addDescriptionContains(String s) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addDescriptionContains(String s)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addRequesterIdEquals(String s) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addRequesterIdEquals(String s)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addStartTimeEquals(long l) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addStartTimeEquals(long l)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addStartTimeBefore(long l) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addStartTimeBefore(long l)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addStartTimeAfter(long l) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addStartTimeAfter(long l)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addLastStateTimeEquals(long l) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addLastStateTimeEquals(long l)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addLastStateTimeBefore(long l) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addLastStateTimeBefore(long l)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addLastStateTimeAfter(long l) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addLastStateTimeAfter(long l)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addActiveActivitiesCountEquals(long l) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addActiveActivitiesCountEquals(long l)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addActiveActivitiesCountGreaterThan(long l) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addActiveActivitiesCountGreaterThan(long l)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addActiveActivitiesCountLessThan(long l) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addActiveActivitiesCountLessThan(long l)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addVariableEquals(String s, Object o) throws RootException {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addVariableEquals(String s, Object o)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addVariableEquals(String s, String s1) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addVariableEquals(String s, String s1)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addVariableEquals(String s, long l) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addVariableEquals(String s, long l)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addVariableGreaterThan(String s, long l) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addVariableGreaterThan(String s, long l)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addVariableLessThan(String s, long l) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addVariableLessThan(String s, long l)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addVariableEquals(String s, double v) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addVariableEquals(String s, double v)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addVariableGreaterThan(String s, double v) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addVariableGreaterThan(String s, double v)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addVariableLessThan(String s, double v) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addVariableLessThan(String s, double v)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addExpression(String s) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addExpression(String s)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addExpression(ProcessIteratorExpressionBuilder processIteratorExpressionBuilder) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addExpression(ProcessIteratorExpressionBuilder processIteratorExpressionBuilder)",module); return null;
    }

    public ProcessIteratorExpressionBuilder addIsMgrEnabled()
    {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addIsMgrEnabled()",module);
        return null;
    }

    public ProcessIteratorExpressionBuilder addRequesterUsernameEquals(String arg0) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addRequesterUsernameEquals(String arg0)",module);
        return null;
    }

    public ProcessIteratorExpressionBuilder addCreatedTimeEquals(long arg0) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addCreatedTimeEquals(long arg0)",module);
        return null;
    }

    public ProcessIteratorExpressionBuilder addCreatedTimeBefore(long arg0) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addCreatedTimeBefore(long arg0)",module);
        return null;
    }

    public ProcessIteratorExpressionBuilder addCreatedTimeAfter(long arg0) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder addCreatedTimeAfter(long arg0)",module);
        return null;
    }

    public ProcessIteratorExpressionBuilder setOrderByMgrName(boolean arg0) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder setOrderByMgrName(boolean arg0)",module);
        return null;
    }

    public ProcessIteratorExpressionBuilder setOrderById(boolean arg0) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder setOrderById(boolean arg0)",module);
        return null;
    }

    public ProcessIteratorExpressionBuilder setOrderByName(boolean arg0) {
        Debug.logInfo("Call :  ProcessIteratorExpressionBuilder setOrderByName(boolean arg0)",module);
        return null;
    }

    public ProcessIteratorExpressionBuilder setOrderByState(boolean arg0) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder setOrderByState(boolean arg0)",module);
        return null;
    }

    public ProcessIteratorExpressionBuilder setOrderByPriority(boolean arg0) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder setOrderByPriority(boolean arg0)",module);
        return null;
    }

    public ProcessIteratorExpressionBuilder setOrderByCreatedTime(boolean arg0) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder setOrderByCreatedTime(boolean arg0)",module);
        return null;
    }

    public ProcessIteratorExpressionBuilder setOrderByStartTime(boolean arg0) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder setOrderByStartTime(boolean arg0)",module);
        return null;
    }

    public ProcessIteratorExpressionBuilder setOrderByLastStateTime(boolean arg0) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder setOrderByLastStateTime(boolean arg0)",module);
        return null;
    }

    public ProcessIteratorExpressionBuilder setOrderByResourceRequesterId(boolean arg0) {
        Debug.logInfo("Call : ProcessIteratorExpressionBuilder setOrderByResourceRequesterId(boolean arg0)",module);
        return null;
    }
}
