/*
 * $Id: ParamTag.java 5462 2005-08-05 18:35:48Z jonesde $
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

import java.util.Map;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * ParamTag - Defines a parameter for the service tag.
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
 */
public class ParamTag extends TagSupport {

    protected String name = null;
    protected String mode = null;
    protected String map = null;
    protected String alias = null;
    protected String attribute = null;
    protected Object paramValue = null;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getMode() {
        return this.mode;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setValue(Object paramValue) {
        this.paramValue = paramValue;
    }

    public Object getValue() {
        return paramValue;
    }

    public void setMap(String map) {
        this.map = map;
    }

    public String getMap() {
        return map;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getAlias() {
        return this.alias;
    }

    public int doStartTag() throws JspTagException {
        AbstractParameterTag sTag = (AbstractParameterTag) findAncestorWithClass(this, AbstractParameterTag.class);

        if (sTag == null)
            throw new JspTagException("ParamTag not inside a ServiceTag.");

        if (mode != null && !mode.equals("IN") && !mode.equals("OUT") && !mode.equals("INOUT"))
            throw new JspTagException("Invalid mode attribute. Must be IN/OUT/INOUT.");

        if (mode != null && (mode.equals("OUT") || mode.equals("INOUT")))
            sTag.addOutParameter(name, (alias != null ? alias : name));

        if (mode == null || mode.equals("IN") || mode.equals("INOUT")) {
            Object value = null;

            if (attribute != null) {
                if (map == null) {
                    value = pageContext.findAttribute(attribute);
                    if (value == null)
                        value = pageContext.getRequest().getParameter(attribute);
                } else {
                    try {
                        Map mapObject = (Map) pageContext.findAttribute(map);

                        value = mapObject.get(attribute);
                    } catch (Exception e) {
                        throw new JspTagException("Problem processing map (" + map + ") for attributes.");
                    }
                }
            }
            if (value == null && paramValue != null) {
                value = paramValue;
            }

            sTag.addInParameter(name, value);
        }

        return SKIP_BODY;
    }

    public int doEndTag() {
        return EVAL_PAGE;
    }
}
