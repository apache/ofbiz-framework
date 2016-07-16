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
package org.apache.ofbiz.entity.eca;

import java.util.List;
import java.util.Map;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericEntityException;

/**
 * EntityEcaHandler interface
 *
 */
public interface EntityEcaHandler<T> {

    public static final String EV_VALIDATE = "validate";
    public static final String EV_RUN = "run";
    public static final String EV_RETURN = "return";
    /**
     * Invoked after the entity operation, but before the cache is cleared.
     */
    public static final String EV_CACHE_CLEAR = "cache-clear";
    public static final String EV_CACHE_CHECK = "cache-check";
    public static final String EV_CACHE_PUT = "cache-put";

    public static final String OP_CREATE = "create";
    public static final String OP_STORE = "store";
    public static final String OP_REMOVE = "remove";
    public static final String OP_FIND = "find";


    public void setDelegator(Delegator delegator);

    public Map<String, List<T>> getEntityEventMap(String entityName);

    public void evalRules(String currentOperation, Map<String, List<T>> eventMap, String event, GenericEntity value, boolean isError) throws GenericEntityException;
}
