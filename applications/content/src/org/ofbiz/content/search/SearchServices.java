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
package org.ofbiz.content.search;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.Delegator;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.LocalDispatcher;


/**
 * SearchServices Class
 */
public class SearchServices {

    public static final String module = SearchServices.class.getName();
    public static final String resource = "ContentUiLabels";

    public static Map<String, Object> indexTree(DispatchContext dctx, Map<String, ? extends Object> context) {
        Date start = new Date();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        String siteId = (String) context.get("contentId");
        String path = (String) context.get("path");
        Locale locale = (Locale) context.get("locale");
        if (path == null) {
            path = SearchWorker.getIndexPath(path);
        }
        Map<String, Object> envContext = FastMap.newInstance();

        if (Debug.infoOn()) Debug.logInfo("in indexTree, siteId:" + siteId, module);
        List<String> badIndexList = FastList.newInstance();
        envContext.put("badIndexList", badIndexList);
        envContext.put("goodIndexCount", Integer.valueOf(0));

        Map<String, Object> results;
        try {
            results = SearchWorker.indexTree(dispatcher, delegator, siteId, envContext, path);
        } catch (Exception e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "ContentIndexingTreeError", UtilMisc.toMap("errorString", e.toString()), locale));
        }
        Date end = new Date();
        if (Debug.infoOn()) Debug.logInfo("in indexTree, results:" + results, module);
        if (Debug.infoOn()) Debug.logInfo("Indexing done in: " + (end.getTime()-start.getTime()) + " ms", module);
        return results;
    }
}
