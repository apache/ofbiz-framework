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
import org.ofbiz.widget.renderer.html.HtmlFormWrapper;
import org.ofbiz.securityext.login.*;
import org.ofbiz.common.*;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.content.ContentManagementWorker;

import javax.servlet.*;
import javax.servlet.http.*;

paramMap = UtilHttp.getParameterMap(request);

// Strip old VIEW_INDEX from query string if present
//since we are adding them again.
temp = request.getQueryString();
queryString = UtilHttp.stripViewParamsFromQueryString(temp);
//Debug.logInfo("in viewprep, queryString(1):" + queryString,"");
context.queryString = queryString;
//Debug.logInfo("in viewprep, queryString(2):" + queryString,"");

requestURL = request.getRequestURL();
//Debug.logInfo("in viewprep, requestURL(3):" + requestURL,"");
context.requestURL = requestURL;
viewSize = paramMap.VIEW_SIZE;
context.viewSize = viewSize;
//Debug.logInfo("in viewprep, viewSize(3):" + viewSize,"");
viewIndex = paramMap.VIEW_INDEX;
context.viewIndex = viewIndex;
//Debug.logInfo("in viewprep, viewIndex(3):" + viewIndex,"");

nodeTrailCsv = ContentManagementWorker.getFromSomewhere("nodeTrailCsv", paramMap, request, context);
context.nodeTrailCsv = nodeTrailCsv;
contentId = ContentManagementWorker.getFromSomewhere("contentId", paramMap, request, context);
//Debug.logInfo("in viewprep, contentId(3):" + contentId,"");
context.subContentId = contentId;
context.contentIdTo = contentId;
forumId = ContentManagementWorker.getFromSomewhere("forumId", paramMap, request, context);
//forumContent = delegator.findOne("Content", [contentId : forumId], true);
//context.forumContent = forumContent;
