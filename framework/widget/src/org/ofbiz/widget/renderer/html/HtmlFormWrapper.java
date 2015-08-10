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
package org.ofbiz.widget.renderer.html;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.entity.Delegator;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.widget.model.FormFactory;
import org.ofbiz.widget.model.ModelForm;
import org.ofbiz.widget.renderer.FormRenderer;
import org.ofbiz.widget.renderer.FormStringRenderer;
import org.xml.sax.SAXException;


/**
 * Widget Library - HTML Form Wrapper class - makes it easy to do the setup and render of a form
 */
public class HtmlFormWrapper {

    public static final String module = HtmlFormWrapper.class.getName();

    protected String resourceName;
    protected String formName;
    protected HttpServletRequest request;
    protected HttpServletResponse response;
    protected ModelForm modelForm;
    protected FormStringRenderer renderer;
    protected Map<String, Object> context;

    protected HtmlFormWrapper() {}

    public HtmlFormWrapper(String resourceName, String formName, HttpServletRequest request, HttpServletResponse response)
            throws IOException, SAXException, ParserConfigurationException {
        this.resourceName = resourceName;
        this.formName = formName;
        this.request = request;
        this.response = response;
        Delegator delegator = null;
        try {
            delegator = (Delegator) request.getAttribute("delegator");
            LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
            this.modelForm = FormFactory.getFormFromLocation(resourceName, formName, delegator.getModelReader(), dispatcher.getDispatchContext());
        } catch (IllegalArgumentException iae) {
            Debug.logWarning("Could not find form with name [" + formName + "] in class resource [" + resourceName + "], will try to load it using relative path syntax.", module);
            this.modelForm = FormFactory.getFormFromWebappContext(resourceName, formName, request);
        }

        this.renderer = new HtmlFormRenderer(request, response);

        this.context = new HashMap<String, Object>();
        Map<String, Object> parameterMap = UtilHttp.getParameterMap(request);
        context.put("parameters", parameterMap);

        //make sure the locale is in the context
        context.put("locale", UtilHttp.getLocale(request));
        //make sure the timeZone is in the context
        context.put("timeZone", UtilHttp.getTimeZone(request));

        // if there was an error message, this is an error
        if (UtilValidate.isNotEmpty(request.getAttribute("_ERROR_MESSAGE_"))) {
            context.put("isError", Boolean.TRUE);
        } else {
            context.put("isError", Boolean.FALSE);
        }

        // if a parameter was passed saying this is an error, it is an error
        if ("true".equals(parameterMap.get("isError"))) {
            context.put("isError", Boolean.TRUE);
        }

        Map<String, String> uiLabelMap = UtilGenerics.cast(request.getAttribute("uiLabelMap"));
        if (UtilValidate.isNotEmpty(uiLabelMap) && context.get("uiLabelMap") == null) {
            Debug.logInfo("Got uiLabelMap: " + uiLabelMap, module);
            context.put("uiLabelMap", uiLabelMap);
        }
        if (UtilValidate.isNotEmpty(delegator) && context.get("delegator") == null) {
            context.put("delegator", delegator);
        }
    }

    @SuppressWarnings("unchecked")
    public StringWriter renderFormString(Object contextStack) throws Exception {
        if (contextStack instanceof MapStack) {
            return renderFormString((MapStack) contextStack);
        } else {
            Debug.logWarning("Call renderFormString with a non-MapStack: " + (contextStack == null ? "null" : contextStack.getClass().getName()), module);
            return renderFormString();
        }
    }
    public StringWriter renderFormString(MapStack<String> contextStack) throws Exception {
        // create a new context with the current context on the bottom
        contextStack.push(this.context);
        StringWriter buffer = new StringWriter();
        FormRenderer formRenderer = new FormRenderer(modelForm, renderer);
        formRenderer.render(buffer, contextStack);
        contextStack.pop();
        return buffer;
    }
    public StringWriter renderFormString() throws Exception {
        StringWriter buffer = new StringWriter();
        FormRenderer formRenderer = new FormRenderer(modelForm, renderer);
        formRenderer.render(buffer, context);
        return buffer;
    }

    /**
     * Tells the form library whether this is a response to an error or not.
     * Defaults on initialization according to the presense of an errorMessage
     * in the request or if an isError parameter was passed to the page with
     * the value "true". If true then the prefilled values will come from the
     * parameters Map instead of the value Map.
     */
    public void setIsError(boolean isError) {
        this.context.put("isError", Boolean.valueOf(isError));
    }

    public boolean getIsError() {
        Boolean isErrorBoolean = (Boolean) this.context.get("isError");
        if (isErrorBoolean == null) {
            return false;
        } else {
            return isErrorBoolean.booleanValue();
        }
    }

    /**
     * The "useRequestParameters" value in the form context tells the form library
     * to use the request parameters to fill in values instead of the value map.
     * This is generally used when it is an empty form to pre-set inital values.
     * This is automatically set to false for list and multi forms. For related
     * functionality see the setIsError method.
     *
     * @param useRequestParameters
     */
    public void setUseRequestParameters(boolean useRequestParameters) {
        this.context.put("useRequestParameters", Boolean.valueOf(useRequestParameters));
    }

    public boolean getUseRequestParameters() {
        Boolean useRequestParametersBoolean = (Boolean) this.context.get("useRequestParameters");
        if (useRequestParametersBoolean == null) {
            return false;
        } else {
            return useRequestParametersBoolean.booleanValue();
        }
    }

    public void setFormOverrideName(String formName) {
        this.context.put("formName", formName);
    }

    public void putInContext(String name, Object value) {
        this.context.put(name, value);
    }

    public Object getFromContext(String name) {
        return this.context.get(name);
    }

    public ModelForm getModelForm() {
        return modelForm;
    }

    public FormStringRenderer getRenderer() {
        return renderer;
    }

    public void setRenderer(FormStringRenderer renderer) {
        this.renderer = renderer;
    }
}
