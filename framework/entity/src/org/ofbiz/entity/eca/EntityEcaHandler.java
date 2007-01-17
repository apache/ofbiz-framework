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
package org.ofbiz.entity.eca;

import java.util.*;
import org.ofbiz.entity.*;

/**
 * EntityEcaHandler interface
 *
 */
public interface EntityEcaHandler {
    
    public static final String EV_VALIDATE = "validate";
    public static final String EV_RUN = "run";
    public static final String EV_RETURN = "return";
    public static final String EV_CACHE_CLEAR = "cache-clear";
    public static final String EV_CACHE_CHECK = "cache-check";
    public static final String EV_CACHE_PUT = "cache-put";
    
    public static final String OP_CREATE = "create";
    public static final String OP_STORE = "store";
    public static final String OP_REMOVE = "remove";
    public static final String OP_FIND = "find";
    

    public void setDelegator(GenericDelegator delegator);

    public Map getEntityEventMap(String entityName);

    public void evalRules(String currentOperation, Map eventMap, String event, GenericEntity value, boolean isError) throws GenericEntityException;
}
