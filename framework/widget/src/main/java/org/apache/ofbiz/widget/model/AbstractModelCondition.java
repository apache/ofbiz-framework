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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.Perl5Matcher;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.PatternFactory;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entityext.permission.EntityPermissionChecker;
import org.apache.ofbiz.minilang.operation.BaseCompare;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.w3c.dom.Element;

/**
 * Abstract base class for the condition models.
 */
@SuppressWarnings("serial")
public abstract class AbstractModelCondition implements Serializable, ModelCondition {

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

    public static final String module = AbstractModelCondition.class.getName();
    public static final ModelConditionFactory DEFAULT_CONDITION_FACTORY = new DefaultConditionFactory();

    public static List<ModelCondition> readSubConditions(ModelConditionFactory factory, ModelWidget modelWidget,
            Element conditionElement) {
        List<? extends Element> subElementList = UtilXml.childElementList(conditionElement);
        List<ModelCondition> condList = new ArrayList<ModelCondition>(subElementList.size());
        for (Element subElement : subElementList) {
            condList.add(factory.newInstance(modelWidget, subElement));
        }
        return Collections.unmodifiableList(condList);
    }

    private final ModelWidget modelWidget;

    protected AbstractModelCondition(ModelConditionFactory factory, ModelWidget modelWidget, Element conditionElement) {
        this.modelWidget = modelWidget;
    }

    public ModelWidget getModelWidget() {
        return modelWidget;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        ModelConditionVisitor visitor = new XmlWidgetConditionVisitor(sb);
        try {
            accept(visitor);
        } catch (Exception e) {
            Debug.logWarning(e, "Exception thrown in XmlWidgetConditionVisitor: ", module);
        }
        return sb.toString();
    }

    /**
     * Models the &lt;and&gt; element.
     * 
     * @see <code>widget-common.xsd</code>
     */
    public static class And extends AbstractModelCondition {
        private final List<ModelCondition> subConditions;

        private And(ModelConditionFactory factory, ModelWidget modelWidget, Element condElement) {
            super(factory, modelWidget, condElement);
            this.subConditions = readSubConditions(factory, modelWidget, condElement);
        }

        @Override
        public void accept(ModelConditionVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public boolean eval(Map<String, Object> context) {
            // return false for the first one in the list that is false, basic and algo
            for (ModelCondition subCondition : this.subConditions) {
                if (!subCondition.eval(context)) {
                    return false;
                }
            }
            return true;
        }

        public List<ModelCondition> getSubConditions() {
            return subConditions;
        }
    }

    /**
     * A <code>ModelCondition</code> factory. This factory handles elements
     * common to all widgets that support conditions. Widgets that have
     * specialized conditions can extend this class.
     *
     */
    public static class DefaultConditionFactory implements ModelConditionFactory {
        public static final ModelCondition TRUE = new ModelCondition() {
            @Override
            public boolean eval(Map<String, Object> context) {
                return true;
            }

            @Override
            public void accept(ModelConditionVisitor visitor) throws Exception {
            }
        };

        public static final ModelCondition FALSE = new ModelCondition() {
            @Override
            public boolean eval(Map<String, Object> context) {
                return false;
            }

            @Override
            public void accept(ModelConditionVisitor visitor) throws Exception {
            }
        };

        public ModelCondition newInstance(ModelWidget modelWidget, Element conditionElement) {
            return newInstance(this, modelWidget, conditionElement);
        }

