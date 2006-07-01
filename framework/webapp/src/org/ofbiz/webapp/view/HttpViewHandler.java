/*
 * $Id: HttpViewHandler.java 5462 2005-08-05 18:35:48Z jonesde $
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
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.HttpClient;
import org.ofbiz.base.util.HttpClientException;

/**
 * ViewHandlerException - View Handler Exception
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class HttpViewHandler implements ViewHandler {
    
    public static final String module = HttpViewHandler.class.getName();

    protected ServletContext context;

    public void init(ServletContext context) throws ViewHandlerException {
        this.context = context;
    }

    public void render(String name, String page, String info, String contentType, String encoding, HttpServletRequest request, HttpServletResponse response) throws ViewHandlerException {
        // some containers call filters on EVERY request, even forwarded ones,
        // so let it know that it came from the control servlet

        if (request == null)
            throw new ViewHandlerException("Null HttpServletRequest object");
        if (page == null || page.length() == 0)
            throw new ViewHandlerException("Null or empty source");

        if (Debug.infoOn()) Debug.logInfo("Retreiving HTTP resource at: " + page, module);
        try {
            HttpClient httpClient = new HttpClient(page);
            String pageText = httpClient.get();

            // TODO: parse page and remove harmful tags like <HTML>, <HEAD>, <BASE>, etc - look into the OpenSymphony piece for an example
            response.getWriter().print(pageText);
        } catch (IOException e) {
            throw new ViewHandlerException("IO Error in view", e);
        } catch (HttpClientException e) {
            throw new ViewHandlerException(e.getNonNestedMessage(), e.getNested());
        }
    }
}
