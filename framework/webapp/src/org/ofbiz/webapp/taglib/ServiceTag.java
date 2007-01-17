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

import java.util.Iterator;
import java.util.Map;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;

/**
 * ServiceTag - Service invocation tag.
 */
public class ServiceTag extends AbstractParameterTag {

    protected String serviceName;
    protected String resultScope = "page";
    protected String mode = "sync";

    public static final String module = ServiceTag.class.getName();

    public void setName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getName() {
        return serviceName;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getMode() {
        return mode;
    }

    public void setResultTo(String resultScope) {
        this.resultScope = resultScope;
    }

    public String getResultTo() {
        return resultScope;
    }

    public int doEndTag() throws JspTagException {
        LocalDispatcher dispatcher = (LocalDispatcher) pageContext.getRequest().getAttribute("dispatcher");

        if (dispatcher == null)
            throw new JspTagException("Cannot get dispatcher from the request object.");

        GenericValue userLogin = (GenericValue) pageContext.getSession().getAttribute("userLogin");

        int scope = PageContext.PAGE_SCOPE;
        char scopeChar = resultScope.toUpperCase().charAt(0);

        switch (scopeChar) {
        case 'A':
            scope = PageContext.APPLICATION_SCOPE;
            break;

        case 'S':
            scope = PageContext.SESSION_SCOPE;
            break;

        case 'R':
            scope = PageContext.REQUEST_SCOPE;
            break;

        case 'P':
            scope = PageContext.PAGE_SCOPE;
            break;

        default:
            throw new JspTagException("Invaild result scope specified. (page, request, session, application)");
        }

        Map context = getInParameters();
        Map result = null;

        if (userLogin != null)
            context.put("userLogin", userLogin);
        try {
            if (mode.equalsIgnoreCase("async"))
                dispatcher.runAsync(serviceName, context);
            else
                result = dispatcher.runSync(serviceName, context);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            throw new JspTagException("Problems invoking the requested service: " + e.getMessage());
        }

        Map aliases = getOutParameters();

        if (result != null) {
            // expand the result
            Iterator i = result.entrySet().iterator();

            while (i.hasNext()) {
                Map.Entry entry = (Map.Entry) i.next();
                Object key = entry.getKey();
                Object value = entry.getValue();
                String ctxName = (String) (aliases.containsKey(key) ? aliases.get(key) : key);

                if (value == null) value = new String();
                pageContext.setAttribute(ctxName, value, scope);
            }
        }

        return EVAL_PAGE;
    }

}

