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

/*
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
import org.ofbiz.widget.html.*;
import org.ofbiz.widget.form.*;
import org.ofbiz.securityext.login.*;
import org.ofbiz.common.*;
import org.ofbiz.entity.model.*;

import org.ofbiz.widget.html.HtmlMenuWrapper;
import org.ofbiz.widget.WidgetWorker;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;

import freemarker.template.SimpleHash;
import freemarker.template.SimpleSequence;

import javax.servlet.*;
import javax.servlet.http.*;
*/

import org.ofbiz.base.util.*
import org.ofbiz.entity.util.*
import org.ofbiz.content.ContentManagementWorker;
import org.ofbiz.content.content.ContentWorker;

Debug.logInfo("in mostrecentprep(1):","");
paramMap = UtilHttp.getParameterMap(request);
forumId = ContentManagementWorker.getFromSomewhere("moderatedSiteId", paramMap, request, context);

if (forumId) {
    exprList = [] as ArrayList;
    fromDate = UtilDateTime.nowTimestamp();
    thruExpr2 = EntityCondition.makeCondition("caThruDate", EntityOperator.EQUALS, null);
    exprList.add(thruExpr2);
    statusIdExpr = EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "CTNT_IN_PROGRESS");
    exprList.add(statusIdExpr);
    contentIdToExpr = EntityCondition.makeCondition("caContentIdTo", EntityOperator.EQUALS, forumId);
    exprList.add(contentIdToExpr);
    expr = EntityCondition.makeCondition(exprList, EntityOperator.AND);
    entityList = delegator.findList("ContentAssocViewFrom", expr, null, ['-caFromDate'], null, false);
    
    Debug.logInfo("in mostrecentprep(1), entityList.size():" + entityList.size(),"");
    Debug.logInfo("in mostrecentprep(1), entityList:" + entityList,"");
    context.mostRecentList = entityList;
}