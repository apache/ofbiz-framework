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
package org.apache.ofbiz.content.webapp.ftl;

import java.io.IOException;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;
import org.apache.ofbiz.content.content.ContentWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.webapp.ftl.LoopWriter;

import freemarker.core.Environment;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTransformModel;
import freemarker.template.TransformControl;

/**
 * TraverseSubContentTransform - Freemarker Transform for URLs (links)
 */
public class TraverseSubContentTransform implements TemplateTransformModel {

    private static final String MODULE = TraverseSubContentTransform.class.getName();
    public static final String[] SAVE_KEY_NAMES = {"contentId", "subContentId", "mimeType", "subContentDataResourceView", "wrapTemplateId",
            "templateContentId", "pickWhen", "followWhen", "returnAfterPickWhen", "returnBeforePickWhen", "indent"};
    public static final String[] REMOVE_KEY_NAMES = {"templateContentId", "subDataResourceTypeId", "mapKey", "wrappedFTL", "nodeTrail"};

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

    @Override
    @SuppressWarnings("unchecked")
    public Writer getWriter(final Writer out, @SuppressWarnings("rawtypes") Map args) {
        final StringBuilder buf = new StringBuilder();
        final Environment env = Environment.getCurrentEnvironment();
        final Map<String, Object> templateCtx = FreeMarkerWorker.getWrappedObject("context", env);
        final Map<String, Object> savedValues = FreeMarkerWorker.saveValues(templateCtx, SAVE_KEY_NAMES);
        FreeMarkerWorker.overrideWithArgs(templateCtx, args);
        final Delegator delegator = FreeMarkerWorker.getWrappedObject("delegator", env);
        final LocalDispatcher dispatcher = FreeMarkerWorker.getWrappedObject("dispatcher", env);
        GenericValue view = FreeMarkerWorker.getWrappedObject("subContentDataResourceView", env);
        final Integer indent = (templateCtx.get("indent") == null) ? Integer.valueOf(0) : (Integer) templateCtx.get("indent");

        String contentId = (String) templateCtx.get("contentId");
        String subContentId = (String) templateCtx.get("subContentId");
        if (view == null) {
            String thisContentId = subContentId;
            if (UtilValidate.isEmpty(thisContentId)) {
                thisContentId = contentId;
            }
            if (UtilValidate.isNotEmpty(thisContentId)) {
                try {
                    view = EntityQuery.use(delegator).from("Content").where("contentId", thisContentId).queryOne();
                } catch (GenericEntityException e) {
                    Debug.logError(e, "Error getting sub-content", MODULE);
                    throw new RuntimeException(e.getMessage());
                }
            }
        }

        final GenericValue subContentDataResourceView = view;
        final Map<String, Object> traverseContext = new HashMap<>();
        traverseContext.put("delegator", delegator);
        Map<String, Object> whenMap = new HashMap<>();
        whenMap.put("followWhen", templateCtx.get("followWhen"));
        whenMap.put("pickWhen", templateCtx.get("pickWhen"));
        whenMap.put("returnBeforePickWhen", templateCtx.get("returnBeforePickWhen"));
        whenMap.put("returnAfterPickWhen", templateCtx.get("returnAfterPickWhen"));
        traverseContext.put("whenMap", whenMap);
        String fromDateStr = (String) templateCtx.get("fromDateStr");
        String thruDateStr = (String) templateCtx.get("thruDateStr");
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
        String startContentAssocTypeId = (String) templateCtx.get("contentAssocTypeId");
        if (startContentAssocTypeId != null) {
            startContentAssocTypeId = "SUB_CONTENT";
        }
        traverseContext.put("contentAssocTypeId", startContentAssocTypeId);
        String direction = (String) templateCtx.get("direction");
        if (UtilValidate.isEmpty(direction)) {
            direction = "From";
        }
        traverseContext.put("direction", direction);


        return new LoopWriter(out) {

            @Override
            public void write(char cbuf[], int off, int len) {
                buf.append(cbuf, off, len);
            }

            @Override
            public void flush() throws IOException {
                out.flush();
            }

            @Override
            public int onStart() throws TemplateModelException, IOException {
                List<Map<String, Object>> nodeTrail = new LinkedList<>();
                traverseContext.put("nodeTrail", nodeTrail);
                Map<String, Object> rootNode = ContentWorker.makeNode(subContentDataResourceView);
                ContentWorker.traceNodeTrail("1", nodeTrail);
                ContentWorker.selectKids(rootNode, traverseContext);
                ContentWorker.traceNodeTrail("2", nodeTrail);
                nodeTrail.add(rootNode);
                boolean isPick = checkWhen(subContentDataResourceView, (String) traverseContext.get("contentAssocTypeId"));
                rootNode.put("isPick", isPick);
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
                List<Map<String, Object>> nodeTrail = UtilGenerics.cast(traverseContext.get("nodeTrail"));
                ContentWorker.traceNodeTrail("6", nodeTrail);
                boolean inProgress = ContentWorker.traverseSubContent(traverseContext);
                ContentWorker.traceNodeTrail("7", nodeTrail);
                if (inProgress) {
                    populateContext(traverseContext, templateCtx);
                    ContentWorker.traceNodeTrail("8", nodeTrail);
                    return TransformControl.REPEAT_EVALUATION;
                } else {
                    return TransformControl.END_EVALUATION;
                }
            }

            @Override
            public void close() throws IOException {
                String wrappedFTL = buf.toString();
                String encloseWrappedText = (String) templateCtx.get("encloseWrappedText");
                if (UtilValidate.isEmpty(encloseWrappedText) || "false".equalsIgnoreCase(encloseWrappedText)) {
                    out.write(wrappedFTL);
                    wrappedFTL = null; // So it won't get written again below.
                }
                String wrapTemplateId = (String) templateCtx.get("wrapTemplateId");
                if (UtilValidate.isNotEmpty(wrapTemplateId)) {
                    templateCtx.put("wrappedFTL", wrappedFTL);
                    Map<String, Object> templateRoot = FreeMarkerWorker.createEnvironmentMap(env);
                    templateRoot.put("context", templateCtx);
                    String mimeTypeId = (String) templateCtx.get("mimeTypeId");
                    Locale locale = (Locale) templateCtx.get("locale");
                    if (locale == null) {
                        locale = Locale.getDefault();
                    }
                    try {
                        ContentWorker.renderContentAsText(dispatcher, wrapTemplateId, out, templateRoot, locale, mimeTypeId, null, null, true);
                    } catch (GeneralException e) {
                        Debug.logError(e, "Error rendering content", MODULE);
                        throw new IOException("Error rendering content" + e.toString());
                    }
                } else {
                    if (UtilValidate.isNotEmpty(wrappedFTL)) {
                        out.write(wrappedFTL);
                    }
                }
                FreeMarkerWorker.removeValues(templateCtx, REMOVE_KEY_NAMES);
                FreeMarkerWorker.reloadValues(templateCtx, savedValues, env);
            }

            private boolean checkWhen(GenericValue thisContent, String contentAssocTypeId) {
                boolean isPick = false;
                Map<String, Object> assocContext = new HashMap<>();
                if (UtilValidate.isEmpty(contentAssocTypeId)) {
                    contentAssocTypeId = "";
                }
                assocContext.put("contentAssocTypeId", contentAssocTypeId);
                String thisDirection = (String) templateCtx.get("direction");
                String thisContentId = (String) templateCtx.get("thisContentId");
                if (thisDirection != null && "From".equalsIgnoreCase(thisDirection)) {
                    assocContext.put("contentIdFrom", thisContentId);
                } else {
                    assocContext.put("contentIdTo", thisContentId);
                }
                assocContext.put("content", thisContent);
                List<Object> purposes = ContentWorker.getPurposes(thisContent);
                assocContext.put("purposes", purposes);
                List<String> contentTypeAncestry = new LinkedList<>();
                String contentTypeId = (String) thisContent.get("contentTypeId");
                try {
                    ContentWorker.getContentTypeAncestry(delegator, contentTypeId, contentTypeAncestry);
                } catch (GenericEntityException e) {
                    return false;
                }
                assocContext.put("typeAncestry", contentTypeAncestry);
                Map<String, Object> whenMap = UtilGenerics.cast(traverseContext.get("whenMap"));
                List<Map<String, ? extends Object>> nodeTrail = UtilGenerics.cast(traverseContext.get("nodeTrail"));
                int indentSz = indent + nodeTrail.size();
                assocContext.put("indentObj", indentSz);
                isPick = ContentWorker.checkWhen(assocContext, (String) whenMap.get("pickWhen"), true);
                return isPick;
            }

            public void populateContext(Map<String, Object> traverseContext, Map<String, Object> templateContext) {
                List<Map<String, Object>> nodeTrail = UtilGenerics.cast(traverseContext.get("nodeTrail"));
                int sz = nodeTrail.size();
                Map<String, Object> node = nodeTrail.get(sz - 1);
                String contentId = (String) node.get("contentId");
                templateContext.put("subContentId", contentId);
                templateContext.put("subContentDataResourceView", null);
                int indentSz = indent + nodeTrail.size();
                templateContext.put("indent", indentSz);
                if (sz >= 2) {
                    Map<String, Object> parentNode = nodeTrail.get(sz - 2);
                    GenericValue parentContent = (GenericValue) parentNode.get("value");
                    String parentContentId = (String) parentNode.get("contentId");
                    templateContext.put("parentContentId", parentContentId);
                    templateContext.put("parentContent", parentContent);
                    templateContext.put("nodeTrail", nodeTrail);
                }
            }
        };
    }
}
