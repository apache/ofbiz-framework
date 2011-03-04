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
package org.ofbiz.widget.form;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.PatternSyntaxException;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.codehaus.groovy.runtime.InvokerHelper;
import org.ofbiz.base.util.BshUtil;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.GroovyUtil;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.finder.ByAndFinder;
import org.ofbiz.entity.finder.ByConditionFinder;
import org.ofbiz.entity.finder.EntityFinderUtil;
import org.ofbiz.entity.finder.PrimaryKeyFinder;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelService;
import org.ofbiz.widget.WidgetWorker;
import org.w3c.dom.Element;


/**
 * Widget Library - Screen model class
 */
public abstract class ModelFormAction {
    public static final String module = ModelFormAction.class.getName();

    protected ModelForm modelForm;

    public ModelFormAction(ModelForm modelForm, Element actionElement) {
        this.modelForm = modelForm;
        if (Debug.verboseOn()) Debug.logVerbose("Reading Screen action with name: " + actionElement.getNodeName(), module);
    }

    public abstract void runAction(Map<String, Object> context);

    public static List<ModelFormAction> readSubActions(ModelForm modelForm, Element parentElement) {
        List<ModelFormAction> actions = new LinkedList<ModelFormAction>();

        for (Element actionElement: UtilXml.childElementList(parentElement)) {
            if ("set".equals(actionElement.getNodeName())) {
                actions.add(new SetField(modelForm, actionElement));
            } else if ("property-map".equals(actionElement.getNodeName())) {
                actions.add(new PropertyMap(modelForm, actionElement));
            } else if ("property-to-field".equals(actionElement.getNodeName())) {
                actions.add(new PropertyToField(modelForm, actionElement));
            } else if ("script".equals(actionElement.getNodeName())) {
                actions.add(new Script(modelForm, actionElement));
            } else if ("service".equals(actionElement.getNodeName())) {
                actions.add(new Service(modelForm, actionElement));
            } else if ("entity-one".equals(actionElement.getNodeName())) {
                actions.add(new EntityOne(modelForm, actionElement));
            } else if ("entity-and".equals(actionElement.getNodeName())) {
                actions.add(new EntityAnd(modelForm, actionElement));
            } else if ("entity-condition".equals(actionElement.getNodeName())) {
                actions.add(new EntityCondition(modelForm, actionElement));
            } else {
                throw new IllegalArgumentException("Action element not supported with name: " + actionElement.getNodeName());
            }
        }

        return actions;
    }

    public static void runSubActions(List<ModelFormAction> actions, Map<String, Object> context) {
        if (actions == null) return;

        for (ModelFormAction action: actions) {
            if (Debug.verboseOn()) Debug.logVerbose("Running screen action " + action.getClass().getName(), module);
            action.runAction(context);
        }
    }

    public static class SetField extends ModelFormAction {
        protected FlexibleMapAccessor<Object> field;
        protected FlexibleMapAccessor<String> fromField;
        protected FlexibleStringExpander valueExdr;
        protected FlexibleStringExpander defaultExdr;
        protected FlexibleStringExpander globalExdr;
        protected String type;

        public SetField(ModelForm modelForm, Element setElement) {
            super (modelForm, setElement);
            this.field = FlexibleMapAccessor.getInstance(setElement.getAttribute("field"));
            this.fromField = FlexibleMapAccessor.getInstance(setElement.getAttribute("from-field"));
            this.valueExdr = FlexibleStringExpander.getInstance(setElement.getAttribute("value"));
            this.defaultExdr = FlexibleStringExpander.getInstance(setElement.getAttribute("default-value"));
            this.globalExdr = FlexibleStringExpander.getInstance(setElement.getAttribute("global"));
            this.type = setElement.getAttribute("type");
            if (!this.fromField.isEmpty() && !this.valueExdr.isEmpty()) {
                throw new IllegalArgumentException("Cannot specify a from-field [" + setElement.getAttribute("from-field") + "] and a value [" + setElement.getAttribute("value") + "] on the set action in a screen widget");
            }
        }

        @Override
        public void runAction(Map<String, Object> context) {
            String globalStr = this.globalExdr.expandString(context);
            // default to false
            boolean global = "true".equals(globalStr);

            Object newValue = null;
            if (!this.fromField.isEmpty()) {
                newValue = this.fromField.get(context);
                if (Debug.verboseOn()) Debug.logVerbose("In screen getting value for field from [" + this.fromField.getOriginalName() + "]: " + newValue, module);
            } else if (!this.valueExdr.isEmpty()) {
                newValue = this.valueExdr.expand(context);
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
            if (Debug.verboseOn()) Debug.logVerbose("In screen setting field [" + this.field.getOriginalName() + "] to value: " + newValue, module);
            this.field.put(context, newValue);

            if (global) {
                Map<String, Object> globalCtx = UtilGenerics.checkMap(context.get("globalContext"));
                if (globalCtx != null) {
                    this.field.put(globalCtx, newValue);
                }
            }

            // this is a hack for backward compatibility with the JPublish page object
            Map<String, Object> page = UtilGenerics.checkMap(context.get("page"));
            if (page != null) {
                this.field.put(page, newValue);
            }
        }
    }

