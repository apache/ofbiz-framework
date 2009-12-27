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
package org.ofbiz.base.util.string;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.PropertyNotFoundException;
import javax.el.PropertyNotWritableException;
import javax.el.ResourceBundleELResolver;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.collections.LocalizedMap;

/** Implements the Unified Expression Language (JSR-245). */
public class UelUtil {
    protected static final String module = UelUtil.class.getName();
    public static final String localizedMapLocaleKey = LocalizedMap.class.getName() + "_locale".replace(".", "_");
    protected static final ExpressionFactory exprFactory = JuelConnector.newExpressionFactory();
    protected static final ELResolver defaultResolver = new ExtendedCompositeResolver() {
        {
            add(new ExtendedMapResolver(false));
            add(new ExtendedListResolver(false));
            add(new ArrayELResolver(false));
            add(new NodeELResolver()); // Below the most common but must be kept above BeanELResolver
            add(new ResourceBundleELResolver());
            add(new BeanELResolver(false));
        }
    };

    /** Evaluates a Unified Expression Language expression and returns the result.
     * @param context Evaluation context (variables)
     * @param expression UEL expression
     * @return Result object
     * @throws <code>javax.el.*</code> exceptions
     */
    public static Object evaluate(Map<String, ? extends Object> context, String expression) {
        return evaluate(context, expression, Object.class);
    }

    /** Evaluates a Unified Expression Language expression and returns the result.
     * @param context Evaluation context (variables)
     * @param expression UEL expression
     * @param expectedType The expected object Class to return
     * @return Result object
     * @throws <code>javax.el.*</code> exceptions
     */
    @SuppressWarnings("unchecked")
    public static Object evaluate(Map<String, ? extends Object> context, String expression, Class expectedType) {
        ELContext elContext = new BasicContext(context);
        ValueExpression ve = exprFactory.createValueExpression(elContext, expression, expectedType);
        return ve.getValue(elContext);
    }

    /** Evaluates a Unified Expression Language expression and sets the resulting object
     * to the specified value.
     * @param context Evaluation context (variables)
     * @param expression UEL expression
     * @param expectedType The expected object Class to set
     * @throws <code>javax.el.*</code> exceptions
     */
    @SuppressWarnings("unchecked")
    public static void setValue(Map<String, Object> context, String expression, Class expectedType, Object value) {
        if (Debug.verboseOn()) {
            Debug.logVerbose("UelUtil.setValue invoked, expression = " + expression + ", value = " + value, module);
        }
        ELContext elContext = new BasicContext(context);
        ValueExpression ve = exprFactory.createValueExpression(elContext, expression, expectedType);
        ve.setValue(elContext, value);
    }

    /** Evaluates a Unified Expression Language expression and sets the resulting object
     * to null.
     * @param context Evaluation context (variables)
     * @param expression UEL expression
     * @throws <code>javax.el.*</code> exceptions
     */
    public static void removeValue(Map<String, Object> context, String expression) {
        if (Debug.verboseOn()) {
            Debug.logVerbose("UelUtil.removeValue invoked, expression = " + expression , module);
        }
        ELContext elContext = new BasicContext(context);
        ValueExpression ve = exprFactory.createValueExpression(elContext, expression, Object.class);
        ve.setValue(elContext, null);
    }

    protected static class BasicContext extends ELContext {
        protected final VariableMapper variableMapper;
        public BasicContext(Map<String, ? extends Object> context) {
            this.variableMapper = new BasicVariableMapper(context, this);
        }
        @Override
        public ELResolver getELResolver() {
            return defaultResolver;
        }
        @Override
        public FunctionMapper getFunctionMapper() {
            return UelFunctions.getFunctionMapper();
        }
        @Override
        public VariableMapper getVariableMapper() {
            return this.variableMapper;
        }
    }

    protected static class BasicVariableMapper extends VariableMapper {
        protected final ELContext elContext;
        protected final Map<String, Object> variables;
        protected BasicVariableMapper(Map<String, ? extends Object> context, ELContext parentContext) {
            this.variables = UtilGenerics.cast(context);
            this.elContext = parentContext;
        }

        /**
         * Returns a BasicValueExpression containing the value of the named variable.
         * Resolves against LocalizedMap if available.
         * @param variable the variable's name
         * @return a BasicValueExpression containing the variable's value or null if the variable is unknown
         */
        @Override
        public ValueExpression resolveVariable(String variable) {
            Object obj = UelUtil.resolveVariable(variable, this.variables, null);
            if (obj != null) {
                return new BasicValueExpression(obj);
            }
            return null;
        }
        @Override
        public ValueExpression setVariable(String variable, ValueExpression expression) {
            return new BasicValueExpression(this.variables.put(variable, expression.getValue(this.elContext)));
        }
    }

