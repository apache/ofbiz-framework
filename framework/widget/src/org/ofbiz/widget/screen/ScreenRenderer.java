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
package org.ofbiz.widget.screen;

import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.ParserConfigurationException;

import javolution.util.FastMap;
import javolution.util.FastSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.webapp.control.LoginWorker;
import org.xml.sax.SAXException;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.jsp.TaglibFactory;
import freemarker.ext.servlet.HttpRequestHashModel;
import freemarker.ext.servlet.HttpSessionHashModel;

/**
 * Widget Library - Screen model class
 */
public class ScreenRenderer {

    public static final String module = ScreenRenderer.class.getName();
    
    protected Writer writer;
    protected MapStack context;
    protected ScreenStringRenderer screenStringRenderer;
    
    public ScreenRenderer(Writer writer, MapStack context, ScreenStringRenderer screenStringRenderer) {
        this.writer = writer;
        this.context = context;
        if (this.context == null) this.context = MapStack.create();
        this.screenStringRenderer = screenStringRenderer;
    }
    
    /**
     * Renders the named screen using the render environment configured when this ScreenRenderer was created.
     * 
     * @param combinedName A combination of the resource name/location for the screen XML file and the name of the screen within that file, separated by a puund sign ("#"). This is the same format that is used in the view-map elements on the controller.xml file.
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
     * 
     * @param resourceName The name/location of the resource to use, can use "component://[component-name]/" and "ofbiz://" and other special OFBiz style URLs
     * @param screenName The name of the screen within the XML file specified by the resourceName.
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public String render(String resourceName, String screenName) throws GeneralException, IOException, SAXException, ParserConfigurationException {
        ModelScreen modelScreen = ScreenFactory.getScreenFromLocation(resourceName, screenName);
        modelScreen.renderScreenString(writer, context, screenStringRenderer);
        return "";
    }

    public ScreenStringRenderer getScreenStringRenderer() {
        return this.screenStringRenderer;
    }

    public void populateBasicContext(Map parameters, GenericDelegator delegator, LocalDispatcher dispatcher, Security security, Locale locale, GenericValue userLogin) {
        populateBasicContext(context, this, parameters, delegator, dispatcher, security, locale, userLogin);
    }

    public static void populateBasicContext(MapStack context, ScreenRenderer screens, Map parameters, GenericDelegator delegator, LocalDispatcher dispatcher, Security security, Locale locale, GenericValue userLogin) {
        // ========== setup values that should always be in a screen context
        // include an object to more easily render screens
        context.put("screens", screens);

        // make a reference for high level variables, a global context
        context.put("globalContext", context.standAloneStack());

        // make sure the "nullField" object is in there for entity ops; note this is nullField and not null because as null causes problems in FreeMarker and such...
        context.put("nullField", GenericEntity.NULL_FIELD);

        // get all locale information
        context.put("availableLocales", UtilMisc.availableLocales());

        context.put("parameters", parameters);
        context.put("delegator", delegator);
        context.put("dispatcher", dispatcher);
        context.put("security", security);
        context.put("locale", locale);
        context.put("userLogin", userLogin);
    }
    
    /**
     * This method populates the context for this ScreenRenderer based on the HTTP Request and Respone objects and the ServletContext.
     * It leverages various conventions used in other places, namely the ControlServlet and so on, of OFBiz to get the different resources needed.
     * 
     * @param request
     * @param response
     * @param servletContext
     */
    public void populateContextForRequest(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext) {
        populateContextForRequest(context, this, request, response, servletContext);
    }

