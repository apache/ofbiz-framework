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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javolution.util.FastList;

import org.apache.oro.text.regex.MalformedPatternException;
import org.ofbiz.base.util.CompilerMatcher;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entityext.permission.EntityPermissionChecker;
import org.ofbiz.minilang.operation.BaseCompare;
import org.ofbiz.security.Security;
import org.ofbiz.security.authz.Authorization;
import org.w3c.dom.Element;

/**
 * Widget Library - Screen model condition class
 */
public class ModelTreeCondition {
    public static final String module = ModelTreeCondition.class.getName();

    protected ModelTree modelTree;
    protected TreeCondition rootCondition;

    public ModelTreeCondition(ModelTree modelTree, Element conditionElement) {
        this.modelTree = modelTree;
        Element firstChildElement = UtilXml.firstChildElement(conditionElement);
        this.rootCondition = readCondition(modelTree, firstChildElement);
    }

    public boolean eval(Map<String, ? extends Object> context) {
        if (rootCondition == null) {
            return true;
        }
        return rootCondition.eval(context);
    }

    public static abstract class TreeCondition {
        protected ModelTree modelTree;

        public TreeCondition(ModelTree modelTree, Element conditionElement) {
            this.modelTree = modelTree;
        }

        public abstract boolean eval(Map<String, ? extends Object> context);
    }

    public static List<TreeCondition> readSubConditions(ModelTree modelTree, Element conditionElement) {
        List<TreeCondition> condList = FastList.newInstance();
        for (Element subElement: UtilXml.childElementList(conditionElement)) {
            condList.add(readCondition(modelTree, subElement));
        }
        return condList;
    }

    public static TreeCondition readCondition(ModelTree modelTree, Element conditionElement) {
        if (conditionElement == null) {
            return null;
        }
        if ("and".equals(conditionElement.getNodeName())) {
            return new And(modelTree, conditionElement);
        } else if ("xor".equals(conditionElement.getNodeName())) {
            return new Xor(modelTree, conditionElement);
        } else if ("or".equals(conditionElement.getNodeName())) {
            return new Or(modelTree, conditionElement);
        } else if ("not".equals(conditionElement.getNodeName())) {
            return new Not(modelTree, conditionElement);
        } else if ("if-has-permission".equals(conditionElement.getNodeName())) {
            return new IfHasPermission(modelTree, conditionElement);
        } else if ("if-validate-method".equals(conditionElement.getNodeName())) {
            return new IfValidateMethod(modelTree, conditionElement);
        } else if ("if-compare".equals(conditionElement.getNodeName())) {
            return new IfCompare(modelTree, conditionElement);
        } else if ("if-compare-field".equals(conditionElement.getNodeName())) {
            return new IfCompareField(modelTree, conditionElement);
        } else if ("if-regexp".equals(conditionElement.getNodeName())) {
            return new IfRegexp(modelTree, conditionElement);
        } else if ("if-empty".equals(conditionElement.getNodeName())) {
            return new IfEmpty(modelTree, conditionElement);
        } else if ("if-entity-permission".equals(conditionElement.getNodeName())) {
            return new IfEntityPermission(modelTree, conditionElement);
        } else {
            throw new IllegalArgumentException("Condition element not supported with name: " + conditionElement.getNodeName());
        }
    }

    public static class And extends TreeCondition {
        protected List<? extends TreeCondition> subConditions;

        public And(ModelTree modelTree, Element condElement) {
            super (modelTree, condElement);
            this.subConditions = readSubConditions(modelTree, condElement);
        }

        @Override
        public boolean eval(Map<String, ? extends Object> context) {
            // return false for the first one in the list that is false, basic and algo
            for (TreeCondition subCondition: subConditions) {
                if (!subCondition.eval(context)) {
                    return false;
                }
            }
            return true;
        }
    }

    public static class Xor extends TreeCondition {
        protected List<? extends TreeCondition> subConditions;

        public Xor(ModelTree modelTree, Element condElement) {
            super (modelTree, condElement);
            this.subConditions = readSubConditions(modelTree, condElement);
        }

