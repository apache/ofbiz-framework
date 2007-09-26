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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.widget.WidgetWorker;
import org.ofbiz.service.LocalDispatcher;

import freemarker.core.Environment;
import freemarker.template.TemplateTransformModel;

/**
 * RenderContentAndSubContent - Freemarker Transform for Content rendering
 * This transform cannot be called recursively (at this time).
 */
public class RenderContentAndSubContent implements TemplateTransformModel { 

    public static final String module = RenderContentAndSubContent.class.getName();

    public Writer getWriter(final Writer out, Map args) {
        final Environment env = Environment.getCurrentEnvironment();
        final LocalDispatcher dispatcher = (LocalDispatcher) FreeMarkerWorker.getWrappedObject("dispatcher", env);
        final GenericDelegator delegator = (GenericDelegator) FreeMarkerWorker.getWrappedObject("delegator", env);
        final HttpServletRequest request = (HttpServletRequest) FreeMarkerWorker.getWrappedObject("request", env);
        final HttpServletResponse response = (HttpServletResponse) FreeMarkerWorker.getWrappedObject("response", env);
        final Map envMap = FreeMarkerWorker.createEnvironmentMap(env);
        final Map templateRoot = MapStack.create(envMap);
        if (Debug.verboseOn()) Debug.logVerbose("in RenderContentAndSubContent, contentId(0):" + templateRoot.get("contentId"), module);
        FreeMarkerWorker.getSiteParameters(request, templateRoot);
        FreeMarkerWorker.overrideWithArgs(templateRoot, args);

        return new Writer(out) {

            public void write(char cbuf[], int off, int len) {
            }

            public void flush() throws IOException {
                out.flush();
            }

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
                        if (UtilValidate.isNotEmpty(mapKey) || UtilValidate.isNotEmpty(mapKey)) {
                            List assocList = null;
                            if (UtilValidate.isNotEmpty(contentAssocTypeId)) {
                                assocList = UtilMisc.toList(contentAssocTypeId);
                            }
                            GenericValue content = ContentWorker.getSubContent(delegator, contentId, mapKey, null, null, assocList, null);
                            if (content != null) { 
                              contentId = content.getString("contentId");
                            } else {
                              contentId = null;
                            }
                        } 
                        if (contentId != null) { 
                            ContentWorker.renderContentAsText(dispatcher, delegator, contentId, out, templateRoot, locale, mimeTypeId, true);
                        }
                        
                    } catch (GeneralException e) {
                        String errMsg = "Error rendering thisContentId:" + (String)templateRoot.get("contentId") + " msg:" + e.toString();
                        Debug.logError(e, errMsg, module);
                        // just log a message and don't return anything: throw new IOException();
                    }
            }

        };
    }
}
