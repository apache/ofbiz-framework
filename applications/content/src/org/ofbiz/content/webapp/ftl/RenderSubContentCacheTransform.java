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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.widget.WidgetWorker;
import org.ofbiz.service.LocalDispatcher;

import freemarker.core.Environment;
import freemarker.template.TemplateTransformModel;
//import com.clarkware.profiler.Profiler;

/**
 * RenderSubContentCacheTransform - Freemarker Transform for Content rendering
 * This transform cannot be called recursively (at this time).
 */
public class RenderSubContentCacheTransform implements TemplateTransformModel {

    public static final String module = RenderSubContentCacheTransform.class.getName();
    public static final String [] upSaveKeyNames = {"globalNodeTrail"};
    public static final String [] saveKeyNames = {"contentId", "subContentId", "subDataResourceTypeId", "mimeTypeId", "whenMap", "locale",  "wrapTemplateId", "encloseWrapText", "nullThruDatesOnly", "globalNodeTrail"};

    public Writer getWriter(final Writer out, Map args) {
        final StringBuffer buf = new StringBuffer();
        final Environment env = Environment.getCurrentEnvironment();
        //final Map templateCtx = (Map) FreeMarkerWorker.getWrappedObject("context", env);
        //final Map templateCtx = new HashMap();
        final LocalDispatcher dispatcher = (LocalDispatcher) FreeMarkerWorker.getWrappedObject("dispatcher", env);
        final GenericDelegator delegator = (GenericDelegator) FreeMarkerWorker.getWrappedObject("delegator", env);
        final HttpServletRequest request = (HttpServletRequest) FreeMarkerWorker.getWrappedObject("request", env);
        final HttpServletResponse response = (HttpServletResponse) FreeMarkerWorker.getWrappedObject("response", env);
        final Map templateRoot = FreeMarkerWorker.createEnvironmentMap(env);
        //if (Debug.infoOn()) Debug.logInfo("in RenderSubContent, contentId(0):" + templateRoot.get( "contentId"), module);
        FreeMarkerWorker.getSiteParameters(request, templateRoot);
        final Map savedValuesUp = new HashMap();
        FreeMarkerWorker.saveContextValues(templateRoot, upSaveKeyNames, savedValuesUp);
        FreeMarkerWorker.overrideWithArgs(templateRoot, args);
        //if (Debug.infoOn()) Debug.logInfo("in RenderSubContent, contentId(2):" + templateRoot.get( "contentId"), module);
        final GenericValue userLogin = (GenericValue) FreeMarkerWorker.getWrappedObject("userLogin", env);
        List trail = (List)templateRoot.get( "globalNodeTrail");
        //if (Debug.infoOn()) Debug.logInfo("in Render(0), globalNodeTrail ." + trail , module);
        String contentAssocPredicateId = (String)templateRoot.get( "contentAssocPredicateId");
        String strNullThruDatesOnly = (String)templateRoot.get( "nullThruDatesOnly");
        Boolean nullThruDatesOnly = (strNullThruDatesOnly != null && strNullThruDatesOnly.equalsIgnoreCase("true")) ? Boolean.TRUE :Boolean.FALSE;
        String thisSubContentId =  (String)templateRoot.get("subContentId");
        //if (Debug.infoOn()) Debug.logInfo("in Render(0), thisSubContentId ." + thisSubContentId , module);
        final boolean directAssocMode = UtilValidate.isNotEmpty(thisSubContentId) ? true : false;
        //if (Debug.infoOn()) Debug.logInfo("in Render(0), directAssocMode ." + directAssocMode , module);
        GenericValue val = null;
        try {
            val = ContentWorker.getCurrentContent(delegator, trail, userLogin, templateRoot, nullThruDatesOnly, contentAssocPredicateId);
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
        String subDataResourceTypeId = (String)templateRoot.get( "subDataResourceTypeId");
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
        String mimeTypeId = ContentWorker.getMimeTypeId(delegator, view, templateRoot);
        templateRoot.put( "drDataResourceId", dataResourceId);
        templateRoot.put( "mimeTypeId", mimeTypeId);
        templateRoot.put( "dataResourceId", dataResourceId);
        templateRoot.put( "subContentId", subContentIdSub);
        templateRoot.put( "subDataResourceTypeId", subDataResourceTypeId);

        //final Map savedValues = new HashMap();
        //FreeMarkerWorker.saveContextValues(templateCtx, saveKeyNames, savedValues);

        return new Writer(out) {

            public void write(char cbuf[], int off, int len) {
            }

            public void flush() throws IOException {
                out.flush();
            }

            public void close() throws IOException {
                List globalNodeTrail = (List)templateRoot.get("globalNodeTrail");
                //if (Debug.infoOn()) Debug.logInfo("Render close, globalNodeTrail(2a):" + FreeMarkerWorker.nodeTrailToCsv(globalNodeTrail), "");
                try {
                    renderSubContent();
                FreeMarkerWorker.reloadValues(templateRoot, savedValuesUp, env);
                 //if (Debug.infoOn()) Debug.logInfo("in Render(2), globalNodeTrail ." + getWrapped(env, "globalNodeTrail") , module);
                } catch (IOException e) {
                    throw new IOException(e.getMessage());
                }
            }

            public void renderSubContent() throws IOException {

                //TemplateHashModel dataRoot = env.getDataModel();
                Timestamp fromDate = UtilDateTime.nowTimestamp();
                List passedGlobalNodeTrail = (List)templateRoot.get("globalNodeTrail");
                String editRequestName = (String)templateRoot.get("editRequestName");
                //if (Debug.infoOn()) Debug.logInfo("in Render(3), editRequestName ." + editRequestName , module);
                GenericValue thisView = null;
                if (view != null) {
                    thisView = view;
                } else if (passedGlobalNodeTrail.size() > 0) {
                    Map map = (Map)passedGlobalNodeTrail.get(passedGlobalNodeTrail.size() - 1);
                    if (Debug.infoOn()) Debug.logInfo("in Render(3), map ." + map , module);
                    if (map != null)
                        thisView = (GenericValue)map.get("value");
                }
                //if (Debug.infoOn()) Debug.logInfo("in RenderSubContent, subContentId:" + templateRoot.get( "subContentId"), module);
                //if (Debug.infoOn()) Debug.logInfo("in RenderSubContent, contentId:" + templateRoot.get( "contentId"), module);

                String mimeTypeId = (String) templateRoot.get("mimeTypeId");
                Locale locale = (Locale) templateRoot.get("locale");
                if (locale == null)
                    locale = Locale.getDefault();

                if (UtilValidate.isNotEmpty(editRequestName)) {
                    String editStyle = getEditStyle();
                    openEditWrap(out, editStyle);
                }

                if (thisView != null) {
                    String contentId = thisView.getString("contentId");
                    if (contentId != null) {
                        try {
                            ContentWorker.renderContentAsText(dispatcher, delegator, contentId, out, templateRoot, locale, mimeTypeId, true);
                            //if (Debug.infoOn()) Debug.logInfo("in RenderSubContent, after renderContentAsTextCache:", module);
                        } catch (GeneralException e) {
                            Debug.logError(e, "Error rendering content", module);
                            throw new IOException("Error rendering thisView:" + thisView + " msg:" + e.toString());
                        }
                    }
                }
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
                //if (Debug.infoOn()) Debug.logInfo("in RenderSubContent, contentId(1):" + templateRoot.get( "contentId"), module);
                //if (Debug.infoOn()) Debug.logInfo("in Render(0), templateRoot ." + templateRoot , module);
                StringBuffer sb = new StringBuffer();
                String fullRequest = editRequestName;
                String contentId = null;
                String contentIdTo = null;
                String contentAssocTypeId = null;
                String mapKey = null;
                String fromDate = null;
             
                if (!directAssocMode) {
                    contentIdTo = (String)templateRoot.get("contentId");
                    contentAssocTypeId = (String)templateRoot.get("contentAssocTypeId");
                    mapKey = (String)templateRoot.get("mapKey");
                    fromDate = (String)templateRoot.get("fromDate");
                    if (Debug.infoOn()) Debug.logInfo("in Render(0), view ." + view , module);
                    if (view != null) {
                        ModelEntity modelEntity = view.getModelEntity();
                        if (UtilValidate.isEmpty(contentId) && modelEntity.getField("caContentId") != null )
                            contentId = view.getString("caContentId");
                        if (UtilValidate.isEmpty(contentId) && modelEntity.getField("contentId") != null )
                            contentId = view.getString("contentId");
                        if (UtilValidate.isEmpty(contentIdTo) && modelEntity.getField("caContentIdTo") != null )
                            contentIdTo = view.getString("caContentIdTo");
                        if (UtilValidate.isEmpty(contentIdTo) && modelEntity.getField("contentIdTo") != null )
                            contentIdTo = view.getString("contentIdTo");
                        if (UtilValidate.isEmpty(contentAssocTypeId) && modelEntity.getField("caContentAssocTypeId") != null )
                            contentAssocTypeId = view.getString("caContentAssocTypeId");
                        if (UtilValidate.isEmpty(contentAssocTypeId) && modelEntity.getField("contentAssocTypeId") != null )
                            contentAssocTypeId = view.getString("contentAssocTypeId");
                        if (UtilValidate.isEmpty(mapKey) && modelEntity.getField("caMapKey") != null )
                            mapKey = view.getString("caMapKey");
                        if (UtilValidate.isEmpty(mapKey) && modelEntity.getField("mapKey") != null )
                            mapKey = view.getString("mapKey");
                        if (UtilValidate.isEmpty(fromDate) && modelEntity.getField("caFromDate") != null )
                            fromDate = view.getString("caFromDate");
                        if (UtilValidate.isEmpty(fromDate) && modelEntity.getField("fromDate") != null )
                            fromDate = view.getString("fromDate");
                    }
                } else {
                    contentId = (String)templateRoot.get("subContentId");
                }
                if (Debug.infoOn()) Debug.logInfo("in Render(0), contentIdTo ." + contentIdTo , module);
                String delim = "?";
                if (UtilValidate.isNotEmpty(contentId)) {
                    fullRequest += delim + "contentId=" + contentId;
                    delim = "&";
                }
                if (UtilValidate.isNotEmpty(contentIdTo)) {
                    fullRequest += delim + "contentIdTo=" + contentIdTo;
                    delim = "&";
                }
                if (UtilValidate.isNotEmpty(contentAssocTypeId)) {
                    fullRequest += delim + "contentAssocTypeId=" + contentAssocTypeId;
                    delim = "&";
                }
                if (UtilValidate.isNotEmpty(mapKey)) {
                    fullRequest += delim + "mapKey=" + mapKey;
                    delim = "&";
                }
                if (UtilValidate.isNotEmpty(fromDate)) {
                    fullRequest += delim + "fromDate=" + fromDate;
                    delim = "&";
                }
              
                if (Debug.infoOn()) Debug.logInfo("in Render(2), contentIdTo ." + contentIdTo , module);
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
