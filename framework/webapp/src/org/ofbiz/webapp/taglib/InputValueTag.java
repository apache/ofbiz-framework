/*
 * $Id: InputValueTag.java 5462 2005-08-05 18:35:48Z jonesde $
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
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilJ2eeCompat;
import org.ofbiz.webapp.pseudotag.InputValue;

/**
 * InputValueTag - Outputs a string for an input box from either an entity field or
 *     a request parameter. Decides which to use by checking to see if the entityattr exist and
 *     using the specified field if it does. If the Boolean object referred to by the tryentityattr
 *     attribute is false, always tries to use the request parameter and ignores the entity field.
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class InputValueTag extends TagSupport {
    
    public static final String module = InputValueTag.class.getName();

    private String field = null;
    private String param = null;
    private String entityAttr = null;
    private String tryEntityAttr = null;
    private String defaultStr = "";
    private String fullattrsStr = null;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }

    public String getEntityAttr() {
        return entityAttr;
    }

    public void setEntityAttr(String entityAttr) {
        this.entityAttr = entityAttr;
    }

    public String getTryEntityAttr() {
        return tryEntityAttr;
    }

    public void setTryEntityAttr(String tryEntityAttr) {
        this.tryEntityAttr = tryEntityAttr;
    }

    public String getDefault() {
        return defaultStr;
    }

    public void setDefault(String defaultStr) {
        this.defaultStr = defaultStr;
    }

    public String getFullattrs() {
        return fullattrsStr;
    }

    public void setFullattrs(String fullattrsStr) {
        this.fullattrsStr = fullattrsStr;
    }

    public int doStartTag() throws JspException {
        try {
            InputValue.run(field, param, entityAttr, tryEntityAttr, defaultStr, fullattrsStr, pageContext);
        } catch (IOException e) {
            if (UtilJ2eeCompat.useNestedJspException(pageContext.getServletContext())) {
                throw new JspException(e.getMessage(), e);
            } else {
                Debug.logError(e, "Server does not support nested exceptions, here is the exception", module);
                throw new JspException(e.toString());
            }
        }

        return (SKIP_BODY);
    }
}