    @SuppressWarnings("serial")
    protected static class BasicValueExpression extends ValueExpression {
        protected Object object;
        public BasicValueExpression(Object object) {
            super();
            this.object = object;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            try {
                BasicValueExpression other = (BasicValueExpression) obj;
                if (this.object == other.object) {
                    return true;
                }
                return this.object.equals(other.object);
            } catch (Exception e) {}
            return false;
        }
        @Override
        public int hashCode() {
            return this.object == null ? 0 : this.object.hashCode();
        }
        @Override
        public Object getValue(ELContext context) {
            return this.object;
        }
        @Override
        public String getExpressionString() {
            return null;
        }
        @Override
        public boolean isLiteralText() {
            return false;
        }
        @Override
        public Class<?> getType(ELContext context) {
            return this.object == null ? null : this.object.getClass();
        }
        @Override
        public boolean isReadOnly(ELContext context) {
            return false;
        }
        @Override
        public void setValue(ELContext context, Object value) {
            this.object = value;
        }
        @Override
        public String toString() {
            return "ValueExpression(" + this.object + ")";
        }
        @Override
        public Class<?> getExpectedType() {
            return this.object == null ? null : this.object.getClass();
        }
    }

    /** Custom <code>CompositeELResolver</code> used to handle variable
     * auto-vivify.
     */
    protected static class ExtendedCompositeResolver extends CompositeELResolver {
        @Override
        public void setValue(ELContext context, Object base, Object property, Object val) {
            super.setValue(context, base, property, val);
            if (!context.isPropertyResolved() && base == null) {
                if (Debug.verboseOn()) {
                    Debug.logVerbose("ExtendedCompositeResolver.setValue: base = " + base + ", property = " + property + ", value = " + val, module);
                }
                ValueExpression ve = new BasicValueExpression(val);
                VariableMapper vm = context.getVariableMapper();
                vm.setVariable(property.toString(), ve);
                context.setPropertyResolved(true);
                return;
            }
        }
    }

