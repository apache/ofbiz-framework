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
package org.apache.ofbiz.minilang.method;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;

/**
 * A container for the Mini-language script engine state.
 */
public final class MethodContext {

    public static final int EVENT = 1;
    public static final int SERVICE = 2;

    private Delegator delegator;
    private LocalDispatcher dispatcher;
    private Map<String, Object> env = new HashMap<String, Object>();
    private ClassLoader loader;
    private Locale locale;
    private int methodType;
    private Map<String, Object> parameters;
    private HttpServletRequest request = null;
    private HttpServletResponse response = null;
    private Map<String, Object> results = new HashMap<String, Object>();
    private Security security;
    private TimeZone timeZone;
    private int traceCount = 0;
    private int traceLogLevel = Debug.INFO;
    private GenericValue userLogin;

    public MethodContext(DispatchContext ctx, Map<String, ? extends Object> context, ClassLoader loader) {
        this.methodType = MethodContext.SERVICE;
        this.parameters = UtilMisc.makeMapWritable(context);
        this.loader = loader;
        this.locale = (Locale) context.get("locale");
        this.timeZone = (TimeZone) context.get("timeZone");
        this.dispatcher = ctx.getDispatcher();
        this.delegator = ctx.getDelegator();
        this.security = ctx.getSecurity();
        this.userLogin = (GenericValue) context.get("userLogin");
        if (this.loader == null) {
            try {
                this.loader = Thread.currentThread().getContextClassLoader();
            } catch (SecurityException e) {
                this.loader = this.getClass().getClassLoader();
            }
        }
    }

