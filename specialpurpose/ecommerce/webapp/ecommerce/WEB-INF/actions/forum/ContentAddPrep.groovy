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
import org.ofbiz.entity.model.*;
import org.ofbiz.content.ContentManagementWorker;
import org.ofbiz.content.content.ContentWorker;

import freemarker.template.SimpleHash;
import freemarker.template.SimpleSequence;

import javax.servlet.*;
import javax.servlet.http.*;

singleWrapper = context.singleWrapper;
singleWrapper.putInContext("contentPurposeTypeId", page.contentPurposeTypeId);
singleWrapper.putInContext("contentAssocTypeId", page.contentAssocTypeId);
rootPubPt = parameters.webSiteId;
paramMap = UtilHttp.getParameterMap(request);
contentIdTo = ContentManagementWorker.getFromSomewhere("forumId", paramMap, request, context);
context.contentIdTo = contentIdTo;
//Debug.logInfo("in contentaddprep, contentIdTo:" + contentIdTo,"");
//Debug.logInfo("in contentaddprep, paramMap:" + paramMap,"");
attrList = from("ContentAttribute").where("contentId", contentIdTo, "attrName", "publishOperation").cache(true).queryList();
publishOperation = null;
if (attrList) {
    contentAttribute = attrList.get(0);
    publishOperation = contentAttribute.attrValue;
    //Debug.logInfo("in contentaddprep, publishOperation:" + publishOperation,"");
}

singleWrapper.putInContext("publishOperation", publishOperation);
singleWrapper.putInContext("contentIdTo", contentIdTo);
//singleWrapper.putInContext("ownerContentId", contentIdTo);
summaryDataResourceTypeId = page.summaryDataResourceTypeId;
singleWrapper.putInContext("summaryDataResourceTypeId", summaryDataResourceTypeId);
targetOperation = page.targetOperation ?: "CONTENT_CREATE";

singleWrapper.putInContext("targetOperation", targetOperation);
singleWrapper.putInContext("contentTypeId", "DOCUMENT");
contentPurpose = page.contentPurpose ?: "ARTICLE";

singleWrapper.putInContext("contentPurpose", contentPurpose);
singleWrapper.putInContext("forumId", contentIdTo);

forumContent = from("Content").where("contentId", contentIdTo).cache(true).queryOne();
statusId = "CTNT_PUBLISHED";
if (forumContent) {
    statusId = forumContent.statusId;
    if (!statusId) {
        statusId = page.statusId;
    }
    if (!statusId) {
        statusId = "CTNT_PUBLISHED";
    }
}
singleWrapper.putInContext("statusId", statusId);

siteAncestorList = [];
siteAncestorList.add(contentIdTo);
context.siteAncestorList = siteAncestorList;
//Debug.logInfo("in viewprep, siteAncestorList:" + siteAncestorList,"");
