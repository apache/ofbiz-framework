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
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;
import org.apache.ofbiz.content.content.ContentWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.webapp.website.WebSiteWorker;

import freemarker.core.Environment;
import freemarker.template.TemplateTransformModel;

/**
 * RenderSubContentTransform - Freemarker Transform for Content rendering
 * This transform cannot be called recursively (at this time).
 */
public class RenderSubContentTransform implements TemplateTransformModel {

    private static final String MODULE = RenderSubContentTransform.class.getName();

    /**
     * @deprecated use FreeMarkerWorker.getArg()
     * <p>Does a conditional search to return a value for a parameter with the passed name. Looks first to see if it was
     * passed as an argument to the transform.
     * Secondly, it looks to see if it is passed as a parameter in the template context object.</p>
     * <p>Note that this is different from the getArg method of EditRenderDataResourceTransform, which checks the request object
     * instead of the template context
     * object.</p>
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
    public Writer getWriter(Writer out, @SuppressWarnings("rawtypes") Map args) {
        final Environment env = Environment.getCurrentEnvironment();
        Map<String, Object> ctx = FreeMarkerWorker.getWrappedObject("context", env);
        if (ctx == null) {
            ctx = new HashMap<>();
        }
        final String mapKey = FreeMarkerWorker.getArg(args, "mapKey", ctx);
        final String subContentId = FreeMarkerWorker.getArg(args, "subContentId", ctx);
        final String contentId = FreeMarkerWorker.getArg(args, "contentId", ctx);
        final String mimeTypeId = FreeMarkerWorker.getArg(args, "mimeTypeId", ctx);
        final String throwExceptionOnError = FreeMarkerWorker.getArg(args, "throwExceptionOnError", ctx);
        final Locale locale = FreeMarkerWorker.getWrappedObject("locale", env);
        final HttpServletRequest request = FreeMarkerWorker.getWrappedObject("request", env);
        final LocalDispatcher dispatcher = FreeMarkerWorker.getWrappedObject("dispatcher", env);
        final Delegator delegator = FreeMarkerWorker.getWrappedObject("delegator", env);
        final GenericValue userLogin = FreeMarkerWorker.getWrappedObject("userLogin", env);
        GenericValue subContentDataResourceViewTemp = FreeMarkerWorker.getWrappedObject("subContentDataResourceView", env);
        if (subContentDataResourceViewTemp == null) {
            List<String> assocTypes = UtilMisc.toList("SUB_CONTENT");
            Timestamp fromDate = UtilDateTime.nowTimestamp();
            try {
                subContentDataResourceViewTemp = ContentWorker.getSubContent(delegator, contentId, mapKey, subContentId, userLogin,
                        assocTypes, fromDate);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        final GenericValue subContentDataResourceView = subContentDataResourceViewTemp;


        final Map<String, Object> templateContext = ctx;

        return new Writer(out) {

            @Override
            public void write(char cbuf[], int off, int len) {
            }

            @Override
            public void flush() throws IOException {
                out.flush();
            }

            @Override
            public void close() throws IOException {
                try {
                    renderSubContent();
                } catch (IOException e) {
                    if (!"false".equals(throwExceptionOnError)) {
                        throw new IOException(e.getMessage());
                    }
                }
            }

            public void renderSubContent() throws IOException {
                ServletContext servletContext = request.getSession().getServletContext();
                String rootDir = servletContext.getRealPath("/");
                String webSiteId = WebSiteWorker.getWebSiteId(request);
                String https = (String) servletContext.getAttribute("https");
                templateContext.put("webSiteId", webSiteId);
                templateContext.put("https", https);
                templateContext.put("rootDir", rootDir);

                Map<String, Object> templateRoot = FreeMarkerWorker.createEnvironmentMap(env);

                templateRoot.put("context", templateContext);
                try {
                    if (subContentId != null) {
                        ContentWorker.renderContentAsText(dispatcher, subContentId, out, templateRoot, locale, mimeTypeId, null, null, false);
                    } else {
                        ContentWorker.renderSubContentAsText(dispatcher, contentId, out, mapKey, templateRoot, locale, mimeTypeId, false);
                    }
                } catch (GeneralException e) {
                    Debug.logError(e, "Error rendering content", MODULE);
                    throw new IOException("Error rendering content" + e.toString());
                }
                templateContext.put("mapKey", null);
                templateContext.put("subContentId", null);
                templateContext.put("subDataResourceTypeId", null);
                templateContext.put("contentId", contentId);
                templateContext.put("mimeTypeId", null);
                templateContext.put("locale", locale);
            }
        };
    }
}
