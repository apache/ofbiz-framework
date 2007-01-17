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
package org.ofbiz.minilang.method;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;

/**
 * A single operation, does the specified operation on the given field
 */
public class MethodContext {
    
    public static final int EVENT = 1;
    public static final int SERVICE = 2;

    protected int methodType;

    protected Map env = new HashMap();
    protected Map parameters;
    protected Locale locale;
    protected ClassLoader loader;
    protected LocalDispatcher dispatcher;
    protected GenericDelegator delegator;
    protected Security security;
    protected GenericValue userLogin;

    protected HttpServletRequest request = null;
    protected HttpServletResponse response = null;

    protected Map results = null;
    protected DispatchContext ctx;

    public MethodContext(HttpServletRequest request, HttpServletResponse response, ClassLoader loader) {
        this.methodType = MethodContext.EVENT;
        this.parameters = UtilHttp.getParameterMap(request);
        this.loader = loader;
        this.request = request;
        this.response = response;
        this.locale = UtilHttp.getLocale(request);
        this.dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        this.delegator = (GenericDelegator) request.getAttribute("delegator");
        this.security = (Security) request.getAttribute("security");
        this.userLogin = (GenericValue) request.getSession().getAttribute("userLogin");

        if (this.loader == null) {
            try {
                this.loader = Thread.currentThread().getContextClassLoader();
            } catch (SecurityException e) {
                this.loader = this.getClass().getClassLoader();
            }
        }
    }

    public MethodContext(DispatchContext ctx, Map context, ClassLoader loader) {
        this.methodType = MethodContext.SERVICE;
        this.parameters = context;
        this.loader = loader;
        this.locale = (Locale) context.get("locale");
        this.dispatcher = ctx.getDispatcher();
        this.delegator = ctx.getDelegator();
        this.security = ctx.getSecurity();
        this.results = new HashMap();
        this.userLogin = (GenericValue) context.get("userLogin");

        if (this.loader == null) {
            try {
                this.loader = Thread.currentThread().getContextClassLoader();
            } catch (SecurityException e) {
                this.loader = this.getClass().getClassLoader();
            }
        }
    }

    /**
     * This is a very simple constructor which assumes the needed objects (dispatcher, 
     * delegator, security, request, response, etc) are in the context. 
     * Will result in calling method as a service or event, as specified.
     */    
    public MethodContext(Map context, ClassLoader loader, int methodType) {
        this.methodType = methodType;
        this.parameters = context;
        this.loader = loader;
        this.locale = (Locale) context.get("locale");
        this.dispatcher = (LocalDispatcher) context.get("dispatcher");
        this.delegator = (GenericDelegator) context.get("delegator");
        this.security = (Security) context.get("security");
        this.userLogin = (GenericValue) context.get("userLogin");

        if (methodType == MethodContext.EVENT) {
            this.request = (HttpServletRequest) context.get("request");
            this.response = (HttpServletResponse) context.get("response");
            if (this.locale == null) this.locale = UtilHttp.getLocale(request);
            
            //make sure the delegator and other objects are in place, getting from 
            // request if necessary; assumes this came through the ControlServlet
            // or something similar
            if (this.request != null) {
                if (this.dispatcher == null) this.dispatcher = (LocalDispatcher) this.request.getAttribute("dispatcher");
                if (this.delegator == null) this.delegator = (GenericDelegator) this.request.getAttribute("delegator");
                if (this.security == null) this.security = (Security) this.request.getAttribute("security");
                if (this.userLogin == null) this.userLogin = (GenericValue) this.request.getSession().getAttribute("userLogin");
            }
        } else if (methodType == MethodContext.SERVICE) {
            this.results = new HashMap();
        }
        
        if (this.loader == null) {
            try {
                this.loader = Thread.currentThread().getContextClassLoader();
            } catch (SecurityException e) {
                this.loader = this.getClass().getClassLoader();
            }
        }
    }
    
    public void setErrorReturn(String errMsg, SimpleMethod simpleMethod) {
        if (getMethodType() == MethodContext.EVENT) {
            putEnv(simpleMethod.getEventErrorMessageName(), errMsg);
            putEnv(simpleMethod.getEventResponseCodeName(), simpleMethod.getDefaultErrorCode());
        } else if (getMethodType() == MethodContext.SERVICE) {
            putEnv(simpleMethod.getServiceErrorMessageName(), errMsg);
            putEnv(simpleMethod.getServiceResponseMessageName(), simpleMethod.getDefaultErrorCode());
        }
    }

