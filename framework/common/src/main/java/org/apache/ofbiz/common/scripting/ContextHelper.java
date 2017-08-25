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
package org.apache.ofbiz.common.scripting;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Assert;
import org.apache.ofbiz.base.util.ScriptUtil;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.widget.renderer.VisualTheme;

/**
 * A set of <code>ScriptContext</code> convenience methods for scripting engines.
 */
public final class ContextHelper {

    public static final String module = ContextHelper.class.getName();
    private static final int EVENT = 1;
    private static final int SERVICE = 2;
    private static final int UNKNOWN = 3;

    private final ScriptContext context;
    private final int scriptType;

    public ContextHelper(ScriptContext context) {
        Assert.notNull("context", context);
        this.context = context;
        if (context.getAttribute("request") != null) {
            this.scriptType = EVENT;
        } else if (context.getAttribute("dctx") != null) {
            this.scriptType = SERVICE;
        } else {
            this.scriptType = UNKNOWN;
        }
    }

    public Object addBinding(String key, Object value) {
        return getBindings().put(key, value);
    }

    /** Expands environment variables delimited with ${} */
    public String expandString(String original) {
        return FlexibleStringExpander.expandString(original, getBindings());
    }

    public Map<String, Object> getBindings() {
        return this.context.getBindings(ScriptContext.ENGINE_SCOPE);
    }

    public Delegator getDelegator() {
        return (Delegator) this.context.getAttribute("delegator");
    }

    public LocalDispatcher getDispatcher() {
        return (LocalDispatcher) this.context.getAttribute("dispatcher");
    }

    public <T> T getEnv(FlexibleMapAccessor<T> fma) {
        return fma.get(getBindings());
    }

    /**
     * Gets the named value from the environment. Supports the "." (dot) syntax to access
     * Map members and the "[]" (bracket) syntax to access List entries. This value is
     * expanded, supporting the insertion of other environment values using the "${}"
     * notation.
     * 
     * @param key
     *            The name of the environment value to get. Can contain "." and "[]"
     *            syntax elements as described above.
     * @return The environment value if found, otherwise null.
     */
    public <T> T getEnv(String key) {
        String ekey = this.expandString(key);
        FlexibleMapAccessor<T> fma = FlexibleMapAccessor.getInstance(ekey);
        return getEnv(fma);
    }

    public List<String> getErrorMessages() {
        List<String> errorMessages = null;
        if (isService()) {
            errorMessages = UtilGenerics.checkList(getResults().get(ModelService.ERROR_MESSAGE_LIST));
            if (errorMessages == null) {
                errorMessages = new LinkedList<String>();
                getResults().put(ModelService.ERROR_MESSAGE_LIST, errorMessages);
            }
        } else {
            errorMessages = UtilGenerics.checkList(getResults().get("_error_message_list_"));
            if (errorMessages == null) {
                errorMessages = new LinkedList<String>();
                getResults().put("_error_message_list_", errorMessages);
            }
        }
        return errorMessages;
    }

    public Iterator<Map.Entry<String, Object>> getEnvEntryIterator() {
        return getBindings().entrySet().iterator();
    }

    public Locale getLocale() {
        return (Locale) this.context.getAttribute("locale");
    }

    public VisualTheme getVisualTheme() {
        return (VisualTheme) this.context.getAttribute("visualTheme");
    }

    public Object getParameter(String key) {
        return getParameters().get(key);
    }

    public Map<String, Object> getParameters() {
        Map<String, Object> parameters = UtilGenerics.checkMap(this.context.getAttribute(ScriptUtil.PARAMETERS_KEY));
        if (parameters == null) {
            parameters =  new LinkedHashMap<String, Object>();
            this.context.setAttribute(ScriptUtil.PARAMETERS_KEY, parameters, ScriptContext.ENGINE_SCOPE);
        }
        return parameters;
    }

    public HttpServletRequest getRequest() {
        return (HttpServletRequest) this.context.getAttribute("request");
    }

    public HttpServletResponse getResponse() {
        return (HttpServletResponse) this.context.getAttribute("response");
    }

    public Object getResult(String key) {
        return getResults().get(key);
    }

    public Map<String, Object> getResults() {
        Map<String, Object> results = UtilGenerics.checkMap(this.context.getAttribute(ScriptUtil.RESULT_KEY));
        if (results == null) {
            results =  new LinkedHashMap<String, Object>();
            this.context.setAttribute(ScriptUtil.RESULT_KEY, results, ScriptContext.ENGINE_SCOPE);
        }
        return results;
    }

    public String getScriptName() {
        String scriptName = (String) this.context.getAttribute(ScriptEngine.FILENAME);
        return scriptName != null ? scriptName : "Unknown";
    }

    public Security getSecurity() {
        return (Security) this.context.getAttribute("security");
    }

    public TimeZone getTimeZone() {
        return (TimeZone) this.context.getAttribute("timeZone");
    }

    public GenericValue getUserLogin() {
        return (GenericValue) this.context.getAttribute("userLogin");
    }

    public boolean isEvent() {
        return this.scriptType == EVENT;
    }

    public boolean isService() {
        return this.scriptType == SERVICE;
    }

    /**
     * Calls putEnv for each entry in the Map, thus allowing for the additional
     * flexibility in naming supported in that method.
     */
    public void putAllEnv(Map<String, ? extends Object> values) {
        for (Map.Entry<String, ? extends Object> entry : values.entrySet()) {
            this.putEnv(entry.getKey(), entry.getValue());
        }
    }

    public <T> void putEnv(FlexibleMapAccessor<T> fma, T value) {
        fma.put(getBindings(), value);
    }

    /**
     * Puts the named value in the environment. Supports the "." (dot) syntax to access
     * Map members and the "[]" (bracket) syntax to access List entries. If the brackets
     * for a list are empty the value will be appended to end of the list, otherwise the
     * value will be set in the position of the number in the brackets. If a "+" (plus
     * sign) is included inside the square brackets before the index number the value will
     * inserted/added at that index instead of set at that index. This value is expanded,
     * supporting the insertion of other environment values using the "${}" notation.
     * 
     * @param key
     *            The name of the environment value to get. Can contain "." syntax
     *            elements as described above.
     * @param value
     *            The value to set in the named environment location.
     */
    public <T> void putEnv(String key, T value) {
        String ekey = this.expandString(key);
        FlexibleMapAccessor<T> fma = FlexibleMapAccessor.getInstance(ekey);
        this.putEnv(fma, value);
    }

    public void putParameter(String key, Object value) {
        getParameters().put(key, value);
    }

    public void putResult(String key, Object value) {
        getResults().put(key, value);
    }

    public void putResults(Map<String, Object> results) {
        getResults().putAll(results);
    }

    public Object removeBinding(String key) {
        return getBindings().remove(key);
    }

    public <T> T removeEnv(FlexibleMapAccessor<T> fma) {
        return fma.remove(getBindings());
    }

    /**
     * Removes the named value from the environment. Supports the "." (dot) syntax to
     * access Map members and the "[]" (bracket) syntax to access List entries. This value
     * is expanded, supporting the insertion of other environment values using the "${}"
     * notation.
     * 
     * @param key
     *            The name of the environment value to get. Can contain "." syntax
     *            elements as described above.
     */
    public <T> T removeEnv(String key) {
        String ekey = this.expandString(key);
        FlexibleMapAccessor<T> fma = FlexibleMapAccessor.getInstance(ekey);
        return removeEnv(fma);
    }

    public void setUserLogin(GenericValue userLogin, String userLoginEnvName) {
        putEnv(userLoginEnvName, userLogin);
    }
}
