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
 */
public class ContentUrlTag extends BodyTagSupport {

    public static final String module = UrlTag.class.getName();

    public static void appendContentPrefix(HttpServletRequest request, StringBuffer urlBuffer) {
        if (request == null) {
            Debug.logWarning("WARNING: request was null in appendContentPrefix; this probably means this was used where it shouldn't be, like using ofbizContentUrl in a screen rendered through a service; using best-bet behavior: standard prefix from url.properties (no WebSite or security setting known)", module);
            String prefix = UtilProperties.getPropertyValue("url", "content.url.prefix.standard");
            if (prefix != null) {
                urlBuffer.append(prefix.trim());
            }
            return;
        }
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
