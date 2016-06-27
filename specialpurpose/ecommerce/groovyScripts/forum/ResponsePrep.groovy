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
import org.ofbiz.securityext.login.*;
import org.ofbiz.common.*;
import org.ofbiz.content.content.ContentWorker;

import javax.servlet.*;
import javax.servlet.http.*;

paramMap = UtilHttp.getParameterMap(request);


contentIdTo = paramMap.contentIdTo;
nodeTrailCsv = paramMap.nodeTrailCsv;
//Debug.logInfo("in viewprep, nodeTrailCsv:" + nodeTrailCsv,"");
context.nodeTrailCsv = nodeTrailCsv;
contentId = paramMap.contentIdTo;
context.subContentId = contentId;
context.contentIdTo = contentId;

trail = StringUtil.split(nodeTrailCsv, ",");
siteAncestorList = [];
idList = [];
if (trail) {
    pubPt = trail[0];
    ContentWorker.getContentAncestry(delegator, pubPt, "SUBSITE", "To", idList);
    idList.add(pubPt);
}

// start at 1 to skip webSiteId
idList.each { id ->
    webSitePublishPoint = from("WebSitePublishPoint").where("contentId", id).cache(true).queryOne();
    siteAncestorList.add(webSitePublishPoint);
}
context.siteAncestorList = siteAncestorList;
//Debug.logInfo("in viewprep, siteAncestorList:" + siteAncestorList,"");

singleWrapper = context.singleWrapper;
// singleWrapper.putInContext("pageName", page.getPageName());
singleWrapper.putInContext("contentIdTo", contentIdTo);
singleWrapper.putInContext("summaryDataResourceTypeId", null);
singleWrapper.putInContext("mapKey", null);
singleWrapper.putInContext("nodeTrailCsv", nodeTrailCsv);
