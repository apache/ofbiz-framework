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
package org.apache.ofbiz.widget.renderer;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.collections.MapStack;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.webapp.control.ExternalLoginKeysManager;
import org.apache.ofbiz.webapp.control.LoginWorker;
import org.apache.ofbiz.webapp.website.WebSiteWorker;
import org.apache.ofbiz.widget.cache.GenericWidgetOutput;
import org.apache.ofbiz.widget.cache.ScreenCache;
import org.apache.ofbiz.widget.cache.WidgetContextCacheKey;
import org.apache.ofbiz.widget.model.ModelScreen;
import org.apache.ofbiz.widget.model.ScreenFactory;
import org.apache.ofbiz.widget.model.ScriptLinkHelper;
import org.apache.ofbiz.widget.model.ThemeFactory;
import org.xml.sax.SAXException;

import freemarker.ext.jsp.TaglibFactory;
import freemarker.ext.servlet.HttpRequestHashModel;
import freemarker.ext.servlet.HttpSessionHashModel;
import freemarker.ext.servlet.ServletContextHashModel;

/**
 * Widget Library - Screen model class
 */
public class ScreenRenderer {

    private static final String MODULE = ScreenRenderer.class.getName();

    private Appendable writer;
    private MapStack<String> context;
    private ScreenStringRenderer screenStringRenderer;
    private int renderFormSeqNumber = 0;

    public ScreenRenderer(Appendable writer, MapStack<String> context, ScreenStringRenderer screenStringRenderer) {
        this.writer = writer;
        this.context = context;
        if (this.context == null) {
            this.context = MapStack.create();
        }
        this.screenStringRenderer = screenStringRenderer;
    }

    /**
     * Renders the named screen using the render environment configured when this ScreenRenderer was created.
     * @param combinedName A combination of the resource name/location for the screen XML file and the name of the screen within that file,
     * separated by a pound sign ("#"). This is the same format that is used in the view-map elements on the controller.xml file.
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public String render(String combinedName) throws GeneralException, IOException, SAXException, ParserConfigurationException {
        String resourceName = ScreenFactory.getResourceNameFromCombined(combinedName);
        String screenName = ScreenFactory.getScreenNameFromCombined(combinedName);
        this.render(resourceName, screenName);
        return "";
    }

    /**
     * Renders the named screen using the render environment configured when this ScreenRenderer was created.
     * @param resourceName The name/location of the resource to use, can use "component://[component-name]/" and "ofbiz://"
     * and other special OFBiz style URLs
     * @param screenName The name of the screen within the XML file specified by the resourceName.
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public String render(String resourceName, String screenName) throws GeneralException, IOException, SAXException, ParserConfigurationException {
        ModelScreen modelScreen = ScreenFactory.getScreenFromLocation(resourceName, screenName);
        if (modelScreen.getUseCache()) {
            // if in the screen definition use-cache is set to true
            // then try to get an already built screen output from the cache:
            // 1) if we find it then we get it and attach it to the passed in writer
            // 2) if we can't find one, we create a new StringWriter,
            //    and pass it to the renderScreenString;
            //    then we wrap its content and put it in the cache;
            //    and we attach it to the passed in writer
            WidgetContextCacheKey wcck = new WidgetContextCacheKey(context);
            String screenCombinedName = resourceName + ":" + screenName;
            ScreenCache screenCache = new ScreenCache();
            GenericWidgetOutput gwo = screenCache.get(screenCombinedName, wcck);
            if (gwo == null) {
                Writer sw = new StringWriter();
                modelScreen.renderScreenString(sw, context, screenStringRenderer);
                gwo = new GenericWidgetOutput(sw.toString());
                screenCache.put(screenCombinedName, wcck, gwo);
                writer.append(gwo.toString());
            } else {
                writer.append(gwo.toString());
            }
        } else {
            context.put("renderFormSeqNumber", String.valueOf(renderFormSeqNumber));
            if (context.get(ScriptLinkHelper.FTL_WRITER) != null) {
                Stack<StringWriter> stringWriterStack = UtilGenerics.cast(context.get(ScriptLinkHelper.FTL_WRITER));
                modelScreen.renderScreenString(stringWriterStack.peek(), context, screenStringRenderer);
            } else {
                modelScreen.renderScreenString(writer, context, screenStringRenderer);
            }
        }
        return "";
    }

    /**
     * Sets render form unique seq.
     * @param renderFormSeqNumber the render form seq number
     */
    public void setRenderFormUniqueSeq(int renderFormSeqNumber) {
        this.renderFormSeqNumber = renderFormSeqNumber;
    }

