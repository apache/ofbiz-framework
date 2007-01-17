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

