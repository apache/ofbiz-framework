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
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.Perl5Matcher;
import org.w3c.dom.Element;

/**
 * Abstract base class for the condition models.
 */
@SuppressWarnings("serial")
public abstract class ModelWidgetCondition implements Serializable {

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

    public static final String module = ModelWidgetCondition.class.getName();
    public static final ConditionFactory DEFAULT_CONDITION_FACTORY = new DefaultConditionFactory();

    private final ModelWidget modelWidget;
    private final Condition rootCondition;

    protected ModelWidgetCondition(ConditionFactory factory, ModelWidget modelWidget, Element conditionElement) {
        this.modelWidget = modelWidget;
        Element firstChildElement = UtilXml.firstChildElement(conditionElement);
        this.rootCondition = factory.newInstance(modelWidget, firstChildElement);
    }

    public boolean eval(Map<String, Object> context) {
        return rootCondition.eval(context);
    }

    public ModelWidget getModelWidget() {
        return modelWidget;
    }

    public static List<Condition> readSubConditions(ConditionFactory factory, ModelWidget modelWidget, Element conditionElement) {
        List<? extends Element> subElementList = UtilXml.childElementList(conditionElement);
        List<Condition> condList = new ArrayList<>(subElementList.size());
        for (Element subElement : subElementList) {
            condList.add(factory.newInstance(modelWidget, subElement));
        }
        return Collections.unmodifiableList(condList);
    }

    /**
     * Models the &lt;and&gt; element.
     *
     * @see <code>widget-common.xsd</code>
     */
    public static class And extends ModelWidgetCondition implements Condition {
        private final List<Condition> subConditions;

        private And(ConditionFactory factory, ModelWidget modelWidget, Element condElement) {
            super(factory, modelWidget, condElement);
            this.subConditions = readSubConditions(factory, modelWidget, condElement);
        }

        @Override
        public boolean eval(Map<String, Object> context) {
            // return false for the first one in the list that is false, basic and algo
            for (Condition subCondition : this.subConditions) {
                if (!subCondition.eval(context)) {
                    return false;
                }
            }
            return true;
        }
    }

    public static interface Condition {
        boolean eval(Map<String, Object> context);
    }

    /**
     * A factory for <code>Condition</code> instances.
     *
     */
    public static interface ConditionFactory {
        /**
         * Returns a new <code>Condition</code> instance built from <code>conditionElement</code>.
         *
         * @param modelWidget The <code>ModelWidget</code> that contains the <code>Condition</code> instance.
         * @param conditionElement The XML element used to build the <code>Condition</code> instance.
         * @return A new <code>Condition</code> instance built from <code>conditionElement</code>.
         * @throws IllegalArgumentException if no model was found for the XML element
         */
        Condition newInstance(ModelWidget modelWidget, Element conditionElement);
    }

    public static class DefaultConditionFactory implements ConditionFactory {
        public static final Condition TRUE = new Condition() {
            @Override
            public boolean eval(Map<String, Object> context) {
                return true;
            }
        };
        public static final Condition FALSE = new Condition() {
            @Override
            public boolean eval(Map<String, Object> context) {
                return false;
            }
        };

        @Override
        public Condition newInstance(ModelWidget modelWidget, Element conditionElement) {
            if (conditionElement == null) {
                return TRUE;
            }
            if ("and".equals(conditionElement.getNodeName())) {
                return new And(this, modelWidget, conditionElement);
            } else if ("xor".equals(conditionElement.getNodeName())) {
                return new Xor(this, modelWidget, conditionElement);
            } else if ("or".equals(conditionElement.getNodeName())) {
                return new Or(this, modelWidget, conditionElement);
            } else if ("not".equals(conditionElement.getNodeName())) {
                return new Not(this, modelWidget, conditionElement);
            } else if ("if-service-permission".equals(conditionElement.getNodeName())) {
                return new IfServicePermission(this, modelWidget, conditionElement);
            } else if ("if-has-permission".equals(conditionElement.getNodeName())) {
                return new IfHasPermission(this, modelWidget, conditionElement);
            } else if ("if-validate-method".equals(conditionElement.getNodeName())) {
                return new IfValidateMethod(this, modelWidget, conditionElement);
            } else if ("if-compare".equals(conditionElement.getNodeName())) {
                return new IfCompare(this, modelWidget, conditionElement);
            } else if ("if-compare-field".equals(conditionElement.getNodeName())) {
                return new IfCompareField(this, modelWidget, conditionElement);
            } else if ("if-regexp".equals(conditionElement.getNodeName())) {
                return new IfRegexp(this, modelWidget, conditionElement);
            } else if ("if-empty".equals(conditionElement.getNodeName())) {
                return new IfEmpty(this, modelWidget, conditionElement);
            } else if ("if-entity-permission".equals(conditionElement.getNodeName())) {
                return new IfEntityPermission(this, modelWidget, conditionElement);
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
    public static class IfCompare extends ModelWidgetCondition implements Condition {
        private final FlexibleMapAccessor<Object> fieldAcsr;
        private final FlexibleStringExpander formatExdr;
        private final String operator;
        private final String type;
        private final FlexibleStringExpander valueExdr;

        private IfCompare(ConditionFactory factory, ModelWidget modelWidget, Element condElement) {
            super(factory, modelWidget, condElement);
            String fieldAcsr = condElement.getAttribute("field");
            if (fieldAcsr.isEmpty()) {
                fieldAcsr = condElement.getAttribute("field-name");
            }
            this.fieldAcsr = FlexibleMapAccessor.getInstance(fieldAcsr);
            this.valueExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("value"));
            this.operator = condElement.getAttribute("operator");
            this.type = condElement.getAttribute("type");
            this.formatExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("format"));
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
            List<Object> messages = new LinkedList<>();
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
            return resultBool;
        }
    }