    /**
     * Gets screen string renderer.
     * @return the screen string renderer
     */
    public ScreenStringRenderer getScreenStringRenderer() {
        return this.screenStringRenderer;
    }

    /**
     * Populate basic context.
     * @param parameters the parameters
     * @param delegator  the delegator
     * @param dispatcher the dispatcher
     * @param security   the security
     * @param locale     the locale
     * @param userLogin  the user login
     */
    public void populateBasicContext(Map<String, Object> parameters, Delegator delegator, LocalDispatcher dispatcher,
                                     Security security, Locale locale, GenericValue userLogin) {
        populateBasicContext(context, this, parameters, delegator, dispatcher, security, locale, userLogin);
    }

    public static void populateBasicContext(MapStack<String> context, ScreenRenderer screens, Map<String, Object> parameters,
                Delegator delegator, LocalDispatcher dispatcher, Security security, Locale locale, GenericValue userLogin) {
        // ========== setup values that should always be in a screen context
        // include an object to more easily render screens
        context.put("screens", screens);

        // include an object to follow the screen stack during the screen rendering process
        context.put("screenStack", new ScreenRenderer.ScreenStack());

        // make a reference for high level variables, a global context
        context.put("globalContext", context.standAloneStack());

        // make sure the "nullField" object is in there for entity ops; note this is nullField and not null because as null
        // causes problems in FreeMarker and such...
        context.put("nullField", GenericEntity.NULL_FIELD);

        context.put("parameters", parameters);
        context.put("delegator", delegator);
        context.put("dispatcher", dispatcher);
        context.put("security", security);
        context.put("locale", locale);
        context.put("userLogin", userLogin);
        context.put("nowTimestamp", UtilDateTime.nowTimestamp());
        try {
            Map<String, Object> result = dispatcher.runSync("getUserPreferenceGroup",
                    UtilMisc.toMap("userLogin", userLogin, "userPrefGroupTypeId", "GLOBAL_PREFERENCES"));
            context.put("userPreferences", result.get("userPrefMap"));
        } catch (GenericServiceException e) {
            Debug.logError(e, "Error while getting user preferences: ", MODULE);
        }
    }

    /**
     * This method populates the context for this ScreenRenderer based on the HTTP Request and Response objects and the ServletContext.
     * It leverages various conventions used in other places, namely the ControlServlet and so on, of OFBiz to get the different resources needed.
     * @param request
     * @param response
     * @param servletContext
     */
    public void populateContextForRequest(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext) {
        populateContextForRequest(context, this, request, response, servletContext);
    }

