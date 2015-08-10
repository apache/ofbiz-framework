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
package org.ofbiz.widget.renderer;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.webapp.ftl.LoopWriter;
import org.ofbiz.widget.content.WidgetContentWorker;
import org.ofbiz.widget.renderer.html.HtmlMenuWrapper;

import freemarker.core.Environment;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTransformModel;
import freemarker.template.TransformControl;

//import com.clarkware.profiler.Profiler;
/**
 * MenuWrapTransform -  a FreeMarker transform that allow the ModelMenu
 * stuff to be used at the FM level. It can be used to add "function bars"
 * to pages.
 *
 * Accepts the following arguments (all of which can alternatively be present in the template context):
 * - List<Map<String, ? extends Object>> globalNodeTrail
 * - String contentAssocPredicateId
 * - String nullThruDatesOnly
 * - String subDataResourceTypeId
 * - String renderOnStart
 * - String renderOnClose
 * - String menuDefFile
 * - String menuName
 * - String menuWrapperClassName
 * - String associatedContentId
 *
 * This is an interactive FreeMarker transform that allows the user to modify the contents that are placed within it.
 */
public class MenuWrapTransform implements TemplateTransformModel {

    public static final String module = MenuWrapTransform.class.getName();
    public static final String [] upSaveKeyNames = {"globalNodeTrail"};
    public static final String [] saveKeyNames = {"contentId", "subContentId", "subDataResourceTypeId", "mimeTypeId", "whenMap", "locale",  "wrapTemplateId", "encloseWrapText", "nullThruDatesOnly", "renderOnStart", "renderOnClose", "menuDefFile", "menuName", "associatedContentId", "wrapperClassName"};

    
    @SuppressWarnings("rawtypes")
    public Writer getWriter(final Writer out, Map args) {
        final Environment env = Environment.getCurrentEnvironment();
        final Delegator delegator = FreeMarkerWorker.getWrappedObject("delegator", env);
        final HttpServletRequest request = FreeMarkerWorker.getWrappedObject("request", env);
        final HttpServletResponse response = FreeMarkerWorker.getWrappedObject("response", env);
        final HttpSession session = FreeMarkerWorker.getWrappedObject("session", env);

        final GenericValue userLogin = FreeMarkerWorker.getWrappedObject("userLogin", env);
        final Map<String, Object> templateCtx = FreeMarkerWorker.getWrappedObject("context", env);

        FreeMarkerWorker.getSiteParameters(request, templateCtx);

        final Map<String, Object> savedValuesUp = new HashMap<String, Object>();
        FreeMarkerWorker.saveContextValues(templateCtx, upSaveKeyNames, savedValuesUp);

        Map<String, Object> checkedArgs = UtilGenerics.checkMap(args);
        FreeMarkerWorker.overrideWithArgs(templateCtx, checkedArgs);
        //final String menuDefFile = (String)templateCtx.get("menuDefFile");
        //final String menuName = (String)templateCtx.get("menuName");
        //final String associatedContentId = (String)templateCtx.get("associatedContentId");
        List<Map<String, ? extends Object>> trail = UtilGenerics.checkList(templateCtx.get("globalNodeTrail"));
        String contentAssocPredicateId = (String)templateCtx.get("contentAssocPredicateId");
        String strNullThruDatesOnly = (String)templateCtx.get("nullThruDatesOnly");
        Boolean nullThruDatesOnly = (strNullThruDatesOnly != null && strNullThruDatesOnly.equalsIgnoreCase("true")) ? Boolean.TRUE :Boolean.FALSE;
        GenericValue val = null;
        try {
            if (WidgetContentWorker.contentWorker != null) {
                val = WidgetContentWorker.contentWorker.getCurrentContentExt(delegator, trail, userLogin, templateCtx, nullThruDatesOnly, contentAssocPredicateId);
            } else {
                Debug.logError("Not rendering content, not ContentWorker found.", module);
            }
        } catch (GeneralException e) {
            throw new RuntimeException("Error getting current content. " + e.toString());
        }
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
        // This order is taken so that the mimeType can be overridden in the transform arguments.
        String mimeTypeId = null;
        if (WidgetContentWorker.contentWorker != null) {
            mimeTypeId = WidgetContentWorker.contentWorker.getMimeTypeIdExt(delegator, view, templateCtx);
        } else {
            Debug.logError("Not rendering content, not ContentWorker found.", module);
        }
        templateCtx.put("drDataResourceId", dataResourceId);
        templateCtx.put("mimeTypeId", mimeTypeId);
        templateCtx.put("dataResourceId", dataResourceId);
        templateCtx.put("subContentIdSub", subContentIdSub);
        templateCtx.put("subDataResourceTypeId", subDataResourceTypeId);
        final Map<String, Object> savedValues = new HashMap<String, Object>();
        FreeMarkerWorker.saveContextValues(templateCtx, saveKeyNames, savedValues);

        final StringBuilder buf = new StringBuilder();

        return new LoopWriter(out) {

            @Override
            public int onStart() throws TemplateModelException, IOException {
                String renderOnStart = (String)templateCtx.get("renderOnStart");
                if (renderOnStart != null && renderOnStart.equalsIgnoreCase("true")) {
                    renderMenu();
                }
                return TransformControl.EVALUATE_BODY;
            }

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
                out.write(wrappedContent);
                String renderOnClose = (String)templateCtx.get("renderOnClose");
                if (renderOnClose == null || !renderOnClose.equalsIgnoreCase("false")) {
                    renderMenu();
                }
                FreeMarkerWorker.reloadValues(templateCtx, savedValuesUp, env);
            }

            public void renderMenu() throws IOException {

                String menuDefFile = (String)templateCtx.get("menuDefFile");
                String menuName = (String)templateCtx.get("menuName");
                String menuWrapperClassName = (String)templateCtx.get("menuWrapperClassName");
                HtmlMenuWrapper menuWrapper = HtmlMenuWrapper.getMenuWrapper(request, response, session, menuDefFile, menuName, menuWrapperClassName);

                if (menuWrapper == null) {
                    throw new IOException("HtmlMenuWrapper with def file:" + menuDefFile + " menuName:" + menuName + " and HtmlMenuWrapper class:" + menuWrapperClassName + " could not be instantiated.");
                }

                String associatedContentId = (String)templateCtx.get("associatedContentId");
                menuWrapper.putInContext("defaultAssociatedContentId", associatedContentId);
                menuWrapper.putInContext("currentValue", view);

                String menuStr = menuWrapper.renderMenuString();
                out.write(menuStr);
            }

        };
    }
}