    /**
     * Models the &lt;if-compare-field&gt; element.
     *
     * @see <code>widget-common.xsd</code>
     */
    public static class IfCompareField extends ModelWidgetCondition implements Condition {
        private final FlexibleMapAccessor<Object> fieldAcsr;
        private final FlexibleStringExpander formatExdr;
        private final String operator;
        private final FlexibleMapAccessor<Object> toFieldAcsr;
        private final String type;

        private IfCompareField(ConditionFactory factory, ModelWidget modelWidget, Element condElement) {
            super(factory, modelWidget, condElement);
            String fieldAcsr = condElement.getAttribute("field");
            if (fieldAcsr.isEmpty()) {
                fieldAcsr = condElement.getAttribute("field-name");
            }
            this.fieldAcsr = FlexibleMapAccessor.getInstance(fieldAcsr);
            String toFieldAcsr = condElement.getAttribute("to-field");
            if (toFieldAcsr.isEmpty()) {
                toFieldAcsr = condElement.getAttribute("to-field-name");
            }
            this.toFieldAcsr = FlexibleMapAccessor.getInstance(toFieldAcsr);
            this.operator = condElement.getAttribute("operator");
            this.type = condElement.getAttribute("type");
            this.formatExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("format"));
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
            List<Object> messages = new LinkedList<>();
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
            return resultBool;
        }
    }

    /**
     * Models the &lt;if-empty&gt; element.
     *
     * @see <code>widget-common.xsd</code>
     */
    public static class IfEmpty extends ModelWidgetCondition implements Condition {
        private final FlexibleMapAccessor<Object> fieldAcsr;

        private IfEmpty(ConditionFactory factory, ModelWidget modelWidget, Element condElement) {
            super(factory, modelWidget, condElement);
            String fieldAcsr = condElement.getAttribute("field");
            if (fieldAcsr.isEmpty()) {
                fieldAcsr = condElement.getAttribute("field-name");
            }
            this.fieldAcsr = FlexibleMapAccessor.getInstance(fieldAcsr);
        }

        @Override
        public boolean eval(Map<String, Object> context) {
            Object fieldVal = this.fieldAcsr.get(context);
            return ObjectType.isEmpty(fieldVal);
        }
    }

    /**
     * Models the &lt;if-entity-permission&gt; element.
     *
     * @see <code>widget-common.xsd</code>
     */
    public static class IfEntityPermission extends ModelWidgetCondition implements Condition {
        private final EntityPermissionChecker permissionChecker;

        private IfEntityPermission(ConditionFactory factory, ModelWidget modelWidget, Element condElement) {
            super(factory, modelWidget, condElement);
            this.permissionChecker = new EntityPermissionChecker(condElement);
        }

        @Override
        public boolean eval(Map<String, Object> context) {
            return permissionChecker.runPermissionCheck(context);
        }
    }

    /**
     * Models the &lt;if-has-permission&gt; element.
     *
     * @see <code>widget-common.xsd</code>
     */
    public static class IfHasPermission extends ModelWidgetCondition implements Condition {
        private final FlexibleStringExpander actionExdr;
        private final FlexibleStringExpander permissionExdr;