    public static void populateContextForRequest(MapStack<String> context, ScreenRenderer screens, HttpServletRequest request,
                                                 HttpServletResponse response, ServletContext servletContext) {
        HttpSession session = request.getSession();

        // attribute names to skip for session and application attributes; these are all handled as special cases,
        // duplicating results and causing undesired messages
        Set<String> attrNamesToSkip = UtilMisc.toSet("delegator", "dispatcher", "security", "webSiteId",
                "org.apache.catalina.jsp_classpath");
        Map<String, Object> parameterMap = UtilHttp.getCombinedMap(request, attrNamesToSkip);

        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");

        populateBasicContext(context, screens, parameterMap, (Delegator) request.getAttribute("delegator"),
                (LocalDispatcher) request.getAttribute("dispatcher"),
                (Security) request.getAttribute("security"), UtilHttp.getLocale(request), userLogin);

        context.put("autoUserLogin", session.getAttribute("autoUserLogin"));
        context.put("person", session.getAttribute("person"));
        context.put("partyGroup", session.getAttribute("partyGroup"));

        // some things also seem to require this, so here it is:
        request.setAttribute("userLogin", userLogin);

        // set up the user's time zone
        context.put("timeZone", UtilHttp.getTimeZone(request));

        // ========== setup values that are specific to OFBiz webapps
        VisualTheme visualTheme = UtilHttp.getVisualTheme(request);
        if (visualTheme == null) {
            String defaultVisualThemeId = EntityUtilProperties.getPropertyValue("general",
                    "VISUAL_THEME", (Delegator) request.getAttribute("delegator"));
            visualTheme = ThemeFactory.getVisualThemeFromId(defaultVisualThemeId);
        }
        context.put("visualTheme", visualTheme);
        context.put("modelTheme", visualTheme.getModelTheme());
        context.put("request", request);
        context.put("response", response);
        context.put("session", session);
        context.put("application", servletContext);
        context.put("webappName", session.getAttribute("_WEBAPP_NAME_"));

        if (servletContext != null) {
            String rootDir = (String) context.get("rootDir");
            String webSiteId = (String) context.get("webSiteId");
            String https = (String) context.get("https");
            if (UtilValidate.isEmpty(rootDir)) {
                rootDir = servletContext.getRealPath("/");
                context.put("rootDir", rootDir);
            }
            if (UtilValidate.isEmpty(webSiteId)) {
                webSiteId = WebSiteWorker.getWebSiteId(request);
                context.put("webSiteId", webSiteId);
            }
            if (UtilValidate.isEmpty(https)) {
                https = (String) servletContext.getAttribute("https");
                context.put("https", https);
            }
        }
        context.put("javaScriptEnabled", UtilHttp.isJavaScriptEnabled(request));

        // these ones are FreeMarker specific and will only work in FTL templates, mainly here for backward compatibility
        context.put("sessionAttributes", new HttpSessionHashModel(session, FreeMarkerWorker.getDefaultOfbizWrapper()));
        context.put("requestAttributes", new HttpRequestHashModel(request, FreeMarkerWorker.getDefaultOfbizWrapper()));
        TaglibFactory jspTaglibs = new TaglibFactory(servletContext);
        context.put("JspTaglibs", jspTaglibs);
        context.put("requestParameters", UtilHttp.getParameterMap(request));

        ServletContextHashModel ftlServletContext = (ServletContextHashModel) request.getAttribute("ftlServletContext");
        context.put("Application", ftlServletContext);
        context.put("Request", context.get("requestAttributes"));

        // some information from/about the ControlServlet environment
        context.put("controlPath", request.getAttribute("_CONTROL_PATH_"));
        context.put("contextRoot", request.getAttribute("_CONTEXT_ROOT_"));
        context.put("serverRoot", request.getAttribute("_SERVER_ROOT_URL_"));
        context.put("checkLoginUrl", LoginWorker.makeLoginUrl(request));
        String externalLoginKey = null;
        boolean externalLoginKeyEnabled = ExternalLoginKeysManager.isExternalLoginKeyEnabled(request);
        if (externalLoginKeyEnabled) {
            externalLoginKey = ExternalLoginKeysManager.getExternalLoginKey(request);
        }
        String externalKeyParam = externalLoginKey == null ? "" : "&amp;externalLoginKey=" + externalLoginKey;
        context.put("externalLoginKey", externalLoginKey);
        context.put("externalKeyParam", externalKeyParam);
        Object obj = request.getAttribute("eventMessageList");

        // setup message lists
        List<String> eventMessageList = (obj instanceof List) ? UtilGenerics.cast(obj) : null;
        if (eventMessageList == null) {
            eventMessageList = new LinkedList<>();
        }
        Object obj1 = request.getAttribute("errorMessageList");
        List<String> errorMessageList = (obj1 instanceof List) ? UtilGenerics.cast(obj1) : null;
        if (errorMessageList == null) {
            errorMessageList = new LinkedList<>();
        }

        if (request.getAttribute("_EVENT_MESSAGE_") != null) {
            eventMessageList.add(UtilFormatOut.replaceString((String) request.getAttribute("_EVENT_MESSAGE_"), "\n", "<br/>"));
            request.removeAttribute("_EVENT_MESSAGE_");
        }
        Object obj2 = request.getAttribute("_EVENT_MESSAGE_LIST_");
        List<String> msgList = (obj2 instanceof List) ? UtilGenerics.cast(obj2) : null;
        if (msgList != null) {
            eventMessageList.addAll(msgList);
            request.removeAttribute("_EVENT_MESSAGE_LIST_");
        }
        if (request.getAttribute("_ERROR_MESSAGE_") != null) {
            errorMessageList.add(UtilFormatOut.replaceString((String) request.getAttribute("_ERROR_MESSAGE_"), "\n", "<br/>"));
            request.removeAttribute("_ERROR_MESSAGE_");
        }
        if (session.getAttribute("_ERROR_MESSAGE_") != null) {
            errorMessageList.add(UtilFormatOut.replaceString((String) session.getAttribute("_ERROR_MESSAGE_"), "\n", "<br/>"));
            session.removeAttribute("_ERROR_MESSAGE_");
        }
        Object obj3 = request.getAttribute("_ERROR_MESSAGE_LIST_");
        msgList = (obj3 instanceof List) ? UtilGenerics.cast(obj3) : null;
        if (msgList != null) {
            errorMessageList.addAll(msgList);
            request.removeAttribute("_ERROR_MESSAGE_LIST_");
        }
        context.put("eventMessageList", eventMessageList);
        context.put("errorMessageList", errorMessageList);

        if (request.getAttribute("serviceValidationException") != null) {
            context.put("serviceValidationException", request.getAttribute("serviceValidationException"));
            request.removeAttribute("serviceValidationException");
        }

        // if there was an error message, this is an error
        context.put("isError", !errorMessageList.isEmpty() ? Boolean.TRUE : Boolean.FALSE);
        // if a parameter was passed saying this is an error, it is an error
        if ("true".equals(parameterMap.get("isError"))) {
            context.put("isError", Boolean.TRUE);
        }

        // to preserve these values, push the MapStack
        context.push();
    }

