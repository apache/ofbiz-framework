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
import org.ofbiz.content.content.PermissionRecorder;

import javax.servlet.*;
import javax.servlet.http.*;

paramMap = UtilHttp.getParameterMap(request);

contentIdTo = paramMap.contentIdTo;
if (!contentIdTo) {
    request.setAttribute("errorMsgReq", "contentIdTo is empty");
    return;
}

/*
pubPt = paramMap.pubPt ?: context.pubPt;
if (!pubPt) {
    request.setAttribute("errorMsgReq", "pubPt is empty");
    return;
}
*/

contentToValue = from("Content").where("contentId", contentIdTo).queryOne();
contentToPurposeList = contentToValue.getRelated("ContentPurpose", null, null, true);
currentValue = delegator.makeValue("Content", [contentTypeId : "DOCUMENT", statusId : "CTNT_PUBLISHED", privilegeEnumId : "_00_"]);

if (contentToPurposeList.contains("RESPONSE")) {
    ownerContentId = contentToValue.ownerContentId;
    currentValue.ownerContentId = ownerContentId;
} else {
    contentId = contentToValue.contentId;
    currentValueownerContentId = contentId;
}

mapIn = [:];
mapIn.userLogin = userLogin;
targetOperationList = StringUtil.split(context.targetOperation, "|");
mapIn.targetOperationList = targetOperationList;

if (currentValue) {
    mapIn.currentContent = currentValue;
}
mapIn.entityOperation = "_CREATE";
mapIn.contentPurposeList = ["RESPONSE"];

//org.ofbiz.base.util.Debug.logInfo("in permprep, mapIn:" + mapIn, null);
result = runService('checkContentPermission', mapIn);
permissionStatus = result.permissionStatus;
//org.ofbiz.base.util.Debug.logInfo("permissionStatus:" + permissionStatus, null);
if (!"granted".equals(permissionStatus)) {
    request.setAttribute("errorMsgReq", "Permission to add response is denied (1)");
    errorMessage = "Permission to add response is denied (2)";
    recorder = result.permissionRecorder;
    //Debug.logInfo("recorder(0):" + recorder, "");
    if (recorder) {
        permissionMessage = recorder.toHtml();
        //Debug.logInfo("permissionMessage(0):" + permissionMessage, "");
        errorMessage += " \n " + permissionMessage;
    }
    request.setAttribute("permissionErrorMsg", errorMessage);
    context.permissionErrorMsg = errorMessage;
    context.hasPermission = false;
    request.setAttribute("hasPermission", false);
    request.setAttribute("permissionStatus", "");
    return;
} else {
    context.hasPermission = true;
    request.setAttribute("hasPermission", true);
    request.setAttribute("permissionStatus", "granted");
}

/*
pubContentValue = delegator.findOne("Content", [contentId : pubPt], false);
if (pubContentValue) {
    mapIn.currentContent = pubContentValue;
    mapIn.statusId = "CTNT_PUBLISHED";
}
//org.ofbiz.base.util.Debug.logInfo("in permprep(2), mapIn:" + mapIn, null);
result = dispatcher.runSync("checkContentPermission", mapIn);
permissionStatus = result.permissionStatus;
//org.ofbiz.base.util.Debug.logInfo("permissionStatus(2):" + permissionStatus, null);
if (!"granted".equals(permissionStatus)) {

    request.setAttribute("errorMsgReq", "Permission to add response is denied (2)");
    errorMessage = "Permission to add response is denied (2)";
    recorder = result.permissionRecorder;
        //Debug.logInfo("recorder(0):" + recorder, "");
    if (recorder) {
        permissionMessage = recorder.toHtml();
        //Debug.logInfo("permissionMessage(0):" + permissionMessage, "");
        errorMessage += " \n " + permissionMessage;
    }
    request.setAttribute("permissionErrorMsg", errorMessage);
    context.permissionErrorMsg = errorMessage;
    context.hasPermission = false;
    request.setAttribute("hasPermission", false);
    request.setAttribute("permissionStatus", "");
    return;
} else {
        context.hasPermission = true;
        request.setAttribute("hasPermission", true);
        request.setAttribute("permissionStatus", "granted");
}
*/

request.setAttribute("currentValue", currentValue);
singleWrapper = context.singleWrapper;
singleWrapper.putInContext("contentPurposeTypeId", context.contentPurposeTypeId);
singleWrapper.putInContext("targetOperation", context.targetOperation);
singleWrapper.putInContext("targetOperationString", context.targetOperation);
singleWrapper.putInContext("currentValue", currentValue);

trailList = context.trailList;
replyName = null;
if (trailList) {
    idNamePair = trailList[trailList.size() -1];
    replyName = idNamePair[1];
    if (!replyName.contains("RE:")) {
        replyName = "RE:" + replyName;
    }
}
singleWrapper.putInContext("replyName", [contentName : replyName, description : replyName]);
