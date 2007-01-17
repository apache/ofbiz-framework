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
package org.ofbiz.widget.menu;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.BshUtil;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.finder.ByAndFinder;
import org.ofbiz.entity.finder.ByConditionFinder;
import org.ofbiz.entity.finder.PrimaryKeyFinder;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelService;

import org.w3c.dom.Element;
import javax.servlet.*;
import javax.servlet.http.*;


/**
 * Widget Library - Screen model class
 */
public abstract class ModelMenuAction {
    public static final String module = ModelMenuAction.class.getName();

    protected ModelMenu modelMenu;
    protected ModelMenuItem modelMenuItem;

    public ModelMenuAction(ModelMenu modelMenu, Element actionElement) {
        this.modelMenu = modelMenu;
        if (Debug.verboseOn()) Debug.logVerbose("Reading Screen action with name: " + actionElement.getNodeName(), module);
    }
    
    public ModelMenuAction(ModelMenuItem modelMenuItem, Element actionElement) {
        this.modelMenuItem = modelMenuItem;
        this.modelMenu = modelMenuItem.getModelMenu();
        if (Debug.verboseOn()) Debug.logVerbose("Reading Screen action with name: " + actionElement.getNodeName(), module);
    }
    
    public abstract void runAction(Map context);
    
    public static List readSubActions(ModelMenuItem modelMenuItem, Element parentElement) {
        return readSubActions(modelMenuItem.getModelMenu(), parentElement);
    }
    
    public static List readSubActions(ModelMenu modelMenu, Element parentElement) {
        List actions = new LinkedList();
        
        List actionElementList = UtilXml.childElementList(parentElement);
        Iterator actionElementIter = actionElementList.iterator();
        while (actionElementIter.hasNext()) {
            Element actionElement = (Element) actionElementIter.next();
            if ("set".equals(actionElement.getNodeName())) {
                actions.add(new SetField(modelMenu, actionElement));
            } else if ("property-map".equals(actionElement.getNodeName())) {
                actions.add(new PropertyMap(modelMenu, actionElement));
            } else if ("property-to-field".equals(actionElement.getNodeName())) {
                actions.add(new PropertyToField(modelMenu, actionElement));
            } else if ("script".equals(actionElement.getNodeName())) {
                actions.add(new Script(modelMenu, actionElement));
            } else if ("service".equals(actionElement.getNodeName())) {
                actions.add(new Service(modelMenu, actionElement));
            } else if ("entity-one".equals(actionElement.getNodeName())) {
                actions.add(new EntityOne(modelMenu, actionElement));
            } else if ("entity-and".equals(actionElement.getNodeName())) {
                actions.add(new EntityAnd(modelMenu, actionElement));
            } else if ("entity-condition".equals(actionElement.getNodeName())) {
                actions.add(new EntityCondition(modelMenu, actionElement));
            } else {
                throw new IllegalArgumentException("Action element not supported with name: " + actionElement.getNodeName());
            }
        }
        
        return actions;
    }
    
    public static void runSubActions(List actions, Map context) {
        if (actions == null) return;
        
        Iterator actionIter = actions.iterator();
        while (actionIter.hasNext()) {
            ModelMenuAction action = (ModelMenuAction) actionIter.next();
            if (Debug.verboseOn()) Debug.logVerbose("Running screen action " + action.getClass().getName(), module);
            action.runAction(context);
        }
    }
    
    public static class SetField extends ModelMenuAction {
        protected FlexibleMapAccessor field;
        protected FlexibleMapAccessor fromField;
        protected FlexibleStringExpander valueExdr;
        protected FlexibleStringExpander defaultExdr;
        protected FlexibleStringExpander globalExdr;
        protected String type;
        protected String toScope;
        protected String fromScope;
        
        public SetField(ModelMenu modelMenu, Element setElement) {
            super (modelMenu, setElement);
            this.field = new FlexibleMapAccessor(setElement.getAttribute("field"));
            this.fromField = UtilValidate.isNotEmpty(setElement.getAttribute("from-field")) ? new FlexibleMapAccessor(setElement.getAttribute("from-field")) : null;
            this.valueExdr = UtilValidate.isNotEmpty(setElement.getAttribute("value")) ? new FlexibleStringExpander(setElement.getAttribute("value")) : null;
            this.defaultExdr = UtilValidate.isNotEmpty(setElement.getAttribute("default-value")) ? new FlexibleStringExpander(setElement.getAttribute("default-value")) : null;
            this.globalExdr = new FlexibleStringExpander(setElement.getAttribute("global"));
            this.type = setElement.getAttribute("type");
            this.toScope = setElement.getAttribute("to-scope");
            this.fromScope = setElement.getAttribute("from-scope");
            if (this.fromField != null && this.valueExdr != null) {
                throw new IllegalArgumentException("Cannot specify a from-field [" + setElement.getAttribute("from-field") + "] and a value [" + setElement.getAttribute("value") + "] on the set action in a screen widget");
            }
        }
        