        @Override
        public boolean eval(Map<String, ? extends Object> context) {
            // if more than one is true stop immediately and return false; if all are false return false; if only one is true return true
            boolean foundOneTrue = false;
            for (TreeCondition subCondition: subConditions) {
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
    }

    public static class Or extends TreeCondition {
        protected List<? extends TreeCondition> subConditions;

        public Or(ModelTree modelTree, Element condElement) {
            super (modelTree, condElement);
            this.subConditions = readSubConditions(modelTree, condElement);
        }

        @Override
        public boolean eval(Map<String, ? extends Object> context) {
            // return true for the first one in the list that is true, basic or algo
            for (TreeCondition subCondition: subConditions) {
                if (subCondition.eval(context)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class Not extends TreeCondition {
        protected TreeCondition subCondition;

        public Not(ModelTree modelTree, Element condElement) {
            super (modelTree, condElement);
            Element firstChildElement = UtilXml.firstChildElement(condElement);
            this.subCondition = readCondition(modelTree, firstChildElement);
        }

        @Override
        public boolean eval(Map<String, ? extends Object> context) {
            return !this.subCondition.eval(context);
        }
    }

    public static class IfHasPermission extends TreeCondition {
        protected FlexibleStringExpander permissionExdr;
        protected FlexibleStringExpander actionExdr;

        public IfHasPermission(ModelTree modelTree, Element condElement) {
            super (modelTree, condElement);
            this.permissionExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("permission"));
            this.actionExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("action"));
        }

        @Override
        public boolean eval(Map<String, ? extends Object> context) {
            // if no user is logged in, treat as if the user does not have permission
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            if (userLogin != null) {
                String permission = permissionExdr.expandString(context);
                String action = actionExdr.expandString(context);

                Authorization authz = (Authorization) context.get("authorization");
                Security security = (Security) context.get("security");
                if (UtilValidate.isNotEmpty(action)) {
                    //Debug.logWarning("Deprecated method hasEntityPermission() was called; the action field should no longer be used", module);
                    // run hasEntityPermission
                    if (security.hasEntityPermission(permission, action, userLogin)) {
                        return true;
                    }
                } else {
                    // run hasPermission
                    if (authz.hasPermission(userLogin.getString("userLoginId"), permission, context)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public static class IfValidateMethod extends TreeCondition {
        protected FlexibleMapAccessor<Object> fieldAcsr;
        protected FlexibleStringExpander methodExdr;
        protected FlexibleStringExpander classExdr;

        public IfValidateMethod(ModelTree modelTree, Element condElement) {
            super (modelTree, condElement);
            this.fieldAcsr = FlexibleMapAccessor.getInstance(condElement.getAttribute("field"));
            if (this.fieldAcsr.isEmpty()) this.fieldAcsr = FlexibleMapAccessor.getInstance(condElement.getAttribute("field-name"));
            this.methodExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("method"));
            this.classExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("class"));
        }

        @Override
        public boolean eval(Map<String, ? extends Object> context) {
            String methodName = this.methodExdr.expandString(context);
            String className = this.classExdr.expandString(context);

            Object fieldVal = this.fieldAcsr.get(context);
            String fieldString = null;
            if (fieldVal != null) {
                try {
                    fieldString = (String) ObjectType.simpleTypeConvert(fieldVal, "String", null, (TimeZone) context.get("timeZone"), (Locale) context.get("locale"), true);
                } catch (GeneralException e) {
                    Debug.logError(e, "Could not convert object to String, using empty String", module);
                }
            }

            // always use an empty string by default
            if (fieldString == null) fieldString = "";

            Class<?>[] paramTypes = new Class[] {String.class};
            Object[] params = new Object[] {fieldString};

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
                Debug.logError(e, "Error in IfValidationMethod " + methodName + " of class " + className + ", defaulting to false ", module);
            }

            return resultBool.booleanValue();
        }
    }

    public static class IfCompare extends TreeCondition {
        protected FlexibleMapAccessor<Object> fieldAcsr;
        protected FlexibleStringExpander valueExdr;

        protected String operator;
        protected String type;
        protected FlexibleStringExpander formatExdr;

        public IfCompare(ModelTree modelTree, Element condElement) {
            super (modelTree, condElement);
            this.fieldAcsr = FlexibleMapAccessor.getInstance(condElement.getAttribute("field"));
            if (this.fieldAcsr.isEmpty()) this.fieldAcsr = FlexibleMapAccessor.getInstance(condElement.getAttribute("field-name"));
            this.valueExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("value"));

            this.operator = condElement.getAttribute("operator");
            this.type = condElement.getAttribute("type");

            this.formatExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("format"));
        }

        @Override
        public boolean eval(Map<String, ? extends Object> context) {
            String value = this.valueExdr.expandString(context);
            String format = this.formatExdr.expandString(context);

            Object fieldVal = this.fieldAcsr.get(context);

            // always use an empty string by default
            if (fieldVal == null) {
                fieldVal = "";
            }

            List<Object> messages = FastList.newInstance();
            Boolean resultBool = BaseCompare.doRealCompare(fieldVal, value, operator, type, format, messages, null, null, true);
            if (messages.size() > 0) {
                messages.add(0, "Error with comparison in if-compare between field [" + fieldAcsr.toString() + "] with value [" + fieldVal + "] and value [" + value + "] with operator [" + operator + "] and type [" + type + "]: ");

                StringBuilder fullString = new StringBuilder();
                for (Object message: messages) {
                    fullString.append((String) message);
                }
                Debug.logWarning(fullString.toString(), module);

                throw new IllegalArgumentException(fullString.toString());
            }

            return resultBool.booleanValue();
        }
    }

    public static class IfCompareField extends TreeCondition {
        protected FlexibleMapAccessor<Object> fieldAcsr;
        protected FlexibleMapAccessor<Object> toFieldAcsr;

        protected String operator;
        protected String type;
        protected FlexibleStringExpander formatExdr;

        public IfCompareField(ModelTree modelTree, Element condElement) {
            super (modelTree, condElement);
            this.fieldAcsr = FlexibleMapAccessor.getInstance(condElement.getAttribute("field"));
            if (this.fieldAcsr.isEmpty()) this.fieldAcsr = FlexibleMapAccessor.getInstance(condElement.getAttribute("field-name"));
            this.toFieldAcsr = FlexibleMapAccessor.getInstance(condElement.getAttribute("to-field"));
            if (this.toFieldAcsr.isEmpty()) this.toFieldAcsr = FlexibleMapAccessor.getInstance(condElement.getAttribute("to-field-name"));

            this.operator = condElement.getAttribute("operator");
            this.type = condElement.getAttribute("type");

            this.formatExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("format"));
        }

        @Override
        public boolean eval(Map<String, ? extends Object> context) {
            String format = this.formatExdr.expandString(context);

            Object fieldVal = this.fieldAcsr.get(context);
            Object toFieldVal = this.toFieldAcsr.get(context);

            // always use an empty string by default
            if (fieldVal == null) {
                fieldVal = "";
            }

            List<Object> messages = FastList.newInstance();
            Boolean resultBool = BaseCompare.doRealCompare(fieldVal, toFieldVal, operator, type, format, messages, null, null, false);
            if (messages.size() > 0) {
                messages.add(0, "Error with comparison in if-compare-field between field [" + fieldAcsr.toString() + "] with value [" + fieldVal + "] and to-field [" + toFieldVal.toString() + "] with value [" + toFieldVal + "] with operator [" + operator + "] and type [" + type + "]: ");

                StringBuilder fullString = new StringBuilder();
                for (Object message: messages) {
                    fullString.append((String) message);
                }
                Debug.logWarning(fullString.toString(), module);

                throw new IllegalArgumentException(fullString.toString());
            }

            return resultBool.booleanValue();
        }
    }

    public static class IfRegexp extends TreeCondition {
        private transient static ThreadLocal<CompilerMatcher> compilerMatcher = CompilerMatcher.getThreadLocal();

        protected FlexibleMapAccessor<Object> fieldAcsr;
        protected FlexibleStringExpander exprExdr;

        public IfRegexp(ModelTree modelTree, Element condElement) {
            super (modelTree, condElement);
            this.fieldAcsr = FlexibleMapAccessor.getInstance(condElement.getAttribute("field"));
            if (this.fieldAcsr.isEmpty()) this.fieldAcsr = FlexibleMapAccessor.getInstance(condElement.getAttribute("field-name"));
            this.exprExdr = FlexibleStringExpander.getInstance(condElement.getAttribute("expr"));
        }

        @Override
        public boolean eval(Map<String, ? extends Object> context) {
            Object fieldVal = this.fieldAcsr.get(context);

            String fieldString = null;
            try {
                fieldString = (String) ObjectType.simpleTypeConvert(fieldVal, "String", null, (TimeZone) context.get("timeZone"), (Locale) context.get("locale"), true);
            } catch (GeneralException e) {
                Debug.logError(e, "Could not convert object to String, using empty String", module);
            }
            // always use an empty string by default
            if (fieldString == null) fieldString = "";

            try {
                return compilerMatcher.get().matches(fieldString, this.exprExdr.expandString(context));
            } catch (MalformedPatternException e) {
                String errMsg = "Error in evaluation in if-regexp in screen: " + e.toString();
                Debug.logError(e, errMsg, module);
                throw new IllegalArgumentException(errMsg);
            }
        }
    }

    public static class IfEmpty extends TreeCondition {
        protected FlexibleMapAccessor<Object> fieldAcsr;

        public IfEmpty(ModelTree modelTree, Element condElement) {
            super (modelTree, condElement);
            this.fieldAcsr = FlexibleMapAccessor.getInstance(condElement.getAttribute("field"));
            if (this.fieldAcsr.isEmpty()) this.fieldAcsr = FlexibleMapAccessor.getInstance(condElement.getAttribute("field-name"));
        }

        @Override
        public boolean eval(Map<String, ? extends Object> context) {
            Object fieldVal = this.fieldAcsr.get(context);
            return ObjectType.isEmpty(fieldVal);
        }
    }
    public static class IfEntityPermission extends TreeCondition {
        protected EntityPermissionChecker permissionChecker;

        public IfEntityPermission(ModelTree modelTree, Element condElement) {
            super (modelTree, condElement);
            this.permissionChecker = new EntityPermissionChecker(condElement);
        }

        @Override
        public boolean eval(Map<String, ? extends Object> context) {

            boolean passed = permissionChecker.runPermissionCheck(context);
            return passed;
        }
    }
}
