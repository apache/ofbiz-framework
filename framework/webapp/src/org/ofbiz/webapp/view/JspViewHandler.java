/*
 * $Id: JspViewHandler.java 5462 2005-08-05 18:35:48Z jonesde $
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
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;

import org.ofbiz.base.util.Debug;
import org.ofbiz.webapp.control.ContextFilter;

/**
 * ViewHandlerException - View Handler Exception
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
 */
public class JspViewHandler implements ViewHandler {
    
    public static final String module = JspViewHandler.class.getName();

    protected ServletContext context;

    public void init(ServletContext context) throws ViewHandlerException {
        this.context = context;
    }

    public void render(String name, String page, String contentType, String encoding, String info, HttpServletRequest request, HttpServletResponse response) throws ViewHandlerException {
        // some containers call filters on EVERY request, even forwarded ones,
        // so let it know that it came from the control servlet

        if (request == null) {
            throw new ViewHandlerException("Null HttpServletRequest object");
        }
        if (page == null || page.length() == 0) {
            throw new ViewHandlerException("Null or empty source");
        }

        //Debug.log("Requested Page : " + page, module);
        //Debug.log("Physical Path  : " + context.getRealPath(page));

        // tell the ContextFilter we are forwarding
        request.setAttribute(ContextFilter.FORWARDED_FROM_SERVLET, new Boolean(true));
        RequestDispatcher rd = request.getRequestDispatcher(page);
        
        if (rd == null) {
            Debug.logInfo("HttpServletRequest.getRequestDispatcher() failed; trying ServletContext", module);
            rd = context.getRequestDispatcher(page);
            if (rd == null) {
                Debug.logInfo("ServletContext.getRequestDispatcher() failed; trying ServletContext.getNamedDispatcher(\"jsp\")", module);
                rd = context.getNamedDispatcher("jsp");
                if (rd == null) {
                    throw new ViewHandlerException("Source returned a null dispatcher (" + page + ")");
                }
            }
        }

        try {
            rd.include(request, response);
        } catch (IOException ie) {
            throw new ViewHandlerException("IO Error in view", ie);
        } catch (ServletException e) {
            Throwable throwable = e.getRootCause() != null ? e.getRootCause() : e;

            if (throwable instanceof JspException) {
                JspException jspe = (JspException) throwable;

                throwable = jspe.getRootCause() != null ? jspe.getRootCause() : jspe;
            }
            Debug.logError(throwable, "ServletException rendering JSP view", module);
            throw new ViewHandlerException(e.getMessage(), throwable);
        }
    }
}