    /** Custom <code>ListELResolver</code> used to handle OFBiz
     * <code>List</code> syntax.
     */
    protected static class ExtendedListResolver extends ListELResolver {
        protected boolean isReadOnly;
        public ExtendedListResolver(boolean isReadOnly) {
            super(isReadOnly);
            this.isReadOnly = isReadOnly;
        }
        @Override
        @SuppressWarnings("unchecked")
        public void setValue(ELContext context, Object base, Object property, Object val) {
            if (context == null) {
                throw new NullPointerException();
            }
            if (base != null && base instanceof List) {
                if (isReadOnly) {
                    throw new PropertyNotWritableException();
                }
                String str = property.toString();
                if ("add".equals(str)) {
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("ExtendedListResolver.setValue adding List element: base = " + base + ", property = " + property + ", value = " + val, module);
                    }
                    context.setPropertyResolved(true);
                    List list = (List) base;
                    list.add(val);
                } else if (str.startsWith("insert@")) {
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("ExtendedListResolver.setValue inserting List element: base = " + base + ", property = " + property + ", value = " + val, module);
                    }
                    context.setPropertyResolved(true);
                    String indexStr = str.replace("insert@", "");
                    int index = Integer.parseInt(indexStr);
                    List list = (List) base;
                    try {
                        list.add(index, val);
                    } catch (UnsupportedOperationException ex) {
                        throw new PropertyNotWritableException();
                    } catch (IndexOutOfBoundsException ex) {
                        throw new PropertyNotFoundException();
                    }
                } else {
                    super.setValue(context, base, property, val);
                }
            }
        }
    }

    /** Custom <code>MapELResolver</code> class used to accommodate
     * <code>LocalizedMap</code> instances.
     */
    protected static class ExtendedMapResolver extends MapELResolver {
        public ExtendedMapResolver(boolean isReadOnly) {
            super(isReadOnly);
        }
        @Override
        @SuppressWarnings("unchecked")
        public Object getValue(ELContext context, Object base, Object property) {
            if (context == null) {
                throw new NullPointerException();
            }
            if (base != null && base instanceof LocalizedMap) {
                context.setPropertyResolved(true);
                LocalizedMap map = (LocalizedMap) base;
                Locale locale = null;
                try {
                    VariableMapper vm = context.getVariableMapper();
                    ValueExpression ve = vm.resolveVariable(localizedMapLocaleKey);
                    if (ve != null) {
                        locale = (Locale) ve.getValue(context);
                    }
                    if (locale == null) {
                        ve = vm.resolveVariable("locale");
                        if (ve != null) {
                            locale = (Locale) ve.getValue(context);
                        }
                    }
                } catch (Exception e) {
                    Debug.logWarning("Exception thrown while getting LocalizedMap element, locale = " + locale + ", exception " + e, module);
                }
                if (locale == null) {
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("ExtendedMapResolver.getValue: unable to find Locale for LocalizedMap element, using default locale", module);
                    }
                    locale = Locale.getDefault();
                }
                return resolveVariable(property.toString(), (Map) map, locale);
            }
            if (base != null && base instanceof Map && property instanceof String) {
                context.setPropertyResolved(true);
                return resolveVariable(property.toString(), (Map) base, null);
            }
            return super.getValue(context, base, property);
        }
    }

    /** Evaluates a property <code>Object</code> and returns a new
     * <code>List</code> or <code>Map</code>. If <code>property</code>
     * is not a String object type and it evaluates to an integer value,
     * a new <code>List</code> instance is returned, otherwise a new
     * <code>Map</code> instance is returned.
     * @param property Property <code>Object</code> to be evaluated
     * @return New <code>List</code> or <code>Map</code>
     */
    public static Object autoVivifyListOrMap(Object property) {
        String str = property.toString();
        boolean isList = ("add".equals(str) || str.startsWith("insert@"));
        if (!isList && !"java.lang.String".equals(property.getClass().getName())) {
            Integer index = UtilMisc.toIntegerObject(property);
            isList = (index != null);
        }
        if (isList) {
            return FastList.newInstance();
        } else {
            return FastMap.newInstance();
        }
    }

    /** Prepares an expression for evaluation by UEL.<p>The OFBiz syntax is
     * converted to UEL-compatible syntax and the resulting expression is
     * returned.</p>
     * @see <a href="StringUtil.html#convertOperatorSubstitutions(java.lang.String)">StringUtil.convertOperatorSubstitutions(java.lang.String)</a>
     * @param expression Expression to be converted
     * @return Converted expression
     */
    public static String prepareExpression(String expression) {
        String result = StringUtil.convertOperatorSubstitutions(expression);
        result = result.replace("[]", "['add']");
        int openBrace = result.indexOf("[+");
        int closeBrace = (openBrace == -1 ? -1 : result.indexOf(']', openBrace));
        if (closeBrace != -1) {
            String base = result.substring(0, openBrace);
            String property = result.substring(openBrace+2, closeBrace).trim();
            String end = result.substring(closeBrace + 1);
            result = base + "['insert@" + property + "']" + end;
        }
        return result;
    }

    public static Object resolveVariable(String variable, Map<String, Object> variables, Locale locale) {
        Object obj = null;
        String createObjectType = null;
        String name = variable;
        if (variable.contains("$")) {
            if (variable.endsWith("$string")) {
                name = variable.substring(0, variable.length() - 7);
                createObjectType = "string";
            } else if (variable.endsWith("$null")) {
                name = variable.substring(0, variable.length() - 5);
                createObjectType = "null";
            } else if (variable.endsWith("$boolean")) {
                name = variable.substring(0, variable.length() - 8);
                createObjectType = "boolean";
            } else if (variable.endsWith("$integer")) {
                name = variable.substring(0, variable.length() - 8);
                createObjectType = "integer";
            } else if (variable.endsWith("$long")) {
                name = variable.substring(0, variable.length() - 5);
                createObjectType = "long";
            } else if (variable.endsWith("$double")) {
                name = variable.substring(0, variable.length() - 7);
                createObjectType = "double";
            } else if (variable.endsWith("$bigDecimal")) {
                name = variable.substring(0, variable.length() - 11);
                createObjectType = "bigDecimal";
            }
        }
        if (variables instanceof LocalizedMap) {
            if (locale == null) {
                locale = (Locale) variables.get(localizedMapLocaleKey);
                if (locale == null) {
                    locale = (Locale) variables.get("locale");
                    if (locale == null) {
                        locale = Locale.getDefault();
                    }
                }
            }
            obj = ((LocalizedMap<?>) variables).get(name, locale);
        } else {
            obj = variables.get(name);
        }
        if (obj != null) {
            return obj;
        }
        if (createObjectType != null) {
            if ("string".equals(createObjectType)) {
                return "";
            } else if ("null".equals(createObjectType)) {
                return null;
            } else if ("boolean".equals(createObjectType)) {
                return Boolean.FALSE;
            } else if ("integer".equals(createObjectType)) {
                return Integer.valueOf(0);
            } else if ("long".equals(createObjectType)) {
                return Long.valueOf(0);
            } else if ("double".equals(createObjectType)) {
                return Double.valueOf(0);
            } else if ("bigDecimal".equals(createObjectType)) {
                return BigDecimal.ZERO;
            }
        }
        return null;
    }

}
