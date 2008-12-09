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

import java.util.Map;
import javax.el.*;

import javolution.util.FastMap;

/** Implements the Unified Expression Language (JSR-245). */
public class UelUtil {
    
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
     * @throws Various <code>javax.el.*</code> exceptions
     */
    public static Object evaluate(Map<String, ? extends Object> context, String expression) {
        ELContext elContext = new BasicContext(context);
        ValueExpression ve = exprFactory.createValueExpression(elContext, expression, Object.class);
        return ve.getValue(elContext);
    }

    protected static class BasicContext extends ELContext {
        protected final VariableMapper variableMapper;
        public BasicContext(Map<String, ? extends Object> context) {
            this.variableMapper = new BasicVariableMapper(context, this);
        }
        public ELResolver getELResolver() {
            return defaultResolver;
        }
        public FunctionMapper getFunctionMapper() {
            return UelFunctions.getFunctionMapper();
        }
        public VariableMapper getVariableMapper() {
            return this.variableMapper;
        }
        protected class BasicVariableMapper extends VariableMapper {
            protected final ELContext elContext;
            protected final Map<String, Object> variables = FastMap.newInstance();
            protected BasicVariableMapper(Map<String, ? extends Object> context, ELContext parentContext) {
                this.variables.putAll(context);
                this.elContext = parentContext;
            }
            public ValueExpression resolveVariable(String variable) {
                Object obj = this.variables.get(variable);
                if (obj != null) {
                    return new BasicValueExpression(obj);
                }
                return null;
            }
            public ValueExpression setVariable(String variable, ValueExpression expression) {
                return new BasicValueExpression(this.variables.put(variable, expression.getValue(this.elContext)));
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
}
