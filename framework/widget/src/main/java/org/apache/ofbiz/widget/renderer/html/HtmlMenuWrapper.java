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
package org.apache.ofbiz.widget.renderer.html;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.widget.model.MenuFactory;
import org.apache.ofbiz.widget.model.ModelMenu;
import org.apache.ofbiz.widget.renderer.MenuStringRenderer;
import org.xml.sax.SAXException;


/**
 * Widget Library - HTML Menu Wrapper class - makes it easy to do the setup and render of a menu
 */
public class HtmlMenuWrapper {

    public static final String module = HtmlMenuWrapper.class.getName();

    protected String resourceName;
    protected String menuName;
    protected HttpServletRequest request;
    protected HttpServletResponse response;
    protected ModelMenu modelMenu;
    protected MenuStringRenderer renderer;
    protected Map<String, Object> context;

    protected HtmlMenuWrapper() {}

    public HtmlMenuWrapper(String resourceName, String menuName, HttpServletRequest request, HttpServletResponse response)
            throws IOException, SAXException, ParserConfigurationException {
        init(resourceName, menuName, request, response);
    }

    public void init(String resourceName, String menuName, HttpServletRequest request, HttpServletResponse response)
            throws IOException, SAXException, ParserConfigurationException {
        this.resourceName = resourceName;
        this.menuName = menuName;
        this.request = request;
        this.response = response;

        this.modelMenu = MenuFactory.getMenuFromWebappContext(resourceName, menuName, request);

        this.renderer = getMenuRenderer();

        this.context = new HashMap<>();
        Map<String, Object> parameterMap = UtilHttp.getParameterMap(request);
        context.put("parameters", parameterMap);

        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue)session.getAttribute("userLogin");
        context.put("userLogin", userLogin);

        //make sure the locale is in the context
        context.put("locale", UtilHttp.getLocale(request));

        // if there was an error message, this is an error
        if (UtilValidate.isNotEmpty(request.getAttribute("_ERROR_MESSAGE_"))) {
            context.put("isError", Boolean.TRUE);
        } else {
            context.put("isError", Boolean.FALSE);
        }

        // if a parameter was passed saying this is an error, it is an error
        if ("true".equals(parameterMap.get("isError"))) {
            context.put("isError", Boolean.TRUE);
        }
    }

    public MenuStringRenderer getMenuRenderer() {
        return new HtmlMenuRenderer(request, response);
    }

    public String renderMenuString() throws IOException {
        HttpServletRequest req = ((HtmlMenuRenderer)renderer).request;
        ServletContext ctx = (ServletContext) req.getAttribute("servletContext");
        if (ctx == null) {
            if (Debug.infoOn()) {
                Debug.logInfo("in renderMenuString, ctx is null(0)" , "");
            }
        }

        Writer writer = new StringWriter();
        modelMenu.renderMenuString(writer, context, renderer);

        HttpServletRequest req2 = ((HtmlMenuRenderer)renderer).request;
        ServletContext ctx2 = (ServletContext) req2.getAttribute("servletContext");
        if (ctx2 == null) {
            if (Debug.infoOn()) {
                Debug.logInfo("in renderMenuString, ctx is null(2)" , "");
            }
        }

        return writer.toString();
    }

    /**
     * Tells the menu library whether this is a response to an error or not.
     * Defaults on initialization according to the presense of an errorMessage
     * in the request or if an isError parameter was passed to the page with
     * the value "true". If true then the prefilled values will come from the
     * parameters Map instead of the value Map.
     */
    public void setIsError(boolean isError) {
        this.context.put("isError", Boolean.valueOf(isError));
    }

    public boolean getIsError() {
        Boolean isErrorBoolean = (Boolean) this.context.get("isError");
        if (isErrorBoolean == null) {
            return false;
        } else {
            return isErrorBoolean.booleanValue();
        }
    }

    public void setMenuOverrideName(String menuName) {
        this.context.put("menuName", menuName);
    }

    public void putInContext(String name, Object value) {
        this.context.put(name, value);
    }

    public void putInContext(String menuItemName, String valueName,  Object value) {
        Map<String, Object> valueMap = UtilGenerics.toMap(context.get(menuItemName));
        if (valueMap == null) {
            valueMap = new HashMap<>();
            context.put(menuItemName, valueMap);
        }
        valueMap.put(valueName, value);
    }

    public Object getFromContext(String name) {
        return this.context.get(name);
    }

    public Object getFromContext(String menuItemName, String valueName) {
        Map<String, Object> valueMap = UtilGenerics.toMap(context.get(menuItemName));
        if (valueMap == null) {
            valueMap = new HashMap<>();
            context.put(menuItemName, valueMap);
        }
        return valueMap.get(valueName);
    }

    public ModelMenu getModelMenu() {
        return modelMenu;
    }

    public MenuStringRenderer getRenderer() {
        return renderer;
    }

    public void setRenderer(MenuStringRenderer renderer) {
        this.renderer = renderer;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
        ((HtmlMenuRenderer)renderer).setRequest(request);
    }

    public void setResponse(HttpServletResponse response) {
        this.response = response;
        ((HtmlMenuRenderer)renderer).setResponse(response);
    }

    public HttpServletRequest getRequest() {
        return ((HtmlMenuRenderer)renderer).request;
    }

    public HttpServletResponse getResponse() {
        return ((HtmlMenuRenderer)renderer).response;
    }

    public static HtmlMenuWrapper getMenuWrapper(HttpServletRequest request, HttpServletResponse response, HttpSession session, String menuDefFile, String menuName, String menuWrapperClassName) {

        HtmlMenuWrapper menuWrapper = null;

        String menuSig = menuDefFile + "__" + menuName;
        if (session != null) {
             menuWrapper = (HtmlMenuWrapper)session.getAttribute(menuSig);
        }

        if (menuWrapper == null) {
            try {
                Class<?> cls = Class.forName("org.apache.ofbiz.widget.html." + menuWrapperClassName);
                menuWrapper = (HtmlMenuWrapper)cls.newInstance();
                menuWrapper.init(menuDefFile, menuName, request, response);
            } catch (InstantiationException | IllegalAccessException | IOException | SAXException | ParserConfigurationException e) {
                throw new RuntimeException(e.getMessage());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Class not found:" + e.getMessage());
            }
        } else {
            menuWrapper.setRequest(request);
            menuWrapper.setResponse(response);
            Map<String, Object> parameterMap = UtilHttp.getParameterMap(request);
            menuWrapper.setParameters(parameterMap);

            GenericValue userLogin = (GenericValue)session.getAttribute("userLogin");
            menuWrapper.putInContext("userLogin", userLogin);

        }

        if (session != null) {
            session.setAttribute(menuSig, menuWrapper);
        }
        return menuWrapper;
    }

    public void setParameters(Map<String, Object> paramMap) {
        context.put("parameters", paramMap);
    }

}
