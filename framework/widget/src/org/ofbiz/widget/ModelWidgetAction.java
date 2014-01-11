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
package org.ofbiz.widget;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.PatternSyntaxException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.w3c.dom.Element;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.ScriptUtil;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.finder.ByAndFinder;
import org.ofbiz.entity.finder.ByConditionFinder;
import org.ofbiz.entity.finder.EntityFinderUtil;
import org.ofbiz.entity.finder.PrimaryKeyFinder;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelService;

@SuppressWarnings("serial")
public abstract class ModelWidgetAction implements Serializable {
    public static final String module = ModelWidgetAction.class.getName();

    protected ModelWidget modelWidget;

    protected ModelWidgetAction() {}

    public ModelWidgetAction(ModelWidget modelWidget, Element actionElement) {
        this.modelWidget = modelWidget;
        if (Debug.verboseOn()) Debug.logVerbose("Reading widget action with name: " + actionElement.getNodeName(), module);
    }

    public abstract void runAction(Map<String, Object> context) throws GeneralException;

    public static List<ModelWidgetAction> readSubActions(ModelWidget modelWidget, Element parentElement) {
        List<? extends Element> actionElementList = UtilXml.childElementList(parentElement);
        List<ModelWidgetAction> actions = new ArrayList<ModelWidgetAction>(actionElementList.size());
        for (Element actionElement: actionElementList) {
            if ("set".equals(actionElement.getNodeName())) {
                actions.add(new SetField(modelWidget, actionElement));
            } else if ("property-map".equals(actionElement.getNodeName())) {
                actions.add(new PropertyMap(modelWidget, actionElement));
            } else if ("property-to-field".equals(actionElement.getNodeName())) {
                actions.add(new PropertyToField(modelWidget, actionElement));
            } else if ("script".equals(actionElement.getNodeName())) {
                actions.add(new Script(modelWidget, actionElement));
            } else if ("service".equals(actionElement.getNodeName())) {
                actions.add(new Service(modelWidget, actionElement));
            } else if ("entity-one".equals(actionElement.getNodeName())) {
                actions.add(new EntityOne(modelWidget, actionElement));
            } else if ("entity-and".equals(actionElement.getNodeName())) {
                actions.add(new EntityAnd(modelWidget, actionElement));
            } else if ("entity-condition".equals(actionElement.getNodeName())) {
                actions.add(new EntityCondition(modelWidget, actionElement));
            } else if ("get-related-one".equals(actionElement.getNodeName())) {
                actions.add(new GetRelatedOne(modelWidget, actionElement));
            } else if ("get-related".equals(actionElement.getNodeName())) {
                actions.add(new GetRelated(modelWidget, actionElement));
            } else {
                throw new IllegalArgumentException("Action element not supported with name: " + actionElement.getNodeName());
            }
        }
        return actions;
    }

    public static void runSubActions(List<ModelWidgetAction> actions, Map<String, Object> context) throws GeneralException {
        if (actions == null) return;

        for (ModelWidgetAction action: actions) {
            if (Debug.verboseOn()) Debug.logVerbose("Running widget action " + action.getClass().getName(), module);
            action.runAction(context);
        }
    }

    public static class SetField extends ModelWidgetAction {
        protected FlexibleMapAccessor<Object> field;
        protected FlexibleMapAccessor<Object> fromField;
        protected FlexibleStringExpander valueExdr;
        protected FlexibleStringExpander defaultExdr;
        protected FlexibleStringExpander globalExdr;
        protected String type;
        protected String toScope;
        protected String fromScope;

