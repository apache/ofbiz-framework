/*
 * $Id: ServiceTag.java 5462 2005-08-05 18:35:48Z jonesde $
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
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
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