    public static void populateContextForRequest(MapStack context, ScreenRenderer screens, HttpServletRequest request, HttpServletResponse response, ServletContext servletContext) {
        HttpSession session = request.getSession();

        // attribute names to skip for session and application attributes; these are all handled as special cases, duplicating results and causing undesired messages
        Set attrNamesToSkip = FastSet.newInstance();
        attrNamesToSkip.add("delegator");
        attrNamesToSkip.add("dispatcher");
        attrNamesToSkip.add("security");
        attrNamesToSkip.add("webSiteId");

        Map parameterMap = UtilHttp.getParameterMap(request);
        // go through all request attributes and for each name that is not already in the parameters Map add the attribute value
        Enumeration requestAttrNames = request.getAttributeNames();
        while (requestAttrNames.hasMoreElements()) {
            String attrName = (String) requestAttrNames.nextElement();
            Object attrValue = request.getAttribute(attrName);

            // NOTE: this is being set to false by default now 
            //this change came after the realization that it is common to want a request attribute to override a request parameter, but I can't think of even ONE reason why a request parameter should override a request attribute...
            final boolean preserveRequestParameters = false;
            if (preserveRequestParameters) {
                Object param = parameterMap.get(attrName);
                if (param == null) {
                    parameterMap.put(attrName, attrValue);
                } else if (param instanceof String && ((String) param).length() == 0) {
                    // also put the attribute value in if the parameter is empty
                    parameterMap.put(attrName, attrValue);
                } else {
                    // do nothing, just log something
                    Debug.logInfo("Found request attribute that conflicts with parameter name, leaving request parameter in place for name: " + attrName, module);
                }
            } else {
                parameterMap.put(attrName, attrValue);
            }
        }

        // do the same for session attributes, for convenience
        Enumeration sessionAttrNames = session.getAttributeNames();
        while (sessionAttrNames.hasMoreElements()) {
            String attrName = (String) sessionAttrNames.nextElement();
            if (attrNamesToSkip.contains(attrName)) continue;
            Object attrValue = session.getAttribute(attrName);
            Object param = parameterMap.get(attrName);
            if (param == null) {
                parameterMap.put(attrName, attrValue);
            } else if (param instanceof String && ((String) param).length() == 0) {
                // also put the attribute value in if the parameter is empty
                parameterMap.put(attrName, attrValue);
            } else {
                // do nothing, just log something
                Debug.logInfo("Found session attribute that conflicts with parameter name, leaving request parameter in place for name: " + attrName, module);
            }
        }

        // do the same for servlet context (application) attribute, for convenience
        Enumeration applicationAttrNames = servletContext.getAttributeNames();
        while (applicationAttrNames.hasMoreElements()) {
            String attrName = (String) applicationAttrNames.nextElement();
            if (attrNamesToSkip.contains(attrName)) continue;
            Object param = parameterMap.get(attrName);
            Object attrValue = servletContext.getAttribute(attrName);
            if (Debug.verboseOn()) Debug.logVerbose("Getting parameter from application attrbute with name [" + attrName + "] and value [" + attrValue + "]", module);
            if (param == null) {
                parameterMap.put(attrName, attrValue);
            } else if (param instanceof String && ((String) param).length() == 0) {
                // also put the attribute value in if the parameter is empty
                parameterMap.put(attrName, attrValue);
            } else {
                // do nothing, just log something
                Debug.logInfo("Found servlet context (application) attribute that conflicts with parameter name, leaving request parameter in place for name: " + attrName, module);
            }
        }

        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        
        populateBasicContext(context, screens, parameterMap, (GenericDelegator) request.getAttribute("delegator"),
                (LocalDispatcher) request.getAttribute("dispatcher"), (Security) request.getAttribute("security"), 
                UtilHttp.getLocale(request), userLogin);

        context.put("autoUserLogin", session.getAttribute("autoUserLogin"));
        context.put("person", session.getAttribute("person"));
        context.put("partyGroup", session.getAttribute("partyGroup"));

        // some things also seem to require this, so here it is:
        request.setAttribute("userLogin", userLogin);

        // ========== setup values that are specific to OFBiz webapps

        context.put("request", request);
        context.put("response", response);
        context.put("session", session);
        context.put("application", servletContext);
        if (servletContext != null) {
            String rootDir = (String) context.get("rootDir");
            String webSiteId = (String) context.get("webSiteId");
            String https = (String) context.get("https");
            if (UtilValidate.isEmpty(rootDir)) {
                rootDir = servletContext.getRealPath("/");
                context.put("rootDir", rootDir);
            }
            if (UtilValidate.isEmpty(webSiteId)) {
                webSiteId = (String) servletContext.getAttribute("webSiteId");
                context.put("webSiteId", webSiteId);
            }
            if (UtilValidate.isEmpty(https)) {
                https = (String) servletContext.getAttribute("https");
                context.put("https", https);
            }
        }

        // these ones are FreeMarker specific and will only work in FTL templates, mainly here for backward compatibility
        BeansWrapper wrapper = BeansWrapper.getDefaultInstance();
        context.put("sessionAttributes", new HttpSessionHashModel(session, wrapper));
        context.put("requestAttributes", new HttpRequestHashModel(request, wrapper));
        TaglibFactory JspTaglibs = new TaglibFactory(servletContext);
        context.put("JspTaglibs", JspTaglibs);
        context.put("requestParameters",  UtilHttp.getParameterMap(request));

        // this is a dummy object to stand-in for the JPublish page object for backward compatibility
        context.put("page", FastMap.newInstance());

        // some information from/about the ControlServlet environment
        context.put("controlPath", request.getAttribute("_CONTROL_PATH_"));
        context.put("contextRoot", request.getAttribute("_CONTEXT_ROOT_"));
        context.put("serverRoot", request.getAttribute("_SERVER_ROOT_URL_"));
        context.put("checkLoginUrl", LoginWorker.makeLoginUrl(request, "checkLogin"));
        String externalLoginKey = LoginWorker.getExternalLoginKey(request);
        String externalKeyParam = externalLoginKey == null ? "" : "&externalLoginKey=" + externalLoginKey;
        context.put("externalLoginKey", externalLoginKey);
        context.put("externalKeyParam", externalKeyParam);

        // setup message lists
        List eventMessageList = (List) request.getAttribute("eventMessageList");
        if (eventMessageList == null) eventMessageList = new LinkedList();
        List errorMessageList = (List) request.getAttribute("errorMessageList");
        if (errorMessageList == null) errorMessageList = new LinkedList();

        if (request.getAttribute("_EVENT_MESSAGE_") != null) {
            eventMessageList.add(UtilFormatOut.replaceString((String) request.getAttribute("_EVENT_MESSAGE_"), "\n", "<br/>"));
            request.removeAttribute("_EVENT_MESSAGE_");
        }
        if (request.getAttribute("_EVENT_MESSAGE_LIST_") != null) {
            eventMessageList.addAll((List) request.getAttribute("_EVENT_MESSAGE_LIST_"));
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
        if (request.getAttribute("_ERROR_MESSAGE_LIST_") != null) {
            errorMessageList.addAll((List) request.getAttribute("_ERROR_MESSAGE_LIST_"));
            request.removeAttribute("_ERROR_MESSAGE_LIST_");
        }
        context.put("eventMessageList", eventMessageList);
        context.put("errorMessageList", errorMessageList);

        if (request.getAttribute("serviceValidationException") != null) {
            context.put("serviceValidationException", request.getAttribute("serviceValidationException"));
            request.removeAttribute("serviceValidationException");
        }

        // if there was an error message, this is an error
        if (errorMessageList.size() > 0) {
            context.put("isError", Boolean.TRUE);
        } else {
            context.put("isError", Boolean.FALSE);
        }
        // if a parameter was passed saying this is an error, it is an error
        if ("true".equals((String) parameterMap.get("isError"))) {
            context.put("isError", Boolean.TRUE);
        }
        context.put("nowTimestamp", UtilDateTime.nowTimestamp());

        // to preserve these values, push the MapStack
        context.push();
    }

    public Map getContext() {
    	return context;
    }

    public void populateContextForService(DispatchContext dctx, Map serviceContext) {
        this.populateBasicContext(serviceContext, dctx.getDelegator(), dctx.getDispatcher(), dctx.getSecurity(),
                (Locale) serviceContext.get("locale"), (GenericValue) serviceContext.get("userLogin"));
    }
}
