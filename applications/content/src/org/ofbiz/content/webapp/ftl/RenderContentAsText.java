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
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.widget.WidgetWorker;
import org.ofbiz.service.LocalDispatcher;

import freemarker.core.Environment;
import freemarker.template.TemplateTransformModel;

/**
 * RenderContentAsText - Freemarker Transform for Content rendering
 * This transform cannot be called recursively (at this time).
 */
public class RenderContentAsText implements TemplateTransformModel {

    public static final String module = RenderContentAsText.class.getName();
    public static final String [] upSaveKeyNames = {"globalNodeTrail"};
    public static final String [] saveKeyNames = {"contentId", "subContentId", "subDataResourceTypeId", "mimeTypeId", "whenMap", "locale",  "wrapTemplateId", "encloseWrapText", "nullThruDatesOnly", "globalNodeTrail"};

    public Writer getWriter(final Writer out, Map args) {
        final Environment env = Environment.getCurrentEnvironment();
        //final Map templateCtx = (Map) FreeMarkerWorker.getWrappedObject("context", env);
        //final Map templateCtx = new HashMap();
        final LocalDispatcher dispatcher = (LocalDispatcher) FreeMarkerWorker.getWrappedObject("dispatcher", env);
        final GenericDelegator delegator = (GenericDelegator) FreeMarkerWorker.getWrappedObject("delegator", env);
        final HttpServletRequest request = (HttpServletRequest) FreeMarkerWorker.getWrappedObject("request", env);
        final HttpServletResponse response = (HttpServletResponse) FreeMarkerWorker.getWrappedObject("response", env);
        final Map templateRoot = FreeMarkerWorker.createEnvironmentMap(env);
                if (Debug.infoOn()) Debug.logInfo("in RenderSubContent, contentId(0):" + templateRoot.get("contentId"), module);
        FreeMarkerWorker.getSiteParameters(request, templateRoot);
        final Map savedValuesUp = new HashMap();
        FreeMarkerWorker.saveContextValues(templateRoot, upSaveKeyNames, savedValuesUp);
        FreeMarkerWorker.overrideWithArgs(templateRoot, args);
                if (Debug.infoOn()) Debug.logInfo("in RenderSubContent, contentId(2):" + templateRoot.get("contentId"), module);
        // not used yet: final GenericValue userLogin = (GenericValue) FreeMarkerWorker.getWrappedObject("userLogin", env);
        // not used yet: List trail = (List)templateRoot.get("globalNodeTrail");
        //if (Debug.infoOn()) Debug.logInfo("in Render(0), globalNodeTrail ." + trail , module);
        // not used yet: String contentAssocPredicateId = (String)templateRoot.get("contentAssocPredicateId");
        // not used yet: String strNullThruDatesOnly = (String)templateRoot.get("nullThruDatesOnly");
        // not used yet: Boolean nullThruDatesOnly = (strNullThruDatesOnly != null && strNullThruDatesOnly.equalsIgnoreCase("true")) ? Boolean.TRUE :Boolean.FALSE;
        final String thisContentId =  (String)templateRoot.get("contentId");
        final String xmlEscape =  (String)templateRoot.get("xmlEscape");
        final boolean directAssocMode = UtilValidate.isNotEmpty(thisContentId) ? true : false;
        if (Debug.infoOn()) Debug.logInfo("in Render(0), directAssocMode ." + directAssocMode , module);
        /*
        if (Debug.infoOn()) Debug.logInfo("in Render(0), thisSubContentId ." + thisSubContentId , module);
        String thisSubContentId =  (String)templateRoot.get("subContentId");
        GenericValue val = null;
        try {
            val = FreeMarkerWorker.getCurrentContent(delegator, trail, userLogin, templateRoot, nullThruDatesOnly, contentAssocPredicateId);
        } catch(GeneralException e) {
            throw new RuntimeException("Error getting current content. " + e.toString());
        }
        final GenericValue view = val;

        String dataResourceId = null;
        String subContentIdSub = null;
        if (view != null) {
            try {
                dataResourceId = (String) view.get("drDataResourceId");
            } catch (Exception e) {
                dataResourceId = (String) view.get("dataResourceId");
            }
            subContentIdSub = (String) view.get("contentId");
        }
        // This order is taken so that the dataResourceType can be overridden in the transform arguments.
        String subDataResourceTypeId = (String)templateRoot.get("subDataResourceTypeId");
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
        String mimeTypeId = FreeMarkerWorker.getMimeTypeId(delegator, view, templateRoot);
        templateRoot.put("drDataResourceId", dataResourceId);
        templateRoot.put("mimeTypeId", mimeTypeId);
        templateRoot.put("dataResourceId", dataResourceId);
        templateRoot.put("subContentId", subContentIdSub);
        templateRoot.put("subDataResourceTypeId", subDataResourceTypeId);
        */

        final Map savedValues = new HashMap();

        return new Writer(out) {

            public void write(char cbuf[], int off, int len) {
            }

            public void flush() throws IOException {
                out.flush();
            }

            public void close() throws IOException {
                List globalNodeTrail = (List)templateRoot.get("globalNodeTrail");
                if (Debug.infoOn()) Debug.logInfo("Render close, globalNodeTrail(2a):" + ContentWorker.nodeTrailToCsv(globalNodeTrail), "");
                renderSubContent();
                //if (Debug.infoOn()) Debug.logInfo("in Render(2), globalNodeTrail ." + getWrapped(env, "globalNodeTrail") , module);
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

                //TemplateHashModel dataRoot = env.getDataModel();
                // Timestamp fromDate = UtilDateTime.nowTimestamp();
                // List passedGlobalNodeTrail = (List)templateRoot.get("globalNodeTrail");
                String editRequestName = (String)templateRoot.get("editRequestName");
                 if (Debug.infoOn()) Debug.logInfo("in Render(3), editRequestName ." + editRequestName , module);
                 /*
                GenericValue thisView = null;
                if (view != null) {
                    thisView = view;
                } else if (passedGlobalNodeTrail.size() > 0) {
                    Map map = (Map)passedGlobalNodeTrail.get(passedGlobalNodeTrail.size() - 1);
                    if (Debug.infoOn()) Debug.logInfo("in Render(3), map ." + map , module);
                    if (map != null)
                        thisView = (GenericValue)map.get("value");
                }
                if (Debug.infoOn()) Debug.logInfo("in RenderSubContent, subContentId:" + templateRoot.get("subContentId"), module);
                if (Debug.infoOn()) Debug.logInfo("in RenderSubContent, contentId:" + templateRoot.get("contentId"), module);
                */

                if (UtilValidate.isNotEmpty(editRequestName)) {
                    String editStyle = getEditStyle();
                    openEditWrap(out, editStyle);
                }

                if (Debug.infoOn()) Debug.logInfo("in RenderSubContent, contentId(2):" + templateRoot.get("contentId"), module);
                if (Debug.infoOn()) Debug.logInfo("in RenderSubContent, subContentId(2):" + templateRoot.get("subContentId"), module);
                FreeMarkerWorker.saveContextValues(templateRoot, saveKeyNames, savedValues);
                //if (thisView != null) {
                    try {
                        String txt = ContentWorker.renderContentAsText(dispatcher, delegator, thisContentId, templateRoot, locale, mimeTypeId, true);
                        if ("true".equals(xmlEscape)) {
                            txt = UtilFormatOut.encodeXmlValue(txt);
                        }
                        
                        out.write(txt);
                        
                        // if (Debug.infoOn()) Debug.logInfo("in RenderSubContent, after renderContentAsTextCache:", module);
                    } catch (GeneralException e) {
                        String errMsg = "Error rendering thisContentId:" + thisContentId + " msg:" + e.toString();
                        Debug.logError(e, errMsg, module);
                        // just log a message and don't return anything: throw new IOException();
                    }
                //}
                FreeMarkerWorker.reloadValues(templateRoot, savedValuesUp, env);
                FreeMarkerWorker.reloadValues(templateRoot, savedValues, env);
                if (UtilValidate.isNotEmpty(editRequestName)) {
                    closeEditWrap(out, editRequestName);
                }

                //if (Debug.infoOn()) Debug.logInfo("in Render(4), globalNodeTrail ." + getWrapped(env, "globalNodeTrail") , module);
            }

            public void openEditWrap(Writer out, String editStyle) throws IOException {
                String divStr = "<div class=\"" + editStyle + "\">";
                out.write(divStr);
            }

            public void closeEditWrap(Writer out, String editRequestName) throws IOException {
                if (Debug.infoOn()) Debug.logInfo("in RenderSubContent, contentId(3):" + templateRoot.get("contentId"), module);
                if (Debug.infoOn()) Debug.logInfo("in RenderSubContent, subContentId(3):" + templateRoot.get("subContentId"), module);
                StringBuffer sb = new StringBuffer();
                String fullRequest = editRequestName;
                String contentId = null;
                contentId = (String)templateRoot.get("subContentId");
                String delim = "?";
                if (UtilValidate.isNotEmpty(contentId)) {
                    fullRequest += delim + "contentId=" + contentId;
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
