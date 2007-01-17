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

import java.util.ResourceBundle;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilJ2eeCompat;
import org.ofbiz.base.util.UtilProperties;

/**
 * I18nBundleTag - JSP tag that the MessageTags will use when retrieving keys
 * for this page.
 */
public class I18nBundleTag extends TagSupport {
    
    public static final String module = I18nBundleTag.class.getName();

    private String baseName = null;

    private ResourceBundle bundle = null;

    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }

    public String getBaseName() {
        return this.baseName;
    }

    public void setBundle(ResourceBundle bundle) {
        this.bundle = bundle;
    }

    public ResourceBundle getBundle() {
        return this.bundle;
    }

    public int doStartTag() throws JspException {
        try {
            this.bundle = UtilProperties.getResourceBundle(this.baseName, UtilHttp.getLocale((HttpServletRequest) pageContext.getRequest()));

            if (this.getId() != null) {
                pageContext.setAttribute(this.getId(), this.bundle);
            }
        } catch (Exception e) {
            if (UtilJ2eeCompat.useNestedJspException(pageContext.getServletContext())) {
                throw new JspException(e.getMessage(), e);
            } else {
                Debug.logError(e, "Server does not support nested exceptions, here is the exception", module);
                throw new JspException(e.toString());
            }
        }

        return EVAL_BODY_INCLUDE;
    }

    public int doEndTag() throws JspException {
        return EVAL_PAGE;
    }
}
