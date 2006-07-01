/*
 * $Id: Content.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001-2003 Sun Microsystems Inc., published in "Advanced Java Server Pages" by Prentice Hall PTR
 * Copyright (c) 2002 The Open For Business Project - www.ofbiz.org
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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

/**
 * Abstract base class for Section and Region
 * <br/>Subclasses of Content must implement render(PageContext)
 *
 *@author     David M. Geary in the book "Advanced Java Server Pages"
 *@author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 *@created    February 26, 2002
 *@version    1.0
 */
public abstract class Content implements java.io.Serializable {
    protected final String content;

    /** type can be:
     * <br/>- direct (for direct inline content)
     * <br/>- region (for a nested region)
     * <br/>- default (for region if matches region name OR JSP/Servlet resource otherwise)
     * <br/>- resource (for JSP/Servlet resource)
     * <br/>- or any ViewHandler defined in the corresponding controller.xml file 
     */
    protected final String type;

    // Render this content in a JSP page
    abstract void render(PageContext pc) throws JspException;

    abstract void render(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException, ServletException;

    public Content(String content, String type) {
        this.content = content;
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public String getType() {
        return type;
    }

    public String toString() {
        return "Content: " + content + ", type: " + type;
    }
}