    /**
     * Gets context.
     * @return the context
     */
    public Map<String, Object> getContext() {
        return context;
    }

    /**
     * Populate context for service.
     * @param dctx the dctx
     * @param serviceContext the service context
     */
    public void populateContextForService(DispatchContext dctx, Map<String, Object> serviceContext) {
        this.populateBasicContext(serviceContext, dctx.getDelegator(), dctx.getDispatcher(),
                dctx.getSecurity(), (Locale) serviceContext.get("locale"), (GenericValue) serviceContext.get("userLogin"));
    }

    /**
     * Contains the stack of screen area ids that are generated during screen rendering
     * This allow inherent refreshment of the parent screen, when using callback feature
     * */
    public static class ScreenStack {
        private LinkedList<Map<String, Object>> visitedScreens;

        /**
         * @return the visitedScreens
         */
        public LinkedList<Map<String, Object>> getVisitedScreens() {
            return visitedScreens;
        }

        /**
         * @param visitedScreens the visitedScreens to set
         */
        public void setVisitedScreens(LinkedList<Map<String, Object>> visitedScreens) {
            this.visitedScreens = visitedScreens;
        }

        public ScreenStack() {
            visitedScreens = new LinkedList<>();
        }

        /**
         * Push a screen id upon the stack
         * @param modelScreen
         */
        public void push(ModelScreen modelScreen) {
            if (modelScreen != null) {
                Map<String, Object> screenAreaAssociation = UtilMisc.toMap(
                        "modelScreen", modelScreen,
                        "areaId", modelScreen.getSection().getName() + UUID.randomUUID().toString());
                visitedScreens.addLast(screenAreaAssociation);
            }
        }

        /**
         * Remove the last visited screen from the stack
         */
        public void drop() {
            visitedScreens.removeLast();
        }

        /**
         * Return a map with the modelScreen and the unique areaId related to the current screen from the stack
         *
         * @return ["modelScreen": {@link ModelScreen}, "areaId": {@link String}]
         */
        private Map<String, Object> resolveCurrentScreenMap() {
            return visitedScreens.getLast();
        }

        /**
         * Return the {@link ModelScreen} of the current screen from the stack
         *
         * @return {@link ModelScreen}
         */
        public ModelScreen resolveCurrentModelScreen() {
            if (visitedScreens.isEmpty()) return null;
            return (ModelScreen) resolveCurrentScreenMap().get("modelScreen");
        }

        /**
         * Return the area id reference of the current screen on the screen stack
         * @return
         */
        public String resolveCurrentScreenId() {
            if (visitedScreens.isEmpty()) return null;
            return (String) resolveCurrentScreenMap().get("areaId");
        }

        /**
         * If the given areaId have not consistency, return the current screen
         * area id on the stack
         * @return
         */
        public String resolveScreenAreaId(String areaId) {
            if (UtilValidate.isNotEmpty(areaId)) return areaId;
            return resolveCurrentScreenId();
        }
    }
}
