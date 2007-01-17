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

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilValidate;

/**
 * ObjectTag - Loads an object from the PageContext.
 */
public class ObjectTag extends TagSupport {
    
    public static final String module = ObjectTag.class.getName();

    protected Object element = null;
    protected String name = null;
    protected String property = null;
    protected Class type = null;

    public void setName(String name) {
        this.name = name;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public void setType(String type) throws ClassNotFoundException {
        this.type = ObjectType.loadClass(type);
    }

    public String getName() {
        return name;
    }

    public String getProperty() {
        return property;
    }

    public Object getObject() {
        return element;
    }

    public String getType() {
        return type.getName();
    }

    public int doStartTag() throws JspTagException {
        String realAttrName = property;

        if (UtilValidate.isEmpty(realAttrName)) {
            realAttrName = name;
        }
        element = pageContext.findAttribute(realAttrName);
        if (element != null) {
            pageContext.setAttribute(name, element);
        } else {
            Debug.logWarning("Did not find element in property. (" + property + ")", module);
        }
        return EVAL_BODY_INCLUDE;
    }

    public int doEndTag() {
        return EVAL_PAGE;
    }
}

