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
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.ScriptUtil;
import org.apache.ofbiz.base.util.UtilFormatOut;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.finder.ByAndFinder;
import org.apache.ofbiz.entity.finder.ByConditionFinder;
import org.apache.ofbiz.entity.finder.EntityFinderUtil;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.widget.WidgetWorker;
import org.apache.ofbiz.widget.model.ModelTree.ModelNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Abstract tree action.
 */
@SuppressWarnings("serial")
public abstract class ModelTreeAction extends AbstractModelAction {

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

    public static final String module = ModelTreeAction.class.getName();

    public static List<ModelAction> readNodeActions(ModelNode modelNode, Element actionsElement) {
        List<? extends Element> actionElementList = UtilXml.childElementList(actionsElement);
        List<ModelAction> actions = new ArrayList<>(actionElementList.size());
        for (Element actionElement : actionElementList) {
            if ("service".equals(actionElement.getNodeName())) {
                actions.add(new Service(modelNode, actionElement));
            } else if ("script".equals(actionElement.getNodeName())) {
                actions.add(new Script(modelNode, actionElement));
            } else {
                actions.add(AbstractModelAction.newInstance(modelNode, actionElement));
            }
        }
        return actions;
    }

    public static List<ModelAction> readSubNodeActions(ModelNode.ModelSubNode modelSubNode, Element actionsElement) {
        List<? extends Element> actionElementList = UtilXml.childElementList(actionsElement);
        List<ModelAction> actions = new ArrayList<>(actionElementList.size());
        for (Element actionElement : actionElementList) {
            if ("service".equals(actionElement.getNodeName())) {
                actions.add(new Service(modelSubNode, actionElement));
            } else if ("entity-and".equals(actionElement.getNodeName())) {
                actions.add(new EntityAnd(modelSubNode, actionElement));
            } else if ("entity-condition".equals(actionElement.getNodeName())) {
                actions.add(new EntityCondition(modelSubNode, actionElement));
            } else if ("script".equals(actionElement.getNodeName())) {
                actions.add(new Script(modelSubNode, actionElement));
            } else {
                actions.add(AbstractModelAction.newInstance(modelSubNode, actionElement));
            }
        }
        return actions;
    }

    private final ModelNode.ModelSubNode modelSubNode;
    private final ModelTree modelTree;

    protected ModelTreeAction(ModelNode modelNode, Element actionElement) {
        if (Debug.verboseOn()) {
             Debug.logVerbose("Reading Tree action with name: " + actionElement.getNodeName(), module);
        }
        this.modelTree = modelNode.getModelTree();
        this.modelSubNode = null;
    }

    protected ModelTreeAction(ModelNode.ModelSubNode modelSubNode, Element actionElement) {
        if (Debug.verboseOn()) {
             Debug.logVerbose("Reading Tree action with name: " + actionElement.getNodeName(), module);
        }
        this.modelSubNode = modelSubNode;
        this.modelTree = modelSubNode.getNode().getModelTree();
    }

    public ModelNode.ModelSubNode getModelSubNode() {
        return modelSubNode;
    }

    public ModelTree getModelTree() {
        return modelTree;
    }

    /**
     * Models the &lt;entity-and&gt; element.
     *
     * @see <code>widget-tree.xsd</code>
     */
    public static class EntityAnd extends ModelTreeAction {
        private final ByAndFinder finder;
        private final String listName;

        public EntityAnd(ModelNode.ModelSubNode modelSubNode, Element entityAndElement) {
            super(modelSubNode, entityAndElement);
            boolean useCache = "true".equalsIgnoreCase(entityAndElement.getAttribute("use-cache"));
            Document ownerDoc = entityAndElement.getOwnerDocument();
            if (!useCache) {
                UtilXml.addChildElement(entityAndElement, "use-iterator", ownerDoc);
            }
            String listName = UtilFormatOut.checkEmpty(entityAndElement.getAttribute("list"),
                    entityAndElement.getAttribute("list-name"));
            if (UtilValidate.isEmpty(listName)) {
                listName = "_LIST_ITERATOR_";
            }
            this.listName = listName;
            entityAndElement.setAttribute("list-name", this.listName);
            finder = new ByAndFinder(entityAndElement);
        }

