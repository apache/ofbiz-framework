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
package org.ofbiz.widget.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.ScriptUtil;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.finder.ByAndFinder;
import org.ofbiz.entity.finder.ByConditionFinder;
import org.ofbiz.entity.finder.EntityFinderUtil;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelService;
import org.ofbiz.widget.ModelActionVisitor;
import org.ofbiz.widget.ModelWidget;
import org.ofbiz.widget.ModelWidgetAction;
import org.ofbiz.widget.WidgetWorker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Widget Library - Tree model class
 */
@SuppressWarnings("serial")
public abstract class ModelTreeAction extends ModelWidgetAction {

    public static final String module = ModelTreeAction.class.getName();

    protected ModelTree modelTree;
    protected ModelTree.ModelNode.ModelSubNode modelSubNode;

    public ModelTreeAction(ModelTree.ModelNode modelNode, Element actionElement) {
        if (Debug.verboseOn()) Debug.logVerbose("Reading Tree action with name: " + actionElement.getNodeName(), module);
        this.modelTree = modelNode.getModelTree();
    }

    public ModelTreeAction(ModelTree.ModelNode.ModelSubNode modelSubNode, Element actionElement) {
        if (Debug.verboseOn()) Debug.logVerbose("Reading Tree action with name: " + actionElement.getNodeName(), module);
        this.modelSubNode = modelSubNode;
        this.modelTree = modelSubNode.getNode().getModelTree();
    }

    public static List<ModelWidgetAction> readNodeActions(ModelWidget modelNode, Element actionsElement) {
        List<? extends Element> actionElementList = UtilXml.childElementList(actionsElement);
        List<ModelWidgetAction> actions = new ArrayList<ModelWidgetAction>(actionElementList.size());
        for (Element actionElement : actionElementList) {
            // TODO: Check for tree-specific actions
            actions.add(ModelWidgetAction.toModelWidgetAction(modelNode, actionElement));
        }
        return actions;
    }

    public static class Script extends ModelTreeAction {
        protected String location;
        protected String method;

        public Script(ModelTree.ModelNode modelNode, Element scriptElement) {
            super (modelNode, scriptElement);
            String scriptLocation = scriptElement.getAttribute("location");
            this.location = WidgetWorker.getScriptLocation(scriptLocation);
            this.method = WidgetWorker.getScriptMethodName(scriptLocation);
        }

        public Script(ModelTree.ModelNode.ModelSubNode modelSubNode, Element scriptElement) {
            super (modelSubNode, scriptElement);
            String scriptLocation = scriptElement.getAttribute("location");
            this.location = WidgetWorker.getScriptLocation(scriptLocation);
            this.method = WidgetWorker.getScriptMethodName(scriptLocation);
        }

        @Override
        public void runAction(Map<String, Object> context) {
            context.put("_LIST_ITERATOR_", null);
            if (location.endsWith(".xml")) {
                Map<String, Object> localContext = new HashMap<String, Object>();
                localContext.putAll(context);
                DispatchContext ctx = WidgetWorker.getDispatcher(context).getDispatchContext();
                MethodContext methodContext = new MethodContext(ctx, localContext, null);
                try {
                    SimpleMethod.runSimpleMethod(location, method, methodContext);
                    context.putAll(methodContext.getResults());
                } catch (MiniLangException e) {
                    throw new RuntimeException("Error running simple method at location [" + location + "]", e);
                }
            } else {
                ScriptUtil.executeScript(this.location, this.method, context);
            }
            Object obj = context.get("_LIST_ITERATOR_");
            if (this.modelSubNode != null) {
                if (obj != null && (obj instanceof EntityListIterator || obj instanceof ListIterator<?>)) {
                    ListIterator<? extends Map<String, ? extends Object>> listIt = UtilGenerics.cast(obj);
                    this.modelSubNode.setListIterator(listIt, context);
                } else {
                    if (obj instanceof List<?>) {
                        List<? extends Map<String, ? extends Object>> list = UtilGenerics.checkList(obj);
                        this.modelSubNode.setListIterator(list.listIterator(), context);
                    }
                }
            }
        }

