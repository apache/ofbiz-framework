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

import java.util.Map;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * ParamTag - Defines a parameter for the service tag.
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
