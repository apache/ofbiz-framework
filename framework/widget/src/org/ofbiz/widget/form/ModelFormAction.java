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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.finder.ByAndFinder;
import org.ofbiz.entity.finder.ByConditionFinder;
import org.ofbiz.entity.finder.EntityFinderUtil;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelService;
import org.ofbiz.widget.ModelActionVisitor;
import org.ofbiz.widget.ModelWidgetAction;
import org.ofbiz.widget.WidgetWorker;
import org.w3c.dom.Element;

/**
 * Widget Library - Screen model class
 */
public abstract class ModelFormAction {

    public static final String module = ModelFormAction.class.getName();

    public static List<ModelWidgetAction> readSubActions(ModelForm modelForm, Element parentElement) {
        List<? extends Element> actionElementList = UtilXml.childElementList(parentElement);
        List<ModelWidgetAction> actions = new ArrayList<ModelWidgetAction>(actionElementList.size());
        for (Element actionElement : UtilXml.childElementList(parentElement)) {
            if ("service".equals(actionElement.getNodeName())) {
                actions.add(new Service(modelForm, actionElement));
            } else if ("entity-and".equals(actionElement.getNodeName())) {
                actions.add(new EntityAnd(modelForm, actionElement));
            } else if ("entity-condition".equals(actionElement.getNodeName())) {
                actions.add(new EntityCondition(modelForm, actionElement));
            } else if ("call-parent-actions".equals(actionElement.getNodeName())) {
                actions.add(new CallParentActions(modelForm, actionElement));
            } else {
                actions.add(ModelWidgetAction.toModelWidgetAction(modelForm, actionElement));
            }
        }
        return Collections.unmodifiableList(actions);
    }

    @SuppressWarnings("serial")
    public static class Service extends ModelWidgetAction {
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
                    if (! "true".equals(autoFieldMapString)) {
                        Map<String, Object> autoFieldMap = UtilGenerics.checkMap(context.get(autoFieldMapString));
                        serviceContext = WidgetWorker.getDispatcher(context).getDispatchContext().makeValidContext(serviceNameExpanded, ModelService.IN_PARAM, autoFieldMap);
                    } else {
                        serviceContext = WidgetWorker.getDispatcher(context).getDispatchContext().makeValidContext(serviceNameExpanded, ModelService.IN_PARAM, context);
                    }
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
                        throw new IllegalArgumentException("Error in form [" + this.modelWidget.getName() + "] calling service with name [" + serviceNameExpanded + "]: the result that is supposed to be a List or ListIterator and is not.");
                    }
                    context.put("listName", listName);
                    context.put(listName, listObj);
                }
            } catch (GenericServiceException e) {
                String errMsg = "Error in form [" + this.modelWidget.getName() + "] calling service with name [" + serviceNameExpanded + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                if (!this.ignoreError) {
                    throw new IllegalArgumentException(errMsg);
                }
            }
        }

        @Override
        public void accept(ModelActionVisitor visitor) {
            visitor.visit(this);
        }

        public String getServiceName() {
            return serviceNameExdr.getOriginal();
        }
    }

    @SuppressWarnings("serial")
    public static class EntityAnd extends ModelWidgetAction {
        protected ByAndFinder finder;

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

        @Override
        public void accept(ModelActionVisitor visitor) {
            visitor.visit(this);
        }

        public ByAndFinder getFinder() {
            return finder;
        }
    }

    @SuppressWarnings("serial")
    public static class EntityCondition extends ModelWidgetAction {
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

        @Override
        public void accept(ModelActionVisitor visitor) {
            visitor.visit(this);
        }

        public ByConditionFinder getFinder() {
            return finder;
        }
    }

    @SuppressWarnings("serial")
    public static class CallParentActions extends ModelWidgetAction {
        protected static enum ActionsKind {
            ACTIONS,
            ROW_ACTIONS
        };

        protected ActionsKind kind;
        private final ModelForm modelForm;

        public CallParentActions(ModelForm modelForm, Element callParentActionsElement) {
            super(modelForm, callParentActionsElement);
            String parentName = callParentActionsElement.getParentNode().getNodeName();
            if ("actions".equals(parentName)) {
                kind = ActionsKind.ACTIONS;
            } else if ("row-actions".equals(parentName)) {
                kind = ActionsKind.ROW_ACTIONS;
            } else {
                throw new IllegalArgumentException("Action element not supported for call-parent-actions : " + parentName);
            }

            ModelForm parentModel = modelForm.getParentModelForm();
            if (parentModel == null) {
                throw new IllegalArgumentException("call-parent-actions can only be used with form extending another form");
            }
            this.modelForm = modelForm;
        }

        @Override
        public void runAction(Map<String, Object> context) {
            ModelForm parentModel = modelForm.getParentModelForm();
            switch (kind) {
                case ACTIONS:
                    parentModel.runFormActions(context);
                    break;
                case ROW_ACTIONS:
                    ModelWidgetAction.runSubActions(parentModel.rowActions, context);
                    break;
            }
        }

        @Override
        public void accept(ModelActionVisitor visitor) {
            visitor.visit(this);
        }
    }
}
