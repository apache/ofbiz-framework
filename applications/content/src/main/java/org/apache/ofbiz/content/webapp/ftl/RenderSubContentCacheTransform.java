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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;
import org.apache.ofbiz.content.content.ContentWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.webapp.control.RequestHandler;

import freemarker.core.Environment;
import freemarker.template.TemplateTransformModel;

/**
 * RenderSubContentCacheTransform - Freemarker Transform for Content rendering
 * This transform cannot be called recursively (at this time).
 */
public class RenderSubContentCacheTransform implements TemplateTransformModel {

    public static final String module = RenderSubContentCacheTransform.class.getName();
    static final String[] upSaveKeyNames = { "globalNodeTrail" };

    @Override
    @SuppressWarnings("unchecked")
    public Writer getWriter(Writer out, @SuppressWarnings("rawtypes") Map args) {
        final Environment env = Environment.getCurrentEnvironment();
        final LocalDispatcher dispatcher = FreeMarkerWorker.getWrappedObject("dispatcher", env);
        final Delegator delegator = FreeMarkerWorker.getWrappedObject("delegator", env);
        final HttpServletRequest request = FreeMarkerWorker.getWrappedObject("request", env);
        final HttpServletResponse response = FreeMarkerWorker.getWrappedObject("response", env);
        final Map<String, Object> templateRoot = FreeMarkerWorker.createEnvironmentMap(env);
        FreeMarkerWorker.getSiteParameters(request, templateRoot);
        final Map<String, Object> savedValuesUp = new HashMap<>();
        FreeMarkerWorker.saveContextValues(templateRoot, upSaveKeyNames, savedValuesUp);
        FreeMarkerWorker.overrideWithArgs(templateRoot, args);
        final GenericValue userLogin = FreeMarkerWorker.getWrappedObject("userLogin", env);
        List<Map<String, ? extends Object>> trail = UtilGenerics.cast(templateRoot.get("globalNodeTrail"));
        String contentAssocPredicateId = (String)templateRoot.get("contentAssocPredicateId");
        String strNullThruDatesOnly = (String)templateRoot.get("nullThruDatesOnly");
        Boolean nullThruDatesOnly = (strNullThruDatesOnly != null && "true".equalsIgnoreCase(strNullThruDatesOnly)) ? Boolean.TRUE :Boolean.FALSE;
        String thisSubContentId =  (String)templateRoot.get("subContentId");
        final boolean directAssocMode = UtilValidate.isNotEmpty(thisSubContentId) ? true : false;
        GenericValue val = null;
        try {
            val = ContentWorker.getCurrentContent(delegator, trail, userLogin, templateRoot, nullThruDatesOnly, contentAssocPredicateId);
        } catch (GeneralException e) {
            throw new RuntimeException("Error getting current content. " + e.toString());
        }
        final GenericValue view = val;

        String dataResourceId = null;
        String subContentIdSub = null;
        if (view != null) {
            try {
                dataResourceId = (String) view.get("drDataResourceId");
            } catch (IllegalArgumentException e) {
                dataResourceId = (String) view.get("dataResourceId");
            }
            subContentIdSub = (String) view.get("contentId");
        }
        // This order is taken so that the dataResourceType can be overridden in the transform arguments.
        String subDataResourceTypeId = (String)templateRoot.get("subDataResourceTypeId");

        if (UtilValidate.isEmpty(subDataResourceTypeId) && view != null ) {
            try {
                subDataResourceTypeId = (String) view.get("drDataResourceTypeId");
            } catch (IllegalArgumentException e) {
                // view may be "Content"
            }
            // TODO: If this value is still empty then it is probably necessary to get a value from
            // the parent context. But it will already have one and it is the same context that is
            // being passed.
        }
        String mimeTypeId = ContentWorker.getMimeTypeId(delegator, view, templateRoot);
        templateRoot.put("drDataResourceId", dataResourceId);
        templateRoot.put("mimeTypeId", mimeTypeId);
        templateRoot.put("dataResourceId", dataResourceId);
        templateRoot.put("subContentId", subContentIdSub);
        templateRoot.put("subDataResourceTypeId", subDataResourceTypeId);

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
                    FreeMarkerWorker.reloadValues(templateRoot, savedValuesUp, env);
                } catch (IOException e) {
                    throw new IOException(e.getMessage());
                }
            }

            public void renderSubContent() throws IOException {
                List<Map<String, ? extends Object>> passedGlobalNodeTrail = UtilGenerics.cast(templateRoot.get("globalNodeTrail"));
                String editRequestName = (String)templateRoot.get("editRequestName");
                GenericValue thisView = null;
                if (view != null) {
                    thisView = view;
                } else if (passedGlobalNodeTrail.size() > 0) {
                    Map<String, ? extends Object> map = UtilGenerics.cast(passedGlobalNodeTrail.get(passedGlobalNodeTrail.size() - 1));
                    if (Debug.infoOn()) {
                        Debug.logInfo("in Render(3), map ." + map , module);
                    }
                    if (map != null) {
                        thisView = (GenericValue)map.get("value");
                    }
                }

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
                            ContentWorker.renderContentAsText(dispatcher, contentId, out, templateRoot, locale, mimeTypeId, null, null, true);
                        } catch (GeneralException e) {
                            Debug.logError(e, "Error rendering content", module);
                            throw new IOException("Error rendering thisView:" + thisView + " msg:" + e.toString());
                        }
                    }
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
                        if (UtilValidate.isEmpty(contentId) && modelEntity.getField("caContentId") != null)
                            contentId = view.getString("caContentId");
                        if (UtilValidate.isEmpty(contentId) && modelEntity.getField("contentId") != null)
                            contentId = view.getString("contentId");
                        if (UtilValidate.isEmpty(contentIdTo) && modelEntity.getField("caContentIdTo") != null)
                            contentIdTo = view.getString("caContentIdTo");
                        if (UtilValidate.isEmpty(contentIdTo) && modelEntity.getField("contentIdTo") != null)
                            contentIdTo = view.getString("contentIdTo");
                        if (UtilValidate.isEmpty(contentAssocTypeId) && modelEntity.getField("caContentAssocTypeId") != null)
                            contentAssocTypeId = view.getString("caContentAssocTypeId");
                        if (UtilValidate.isEmpty(contentAssocTypeId) && modelEntity.getField("contentAssocTypeId") != null)
                            contentAssocTypeId = view.getString("contentAssocTypeId");
                        if (UtilValidate.isEmpty(mapKey) && modelEntity.getField("caMapKey") != null)
                            mapKey = view.getString("caMapKey");
                        if (UtilValidate.isEmpty(mapKey) && modelEntity.getField("mapKey") != null)
                            mapKey = view.getString("mapKey");
                        if (UtilValidate.isEmpty(fromDate) && modelEntity.getField("caFromDate") != null)
                            fromDate = view.getString("caFromDate");
                        if (UtilValidate.isEmpty(fromDate) && modelEntity.getField("fromDate") != null)
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
                out.write("<a href=\"");
                ServletContext servletContext = request.getSession().getServletContext();
                RequestHandler rh = (RequestHandler) servletContext.getAttribute("_REQUEST_HANDLER_");
                out.append(rh.makeLink(request, response, "/" + fullRequest, false, false, true));
                out.write("\">Edit</a>");
                out.write("</div>");
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
