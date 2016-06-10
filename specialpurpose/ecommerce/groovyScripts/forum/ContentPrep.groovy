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
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.entity.*;
import org.ofbiz.security.*;
import org.ofbiz.service.*;
import org.ofbiz.entity.model.*;
import org.ofbiz.widget.renderer.html.HtmlFormWrapper;
import org.ofbiz.content.data.DataResourceWorker;
import org.ofbiz.webapp.ftl.FreeMarkerViewHandler;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.content.ContentManagementWorker;

import java.io.StringWriter;
import freemarker.template.SimpleHash;
import freemarker.template.WrappingTemplateModel;


import javax.servlet.*;
import javax.servlet.http.*;

// load edit or create Content form

rootPubPt = parameters.webSiteId;
//Debug.logInfo("in contentprep, security:" + security, "");

singleWrapper = context.singleWrapper;

paramMap = UtilHttp.getParameterMap(request);
contentId = "";
contentId = ContentManagementWorker.getFromSomewhere("masterContentId", paramMap, request, context);
if (!contentId)
    contentId = ContentManagementWorker.getFromSomewhere("contentIdTo", paramMap, request, context);
if (!contentId)
    contentId = ContentManagementWorker.getFromSomewhere("contentId", paramMap, request, context);

//Debug.logInfo("in contentprep, contentId(1):" + contentId, "");
currentValue = parameters.currentValue;
//Debug.logInfo("in contentprep, currentValue(0):" + currentValue, "");

if (!contentId && currentValue) {
    contentId = currentValue.contentId;
}
if (contentId && !currentValue) {
    currentValue = from("Content").where("contentId", contentId).cache(true).queryOne();
}
//Debug.logInfo("in contentprep, currentValue(1):" + currentValue, "");
//Debug.logInfo("in contentprep, contentId(4):" + contentId, "");

if (currentValue) {
    dataResourceId = currentValue.dataResourceId;
    context.contentId = contentId;
    context.contentName = currentValue.contentName;
    context.description = currentValue.description;
    context.statusId = currentValue.statusId;

    mimeTypeId =  currentValue.mimeTypeId;
    rootDir = request.getSession().getServletContext().getRealPath("/");
    wrapper = FreeMarkerWorker.getDefaultOfbizWrapper();
    WrappingTemplateModel.setDefaultObjectWrapper(wrapper);
    //templateRoot = new SimpleHash(wrapper);
    templateRoot = [:];
    FreeMarkerViewHandler.prepOfbizRoot(templateRoot, request, response);

    ctx = [:];
    ctx.rootDir = rootDir;
    // webSiteId and https need to go here, too
    templateRoot.context = ctx;
    fromDate = nowTimestamp;
    assocTypes = null;
    //assocTypes = ["SUB_CONTENT"];
    subContentDataResourceView = ContentWorker.getSubContent(delegator, contentId, "ARTICLE", null, userLogin, assocTypes, fromDate);
    if (subContentDataResourceView) {
        out = new StringWriter();
        ContentWorker.renderContentAsText(dispatcher, delegator, null, out, templateRoot, subContentDataResourceView, locale, mimeTypeId, true);
        textData = out.toString();
        context.txtContentId = subContentDataResourceView.contentId;
        context.txtDataResourceId = subContentDataResourceView.dataResourceId;
        context.textData = textData;
        //Debug.logInfo("textId:" + txtContentId, "");
        //Debug.logInfo("textData:" + textData, "");
        if (singleWrapper) {
           //Debug.logInfo("textData:" + textData, "");
           singleWrapper.putInContext("textData", textData);
        }
    }

    subContentDataResourceView = ContentWorker.getSubContent(dispatcher, delegator, contentId, "SUMMARY", null, userLogin, assocTypes, fromDate, true);
    if (subContentDataResourceView) {
        out = new StringWriter();
        ContentWorker.renderContentAsText(delegator, null, out, templateRoot, subContentDataResourceView, locale, mimeTypeId);
        summaryData = out.toString();
        context.sumContentId = subContentDataResourceView.contentId;
        context.sumDataResourceId = subContentDataResourceView.dataResourceId;
        context.summaryData = summaryData;
        //Debug.logInfo("sumId:" + sumContentId, "");
        //Debug.logInfo("summaryData:" + summaryData, "");
        if (singleWrapper) {
            //Debug.logInfo("summaryData:" + summaryData, "");
            singleWrapper.putInContext("summaryData", summaryData);
        }
    }

    subContentDataResourceView = ContentWorker.getSubContent(dispatcher, delegator, contentId, "IMAGE", null, userLogin, assocTypes, fromDate, true);
    if (subContentDataResourceView) {
        out = new StringWriter();
        ContentWorker.renderContentAsText(delegator, null, out, templateRoot, subContentDataResourceView, locale, mimeTypeId);
        imageData = out.toString();
        context.imgContentId = subContentDataResourceView.contentId;
        context.imgDataResourceId = subContentDataResourceView.dataResourceId;
        context.imageData = imageData;
    }
}
