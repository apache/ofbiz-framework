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
package org.ofbiz.content.webapp.ftl;

import java.io.IOException;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.webapp.ftl.LoopWriter;
import org.ofbiz.service.LocalDispatcher;

import freemarker.core.Environment;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTransformModel;
import freemarker.template.TransformControl;

/**
 * TraverseSubContentTransform - Freemarker Transform for URLs (links)
 */
public class TraverseSubContentTransform implements TemplateTransformModel {

    public static final String module = TraverseSubContentTransform.class.getName();
    public static final String [] saveKeyNames = {"contentId", "subContentId", "mimeType", "subContentDataResourceView", "wrapTemplateId", "templateContentId", "pickWhen", "followWhen", "returnAfterPickWhen", "returnBeforePickWhen", "indent"};
    public static final String [] removeKeyNames = {"templateContentId", "subDataResourceTypeId", "mapKey", "wrappedFTL", "nodeTrail"};

    /**
     * A wrapper for the FreeMarkerWorker version.
     */
    public static Object getWrappedObject(String varName, Environment env) {
        return FreeMarkerWorker.getWrappedObject(varName, env);
    }

    public static String getArg(Map args, String key, Environment env) {
        return FreeMarkerWorker.getArg(args, key, env);
    }

    public static String getArg(Map args, String key, Map ctx) {
        return FreeMarkerWorker.getArg(args, key, ctx);
    }

