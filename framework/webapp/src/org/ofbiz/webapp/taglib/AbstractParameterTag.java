/*
 * $Id: AbstractParameterTag.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2002-2003 The Open For Business Project - www.ofbiz.org
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

import java.util.HashMap;
import java.util.Map;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * AbstractParameterTag - Tag which support child parameter tags.
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    1.0
 * @created    March 27, 2002
 */
public abstract class AbstractParameterTag extends TagSupport {

    private Map inParameters = null;
    private Map outParameters = null;

    public void addInParameter(Object name, Object value) {
        if (this.inParameters == null)
            this.inParameters = new HashMap();
        inParameters.put(name, value);
    }

    public Map getInParameters() {
        if (this.inParameters == null)
            return new HashMap();
        else
            return this.inParameters;
    }

    public void addOutParameter(Object name, Object alias) {
        if (this.outParameters == null)
            this.outParameters = new HashMap();
        outParameters.put(name, alias);
    }

    public Map getOutParameters() {
        if (this.outParameters == null)
            return new HashMap();
        else
            return this.outParameters;
    }

    public int doStartTag() throws JspTagException {
        inParameters = new HashMap();
        return EVAL_BODY_INCLUDE;
    }

    public abstract int doEndTag() throws JspTagException;

}
