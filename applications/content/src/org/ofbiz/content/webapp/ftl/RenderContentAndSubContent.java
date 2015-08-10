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
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.entity.Delegator;
import org.ofbiz.service.LocalDispatcher;

import freemarker.core.Environment;
import freemarker.template.TemplateTransformModel;

/**
 * RenderContentAndSubContent - Freemarker Transform for Content rendering
 * This transform cannot be called recursively (at this time).
 */
public class RenderContentAndSubContent implements TemplateTransformModel {

    public static final String module = RenderContentAndSubContent.class.getName();
//    public static final String [] upSaveKeyNames = {"globalNodeTrail"};
//    public static final String [] saveKeyNames = {"contentId", "subContentId", "subDataResourceTypeId", "mimeTypeId", "whenMap", "locale",  "wrapTemplateId", "encloseWrapText", "nullThruDatesOnly", "globalNodeTrail"};

    @SuppressWarnings("unchecked")
    public Writer getWriter(final Writer out, Map args) {
        final Environment env = Environment.getCurrentEnvironment();
        final LocalDispatcher dispatcher = FreeMarkerWorker.getWrappedObject("dispatcher", env);
        final Delegator delegator = FreeMarkerWorker.getWrappedObject("delegator", env);
        final HttpServletRequest request = FreeMarkerWorker.getWrappedObject("request", env);
        // final HttpServletResponse response = FreeMarkerWorker.getWrappedObject("response", env);
        final Map<String, Object> envMap = FreeMarkerWorker.createEnvironmentMap(env);
        final MapStack<String> templateRoot = MapStack.create();
        ((MapStack)templateRoot).push(envMap);
        if (Debug.verboseOn()) Debug.logVerbose("in RenderContentAndSubContent, contentId(0):" + templateRoot.get("contentId"), module);
        FreeMarkerWorker.getSiteParameters(request, templateRoot);
        // final Map savedValuesUp = new HashMap<String, Object>();
        // FreeMarkerWorker.saveContextValues(templateRoot, upSaveKeyNames, savedValuesUp);
        FreeMarkerWorker.overrideWithArgs(templateRoot, args);

        // final Map<String, Object> savedValues = new HashMap<String, Object>();

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
                renderSubContent();
                //if (Debug.verboseOn()) Debug.logVerbose("in Render(2), globalNodeTrail ." + getWrapped(env, "globalNodeTrail") , module);
            }

            public void renderSubContent() throws IOException {
                String mimeTypeId = (String) templateRoot.get("mimeTypeId");
                Object localeObject = templateRoot.get("locale");
                Locale locale = null;
                if (localeObject == null) {
                    locale = UtilHttp.getLocale(request);
                } else {
                    locale = UtilMisc.ensureLocale(localeObject);
                }

                if (Debug.verboseOn()) Debug.logVerbose("in RenderContentAndSubContent, contentId(2):" + templateRoot.get("contentId"), module);
                if (Debug.verboseOn()) Debug.logVerbose("in RenderContentAndSubContent, subContentId(2):" + templateRoot.get("subContentId"), module);
                //if (thisView != null) {
                    try {
                        String contentId = (String)templateRoot.get("contentId");
                        String mapKey = (String)templateRoot.get("mapKey");
                        String contentAssocTypeId = (String)templateRoot.get("contentAssocTypeId");
                        if (UtilValidate.isNotEmpty(mapKey) || UtilValidate.isNotEmpty(contentAssocTypeId)) {
                            String txt = ContentWorker.renderSubContentAsText(dispatcher, delegator, contentId, mapKey, templateRoot, locale, mimeTypeId, true);
                            //String txt = ContentWorker.renderSubContentAsTextCache(delegator, thisContentId, thisMapKey, null, templateRoot, locale, mimeTypeId, null, fromDate);
//                           if ("true".equals(xmlEscape)) {
//                                txt = UtilFormatOut.encodeXmlValue(txt);
//                            }

                            out.write(txt);

//                            if (Debug.infoOn()) Debug.logInfo("in RenderSubContent, after renderContentAsTextCache:", module);
//                                     List assocList = null;
//                            if (UtilValidate.isNotEmpty(contentAssocTypeId)) {
//                                assocList = UtilMisc.toList(contentAssocTypeId);
//                            }
//                            GenericValue content = ContentWorker.getSubContent(delegator, contentId, mapKey, null, null, assocList, null);
//                            if (content != null) {
//                              contentId = content.getString("contentId");
//                            } else {
//                              contentId = null;
//                            }
                        } else if (contentId != null) {
                            ContentWorker.renderContentAsText(dispatcher, delegator, contentId, out, templateRoot, locale, mimeTypeId, null, null, true);
//                            ((MapStack)templateRoot).pop();
                        }
                        //FreeMarkerWorker.reloadValues(templateRoot, savedValues, env);
                        //FreeMarkerWorker.reloadValues(templateRoot, savedValuesUp, env);

                    } catch (GeneralException e) {
                        String errMsg = "Error rendering thisContentId:" + (String)templateRoot.get("contentId") + " msg:" + e.toString();
                        Debug.logError(e, errMsg, module);
                        // just log a message and don't return anything: throw new IOException();
                    }
            }

        };
    }
}
