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

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.content.content.ContentServicesComplex;
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
 * LoopSubContentTransform - Freemarker Transform for URLs (links)
 */
public class LoopSubContentTransform implements TemplateTransformModel {

    public static final String module = LoopSubContentTransform.class.getName();

    public static final String [] saveKeyNames = {"contentId", "subContentId", "mimeType", "subContentDataResourceView", "wrapTemplateId", "contentTemplateId"};
    public static final String [] removeKeyNames = {"wrapTemplateId", "entityList", "entityIndex", "textData", "dataResourceId","drDataResourceId", "subContentIdSub", "parentContent", "wrappedFTL"};

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

    public static boolean prepCtx(GenericDelegator delegator, Map ctx) {

        //String contentId = (String)ctx.get("contentId");
        //String mimeTypeId = (String)ctx.get("mimeTypeId");
        List lst = (List) ctx.get("entityList");
        Integer idx = (Integer) ctx.get("entityIndex");
        if (idx == null)
            idx = new Integer(0);
        int i = idx.intValue();
        if (i >= lst.size()) {
            return false;
        }
        GenericValue subContentDataResourceView = (GenericValue) lst.get(i);
        ctx.put("subContentDataResourceView", subContentDataResourceView);
        GenericValue electronicText = null;
        try {
            electronicText = subContentDataResourceView.getRelatedOne("ElectronicText");
        } catch (GenericEntityException e) {
            throw new RuntimeException(e.getMessage());
        }

        String dataResourceId = (String) subContentDataResourceView.get("drDataResourceId");
        String subContentIdSub = (String) subContentDataResourceView.get("contentId");
        // This order is taken so that the dataResourceType can be overridden in the transform arguments.
        String subDataResourceTypeId = (String)ctx.get("subDataResourceTypeId");
        if (UtilValidate.isEmpty(subDataResourceTypeId)) {
            subDataResourceTypeId = (String) subContentDataResourceView.get("drDataResourceTypeId");
            // TODO: If this value is still empty then it is probably necessary to get a value from
            // the parent context. But it will already have one and it is the same context that is
            // being passed.
        }
        // This order is taken so that the mimeType can be overridden in the transform arguments.
        String mimeTypeId = (String)ctx.get("mimeTypeId");
        if (UtilValidate.isEmpty(mimeTypeId)) {
            mimeTypeId = (String) subContentDataResourceView.get("mimeTypeId");
            String parentContentId = (String)ctx.get("contentId");
            if (UtilValidate.isEmpty(mimeTypeId) && UtilValidate.isNotEmpty(parentContentId)) { // will need these below
                try {
                    GenericValue parentContent = delegator.findByPrimaryKey("Content", UtilMisc.toMap("contentId", parentContentId));
                    if (parentContent != null) {
                        mimeTypeId = (String) parentContent.get("mimeTypeId");
                        ctx.put("parentContent", parentContent);
                    }
                } catch (GenericEntityException e) {
                    throw new RuntimeException(e.getMessage());
                }
            }

        }

        // This is what the FM template will see.
        ctx.put("subContentDataResourceView", subContentDataResourceView);
        if (electronicText != null)
            ctx.put("textData", electronicText.get("textData"));
        else
            ctx.put("textData", null);
        ctx.put("entityIndex", new Integer(i + 1));
        ctx.put("subContentId", subContentIdSub);
        ctx.put("drDataResourceId", dataResourceId);
        ctx.put("mimeTypeId", mimeTypeId);
        ctx.put("dataResourceId", dataResourceId);
        ctx.put("subContentIdSub", subContentIdSub);
        ctx.put("subDataResourceTypeId", subDataResourceTypeId);
        return true;
    }

