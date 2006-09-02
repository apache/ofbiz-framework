/*
 * Copyright 2001-2006 The Apache Software Foundation
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
package org.ofbiz.entity.util;

import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericPK;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;

/**
 * Distributed Cache Clear interface definition
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public interface DistributedCacheClear {

    public void setDelegator(GenericDelegator delegator, String userLoginId);

    public void distributedClearCacheLine(GenericValue value);

    public void distributedClearCacheLineFlexible(GenericEntity dummyPK);

    public void distributedClearCacheLineByCondition(String entityName, EntityCondition condition);

    public void distributedClearCacheLine(GenericPK primaryKey);
    
    public void clearAllCaches();
}
