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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;
import org.apache.ofbiz.content.content.ContentWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.LocalDispatcher;

import freemarker.core.Environment;
import freemarker.template.TemplateTransformModel;

/**
 * EditRenderSubContentCacheTransform - Freemarker Transform for URLs (links)
 *
 * This is an interactive FreeMarker tranform that allows the user to modify the contents that are placed within it.
 */
public class EditRenderSubContentCacheTransform implements TemplateTransformModel {

    public static final String module = EditRenderSubContentCacheTransform.class.getName();
    static final String [] saveKeyNames = {"contentId", "subContentId", "subDataResourceTypeId", "mimeTypeId", "whenMap", "locale",  "wrapTemplateId", "encloseWrapText", "nullThruDatesOnly"};

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
        final Map<String, Object> templateCtx = FreeMarkerWorker.getWrappedObject("context", env);
        final LocalDispatcher dispatcher = FreeMarkerWorker.getWrappedObject("dispatcher", env);
        final Delegator delegator = FreeMarkerWorker.getWrappedObject("delegator", env);
        final HttpServletRequest request = FreeMarkerWorker.getWrappedObject("request", env);
        FreeMarkerWorker.getSiteParameters(request, templateCtx);
        FreeMarkerWorker.overrideWithArgs(templateCtx, args);
        final GenericValue userLogin = FreeMarkerWorker.getWrappedObject("userLogin", env);
        List<Map<String, ? extends Object>> trail = UtilGenerics.cast(templateCtx.get("globalNodeTrail"));
        String contentAssocPredicateId = (String)templateCtx.get("contentAssocPredicateId");
        String strNullThruDatesOnly = (String)templateCtx.get("nullThruDatesOnly");
        Boolean nullThruDatesOnly = (strNullThruDatesOnly != null && "true".equalsIgnoreCase(strNullThruDatesOnly)) ? Boolean.TRUE :Boolean.FALSE;
        GenericValue val = null;
        try {
            val = ContentWorker.getCurrentContent(delegator, trail, userLogin, templateCtx, nullThruDatesOnly, contentAssocPredicateId);
        } catch (GeneralException e) {
            throw new RuntimeException("Error getting current content. " + e.toString());
        }
        final GenericValue view = val;

        String dataResourceId = null;
        try {
            dataResourceId = (String) view.get("drDataResourceId");
        } catch (IllegalArgumentException e) {
            dataResourceId = (String) view.get("dataResourceId");
        }
        String subContentIdSub = (String) view.get("contentId");
        // This order is taken so that the dataResourceType can be overridden in the transform arguments.
        String subDataResourceTypeId = (String)templateCtx.get("subDataResourceTypeId");
        if (UtilValidate.isEmpty(subDataResourceTypeId)) {
            try {
                subDataResourceTypeId = (String) view.get("drDataResourceTypeId");
            } catch (IllegalArgumentException e) {
                // view may be "Content"
            }
            // TODO: If this value is still empty then it is probably necessary to get a value from
            // the parent context. But it will already have one and it is the same context that is
            // being passed.
        }
        // This order is taken so that the mimeType can be overridden in the transform arguments.
        String mimeTypeId = ContentWorker.getMimeTypeId(delegator, view, templateCtx);
        templateCtx.put("drDataResourceId", dataResourceId);
        templateCtx.put("mimeTypeId", mimeTypeId);
        templateCtx.put("dataResourceId", dataResourceId);
        templateCtx.put("subContentIdSub", subContentIdSub);
        templateCtx.put("subDataResourceTypeId", subDataResourceTypeId);
        final Map<String, Object> savedValues = new HashMap<>();
        FreeMarkerWorker.saveContextValues(templateCtx, saveKeyNames, savedValues);

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
                FreeMarkerWorker.reloadValues(templateCtx, savedValues, env);
                String wrappedContent = buf.toString();
                String wrapTemplateId = (String)templateCtx.get("wrapTemplateId");
                if (UtilValidate.isNotEmpty(wrapTemplateId)) {
                    templateCtx.put("wrappedContent", wrappedContent);
                    Map<String, Object> templateRoot = null;
                    Map<String, Object> templateRootTemplate = UtilGenerics.cast(templateCtx.get("templateRootTemplate"));
                    if (templateRootTemplate == null) {
                        Map<String, Object> templateRootTmp = FreeMarkerWorker.createEnvironmentMap(env);
                        templateRoot = UtilMisc.makeMapWritable(templateRootTmp);
                        templateCtx.put("templateRootTemplate", templateRootTmp);
                    } else {
                        templateRoot = UtilMisc.makeMapWritable(templateRootTemplate);
                    }

                    templateRoot.put("context", templateCtx);
                    if (Debug.verboseOn()) {
                        for (Entry<String, Object> ky : templateCtx.entrySet()) {
                            Object val = ky.getValue();
                            Debug.logVerbose("context key: " + ky + " val: " + val, module);
                        }
                    }

                    String mimeTypeId = (String) templateCtx.get("mimeTypeId");
                    Locale locale = (Locale) templateCtx.get("locale");
                    if (locale == null) {
                        locale = Locale.getDefault();
                    }
                    try {
                        ContentWorker.renderContentAsText(dispatcher, wrapTemplateId, out, templateRoot, locale, mimeTypeId, null, null, true);
                    } catch (IOException | GeneralException e) {
                        Debug.logError(e, "Error rendering content" + e.getMessage(), module);
                        throw new IOException("Error rendering content" + e.toString());
                    }
                } else {
                    out.write(wrappedContent);
                }
            }
        };
    }
}
