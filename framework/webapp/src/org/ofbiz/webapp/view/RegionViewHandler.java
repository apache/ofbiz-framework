/*
 * $Id: RegionViewHandler.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001-2003 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.ofbiz.webapp.view;

import java.io.IOException;
import java.net.URL;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;

import org.ofbiz.base.util.Debug;
import org.ofbiz.webapp.control.ContextFilter;
import org.ofbiz.webapp.region.Region;
import org.ofbiz.webapp.region.RegionManager;
import org.ofbiz.webapp.region.RegionStack;

/**
 * Handles Region type view rendering
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class RegionViewHandler implements ViewHandler {
    
    public static final String module = RegionViewHandler.class.getName();

    protected ServletContext context;
    protected RegionManager regionManager = null;

    public void init(ServletContext context) throws ViewHandlerException {
        this.context = context;

        URL regionFile = null;
        try {
            regionFile = context.getResource("/WEB-INF/regions.xml");
        } catch (java.net.MalformedURLException e) {
            throw new IllegalArgumentException("regions.xml file URL invalid: " + e.getMessage());
        }

        if (regionFile == null) {
            Debug.logWarning("No " + "/WEB-INF/regions.xml" + " file found in this webapp", module);
        } else {
            Debug.logVerbose("Loading regions from XML file in: " + regionFile, module);
            regionManager = new RegionManager(regionFile);
        }
    }

    public void render(String name, String page, String info, String contentType, String encoding, HttpServletRequest request, HttpServletResponse response) throws ViewHandlerException {
        // some containers call filters on EVERY request, even forwarded ones,
        // so let it know that it came from the control servlet

        if (request == null) {
            throw new ViewHandlerException("The HttpServletRequest object was null, how did that happen?");
        }
        if (page == null || page.length() == 0) {
            throw new ViewHandlerException("Page name was null or empty, but must be specified");
        }

        // tell the ContextFilter we are forwarding
        request.setAttribute(ContextFilter.FORWARDED_FROM_SERVLET, new Boolean(true));

        Region region = regionManager.getRegion(page);
        if (region == null) {
            throw new ViewHandlerException("Error: could not find region with name " + page);
        }

        try {
            // this render method does not come from a page tag so some setup needs to happen here
            RegionStack.push(request, region);

            region.render(request, response);
        } catch (IOException ie) {
            throw new ViewHandlerException("IO Error in region", ie);
        } catch (ServletException e) {
            Throwable throwable = e.getRootCause() != null ? e.getRootCause() : e;

            if (throwable instanceof JspException) {
                JspException jspe = (JspException) throwable;

                throwable = jspe.getRootCause() != null ? jspe.getRootCause() : jspe;
            }
            Debug.logError(throwable, "ServletException rendering JSP view", module);
            throw new ViewHandlerException(e.getMessage(), throwable);
        }
        RegionStack.pop(request);
    }
}
