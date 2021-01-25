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
     * This model is intended to be a read-only data structure that represents
     * an XML element. Outside of object construction, the class should not
     * have any behaviors.
     * Instances of this class will be shared by multiple threads - therefore
     * it is immutable. DO NOT CHANGE THE OBJECT'S STATE AT RUN TIME!
     */

    private static final String MODULE = AbstractModelAction.class.getName();

    /**
     * Returns a new <code>ModelAction</code> instance, built from <code>actionElement</code>.
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
        List<ModelAction> actions = new ArrayList<>(actionElementList.size());
        for (Element actionElement : actionElementList) {
            actions.add(newInstance(modelWidget, actionElement));
        }
        return Collections.unmodifiableList(actions);
    }

    /**
     * Executes the actions contained in <code>actions</code>.
     * @param actions
     * @param context
     */
    public static void runSubActions(List<ModelAction> actions, Map<String, Object> context) {
        if (actions == null) {
            return;
        }
        for (ModelAction action : actions) {
            if (Debug.verboseOn()) {
                Debug.logVerbose("Running action " + action.getClass().getName(), MODULE);
            }
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
        if (Debug.verboseOn()) {
            Debug.logVerbose("Reading widget action with name: " + actionElement.getNodeName(), MODULE);
        }
    }

    /**
     * Returns the <code>ModelWidget</code> that contains the &lt;actions&gt; element.
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
            Debug.logWarning(e, "Exception thrown in XmlWidgetActionVisitor: ", MODULE);
        }
        return sb.toString();
    }

    /**
     * Models the &lt;entity-and&gt; element.
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

        /**
         * Gets finder.
         * @return the finder
         */
        public ByAndFinder getFinder() {
            return this.finder;
        }

        @Override
        public void runAction(Map<String, Object> context) {
            try {
                finder.runFind(context, WidgetWorker.getDelegator(context));
            } catch (GeneralException e) {
                String errMsg = "Error doing entity query by condition: " + e.toString();
                Debug.logError(e, errMsg, MODULE);
                throw new IllegalArgumentException(errMsg);
            }
        }
    }

    /**
     * Models the &lt;entity-condition&gt; element.
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

        /**
         * Gets finder.
         * @return the finder
         */
        public ByConditionFinder getFinder() {
            return this.finder;
        }

        @Override
        public void runAction(Map<String, Object> context) {
            try {
                finder.runFind(context, WidgetWorker.getDelegator(context));
            } catch (GeneralException e) {
                String errMsg = "Error doing entity query by condition: " + e.toString();
                Debug.logError(e, errMsg, MODULE);
                throw new IllegalArgumentException(errMsg);
            }
        }
    }

    /**
     * Models the &lt;entity-one&gt; element.
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

        /**
         * Gets finder.
         * @return the finder
         */
        public PrimaryKeyFinder getFinder() {
            return this.finder;
        }

        @Override
        public void runAction(Map<String, Object> context) {
            try {
                finder.runFind(context, WidgetWorker.getDelegator(context));
            } catch (GeneralException e) {
                String errMsg = "Error doing entity query by condition: " + e.toString();
                Debug.logError(e, errMsg, MODULE);
                throw new IllegalArgumentException(errMsg);
            }
        }
    }

    /**
     * Models the &lt;get-related&gt; element.
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

        /**
         * Gets relation name.
         * @return the relation name
         */
        public String getRelationName() {
            return this.relationName;
        }

        @Override
        public void runAction(Map<String, Object> context) {
            Object valueObject = valueNameAcsr.get(context);
            if (valueObject == null) {
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Value not found with name: " + valueNameAcsr + ", not getting related...", MODULE);
                }
                return;
            }
            if (!(valueObject instanceof GenericValue)) {
                String errMsg = "Env variable for value-name " + valueNameAcsr.toString()
                        + " is not a GenericValue object; for the relation-name: " + relationName + "]";
                Debug.logError(errMsg, MODULE);
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
                Debug.logError(e, errMsg, MODULE);
                throw new IllegalArgumentException(errMsg);
            }
        }

        /**
         * Gets list name acsr.
         * @return the list name acsr
         */
        public FlexibleMapAccessor<List<GenericValue>> getListNameAcsr() {
            return listNameAcsr;
        }

        /**
         * Gets map acsr.
         * @return the map acsr
         */
        public FlexibleMapAccessor<Map<String, Object>> getMapAcsr() {
            return mapAcsr;
        }

        /**
         * Gets order by list acsr.
         * @return the order by list acsr
         */
        public FlexibleMapAccessor<List<String>> getOrderByListAcsr() {
            return orderByListAcsr;
        }

        /**
         * Gets use cache.
         * @return the use cache
         */
        public boolean getUseCache() {
            return useCache;
        }

        /**
         * Gets value name acsr.
         * @return the value name acsr
         */
        public FlexibleMapAccessor<Object> getValueNameAcsr() {
            return valueNameAcsr;
        }
    }

    /**
     * Models the &lt;get-related-one&gt; element.
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

        /**
         * Gets relation name.
         * @return the relation name
         */
        public String getRelationName() {
            return this.relationName;
        }

        @Override
        public void runAction(Map<String, Object> context) {
            Object valueObject = valueNameAcsr.get(context);
            if (valueObject == null) {
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Value not found with name: " + valueNameAcsr + ", not getting related...", MODULE);
                }
                return;
            }
            if (!(valueObject instanceof GenericValue)) {
                String errMsg = "Env variable for value-name " + valueNameAcsr.toString()
                        + " is not a GenericValue object; for the relation-name: " + relationName + "]";
                Debug.logError(errMsg, MODULE);
                throw new IllegalArgumentException(errMsg);
            }
            GenericValue value = (GenericValue) valueObject;
            try {
                toValueNameAcsr.put(context, value.getRelatedOne(relationName, useCache));
            } catch (GenericEntityException e) {
                String errMsg = "Problem getting related one from entity with name " + value.getEntityName()
                        + " for the relation-name: " + relationName + ": " + e.getMessage();
                Debug.logError(e, errMsg, MODULE);
                throw new IllegalArgumentException(errMsg);
            }
        }

        /**
         * Gets to value name acsr.
         * @return the to value name acsr
         */
        public FlexibleMapAccessor<Object> getToValueNameAcsr() {
            return toValueNameAcsr;
        }

        /**
         * Gets use cache.
         * @return the use cache
         */
        public boolean getUseCache() {
            return useCache;
        }

        /**
         * Gets value name acsr.
         * @return the value name acsr
         */
        public FlexibleMapAccessor<Object> getValueNameAcsr() {
            return valueNameAcsr;
        }
    }

    /**
     * Models the &lt;property-map&gt; element.
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
                    Debug.logError(e, "Error adding resource bundle [" + resource + "]: " + e.toString(), MODULE);
                }
            }
            if (global) {
                Map<String, Object> globalCtx = UtilGenerics.cast(context.get("globalContext"));
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
                                Debug.logError(e, "Error adding resource bundle [" + resource + "]: " + e.toString(), MODULE);
                            }
                        }
                    }
                }
            }
        }

        /**
         * Gets global exdr.
         * @return the global exdr
         */
        public FlexibleStringExpander getGlobalExdr() {
            return globalExdr;
        }

        /**
         * Gets map name acsr.
         * @return the map name acsr
         */
        public FlexibleMapAccessor<ResourceBundleMapWrapper> getMapNameAcsr() {
            return mapNameAcsr;
        }

        /**
         * Gets resource exdr.
         * @return the resource exdr
         */
        public FlexibleStringExpander getResourceExdr() {
            return resourceExdr;
        }
    }

    /**
     * Models the &lt;property-to-field&gt; element.
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

        /**
         * Gets arg list acsr.
         * @return the arg list acsr
         */
        public FlexibleMapAccessor<List<? extends Object>> getArgListAcsr() {
            return argListAcsr;
        }

        /**
         * Gets default exdr.
         * @return the default exdr
         */
        public FlexibleStringExpander getDefaultExdr() {
            return defaultExdr;
        }

        /**
         * Gets field acsr.
         * @return the field acsr
         */
        public FlexibleMapAccessor<Object> getFieldAcsr() {
            return fieldAcsr;
        }

        /**
         * Gets global exdr.
         * @return the global exdr
         */
        public FlexibleStringExpander getGlobalExdr() {
            return globalExdr;
        }

        /**
         * Gets no locale.
         * @return the no locale
         */
        public boolean getNoLocale() {
            return noLocale;
        }

        /**
         * Gets property exdr.
         * @return the property exdr
         */
        public FlexibleStringExpander getPropertyExdr() {
            return propertyExdr;
        }

        /**
         * Gets resource exdr.
         * @return the resource exdr
         */
        public FlexibleStringExpander getResourceExdr() {
            return resourceExdr;
        }
    }

    /**
     * Models the &lt;script&gt; element.
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
                Map<String, Object> localContext = new HashMap<>();
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

        /**
         * Gets location.
         * @return the location
         */
        public String getLocation() {
            return location;
        }

        /**
         * Gets method.
         * @return the method
         */
        public String getMethod() {
            return method;
        }
    }

    /**
     * Models the &lt;service&gt; element.
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

        /**
         * Gets service name exdr.
         * @return the service name exdr
         */
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
                    Map<String, Object> combinedMap = new HashMap<>();
                    Object obj = context.get("parameters");
                    Map<String, Object> parametersObj = (obj instanceof Map) ? UtilGenerics.cast(obj) : null;
                    if (parametersObj != null) {
                        combinedMap.putAll(parametersObj);
                    }
                    combinedMap.putAll(context);
                    serviceContext = dc.makeValidContext(serviceNameExpanded, ModelService.IN_PARAM, combinedMap);
                } else if (UtilValidate.isNotEmpty(autoFieldMapString) && !"false".equals(autoFieldMapString)) {
                    FlexibleMapAccessor<Object> fieldFma = FlexibleMapAccessor.getInstance(autoFieldMapString);
                    Object obj = fieldFma.get(context);
                    Map<String, Object> autoFieldMap = (obj instanceof Map) ? UtilGenerics.cast(obj) : null;
                    if (autoFieldMap != null) {
                        serviceContext = WidgetWorker.getDispatcher(context).getDispatchContext()
                                .makeValidContext(serviceNameExpanded, ModelService.IN_PARAM, autoFieldMap);
                    }
                }
                if (serviceContext == null) {
                    serviceContext = new HashMap<>();
                }
                if (this.fieldMap != null) {
                    EntityFinderUtil.expandFieldMapToContext(this.fieldMap, context, serviceContext);
                }
                Map<String, Object> result = WidgetWorker.getDispatcher(context).runSync(serviceNameExpanded, serviceContext);
                ModelActionUtil.contextPutQueryStringOrAllResult(context, result, this.resultMapNameAcsr);
            } catch (GenericServiceException e) {
                String errMsg = "Error calling service with name " + serviceNameExpanded + ": " + e.toString();
                Debug.logError(e, errMsg, MODULE);
                throw new IllegalArgumentException(errMsg);
            }
        }

        /**
         * Gets auto field map exdr.
         * @return the auto field map exdr
         */
        public FlexibleStringExpander getAutoFieldMapExdr() {
            return autoFieldMapExdr;
        }

        /**
         * Gets field map.
         * @return the field map
         */
        public Map<FlexibleMapAccessor<Object>, Object> getFieldMap() {
            return fieldMap;
        }

        /**
         * Gets result map name acsr.
         * @return the result map name acsr
         */
        public FlexibleMapAccessor<Map<String, Object>> getResultMapNameAcsr() {
            return resultMapNameAcsr;
        }
    }

    /**
     * Models the &lt;set&gt; element.
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
        private final boolean setIfNull;
        private final boolean setIfEmpty;

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
            this.setIfNull = !"false".equals(setElement.getAttribute("set-if-null")); //default to true
            this.setIfEmpty = !"false".equals(setElement.getAttribute("set-if-empty")); //default to true
            if (!this.fromField.isEmpty() && !this.valueExdr.isEmpty()) {
                throw new IllegalArgumentException("Cannot specify a from-field [" + setElement.getAttribute("from-field")
                        + "] and a value [" + setElement.getAttribute("value") + "] on the set action in a widget");
            }
        }

        @Override
        public void accept(ModelActionVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        /**
         * Gets in memory persisted from field.
         * @param storeAgent the store agent
         * @param context the context
         * @return the in memory persisted from field
         */
        public Object getInMemoryPersistedFromField(Object storeAgent, Map<String, Object> context) {
            Object newValue = null;
            String originalName = this.fromField.getOriginalName();
            Object obj = context.get("_WIDGETTRAIL_");
            List<String> currentWidgetTrail = (obj instanceof List) ? UtilGenerics.cast(obj) : null;
            List<String> trailList = new ArrayList<>();
            if (currentWidgetTrail != null) {
                trailList.addAll(currentWidgetTrail);
            }
            for (int i = trailList.size(); i >= 0; i--) {
                List<String> subTrail = trailList.subList(0, i);
                String newKey = null;
                if (!subTrail.isEmpty()) {
                    newKey = StringUtil.join(subTrail, "|") + "|" + originalName;
                } else {
                    newKey = originalName;
                }
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
            if (this.fromScope != null && "user".equals(this.fromScope)) {
                if (!this.fromField.isEmpty()) {
                    HttpSession session = (HttpSession) context.get("session");
                    newValue = getInMemoryPersistedFromField(session, context);
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("In user getting value for field from [" + this.fromField.getOriginalName() + "]: " + newValue, MODULE);
                    }
                } else if (!this.valueExdr.isEmpty()) {
                    newValue = this.valueExdr.expand(context);
                }
            } else if (this.fromScope != null && "application".equals(this.fromScope)) {
                if (!this.fromField.isEmpty()) {
                    ServletContext servletContext = (ServletContext) context.get("application");
                    newValue = getInMemoryPersistedFromField(servletContext, context);
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("In application getting value for field from [" + this.fromField.getOriginalName() + "]: " + newValue,
                                MODULE);
                    }
                } else if (!this.valueExdr.isEmpty()) {
                    newValue = this.valueExdr.expandString(context);
                }
            } else {
                if (!this.fromField.isEmpty()) {
                    newValue = this.fromField.get(context);
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("Getting value for field from [" + this.fromField.getOriginalName() + "]: " + newValue, MODULE);
                    }
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
                        newValue = ObjectType.simpleTypeOrObjectConvert(newValue, this.type, null, (TimeZone) context.get("timeZone"),
                                (Locale) context.get("locale"), true);
                    } catch (GeneralException e) {
                        String errMsg = "Could not convert field value for the field: [" + this.field.getOriginalName()
                                + "] to the [" + this.type + "] type for the value [" + newValue + "]: " + e.toString();
                        Debug.logError(e, errMsg, MODULE);
                        throw new IllegalArgumentException(errMsg);
                    }
                }
            }
            if (!setIfNull && newValue == null) {
                if (Debug.warningOn()) {
                    Debug.logWarning("Field value not found (null) for the field: [" + this.field.getOriginalName()
                            + " and there was no default value, so field was not set", MODULE);
                }
                return;
            }
            if (!setIfEmpty && ObjectType.isEmpty(newValue)) {
                if (Debug.warningOn()) {
                    Debug.logWarning("Field value not found (empty) for the field: [" + this.field.getOriginalName()
                            + " and there was no default value, so field was not set", MODULE);
                }
                return;
            }
            if (this.toScope != null && "user".equals(this.toScope)) {
                String originalName = this.field.getOriginalName();
                Object obj = context.get("_WIDGETTRAIL_");
                List<String> currentWidgetTrail = (obj instanceof List) ? UtilGenerics.cast(obj) : null;
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
                if (Debug.verboseOn()) {
                    Debug.logVerbose("In user setting value for field from [" + this.field.getOriginalName() + "]: " + newValue, MODULE);
                }
            } else if (this.toScope != null && "application".equals(this.toScope)) {
                String originalName = this.field.getOriginalName();
                Object obj = context.get("_WIDGETTRAIL_");
                List<String> currentWidgetTrail = (obj instanceof List) ? UtilGenerics.cast(obj) : null;
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
                if (Debug.verboseOn()) {
                    Debug.logVerbose("In application setting value for field from [" + this.field.getOriginalName() + "]: " + newValue, MODULE);
                }
            } else {
                // only do this if it is not global, if global ONLY put it in the global context
                if (!global) {
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("Setting field [" + this.field.getOriginalName() + "] to value: " + newValue, MODULE);
                    }
                    this.field.put(context, newValue);
                }
            }
            if (global) {
                Map<String, Object> globalCtx = UtilGenerics.cast(context.get("globalContext"));
                if (globalCtx != null) {
                    this.field.put(globalCtx, newValue);
                } else {
                    this.field.put(context, newValue);
                }
            }
        }

        /**
         * Gets default exdr.
         * @return the default exdr
         */
        public FlexibleStringExpander getDefaultExdr() {
            return defaultExdr;
        }

        /**
         * Gets field.
         * @return the field
         */
        public FlexibleMapAccessor<Object> getField() {
            return field;
        }

        /**
         * Gets from field.
         * @return the from field
         */
        public FlexibleMapAccessor<Object> getFromField() {
            return fromField;
        }

        /**
         * Gets from scope.
         * @return the from scope
         */
        public String getFromScope() {
            return fromScope;
        }

        /**
         * Gets global exdr.
         * @return the global exdr
         */
        public FlexibleStringExpander getGlobalExdr() {
            return globalExdr;
        }

        /**
         * Gets to scope.
         * @return the to scope
         */
        public String getToScope() {
            return toScope;
        }

        /**
         * Gets type.
         * @return the type
         */
        public String getType() {
            return type;
        }

        /**
         * Gets value exdr.
         * @return the value exdr
         */
        public FlexibleStringExpander getValueExdr() {
            return valueExdr;
        }
    }
}
