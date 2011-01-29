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
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.LocalDispatcher;

import freemarker.core.Environment;
import freemarker.template.TemplateTransformModel;

//import com.clarkware.profiler.Profiler;
/**
 * WrapSubContentCacheTransform - Freemarker Transform for URLs (links)
 *
 * This is an interactive FreeMarker tranform that allows the user to modify the contents that are placed within it.
 */
public class WrapSubContentCacheTransform implements TemplateTransformModel {

    public static final String module = WrapSubContentCacheTransform.class.getName();
    public static final String [] upSaveKeyNames = {"globalNodeTrail"};
    public static final String [] saveKeyNames = {"contentId", "subContentId", "subDataResourceTypeId", "mimeTypeId", "whenMap", "locale",  "wrapTemplateId", "encloseWrapText", "nullThruDatesOnly"};

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
        Map<String, Object> envContext = FreeMarkerWorker.getWrappedObject("context", env);
        final Map<String, Object> templateCtx;
        if (envContext == null) {
            templateCtx = FreeMarkerWorker.createEnvironmentMap(env);
        } else {
            templateCtx = envContext;
        }
        final LocalDispatcher dispatcher = FreeMarkerWorker.getWrappedObject("dispatcher", env);
        final Delegator delegator = FreeMarkerWorker.getWrappedObject("delegator", env);
        final HttpServletRequest request = FreeMarkerWorker.getWrappedObject("request", env);
        FreeMarkerWorker.getSiteParameters(request, templateCtx);
        final Map<String, Object> savedValuesUp = FastMap.newInstance();
        FreeMarkerWorker.saveContextValues(templateCtx, upSaveKeyNames, savedValuesUp);
        //if (Debug.infoOn()) Debug.logInfo("in Wrap(0a), savedValuesUp ." + savedValuesUp , module);
        FreeMarkerWorker.overrideWithArgs(templateCtx, args);
        //if (Debug.infoOn()) Debug.logInfo("in Wrap(0b), savedValuesUp ." + savedValuesUp , module);
        final String wrapTemplateId = (String)templateCtx.get("wrapTemplateId");
        //if (Debug.infoOn()) Debug.logInfo("in WrapSubContent, wrapTemplateId(1):" + wrapTemplateId, module);
        final GenericValue userLogin = FreeMarkerWorker.getWrappedObject("userLogin", env);
        List<Map<String, ? extends Object>> trail = UtilGenerics.checkList(templateCtx.get("globalNodeTrail"));
        //if (Debug.infoOn()) Debug.logInfo("in WrapSubContent, trail(0):" + trail, "");
        String contentAssocPredicateId = (String)templateCtx.get("contentAssocPredicateId");
        String strNullThruDatesOnly = (String)templateCtx.get("nullThruDatesOnly");
        Boolean nullThruDatesOnly = (strNullThruDatesOnly != null && strNullThruDatesOnly.equalsIgnoreCase("true")) ? Boolean.TRUE :Boolean.FALSE;
        GenericValue val = null;
        try {
            val = ContentWorker.getCurrentContent(delegator, trail, userLogin, templateCtx, nullThruDatesOnly, contentAssocPredicateId);
        } catch (GeneralException e) {
            throw new RuntimeException("Error getting current content. " + e.toString());
        }
                //if (Debug.infoOn()) Debug.logInfo("in WrapSubContent, trail(1):" + trail, "");
        final GenericValue view = val;

        String dataResourceId = null;
        try {
            dataResourceId = (String) view.get("drDataResourceId");
        } catch (Exception e) {
            dataResourceId = (String) view.get("dataResourceId");
        }
        String subContentIdSub = (String) view.get("contentId");
        // This order is taken so that the dataResourceType can be overridden in the transform arguments.
        String subDataResourceTypeId = (String)templateCtx.get("subDataResourceTypeId");
        if (UtilValidate.isEmpty(subDataResourceTypeId)) {
            try {
                subDataResourceTypeId = (String) view.get("drDataResourceTypeId");
            } catch (Exception e) {
                // view may be "Content"
            }
            // TODO: If this value is still empty then it is probably necessary to get a value from
            // the parent context. But it will already have one and it is the same context that is
            // being passed.
        }
        final Map<String, Object> savedValues = FastMap.newInstance();
        FreeMarkerWorker.saveContextValues(templateCtx, saveKeyNames, savedValues);
        //if (Debug.infoOn()) Debug.logInfo("in Wrap(1), savedValues ." + savedValues , module);
        // This order is taken so that the mimeType can be overridden in the transform arguments.
        String mimeTypeId = ContentWorker.getMimeTypeId(delegator, view, templateCtx);
        templateCtx.put("drDataResourceId", dataResourceId);
        templateCtx.put("mimeTypeId", mimeTypeId);
        templateCtx.put("dataResourceId", dataResourceId);
        templateCtx.put("subContentIdSub", subContentIdSub);
        templateCtx.put("subDataResourceTypeId", subDataResourceTypeId);
        templateCtx.put("wrapTemplateId", null); // Not something to pass on

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
                //if (Debug.infoOn()) Debug.logInfo("in Wrap(2), savedValues ." + savedValues , module);
                String wrappedContent = buf.toString();

                //if (view != null && Debug.infoOn()) Debug.logInfo("in WrapSubContent, view(2):" + view.get("contentId"), module);
                //if (Debug.infoOn()) Debug.logInfo("in WrapSubContent, wrappedContent:" + wrappedContent, module);
                //if (Debug.infoOn()) Debug.logInfo("in WrapSubContent, wrapTemplateId(2):" + wrapTemplateId, module);
                if (UtilValidate.isNotEmpty(wrapTemplateId)) {
                    templateCtx.put("wrappedContent", wrappedContent);

                    //Map templateRoot = FreeMarkerWorker.createEnvironmentMap(env);
                    Map<String, Object> templateRoot = null;
                    Map<String, Object> templateRootTemplate = UtilGenerics.checkMap(templateCtx.get("templateRootTemplate"));
                    if (templateRootTemplate == null) {
                        Map<String, Object> templateRootTmp = FreeMarkerWorker.createEnvironmentMap(env);
                        templateRoot = UtilMisc.makeMapWritable(templateRootTmp);
                        templateCtx.put("templateRootTemplate", templateRootTmp);
                    } else {
                        templateRoot = UtilMisc.makeMapWritable(templateRootTemplate);
                    }

                    templateRoot.put("context", templateCtx);

                    String mimeTypeId = (String)templateCtx.get("mimeTypeId");
                    Locale locale = null;
                    try {
                        //if (Debug.infoOn()) Debug.logInfo("in Edit(0), before calling renderContentAsText ." , module);
                        ContentWorker.renderContentAsText(dispatcher, delegator, wrapTemplateId, out, templateRoot, locale, mimeTypeId, null, null, true);
                        //if (Debug.infoOn()) Debug.logInfo("in Edit(0), after calling renderContentAsText ." , module);
                    } catch (IOException e) {
                        Debug.logError(e, "Error rendering content" + e.getMessage(), module);
                        throw new IOException("Error rendering content" + e.toString());
                    } catch (GeneralException e2) {
                        Debug.logError(e2, "Error rendering content" + e2.getMessage(), module);
                        throw new IOException("Error rendering content" + e2.toString());
                    }
                    FreeMarkerWorker.reloadValues(templateCtx, savedValuesUp, env);
                    //if (Debug.infoOn()) Debug.logInfo("in Wrap(2), savedValuesUp ." + savedValuesUp , module);
                }
            }
        };
    }
}
