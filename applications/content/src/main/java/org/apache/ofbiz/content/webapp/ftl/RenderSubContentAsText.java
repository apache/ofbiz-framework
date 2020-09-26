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

import javax.servlet.http.HttpServletRequest;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;
import org.apache.ofbiz.content.content.ContentWorker;
import org.apache.ofbiz.service.LocalDispatcher;

import freemarker.core.Environment;
import freemarker.template.TemplateTransformModel;
/**
 * RenderSubContentAsText - Freemarker Transform for Content rendering
 * This transform cannot be called recursively (at this time).
 */
public class RenderSubContentAsText implements TemplateTransformModel {

    private static final String MODULE = RenderSubContentAsText.class.getName();
    public static final String[] UP_SAVE_KEY_NAMES = {"globalNodeTrail"};
    public static final String[] SAVE_KEY_NAMES = {"contentId", "subContentId", "subDataResourceTypeId", "mimeTypeId", "whenMap", "locale",
            "wrapTemplateId", "encloseWrapText", "nullThruDatesOnly", "globalNodeTrail"};

    @Override
    @SuppressWarnings("unchecked")
    public Writer getWriter(Writer out, @SuppressWarnings("rawtypes") Map args) {
        final Environment env = Environment.getCurrentEnvironment();
        final LocalDispatcher dispatcher = FreeMarkerWorker.getWrappedObject("dispatcher", env);
        final HttpServletRequest request = FreeMarkerWorker.getWrappedObject("request", env);
        final Map<String, Object> templateRoot = FreeMarkerWorker.createEnvironmentMap(env);
        if (Debug.infoOn()) {
            Debug.logInfo("in RenderSubContent, contentId(0):" + templateRoot.get("contentId"), MODULE);
        }
        FreeMarkerWorker.getSiteParameters(request, templateRoot);
        final Map<String, Object> savedValuesUp = new HashMap<>();
        FreeMarkerWorker.saveContextValues(templateRoot, UP_SAVE_KEY_NAMES, savedValuesUp);
        FreeMarkerWorker.overrideWithArgs(templateRoot, args);
        if (Debug.infoOn()) {
            Debug.logInfo("in RenderSubContent, contentId(2):" + templateRoot.get("contentId"), MODULE);
        }
        final String thisContentId = (String) templateRoot.get("contentId");
        final String thisMapKey = (String) templateRoot.get("mapKey");
        final String xmlEscape = (String) templateRoot.get("xmlEscape");
        if (Debug.infoOn()) {
            Debug.logInfo("in Render(0), thisSubContentId ." + thisContentId, MODULE);
        }
        final boolean directAssocMode = UtilValidate.isNotEmpty(thisContentId) ? true : false;
        if (Debug.infoOn()) {
            Debug.logInfo("in Render(0), directAssocMode ." + directAssocMode, MODULE);
        }

        final Map<String, Object> savedValues = new HashMap<>();

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
                List<Map<String, ? extends Object>> globalNodeTrail = UtilGenerics.cast(templateRoot.get("globalNodeTrail"));
                if (Debug.infoOn()) {
                    Debug.logInfo("Render close, globalNodeTrail(2a):" + ContentWorker.nodeTrailToCsv(globalNodeTrail), "");
                }
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

                String editRequestName = (String) templateRoot.get("editRequestName");
                if (Debug.infoOn()) {
                    Debug.logInfo("in Render(3), editRequestName ." + editRequestName, MODULE);
                }

                if (UtilValidate.isNotEmpty(editRequestName)) {
                    String editStyle = getEditStyle();
                    openEditWrap(out, editStyle);
                }

                FreeMarkerWorker.saveContextValues(templateRoot, SAVE_KEY_NAMES, savedValues);
                try {
                    String txt = ContentWorker.renderSubContentAsText(dispatcher, thisContentId, thisMapKey, templateRoot, locale, mimeTypeId, true);
                    if ("true".equals(xmlEscape)) {
                        txt = UtilFormatOut.encodeXmlValue(txt);
                    }

                    out.write(txt);

                    if (Debug.infoOn()) {
                        Debug.logInfo("in RenderSubContent, after renderContentAsTextCache:", MODULE);
                    }
                } catch (GeneralException e) {
                    String errMsg = "Error rendering thisContentId:" + thisContentId + " msg:" + e.toString();
                    Debug.logError(e, errMsg, MODULE);
                }
                FreeMarkerWorker.reloadValues(templateRoot, savedValues, env);
                FreeMarkerWorker.reloadValues(templateRoot, savedValuesUp, env);
                if (UtilValidate.isNotEmpty(editRequestName)) {
                    closeEditWrap(out, editRequestName);
                }

            }

            public void openEditWrap(Writer out, String editStyle) throws IOException {

                String divStr = "<div class=\"" + editStyle + "\">";
                out.write(divStr);
            }

            public void closeEditWrap(Writer out, String editRequestName) throws IOException {

            }

            public String getEditStyle() {
                String editStyle = (String) templateRoot.get("editStyle");
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
