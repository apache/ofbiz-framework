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

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastMap;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.security.Security;
import org.ofbiz.security.authz.Authorization;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;

/**
 * A single operation, does the specified operation on the given field
 */
public class MethodContext implements Iterable<Map.Entry<String, Object>> {

    public static final int EVENT = 1;
    public static final int SERVICE = 2;

    protected int methodType;

    protected Map<String, Object> env = FastMap.newInstance();
    protected Map<String, Object> parameters;
    protected Locale locale;
    protected TimeZone timeZone;
    protected ClassLoader loader;
    protected LocalDispatcher dispatcher;
    protected Delegator delegator;
    protected Authorization authz;
    protected Security security;
    protected GenericValue userLogin;

    protected HttpServletRequest request = null;
    protected HttpServletResponse response = null;

    protected Map<String, Object> results = null;
    protected DispatchContext ctx;

    public MethodContext(HttpServletRequest request, HttpServletResponse response, ClassLoader loader) {
        this.methodType = MethodContext.EVENT;
        this.parameters = UtilHttp.getParameterMap(request);
        this.loader = loader;
        this.request = request;
        this.response = response;
        this.locale = UtilHttp.getLocale(request);
        this.timeZone = UtilHttp.getTimeZone(request);
        this.dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        this.delegator = (Delegator) request.getAttribute("delegator");
        this.authz = (Authorization) request.getAttribute("authz");
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

    public MethodContext(DispatchContext ctx, Map<String, ? extends Object> context, ClassLoader loader) {
        this.methodType = MethodContext.SERVICE;
        this.parameters = UtilMisc.makeMapWritable(context);
        this.loader = loader;
        this.locale = (Locale) context.get("locale");
        this.timeZone = (TimeZone) context.get("timeZone");
        this.dispatcher = ctx.getDispatcher();
        this.delegator = ctx.getDelegator();
        this.authz = ctx.getAuthorization();
        this.security = ctx.getSecurity();
        this.results = FastMap.newInstance();
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
     * delegator, authz, security, request, response, etc) are in the context.
     * Will result in calling method as a service or event, as specified.
     */
    public MethodContext(Map<String, ? extends Object> context, ClassLoader loader, int methodType) {
        this.methodType = methodType;
        this.parameters = UtilMisc.makeMapWritable(context);
        this.loader = loader;
        this.locale = (Locale) context.get("locale");
        this.timeZone = (TimeZone) context.get("timeZone");
        this.dispatcher = (LocalDispatcher) context.get("dispatcher");
        this.delegator = (Delegator) context.get("delegator");
        this.authz = (Authorization) context.get("authz");
        this.security = (Security) context.get("security");
        this.userLogin = (GenericValue) context.get("userLogin");

        if (methodType == MethodContext.EVENT) {
            this.request = (HttpServletRequest) context.get("request");
            this.response = (HttpServletResponse) context.get("response");
            if (this.locale == null) this.locale = UtilHttp.getLocale(request);
            if (this.timeZone == null) this.timeZone = UtilHttp.getTimeZone(request);

            //make sure the delegator and other objects are in place, getting from
            // request if necessary; assumes this came through the ControlServlet
            // or something similar
            if (this.request != null) {
                if (this.dispatcher == null) this.dispatcher = (LocalDispatcher) this.request.getAttribute("dispatcher");
                if (this.delegator == null) this.delegator = (Delegator) this.request.getAttribute("delegator");
                if (this.authz == null) this.authz = (Authorization) this.request.getAttribute("authz");
                if (this.security == null) this.security = (Security) this.request.getAttribute("security");
                if (this.userLogin == null) this.userLogin = (GenericValue) this.request.getSession().getAttribute("userLogin");
            }
        } else if (methodType == MethodContext.SERVICE) {
            this.results = FastMap.newInstance();
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

    public Map<String, Object> getEnvMap() {
        return this.env;
    }

    /** Gets the named value from the environment. Supports the "." (dot) syntax to access Map members and the
     * "[]" (bracket) syntax to access List entries. This value is expanded, supporting the insertion of other
     * environment values using the "${}" notation.
     *
     * @param key The name of the environment value to get. Can contain "." and "[]" syntax elements as described above.
     * @return The environment value if found, otherwise null.
     */
    public <T> T getEnv(String key) {
        String ekey = this.expandString(key);
        FlexibleMapAccessor<T> fma = FlexibleMapAccessor.getInstance(ekey);
        return this.getEnv(fma);
    }
    public <T> T getEnv(FlexibleMapAccessor<T> fma) {
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
    public <T> void putEnv(String key, T value) {
        String ekey = this.expandString(key);
        FlexibleMapAccessor<T> fma = FlexibleMapAccessor.getInstance(ekey);
        this.putEnv(fma, value);
    }
    public <T> void putEnv(FlexibleMapAccessor<T> fma, T value) {
        fma.put(this.env, value);
    }

    /** Calls putEnv for each entry in the Map, thus allowing for the additional flexibility in naming
     * supported in that method.
     */
    public void putAllEnv(Map<String, ? extends Object> values) {
        for (Map.Entry<String, ? extends Object> entry: values.entrySet()) {
            this.putEnv(entry.getKey(), entry.getValue());
        }
    }

    /** Removes the named value from the environment. Supports the "." (dot) syntax to access Map members and the
     * "[]" (bracket) syntax to access List entries. This value is expanded, supporting the insertion of other
     * environment values using the "${}" notation.
     *
     * @param key The name of the environment value to get. Can contain "." syntax elements as described above.
     */
    public <T> T removeEnv(String key) {
        String ekey = this.expandString(key);
        FlexibleMapAccessor<T> fma = FlexibleMapAccessor.getInstance(ekey);
        return this.removeEnv(fma);
    }
    public <T> T removeEnv(FlexibleMapAccessor<T> fma) {
        return fma.remove(this.env);
    }

    public Iterator<Map.Entry<String, Object>> iterator() {
        return this.env.entrySet().iterator();
    }

    public Iterator<Map.Entry<String, Object>> getEnvEntryIterator() {
        return this.env.entrySet().iterator();
    }

    public Object getParameter(String key) {
        return this.parameters.get(key);
    }

    public void putParameter(String key, Object value) {
        this.parameters.put(key, value);
    }

    public Map<String, Object> getParameters() {
        return this.parameters;
    }

    public ClassLoader getLoader() {
        return this.loader;
    }

    public Locale getLocale() {
        return this.locale;
    }

    public TimeZone getTimeZone() {
        return this.timeZone;
    }

    public LocalDispatcher getDispatcher() {
        return this.dispatcher;
    }

    public Delegator getDelegator() {
        return this.delegator;
    }

    public Authorization getAuthz() {
        return this.authz;
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

    public Map<String, Object> getResults() {
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