        @Override
        public void accept(ModelActionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class Service extends ModelTreeAction {
        protected FlexibleStringExpander serviceNameExdr;
        protected FlexibleMapAccessor<Map<String, Object>> resultMapNameAcsr;
        protected FlexibleStringExpander autoFieldMapExdr;
        protected FlexibleStringExpander resultMapListNameExdr;
        protected FlexibleStringExpander resultMapValueNameExdr;
        protected FlexibleStringExpander valueNameExdr;
        protected Map<FlexibleMapAccessor<Object>, Object> fieldMap;

        public Service(ModelTree.ModelNode modelNode, Element serviceElement) {
            super (modelNode, serviceElement);
            initService(serviceElement);
        }

        public Service(ModelTree.ModelNode.ModelSubNode modelSubNode, Element serviceElement) {
            super (modelSubNode, serviceElement);
            initService(serviceElement);
        }

        public void initService(Element serviceElement) {

            this.serviceNameExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("service-name"));
            this.resultMapNameAcsr = FlexibleMapAccessor.getInstance(serviceElement.getAttribute("result-map"));
            if (this.resultMapNameAcsr.isEmpty()) this.resultMapNameAcsr = FlexibleMapAccessor.getInstance(serviceElement.getAttribute("result-map-name"));
            this.autoFieldMapExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("auto-field-map"));
            this.resultMapListNameExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("result-map-list"));
            if (this.resultMapListNameExdr.isEmpty()) this.resultMapListNameExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("result-map-list-name"));
            if (this.resultMapListNameExdr.isEmpty()) this.resultMapListNameExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("result-map-list-iterator-name"));
            this.resultMapValueNameExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("result-map-value"));
            if (this.resultMapValueNameExdr.isEmpty()) this.resultMapValueNameExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("result-map-value-name"));
            this.valueNameExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("value"));
            if (this.valueNameExdr.isEmpty()) this.valueNameExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("value-name"));
            this.fieldMap = EntityFinderUtil.makeFieldMap(serviceElement);
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
                String resultMapListName = resultMapListNameExdr.expandString(context);
                //String resultMapListIteratorName = resultMapListIteratorNameExdr.expandString(context);
                String resultMapValueName = resultMapValueNameExdr.expandString(context);
                String valueName = valueNameExdr.expandString(context);

                if (this.modelSubNode != null) {
                    //ListIterator iter = null;
                    if (UtilValidate.isNotEmpty(resultMapListName)) {
                        List<? extends Map<String, ? extends Object>> lst = UtilGenerics.checkList(result.get(resultMapListName));
                        if (lst != null) {
                            if (lst instanceof ListIterator<?>) {
                                ListIterator<? extends Map<String, ? extends Object>> listIt = UtilGenerics.cast(lst);
                                this.modelSubNode.setListIterator(listIt, context);
                            } else {
                                this.modelSubNode.setListIterator(lst.listIterator(), context);
                            }
                        }
                    }
                } else {
                    if (UtilValidate.isNotEmpty(resultMapValueName)) {
                        if (UtilValidate.isNotEmpty(valueName)) {
                            context.put(valueName, result.get(resultMapValueName));
                        } else {
                            Map<String, Object> resultMap = UtilGenerics.checkMap(result.get(resultMapValueName));
                            context.putAll(resultMap);
                        }
                    }
                }
            } catch (GenericServiceException e) {
                String errMsg = "Error calling service with name " + serviceNameExpanded + ": " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
        }

        @Override
        public void accept(ModelActionVisitor visitor) {
            visitor.visit(this);
        }
    }

    public static class EntityAnd extends ModelTreeAction {
        protected ByAndFinder finder;
        String listName;

        public EntityAnd(ModelTree.ModelNode.ModelSubNode modelSubNode, Element entityAndElement) {
            super (modelSubNode, entityAndElement);
            boolean useCache = "true".equalsIgnoreCase(entityAndElement.getAttribute("use-cache"));
            Document ownerDoc = entityAndElement.getOwnerDocument();
            if (!useCache) UtilXml.addChildElement(entityAndElement, "use-iterator", ownerDoc);

            this.listName = UtilFormatOut.checkEmpty(entityAndElement.getAttribute("list"), entityAndElement.getAttribute("list-name"));
            if (UtilValidate.isEmpty(this.listName)) this.listName = "_LIST_ITERATOR_";
            entityAndElement.setAttribute("list-name", this.listName);

            finder = new ByAndFinder(entityAndElement);
        }

        @Override
        public void runAction(Map<String, Object> context) {
            try {
                context.put(this.listName, null);
                finder.runFind(context, WidgetWorker.getDelegator(context));
                Object obj = context.get(this.listName);
                if (obj != null && (obj instanceof EntityListIterator || obj instanceof ListIterator<?>)) {
                    ListIterator<? extends Map<String, ? extends Object>> listIt = UtilGenerics.cast(obj);
                    this.modelSubNode.setListIterator(listIt, context);
                } else {
                    if (obj instanceof List<?>) {
                        List<? extends Map<String, ? extends Object>> list = UtilGenerics.checkList(obj);
                        this.modelSubNode.setListIterator(list.listIterator(), context);
                    }
                }
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
    }

    public static class EntityCondition extends ModelTreeAction {
        ByConditionFinder finder;
        String listName;

        public EntityCondition(ModelTree.ModelNode.ModelSubNode modelSubNode, Element entityConditionElement) {
            super (modelSubNode, entityConditionElement);
            Document ownerDoc = entityConditionElement.getOwnerDocument();
            boolean useCache = "true".equalsIgnoreCase(entityConditionElement.getAttribute("use-cache"));
            if (!useCache) UtilXml.addChildElement(entityConditionElement, "use-iterator", ownerDoc);

            this.listName = UtilFormatOut.checkEmpty(entityConditionElement.getAttribute("list"), entityConditionElement.getAttribute("list-name"));
            if (UtilValidate.isEmpty(this.listName)) this.listName = "_LIST_ITERATOR_";
            entityConditionElement.setAttribute("list-name", this.listName);

            finder = new ByConditionFinder(entityConditionElement);
        }

        @Override
        public void runAction(Map<String, Object> context) {
            try {
                context.put(this.listName, null);
                finder.runFind(context, WidgetWorker.getDelegator(context));
                Object obj = context.get(this.listName);
                if (obj != null && (obj instanceof EntityListIterator || obj instanceof ListIterator<?>)) {
                    ListIterator<? extends Map<String, ? extends Object>> listIt = UtilGenerics.cast(obj);
                    this.modelSubNode.setListIterator(listIt, context);
                } else {
                    if (obj instanceof List<?>) {
                        List<? extends Map<String, ? extends Object>> list = UtilGenerics.cast(obj);
                        this.modelSubNode.setListIterator(list.listIterator(), context);
                    }
                }
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
    }
}