        @Override
        public void accept(ModelActionVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        public ByAndFinder getFinder() {
            return finder;
        }

        public String getListName() {
            return listName;
        }

        @Override
        public void runAction(Map<String, Object> context) {
            try {
                context.put(this.listName, null);
                finder.runFind(context, WidgetWorker.getDelegator(context));
                Object obj = context.get(this.listName);
                if (obj != null && (obj instanceof EntityListIterator || obj instanceof ListIterator<?>)) {
                    ListIterator<? extends Map<String, ? extends Object>> listIt = UtilGenerics.cast(obj);
                    this.getModelSubNode().setListIterator(listIt, context);
                } else {
                    if (obj instanceof List<?>) {
                        List<? extends Map<String, ? extends Object>> list = UtilGenerics.checkList(obj);
                        this.getModelSubNode().setListIterator(list.listIterator(), context);
                    }
                }
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
     * @see <code>widget-tree.xsd</code>
     */
    public static class EntityCondition extends ModelTreeAction {
        private final ByConditionFinder finder;
        private final String listName;

        public EntityCondition(ModelNode.ModelSubNode modelSubNode, Element entityConditionElement) {
            super(modelSubNode, entityConditionElement);
            Document ownerDoc = entityConditionElement.getOwnerDocument();
            boolean useCache = "true".equalsIgnoreCase(entityConditionElement.getAttribute("use-cache"));
            if (!useCache) {
                UtilXml.addChildElement(entityConditionElement, "use-iterator", ownerDoc);
            }
            String listName = UtilFormatOut.checkEmpty(entityConditionElement.getAttribute("list"),
                    entityConditionElement.getAttribute("list-name"));
            if (UtilValidate.isEmpty(listName)) {
                listName = "_LIST_ITERATOR_";
            }
            this.listName = listName;
            entityConditionElement.setAttribute("list-name", this.listName);
            finder = new ByConditionFinder(entityConditionElement);
        }

        @Override
        public void accept(ModelActionVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        public ByConditionFinder getFinder() {
            return finder;
        }

        public String getListName() {
            return listName;
        }

        @Override
        public void runAction(Map<String, Object> context) {
            try {
                context.put(this.listName, null);
                finder.runFind(context, WidgetWorker.getDelegator(context));
                Object obj = context.get(this.listName);
                if (obj != null && (obj instanceof EntityListIterator || obj instanceof ListIterator<?>)) {
                    ListIterator<? extends Map<String, ? extends Object>> listIt = UtilGenerics.cast(obj);
                    this.getModelSubNode().setListIterator(listIt, context);
                } else {
                    if (obj instanceof List<?>) {
                        List<? extends Map<String, ? extends Object>> list = UtilGenerics.cast(obj);
                        this.getModelSubNode().setListIterator(list.listIterator(), context);
                    }
                }
            } catch (GeneralException e) {
                String errMsg = "Error doing entity query by condition: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
        }
    }

    /**
     * Models the &lt;script&gt; element.
     *
     * @see <code>widget-tree.xsd</code>
     */
    public static class Script extends ModelTreeAction {
        private final String location;
        private final String method;

        public Script(ModelNode modelNode, Element scriptElement) {
            super(modelNode, scriptElement);
            String scriptLocation = scriptElement.getAttribute("location");
            this.location = WidgetWorker.getScriptLocation(scriptLocation);
            this.method = WidgetWorker.getScriptMethodName(scriptLocation);
        }

        public Script(ModelNode.ModelSubNode modelSubNode, Element scriptElement) {
            super(modelSubNode, scriptElement);
            String scriptLocation = scriptElement.getAttribute("location");
            this.location = WidgetWorker.getScriptLocation(scriptLocation);
            this.method = WidgetWorker.getScriptMethodName(scriptLocation);
        }

        @Override
        public void accept(ModelActionVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        public String getLocation() {
            return location;
        }

        public String getMethod() {
            return method;
        }

        @Override
        public void runAction(Map<String, Object> context) {
            context.put("_LIST_ITERATOR_", null);
            if (location.endsWith(".xml")) {
                Map<String, Object> localContext = new HashMap<>();
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
            if (this.getModelSubNode() != null) {
                if (obj != null && (obj instanceof EntityListIterator || obj instanceof ListIterator<?>)) {
                    ListIterator<? extends Map<String, ? extends Object>> listIt = UtilGenerics.cast(obj);
                    this.getModelSubNode().setListIterator(listIt, context);
                } else {
                    if (obj instanceof List<?>) {
                        List<? extends Map<String, ? extends Object>> list = UtilGenerics.checkList(obj);
                        this.getModelSubNode().setListIterator(list.listIterator(), context);
                    }
                }
            }
        }
    }

    /**
     * Models the &lt;service&gt; element.
     *
     * @see <code>widget-tree.xsd</code>
     */
    public static class Service extends ModelTreeAction {
        private final FlexibleStringExpander autoFieldMapExdr;
        private final Map<FlexibleMapAccessor<Object>, Object> fieldMap;
        private final FlexibleStringExpander resultMapListNameExdr;
        private final FlexibleMapAccessor<Map<String, Object>> resultMapNameAcsr;
        private final FlexibleStringExpander resultMapValueNameExdr;
        private final FlexibleStringExpander serviceNameExdr;
        private final FlexibleStringExpander valueNameExdr;

        public Service(ModelNode modelNode, Element serviceElement) {
            super(modelNode, serviceElement);
            this.serviceNameExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("service-name"));
            this.resultMapNameAcsr = FlexibleMapAccessor.getInstance(serviceElement.getAttribute("result-map"));
            this.autoFieldMapExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("auto-field-map"));
            this.resultMapListNameExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("result-map-list"));
            this.resultMapValueNameExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("result-map-value"));
            this.valueNameExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("value"));
            this.fieldMap = EntityFinderUtil.makeFieldMap(serviceElement);
        }

        public Service(ModelNode.ModelSubNode modelSubNode, Element serviceElement) {
            super(modelSubNode, serviceElement);
            this.serviceNameExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("service-name"));
            this.resultMapNameAcsr = FlexibleMapAccessor.getInstance(serviceElement.getAttribute("result-map"));
            this.autoFieldMapExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("auto-field-map"));
            this.resultMapListNameExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("result-map-list"));
            this.resultMapValueNameExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("result-map-value"));
            this.valueNameExdr = FlexibleStringExpander.getInstance(serviceElement.getAttribute("value"));
            this.fieldMap = EntityFinderUtil.makeFieldMap(serviceElement);
        }

