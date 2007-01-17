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
        request.setAttribute(ContextFilter.FORWARDED_FROM_SERVLET, Boolean.TRUE);

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
