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

import org.ofbiz.base.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.security.*;
import org.ofbiz.service.*;
import org.ofbiz.entity.model.*;
import org.ofbiz.content.data.DataResourceWorker;
import org.ofbiz.webapp.ftl.FreeMarkerViewHandler;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.content.ContentManagementWorker;

import java.io.StringWriter;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.WrappingTemplateModel;

import javax.servlet.*;
import javax.servlet.http.*;

// load edit or create Content form

//Debug.logInfo("in ownerprep, security:" + security, "");

rootPubPt = parameters.webSiteId;
//Debug.logInfo("in ownerprep, rootPubPt:" + rootPubPt, "");
entityAction = page.entityOperation;
permittedOperations = page.permittedOperations;

allDepartmentContentList = ContentManagementWorker.getAllDepartmentContent(delegator, rootPubPt);
//Debug.logInfo("in ownercontentprep, allDepartmentContentList:" + allDepartmentContentList, "");
departmentPointList = ContentManagementWorker.getPermittedDepartmentPoints( delegator, allDepartmentContentList, userLogin, security, entityAction, "CONTENT_CREATE", null );
//Debug.logInfo("in ownercontentprep, departmentPointList:" + departmentPointList, "");
departmentPointMap = [:];
departmentPointMapAll = [:];
ownerContentList = [];
departmentPointList.each { arr ->
    contentId = arr[0];
    description = arr[1];
    subPointList = [];
    lineMap = [:];
    lineMap.contentId = contentId;
    lineMap.description = description.toUpperCase();
    ownerContentList.add(lineMap);
    subDepartmentContentList = ContentManagementWorker.getAllDepartmentContent(delegator, contentId);
    subDepartmentContentList.each { departmentPoint2 ->
        contentId2 = departmentPoint2.contentId;
        description2 = departmentPoint2.templateTitle;
        lineMap2 = [:];
        lineMap2.contentId = contentId2;
        lineMap2.description = "&nbsp;&nbsp;&nbsp;-" + description2;
        ownerContentList.add(lineMap2);
    }
}
//Debug.logInfo("in ownercontentprep, ownerContentList:" + ownerContentList, "");

pubPt = context.pubPt;
//Debug.logInfo("in ownercontentprep, pubPt:" + pubPt, "");
singleWrapper = context.singleWrapper;
singleWrapper.putInContext("ownerContentList", ownerContentList);
singleWrapper.putInContext("pubPt", pubPt);
