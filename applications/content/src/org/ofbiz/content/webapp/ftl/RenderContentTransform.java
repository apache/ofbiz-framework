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
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.widget.WidgetWorker;

import freemarker.core.Environment;
import freemarker.template.TemplateTransformModel;

/**
 * RenderContentAsText - Freemarker Transform for Content rendering
 * This transform cannot be called recursively (at this time).
 */
public class RenderContentTransform implements TemplateTransformModel {

    public static final String module = RenderContentTransform.class.getName();

    public Writer getWriter(final Writer out, Map args) {
        final Environment env = Environment.getCurrentEnvironment();
        final GenericDelegator delegator = (GenericDelegator) FreeMarkerWorker.getWrappedObject("delegator", env);
        final LocalDispatcher dispatcher = (LocalDispatcher) FreeMarkerWorker.getWrappedObject("dispatcher", env);
        final HttpServletRequest request = (HttpServletRequest) FreeMarkerWorker.getWrappedObject("request", env);
        final HttpServletResponse response = (HttpServletResponse) FreeMarkerWorker.getWrappedObject("response", env);
        
        final Map templateRoot = MapStack.create(FreeMarkerWorker.createEnvironmentMap(env));
        ((MapStack)templateRoot).push(args);
        final String xmlEscape =  (String)templateRoot.get("xmlEscape");
        final String thisContentId = (String)templateRoot.get("contentId");

        return new Writer(out) {

            public void write(char cbuf[], int off, int len) {
            }

            public void flush() throws IOException {
                out.flush();
            }

            public void close() throws IOException {
                renderSubContent();
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

                String editRequestName = (String)templateRoot.get("editRequestName");

                if (UtilValidate.isNotEmpty(editRequestName)) {
                    String editStyle = getEditStyle();
                    openEditWrap(out, editStyle);
                }

                try {
                    String txt = null;
                    
                    String mapKey = (String)templateRoot.get("mapKey");
                    if( UtilValidate.isEmpty(mapKey)) {
                        txt = ContentWorker.renderContentAsText(dispatcher, delegator, thisContentId, templateRoot, locale, mimeTypeId, true);
                    } else {
                        txt = ContentWorker.renderSubContentAsText(dispatcher, delegator, thisContentId, mapKey, templateRoot, locale, mimeTypeId, true);
                    }
                    if ("true".equals(xmlEscape)) {
                        txt = UtilFormatOut.encodeXmlValue(txt);
                    }
                    
                    out.write(txt);
                    
                } catch (GeneralException e) {
                    String errMsg = "Error rendering thisContentId:" + thisContentId + " msg:" + e.toString();
                    Debug.logError(e, errMsg, module);
                    // just log a message and don't return anything: throw new IOException();
                }
                if (UtilValidate.isNotEmpty(editRequestName)) {
                    closeEditWrap(out, editRequestName);
                }

            }

            public void openEditWrap(Writer out, String editStyle) throws IOException {
                String divStr = "<div class=\"" + editStyle + "\">";
                out.write(divStr);
            }

            public void closeEditWrap(Writer out, String editRequestName) throws IOException {
                StringBuffer sb = new StringBuffer();
                String fullRequest = editRequestName;
                String delim = "?";
                if (UtilValidate.isNotEmpty(thisContentId)) {
                    fullRequest += delim + "contentId=" + thisContentId;
                    delim = "&";
                }
              
                WidgetWorker.appendOfbizUrl(sb, fullRequest, request, response);
                String url = sb.toString();
                String link = "<a href=\"" + url + "\">Edit</a>";
                out.write(link);
                String divStr = "</div>";
                out.write(divStr);
            }

            public String getEditStyle() {
                String editStyle = (String)templateRoot.get("editStyle");
                if (UtilValidate.isEmpty(editStyle)) {
                    editStyle = UtilProperties.getPropertyValue("content", "defaultEditStyle");
                }
                if (UtilValidate.isEmpty(editStyle)) {
                    editStyle = "buttontext";
                }
                return editStyle; 
            }
        };
    }
}
