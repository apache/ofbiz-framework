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
package org.apache.ofbiz.webapp.ftl;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.collections.MapStack;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;
import org.apache.ofbiz.security.SecuredFreemarker;
import org.apache.ofbiz.webapp.control.ConfigXMLReader;
import org.apache.ofbiz.webapp.view.AbstractViewHandler;
import org.apache.ofbiz.webapp.view.ViewHandlerException;

import freemarker.ext.jsp.TaglibFactory;
import freemarker.ext.servlet.HttpRequestHashModel;
import freemarker.ext.servlet.HttpSessionHashModel;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/** FreemarkerViewHandler - Freemarker Template Engine View Handler.
 */
public class FreeMarkerViewHandler extends AbstractViewHandler {

    private Configuration config = (Configuration) FreeMarkerWorker.getDefaultOfbizConfig().clone();

    @Override
    public void init(ServletContext context) throws ViewHandlerException {
        config.setCacheStorage(new OfbizCacheStorage("unknown"));
        config.setServletContextForTemplateLoading(context, "/");
    }

    @Override
    public Map<String, Object> prepareViewContext(HttpServletRequest request, HttpServletResponse response, ConfigXMLReader.ViewMap viewMap) {
        ServletContext servletContext = request.getServletContext();
        HttpSession session = request.getSession();
        MapStack<String> root = MapStack.create();

        // add in the OFBiz objects
        root.put("delegator", request.getAttribute("delegator"));
        root.put("dispatcher", request.getAttribute("dispatcher"));
        root.put("security", request.getAttribute("security"));
        root.put("userLogin", session.getAttribute("userLogin"));

        // add the response object (for transforms) to the context as a BeanModel
        root.put("response", response);

        // add the application object (for transforms) to the context as a BeanModel
        root.put("application", servletContext);

        // add the session object (for transforms) to the context as a BeanModel
        root.put("session", session);

        // add the session
        root.put("sessionAttributes", new HttpSessionHashModel(session, FreeMarkerWorker.getDefaultOfbizWrapper()));

        // add the request object (for transforms) to the context as a BeanModel
        root.put("request", request);

        // add the request
        root.put("requestAttributes", new HttpRequestHashModel(request, FreeMarkerWorker.getDefaultOfbizWrapper()));

        // add the request parameters -- this now uses a Map from UtilHttp
        Map<String, Object> requestParameters = UtilHttp.getParameterMap(request);
        if (viewMap.isSecureContext()) {
            requestParameters = SecuredFreemarker.sanitizeParameterMap(requestParameters);
        }
        root.put("requestParameters", requestParameters);

        // add the TabLibFactory
        TaglibFactory jspTaglibs = new TaglibFactory(servletContext);
        root.put("JspTaglibs", jspTaglibs);
        return root;
    }

    @Override
    public void render(String name, String page, String info, String contentType, String encoding,
                       HttpServletRequest request, HttpServletResponse response, Map<String, Object> context) throws ViewHandlerException {
        if (UtilValidate.isEmpty(page)) {
            throw new ViewHandlerException("Invalid template source");
        }

        // process the template & flush the output
        try {
            if (page.startsWith("component://")) {
                FreeMarkerWorker.renderTemplate(page, context, response.getWriter());
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
}