        @Override
        public void accept(ModelActionVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        public FlexibleStringExpander getAutoFieldMapExdr() {
            return autoFieldMapExdr;
        }

        public Map<FlexibleMapAccessor<Object>, Object> getFieldMap() {
            return fieldMap;
        }

        public FlexibleStringExpander getResultMapListNameExdr() {
            return resultMapListNameExdr;
        }

        public FlexibleMapAccessor<Map<String, Object>> getResultMapNameAcsr() {
            return resultMapNameAcsr;
        }

        public FlexibleStringExpander getResultMapValueNameExdr() {
            return resultMapValueNameExdr;
        }

        public FlexibleStringExpander getServiceNameExdr() {
            return serviceNameExdr;
        }

        public FlexibleStringExpander getValueNameExdr() {
            return valueNameExdr;
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
                    serviceContext = WidgetWorker.getDispatcher(context).getDispatchContext()
                            .makeValidContext(serviceNameExpanded, ModelService.IN_PARAM, context);
                } else {
                    serviceContext = new HashMap<>();
                }
                if (this.fieldMap != null) {
                    EntityFinderUtil.expandFieldMapToContext(this.fieldMap, context, serviceContext);
                }
                Map<String, Object> result = WidgetWorker.getDispatcher(context).runSync(serviceNameExpanded, serviceContext);
                ModelActionUtil.contextPutQueryStringOrAllResult(context, result, this.resultMapNameAcsr);
                String resultMapListName = resultMapListNameExdr.expandString(context);
                String resultMapValueName = resultMapValueNameExdr.expandString(context);
                String valueName = valueNameExdr.expandString(context);
                if (this.getModelSubNode() != null) {
                    if (UtilValidate.isNotEmpty(resultMapListName)) {
                        List<? extends Map<String, ? extends Object>> lst = UtilGenerics.checkList(result.get(resultMapListName));
                        if (lst != null) {
                            if (lst instanceof ListIterator<?>) {
                                ListIterator<? extends Map<String, ? extends Object>> listIt = UtilGenerics.cast(lst);
                                this.getModelSubNode().setListIterator(listIt, context);
                            } else {
                                this.getModelSubNode().setListIterator(lst.listIterator(), context);
                            }
                        }
                    }
                } else {
                    if (UtilValidate.isNotEmpty(resultMapValueName)) {
                        if (UtilValidate.isNotEmpty(valueName)) {
                            context.put(valueName, result.get(resultMapValueName));
                        } else {
                            Map<String, Object> resultMap = UtilGenerics.cast(result.get(resultMapValueName));
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
    }
}
