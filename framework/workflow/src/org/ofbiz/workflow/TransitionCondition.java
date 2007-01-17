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
package org.ofbiz.workflow;

import java.util.Map;

import org.ofbiz.service.DispatchContext;

/**
 * TransitionCondition - Interface for implementing transition conditions
 */
public interface TransitionCondition {
    
    /**
     * Evaluate a condition and return the result as a Boolean
     * @param context Map of environment info (processContext) for use in evaluation
     * @param attrs Map of transition's extended attributes
     * @param expression The expression from the transition condition
     * @param dctx The DispatchContext to be used in processing the condition
     * @return The result of the evaluation
     * @throws EvaluationException
     */
    public Boolean evaluateCondition(Map context, Map attrs, String expression, DispatchContext dctx) throws EvaluationException;

}
