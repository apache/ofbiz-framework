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
package org.apache.ofbiz.widget.model;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.PatternSyntaxException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.ScriptUtil;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.finder.ByAndFinder;
import org.apache.ofbiz.entity.finder.ByConditionFinder;
import org.apache.ofbiz.entity.finder.EntityFinderUtil;
import org.apache.ofbiz.entity.finder.PrimaryKeyFinder;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.widget.WidgetWorker;
import org.w3c.dom.Element;

/**
 * Abstract base class for the action models.
 */
@SuppressWarnings("serial")
public abstract class AbstractModelAction implements Serializable, ModelAction {

    /*
     * ----------------------------------------------------------------------- *
     *                     DEVELOPERS PLEASE READ
     * ----------------------------------------------------------------------- *
     * 
     * This model is intended to be a read-only data structure that represents
     * an XML element. Outside of object construction, the class should not
     * have any behaviors.
     * 
     * Instances of this class will be shared by multiple threads - therefore
     * it is immutable. DO NOT CHANGE THE OBJECT'S STATE AT RUN TIME!
     * 
     */

    public static final String module = AbstractModelAction.class.getName();

    /**
     * Returns a new <code>ModelAction</code> instance, built from <code>actionElement</code>.
     * 
     * @param modelWidget The <code>ModelWidget</code> that contains the &lt;actions&gt; element
     * @param actionElement
     * @return A new <code>ModelAction</code> instance
     */
    public static ModelAction newInstance(ModelWidget modelWidget, Element actionElement) {
        String nodeName = UtilXml.getNodeNameIgnorePrefix(actionElement);
        if ("set".equals(nodeName)) {
            return new SetField(modelWidget, actionElement);
        } else if ("property-map".equals(nodeName)) {
            return new PropertyMap(modelWidget, actionElement);
        } else if ("property-to-field".equals(nodeName)) {
            return new PropertyToField(modelWidget, actionElement);
        } else if ("script".equals(nodeName)) {
            return new Script(modelWidget, actionElement);
        } else if ("service".equals(nodeName)) {
            return new Service(modelWidget, actionElement);
        } else if ("entity-one".equals(nodeName)) {
            return new EntityOne(modelWidget, actionElement);
        } else if ("entity-and".equals(nodeName)) {
            return new EntityAnd(modelWidget, actionElement);
        } else if ("entity-condition".equals(nodeName)) {
            return new EntityCondition(modelWidget, actionElement);
        } else if ("get-related-one".equals(nodeName)) {
            return new GetRelatedOne(modelWidget, actionElement);
        } else if ("get-related".equals(nodeName)) {
            return new GetRelated(modelWidget, actionElement);
        } else {
            throw new IllegalArgumentException("Action element not supported with name: " + actionElement.getNodeName());
        }
    }

    public static List<ModelAction> readSubActions(ModelWidget modelWidget, Element parentElement) {
        List<? extends Element> actionElementList = UtilXml.childElementList(parentElement);
        List<ModelAction> actions = new ArrayList<ModelAction>(actionElementList.size());
        for (Element actionElement : actionElementList) {
            actions.add(newInstance(modelWidget, actionElement));
        }
        return Collections.unmodifiableList(actions);
    }

