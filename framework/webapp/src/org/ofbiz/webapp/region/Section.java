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
package org.ofbiz.webapp.region;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilJ2eeCompat;
import org.ofbiz.webapp.control.RequestHandler;
import org.ofbiz.webapp.view.ViewHandler;
import org.ofbiz.webapp.view.ViewHandlerException;

/**
 * A section is content with a name that implements Content.render. 
 * <p>That method renders content either by including
 * it or by printing it directly, depending upon the direct
 * value passed to the Section constructor.</p>
 *
 * <p>Note that a section's content can also be a region;if so,
 * Region.render is called from Section.Render().</p>
 */
public class Section extends Content {

    protected final String name;
    protected final String info;
    protected RegionManager regionManager;
    
    public final static String module = Section.class.getName();

    public Section(String name, String info, String content, String type, RegionManager regionManager) {
        super(content, type);
        this.name = name;
        this.info = info;
        this.regionManager = regionManager;
    }

    public String getName() {
        return name;
    }

    public void render(PageContext pageContext) throws JspException {
        try {
            if (UtilJ2eeCompat.doFlushOnRender(pageContext.getServletContext())) {
                pageContext.getOut().flush();
            }
            render((HttpServletRequest) pageContext.getRequest(), (HttpServletResponse) pageContext.getResponse());
        } catch (java.io.IOException e) {
            Debug.logError(e, "Error rendering section: ", module);
            if (UtilJ2eeCompat.useNestedJspException(pageContext.getServletContext()))
                throw new JspException(e);
            else
                throw new JspException(e.toString());
        } catch (ServletException e) {
            Throwable throwable = e.getRootCause() != null ? e.getRootCause() : e;

            Debug.logError(throwable, "Error rendering section: ", module);
            if (UtilJ2eeCompat.useNestedJspException(pageContext.getServletContext()))
                throw new JspException(throwable);
            else
                throw new JspException(throwable.toString());
        }
    }

    public void render(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException, ServletException {
        ServletContext context = (ServletContext) request.getAttribute("servletContext");
        boolean verboseOn = Debug.verboseOn();

        if (verboseOn) Debug.logVerbose("Rendering " + this.toString(), module);

        // long viewStartTime = System.currentTimeMillis();
        if (content != null) {
            if ("direct".equals(type)) {
                if (UtilJ2eeCompat.useOutputStreamNotWriter(context)) {
                    response.getOutputStream().print(content);
                } else {
                    response.getWriter().print(content);
                }
            } else if ("default".equals(type) || "region".equals(type) || "resource".equals(type) || "jpublish".equals(type)) {
                // if type is resource then we won't even look up the region

                // if this is default or region, check to see if the content points to a valid region name
                Region region = null;

                if ("default".equals(type) || "region".equals(type)) {
                    region = regionManager.getRegion(content);
                }

                if ("region".equals(type) || region != null) {
                    if (region == null) {
                        throw new IllegalArgumentException("No region definition found with name: " + content);
                    }
                    // render the content as a region
                    RegionStack.push(request, region);
                    region.render(request, response);
                    RegionStack.pop(request);
                } else if ("jpublish".equals(type)) {
                    // rather then using the view handler use the wrapper directly
                    /* NOTE: In order to use the "jpublish" section type the JPublish libraries must be added as described in OPTIONAL_LIBRARIES and then uncomment this section
                    ServletContext sctx = (ServletContext) request.getAttribute("servletContext");
                    if (sctx != null) {
                        JPublishWrapper jp = this.getJPublishWrapper(sctx);
                        if (jp != null) {
                            String contentStr = "<!-- " + content + " Not Processed -->";
                            try {
                                contentStr = jp.render(content, request, response);
                            } catch (GeneralException e) {
                                Debug.logError(e, "Problems rendering view from JPublish", module);                                
                            }
                            if (UtilJ2eeCompat.useOutputStreamNotWriter(context)) {
                                response.getOutputStream().print(contentStr);
                            } else {
                                response.getWriter().print(contentStr);
                            }
                        } else {
                            throw new IllegalStateException("No jpublishWrapper found in ServletContext");
                        }
                    } else {
                        throw new IllegalStateException("No servletContext found in request");
                    }
                     */
                } else {
                    // default is the string that the ViewFactory expects for webapp resources
                    viewHandlerRender("default", request, response);
                }
            } else {
                viewHandlerRender(type, request, response);
            }
        }
        if (verboseOn) Debug.logVerbose("DONE Rendering " + this.toString(), module);
    }

    protected void viewHandlerRender(String typeToUse, HttpServletRequest request, HttpServletResponse response) throws ServletException {
        ServletContext context = (ServletContext) request.getAttribute("servletContext");
        RequestHandler requestHandler = (RequestHandler) context.getAttribute("_REQUEST_HANDLER_");

        // see if the type is defined in the controller.xml file
        try {
            if (Debug.verboseOn()) Debug.logVerbose("Rendering view [" + content + "] of type [" + typeToUse + "]", module);
            ViewHandler vh = requestHandler.getViewFactory().getViewHandler(typeToUse);
            // use the default content-type and encoding for the ViewHandler -- may want to change this.
            vh.render(name, content, info, null, null, request, response);
        } catch (ViewHandlerException e) {
            throw new ServletException(e.getNonNestedMessage(), e.getNested());
        }
    }

    /* NOTE: In order to use the "jpublish" section type the JPublish libraries must be added as described in OPTIONAL_LIBRARIES and then uncomment this section
    protected JPublishWrapper getJPublishWrapper(ServletContext ctx) {
        JPublishWrapper wrapper = (JPublishWrapper) ctx.getAttribute("jpublishWrapper");
        if (wrapper == null) {
            wrapper = new JPublishWrapper(ctx);
        }
        return wrapper;
    }
     */

    public String toString() {
        return "Section: " + name + ", info=" + info + ", content=" + content + ", type=" + type;
    }
}
