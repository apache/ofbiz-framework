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
package org.ofbiz.webapp.ftl;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.webapp.view.AbstractViewHandler;
import org.ofbiz.webapp.view.ViewHandlerException;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.jsp.TaglibFactory;
import freemarker.ext.servlet.HttpRequestHashModel;
import freemarker.ext.servlet.HttpSessionHashModel;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/** FreemarkerViewHandler - Freemarker Template Engine View Handler.
 */
public class FreeMarkerViewHandler extends AbstractViewHandler {

    public static final String module = FreeMarkerViewHandler.class.getName();
    protected ServletContext servletContext = null;
    protected Configuration config = (Configuration) FreeMarkerWorker.getDefaultOfbizConfig().clone();

    public void init(ServletContext context) throws ViewHandlerException {
        this.servletContext = context;
        config.setCacheStorage(new OfbizCacheStorage("unknown"));
        try {
            config.setDirectoryForTemplateLoading(new File(servletContext.getRealPath("/")));
        } catch (IOException e) {
            throw new ViewHandlerException("Could not create file for webapp root path", e);
        }
    }

    public void render(String name, String page, String info, String contentType, String encoding,
            HttpServletRequest request, HttpServletResponse response) throws ViewHandlerException {
        if (UtilValidate.isEmpty(page))
            throw new ViewHandlerException("Invalid template source");

        // make the root context (data model) for freemarker
        MapStack<String> context = MapStack.create();
        prepOfbizRoot(context, request, response);

        // process the template & flush the output
        try {
            if (page.startsWith("component://")) {
                FreeMarkerWorker.renderTemplateAtLocation(page, context, response.getWriter());
            } else {
                // backwards compatibility
                Template template = config.getTemplate(page);
                FreeMarkerWorker.renderTemplate(template, context, response.getWriter());
            }
            response.flushBuffer();
        } catch (TemplateException te) {
            throw new ViewHandlerException("Problems processing Freemarker template", te);
        } catch (IOException ie) {
            throw new ViewHandlerException("Problems writing to output stream", ie);
        }
    }

    public static void prepOfbizRoot(Map<String, Object> root, HttpServletRequest request, HttpServletResponse response) {
        ServletContext servletContext = (ServletContext) request.getAttribute("servletContext");
        HttpSession session = request.getSession();

        BeansWrapper wrapper = BeansWrapper.getDefaultInstance();

        // add in the OFBiz objects
        root.put("delegator", request.getAttribute("delegator"));
        root.put("dispatcher", request.getAttribute("dispatcher"));
        root.put("authz", request.getAttribute("authz"));
        root.put("security", request.getAttribute("security"));
        root.put("userLogin", session.getAttribute("userLogin"));

        // add the response object (for transforms) to the context as a BeanModel
        root.put("response", response);

        // add the application object (for transforms) to the context as a BeanModel
        root.put("application", servletContext);

        // add the servlet context -- this has been deprecated, and now requires servlet, do we really need it?
        //root.put("applicationAttributes", new ServletContextHashModel(servletContext, BeansWrapper.getDefaultInstance()));

        // add the session object (for transforms) to the context as a BeanModel
        root.put("session", session);

        // add the session
        root.put("sessionAttributes", new HttpSessionHashModel(session, wrapper));

        // add the request object (for transforms) to the context as a BeanModel
        root.put("request", request);

        // add the request
        root.put("requestAttributes", new HttpRequestHashModel(request, wrapper));

        // add the request parameters -- this now uses a Map from UtilHttp
        Map<String, Object> requestParameters = UtilHttp.getParameterMap(request);
        root.put("requestParameters", requestParameters);

        // add the TabLibFactory
        TaglibFactory JspTaglibs = new TaglibFactory(servletContext);
        root.put("JspTaglibs", JspTaglibs);

    }
}
