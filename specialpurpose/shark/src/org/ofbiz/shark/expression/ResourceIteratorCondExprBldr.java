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

import org.enhydra.shark.api.common.ResourceIteratorExpressionBuilder;
import org.ofbiz.base.util.Debug;
public class ResourceIteratorCondExprBldr extends BaseEntityCondExprBldr implements ResourceIteratorExpressionBuilder {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public ResourceIteratorExpressionBuilder and() {
        Debug.logInfo("Call : ResourceIteratorExpressionBuilder and()",module); return null;
    }

    public ResourceIteratorExpressionBuilder or() {
        Debug.logInfo("Call : ResourceIteratorExpressionBuilder or()",module); return null;
    }

    public ResourceIteratorExpressionBuilder not() {
        Debug.logInfo("Call : ResourceIteratorExpressionBuilder not() ",module); return null;
    }

    public ResourceIteratorExpressionBuilder addUsernameEquals(String s) {
        Debug.logInfo("Call : ResourceIteratorExpressionBuilder addUsernameEquals(String s)",module); return null;
    }

    public ResourceIteratorExpressionBuilder addAssignemtCountEquals(long l) {
        Debug.logInfo("Call : ResourceIteratorExpressionBuilder addAssignemtCountEquals(long l)",module); return null;
    }

    public ResourceIteratorExpressionBuilder addAssignemtCountLessThan(long l) {
        Debug.logInfo("Call : ResourceIteratorExpressionBuilder addAssignemtCountLessThan(long l)",module); return null;
    }

    public ResourceIteratorExpressionBuilder addAssignemtCountGreaterThan(long l) {
        Debug.logInfo("Call : ResourceIteratorExpressionBuilder addAssignemtCountGreaterThan(long l)",module); return null;
    }

    public ResourceIteratorExpressionBuilder addExpression(String s) {
        Debug.logInfo("Call : ResourceIteratorExpressionBuilder addExpression(String s)",module); return null;
    }

    public ResourceIteratorExpressionBuilder addExpression(ResourceIteratorExpressionBuilder resourceIteratorExpressionBuilder) {
        Debug.logInfo("Call : ResourceIteratorExpressionBuilder addExpression(ResourceIteratorExpressionBuilder resourceIteratorExpressionBuilder)",module); return null;
    }

    public ResourceIteratorExpressionBuilder setOrderByUsername(boolean arg0) {
        Debug.logInfo("Call : ResourceIteratorExpressionBuilder setOrderByUsername(boolean arg0)",module); return null;
    }
}
