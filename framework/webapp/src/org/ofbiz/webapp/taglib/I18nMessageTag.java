/*
 * $Id: I18nMessageTag.java 5462 2005-08-05 18:35:48Z jonesde $
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
 *
 * @author     <a href="mailto:k3ysss@yahoo.com">Jian He</a>
 * @author     <a href="mailto:">Quake Wang</a>
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
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