    public MethodContext(HttpServletRequest request, HttpServletResponse response, ClassLoader loader) {
        this.methodType = MethodContext.EVENT;
        this.parameters = UtilHttp.getCombinedMap(request);
        this.loader = loader;
        this.request = request;
        this.response = response;
        this.locale = UtilHttp.getLocale(request);
        this.timeZone = UtilHttp.getTimeZone(request);
        this.dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        this.delegator = (Delegator) request.getAttribute("delegator");
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

    /**
     * This is a very simple constructor which assumes the needed objects (dispatcher, delegator, security, request, response, etc) are in the context. Will result in calling method as a
     * service or event, as specified.
     */
    public MethodContext(Map<String, ? extends Object> context, ClassLoader loader, int methodType) {
        this.methodType = methodType;
        this.parameters = UtilMisc.makeMapWritable(context);
        this.loader = loader;
        this.locale = (Locale) context.get("locale");
        this.timeZone = (TimeZone) context.get("timeZone");
        this.dispatcher = (LocalDispatcher) context.get("dispatcher");
        this.delegator = (Delegator) context.get("delegator");
        this.security = (Security) context.get("security");
        this.userLogin = (GenericValue) context.get("userLogin");
        if (methodType == MethodContext.EVENT) {
            this.request = (HttpServletRequest) context.get("request");
            this.response = (HttpServletResponse) context.get("response");
            if (this.locale == null)
                this.locale = UtilHttp.getLocale(request);
            if (this.timeZone == null)
                this.timeZone = UtilHttp.getTimeZone(request);
            // make sure the delegator and other objects are in place, getting from
            // request if necessary; assumes this came through the ControlServlet
            // or something similar
            if (this.request != null) {
                if (this.dispatcher == null)
                    this.dispatcher = (LocalDispatcher) this.request.getAttribute("dispatcher");
                if (this.delegator == null)
                    this.delegator = (Delegator) this.request.getAttribute("delegator");
                if (this.security == null)
                    this.security = (Security) this.request.getAttribute("security");
                if (this.userLogin == null)
                    this.userLogin = (GenericValue) this.request.getSession().getAttribute("userLogin");
            }
        }
        if (this.loader == null) {
            try {
                this.loader = Thread.currentThread().getContextClassLoader();
            } catch (SecurityException e) {
                this.loader = this.getClass().getClassLoader();
            }
        }
    }

    public Delegator getDelegator() {
        return this.delegator;
    }

    public LocalDispatcher getDispatcher() {
        return this.dispatcher;
    }

    public <T> T getEnv(FlexibleMapAccessor<T> fma) {
        return fma.get(this.env);
    }

    /**
     * Gets the named value from the environment. Supports the "." (dot) syntax to access Map members and the "[]" (bracket) syntax to access List entries. This value is expanded, supporting the
     * insertion of other environment values using the "${}" notation.
     * 
     * @param key
     *            The name of the environment value to get. Can contain "." and "[]" syntax elements as described above.
     * @return The environment value if found, otherwise null.
     */
    public <T> T getEnv(String key) {
        String ekey = FlexibleStringExpander.expandString(key, this.env);
        FlexibleMapAccessor<T> fma = FlexibleMapAccessor.getInstance(ekey);
        return this.getEnv(fma);
    }

    public Map<String, Object> getEnvMap() {
        return this.env;
    }

    public ClassLoader getLoader() {
        return this.loader;
    }

    public Locale getLocale() {
        return this.locale;
    }

    public int getMethodType() {
        return this.methodType;
    }

    public Object getParameter(String key) {
        return this.parameters.get(key);
    }

    public Map<String, Object> getParameters() {
        return this.parameters;
    }

    public HttpServletRequest getRequest() {
        return this.request;
    }

    public HttpServletResponse getResponse() {
        return this.response;
    }

    public Object getResult(String key) {
        return this.results.get(key);
    }

    public Map<String, Object> getResults() {
        return this.results;
    }

    public Security getSecurity() {
        return this.security;
    }

    public TimeZone getTimeZone() {
        return this.timeZone;
    }

    public int getTraceLogLevel() {
        return this.traceLogLevel;
    }

    public GenericValue getUserLogin() {
        return this.userLogin;
    }

    public boolean isTraceOn() {
        return this.traceCount > 0;
    }

    /**
     * Calls putEnv for each entry in the Map, thus allowing for the additional flexibility in naming supported in that method.
     */
    public void putAllEnv(Map<String, ? extends Object> values) {
        for (Map.Entry<String, ? extends Object> entry : values.entrySet()) {
            this.putEnv(entry.getKey(), entry.getValue());
        }
    }

    public <T> void putEnv(FlexibleMapAccessor<T> fma, T value) {
        fma.put(this.env, value);
    }

    /**
     * Puts the named value in the environment. Supports the "." (dot) syntax to access Map members and the "[]" (bracket) syntax to access List entries. If the brackets for a list are empty the value
     * will be appended to end of the list, otherwise the value will be set in the position of the number in the brackets. If a "+" (plus sign) is included inside the square brackets before the index
     * number the value will inserted/added at that index instead of set at that index. This value is expanded, supporting the insertion of other environment values using the "${}" notation.
     * 
     * @param key
     *            The name of the environment value to get. Can contain "." syntax elements as described above.
     * @param value
     *            The value to set in the named environment location.
     */
    public <T> void putEnv(String key, T value) {
        String ekey = FlexibleStringExpander.expandString(key, this.env);
        FlexibleMapAccessor<T> fma = FlexibleMapAccessor.getInstance(ekey);
        this.putEnv(fma, value);
    }

    public void putParameter(String key, Object value) {
        this.parameters.put(key, value);
    }

    public void putResult(String key, Object value) {
        this.results.put(key, value);
    }

    public <T> T removeEnv(FlexibleMapAccessor<T> fma) {
        return fma.remove(this.env);
    }

    /**
     * Removes the named value from the environment. Supports the "." (dot) syntax to access Map members and the "[]" (bracket) syntax to access List entries. This value is expanded, supporting the
     * insertion of other environment values using the "${}" notation.
     * 
     * @param key
     *            The name of the environment value to get. Can contain "." syntax elements as described above.
     */
    public <T> T removeEnv(String key) {
        String ekey = FlexibleStringExpander.expandString(key, this.env);
        FlexibleMapAccessor<T> fma = FlexibleMapAccessor.getInstance(ekey);
        return this.removeEnv(fma);
    }

    public void setTraceOff() {
        if (this.traceCount > 0) {
            this.traceCount--;
        }
    }

    public void setTraceOn(int logLevel) {
        if (this.traceCount == 0) {
            // Outermost trace element sets the logging level
            this.traceLogLevel = logLevel;
        }
        this.traceCount++;
    }

    public void setUserLogin(GenericValue userLogin, String userLoginEnvName) {
        this.userLogin = userLogin;
        this.putEnv(userLoginEnvName, userLogin);
    }
}