        // TODO: Test extended factory
        protected ModelCondition newInstance(ModelConditionFactory factory, ModelWidget modelWidget, Element conditionElement) {
            if (conditionElement == null) {
                return TRUE;
            }
            String nodeName = conditionElement.getNodeName();
            if ("and".equals(nodeName)) {
                return new And(factory, modelWidget, conditionElement);
            } else if ("xor".equals(nodeName)) {
                return new Xor(factory, modelWidget, conditionElement);
            } else if ("or".equals(nodeName)) {
                return new Or(factory, modelWidget, conditionElement);
            } else if ("not".equals(nodeName)) {
                return new Not(factory, modelWidget, conditionElement);
            } else if ("if-service-permission".equals(nodeName)) {
                return new IfServicePermission(factory, modelWidget, conditionElement);
            } else if ("if-has-permission".equals(nodeName)) {
                return new IfHasPermission(factory, modelWidget, conditionElement);
            } else if ("if-validate-method".equals(nodeName)) {
                return new IfValidateMethod(factory, modelWidget, conditionElement);
            } else if ("if-compare".equals(nodeName)) {
                return new IfCompare(factory, modelWidget, conditionElement);
            } else if ("if-compare-field".equals(nodeName)) {
                return new IfCompareField(factory, modelWidget, conditionElement);
            } else if ("if-regexp".equals(nodeName)) {
                return new IfRegexp(factory, modelWidget, conditionElement);
            } else if ("if-empty".equals(nodeName)) {
                return new IfEmpty(factory, modelWidget, conditionElement);
            } else if ("if-entity-permission".equals(nodeName)) {
                return new IfEntityPermission(factory, modelWidget, conditionElement);
            } else {
                throw new IllegalArgumentException("Condition element not supported with name: " + conditionElement.getNodeName());
            }
        }
    }

    /**
     * Models the &lt;if-compare&gt; element.
     * 
     * @see <code>widget-common.xsd</code>
     */
    public static class IfCompare extends AbstractModelCondition {
        private final FlexibleMapAccessor<Object> fieldAcsr;
        private final FlexibleStringExpander formatExdr;
        private final String operator;
        private final String type;
        private final FlexibleStringExpander valueExdr;

        private IfCompare(ModelConditionFactory factory, ModelWidget modelWidget, Element condElement) {
            super(factory, modelWidget, condElement);
            String fieldAcsr = condElement.getAttribute("field");
            if (fieldAcsr.isEmpty())
                fieldAcsr = condElement.getAttribute("field-name");
            this.fieldAcsr = FlexibleMapAccessor.getInstance(fieldAcsr);
            this.valueExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("value"));
            this.operator = condElement.getAttribute("operator");
            this.type = condElement.getAttribute("type");
            this.formatExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("format"));
        }

        @Override
        public void accept(ModelConditionVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public boolean eval(Map<String, Object> context) {
            String value = this.valueExdr.expandString(context);
            String format = this.formatExdr.expandString(context);
            Object fieldVal = this.fieldAcsr.get(context);
            // always use an empty string by default
            if (fieldVal == null) {
                fieldVal = "";
            }
            List<Object> messages = new LinkedList<Object>();
            Boolean resultBool = BaseCompare.doRealCompare(fieldVal, value, operator, type, format, messages, null, null, true);
            if (messages.size() > 0) {
                messages.add(0, "Error with comparison in if-compare between field [" + fieldAcsr.toString() + "] with value ["
                        + fieldVal + "] and value [" + value + "] with operator [" + operator + "] and type [" + type + "]: ");

                StringBuilder fullString = new StringBuilder();
                for (Object item : messages) {
                    fullString.append(item.toString());
                }
                Debug.logWarning(fullString.toString(), module);
                throw new IllegalArgumentException(fullString.toString());
            }
            return resultBool.booleanValue();
        }

        public FlexibleMapAccessor<Object> getFieldAcsr() {
            return fieldAcsr;
        }

        public FlexibleStringExpander getFormatExdr() {
            return formatExdr;
        }

        public String getOperator() {
            return operator;
        }

        public String getType() {
            return type;
        }

        public FlexibleStringExpander getValueExdr() {
            return valueExdr;
        }
    }

    /**
     * Models the &lt;if-compare-field&gt; element.
     * 
     * @see <code>widget-common.xsd</code>
     */
    public static class IfCompareField extends AbstractModelCondition {
        private final FlexibleMapAccessor<Object> fieldAcsr;
        private final FlexibleStringExpander formatExdr;
        private final String operator;
        private final FlexibleMapAccessor<Object> toFieldAcsr;
        private final String type;

