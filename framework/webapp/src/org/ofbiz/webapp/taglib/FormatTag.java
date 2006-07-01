/*
 * $Id: FormatTag.java 5462 2005-08-05 18:35:48Z jonesde $
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

import java.text.DateFormat;
import java.text.NumberFormat;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilJ2eeCompat;

/**
 * FormatTag - JSP Tag to format numbers and dates.
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
 */
public class FormatTag extends BodyTagSupport {
    
    public static final String module = FormatTag.class.getName();

    private String type = "N";
    private String defaultStr = "";

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public String getDefault() {
        return defaultStr;
    }

    public void setDefault(String defaultStr) {
        this.defaultStr = defaultStr;
    }

    public int doAfterBody() throws JspException {
        NumberFormat nf = null;
        DateFormat df = null;
        BodyContent body = getBodyContent();
        String value = body.getString();
        body.clearBody();

        if (value == null || value.length() == 0)
            return SKIP_BODY;

        if (type.charAt(0) == 'C' || type.charAt(0) == 'c')
            nf = NumberFormat.getCurrencyInstance();
        if (type.charAt(0) == 'N' || type.charAt(0) == 'n')
            nf = NumberFormat.getNumberInstance();
        if (type.charAt(0) == 'D' || type.charAt(0) == 'd')
            df = DateFormat.getDateInstance();

        try {
            if (nf != null) {
                // do the number formatting
                NumberFormat strFormat = NumberFormat.getInstance();

                getPreviousOut().print(nf.format(strFormat.parse(value.trim())));
            } else if (df != null) {
                // do the date formatting
                getPreviousOut().print(df.format(df.parse(value.trim())));
            } else {
                // just return the value
                getPreviousOut().print(value);
            }
        } catch (Exception e) {
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

