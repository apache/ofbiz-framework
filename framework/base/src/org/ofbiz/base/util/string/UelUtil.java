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

import java.lang.reflect.Method;
import java.util.Map;
import javax.el.*;

import javolution.util.FastMap;

/** Implements the Unified Expression Language (JSR-245). */
public class UelUtil {
    
    public static final FunctionMapper functionMapper = new Functions();
    protected static final ExpressionFactory exprFactory = new de.odysseus.el.ExpressionFactoryImpl();
    protected static final ELResolver defaultResolver = new CompositeELResolver() {
        {
            add(new ArrayELResolver(false));
            add(new ListELResolver(false));
            add(new MapELResolver(false));
            add(new ResourceBundleELResolver());
            add(new BeanELResolver(false));
        }
    };

    /** Evaluates a Unified Expression Language expression and returns the result.
     * @param context Evaluation context (variables)
     * @param expression UEL expression
     * @return Result object
     */
    public static Object evaluate(Map<String, ? extends Object> context, String expression) {
        ELContext elContext = new BasicContext(context);
        ValueExpression ve = exprFactory.createValueExpression(elContext, expression, Object.class);
        Object obj = null;
        try {
            obj = ve.getValue(elContext);
        } catch (Exception e) {}
        return obj;
    }

    protected static class BasicContext extends ELContext {
        protected VariableMapper variables = null;
        protected BasicContext() {}
        public BasicContext(Map<String, ? extends Object> context) {
            this.variables = new Variables(context);
        }
        public ELResolver getELResolver() {
            return defaultResolver;
        }
        public FunctionMapper getFunctionMapper() {
            return functionMapper;
        }
        public VariableMapper getVariableMapper() {
            return this.variables;
        }
        protected class Variables extends VariableMapper {
            protected Map<String, Object> context = FastMap.newInstance();
            protected Variables(Map<String, ? extends Object> context) {
                this.context.putAll(context);
            }
            public ValueExpression resolveVariable(String variable) {
                Object obj = this.context.get(variable);
                if (obj != null) {
                    return new BasicValueExpression(obj);
                }
                return null;
            }
            public ValueExpression setVariable(String variable, ValueExpression expression) {
                return new BasicValueExpression(this.context.put(variable, expression.getValue(null)));
            }
        }
        @SuppressWarnings("serial")
        protected class BasicValueExpression extends ValueExpression {
            protected Object object;
            public BasicValueExpression(Object object) {
                super();
                this.object = object;
            }
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                try {
                    BasicValueExpression other = (BasicValueExpression) obj;
                    return this.object.equals(other.object);
                } catch (Exception e) {}
                return false;
            }
            public int hashCode() {
                return this.object == null ? 0 : this.object.hashCode();
            }
            public Object getValue(ELContext context) {
                return this.object;
            }
            public String getExpressionString() {
                return null;
            }
            public boolean isLiteralText() {
                return false;
            }
            public Class<?> getType(ELContext context) {
                return this.object == null ? null : this.object.getClass();
            }
            public boolean isReadOnly(ELContext context) {
                return false;
            }
            public void setValue(ELContext context, Object value) {
                this.object = value;
            }
            public String toString() {
                return "ValueExpression(" + this.object + ")";
            }
            public Class<?> getExpectedType() {
                return this.object == null ? null : this.object.getClass();
            }
        }
    }

    protected static class Functions extends FunctionMapper {
        protected Map<String, Method> functionMap = FastMap.newInstance();
        public void setFunction(String prefix, String localName, Method method) {
            synchronized(this) {
                functionMap.put(prefix + ":" + localName, method);
            }
        }
        public Method resolveFunction(String prefix, String localName) {
            return functionMap.get(prefix + ":" + localName);
        }
    }

}
