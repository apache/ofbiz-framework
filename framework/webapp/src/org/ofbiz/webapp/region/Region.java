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

import java.util.HashMap;
import java.util.Map;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilJ2eeCompat;

/**
 * A region is content that contains a set of sections that can render in a PageContext
 * <br/>Implements abstract render(PageContext) from Content
 */
public class Region extends Content {
    
    public static final String module = Region.class.getName();

    private Map sections = new HashMap();
    protected String id;

    public Region(String id, String content) {
        this(id, content, null); // content is the name of a template
    }

    public Region(String id, String content, Map sections) {
        super(content, "region");
        this.id = id;
        if (sections != null) {
            this.sections.putAll(sections);
        }
    }

    public String getId() {
        return this.id;
    }

    public void put(Section section) {
        sections.put(section.getName(), section);
    }

    public void putAll(Map newSections) {
        sections.putAll(newSections);
    }

    public Section get(String name) {
        return (Section) sections.get(name);
    }

    public Map getSections() {
        return sections;
    }

    public void render(PageContext pageContext) throws JspException {
        if (Debug.verboseOn()) Debug.logVerbose("Rendering " + this.toString(), module);

        try {
            this.render((HttpServletRequest) pageContext.getRequest(), (HttpServletResponse) pageContext.getResponse());
        } catch (java.io.IOException e) {
            Debug.logError(e, "Error rendering region: ", module);
            if (UtilJ2eeCompat.useNestedJspException(pageContext.getServletContext()))
                throw new JspException(e);
            else
                throw new JspException(e.toString());
        } catch (ServletException e) {
            Throwable throwable = e.getRootCause() != null ? e.getRootCause() : e;

            Debug.logError(throwable, "Error rendering region: ", module);
            if (UtilJ2eeCompat.useNestedJspException(pageContext.getServletContext())) {
                throw new JspException(throwable);
            } else {
                throw new JspException(throwable.toString());
            }
        }
    }

    public void render(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException, ServletException {
        if (Debug.verboseOn()) Debug.logVerbose("Rendering " + this.toString(), module);

        ServletContext application = (ServletContext) request.getAttribute("servletContext");
        RequestDispatcher rd = application.getRequestDispatcher(content);
        Debug.logInfo("real path for [" + content + "] is: " + application.getRealPath(content), module);
        //RequestDispatcher rd = request.getRequestDispatcher(content);
        if (rd == null) {
            throw new IllegalArgumentException("HttpServletRequest returned a null RequestDispatcher for: [" + content + "]");
        } else {
            // Exception newE = new Exception("Stack Trace");
            // Debug.logInfo(newE, "Got RD for: [" + content + "]: " + rd.toString(), module);
        }
        rd.include(request, response);
    }

    public String toString() {
        String s = "Region: " + content + ", type=" + type;

        /*
         int indent = 4;
         Iterator iter = sections.values().iterator();

         while (iter.hasNext()) {
         Section section = (Section) iter.next();
         for (int i = 0; i < indent; ++i) {
         s += "&nbsp;";
         }
         s += section.toString() + "<br/>";
         }
         */
        return s;
    }
}