    public Writer getWriter(final Writer out, Map args) {
        final StringBuffer buf = new StringBuffer();
        final Environment env = Environment.getCurrentEnvironment();
        final Map templateCtx = (Map) FreeMarkerWorker.getWrappedObject("context", env);
        //FreeMarkerWorker.convertContext(templateCtx);
        final Map savedValues = FreeMarkerWorker.saveValues(templateCtx, saveKeyNames);
        FreeMarkerWorker.overrideWithArgs(templateCtx, args);
        final GenericDelegator delegator = (GenericDelegator) FreeMarkerWorker.getWrappedObject("delegator", env);
/*
        final String editTemplate = getArg(args, "editTemplate", ctx);
        final String wrapTemplateId = getArg(args, "wrapTemplateId", ctx);
        //final String mapKey = getArg(args, "mapKey", ctx);
        final String templateContentId = getArg(args, "templateContentId", ctx);
        final String subDataResourceTypeId = getArg(args, "subDataResourceTypeId", ctx);
        final String contentId = getArg(args, "contentId", ctx);
        final String subContentId = getArg(args, "subContentId", ctx);
        final String rootDir = getArg(args, "rootDir", ctx);
        final String webSiteId = getArg(args, "webSiteId", ctx);
        final String https = getArg(args, "https", ctx);
        final String viewSize = getArg(args, "viewSize", ctx);
        final String viewIndex = getArg(args, "viewIndex", ctx);
        final String listSize = getArg(args, "listSize", ctx);
        final String highIndex = getArg(args, "highIndex", ctx);
        final String lowIndex = getArg(args, "lowIndex", ctx);
        final String queryString = getArg(args, "queryString", ctx);
        final Locale locale = (Locale) FreeMarkerWorker.getWrappedObject("locale", env);
        final String mimeTypeId = getArg(args, "mimeTypeId", ctx);
*/
        final LocalDispatcher dispatcher = (LocalDispatcher) FreeMarkerWorker.getWrappedObject("dispatcher", env);
        //final GenericValue userLogin = (GenericValue) FreeMarkerWorker.getWrappedObject("userLogin", env);
        GenericValue view = (GenericValue) FreeMarkerWorker.getWrappedObject("subContentDataResourceView", env);
        final Integer indent = (templateCtx.get("indent") == null) ? new Integer(0) : (Integer)templateCtx.get("indent");
       
        String contentId = (String)templateCtx.get("contentId");
        String subContentId = (String)templateCtx.get("subContentId");
        if (view == null) {
            String thisContentId = subContentId;
            if (UtilValidate.isEmpty(thisContentId)) 
                thisContentId = contentId;

            if (UtilValidate.isNotEmpty(thisContentId)) {
            
                try {
                    view = delegator.findByPrimaryKey("Content", UtilMisc.toMap("contentId", thisContentId));
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Error getting sub-content", module);
                    throw new RuntimeException(e.getMessage());
                }
            }
        }

        final GenericValue subContentDataResourceView = view;

        final Map traverseContext = new HashMap();
        traverseContext.put("delegator", delegator);
        Map whenMap = new HashMap();
        whenMap.put("followWhen", (String)templateCtx.get( "followWhen"));
        whenMap.put("pickWhen", (String)templateCtx.get( "pickWhen"));
        whenMap.put("returnBeforePickWhen", (String)templateCtx.get( "returnBeforePickWhen"));
        whenMap.put("returnAfterPickWhen", (String)templateCtx.get( "returnAfterPickWhen"));
        traverseContext.put("whenMap", whenMap);
        String fromDateStr = (String)templateCtx.get( "fromDateStr");
        String thruDateStr = (String)templateCtx.get( "thruDateStr");
        Timestamp fromDate = null;
        if (fromDateStr != null && fromDateStr.length() > 0) {
            fromDate = UtilDateTime.toTimestamp(fromDateStr);
        }
        traverseContext.put("fromDate", fromDate);
        Timestamp thruDate = null;
        if (thruDateStr != null && thruDateStr.length() > 0) {
            thruDate = UtilDateTime.toTimestamp(thruDateStr);
        }
        traverseContext.put("thruDate", thruDate);
        String startContentAssocTypeId = (String)templateCtx.get( "contentAssocTypeId");
        if (startContentAssocTypeId != null)
            startContentAssocTypeId = "SUB_CONTENT";
        traverseContext.put("contentAssocTypeId", startContentAssocTypeId);
        String direction = (String)templateCtx.get( "direction");
        if (UtilValidate.isEmpty(direction)) 
            direction = "From";
        traverseContext.put("direction", direction);


        return new LoopWriter(out) {

            public void write(char cbuf[], int off, int len) {
                //StringBuffer ctxBuf = (StringBuffer) templateContext.get("buf");
                //ctxBuf.append(cbuf, off, len);
                buf.append(cbuf, off, len);
            }

            public void flush() throws IOException {
                out.flush();
            }

            public int onStart() throws TemplateModelException, IOException {
                //templateContext.put("buf", new StringBuffer());
                List nodeTrail = new ArrayList();
                traverseContext.put("nodeTrail", nodeTrail);
                GenericValue content = null;
/*
                if (UtilValidate.isNotEmpty(contentId)) {
                    try {
                        content = delegator.findByPrimaryKey("Content", UtilMisc.toMap("contentId", contentId));
                    } catch(GenericEntityException e){
                        // TODO: Not sure what to put here.
                        throw new RuntimeException(e.getMessage());
                    }
                }
*/
                Map rootNode = ContentWorker.makeNode(subContentDataResourceView);
                ContentWorker.traceNodeTrail("1",nodeTrail);
                ContentWorker.selectKids(rootNode, traverseContext);
                ContentWorker.traceNodeTrail("2",nodeTrail);
                nodeTrail.add(rootNode);
                boolean isPick = checkWhen(subContentDataResourceView, (String)traverseContext.get("contentAssocTypeId"));
                rootNode.put("isPick", new Boolean(isPick));
                if (!isPick) {
                    ContentWorker.traceNodeTrail("3",nodeTrail);
                    isPick = ContentWorker.traverseSubContent(traverseContext);
                    ContentWorker.traceNodeTrail("4",nodeTrail);
                }
                if (isPick) {
                    populateContext(traverseContext, templateCtx);
                    ContentWorker.traceNodeTrail("5",nodeTrail);
                    return TransformControl.EVALUATE_BODY;
                } else {
                    return TransformControl.SKIP_BODY;
                }
            }

            public int afterBody() throws TemplateModelException, IOException {
                //out.write(buf.toString());
                //buf.setLength(0);
                //templateContext.put("buf", new StringBuffer());
                List nodeTrail = (List)traverseContext.get("nodeTrail");
                ContentWorker.traceNodeTrail("6",nodeTrail);
                boolean inProgress = ContentWorker.traverseSubContent(traverseContext);
                ContentWorker.traceNodeTrail("7",nodeTrail);
                if (inProgress) {
                    populateContext(traverseContext, templateCtx);
                    ContentWorker.traceNodeTrail("8",nodeTrail);
                    return TransformControl.REPEAT_EVALUATION;
                } else
                    return TransformControl.END_EVALUATION;
            }

            public void close() throws IOException {

                String wrappedFTL = buf.toString();
                String encloseWrappedText = (String)templateCtx.get("encloseWrappedText");
                if (UtilValidate.isEmpty(encloseWrappedText) || encloseWrappedText.equalsIgnoreCase("false")) {

                    out.write(wrappedFTL);
                    wrappedFTL = null; // So it won't get written again below.
                }
                String wrapTemplateId = (String)templateCtx.get("wrapTemplateId");
                if (UtilValidate.isNotEmpty(wrapTemplateId)) {
                    templateCtx.put("wrappedFTL", wrappedFTL);
                    
                    Map templateRoot = FreeMarkerWorker.createEnvironmentMap(env);
                    
/*
                    templateRoot.put("viewSize", viewSize);
                    templateRoot.put("viewIndex", viewIndex);
                    templateRoot.put("listSize", listSize);
                    templateRoot.put("highIndex", highIndex);
                    templateRoot.put("lowIndex", lowIndex);
                    templateRoot.put("queryString", queryString);
                    templateRoot.put("wrapDataResourceTypeId", subDataResourceTypeId);
                    templateRoot.put("wrapContentIdTo", contentId);
                    templateRoot.put("wrapMimeTypeId", mimeTypeId);
                    //templateRoot.put("wrapMapKey", mapKey);
                    
*/
                    templateRoot.put("context", templateCtx);
                    String mimeTypeId = (String) templateCtx.get("mimeTypeId");
                    Locale locale = (Locale) templateCtx.get("locale");
                    if (locale == null)
                        locale = Locale.getDefault();
                    try {
                        ContentWorker.renderContentAsText(dispatcher, delegator, wrapTemplateId, out, templateRoot, locale, mimeTypeId, true);
                    } catch (GeneralException e) {
                        Debug.logError(e, "Error rendering content", module);
                        throw new IOException("Error rendering content" + e.toString());
                    }
/*
                    Map resultsCtx = (Map) FreeMarkerWorker.getWrappedObject("context", env);
                    templateContext.put("contentId", contentId);
                    templateContext.put("locale", locale);
                    templateContext.put("mapKey", null);
                    templateContext.put("subContentId", null);
                    templateContext.put("templateContentId", null);
                    templateContext.put("subDataResourceTypeId", null);
                    templateContext.put("mimeTypeId", null);
*/


                } else {
                    if (UtilValidate.isNotEmpty(wrappedFTL))
                        out.write(wrappedFTL);
                }
                    FreeMarkerWorker.removeValues(templateCtx, removeKeyNames);
                    FreeMarkerWorker.reloadValues(templateCtx, savedValues, env);
            }

            private boolean checkWhen (GenericValue thisContent, String contentAssocTypeId) {
        
                boolean isPick = false;
                Map assocContext = new HashMap();
                if (UtilValidate.isEmpty(contentAssocTypeId))
                    contentAssocTypeId = "";
                assocContext.put("contentAssocTypeId", contentAssocTypeId);
                //assocContext.put("contentTypeId", assocValue.get("contentTypeId") );
                String assocRelation = null;
                String thisDirection = (String)templateCtx.get("direction");
                String thisContentId = (String)templateCtx.get("thisContentId");
                String relatedDirection = null;
                if (thisDirection != null && thisDirection.equalsIgnoreCase("From")) {
                    assocContext.put("contentIdFrom", thisContentId);
                    assocRelation = "FromContent";
                    relatedDirection = "From";
                } else {
                    assocContext.put("contentIdTo", thisContentId);
                    assocRelation = "ToContent";
                    relatedDirection = "To";
                }
                assocContext.put("content", thisContent);
                List purposes = ContentWorker.getPurposes(thisContent);
                assocContext.put("purposes", purposes);
                List contentTypeAncestry = new ArrayList();
                String contentTypeId = (String)thisContent.get("contentTypeId");
                try {
                    ContentWorker.getContentTypeAncestry(delegator, contentTypeId, contentTypeAncestry);
                } catch(GenericEntityException e) {
                    return false;
                }
                assocContext.put("typeAncestry", contentTypeAncestry);
                Map whenMap = (Map)traverseContext.get("whenMap");
                String pickWhen = (String)whenMap.get("pickWhen");
                List nodeTrail = (List)traverseContext.get("nodeTrail");
                int indentSz = indent.intValue() + nodeTrail.size();
                assocContext.put("indentObj", new Integer(indentSz));
                isPick = ContentWorker.checkWhen(assocContext, (String)whenMap.get("pickWhen"));
                return isPick;
           }


            public void populateContext(Map traverseContext, Map templateContext) {

                List nodeTrail = (List)traverseContext.get("nodeTrail");
                int sz = nodeTrail.size();
                Map node = (Map)nodeTrail.get(sz - 1);
                GenericValue content = (GenericValue)node.get("value");
                String contentId = (String)node.get("contentId");
                String subContentId = (String)node.get("subContentId");
                templateContext.put("subContentId", contentId);
                templateContext.put("subContentDataResourceView", null);
                int indentSz = indent.intValue() + nodeTrail.size();
                templateContext.put("indent", new Integer(indentSz));
                if (sz >= 2) {
                    Map parentNode = (Map)nodeTrail.get(sz - 2);
                    GenericValue parentContent = (GenericValue)parentNode.get("value");
                    String parentContentId = (String)parentNode.get("contentId");
                    templateContext.put("parentContentId", parentContentId);
                    templateContext.put("parentContent", parentContent);
                    templateContext.put("nodeTrail", nodeTrail);
                }
            }

        };
    }
}
