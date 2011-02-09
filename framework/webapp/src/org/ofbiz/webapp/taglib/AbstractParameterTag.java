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

import javolution.util.FastMap;

/**
 * AbstractParameterTag - Tag which support child parameter tags.
 */
@SuppressWarnings("serial")
public abstract class AbstractParameterTag extends TagSupport {

    private Map<String, Object> inParameters = null;
    private Map<String, String> outParameters = null;

    public void addInParameter(String name, Object value) {
        if (this.inParameters == null)
            this.inParameters = FastMap.newInstance();
        inParameters.put(name, value);
    }

    public Map<String, Object> getInParameters() {
        if (this.inParameters == null)
            return FastMap.newInstance();
        else
            return this.inParameters;
    }

    public void addOutParameter(Object name, Object alias) {
        if (this.outParameters == null)
            this.outParameters = FastMap.newInstance();
        outParameters.put((String) name, (String) alias);
    }

    public Map<String, String> getOutParameters() {
        if (this.outParameters == null)
            return FastMap.newInstance();
        else
            return this.outParameters;
    }

    @Override
    public int doStartTag() throws JspTagException {
        inParameters = FastMap.newInstance();
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public abstract int doEndTag() throws JspTagException;

}