    public Writer getWriter(final Writer out, Map args) {
        final StringBuffer buf = new StringBuffer();
        final Environment env = Environment.getCurrentEnvironment();
        final Map templateCtx = (Map) FreeMarkerWorker.getWrappedObject("context", env);
        //FreeMarkerWorker.convertContext(templateCtx);
        final LocalDispatcher dispatcher = (LocalDispatcher) FreeMarkerWorker.getWrappedObject("dispatcher", env);
        final GenericDelegator delegator = (GenericDelegator) FreeMarkerWorker.getWrappedObject("delegator", env);
        //templateCtx.put("buf", buf);
        final Map savedValues = FreeMarkerWorker.saveValues(templateCtx, saveKeyNames);
        FreeMarkerWorker.overrideWithArgs(templateCtx, args);
        String contentAssocTypeId = (String)templateCtx.get("contentAssocTypeId");
        if (UtilValidate.isEmpty(contentAssocTypeId)) {
            contentAssocTypeId = "SUB_CONTENT";
            templateCtx.put("contentAssocTypeId ", contentAssocTypeId );
        }
        List assocTypes = UtilMisc.toList(contentAssocTypeId);
        templateCtx.put("assocTypes", assocTypes);
        Locale locale = (Locale) templateCtx.get("locale");
        if (locale == null) {
            locale = Locale.getDefault();
            templateCtx.put("locale", locale);
        }

/*
        final String editTemplate = getArg(args, "editTemplate", ctx);
        final String wrapTemplateId = getArg(args, "wrapTemplateId", ctx);
        final String mapKey = getArg(args, "mapKey", ctx);
        final String templateContentId = getArg(args, "templateContentId", ctx);
        final String subDataResourceTypeId = getArg(args, "subDataResourceTypeId", ctx);
        final String contentId = getArg(args, "contentId", ctx);
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
        //final LocalDispatcher dispatcher = (LocalDispatcher) FreeMarkerWorker.getWrappedObject("dispatcher", env);
        final GenericDelegator delegator = (GenericDelegator) FreeMarkerWorker.getWrappedObject("delegator", env);
        //final GenericValue userLogin = (GenericValue) FreeMarkerWorker.getWrappedObject("userLogin", env);

        ctx.put("mapKey", mapKey);
        ctx.put("contentId", contentId);
        ctx.put("templateContentId", templateContentId);
        ctx.put("locale", locale);

        //ctx.put("userLogin", userLogin);
        String contentAssocTypeId = getArg(args, "contentAssocTypeId", ctx);
        if (UtilValidate.isEmpty(contentAssocTypeId))
            contentAssocTypeId = "SUB_CONTENT";
        List assocTypes = UtilMisc.toList(contentAssocTypeId);
        ctx.put("assocTypes", assocTypes);
*/
        String fromDateStr = (String)templateCtx.get("fromDateStr");
        Timestamp fromDate = null;
        if (UtilValidate.isNotEmpty(fromDateStr)) {
            fromDate = UtilDateTime.toTimestamp(fromDateStr);
        }
        if (fromDate == null)
            fromDate = UtilDateTime.nowTimestamp();
        String thisContentId = (String)templateCtx.get("contentId");
        if (UtilValidate.isEmpty(thisContentId)) {
            thisContentId = (String)templateCtx.get("subContentId");
        }
        String thisMapKey = (String)templateCtx.get("mapKey");
        //GenericValue subContentDataResourceView = null;
        Map results =
            ContentServicesComplex.getAssocAndContentAndDataResourceMethod(delegator, thisContentId, thisMapKey, "From", fromDate, null, null, null, assocTypes, null);
        List entityList = (List) results.get("entityList");
        templateCtx.put("entityList", entityList);

        return new LoopWriter(out) {

            public void write(char cbuf[], int off, int len) {
                buf.append(cbuf, off, len);
                //StringBuffer ctxBuf = (StringBuffer) templateCtx.get("buf");
                //ctxBuf.append(cbuf, off, len);
            }

            public void flush() throws IOException {
                out.flush();
            }

            public int onStart() throws TemplateModelException, IOException {
                templateCtx.put("entityIndex", new Integer(0));
                boolean inProgress = prepCtx(delegator, templateCtx);
                if (inProgress) {
                    return TransformControl.EVALUATE_BODY;
                } else {
                    return TransformControl.SKIP_BODY;
                }
            }

            public int afterBody() throws TemplateModelException, IOException {
                Integer idx = (Integer) templateCtx.get("entityIndex");
                int i = idx.intValue();
                boolean inProgress = prepCtx(delegator, templateCtx);
                //out.write(buf.toString());
                //buf.setLength(0);
                if (inProgress)
                    return TransformControl.REPEAT_EVALUATION;
                else
                    return TransformControl.END_EVALUATION;
            }

            public void close() throws IOException {

                String wrappedFTL = buf.toString();
                String encloseWrappedText = (String)templateCtx.get("encloseWrappedText");
                if (UtilValidate.isEmpty(encloseWrappedText) || encloseWrappedText.equalsIgnoreCase("false")) {
                    out.write(wrappedFTL);
                    wrappedFTL = ""; // So it won't get written again below.
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
                    templateRoot.put("wrapMapKey", mapKey);
*/
                    templateRoot.put("wrapDataResourceTypeId", templateCtx.get("subDataResourceTypeId"));
                    templateRoot.put("wrapContentIdTo", templateCtx.get("contentId"));
                    templateRoot.put("wrapMimeTypeId", templateCtx.get("mimeTypeId"));
                    templateRoot.put("context", templateCtx);
                    

                    Locale locale = (Locale) templateCtx.get("locale");
                    if (locale == null)
                        locale = Locale.getDefault();
                    String mimeTypeId = (String) templateCtx.get("mimeTypeId");
                    try {
                        ContentWorker.renderContentAsText(dispatcher, delegator, wrapTemplateId, out, templateRoot, locale, mimeTypeId, true);
                    } catch (GeneralException e) {
                        Debug.logError(e, "Error rendering content", module);
                        throw new IOException("Error rendering content" + e.toString());
                    }
                } else {
                    if (UtilValidate.isNotEmpty(wrappedFTL))
                        out.write(wrappedFTL);
                }
                    FreeMarkerWorker.removeValues(templateCtx, removeKeyNames);
                    FreeMarkerWorker.reloadValues(templateCtx, savedValues, env);
            }
        };
    }
}
