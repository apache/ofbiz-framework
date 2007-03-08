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
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.LocalDispatcher;

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
        Map ctx = (Map) FreeMarkerWorker.getWrappedObject("context", env);
        final String editTemplate = getArg(args, "editTemplate", ctx);
        final String wrapTemplateId = getArg(args, "wrapTemplateId", ctx);
        final String mapKey = getArg(args, "mapKey", ctx);
        final String templateContentId = getArg(args, "templateContentId", ctx);
        final String subContentId = getArg(args, "subContentId", ctx);
        String subDataResourceTypeIdTemp = getArg(args, "subDataResourceTypeId", ctx);
        final String contentId = getArg(args, "contentId", ctx);

        
        final Locale locale = (Locale) FreeMarkerWorker.getWrappedObject("locale", env);
        String mimeTypeIdTemp = getArg(args, "mimeTypeId", ctx);
        final String rootDir = getArg(args, "rootDir", ctx);
        final String webSiteId = getArg(args, "webSiteId", ctx);
        final String https = getArg(args, "https", ctx);
        final LocalDispatcher dispatcher = (LocalDispatcher) FreeMarkerWorker.getWrappedObject("dispatcher", env);
        final GenericDelegator delegator = (GenericDelegator) FreeMarkerWorker.getWrappedObject("delegator", env);
        final GenericValue userLogin = (GenericValue) FreeMarkerWorker.getWrappedObject("userLogin", env);
        GenericValue subContentDataResourceViewTemp = (GenericValue) FreeMarkerWorker.getWrappedObject("subContentDataResourceView", env);
        //final HttpServletRequest request = (HttpServletRequest)FreeMarkerWorker.getWrappedObject("request", env);

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
        //ctx.put("userLogin", userLogin);
        List assocTypes = UtilMisc.toList("SUB_CONTENT");
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
                        parentContent = delegator.findByPrimaryKey("Content", UtilMisc.toMap("contentId", contentId));
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
            //request.setAttribute("drDataResourceId", subContentDataResourceView.get("drDataResourceId"));
        } else {
            ctx.put("subContentId", null);
            ctx.put("drDataResourceId", null);
            ctx.put("subContentDataResourceView", null);
            ctx.put("mimeTypeId", null);
            //request.setAttribute("drDataResourceId", null);
        }

        final String dataResourceId = dataResourceIdTemp;
        final String subContentIdSub = subContentIdSubTemp;
        //final GenericValue finalSubContentView = subContentDataResourceView;
        //final GenericValue content = parentContent;
        final Map templateContext = ctx;
        final String mimeTypeId = mimeTypeIdTemp;
        final String subDataResourceTypeId = subDataResourceTypeIdTemp;

        return new Writer(out) {

            public void write(char cbuf[], int off, int len) {
                buf.append(cbuf, off, len);
            }

            public void flush() throws IOException {
                out.flush();
            }

            public void close() throws IOException {
                String wrappedFTL = buf.toString();
                if (editTemplate != null && editTemplate.equalsIgnoreCase("true")) {
                    if (UtilValidate.isNotEmpty(wrapTemplateId)) {
                        templateContext.put("wrappedFTL", wrappedFTL);
                        //ServletContext servletContext = (ServletContext)request.getSession().getServletContext();
                        //String rootDir = servletContext.getRealPath("/");
                        templateContext.put("webSiteId", webSiteId);
                        templateContext.put("https", https);
                        templateContext.put("rootDir", rootDir);
                        
                        Map templateRoot = FreeMarkerWorker.createEnvironmentMap(env);
                        
                        templateRoot.put("wrapDataResourceId", dataResourceId);
                        templateRoot.put("wrapDataResourceTypeId", subDataResourceTypeId);
                        templateRoot.put("wrapContentIdTo", contentId);
                        templateRoot.put("wrapSubContentId", subContentIdSub);
                        templateRoot.put("wrapMimeTypeId", mimeTypeId);
                        templateRoot.put("wrapMapKey", mapKey);
                        templateRoot.put("context", templateContext);
                        
                        try {
                            ContentWorker.renderContentAsText(dispatcher, delegator, wrapTemplateId, out, templateRoot, locale, mimeTypeId, false);
                        } catch (IOException e) {
                            Debug.logError(e, "Error rendering content" + e.getMessage(), module);
                            throw new IOException("Error rendering content" + e.toString());
                        } catch (GeneralException e2) {
                            Debug.logError(e2, "Error rendering content" + e2.getMessage(), module);
                            throw new IOException("Error rendering content" + e2.toString());
                        }
                        
                        Map ctx = (Map) FreeMarkerWorker.getWrappedObject("context", env);
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
