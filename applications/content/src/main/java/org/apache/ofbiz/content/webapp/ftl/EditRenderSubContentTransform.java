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
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;
import org.apache.ofbiz.content.content.ContentWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.LocalDispatcher;

import freemarker.core.Environment;
import freemarker.template.TemplateTransformModel;

/**
 * EditRenderSubContentTransform - Freemarker Transform for URLs (links)
 *
 * This is an interactive FreeMarker tranform that allows the user to modify the contents that are placed within it.
 */
public class EditRenderSubContentTransform implements TemplateTransformModel {

    public static final String module = EditRenderSubContentTransform.class.getName();

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
    public static String getArg(Map<String, ? extends Object> args, String key, Environment env) {
        return FreeMarkerWorker.getArg(args, key, env);
    }

    /**
     * @deprecated use FreeMarkerWorker.getArg()
     */
    @Deprecated
    public static String getArg(Map<String, ? extends Object> args, String key, Map<String, ? extends Object> ctx) {
        return FreeMarkerWorker.getArg(args, key, ctx);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Writer getWriter(Writer out, @SuppressWarnings("rawtypes") Map args) {
        final StringBuilder buf = new StringBuilder();
        final Environment env = Environment.getCurrentEnvironment();
        Map<String, Object> ctx = FreeMarkerWorker.getWrappedObject("context", env);
        final String editTemplate = FreeMarkerWorker.getArg(args, "editTemplate", ctx);
        final String wrapTemplateId = FreeMarkerWorker.getArg(args, "wrapTemplateId", ctx);
        final String mapKey = FreeMarkerWorker.getArg(args, "mapKey", ctx);
        final String templateContentId = FreeMarkerWorker.getArg(args, "templateContentId", ctx);
        final String subContentId = FreeMarkerWorker.getArg(args, "subContentId", ctx);
        String subDataResourceTypeIdTemp = FreeMarkerWorker.getArg(args, "subDataResourceTypeId", ctx);
        final String contentId = FreeMarkerWorker.getArg(args, "contentId", ctx);
        final Locale locale = FreeMarkerWorker.getWrappedObject("locale", env);
        String mimeTypeIdTemp = FreeMarkerWorker.getArg(args, "mimeTypeId", ctx);
        final String rootDir = FreeMarkerWorker.getArg(args, "rootDir", ctx);
        final String webSiteId = FreeMarkerWorker.getArg(args, "webSiteId", ctx);
        final String https = FreeMarkerWorker.getArg(args, "https", ctx);
        final LocalDispatcher dispatcher = FreeMarkerWorker.getWrappedObject("dispatcher", env);
        final Delegator delegator = FreeMarkerWorker.getWrappedObject("delegator", env);
        final GenericValue userLogin = FreeMarkerWorker.getWrappedObject("userLogin", env);
        GenericValue subContentDataResourceViewTemp = FreeMarkerWorker.getWrappedObject("subContentDataResourceView", env);

        ctx.put("mapKey", mapKey);
        ctx.put("subDataResourceTypeIdTemp", subDataResourceTypeIdTemp);
        ctx.put("contentId", contentId);
        ctx.put("templateContentId", templateContentId);
        ctx.put("locale", locale);

        // This transform does not need information about the subContent until the
        // close action, but any embedded RenderDataResourceTransformation will need it
        // and since it cannot be passed back up from that transform, the subContent view
        // is gotten here and made available to underlying transforms to save overall
        // processing time.
        GenericValue parentContent = null;
        List<String> assocTypes = UtilMisc.toList("SUB_CONTENT");
        Timestamp fromDate = UtilDateTime.nowTimestamp();
        if (subContentDataResourceViewTemp == null) {
            try {
                subContentDataResourceViewTemp = ContentWorker.getSubContent(delegator, contentId, mapKey, subContentId, userLogin, assocTypes, fromDate);
            } catch (IOException e) {
                Debug.logError(e, "Error getting sub-content", module);
                throw new RuntimeException(e.getMessage());
            }
        }

        final GenericValue subContentDataResourceView = subContentDataResourceViewTemp;

        String dataResourceIdTemp = null;
        String subContentIdSubTemp = null;
        if (subContentDataResourceView != null && subContentDataResourceView.get("contentId") != null) {
            dataResourceIdTemp = (String) subContentDataResourceView.get("drDataResourceId");
            subContentIdSubTemp = (String) subContentDataResourceView.get("contentId");
            if (UtilValidate.isEmpty(subDataResourceTypeIdTemp)) {
                subDataResourceTypeIdTemp = (String) subContentDataResourceView.get("drDataResourceTypeId");
            }
            if (UtilValidate.isEmpty(mimeTypeIdTemp)) {
                mimeTypeIdTemp = (String) subContentDataResourceView.get("mimeTypeId");
                if (UtilValidate.isEmpty(mimeTypeIdTemp) && UtilValidate.isNotEmpty(contentId)) { // will need these below
                    try {
                        parentContent = EntityQuery.use(delegator).from("Content").where("contentId", contentId).queryOne();
                        if (parentContent != null) {
                            mimeTypeIdTemp = (String) parentContent.get("mimeTypeId");
                        }
                    } catch (GenericEntityException e) {
                        throw new RuntimeException(e.getMessage());
                    }
                }
            }
            ctx.put("subContentId", subContentIdSubTemp);
            ctx.put("drDataResourceId", dataResourceIdTemp);
            ctx.put("subContentDataResourceView", subContentDataResourceView);
            ctx.put("mimeTypeId", mimeTypeIdTemp);
        } else {
            ctx.put("subContentId", null);
            ctx.put("drDataResourceId", null);
            ctx.put("subContentDataResourceView", null);
            ctx.put("mimeTypeId", null);
        }

        final String dataResourceId = dataResourceIdTemp;
        final String subContentIdSub = subContentIdSubTemp;
        final Map<String, Object> templateContext = ctx;
        final String mimeTypeId = mimeTypeIdTemp;
        final String subDataResourceTypeId = subDataResourceTypeIdTemp;

        return new Writer(out) {

            @Override
            public void write(char cbuf[], int off, int len) {
                buf.append(cbuf, off, len);
            }

            @Override
            public void flush() throws IOException {
                out.flush();
            }

            @Override
            public void close() throws IOException {
                String wrappedFTL = buf.toString();
                if (editTemplate != null && "true".equalsIgnoreCase(editTemplate)) {
                    if (UtilValidate.isNotEmpty(wrapTemplateId)) {
                        templateContext.put("wrappedFTL", wrappedFTL);
                        templateContext.put("webSiteId", webSiteId);
                        templateContext.put("https", https);
                        templateContext.put("rootDir", rootDir);

                        Map<String, Object> templateRoot = FreeMarkerWorker.createEnvironmentMap(env);
                        templateRoot.put("wrapDataResourceId", dataResourceId);
                        templateRoot.put("wrapDataResourceTypeId", subDataResourceTypeId);
                        templateRoot.put("wrapContentIdTo", contentId);
                        templateRoot.put("wrapSubContentId", subContentIdSub);
                        templateRoot.put("wrapMimeTypeId", mimeTypeId);
                        templateRoot.put("wrapMapKey", mapKey);
                        templateRoot.put("context", templateContext);

                        try {
                            ContentWorker.renderContentAsText(dispatcher, wrapTemplateId, out, templateRoot, locale, mimeTypeId, null, null, false);
                        } catch (IOException | GeneralException e) {
                            Debug.logError(e, "Error rendering content" + e.getMessage(), module);
                            throw new IOException("Error rendering content" + e.toString());
                        }

                        FreeMarkerWorker.getWrappedObject("context", env);
                        templateContext.put("contentId", contentId);
                        templateContext.put("locale", locale);
                        templateContext.put("mapKey", null);
                        templateContext.put("subContentId", null);
                        templateContext.put("templateContentId", null);
                        templateContext.put("subDataResourceTypeId", null);
                        templateContext.put("mimeTypeId", null);
                        templateContext.put("wrappedFTL", null);
                    }
                } else {
                    out.write(wrappedFTL);
                }
            }
        };
    }
}
