/*
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
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.ofbiz.base.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.security.*;
import org.ofbiz.service.*;
import org.ofbiz.entity.model.*;
import org.ofbiz.content.ContentManagementWorker;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.base.util.collections.LifoSet;

import javax.servlet.*;
import javax.servlet.http.*;

lookupCaches = session.getAttribute("lookupCaches");
//org.ofbiz.base.util.Debug.logInfo("entityName:" + entityName, "");
//org.ofbiz.base.util.Debug.logInfo("in MruAdd.groovy, lookupCaches:" + lookupCaches, "");

if (!lookupCaches) {
    lookupCaches = [:];
    session.setAttribute("lookupCaches", lookupCaches);
}

cacheEntityName = entityName;
//Debug.logInfo("cacheEntityName:" + cacheEntityName, "");
lifoSet = lookupCaches[cacheEntityName];
//org.ofbiz.base.util.Debug.logInfo("lifoSet:" + lifoSet, "");
if (!lifoSet) {
    lifoSet = new LifoSet(10);
    lookupCaches[cacheEntityName] = lifoSet;
}

paramMap = UtilHttp.getParameterMap(request);
contentId = paramMap.contentId;
contentAssocDataResourceViewFrom = ContentWorker.getSubContentCache(delegator, null, null, contentId, null, null, null, null, null);
//Debug.logInfo("in mruadd, contentAssocDataResourceViewFrom :" + contentAssocDataResourceViewFrom , "");
if (contentAssocDataResourceViewFrom) {
    lookupCaches = session.getAttribute("lookupCaches");
    viewPK = contentAssocDataResourceViewFrom.getPrimaryKey();
    //Debug.logInfo("in mruadd, viewPK :" + viewPK , "");
    if (viewPK) {
        ContentManagementWorker.mruAdd(session, viewPK);
    }
}
