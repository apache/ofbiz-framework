/*
 * $Id: ContentUrlTag.java 5462 2005-08-05 18:35:48Z jonesde $
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
package org.ofbiz.webapp.taglib;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilJ2eeCompat;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.webapp.website.WebSiteWorker;
import org.ofbiz.entity.GenericValue;

/**
 * ContentUrlTag - Creates a URL string prepending the content prefix from url.properties
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
 */
public class ContentUrlTag extends BodyTagSupport {

    public static final String module = UrlTag.class.getName();

    public static void appendContentPrefix(HttpServletRequest request, StringBuffer urlBuffer) {
        GenericValue webSite = WebSiteWorker.getWebSite(request);
        if (request.isSecure()) {
            if (webSite != null && UtilValidate.isNotEmpty(webSite.getString("secureContentPrefix"))) {
                urlBuffer.append(webSite.getString("secureContentPrefix").trim());
            } else {
                String prefix = UtilProperties.getPropertyValue("url", "content.url.prefix.secure");
                if (prefix != null) {
                    urlBuffer.append(prefix.trim());
                }
            }
        } else {
            if (webSite != null && UtilValidate.isNotEmpty(webSite.getString("standardContentPrefix"))) {
                urlBuffer.append(webSite.getString("standardContentPrefix").trim());
            } else {
                String prefix = UtilProperties.getPropertyValue("url", "content.url.prefix.standard");
                if (prefix != null) {
                    urlBuffer.append(prefix.trim());
                }
            }
        }
    }

    public static String getContentPrefix(HttpServletRequest request) {
        StringBuffer buf = new StringBuffer();
        ContentUrlTag.appendContentPrefix(request, buf);
        return buf.toString();
    }

    public int doEndTag() throws JspException {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

        BodyContent body = getBodyContent();
        String bodyString = body.getString();

        StringBuffer newURL = new StringBuffer();

        appendContentPrefix(request, newURL);
        newURL.append(bodyString);
        body.clearBody();

        try {
            getPreviousOut().print(newURL.toString());
        } catch (IOException e) {
            if (UtilJ2eeCompat.useNestedJspException(pageContext.getServletContext())) {
                throw new JspException(e.getMessage(), e);
            } else {
                Debug.logError(e, "Server does not support nested exceptions, here is the exception", module);
                throw new JspException(e.toString());
            }
        }
        return SKIP_BODY;
    }
}