    public static class PropertyMap extends ModelFormAction {
        protected FlexibleStringExpander resourceExdr;
        protected FlexibleMapAccessor<Map<String, Object>> mapNameAcsr;
        protected FlexibleStringExpander globalExdr;

        public PropertyMap(ModelForm modelForm, Element setElement) {
            super (modelForm, setElement);
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
            Map<String, Object> propertyMap = UtilProperties.getResourceBundleMap(resource, locale);
            this.mapNameAcsr.put(context, propertyMap);

            if (global) {
                Map<String, Object> globalCtx = UtilGenerics.checkMap(context.get("globalContext"));
                if (globalCtx != null) {
                    this.mapNameAcsr.put(globalCtx, propertyMap);
                }
            }
        }
    }

    public static class PropertyToField extends ModelFormAction {

        protected FlexibleStringExpander resourceExdr;
        protected FlexibleStringExpander propertyExdr;
        protected FlexibleMapAccessor<String> fieldAcsr;
        protected FlexibleStringExpander defaultExdr;
        protected boolean noLocale;
        protected FlexibleMapAccessor<List<Object>> argListAcsr;
        protected FlexibleStringExpander globalExdr;

        public PropertyToField(ModelForm modelForm, Element setElement) {
            super (modelForm, setElement);
            this.resourceExdr = FlexibleStringExpander.getInstance(setElement.getAttribute("resource"));
            this.propertyExdr = FlexibleStringExpander.getInstance(setElement.getAttribute("property"));
            this.fieldAcsr = FlexibleMapAccessor.getInstance(setElement.getAttribute("field"));
            this.defaultExdr = FlexibleStringExpander.getInstance(setElement.getAttribute("default"));
            noLocale = "true".equals(setElement.getAttribute("no-locale"));
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
                value = UtilProperties.getPropertyValue(resource, property);
            } else {
                value = UtilProperties.getMessage(resource, property, locale);
            }
            if (UtilValidate.isEmpty(value)) {
                value = this.defaultExdr.expandString(context);
            }

            // note that expanding the value string here will handle defaultValue and the string from
            //  the properties file; if we decide later that we don't want the string from the properties
            //  file to be expanded we should just expand the defaultValue at the beginning of this method.
            value = FlexibleStringExpander.expandString(value, context);

            if (!argListAcsr.isEmpty()) {
                List<Object> argList = argListAcsr.get(context);
                if (UtilValidate.isNotEmpty(argList)) {
                    value = MessageFormat.format(value, argList.toArray());
                }
            }

            fieldAcsr.put(context, value);
        }
    }

    public static class Script extends ModelFormAction {
        protected static final Object[] EMPTY_ARGS = {};
        protected String location;
        protected String method;

        public Script(ModelForm modelForm, Element scriptElement) {
            super (modelForm, scriptElement);
            String scriptLocation = scriptElement.getAttribute("location");
            this.location = WidgetWorker.getScriptLocation(scriptLocation);
            this.method = WidgetWorker.getScriptMethodName(scriptLocation);
        }

        @Override
        public void runAction(Map<String, Object> context) {
            if (location.endsWith(".bsh")) {
                try {
                    BshUtil.runBshAtLocation(location, context);
                } catch (GeneralException e) {
                    String errMsg = "Error running BSH script at location [" + location + "]: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    throw new IllegalArgumentException(errMsg);
                }
            } else if (location.endsWith(".groovy")) {
                try {
                    groovy.lang.Script script = InvokerHelper.createScript(GroovyUtil.getScriptClassFromLocation(location), GroovyUtil.getBinding(context));
                    if (UtilValidate.isEmpty(method)) {
                        script.run();
                    } else {
                        script.invokeMethod(method, EMPTY_ARGS);
                    }
                } catch (GeneralException e) {
                    String errMsg = "Error running Groovy script at location [" + location + "]: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    throw new IllegalArgumentException(errMsg);
                }
            } else if (location.endsWith(".xml")) {
                Map<String, Object> localContext = FastMap.newInstance();
                localContext.putAll(context);
                DispatchContext ctx = this.modelForm.dispatchContext;
                MethodContext methodContext = new MethodContext(ctx, localContext, null);
                try {
                    SimpleMethod.runSimpleMethod(location, method, methodContext);
                    context.putAll(methodContext.getResults());
                } catch (MiniLangException e) {
                    throw new IllegalArgumentException("Error running simple method at location [" + location + "]", e);
                }
            } else {
                throw new IllegalArgumentException("For screen script actions the script type is not yet support for location:" + location);
            }
        }
    }