        private IfCompareField(ModelConditionFactory factory, ModelWidget modelWidget, Element condElement) {
            super(factory, modelWidget, condElement);
            String fieldAcsr = condElement.getAttribute("field");
            if (fieldAcsr.isEmpty())
                fieldAcsr = condElement.getAttribute("field-name");
            this.fieldAcsr = FlexibleMapAccessor.getInstance(fieldAcsr);
            String toFieldAcsr = condElement.getAttribute("to-field");
            if (toFieldAcsr.isEmpty())
                toFieldAcsr = condElement.getAttribute("to-field-name");
            this.toFieldAcsr = FlexibleMapAccessor.getInstance(toFieldAcsr);
            this.operator = condElement.getAttribute("operator");
            this.type = condElement.getAttribute("type");
            this.formatExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("format"));
        }

        @Override
        public void accept(ModelConditionVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public boolean eval(Map<String, Object> context) {
            String format = this.formatExdr.expandString(context);
            Object fieldVal = this.fieldAcsr.get(context);
            Object toFieldVal = this.toFieldAcsr.get(context);
            // always use an empty string by default
            if (fieldVal == null) {
                fieldVal = "";
            }
            List<Object> messages = new LinkedList<Object>();
            Boolean resultBool = BaseCompare.doRealCompare(fieldVal, toFieldVal, operator, type, format, messages, null, null,
                    false);
            if (messages.size() > 0) {
                messages.add(0, "Error with comparison in if-compare-field between field [" + fieldAcsr.toString()
                        + "] with value [" + fieldVal + "] and to-field [" + toFieldAcsr.toString() + "] with value ["
                        + toFieldVal + "] with operator [" + operator + "] and type [" + type + "]: ");

                StringBuilder fullString = new StringBuilder();
                for (Object item : messages) {
                    fullString.append(item.toString());
                }
                Debug.logWarning(fullString.toString(), module);
                throw new IllegalArgumentException(fullString.toString());
            }
            return resultBool.booleanValue();
        }

        public FlexibleMapAccessor<Object> getFieldAcsr() {
            return fieldAcsr;
        }

        public FlexibleStringExpander getFormatExdr() {
            return formatExdr;
        }

        public String getOperator() {
            return operator;
        }

        public FlexibleMapAccessor<Object> getToFieldAcsr() {
            return toFieldAcsr;
        }

        public String getType() {
            return type;
        }
    }

    /**
     * Models the &lt;if-empty&gt; element.
     * 
     * @see <code>widget-common.xsd</code>
     */
    public static class IfEmpty extends AbstractModelCondition {
        private final FlexibleMapAccessor<Object> fieldAcsr;

        private IfEmpty(ModelConditionFactory factory, ModelWidget modelWidget, Element condElement) {
            super(factory, modelWidget, condElement);
            String fieldAcsr = condElement.getAttribute("field");
            if (fieldAcsr.isEmpty())
                fieldAcsr = condElement.getAttribute("field-name");
            this.fieldAcsr = FlexibleMapAccessor.getInstance(fieldAcsr);
        }

        @Override
        public void accept(ModelConditionVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public boolean eval(Map<String, Object> context) {
            Object fieldVal = this.fieldAcsr.get(context);
            return ObjectType.isEmpty(fieldVal);
        }

        public FlexibleMapAccessor<Object> getFieldAcsr() {
            return fieldAcsr;
        }

    }

    /**
     * Models the &lt;if-entity-permission&gt; element.
     * 
     * @see <code>widget-common.xsd</code>
     */
    public static class IfEntityPermission extends AbstractModelCondition {
        private final EntityPermissionChecker permissionChecker;

        private IfEntityPermission(ModelConditionFactory factory, ModelWidget modelWidget, Element condElement) {
            super(factory, modelWidget, condElement);
            this.permissionChecker = new EntityPermissionChecker(condElement);
        }

