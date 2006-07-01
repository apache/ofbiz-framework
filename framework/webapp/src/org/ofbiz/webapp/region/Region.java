/*
 * $Id: Region.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001-2003 Sun Microsystems Inc., published in "Advanced Java Server Pages" by Prentice Hall PTR
 * Copyright (c) 2001-2002 The Open For Business Project - www.ofbiz.org
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
 *
 * @author     David M. Geary in the book "Advanced Java Server Pages"
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
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
