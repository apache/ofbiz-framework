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
import org.ofbiz.content.content.PermissionRecorder;
import org.ofbiz.content.ContentManagementWorker;

import javax.servlet.*;
import javax.servlet.http.*;

paramMap = UtilHttp.getParameterMap(request);
//Debug.logInfo("in permprep, userLogin(0):" + userLogin, null);

// Get permission from pagedef config file
permission = context.permission;
permissionType = context.permissionType ?: "simple";

entityName = context.entityName;
entityOperation = context.entityOperation;
targetOperation = context.targetOperation;
//Debug.logInfo("in permprep, targetOperation(0):" + targetOperation, null);

mode = paramMap.mode;
//Debug.logInfo("in permprep, contentId(0):" + request.getAttribute("contentId"),"");
currentValue = request.getAttribute("currentValue");
//Debug.logInfo("in permprep, paramMap(1):" + paramMap, null);
//Debug.logInfo("in permprep, currentValue(1):" + currentValue, null);

if ("add".equals(mode)) {
    entityOperation = context.addEntityOperation ?: context.entityOperation ?: "_CREATE";
    targetOperation = context.addTargetOperation ?: context.get("targetOperation") ?: "CONTENT_CREATE";
    //org.ofbiz.base.util.Debug.logInfo("in permprep, targetOperation:" + targetOperation, null);
} else {
    if (!entityOperation) {
        entityOperation = "_UPDATE";
    }
    if (!targetOperation) {
        targetOperation = "CONTENT_UPDATE";
    }
}

if (permissionType.equals("complex")) {
    mapIn = [:];
    mapIn.userLogin = userLogin;
    targetOperationList = StringUtil.split(targetOperation, "|");
    mapIn.targetOperationList = targetOperationList;
    thisContentId = null;

    //Debug.logInfo("in permprep, userLogin(1):" + userLogin, null);
    //if (userLogin != null) {
        //Debug.logInfo("in permprep, userLoginId(1):" + userLogin.get("userLoginId"), null);
    //}
    if (!currentValue || !"Content".equals(entityName)) {
        permissionIdName = context.permissionIdName;
        //org.ofbiz.base.util.Debug.logInfo("in permprep, permissionIdName(1):" + permissionIdName, null);
        if (!permissionIdName) {
            thisContentId = ContentManagementWorker.getFromSomewhere(permissionIdName, paramMap, request, context);
        } else if (!thisContentId) {
            thisContentId = ContentManagementWorker.getFromSomewhere("subContentId", paramMap, request, context);
        } else if (!thisContentId) {
            thisContentId = ContentManagementWorker.getFromSomewhere("contentIdTo", paramMap, request, context);
        } else if (!thisContentId) {
            thisContentId = ContentManagementWorker.getFromSomewhere("contentId", paramMap, request, context);
        }
        //org.ofbiz.base.util.Debug.logInfo("in permprep, thisContentId(2):" + thisContentId, null);
    } else {
        thisContentId = currentValue.contentId;
    }
    //org.ofbiz.base.util.Debug.logInfo("in permprep, thisContentId(3):" + thisContentId, null);

    if (!currentValue || !"Content".equals(entityName)) {
        if (thisContentId) {
            currentValue = from("Content").where("contentId", thisContentId).queryOne();
        }
    }
    if ("add".equals(mode)) {
        addEntityOperation = context.addEntityOperation;
        if (addEntityOperation) {
            entityOperation = addEntityOperation;
        }
    } else {
        editEntityOperation = context.editEntityOperation;
        if (editEntityOperation) {
            entityOperation = editEntityOperation;
        }
    }
    //org.ofbiz.base.util.Debug.logInfo("in permprep, currentValue(2):" + currentValue, null);
    if ("Content".equals(currentValue?.getEntityName())) {
        mapIn.currentContent = currentValue;
    }
    mapIn.entityOperation = entityOperation;

    contentPurposeTypeId = context.contentPurposeTypeId;
    if (contentPurposeTypeId) {
        mapIncontentPurposeList = StringUtil.split(contentPurposeTypeId, "|");
    }

    //org.ofbiz.base.util.Debug.logInfo("in permprep, mapIn:" + mapIn, null);
    result = runService('checkContentPermission', mapIn);
    permissionStatus = result.permissionStatus;
    //org.ofbiz.base.util.Debug.logInfo("in permprep, permissionStatus:" + permissionStatus, null);
    if ("granted".equals(permissionStatus)) {
        context.hasPermission = true;
        request.setAttribute("hasPermission", true);
        request.setAttribute("permissionStatus", "granted");
    } else {
        context.hasPermission = false;
        request.setAttribute("hasPermission", false);
        request.setAttribute("permissionStatus", "");
        errorMessage = "Permission to display:" + page.getPageName() + " is denied.";
        recorder = result.permissionRecorder;
        //Debug.logInfo("recorder(0):" + recorder, "");
        if (recorder) {
            permissionMessage = recorder.toHtml();
            //Debug.logInfo("permissionMessage(0):" + permissionMessage, "");
            errorMessage += " \n " + permissionMessage;
        }
        request.setAttribute("errorMsgReq", errorMessage);
    }
    //Debug.logInfo("in permprep, contentId(1):" + request.getAttribute("contentId"),"");
} else {
    //org.ofbiz.base.util.Debug.logInfo("permission:" + permission , null);
    //org.ofbiz.base.util.Debug.logInfo("entityOperation:" + entityOperation , null);
    if (security.hasEntityPermission(permission, entityOperation, session)) {
        //org.ofbiz.base.util.Debug.logInfo("hasEntityPermission is true:" , null);
        context.hasPermission = true;
        request.setAttribute("hasPermission", true);
        request.setAttribute("permissionStatus", "granted");
    } else {
        //org.ofbiz.base.util.Debug.logInfo("hasEntityPermission is false:" , null);
        context.hasPermission = false;
        request.setAttribute("hasPermission", false);
        request.setAttribute("permissionStatus", "");
    }
}