    public static class Service extends ModelFormAction {
        protected FlexibleStringExpander serviceNameExdr;
        protected FlexibleMapAccessor<Map<String, Object>> resultMapNameAcsr;
        protected FlexibleStringExpander autoFieldMapExdr;
        protected FlexibleStringExpander resultMapListNameExdr;
        protected Map<FlexibleMapAccessor<Object>, Object> fieldMap;
        protected boolean ignoreError = false;

        public Service(ModelForm modelForm, Element serviceElement) {
            super (modelForm, serviceElement);
            this.serviceNameExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("service-name"));
            this.resultMapNameAcsr = FlexibleMapAccessor.getInstance(serviceElement.getAttribute("result-map"));
            if (this.resultMapNameAcsr.isEmpty()) this.resultMapNameAcsr = FlexibleMapAccessor.getInstance(serviceElement.getAttribute("result-map-name"));
            this.autoFieldMapExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("auto-field-map"));
            if (UtilValidate.isEmpty(serviceElement.getAttribute("result-map-list")) && UtilValidate.isEmpty(serviceElement.getAttribute("result-map-list-name"))) {
                if (UtilValidate.isEmpty(serviceElement.getAttribute("result-map-list-iterator")) && UtilValidate.isEmpty(serviceElement.getAttribute("result-map-list-iterator-name"))) {
                    String lstNm = modelForm.getListName();
                    if (UtilValidate.isEmpty(lstNm)) {
                        lstNm = ModelForm.DEFAULT_FORM_RESULT_LIST_NAME;
                    }
                    this.resultMapListNameExdr = FlexibleStringExpander.getInstance(lstNm);
                } else {
                    // this is deprecated, but support it for now anyway
                    this.resultMapListNameExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("result-map-list-iterator"));
                    if (this.resultMapListNameExdr.isEmpty()) this.resultMapListNameExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("result-map-list-iterator-name"));
                }
            } else {
                this.resultMapListNameExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("result-map-list"));
                if (this.resultMapListNameExdr.isEmpty()) this.resultMapListNameExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("result-map-list-name"));
            }

            this.fieldMap = EntityFinderUtil.makeFieldMap(serviceElement);
            this.ignoreError = "true".equals(serviceElement.getAttribute("ignore-error"));
        }

        @Override
        public void runAction(Map<String, Object> context) {
            String serviceNameExpanded = this.serviceNameExdr.expandString(context);
            if (UtilValidate.isEmpty(serviceNameExpanded)) {
                throw new IllegalArgumentException("Service name was empty, expanded from: " + this.serviceNameExdr.getOriginal());
            }

            String autoFieldMapString = this.autoFieldMapExdr.expandString(context);
            boolean autoFieldMapBool = !"false".equals(autoFieldMapString);

            try {
                Map<String, Object> serviceContext = null;
                if (autoFieldMapBool) {
                    serviceContext = WidgetWorker.getDispatcher(context).getDispatchContext().makeValidContext(serviceNameExpanded, ModelService.IN_PARAM, context);
                } else {
                    serviceContext = new HashMap<String, Object>();
                }

                if (this.fieldMap != null) {
                    EntityFinderUtil.expandFieldMapToContext(this.fieldMap, context, serviceContext);
                }

                Map<String, Object> result = null;
                if (this.ignoreError) {
                    result = WidgetWorker.getDispatcher(context).runSync(serviceNameExpanded, serviceContext, -1, true);
                } else {
                    result = WidgetWorker.getDispatcher(context).runSync(serviceNameExpanded, serviceContext);
                }

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
                String listName = resultMapListNameExdr.expandString(context);
                Object listObj = result.get(listName);
                if (listObj != null) {
                    if (!(listObj instanceof List<?>) && !(listObj instanceof ListIterator<?>)) {
                        throw new IllegalArgumentException("Error in form [" + this.modelForm.getName() + "] calling service with name [" + serviceNameExpanded + "]: the result that is supposed to be a List or ListIterator and is not.");
                    }
                    context.put("listName", listName);
                    context.put(listName, listObj);
                }
            } catch (GenericServiceException e) {
                String errMsg = "Error in form [" + this.modelForm.getName() + "] calling service with name [" + serviceNameExpanded + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                if (!this.ignoreError) {
                    throw new IllegalArgumentException(errMsg);
                }
            }
        }
    }

    public static class EntityOne extends ModelFormAction {
        protected PrimaryKeyFinder finder;

        public EntityOne(ModelForm modelForm, Element entityOneElement) {
            super (modelForm, entityOneElement);
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
    }

    public static class EntityAnd extends ModelFormAction {
        protected ByAndFinder finder;
        String actualListName;

        public EntityAnd(ModelForm modelForm, Element entityAndElement) {
            super (modelForm, entityAndElement);

            //don't want to default to the iterator, should be specified explicitly, not the default
            // Document ownerDoc = entityAndElement.getOwnerDocument();
            // boolean useCache = "true".equalsIgnoreCase(entityAndElement.getAttribute("use-cache"));
            // if (!useCache) UtilXml.addChildElement(entityAndElement, "use-iterator", ownerDoc);

            // make list-name optional
            if (UtilValidate.isEmpty(entityAndElement.getAttribute("list")) && UtilValidate.isEmpty(entityAndElement.getAttribute("list-name"))) {
                String lstNm = modelForm.getListName();
                if (UtilValidate.isEmpty(lstNm)) {
                    lstNm = ModelForm.DEFAULT_FORM_RESULT_LIST_NAME;
                }
                entityAndElement.setAttribute("list", lstNm);
            }
            this.actualListName = entityAndElement.getAttribute("list");
            if (UtilValidate.isEmpty(this.actualListName)) this.actualListName = entityAndElement.getAttribute("list-name");
            finder = new ByAndFinder(entityAndElement);
        }

        @Override
        public void runAction(Map<String, Object> context) {
            try {
                // don't want to do this: context.put("defaultFormResultList", null);
                finder.runFind(context, WidgetWorker.getDelegator(context));
                
                /* NOTE DEJ20100925: this should not be running any more as it causes actions in a list or multi 
                 * form definition to overwrite the desired list elsewhere, this was the really old way of doing 
                 * it that was removed a long time ago and needs to stay gone to avoid issues; the form's list 
                 * should be found by explicitly matching the name:
                Object obj = context.get(this.actualListName);
                if (obj != null && ((obj instanceof List) || (obj instanceof EntityListIterator))) {
                    String modelFormListName = modelForm.getListName();
                    context.put(modelFormListName, obj);
                }
                 */
            } catch (GeneralException e) {
                String errMsg = "Error doing entity query by condition: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
        }

    }

    public static class EntityCondition extends ModelFormAction {
        ByConditionFinder finder;
        String actualListName;

        public EntityCondition(ModelForm modelForm, Element entityConditionElement) {
            super (modelForm, entityConditionElement);

            //don't want to default to the iterator, should be specified explicitly, not the default
            // Document ownerDoc = entityConditionElement.getOwnerDocument();
            // boolean useCache = "true".equalsIgnoreCase(entityConditionElement.getAttribute("use-cache"));
            // if (!useCache) UtilXml.addChildElement(entityConditionElement, "use-iterator", ownerDoc);

            // make list-name optional
            if (UtilValidate.isEmpty(entityConditionElement.getAttribute("list")) && UtilValidate.isEmpty(entityConditionElement.getAttribute("list-name"))) {
                String lstNm = modelForm.getListName();
                if (UtilValidate.isEmpty(lstNm)) {
                    lstNm = ModelForm.DEFAULT_FORM_RESULT_LIST_NAME;
                }
                entityConditionElement.setAttribute("list", lstNm);
            }
            this.actualListName = entityConditionElement.getAttribute("list");
            if (UtilValidate.isEmpty(this.actualListName)) this.actualListName = entityConditionElement.getAttribute("list-name");
            finder = new ByConditionFinder(entityConditionElement);
        }

        @Override
        public void runAction(Map<String, Object> context) {
            try {
                // don't want to do this: context.put("defaultFormResultList", null);
                finder.runFind(context, WidgetWorker.getDelegator(context));
                
                /* NOTE DEJ20100925: this should not be running any more as it causes actions in a list or multi 
                 * form definition to overwrite the desired list elsewhere, this was the really old way of doing 
                 * it that was removed a long time ago and needs to stay gone to avoid issues; the form's list 
                 * should be found by explicitly matching the name:
                Object obj = context.get(this.actualListName);
                if (obj != null && ((obj instanceof List) || (obj instanceof EntityListIterator))) {
                    String modelFormListName = modelForm.getListName();
                    context.put(modelFormListName, obj);
                }
                 */
            } catch (GeneralException e) {
                String errMsg = "Error doing entity query by condition: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
        }
    }
}
