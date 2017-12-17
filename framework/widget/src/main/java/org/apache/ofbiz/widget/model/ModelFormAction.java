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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.finder.EntityFinderUtil;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.widget.WidgetWorker;
import org.w3c.dom.Element;

/**
 * Abstract form action.
 */
public abstract class ModelFormAction {

    public static final String module = ModelFormAction.class.getName();

    public static List<ModelAction> readSubActions(ModelForm modelForm, Element parentElement) {
        List<? extends Element> actionElementList = UtilXml.childElementList(parentElement);
        List<ModelAction> actions = new ArrayList<>(actionElementList.size());
        for (Element actionElement : UtilXml.childElementList(parentElement)) {
            if ("service".equals(actionElement.getNodeName())) {
                actions.add(new Service(modelForm, actionElement));
            } else if ("entity-and".equals(actionElement.getNodeName()) || "entity-condition".equals(actionElement.getNodeName())
                    || "get-related".equals(actionElement.getNodeName())) {
                if (!actionElement.hasAttribute("list")) {
                    String listName = modelForm.getListName();
                    if (UtilValidate.isEmpty(listName)) {
                        listName = ModelForm.DEFAULT_FORM_RESULT_LIST_NAME;
                    }
                    actionElement.setAttribute("list", listName);
                }
                actions.add(AbstractModelAction.newInstance(modelForm, actionElement));
            } else if ("call-parent-actions".equals(actionElement.getNodeName())) {
                actions.add(new CallParentActions(modelForm, actionElement));
            } else {
                actions.add(AbstractModelAction.newInstance(modelForm, actionElement));
            }
        }
        return Collections.unmodifiableList(actions);
    }

    /**
     * Models the &lt;call-parent-actions&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    @SuppressWarnings("serial")
    public static class CallParentActions extends AbstractModelAction {
        private final ActionsKind kind;
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
        public void accept(ModelActionVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public void runAction(Map<String, Object> context) {
            ModelForm parentModel = modelForm.getParentModelForm();
            switch (kind) {
            case ACTIONS:
                parentModel.runFormActions(context);
                break;
            case ROW_ACTIONS:
                AbstractModelAction.runSubActions(parentModel.getRowActions(), context);
                break;
            }
        }

        protected static enum ActionsKind {
            ACTIONS, ROW_ACTIONS
        }
    }

    /**
     * Models the &lt;service&gt; element.
     *
     * @see <code>widget-form.xsd</code>
     */
    @SuppressWarnings("serial")
    public static class Service extends AbstractModelAction {
        private final FlexibleStringExpander autoFieldMapExdr;
        private final Map<FlexibleMapAccessor<Object>, Object> fieldMap;
        private final boolean ignoreError;
        private final FlexibleStringExpander resultMapListNameExdr;
        private final FlexibleMapAccessor<Map<String, Object>> resultMapNameAcsr;
        private final FlexibleStringExpander serviceNameExdr;

        public Service(ModelForm modelForm, Element serviceElement) {
            super(modelForm, serviceElement);
            this.serviceNameExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("service-name"));
            this.resultMapNameAcsr = FlexibleMapAccessor.getInstance(serviceElement.getAttribute("result-map"));
            this.autoFieldMapExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("auto-field-map"));
            FlexibleStringExpander resultMapListNameExdr;
            if (UtilValidate.isEmpty(serviceElement.getAttribute("result-map-list"))
                    && UtilValidate.isEmpty(serviceElement.getAttribute("result-map-list-name"))) {
                if (UtilValidate.isEmpty(serviceElement.getAttribute("result-map-list-iterator"))
                        && UtilValidate.isEmpty(serviceElement.getAttribute("result-map-list-iterator-name"))) {
                    String lstNm = modelForm.getListName();
                    if (UtilValidate.isEmpty(lstNm)) {
                        lstNm = ModelForm.DEFAULT_FORM_RESULT_LIST_NAME;
                    }
                    resultMapListNameExdr = FlexibleStringExpander.getInstance(lstNm);
                } else {
                    // this is deprecated, but support it for now anyway
                    resultMapListNameExdr = FlexibleStringExpander.getInstance(serviceElement
                            .getAttribute("result-map-list-iterator"));
                    if (resultMapListNameExdr.isEmpty()) {
                        resultMapListNameExdr = FlexibleStringExpander.getInstance(serviceElement
                                .getAttribute("result-map-list-iterator-name"));
                    }
                }
            } else {
                resultMapListNameExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("result-map-list"));
                if (resultMapListNameExdr.isEmpty()) {
                    resultMapListNameExdr = FlexibleStringExpander.getInstance(serviceElement
                            .getAttribute("result-map-list-name"));
                }
            }
            this.resultMapListNameExdr = resultMapListNameExdr;
            this.fieldMap = EntityFinderUtil.makeFieldMap(serviceElement);
            this.ignoreError = "true".equals(serviceElement.getAttribute("ignore-error"));
        }

        @Override
        public void accept(ModelActionVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        public String getServiceName() {
            return serviceNameExdr.getOriginal();
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
                    if (!"true".equals(autoFieldMapString)) {
                        Map<String, Object> autoFieldMap = UtilGenerics.checkMap(context.get(autoFieldMapString));
                        serviceContext = WidgetWorker.getDispatcher(context).getDispatchContext()
                                .makeValidContext(serviceNameExpanded, ModelService.IN_PARAM, autoFieldMap);
                    } else {
                        serviceContext = WidgetWorker.getDispatcher(context).getDispatchContext()
                                .makeValidContext(serviceNameExpanded, ModelService.IN_PARAM, context);
                    }
                } else {
                    serviceContext = new HashMap<>();
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
                ModelActionUtil.contextPutQueryStringOrAllResult(context, result, this.resultMapNameAcsr);
                String listName = resultMapListNameExdr.expandString(context);
                Object listObj = result.get(listName);
                if (listObj != null) {
                    if (!(listObj instanceof List<?>) && !(listObj instanceof ListIterator<?>)) {
                        throw new IllegalArgumentException("Error in form [" + this.getModelWidget().getName()
                                + "] calling service with name [" + serviceNameExpanded
                                + "]: the result that is supposed to be a List or ListIterator and is not.");
                    }
                    context.put("listName", listName);
                    context.put(listName, listObj);
                }
            } catch (GenericServiceException e) {
                String errMsg = "Error in form [" + this.getModelWidget().getName() + "] calling service with name ["
                        + serviceNameExpanded + "]: " + e.toString();
                Debug.logError(e, errMsg, module);
                if (!this.ignoreError) {
                    throw new IllegalArgumentException(errMsg);
                }
            }
        }

        public FlexibleStringExpander getAutoFieldMapExdr() {
            return autoFieldMapExdr;
        }

        public Map<FlexibleMapAccessor<Object>, Object> getFieldMap() {
            return fieldMap;
        }

        public boolean getIgnoreError() {
            return ignoreError;
        }

        public FlexibleStringExpander getResultMapListNameExdr() {
            return resultMapListNameExdr;
        }

        public FlexibleMapAccessor<Map<String, Object>> getResultMapNameAcsr() {
            return resultMapNameAcsr;
        }

        public FlexibleStringExpander getServiceNameExdr() {
            return serviceNameExdr;
        }
    }
}
