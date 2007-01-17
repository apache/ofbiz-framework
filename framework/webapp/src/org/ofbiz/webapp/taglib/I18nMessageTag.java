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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.TagSupport;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilJ2eeCompat;

/**
 * I18nMessageTag - JSP tag to use a resource bundle to internationalize
 * content in a web page.
 */
public class I18nMessageTag extends BodyTagSupport {
    
    public static final String module = I18nMessageTag.class.getName();

    private String key = null;

    private String value = null;

    private ResourceBundle bundle = null;

    private final List arguments = new ArrayList();

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public void setBundleId(String bundleId) {
        this.bundle = (ResourceBundle) pageContext.getAttribute(bundleId);
    }

    public void addArgument(Object argument) {
        this.arguments.add(argument);
    }

    public int doStartTag() throws JspException {
        try {
            if (this.bundle == null) {
                I18nBundleTag bundleTag = (I18nBundleTag) TagSupport.findAncestorWithClass(this, I18nBundleTag.class);

                if (bundleTag != null) {
                    this.bundle = bundleTag.getBundle();
                }
            }

            if (this.bundle != null) this.value = this.bundle.getString(this.key);

            /* this is a bad assumption, it won't necessarily be an ISO8859_1 charset, much better to just use the string as is
            this.value = new String(s.getBytes("ISO8859_1"));
             */
        } catch (Exception e) {
            if (UtilJ2eeCompat.useNestedJspException(pageContext.getServletContext())) {
                throw new JspException(e.getMessage(), e);
            } else {
                Debug.logError(e, "Server does not support nested exceptions, here is the exception", module);
                throw new JspException(e.toString());
            }
        }

        return EVAL_BODY_AGAIN;
    }

    public int doEndTag() throws JspException {
        try {
            if (this.value != null && this.arguments != null && this.arguments.size() > 0) {
                MessageFormat messageFormat = new MessageFormat(this.value);

                messageFormat.setLocale(this.bundle.getLocale());
                this.value = messageFormat.format(arguments.toArray());
            }

            if (this.value != null) this.pageContext.getOut().print(this.value);
        } catch (Exception e) {
            if (UtilJ2eeCompat.useNestedJspException(pageContext.getServletContext())) {
                throw new JspException(e.getMessage(), e);
            } else {
                Debug.logError(e, "Server does not support nested exceptions, here is the exception", module);
                throw new JspException(e.toString());
            }
        }

        return EVAL_PAGE;
    }
}