        @Override
        public void accept(ModelConditionVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public boolean eval(Map<String, Object> context) {
            return permissionChecker.runPermissionCheck(context);
        }

        public EntityPermissionChecker getPermissionChecker() {
            return permissionChecker;
        }
    }

    /**
     * Models the &lt;if-has-permission&gt; element.
     * 
     * @see <code>widget-common.xsd</code>
     */
    public static class IfHasPermission extends AbstractModelCondition {
        private final FlexibleStringExpander actionExdr;
        private final FlexibleStringExpander permissionExdr;

        private IfHasPermission(ModelConditionFactory factory, ModelWidget modelWidget, Element condElement) {
            super(factory, modelWidget, condElement);
            this.permissionExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("permission"));
            this.actionExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("action"));
        }

        @Override
        public void accept(ModelConditionVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public boolean eval(Map<String, Object> context) {
            // if no user is logged in, treat as if the user does not have permission
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            if (userLogin != null) {
                String permission = permissionExdr.expandString(context);
                String action = actionExdr.expandString(context);
                Security security = (Security) context.get("security");
                if (UtilValidate.isNotEmpty(action)) {
                    // run hasEntityPermission
                    if (security.hasEntityPermission(permission, action, userLogin)) {
                        return true;
                    }
                } else {
                    // run hasPermission
                    if (security.hasPermission(permission, userLogin)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public FlexibleStringExpander getActionExdr() {
            return actionExdr;
        }

        public FlexibleStringExpander getPermissionExdr() {
            return permissionExdr;
        }
    }

    /**
     * Models the &lt;if-regexp&gt; element.
     * 
     * @see <code>widget-common.xsd</code>
     */
    public static class IfRegexp extends AbstractModelCondition {
        private final FlexibleStringExpander exprExdr;
        private final FlexibleMapAccessor<Object> fieldAcsr;

        private IfRegexp(ModelConditionFactory factory, ModelWidget modelWidget, Element condElement) {
            super(factory, modelWidget, condElement);
            String fieldAcsr = condElement.getAttribute("field");
            if (fieldAcsr.isEmpty())
                fieldAcsr = condElement.getAttribute("field-name");
            this.fieldAcsr = FlexibleMapAccessor.getInstance(fieldAcsr);
            this.exprExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("expr"));
        }

        @Override
        public void accept(ModelConditionVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public boolean eval(Map<String, Object> context) {
            Object fieldVal = this.fieldAcsr.get(context);
            String expr = this.exprExdr.expandString(context);
            Pattern pattern;
            try {
                pattern = PatternFactory.createOrGetPerl5CompiledPattern(expr, true);
            } catch (MalformedPatternException e) {
                String errMsg = "Error in evaluation in if-regexp in screen: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
            String fieldString = null;
            try {
                fieldString = (String) ObjectType.simpleTypeConvert(fieldVal, "String", null, (TimeZone) context.get("timeZone"),
                        (Locale) context.get("locale"), true);
            } catch (GeneralException e) {
                Debug.logError(e, "Could not convert object to String, using empty String", module);
            }
            // always use an empty string by default
            if (fieldString == null)
                fieldString = "";
            PatternMatcher matcher = new Perl5Matcher();
            return matcher.matches(fieldString, pattern);
        }

        public FlexibleStringExpander getExprExdr() {
            return exprExdr;
        }

        public FlexibleMapAccessor<Object> getFieldAcsr() {
            return fieldAcsr;
        }
    }

    /**
     * Models the &lt;if-service-permission&gt; element.
     * 
     * @see <code>widget-common.xsd</code>
     */
    public static class IfServicePermission extends AbstractModelCondition {
        private final FlexibleStringExpander actionExdr;
        private final FlexibleStringExpander ctxMapExdr;
        private final FlexibleStringExpander resExdr;
        private final FlexibleStringExpander serviceExdr;

        private IfServicePermission(ModelConditionFactory factory, ModelWidget modelWidget, Element condElement) {
            super(factory, modelWidget, condElement);
            this.serviceExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("service-name"));
            this.actionExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("main-action"));
            this.ctxMapExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("context-map"));
            this.resExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("resource-description"));
        }

        @Override
        public void accept(ModelConditionVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public boolean eval(Map<String, Object> context) {
            // if no user is logged in, treat as if the user does not have permission
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            if (userLogin != null) {
                String serviceName = serviceExdr.expandString(context);
                String mainAction = actionExdr.expandString(context);
                String contextMap = ctxMapExdr.expandString(context);
                String resource = resExdr.expandString(context);
                if (UtilValidate.isEmpty(resource)) {
                    resource = serviceName;
                }
                if (UtilValidate.isEmpty(serviceName)) {
                    Debug.logWarning("No permission service-name specified!", module);
                    return false;
                }
                Map<String, Object> serviceContext = UtilGenerics.toMap(context.get(contextMap));
                if (serviceContext != null) {
                    // copy the required internal fields
                    serviceContext.put("userLogin", context.get("userLogin"));
                    serviceContext.put("locale", context.get("locale"));
                } else {
                    serviceContext = context;
                }
                // get the service engine objects
                LocalDispatcher dispatcher = (LocalDispatcher) context.get("dispatcher");
                DispatchContext dctx = dispatcher.getDispatchContext();
                // get the service
                ModelService permService;
                try {
                    permService = dctx.getModelService(serviceName);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    return false;
                }
                if (permService != null) {
                    // build the context
                    Map<String, Object> svcCtx = permService.makeValid(serviceContext, ModelService.IN_PARAM);
                    svcCtx.put("resourceDescription", resource);
                    if (UtilValidate.isNotEmpty(mainAction)) {
                        svcCtx.put("mainAction", mainAction);
                    }
                    // invoke the service
                    Map<String, Object> resp;
                    try {
                        resp = dispatcher.runSync(permService.name, svcCtx, 300, true);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                        return false;
                    }
                    if (ServiceUtil.isError(resp) || ServiceUtil.isFailure(resp)) {
                        Debug.logError(ServiceUtil.getErrorMessage(resp), module);
                        return false;
                    }
                    Boolean hasPermission = (Boolean) resp.get("hasPermission");
                    if (hasPermission != null) {
                        return hasPermission.booleanValue();
                    }
                }
            }
            return false;
        }

        public FlexibleStringExpander getActionExdr() {
            return actionExdr;
        }

        public FlexibleStringExpander getCtxMapExdr() {
            return ctxMapExdr;
        }

        public FlexibleStringExpander getResExdr() {
            return resExdr;
        }

        public FlexibleStringExpander getServiceExdr() {
            return serviceExdr;
        }
    }

    /**
     * Models the &lt;if-validate-method&gt; element.
     * 
     * @see <code>widget-common.xsd</code>
     */
    public static class IfValidateMethod extends AbstractModelCondition {
        private final FlexibleStringExpander classExdr;
        private final FlexibleMapAccessor<Object> fieldAcsr;
        private final FlexibleStringExpander methodExdr;

        private IfValidateMethod(ModelConditionFactory factory, ModelWidget modelWidget, Element condElement) {
            super(factory, modelWidget, condElement);
            String fieldAcsr = condElement.getAttribute("field");
            if (fieldAcsr.isEmpty())
                fieldAcsr = condElement.getAttribute("field-name");
            this.fieldAcsr = FlexibleMapAccessor.getInstance(fieldAcsr);
            this.methodExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("method"));
            this.classExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("class"));
        }

        @Override
        public void accept(ModelConditionVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public boolean eval(Map<String, Object> context) {
            String methodName = this.methodExdr.expandString(context);
            String className = this.classExdr.expandString(context);
            Object fieldVal = this.fieldAcsr.get(context);
            String fieldString = null;
            if (fieldVal != null) {
                try {
                    fieldString = (String) ObjectType.simpleTypeConvert(fieldVal, "String", null,
                            (TimeZone) context.get("timeZone"), (Locale) context.get("locale"), true);
                } catch (GeneralException e) {
                    Debug.logError(e, "Could not convert object to String, using empty String", module);
                }
            }
            // always use an empty string by default
            if (fieldString == null)
                fieldString = "";
            Class<?>[] paramTypes = new Class[] { String.class };
            Object[] params = new Object[] { fieldString };
            Class<?> valClass;
            try {
                valClass = ObjectType.loadClass(className);
            } catch (ClassNotFoundException cnfe) {
                Debug.logError("Could not find validation class: " + className, module);
                return false;
            }
            Method valMethod;
            try {
                valMethod = valClass.getMethod(methodName, paramTypes);
            } catch (NoSuchMethodException cnfe) {
                Debug.logError("Could not find validation method: " + methodName + " of class " + className, module);
                return false;
            }
            Boolean resultBool = Boolean.FALSE;
            try {
                resultBool = (Boolean) valMethod.invoke(null, params);
            } catch (Exception e) {
                Debug.logError(e, "Error in IfValidationMethod " + methodName + " of class " + className
                        + ", defaulting to false ", module);
            }
            return resultBool.booleanValue();
        }

        public FlexibleStringExpander getClassExdr() {
            return classExdr;
        }

        public FlexibleMapAccessor<Object> getFieldAcsr() {
            return fieldAcsr;
        }

        public FlexibleStringExpander getMethodExdr() {
            return methodExdr;
        }

    }

    /**
     * Models the &lt;not&gt; element.
     * 
     * @see <code>widget-common.xsd</code>
     */
    public static class Not extends AbstractModelCondition {
        private final ModelCondition subCondition;

        private Not(ModelConditionFactory factory, ModelWidget modelWidget, Element condElement) {
            super(factory, modelWidget, condElement);
            Element firstChildElement = UtilXml.firstChildElement(condElement);
            this.subCondition = factory.newInstance(modelWidget, firstChildElement);
        }

        @Override
        public void accept(ModelConditionVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public boolean eval(Map<String, Object> context) {
            return !this.subCondition.eval(context);
        }

        public ModelCondition getSubCondition() {
            return subCondition;
        }
    }

    /**
     * Models the &lt;or&gt; element.
     * 
     * @see <code>widget-common.xsd</code>
     */
    public static class Or extends AbstractModelCondition {
        private final List<ModelCondition> subConditions;

        private Or(ModelConditionFactory factory, ModelWidget modelWidget, Element condElement) {
            super(factory, modelWidget, condElement);
            this.subConditions = readSubConditions(factory, modelWidget, condElement);
        }

        @Override
        public void accept(ModelConditionVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public boolean eval(Map<String, Object> context) {
            // return true for the first one in the list that is true, basic or algo
            for (ModelCondition subCondition : this.subConditions) {
                if (subCondition.eval(context)) {
                    return true;
                }
            }
            return false;
        }

        public List<ModelCondition> getSubConditions() {
            return subConditions;
        }
    }

    /**
     * Models the &lt;xor&gt; element.
     * 
     * @see <code>widget-common.xsd</code>
     */
    public static class Xor extends AbstractModelCondition {
        private final List<ModelCondition> subConditions;

        private Xor(ModelConditionFactory factory, ModelWidget modelWidget, Element condElement) {
            super(factory, modelWidget, condElement);
            this.subConditions = readSubConditions(factory, modelWidget, condElement);
        }

        @Override
        public void accept(ModelConditionVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        @Override
        public boolean eval(Map<String, Object> context) {
            // if more than one is true stop immediately and return false; if all are false return false; if only one is true return true
            boolean foundOneTrue = false;
            for (ModelCondition subCondition : this.subConditions) {
                if (subCondition.eval(context)) {
                    if (foundOneTrue) {
                        // now found two true, so return false
                        return false;
                    } else {
                        foundOneTrue = true;
                    }
                }
            }
            return foundOneTrue;
        }

        public List<ModelCondition> getSubConditions() {
            return subConditions;
        }
    }
}