        private IfHasPermission(ConditionFactory factory, ModelWidget modelWidget, Element condElement) {
            super(factory, modelWidget, condElement);
            this.permissionExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("permission"));
            this.actionExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("action"));
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
    }

    /**
     * Models the &lt;if-regexp&gt; element.
     *
     * @see <code>widget-common.xsd</code>
     */
    public static class IfRegexp extends ModelWidgetCondition implements Condition {
        private final FlexibleStringExpander exprExdr;
        private final FlexibleMapAccessor<Object> fieldAcsr;

        private IfRegexp(ConditionFactory factory, ModelWidget modelWidget, Element condElement) {
            super(factory, modelWidget, condElement);
            String fieldAcsr = condElement.getAttribute("field");
            if (fieldAcsr.isEmpty()) {
                fieldAcsr = condElement.getAttribute("field-name");
            }
            this.fieldAcsr = FlexibleMapAccessor.getInstance(fieldAcsr);
            this.exprExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("expr"));
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
                fieldString = (String) ObjectType.simpleTypeOrObjectConvert(fieldVal, "String", null, (TimeZone) context.get("timeZone"),
                        (Locale) context.get("locale"), true);
            } catch (GeneralException e) {
                Debug.logError(e, "Could not convert object to String, using empty String", module);
            }
            // always use an empty string by default
            if (fieldString == null) {
                fieldString = "";
            }
            PatternMatcher matcher = new Perl5Matcher();
            return matcher.matches(fieldString, pattern);
        }
    }

    /**
     * Models the &lt;if-service-permission&gt; element.
     *
     * @see <code>widget-common.xsd</code>
     */
    public static class IfServicePermission extends ModelWidgetCondition implements Condition {
        private final FlexibleStringExpander actionExdr;
        private final FlexibleStringExpander ctxMapExdr;
        private final FlexibleStringExpander resExdr;
        private final FlexibleStringExpander serviceExdr;

        private IfServicePermission(ConditionFactory factory, ModelWidget modelWidget, Element condElement) {
            super(factory, modelWidget, condElement);
            this.serviceExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("service-name"));
            this.actionExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("main-action"));
            this.ctxMapExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("context-map"));
            this.resExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("resource-description"));
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
                }
                catch (GenericServiceException e) {
                    Debug.logError(e, module);
                    return false;
                }
                if (ServiceUtil.isError(resp) || ServiceUtil.isFailure(resp)) {
                    Debug.logError(ServiceUtil.getErrorMessage(resp), module);
                    return false;
                }
                Boolean hasPermission = (Boolean) resp.get("hasPermission");
                if (hasPermission != null) {
                    return hasPermission;
                }
            }
            return false;
        }
    }

    /**
     * Models the &lt;if-validate-method&gt; element.
     *
     * @see <code>widget-common.xsd</code>
     */
    public static class IfValidateMethod extends ModelWidgetCondition implements Condition {
        private final FlexibleStringExpander classExdr;
        private final FlexibleMapAccessor<Object> fieldAcsr;
        private final FlexibleStringExpander methodExdr;

        private IfValidateMethod(ConditionFactory factory, ModelWidget modelWidget, Element condElement) {
            super(factory, modelWidget, condElement);
            String fieldAcsr = condElement.getAttribute("field");
            if (fieldAcsr.isEmpty()) {
                fieldAcsr = condElement.getAttribute("field-name");
            }
            this.fieldAcsr = FlexibleMapAccessor.getInstance(fieldAcsr);
            this.methodExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("method"));
            this.classExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("class"));
        }

        @Override
        public boolean eval(Map<String, Object> context) {
            String methodName = this.methodExdr.expandString(context);
            String className = this.classExdr.expandString(context);
            Object fieldVal = this.fieldAcsr.get(context);
            String fieldString = null;
            if (fieldVal != null) {
                try {
                    fieldString = (String) ObjectType.simpleTypeOrObjectConvert(fieldVal, "String", null,
                            (TimeZone) context.get("timeZone"), (Locale) context.get("locale"), true);
                } catch (GeneralException e) {
                    Debug.logError(e, "Could not convert object to String, using empty String", module);
                }
            }
            // always use an empty string by default
            if (fieldString == null) {
                fieldString = "";
            }
            Class<?>[] paramTypes = { String.class };
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
            return resultBool;
        }
    }

    /**
     * Models the &lt;not&gt; element.
     *
     * @see <code>widget-common.xsd</code>
     */
    public static class Not extends ModelWidgetCondition implements Condition {
        private final Condition subCondition;

        private Not(ConditionFactory factory, ModelWidget modelWidget, Element condElement) {
            super(factory, modelWidget, condElement);
            Element firstChildElement = UtilXml.firstChildElement(condElement);
            this.subCondition = factory.newInstance(modelWidget, firstChildElement);
        }

        @Override
        public boolean eval(Map<String, Object> context) {
            return !this.subCondition.eval(context);
        }
    }

    /**
     * Models the &lt;or&gt; element.
     *
     * @see <code>widget-common.xsd</code>
     */
    public static class Or extends ModelWidgetCondition implements Condition {
        private final List<Condition> subConditions;

        private Or(ConditionFactory factory, ModelWidget modelWidget, Element condElement) {
            super(factory, modelWidget, condElement);
            this.subConditions = readSubConditions(factory, modelWidget, condElement);
        }

        @Override
        public boolean eval(Map<String, Object> context) {
            // return true for the first one in the list that is true, basic or algo
            for (Condition subCondition : this.subConditions) {
                if (subCondition.eval(context)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Models the &lt;xor&gt; element.
     *
     * @see <code>widget-common.xsd</code>
     */
    public static class Xor extends ModelWidgetCondition implements Condition {
        private final List<Condition> subConditions;

        private Xor(ConditionFactory factory, ModelWidget modelWidget, Element condElement) {
            super(factory, modelWidget, condElement);
            this.subConditions = readSubConditions(factory, modelWidget, condElement);
        }

        @Override
        public boolean eval(Map<String, Object> context) {
            // if more than one is true stop immediately and return false; if all are false return false; if only one is true return true
            boolean foundOneTrue = false;
            for (Condition subCondition : this.subConditions) {
                if (subCondition.eval(context)) {
                    if (foundOneTrue) {
                        // now found two true, so return false
                        return false;
                    }
                    foundOneTrue = true;
                }
            }
            return foundOneTrue;
        }
    }
}