    /**
     * Executes the actions contained in <code>actions</code>.
     * 
     * @param actions
     * @param context
     */
    public static void runSubActions(List<ModelAction> actions, Map<String, Object> context) {
        if (actions == null)
            return;
        for (ModelAction action : actions) {
            if (Debug.verboseOn())
                Debug.logVerbose("Running action " + action.getClass().getName(), module);
            try {
                action.runAction(context);
            } catch (GeneralException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private final ModelWidget modelWidget;

    protected AbstractModelAction() {
        // FIXME: This should not be null.
        this.modelWidget = null;
    }

    protected AbstractModelAction(ModelWidget modelWidget, Element actionElement) {
        this.modelWidget = modelWidget;
        if (Debug.verboseOn())
            Debug.logVerbose("Reading widget action with name: " + actionElement.getNodeName(), module);
    }

    /**
     * Returns the <code>ModelWidget</code> that contains the &lt;actions&gt; element.
     * 
     * @return The <code>ModelWidget</code> that contains the &lt;actions&gt; element
     */
    public ModelWidget getModelWidget() {
        return modelWidget;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        ModelActionVisitor visitor = new XmlWidgetActionVisitor(sb);
        try {
            accept(visitor);
        } catch (Exception e) {
            Debug.logWarning(e, "Exception thrown in XmlWidgetActionVisitor: ", module);
        }
        return sb.toString();
    }

    /**
     * Models the &lt;entity-and&gt; element.
     * 
     * @see <code>widget-screen.xsd</code>
     */
    public static class EntityAnd extends AbstractModelAction {
        private final ByAndFinder finder;

        public EntityAnd(ModelWidget modelWidget, Element entityAndElement) {
            super(modelWidget, entityAndElement);
            finder = new ByAndFinder(entityAndElement);
        }

        @Override
        public void accept(ModelActionVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        public ByAndFinder getFinder() {
            return this.finder;
        }

        @Override
        public void runAction(Map<String, Object> context) {
            try {
                finder.runFind(context, WidgetWorker.getDelegator(context));
            } catch (GeneralException e) {
                String errMsg = "Error doing entity query by condition: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
        }
    }

    /**
     * Models the &lt;entity-condition&gt; element.
     * 
     * @see <code>widget-screen.xsd</code>
     */
    public static class EntityCondition extends AbstractModelAction {
        private final ByConditionFinder finder;

        public EntityCondition(ModelWidget modelWidget, Element entityConditionElement) {
            super(modelWidget, entityConditionElement);
            finder = new ByConditionFinder(entityConditionElement);
        }

        @Override
        public void accept(ModelActionVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        public ByConditionFinder getFinder() {
            return this.finder;
        }

        @Override
        public void runAction(Map<String, Object> context) {
            try {
                finder.runFind(context, WidgetWorker.getDelegator(context));
            } catch (GeneralException e) {
                String errMsg = "Error doing entity query by condition: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
        }
    }

    /**
     * Models the &lt;entity-one&gt; element.
     * 
     * @see <code>widget-common.xsd</code>
     */
    public static class EntityOne extends AbstractModelAction {
        private final PrimaryKeyFinder finder;

        public EntityOne(ModelWidget modelWidget, Element entityOneElement) {
            super(modelWidget, entityOneElement);
            finder = new PrimaryKeyFinder(entityOneElement);
        }

        @Override
        public void accept(ModelActionVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        public PrimaryKeyFinder getFinder() {
            return this.finder;
        }

        @Override
        public void runAction(Map<String, Object> context) {
            try {
                finder.runFind(context, WidgetWorker.getDelegator(context));
            } catch (GeneralException e) {
                String errMsg = "Error doing entity query by condition: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
        }
    }

    /**
     * Models the &lt;get-related&gt; element.
     * 
     * @see <code>widget-common.xsd</code>
     */
    public static class GetRelated extends AbstractModelAction {
        private final FlexibleMapAccessor<List<GenericValue>> listNameAcsr;
        private final FlexibleMapAccessor<Map<String, Object>> mapAcsr;
        private final FlexibleMapAccessor<List<String>> orderByListAcsr;
        private final String relationName;
        private final boolean useCache;
        private final FlexibleMapAccessor<Object> valueNameAcsr;

        public GetRelated(ModelWidget modelWidget, Element getRelatedElement) {
            super(modelWidget, getRelatedElement);
            this.valueNameAcsr = FlexibleMapAccessor.getInstance(getRelatedElement.getAttribute("value-field"));
            this.listNameAcsr = FlexibleMapAccessor.getInstance(getRelatedElement.getAttribute("list"));
            this.relationName = getRelatedElement.getAttribute("relation-name");
            this.mapAcsr = FlexibleMapAccessor.getInstance(getRelatedElement.getAttribute("map"));
            this.orderByListAcsr = FlexibleMapAccessor.getInstance(getRelatedElement.getAttribute("order-by-list"));
            this.useCache = "true".equals(getRelatedElement.getAttribute("use-cache"));
        }

        @Override
        public void accept(ModelActionVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        public String getRelationName() {
            return this.relationName;
        }

        @Override
        public void runAction(Map<String, Object> context) {
            Object valueObject = valueNameAcsr.get(context);
            if (valueObject == null) {
                Debug.logVerbose("Value not found with name: " + valueNameAcsr + ", not getting related...", module);
                return;
            }
            if (!(valueObject instanceof GenericValue)) {
                String errMsg = "Env variable for value-name " + valueNameAcsr.toString()
                        + " is not a GenericValue object; for the relation-name: " + relationName + "]";
                Debug.logError(errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
            GenericValue value = (GenericValue) valueObject;
            List<String> orderByNames = null;
            if (!orderByListAcsr.isEmpty()) {
                orderByNames = orderByListAcsr.get(context);
            }
            Map<String, Object> constraintMap = null;
            if (!mapAcsr.isEmpty()) {
                constraintMap = mapAcsr.get(context);
            }
            try {
                listNameAcsr.put(context, value.getRelated(relationName, constraintMap, orderByNames, useCache));
            } catch (GenericEntityException e) {
                String errMsg = "Problem getting related from entity with name " + value.getEntityName()
                        + " for the relation-name: " + relationName + ": " + e.getMessage();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
        }

        public FlexibleMapAccessor<List<GenericValue>> getListNameAcsr() {
            return listNameAcsr;
        }

        public FlexibleMapAccessor<Map<String, Object>> getMapAcsr() {
            return mapAcsr;
        }

        public FlexibleMapAccessor<List<String>> getOrderByListAcsr() {
            return orderByListAcsr;
        }

        public boolean getUseCache() {
            return useCache;
        }

        public FlexibleMapAccessor<Object> getValueNameAcsr() {
            return valueNameAcsr;
        }
    }

    /**
     * Models the &lt;get-related-one&gt; element.
     * 
     * @see <code>widget-common.xsd</code>
     */
    public static class GetRelatedOne extends AbstractModelAction {
        private final String relationName;
        private final FlexibleMapAccessor<Object> toValueNameAcsr;
        private final boolean useCache;
        private final FlexibleMapAccessor<Object> valueNameAcsr;

        public GetRelatedOne(ModelWidget modelWidget, Element getRelatedOneElement) {
            super(modelWidget, getRelatedOneElement);
            this.valueNameAcsr = FlexibleMapAccessor.getInstance(getRelatedOneElement.getAttribute("value-field"));
            this.toValueNameAcsr = FlexibleMapAccessor.getInstance(getRelatedOneElement.getAttribute("to-value-field"));
            this.relationName = getRelatedOneElement.getAttribute("relation-name");
            this.useCache = "true".equals(getRelatedOneElement.getAttribute("use-cache"));
        }

        @Override
        public void accept(ModelActionVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        public String getRelationName() {
            return this.relationName;
        }

        @Override
        public void runAction(Map<String, Object> context) {
            Object valueObject = valueNameAcsr.get(context);
            if (valueObject == null) {
                Debug.logVerbose("Value not found with name: " + valueNameAcsr + ", not getting related...", module);
                return;
            }
            if (!(valueObject instanceof GenericValue)) {
                String errMsg = "Env variable for value-name " + valueNameAcsr.toString()
                        + " is not a GenericValue object; for the relation-name: " + relationName + "]";
                Debug.logError(errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
            GenericValue value = (GenericValue) valueObject;
            try {
                toValueNameAcsr.put(context, value.getRelatedOne(relationName, useCache));
            } catch (GenericEntityException e) {
                String errMsg = "Problem getting related one from entity with name " + value.getEntityName()
                        + " for the relation-name: " + relationName + ": " + e.getMessage();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
        }

        public FlexibleMapAccessor<Object> getToValueNameAcsr() {
            return toValueNameAcsr;
        }

        public boolean getUseCache() {
            return useCache;
        }

        public FlexibleMapAccessor<Object> getValueNameAcsr() {
            return valueNameAcsr;
        }
    }

    /**
     * Models the &lt;property-map&gt; element.
     * 
     * @see <code>widget-common.xsd</code>
     */
    public static class PropertyMap extends AbstractModelAction {
        private final FlexibleStringExpander globalExdr;
        private final FlexibleMapAccessor<ResourceBundleMapWrapper> mapNameAcsr;
        private final FlexibleStringExpander resourceExdr;

        public PropertyMap(ModelWidget modelWidget, Element setElement) {
            super(modelWidget, setElement);
            this.resourceExdr = FlexibleStringExpander.getInstance(setElement.getAttribute("resource"));
            this.mapNameAcsr = FlexibleMapAccessor.getInstance(setElement.getAttribute("map-name"));
            this.globalExdr = FlexibleStringExpander.getInstance(setElement.getAttribute("global"));
        }

        @Override
        public void accept(ModelActionVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public void runAction(Map<String, Object> context) {
            String globalStr = this.globalExdr.expandString(context);
            // default to false
            boolean global = "true".equals(globalStr);
            Locale locale = (Locale) context.get("locale");
            String resource = this.resourceExdr.expandString(context, locale);
            ResourceBundleMapWrapper existingPropMap = this.mapNameAcsr.get(context);
            if (existingPropMap == null) {
                this.mapNameAcsr.put(context, UtilProperties.getResourceBundleMap(resource, locale, context));
            } else {
                try {
                    existingPropMap.addBottomResourceBundle(resource);
                } catch (IllegalArgumentException e) {
                    // log the error, but don't let it kill everything just for a typo or bad char in an l10n file
                    Debug.logError(e, "Error adding resource bundle [" + resource + "]: " + e.toString(), module);
                }
            }
            if (global) {
                Map<String, Object> globalCtx = UtilGenerics.checkMap(context.get("globalContext"));
                if (globalCtx != null) {
                    ResourceBundleMapWrapper globalExistingPropMap = this.mapNameAcsr.get(globalCtx);
                    if (globalExistingPropMap == null) {
                        this.mapNameAcsr.put(globalCtx, UtilProperties.getResourceBundleMap(resource, locale, context));
                    } else {
                        // is it the same object? if not add it in here too...
                        if (existingPropMap != globalExistingPropMap) {
                            try {
                                globalExistingPropMap.addBottomResourceBundle(resource);
                            } catch (IllegalArgumentException e) {
                                // log the error, but don't let it kill everything just for a typo or bad char in an l10n file
                                Debug.logError(e, "Error adding resource bundle [" + resource + "]: " + e.toString(), module);
                            }
                        }
                    }
                }
            }
        }

        public FlexibleStringExpander getGlobalExdr() {
            return globalExdr;
        }

        public FlexibleMapAccessor<ResourceBundleMapWrapper> getMapNameAcsr() {
            return mapNameAcsr;
        }

        public FlexibleStringExpander getResourceExdr() {
            return resourceExdr;
        }
    }

    /**
     * Models the &lt;property-to-field&gt; element.
     * 
     * @see <code>widget-common.xsd</code>
     */
    public static class PropertyToField extends AbstractModelAction {
        private final FlexibleMapAccessor<List<? extends Object>> argListAcsr;
        private final FlexibleStringExpander defaultExdr;
        private final FlexibleMapAccessor<Object> fieldAcsr;
        private final FlexibleStringExpander globalExdr;
        private final boolean noLocale;
        private final FlexibleStringExpander propertyExdr;
        private final FlexibleStringExpander resourceExdr;

        public PropertyToField(ModelWidget modelWidget, Element setElement) {
            super(modelWidget, setElement);
            this.resourceExdr = FlexibleStringExpander.getInstance(setElement.getAttribute("resource"));
            this.propertyExdr = FlexibleStringExpander.getInstance(setElement.getAttribute("property"));
            this.fieldAcsr = FlexibleMapAccessor.getInstance(setElement.getAttribute("field"));
            this.defaultExdr = FlexibleStringExpander.getInstance(setElement.getAttribute("default"));
            this.noLocale = "true".equals(setElement.getAttribute("no-locale"));
            this.argListAcsr = FlexibleMapAccessor.getInstance(setElement.getAttribute("arg-list-name"));
            this.globalExdr = FlexibleStringExpander.getInstance(setElement.getAttribute("global"));
        }

        @Override
        public void accept(ModelActionVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public void runAction(Map<String, Object> context) {
            //String globalStr = this.globalExdr.expandString(context);
            // default to false
            //boolean global = "true".equals(globalStr);
            Locale locale = (Locale) context.get("locale");
            String resource = this.resourceExdr.expandString(context, locale);
            String property = this.propertyExdr.expandString(context, locale);
            String value = null;
            if (noLocale) {
                value = EntityUtilProperties.getPropertyValue(resource, property, WidgetWorker.getDelegator(context));
            } else {
                value = EntityUtilProperties.getMessage(resource, property, locale, WidgetWorker.getDelegator(context));
            }
            if (UtilValidate.isEmpty(value)) {
                value = this.defaultExdr.expandString(context);
            }
            // note that expanding the value string here will handle defaultValue and the string from
            //  the properties file; if we decide later that we don't want the string from the properties
            //  file to be expanded we should just expand the defaultValue at the beginning of this method.
            value = FlexibleStringExpander.expandString(value, context);
            if (!argListAcsr.isEmpty()) {
                List<? extends Object> argList = argListAcsr.get(context);
                if (UtilValidate.isNotEmpty(argList)) {
                    value = MessageFormat.format(value, argList.toArray());
                }
            }
            fieldAcsr.put(context, value);
        }

        public FlexibleMapAccessor<List<? extends Object>> getArgListAcsr() {
            return argListAcsr;
        }

        public FlexibleStringExpander getDefaultExdr() {
            return defaultExdr;
        }

        public FlexibleMapAccessor<Object> getFieldAcsr() {
            return fieldAcsr;
        }

        public FlexibleStringExpander getGlobalExdr() {
            return globalExdr;
        }

        public boolean getNoLocale() {
            return noLocale;
        }

        public FlexibleStringExpander getPropertyExdr() {
            return propertyExdr;
        }

        public FlexibleStringExpander getResourceExdr() {
            return resourceExdr;
        }
    }

    /**
     * Models the &lt;script&gt; element.
     * 
     * @see <code>widget-common.xsd</code>
     */
    public static class Script extends AbstractModelAction {
        private final String location;
        private final String method;

        public Script(ModelWidget modelWidget, Element scriptElement) {
            super(modelWidget, scriptElement);
            String scriptLocation = scriptElement.getAttribute("location");
            this.location = WidgetWorker.getScriptLocation(scriptLocation);
            this.method = WidgetWorker.getScriptMethodName(scriptLocation);
        }

        @Override
        public void accept(ModelActionVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public void runAction(Map<String, Object> context) throws GeneralException {
            if (location.endsWith(".xml")) {
                Map<String, Object> localContext = new HashMap<String, Object>();
                localContext.putAll(context);
                DispatchContext ctx = WidgetWorker.getDispatcher(context).getDispatchContext();
                MethodContext methodContext = new MethodContext(ctx, localContext, null);
                try {
                    SimpleMethod.runSimpleMethod(location, method, methodContext);
                    context.putAll(methodContext.getResults());
                } catch (MiniLangException e) {
                    throw new GeneralException("Error running simple method at location [" + location + "]", e);
                }
            } else {
                ScriptUtil.executeScript(this.location, this.method, context);
            }
        }

        public String getLocation() {
            return location;
        }

        public String getMethod() {
            return method;
        }
    }

    /**
     * Models the &lt;service&gt; element.
     * 
     * @see <code>widget-screen.xsd</code>
     */
    public static class Service extends AbstractModelAction {
        private final FlexibleStringExpander autoFieldMapExdr;
        private final Map<FlexibleMapAccessor<Object>, Object> fieldMap;
        private final FlexibleMapAccessor<Map<String, Object>> resultMapNameAcsr;
        private final FlexibleStringExpander serviceNameExdr;

        public Service(ModelWidget modelWidget, Element serviceElement) {
            super(modelWidget, serviceElement);
            this.serviceNameExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("service-name"));
            this.resultMapNameAcsr = FlexibleMapAccessor.getInstance(serviceElement.getAttribute("result-map"));
            this.autoFieldMapExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("auto-field-map"));
            this.fieldMap = EntityFinderUtil.makeFieldMap(serviceElement);
        }

        @Override
        public void accept(ModelActionVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        public FlexibleStringExpander getServiceNameExdr() {
            return this.serviceNameExdr;
        }

        @Override
        public void runAction(Map<String, Object> context) {
            String serviceNameExpanded = this.serviceNameExdr.expandString(context);
            if (UtilValidate.isEmpty(serviceNameExpanded)) {
                throw new IllegalArgumentException("Service name was empty, expanded from: " + this.serviceNameExdr.getOriginal());
            }
            String autoFieldMapString = this.autoFieldMapExdr.expandString(context);
            try {
                Map<String, Object> serviceContext = null;
                if ("true".equals(autoFieldMapString)) {
                    DispatchContext dc = WidgetWorker.getDispatcher(context).getDispatchContext();
                    // try a map called "parameters", try it first so values from here are overriden by values in the main context
                    Map<String, Object> combinedMap = new HashMap<String, Object>();
                    Map<String, Object> parametersObj = UtilGenerics.toMap(context.get("parameters"));
                    if (parametersObj != null) {
                        combinedMap.putAll(parametersObj);
                    }
                    combinedMap.putAll(context);
                    serviceContext = dc.makeValidContext(serviceNameExpanded, ModelService.IN_PARAM, combinedMap);
                } else if (UtilValidate.isNotEmpty(autoFieldMapString) && !"false".equals(autoFieldMapString)) {
                    FlexibleMapAccessor<Object> fieldFma = FlexibleMapAccessor.getInstance(autoFieldMapString);
                    Map<String, Object> autoFieldMap = UtilGenerics.toMap(fieldFma.get(context));
                    if (autoFieldMap != null) {
                        serviceContext = WidgetWorker.getDispatcher(context).getDispatchContext()
                                .makeValidContext(serviceNameExpanded, ModelService.IN_PARAM, autoFieldMap);
                    }
                }
                if (serviceContext == null) {
                    serviceContext = new HashMap<String, Object>();
                }
                if (this.fieldMap != null) {
                    EntityFinderUtil.expandFieldMapToContext(this.fieldMap, context, serviceContext);
                }
                Map<String, Object> result = WidgetWorker.getDispatcher(context).runSync(serviceNameExpanded, serviceContext);
                if (!this.resultMapNameAcsr.isEmpty()) {
                    this.resultMapNameAcsr.put(context, result);
                    String queryString = (String) result.get("queryString");
                    context.put("queryString", queryString);
                    context.put("queryStringMap", result.get("queryStringMap"));
                    if (UtilValidate.isNotEmpty(queryString)) {
                        try {
                            String queryStringEncoded = queryString.replaceAll("&", "%26");
                            context.put("queryStringEncoded", queryStringEncoded);
                        } catch (PatternSyntaxException e) {

                        }
                    }
                } else {
                    context.putAll(result);
                }
            } catch (GenericServiceException e) {
                String errMsg = "Error calling service with name " + serviceNameExpanded + ": " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
        }

        public FlexibleStringExpander getAutoFieldMapExdr() {
            return autoFieldMapExdr;
        }

        public Map<FlexibleMapAccessor<Object>, Object> getFieldMap() {
            return fieldMap;
        }

        public FlexibleMapAccessor<Map<String, Object>> getResultMapNameAcsr() {
            return resultMapNameAcsr;
        }
    }

    /**
     * Models the &lt;set&gt; element.
     * 
     * @see <code>widget-common.xsd</code>
     */
    public static class SetField extends AbstractModelAction {
        private final FlexibleStringExpander defaultExdr;
        private final FlexibleMapAccessor<Object> field;
        private final FlexibleMapAccessor<Object> fromField;
        private final String fromScope;
        private final FlexibleStringExpander globalExdr;
        private final String toScope;
        private final String type;
        private final FlexibleStringExpander valueExdr;

        public SetField(ModelWidget modelWidget, Element setElement) {
            super(modelWidget, setElement);
            this.field = FlexibleMapAccessor.getInstance(setElement.getAttribute("field"));
            this.fromField = FlexibleMapAccessor.getInstance(setElement.getAttribute("from-field"));
            this.valueExdr = FlexibleStringExpander.getInstance(setElement.getAttribute("value"));
            this.defaultExdr = FlexibleStringExpander.getInstance(setElement.getAttribute("default-value"));
            this.globalExdr = FlexibleStringExpander.getInstance(setElement.getAttribute("global"));
            this.type = setElement.getAttribute("type");
            this.toScope = setElement.getAttribute("to-scope");
            this.fromScope = setElement.getAttribute("from-scope");
            if (!this.fromField.isEmpty() && !this.valueExdr.isEmpty()) {
                throw new IllegalArgumentException("Cannot specify a from-field [" + setElement.getAttribute("from-field")
                        + "] and a value [" + setElement.getAttribute("value") + "] on the set action in a widget");
            }
        }

        @Override
        public void accept(ModelActionVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        public Object getInMemoryPersistedFromField(Object storeAgent, Map<String, Object> context) {
            Object newValue = null;
            String originalName = this.fromField.getOriginalName();
            List<String> currentWidgetTrail = UtilGenerics.toList(context.get("_WIDGETTRAIL_"));
            List<String> trailList = new ArrayList<String>();
            if (currentWidgetTrail != null) {
                trailList.addAll(currentWidgetTrail);
            }
            for (int i = trailList.size(); i >= 0; i--) {
                List<String> subTrail = trailList.subList(0, i);
                String newKey = null;
                if (subTrail.size() > 0)
                    newKey = StringUtil.join(subTrail, "|") + "|" + originalName;
                else
                    newKey = originalName;
                if (storeAgent instanceof ServletContext) {
                    newValue = ((ServletContext) storeAgent).getAttribute(newKey);
                } else if (storeAgent instanceof HttpSession) {
                    newValue = ((HttpSession) storeAgent).getAttribute(newKey);
                }
                if (newValue != null) {
                    break;
                }
            }
            return newValue;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public void runAction(Map<String, Object> context) {
            String globalStr = this.globalExdr.expandString(context);
            // default to false
            boolean global = "true".equals(globalStr);
            Object newValue = null;
            if (this.fromScope != null && this.fromScope.equals("user")) {
                if (!this.fromField.isEmpty()) {
                    HttpSession session = (HttpSession) context.get("session");
                    newValue = getInMemoryPersistedFromField(session, context);
                    if (Debug.verboseOn())
                        Debug.logVerbose("In user getting value for field from [" + this.fromField.getOriginalName() + "]: "
                                + newValue, module);
                } else if (!this.valueExdr.isEmpty()) {
                    newValue = this.valueExdr.expand(context);
                }
            } else if (this.fromScope != null && this.fromScope.equals("application")) {
                if (!this.fromField.isEmpty()) {
                    ServletContext servletContext = (ServletContext) context.get("application");
                    newValue = getInMemoryPersistedFromField(servletContext, context);
                    if (Debug.verboseOn())
                        Debug.logVerbose("In application getting value for field from [" + this.fromField.getOriginalName()
                                + "]: " + newValue, module);
                } else if (!this.valueExdr.isEmpty()) {
                    newValue = this.valueExdr.expandString(context);
                }
            } else {
                if (!this.fromField.isEmpty()) {
                    newValue = this.fromField.get(context);
                    if (Debug.verboseOn())
                        Debug.logVerbose("Getting value for field from [" + this.fromField.getOriginalName() + "]: " + newValue,
                                module);
                } else if (!this.valueExdr.isEmpty()) {
                    newValue = this.valueExdr.expand(context);
                }
            }
            // If newValue is still empty, use the default value
            if (ObjectType.isEmpty(newValue) && !this.defaultExdr.isEmpty()) {
                newValue = this.defaultExdr.expand(context);
            }
            if (UtilValidate.isNotEmpty(this.type)) {
                if ("NewMap".equals(this.type)) {
                    newValue = new HashMap();
                } else if ("NewList".equals(this.type)) {
                    newValue = new LinkedList();
                } else {
                    try {
                        newValue = ObjectType.simpleTypeConvert(newValue, this.type, null, (TimeZone) context.get("timeZone"),
                                (Locale) context.get("locale"), true);
                    } catch (GeneralException e) {
                        String errMsg = "Could not convert field value for the field: [" + this.field.getOriginalName()
                                + "] to the [" + this.type + "] type for the value [" + newValue + "]: " + e.toString();
                        Debug.logError(e, errMsg, module);
                        throw new IllegalArgumentException(errMsg);
                    }
                }
            }
            if (this.toScope != null && this.toScope.equals("user")) {
                String originalName = this.field.getOriginalName();
                List<String> currentWidgetTrail = UtilGenerics.toList(context.get("_WIDGETTRAIL_"));
                String newKey = "";
                if (currentWidgetTrail != null) {
                    newKey = StringUtil.join(currentWidgetTrail, "|");
                }
                if (UtilValidate.isNotEmpty(newKey)) {
                    newKey += "|";
                }
                newKey += originalName;
                HttpSession session = (HttpSession) context.get("session");
                session.setAttribute(newKey, newValue);
                if (Debug.verboseOn())
                    Debug.logVerbose("In user setting value for field from [" + this.field.getOriginalName() + "]: " + newValue,
                            module);
            } else if (this.toScope != null && this.toScope.equals("application")) {
                String originalName = this.field.getOriginalName();
                List<String> currentWidgetTrail = UtilGenerics.toList(context.get("_WIDGETTRAIL_"));
                String newKey = "";
                if (currentWidgetTrail != null) {
                    newKey = StringUtil.join(currentWidgetTrail, "|");
                }
                if (UtilValidate.isNotEmpty(newKey)) {
                    newKey += "|";
                }
                newKey += originalName;
                ServletContext servletContext = (ServletContext) context.get("application");
                servletContext.setAttribute(newKey, newValue);
                if (Debug.verboseOn())
                    Debug.logVerbose("In application setting value for field from [" + this.field.getOriginalName() + "]: "
                            + newValue, module);
            } else {
                // only do this if it is not global, if global ONLY put it in the global context
                if (!global) {
                    if (Debug.verboseOn())
                        Debug.logVerbose("Setting field [" + this.field.getOriginalName() + "] to value: " + newValue, module);
                    this.field.put(context, newValue);
                }
            }
            if (global) {
                Map<String, Object> globalCtx = UtilGenerics.checkMap(context.get("globalContext"));
                if (globalCtx != null) {
                    this.field.put(globalCtx, newValue);
                } else {
                    this.field.put(context, newValue);
                }
            }
        }

        public FlexibleStringExpander getDefaultExdr() {
            return defaultExdr;
        }

        public FlexibleMapAccessor<Object> getField() {
            return field;
        }

        public FlexibleMapAccessor<Object> getFromField() {
            return fromField;
        }

        public String getFromScope() {
            return fromScope;
        }

        public FlexibleStringExpander getGlobalExdr() {
            return globalExdr;
        }

        public String getToScope() {
            return toScope;
        }

        public String getType() {
            return type;
        }

        public FlexibleStringExpander getValueExdr() {
            return valueExdr;
        }
    }
}
