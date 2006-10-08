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
package org.ofbiz.content.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;



/**
 * SearchServices Class
 */
public class SearchServices {

    public static final String module = SearchServices.class.getName();
	
    public static Map indexTree(DispatchContext dctx, Map context) {

        String siteId = (String)context.get("contentId");
        String path = (String)context.get("path");
        Map envContext = new HashMap();
        GenericDelegator delegator = dctx.getDelegator();
  	if (Debug.infoOn()) Debug.logInfo("in indexTree, siteId:" + siteId, module);
            List badIndexList = new ArrayList();
            envContext.put("badIndexList", badIndexList);
            envContext.put("goodIndexCount", new Integer(0));

        Map results = null;
        try {
            results = SearchWorker.indexTree(delegator, siteId, envContext, path);
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError("Error indexing tree: " + e.toString());
        }
	  	if (Debug.infoOn()) Debug.logInfo("in indexTree, results:" + results, module);
        return results;
    }
}