        public void runAction(Map context) {
            String globalStr = this.globalExdr.expandString(context);
            // default to false
            boolean global = "true".equals(globalStr);
            
            Object newValue = null;
            if (this.fromScope != null && this.fromScope.equals("user")) {
                if (this.fromField != null) {
                    String originalName = this.fromField.getOriginalName();
                    String currentWidgetTrail = (String)context.get("_WIDGETTRAIL_");
                    String newKey = currentWidgetTrail + "|" + originalName;
                    HttpSession session = (HttpSession)context.get("session");
                    newValue = session.getAttribute(newKey);
                    if (Debug.verboseOn()) Debug.logVerbose("In user getting value for field from [" + this.fromField.getOriginalName() + "]: " + newValue, module);
                } else if (this.valueExdr != null) {
                    newValue = this.valueExdr.expandString(context);
                }
                
            } else if (this.fromScope != null && this.fromScope.equals("application")) {
                if (this.fromField != null) {
                    String originalName = this.fromField.getOriginalName();
                    String currentWidgetTrail = (String)context.get("_WIDGETTRAIL_");
                    String newKey = currentWidgetTrail + "|" + originalName;
                    ServletContext servletContext = (ServletContext)context.get("application");
                    newValue = servletContext.getAttribute(newKey);
                    if (Debug.verboseOn()) Debug.logVerbose("In application getting value for field from [" + this.fromField.getOriginalName() + "]: " + newValue, module);
                } else if (this.valueExdr != null) {
                    newValue = this.valueExdr.expandString(context);
                }
                
            } else {
                if (this.fromField != null) {
                    newValue = this.fromField.get(context);
                    if (Debug.verboseOn()) Debug.logVerbose("In screen getting value for field from [" + this.fromField.getOriginalName() + "]: " + newValue, module);
                } else if (this.valueExdr != null) {
                    newValue = this.valueExdr.expandString(context);
                }
            }

            // If newValue is still empty, use the default value
           	if (this.defaultExdr != null) {
           		if (ObjectType.isEmpty(newValue)) {
            		newValue = this.defaultExdr.expandString(context);
               	}
            }
            
            if (UtilValidate.isNotEmpty(this.type)) {
                try {
                    newValue = ObjectType.simpleTypeConvert(newValue, this.type, null, null);
                } catch (GeneralException e) {
                    String errMsg = "Could not convert field value for the field: [" + this.field.getOriginalName() + "] to the [" + this.type + "] type for the value [" + newValue + "]: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    throw new IllegalArgumentException(errMsg);
                }
         
            }
            if (this.toScope != null && this.toScope.equals("user")) {
                    String originalName = this.field.getOriginalName();
                    String currentWidgetTrail = (String)context.get("_WIDGETTRAIL_");
                    String newKey = currentWidgetTrail + "|" + originalName;
                    HttpSession session = (HttpSession)context.get("session");
                    session.setAttribute(newKey, newValue);
                    if (Debug.verboseOn()) Debug.logVerbose("In user setting value for field from [" + this.field.getOriginalName() + "]: " + newValue, module);
                
            } else if (this.toScope != null && this.toScope.equals("application")) {
                    String originalName = this.field.getOriginalName();
                    String currentWidgetTrail = (String)context.get("_WIDGETTRAIL_");
                    String newKey = currentWidgetTrail + "|" + originalName;
                    ServletContext servletContext = (ServletContext)context.get("application");
                    servletContext.setAttribute(newKey, newValue);
                    if (Debug.verboseOn()) Debug.logVerbose("In application setting value for field from [" + this.field.getOriginalName() + "]: " + newValue, module);
                
            } else {
            	if (Debug.verboseOn()) Debug.logVerbose("In screen setting field [" + this.field.getOriginalName() + "] to value: " + newValue, module);
                this.field.put(context, newValue);
            }
            
            if (global) {
                Map globalCtx = (Map) context.get("globalContext");
                if (globalCtx != null) {
                    this.field.put(globalCtx, newValue);
                }
            }
            
            // this is a hack for backward compatibility with the JPublish page object
            Map page = (Map) context.get("page");
            if (page != null) {
                this.field.put(page, newValue);
            }
        }
    }
    