        public SetField(ModelWidget modelWidget, Element setElement) {
            super (modelWidget, setElement);
            this.field = FlexibleMapAccessor.getInstance(setElement.getAttribute("field"));
            this.fromField = FlexibleMapAccessor.getInstance(setElement.getAttribute("from-field"));
            this.valueExdr = FlexibleStringExpander.getInstance(setElement.getAttribute("value"));
            this.defaultExdr = FlexibleStringExpander.getInstance(setElement.getAttribute("default-value"));
            this.globalExdr = FlexibleStringExpander.getInstance(setElement.getAttribute("global"));
            this.type = setElement.getAttribute("type");
            this.toScope = setElement.getAttribute("to-scope");
            this.fromScope = setElement.getAttribute("from-scope");
            if (!this.fromField.isEmpty() && !this.valueExdr.isEmpty()) {
                throw new IllegalArgumentException("Cannot specify a from-field [" + setElement.getAttribute("from-field") + "] and a value [" + setElement.getAttribute("value") + "] on the set action in a widget");
            }
        }

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
                    if (Debug.verboseOn()) Debug.logVerbose("In user getting value for field from [" + this.fromField.getOriginalName() + "]: " + newValue, module);
                } else if (!this.valueExdr.isEmpty()) {
                    newValue = this.valueExdr.expand(context);
                }
            } else if (this.fromScope != null && this.fromScope.equals("application")) {
                if (!this.fromField.isEmpty()) {
                    ServletContext servletContext = (ServletContext) context.get("application");
                    newValue = getInMemoryPersistedFromField(servletContext, context);
                    if (Debug.verboseOn()) Debug.logVerbose("In application getting value for field from [" + this.fromField.getOriginalName() + "]: " + newValue, module);
                } else if (!this.valueExdr.isEmpty()) {
                    newValue = this.valueExdr.expandString(context);
                }
            } else {
                if (!this.fromField.isEmpty()) {
                    newValue = this.fromField.get(context);
                    if (Debug.verboseOn()) Debug.logVerbose("Getting value for field from [" + this.fromField.getOriginalName() + "]: " + newValue, module);
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
                    newValue = FastMap.newInstance();
                } else if ("NewList".equals(this.type)) {
                    newValue = FastList.newInstance();
                } else {
                    try {
                        newValue = ObjectType.simpleTypeConvert(newValue, this.type, null, (TimeZone) context.get("timeZone"), (Locale) context.get("locale"), true);
                    } catch (GeneralException e) {
                        String errMsg = "Could not convert field value for the field: [" + this.field.getOriginalName() + "] to the [" + this.type + "] type for the value [" + newValue + "]: " + e.toString();
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
                HttpSession session = (HttpSession)context.get("session");
                session.setAttribute(newKey, newValue);
                if (Debug.verboseOn()) Debug.logVerbose("In user setting value for field from [" + this.field.getOriginalName() + "]: " + newValue, module);
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
                ServletContext servletContext = (ServletContext)context.get("application");
                servletContext.setAttribute(newKey, newValue);
                if (Debug.verboseOn()) Debug.logVerbose("In application setting value for field from [" + this.field.getOriginalName() + "]: " + newValue, module);
            } else {
                // only do this if it is not global, if global ONLY put it in the global context
                if (!global) {
                    if (Debug.verboseOn()) Debug.logVerbose("Setting field [" + this.field.getOriginalName() + "] to value: " + newValue, module);
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

            // this is a hack for backward compatibility with the JPublish page object
            Map<String, Object> page = UtilGenerics.checkMap(context.get("page"));
            if (page != null) {
                this.field.put(page, newValue);
            }
        }

        public Object getInMemoryPersistedFromField(Object storeAgent, Map<String, Object> context) {
            Object newValue = null;
            String originalName = this.fromField.getOriginalName();
            List<String> currentWidgetTrail = UtilGenerics.toList(context.get("_WIDGETTRAIL_"));
            List<String> trailList = new ArrayList<String>();
            if (currentWidgetTrail != null) {
                trailList.addAll(currentWidgetTrail);
            }

            for (int i=trailList.size(); i >= 0; i--) {
                List<String> subTrail = trailList.subList(0,i);
                String newKey = null;
                if (subTrail.size() > 0)
                    newKey = StringUtil.join(subTrail, "|") + "|" + originalName;
                else
                    newKey = originalName;

                if (storeAgent instanceof ServletContext) {
                    newValue = ((ServletContext)storeAgent).getAttribute(newKey);
                } else if (storeAgent instanceof HttpSession) {
                    newValue = ((HttpSession)storeAgent).getAttribute(newKey);
                }
                if (newValue != null) {
                    break;
                }
            }
            return newValue;
        }
    }

    public static class PropertyMap extends ModelWidgetAction {
        protected FlexibleStringExpander resourceExdr;
        protected FlexibleMapAccessor<ResourceBundleMapWrapper> mapNameAcsr;
        protected FlexibleStringExpander globalExdr;

        public PropertyMap(ModelWidget modelWidget, Element setElement) {
            super (modelWidget, setElement);
            this.resourceExdr = FlexibleStringExpander.getInstance(setElement.getAttribute("resource"));
            this.mapNameAcsr = FlexibleMapAccessor.getInstance(setElement.getAttribute("map-name"));
            this.globalExdr = FlexibleStringExpander.getInstance(setElement.getAttribute("global"));
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
    }

    public static class PropertyToField extends ModelWidgetAction {

        protected FlexibleStringExpander resourceExdr;
        protected FlexibleStringExpander propertyExdr;
        protected FlexibleMapAccessor<Object> fieldAcsr;
        protected FlexibleStringExpander defaultExdr;
        protected boolean noLocale;
        protected FlexibleMapAccessor<List<? extends Object>> argListAcsr;
        protected FlexibleStringExpander globalExdr;

        public PropertyToField(ModelWidget modelWidget, Element setElement) {
            super (modelWidget, setElement);
            this.resourceExdr = FlexibleStringExpander.getInstance(setElement.getAttribute("resource"));
            this.propertyExdr = FlexibleStringExpander.getInstance(setElement.getAttribute("property"));
            this.fieldAcsr = FlexibleMapAccessor.getInstance(setElement.getAttribute("field"));
            this.defaultExdr = FlexibleStringExpander.getInstance(setElement.getAttribute("default"));
            this.noLocale = "true".equals(setElement.getAttribute("no-locale"));
            this.argListAcsr = FlexibleMapAccessor.getInstance(setElement.getAttribute("arg-list-name"));
            this.globalExdr = FlexibleStringExpander.getInstance(setElement.getAttribute("global"));
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
    }

    public static class Script extends ModelWidgetAction {
        protected String location;
        protected String method;

        public Script(ModelWidget modelWidget, Element scriptElement) {
            super (modelWidget, scriptElement);
            String scriptLocation = scriptElement.getAttribute("location");
            this.location = WidgetWorker.getScriptLocation(scriptLocation);
            this.method = WidgetWorker.getScriptMethodName(scriptLocation);
        }

        @Override
        public void runAction(Map<String, Object> context) throws GeneralException {
            if (location.endsWith(".xml")) {
                Map<String, Object> localContext = FastMap.newInstance();
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
    }

    public static class Service extends ModelWidgetAction {
        protected FlexibleStringExpander serviceNameExdr;
        protected FlexibleMapAccessor<Map<String, Object>> resultMapNameAcsr;
        protected FlexibleStringExpander autoFieldMapExdr;
        protected Map<FlexibleMapAccessor<Object>, Object> fieldMap;

        public Service(ModelWidget modelWidget, Element serviceElement) {
            super (modelWidget, serviceElement);
            this.serviceNameExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("service-name"));
            this.resultMapNameAcsr = FlexibleMapAccessor.getInstance(serviceElement.getAttribute("result-map"));
            if (this.resultMapNameAcsr.isEmpty()) this.resultMapNameAcsr = FlexibleMapAccessor.getInstance(serviceElement.getAttribute("result-map-name"));
            this.autoFieldMapExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("auto-field-map"));
            this.fieldMap = EntityFinderUtil.makeFieldMap(serviceElement);
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
                    Map<String, Object> combinedMap = FastMap.newInstance();
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
                        serviceContext = WidgetWorker.getDispatcher(context).getDispatchContext().makeValidContext(serviceNameExpanded, ModelService.IN_PARAM, autoFieldMap);
                    }
                }
                if (serviceContext == null) {
                    serviceContext = FastMap.newInstance();
                }

                if (this.fieldMap != null) {
                    EntityFinderUtil.expandFieldMapToContext(this.fieldMap, context, serviceContext);
                }

                Map<String, Object> result = WidgetWorker.getDispatcher(context).runSync(serviceNameExpanded, serviceContext);

                if (!this.resultMapNameAcsr.isEmpty()) {
                    this.resultMapNameAcsr.put(context, result);
                    String queryString = (String)result.get("queryString");
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

        public FlexibleStringExpander getServiceNameExdr() {
            return this.serviceNameExdr;
        }
    }

    public static class EntityOne extends ModelWidgetAction {
        protected PrimaryKeyFinder finder;

        public EntityOne(ModelWidget modelWidget, Element entityOneElement) {
            super (modelWidget, entityOneElement);
            finder = new PrimaryKeyFinder(entityOneElement);
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

        public PrimaryKeyFinder getFinder() {
            return this.finder;
        }
    }

    public static class EntityAnd extends ModelWidgetAction {
        protected ByAndFinder finder;

        public EntityAnd(ModelWidget modelWidget, Element entityAndElement) {
            super (modelWidget, entityAndElement);
            finder = new ByAndFinder(entityAndElement);
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

        public ByAndFinder getFinder() {
            return this.finder;
        }
    }

    public static class EntityCondition extends ModelWidgetAction {
        ByConditionFinder finder;

        public EntityCondition(ModelWidget modelWidget, Element entityConditionElement) {
            super (modelWidget, entityConditionElement);
            finder = new ByConditionFinder(entityConditionElement);
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

        public ByConditionFinder getFinder() {
            return this.finder;
        }
    }

    public static class GetRelatedOne extends ModelWidgetAction {
        protected FlexibleMapAccessor<Object> valueNameAcsr;
        protected FlexibleMapAccessor<Object> toValueNameAcsr;
        protected String relationName;
        protected boolean useCache;

        public GetRelatedOne(ModelWidget modelWidget, Element getRelatedOneElement) {
            super (modelWidget, getRelatedOneElement);
            this.valueNameAcsr = FlexibleMapAccessor.getInstance(getRelatedOneElement.getAttribute("value-field"));
            if (this.valueNameAcsr.isEmpty()) this.valueNameAcsr = FlexibleMapAccessor.getInstance(getRelatedOneElement.getAttribute("value-name"));
            this.toValueNameAcsr = FlexibleMapAccessor.getInstance(getRelatedOneElement.getAttribute("to-value-field"));
            if (this.toValueNameAcsr.isEmpty()) this.toValueNameAcsr = FlexibleMapAccessor.getInstance(getRelatedOneElement.getAttribute("to-value-name"));
            this.relationName = getRelatedOneElement.getAttribute("relation-name");
            this.useCache = "true".equals(getRelatedOneElement.getAttribute("use-cache"));
        }

        @Override
        public void runAction(Map<String, Object> context) {
            Object valueObject = valueNameAcsr.get(context);
            if (valueObject == null) {
                Debug.logVerbose("Value not found with name: " + valueNameAcsr + ", not getting related...", module);
                return;
            }
            if (!(valueObject instanceof GenericValue)) {
                String errMsg = "Env variable for value-name " + valueNameAcsr.toString() + " is not a GenericValue object; for the relation-name: " + relationName + "]";
                Debug.logError(errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
            GenericValue value = (GenericValue) valueObject;
            try {
                toValueNameAcsr.put(context, value.getRelatedOne(relationName, useCache));
            } catch (GenericEntityException e) {
                String errMsg = "Problem getting related one from entity with name " + value.getEntityName() + " for the relation-name: " + relationName + ": " + e.getMessage();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
        }

        public String getRelationName() {
            return this.relationName;
        }
    }

    public static class GetRelated extends ModelWidgetAction {
        protected FlexibleMapAccessor<Object> valueNameAcsr;
        protected FlexibleMapAccessor<List<GenericValue>> listNameAcsr;
        protected FlexibleMapAccessor<Map<String, Object>> mapAcsr;
        protected FlexibleMapAccessor<List<String>> orderByListAcsr;
        protected String relationName;
        protected boolean useCache;

        public GetRelated(ModelWidget modelWidget, Element getRelatedElement) {
            super (modelWidget, getRelatedElement);
            this.valueNameAcsr = FlexibleMapAccessor.getInstance(getRelatedElement.getAttribute("value-field"));
            if (this.valueNameAcsr.isEmpty()) this.valueNameAcsr = FlexibleMapAccessor.getInstance(getRelatedElement.getAttribute("value-name"));
            this.listNameAcsr = FlexibleMapAccessor.getInstance(getRelatedElement.getAttribute("list"));
            if (this.listNameAcsr.isEmpty()) this.listNameAcsr = FlexibleMapAccessor.getInstance(getRelatedElement.getAttribute("list-name"));
            this.relationName = getRelatedElement.getAttribute("relation-name");
            this.mapAcsr = FlexibleMapAccessor.getInstance(getRelatedElement.getAttribute("map"));
            if (this.mapAcsr.isEmpty()) this.mapAcsr = FlexibleMapAccessor.getInstance(getRelatedElement.getAttribute("map-name"));
            this.orderByListAcsr = FlexibleMapAccessor.getInstance(getRelatedElement.getAttribute("order-by-list"));
            if (this.orderByListAcsr.isEmpty()) this.orderByListAcsr = FlexibleMapAccessor.getInstance(getRelatedElement.getAttribute("order-by-list-name"));
            this.useCache = "true".equals(getRelatedElement.getAttribute("use-cache"));
        }

        @Override
        public void runAction(Map<String, Object> context) {
            Object valueObject = valueNameAcsr.get(context);
            if (valueObject == null) {
                Debug.logVerbose("Value not found with name: " + valueNameAcsr + ", not getting related...", module);
                return;
            }
            if (!(valueObject instanceof GenericValue)) {
                String errMsg = "Env variable for value-name " + valueNameAcsr.toString() + " is not a GenericValue object; for the relation-name: " + relationName + "]";
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
                String errMsg = "Problem getting related from entity with name " + value.getEntityName() + " for the relation-name: " + relationName + ": " + e.getMessage();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
        }

        public String getRelationName() {
            return this.relationName;
        }
    }
}
