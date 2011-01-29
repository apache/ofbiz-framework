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
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.webapp.ftl.LoopWriter;

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
     * @deprecated use FreeMarkerWorker.getWrappedObject()
     * A wrapper for the FreeMarkerWorker version.
     */
    @Deprecated
    public static Object getWrappedObject(String varName, Environment env) {
        return FreeMarkerWorker.getWrappedObject(varName, env);
    }

    /**
     * @deprecated use FreeMarkerWorker.getArg()
     */
    @Deprecated
    public static String getArg(Map<String, Object> args, String key, Environment env) {
        return FreeMarkerWorker.getArg(args, key, env);
    }

    /**
     * @deprecated use FreeMarkerWorker.getArg()
     */
    @Deprecated
    public static String getArg(Map<String, Object> args, String key, Map<String, Object> ctx) {
        return FreeMarkerWorker.getArg(args, key, ctx);
    }

    @SuppressWarnings("unchecked")
    public Writer getWriter(final Writer out, Map args) {
        final StringBuilder buf = new StringBuilder();
        final Environment env = Environment.getCurrentEnvironment();
        final Map<String, Object> templateCtx = FreeMarkerWorker.getWrappedObject("context", env);
        //FreeMarkerWorker.convertContext(templateCtx);
        final Map<String, Object> savedValues = FreeMarkerWorker.saveValues(templateCtx, saveKeyNames);
        FreeMarkerWorker.overrideWithArgs(templateCtx, args);
        final Delegator delegator = FreeMarkerWorker.getWrappedObject("delegator", env);
/*
        final String editTemplate = FreeMarkerWorker.getArg(args, "editTemplate", ctx);
        final String wrapTemplateId = FreeMarkerWorker.getArg(args, "wrapTemplateId", ctx);
        //final String mapKey = FreeMarkerWorker.getArg(args, "mapKey", ctx);
        final String templateContentId = FreeMarkerWorker.getArg(args, "templateContentId", ctx);
        final String subDataResourceTypeId = FreeMarkerWorker.getArg(args, "subDataResourceTypeId", ctx);
        final String contentId = FreeMarkerWorker.getArg(args, "contentId", ctx);
        final String subContentId = FreeMarkerWorker.getArg(args, "subContentId", ctx);
        final String rootDir = FreeMarkerWorker.getArg(args, "rootDir", ctx);
        final String webSiteId = FreeMarkerWorker.getArg(args, "webSiteId", ctx);
        final String https = FreeMarkerWorker.getArg(args, "https", ctx);
        final String viewSize = FreeMarkerWorker.getArg(args, "viewSize", ctx);
        final String viewIndex = FreeMarkerWorker.getArg(args, "viewIndex", ctx);
        final String listSize = FreeMarkerWorker.getArg(args, "listSize", ctx);
        final String highIndex = FreeMarkerWorker.getArg(args, "highIndex", ctx);
        final String lowIndex = FreeMarkerWorker.getArg(args, "lowIndex", ctx);
        final String queryString = FreeMarkerWorker.getArg(args, "queryString", ctx);
        final Locale locale = FreeMarkerWorker.getWrappedObject("locale", env);
        final String mimeTypeId = FreeMarkerWorker.getArg(args, "mimeTypeId", ctx);
*/
        final LocalDispatcher dispatcher = FreeMarkerWorker.getWrappedObject("dispatcher", env);
        //final GenericValue userLogin = FreeMarkerWorker.getWrappedObject("userLogin", env);
        GenericValue view = FreeMarkerWorker.getWrappedObject("subContentDataResourceView", env);
        final Integer indent = (templateCtx.get("indent") == null) ? Integer.valueOf(0) : (Integer)templateCtx.get("indent");

        String contentId = (String)templateCtx.get("contentId");
        String subContentId = (String)templateCtx.get("subContentId");
        if (view == null) {
            String thisContentId = subContentId;
            if (UtilValidate.isEmpty(thisContentId)) {
                thisContentId = contentId;
            }
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
        final Map<String, Object> traverseContext = FastMap.newInstance();
        traverseContext.put("delegator", delegator);
        Map<String, Object> whenMap = FastMap.newInstance();
        whenMap.put("followWhen", templateCtx.get("followWhen"));
        whenMap.put("pickWhen", templateCtx.get("pickWhen"));
        whenMap.put("returnBeforePickWhen", templateCtx.get("returnBeforePickWhen"));
        whenMap.put("returnAfterPickWhen", templateCtx.get("returnAfterPickWhen"));
        traverseContext.put("whenMap", whenMap);
        String fromDateStr = (String)templateCtx.get("fromDateStr");
        String thruDateStr = (String)templateCtx.get("thruDateStr");
        Timestamp fromDate = null;
        if (UtilValidate.isNotEmpty(fromDateStr)) {
            fromDate = UtilDateTime.toTimestamp(fromDateStr);
        }
        traverseContext.put("fromDate", fromDate);
        Timestamp thruDate = null;
        if (UtilValidate.isNotEmpty(thruDateStr)) {
            thruDate = UtilDateTime.toTimestamp(thruDateStr);
        }
        traverseContext.put("thruDate", thruDate);
        String startContentAssocTypeId = (String)templateCtx.get("contentAssocTypeId");
        if (startContentAssocTypeId != null)
            startContentAssocTypeId = "SUB_CONTENT";
        traverseContext.put("contentAssocTypeId", startContentAssocTypeId);
        String direction = (String)templateCtx.get("direction");
        if (UtilValidate.isEmpty(direction))
            direction = "From";
        traverseContext.put("direction", direction);


        return new LoopWriter(out) {

            @Override
            public void write(char cbuf[], int off, int len) {
                //StringBuilder ctxBuf = (StringBuilder) templateContext.get("buf");
                //ctxBuf.append(cbuf, off, len);
                buf.append(cbuf, off, len);
            }

            @Override
            public void flush() throws IOException {
                out.flush();
            }

            @Override
            public int onStart() throws TemplateModelException, IOException {
                //templateContext.put("buf", new StringBuilder());
                List<Map<String, Object>> nodeTrail = FastList.newInstance();
                traverseContext.put("nodeTrail", nodeTrail);
                // GenericValue content = null;
/*
                if (UtilValidate.isNotEmpty(contentId)) {
                    try {
                        content = delegator.findByPrimaryKey("Content", UtilMisc.toMap("contentId", contentId));
                    } catch (GenericEntityException e) {
                        // TODO: Not sure what to put here.
                        throw new RuntimeException(e.getMessage());
                    }
                }
*/
                Map<String, Object> rootNode = ContentWorker.makeNode(subContentDataResourceView);
                ContentWorker.traceNodeTrail("1", nodeTrail);
                ContentWorker.selectKids(rootNode, traverseContext);
                ContentWorker.traceNodeTrail("2", nodeTrail);
                nodeTrail.add(rootNode);
                boolean isPick = checkWhen(subContentDataResourceView, (String)traverseContext.get("contentAssocTypeId"));
                rootNode.put("isPick", Boolean.valueOf(isPick));
                if (!isPick) {
                    ContentWorker.traceNodeTrail("3", nodeTrail);
                    isPick = ContentWorker.traverseSubContent(traverseContext);
                    ContentWorker.traceNodeTrail("4", nodeTrail);
                }
                if (isPick) {
                    populateContext(traverseContext, templateCtx);
                    ContentWorker.traceNodeTrail("5", nodeTrail);
                    return TransformControl.EVALUATE_BODY;
                } else {
                    return TransformControl.SKIP_BODY;
                }
            }

            @Override
            public int afterBody() throws TemplateModelException, IOException {
                //out.write(buf.toString());
                //buf.setLength(0);
                //templateContext.put("buf", new StringBuilder());
                List<Map<String, Object>> nodeTrail = UtilGenerics.checkList(traverseContext.get("nodeTrail"));
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

            @Override
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
                    Map<String, Object> templateRoot = FreeMarkerWorker.createEnvironmentMap(env);
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
                        ContentWorker.renderContentAsText(dispatcher, delegator, wrapTemplateId, out, templateRoot, locale, mimeTypeId, null, null, true);
                    } catch (GeneralException e) {
                        Debug.logError(e, "Error rendering content", module);
                        throw new IOException("Error rendering content" + e.toString());
                    }
/*
                    Map resultsCtx = FreeMarkerWorker.getWrappedObject("context", env);
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
                Map<String, Object> assocContext = FastMap.newInstance();
                if (UtilValidate.isEmpty(contentAssocTypeId)) {
                    contentAssocTypeId = "";
                }
                assocContext.put("contentAssocTypeId", contentAssocTypeId);
                // assocContext.put("contentTypeId", assocValue.get("contentTypeId"));
                // String assocRelation = null;
                String thisDirection = (String)templateCtx.get("direction");
                String thisContentId = (String)templateCtx.get("thisContentId");
                // String relatedDirection = null;
                if (thisDirection != null && thisDirection.equalsIgnoreCase("From")) {
                    assocContext.put("contentIdFrom", thisContentId);
                    // assocRelation = "FromContent";
                    // relatedDirection = "From";
                } else {
                    assocContext.put("contentIdTo", thisContentId);
                    // assocRelation = "ToContent";
                    // relatedDirection = "To";
                }
                assocContext.put("content", thisContent);
                List<Object> purposes = ContentWorker.getPurposes(thisContent);
                assocContext.put("purposes", purposes);
                List<String> contentTypeAncestry = FastList.newInstance();
                String contentTypeId = (String)thisContent.get("contentTypeId");
                try {
                    ContentWorker.getContentTypeAncestry(delegator, contentTypeId, contentTypeAncestry);
                } catch (GenericEntityException e) {
                    return false;
                }
                assocContext.put("typeAncestry", contentTypeAncestry);
                Map<String, Object> whenMap = UtilGenerics.checkMap(traverseContext.get("whenMap"));
                // String pickWhen = (String)whenMap.get("pickWhen");
                List<Map<String, ? extends Object>> nodeTrail = UtilGenerics.checkList(traverseContext.get("nodeTrail"));
                int indentSz = indent.intValue() + nodeTrail.size();
                assocContext.put("indentObj", Integer.valueOf(indentSz));
                isPick = ContentWorker.checkWhen(assocContext, (String)whenMap.get("pickWhen"));
                return isPick;
            }

            public void populateContext(Map<String, Object> traverseContext, Map<String, Object> templateContext) {
                List<Map<String, Object>> nodeTrail = UtilGenerics.checkList(traverseContext.get("nodeTrail"));
                int sz = nodeTrail.size();
                Map<String, Object> node = nodeTrail.get(sz - 1);
                // GenericValue content = (GenericValue)node.get("value");
                String contentId = (String)node.get("contentId");
                // String subContentId = (String)node.get("subContentId");
                templateContext.put("subContentId", contentId);
                templateContext.put("subContentDataResourceView", null);
                int indentSz = indent.intValue() + nodeTrail.size();
                templateContext.put("indent", Integer.valueOf(indentSz));
                if (sz >= 2) {
                    Map<String, Object> parentNode = nodeTrail.get(sz - 2);
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