    public static class PropertyMap extends ModelMenuAction {
        protected FlexibleStringExpander resourceExdr;
        protected FlexibleMapAccessor mapNameAcsr;
        protected FlexibleStringExpander globalExdr;
        
        public PropertyMap(ModelMenu modelMenu, Element setElement) {
            super (modelMenu, setElement);
            this.resourceExdr = new FlexibleStringExpander(setElement.getAttribute("resource"));
            this.mapNameAcsr = new FlexibleMapAccessor(setElement.getAttribute("map-name"));
            this.globalExdr = new FlexibleStringExpander(setElement.getAttribute("global"));
        }
        
        public void runAction(Map context) {
            String globalStr = this.globalExdr.expandString(context);
            // default to false
            boolean global = "true".equals(globalStr);

            Locale locale = (Locale) context.get("locale");
            String resource = this.resourceExdr.expandString(context, locale);
            Map propertyMap = UtilProperties.getResourceBundleMap(resource, locale);
            this.mapNameAcsr.put(context, propertyMap);

            if (global) {
                Map globalCtx = (Map) context.get("globalContext");
                if (globalCtx != null) {
                    this.mapNameAcsr.put(globalCtx, propertyMap);
                }
            }
        }
    }
    
    public static class PropertyToField extends ModelMenuAction {
        
        protected FlexibleStringExpander resourceExdr;
        protected FlexibleStringExpander propertyExdr;
        protected FlexibleMapAccessor fieldAcsr;
        protected FlexibleStringExpander defaultExdr;
        protected boolean noLocale;
        protected FlexibleMapAccessor argListAcsr;
        protected FlexibleStringExpander globalExdr;

        public PropertyToField(ModelMenu modelMenu, Element setElement) {
            super (modelMenu, setElement);
            this.resourceExdr = new FlexibleStringExpander(setElement.getAttribute("resource"));
            this.propertyExdr = new FlexibleStringExpander(setElement.getAttribute("property"));
            this.fieldAcsr = new FlexibleMapAccessor(setElement.getAttribute("field"));
            this.defaultExdr = new FlexibleStringExpander(setElement.getAttribute("default"));
            noLocale = "true".equals(setElement.getAttribute("no-locale"));
            this.argListAcsr = new FlexibleMapAccessor(setElement.getAttribute("arg-list-name"));
            this.globalExdr = new FlexibleStringExpander(setElement.getAttribute("global"));
        }
        
        public void runAction(Map context) {
            // default to false

            Locale locale = (Locale) context.get("locale");
            String resource = this.resourceExdr.expandString(context, locale);
            String property = this.propertyExdr.expandString(context, locale);
            
            String value = null;
            if (noLocale) {
                value = UtilProperties.getPropertyValue(resource, property);
            } else {
                value = UtilProperties.getMessage(resource, property, locale);
            }
            if (value == null || value.length() == 0) {
                value = this.defaultExdr.expandString(context);
            }
            
            // note that expanding the value string here will handle defaultValue and the string from 
            //  the properties file; if we decide later that we don't want the string from the properties 
            //  file to be expanded we should just expand the defaultValue at the beginning of this method.
            value = FlexibleStringExpander.expandString(value, context);

            if (!argListAcsr.isEmpty()) {
                List argList = (List) argListAcsr.get(context);
                if (argList != null && argList.size() > 0) {
                    value = MessageFormat.format(value, argList.toArray());
                }
            }

            fieldAcsr.put(context, value);
        }
    }
    
    public static class Script extends ModelMenuAction {
        protected String location;
        
        public Script(ModelMenu modelMenu, Element scriptElement) {
            super (modelMenu, scriptElement);
            this.location = scriptElement.getAttribute("location");
        }
        
        public void runAction(Map context) {
            if (location.endsWith(".bsh")) {
                try {
                    BshUtil.runBshAtLocation(location, context);
                } catch (GeneralException e) {
                    String errMsg = "Error running BSH script at location [" + location + "]: " + e.toString();
                    Debug.logError(e, errMsg, module);
                    throw new IllegalArgumentException(errMsg);
                }
            } else {
                throw new IllegalArgumentException("For screen script actions the script type is not yet support for location:" + location);
            }
        }
    }

    public static class Service extends ModelMenuAction {
        protected FlexibleStringExpander serviceNameExdr;
        protected FlexibleMapAccessor resultMapNameAcsr;
        protected FlexibleStringExpander autoFieldMapExdr;
        protected Map fieldMap;
        