    public int getMethodType() {
        return this.methodType;
    }

    public Map getEnvMap() {
        return this.env;
    }
    
    /** Gets the named value from the environment. Supports the "." (dot) syntax to access Map members and the
     * "[]" (bracket) syntax to access List entries. This value is expanded, supporting the insertion of other 
     * environment values using the "${}" notation.
     * 
     * @param key The name of the environment value to get. Can contain "." and "[]" syntax elements as described above.
     * @return The environment value if found, otherwise null. 
     */
    public Object getEnv(String key) {
        String ekey = this.expandString(key);
        FlexibleMapAccessor fma = new FlexibleMapAccessor(ekey);
        return this.getEnv(fma);
    }
    public Object getEnv(FlexibleMapAccessor fma) {
        return fma.get(this.env);
    }

    /** Puts the named value in the environment. Supports the "." (dot) syntax to access Map members and the
     * "[]" (bracket) syntax to access List entries. 
     * If the brackets for a list are empty the value will be appended to end of the list,
     * otherwise the value will be set in the position of the number in the brackets.
     * If a "+" (plus sign) is included inside the square brackets before the index 
     * number the value will inserted/added at that index instead of set at that index.
     * This value is expanded, supporting the insertion of other 
     * environment values using the "${}" notation.
     * 
     * @param key The name of the environment value to get. Can contain "." syntax elements as described above.
     * @param value The value to set in the named environment location.
     */
    public void putEnv(String key, Object value) {
        String ekey = this.expandString(key);
        FlexibleMapAccessor fma = new FlexibleMapAccessor(ekey);
        this.putEnv(fma, value);
    }
    public void putEnv(FlexibleMapAccessor fma, Object value) {
        fma.put(this.env, value);
    }

    /** Calls putEnv for each entry in the Map, thus allowing for the additional flexibility in naming 
     * supported in that method. 
     */
    public void putAllEnv(Map values) {
        Iterator viter = values.entrySet().iterator();
        while (viter.hasNext()) {
            Map.Entry entry = (Map.Entry) viter.next();
            this.putEnv((String) entry.getKey(), entry.getValue());
        }
    }

    /** Removes the named value from the environment. Supports the "." (dot) syntax to access Map members and the
     * "[]" (bracket) syntax to access List entries. This value is expanded, supporting the insertion of other 
     * environment values using the "${}" notation.
     * 
     * @param key The name of the environment value to get. Can contain "." syntax elements as described above.
     */
    public Object removeEnv(String key) {
        String ekey = this.expandString(key);
        FlexibleMapAccessor fma = new FlexibleMapAccessor(ekey);
        return this.removeEnv(fma);
    }
    public Object removeEnv(FlexibleMapAccessor fma) {
        return fma.remove(this.env);
    }

    public Iterator getEnvEntryIterator() {
        return this.env.entrySet().iterator();
    }

    public Object getParameter(String key) {
        return this.parameters.get(key);
    }

    public void putParameter(String key, Object value) {
        this.parameters.put(key, value);
    }

    public Map getParameters() {
        return this.parameters;
    }

    public ClassLoader getLoader() {
        return this.loader;
    }
    
    public Locale getLocale() {
        return this.locale;
    }

    public LocalDispatcher getDispatcher() {
        return this.dispatcher;
    }

    public GenericDelegator getDelegator() {
        return this.delegator;
    }

    public Security getSecurity() {
        return this.security;
    }

    public HttpServletRequest getRequest() {
        return this.request;
    }

    public HttpServletResponse getResponse() {
        return this.response;
    }

    public GenericValue getUserLogin() {
        return this.userLogin;
    }

    public void setUserLogin(GenericValue userLogin, String userLoginEnvName) {
        this.userLogin = userLogin;
        this.putEnv(userLoginEnvName, userLogin);
    }

    public Object getResult(String key) {
        return this.results.get(key);
    }

    public void putResult(String key, Object value) {
        this.results.put(key, value);
    }

    public Map getResults() {
        return this.results;
    }
    
    /** Expands environment variables delimited with ${} */
    public String expandString(String original) {
        return FlexibleStringExpander.expandString(original, this.env);
    }

    public String expandString(FlexibleStringExpander originalExdr) {
        return originalExdr.expandString(this.env);
    }
}