        public Service(ModelMenu modelMenu, Element serviceElement) {
            super (modelMenu, serviceElement);
            this.serviceNameExdr = new FlexibleStringExpander(serviceElement.getAttribute("service-name"));
            this.resultMapNameAcsr = UtilValidate.isNotEmpty(serviceElement.getAttribute("result-map-name")) ? new FlexibleMapAccessor(serviceElement.getAttribute("result-map-name")) : null;
            this.autoFieldMapExdr = new FlexibleStringExpander(serviceElement.getAttribute("auto-field-map"));
            
            List fieldMapElementList = UtilXml.childElementList(serviceElement, "field-map");
            if (fieldMapElementList.size() > 0) {
                this.fieldMap = new HashMap();
                Iterator fieldMapElementIter = fieldMapElementList.iterator();
                while (fieldMapElementIter.hasNext()) {
                    Element fieldMapElement = (Element) fieldMapElementIter.next();
                    // set the env-name for each field-name, noting that if no field-name is specified it defaults to the env-name
                    this.fieldMap.put(
                            new FlexibleMapAccessor(UtilFormatOut.checkEmpty(fieldMapElement.getAttribute("field-name"), fieldMapElement.getAttribute("env-name"))), 
                            new FlexibleMapAccessor(fieldMapElement.getAttribute("env-name")));
                }
            }
        }
        
        public void runAction(Map context) {
            String serviceNameExpanded = this.serviceNameExdr.expandString(context);
            if (UtilValidate.isEmpty(serviceNameExpanded)) {
                throw new IllegalArgumentException("Service name was empty, expanded from: " + this.serviceNameExdr.getOriginal());
            }
            
            String autoFieldMapString = this.autoFieldMapExdr.expandString(context);
            boolean autoFieldMapBool = !"false".equals(autoFieldMapString);
            
            try {
                Map serviceContext = null;
                if (autoFieldMapBool) {
                    serviceContext = this.modelMenu.getDispacher().getDispatchContext().makeValidContext(serviceNameExpanded, ModelService.IN_PARAM, context);
                } else {
                    serviceContext = new HashMap();
                }
                
                if (this.fieldMap != null) {
                    Iterator fieldMapEntryIter = this.fieldMap.entrySet().iterator();
                    while (fieldMapEntryIter.hasNext()) {
                        Map.Entry entry = (Map.Entry) fieldMapEntryIter.next();
                        FlexibleMapAccessor serviceContextFieldAcsr = (FlexibleMapAccessor) entry.getKey();
                        FlexibleMapAccessor contextEnvAcsr = (FlexibleMapAccessor) entry.getValue();
                        serviceContextFieldAcsr.put(serviceContext, contextEnvAcsr.get(context));
                    }
                }
                
                Map result = this.modelMenu.getDispacher().runSync(serviceNameExpanded, serviceContext);
                
                if (this.resultMapNameAcsr != null) {
                    this.resultMapNameAcsr.put(context, result);
                } else {
                    context.putAll(result);
                }
            } catch (GenericServiceException e) {
                String errMsg = "Error calling service with name " + serviceNameExpanded + ": " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
        }
    }

    public static class EntityOne extends ModelMenuAction {
        protected PrimaryKeyFinder finder;
        
        public EntityOne(ModelMenu modelMenu, Element entityOneElement) {
            super (modelMenu, entityOneElement);
            finder = new PrimaryKeyFinder(entityOneElement);
        }
        
        public void runAction(Map context) {
            try {
                finder.runFind(context, this.modelMenu.getDelegator());
            } catch (GeneralException e) {
                String errMsg = "Error doing entity query by condition: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
        }
    }

    public static class EntityAnd extends ModelMenuAction {
        protected ByAndFinder finder;
        
        public EntityAnd(ModelMenu modelMenu, Element entityAndElement) {
            super (modelMenu, entityAndElement);
            finder = new ByAndFinder(entityAndElement);
        }
        
        public void runAction(Map context) {
            try {
                finder.runFind(context, this.modelMenu.getDelegator());
            } catch (GeneralException e) {
                String errMsg = "Error doing entity query by condition: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
        }
    }

    public static class EntityCondition extends ModelMenuAction {
        ByConditionFinder finder;
        
        public EntityCondition(ModelMenu modelMenu, Element entityConditionElement) {
            super (modelMenu, entityConditionElement);
            finder = new ByConditionFinder(entityConditionElement);
        }
        
        public void runAction(Map context) {
            try {
                finder.runFind(context, this.modelMenu.getDelegator());
            } catch (GeneralException e) {
                String errMsg = "Error doing entity query by condition: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
        }
    }
}

