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
package org.apache.ofbiz.base.util.string;

import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ExpressionFactory;
import jakarta.el.PropertyNotFoundException;

import org.activiti.core.el.juel.ExpressionFactoryImpl;
import org.activiti.core.el.juel.misc.LocalMessages;
import org.activiti.core.el.juel.tree.Bindings;
import org.activiti.core.el.juel.tree.Tree;
import org.activiti.core.el.juel.tree.TreeStore;
import org.activiti.core.el.juel.tree.impl.Builder;
import org.activiti.core.el.juel.tree.impl.Cache;
import org.activiti.core.el.juel.tree.impl.Parser;
import org.activiti.core.el.juel.tree.impl.Scanner;
import org.activiti.core.el.juel.tree.impl.ast.AstBracket;
import org.activiti.core.el.juel.tree.impl.ast.AstDot;
import org.activiti.core.el.juel.tree.impl.ast.AstEval;
import org.activiti.core.el.juel.tree.impl.ast.AstIdentifier;
import org.activiti.core.el.juel.tree.impl.ast.AstNode;
import org.apache.ofbiz.base.util.Debug;

import static org.activiti.core.el.juel.tree.impl.Scanner.Symbol.END_EVAL;
import static org.activiti.core.el.juel.tree.impl.Scanner.Symbol.FLOAT;
import static org.activiti.core.el.juel.tree.impl.Scanner.Symbol.START_EVAL_DEFERRED;
import static org.activiti.core.el.juel.tree.impl.Scanner.Symbol.START_EVAL_DYNAMIC;


/** A facade class used to connect the OFBiz framework to the JUEL library.
 *<p>The Unified Expression Language specification doesn't allow assignment of
 * values to non-existent variables (auto-vivify) - but the OFBiz scripting
 * languages do. This class modifies the JUEL library behavior to enable
 * auto-vivify.</p>
 */
public class JuelConnector {
    protected static final String MODULE = JuelConnector.class.getName();

    /** Returns an <code>ExpressionFactory</code> instance.
     * @return A customized <code>ExpressionFactory</code> instance
     */
    public static ExpressionFactory newExpressionFactory() {
        return new ExpressionFactoryImpl(new TreeStore(new ExtendedBuilder(), new Cache(1000)));
    }

    /** Custom <code>AstBracket</code> class that implements
     * <code>List</code> or <code>Map</code> auto-vivify.
     */
    public static class ExtendedAstBracket extends AstBracket {
        public ExtendedAstBracket(AstNode base, AstNode property, boolean lvalue, boolean strict) {
            super(base, property, lvalue, strict);
        }
        @Override
        public void setValue(Bindings bindings, ELContext context, Object value) throws ELException {
            if (!lvalue) {
                throw new ELException(LocalMessages.get("error.value.set.rvalue"));
            }
            Object base = null;
            try {
                base = prefix.eval(bindings, context);
            } catch (Exception e) {
                if (Debug.verboseOn()) {
                    Debug.logVerbose(e, MODULE);
                }
            }
            Object property = getProperty(bindings, context);
            if (property == null && strict) {
                throw new PropertyNotFoundException(LocalMessages.get("error.property.property.notfound", "null", base));
            }
            if (base == null) {
                base = UelUtil.autoVivifyListOrMap(property);
                if (Debug.verboseOn()) {
                    Debug.logVerbose("ExtendedAstBracket.setValue auto-vivify base: " + base + ", property = " + property, MODULE);
                }
                prefix.setValue(bindings, context, base);
            }
            context.getELResolver().setValue(context, base, property, value);
            if (!context.isPropertyResolved()) {
                throw new PropertyNotFoundException(LocalMessages.get("error.property.property.notfound", property, base));
            }
        }
    }

    /** Custom <code>AstDot</code> class that implements
     * <code>List</code> or <code>Map</code> auto-vivify.
     */
    public static class ExtendedAstDot extends AstDot {
        public ExtendedAstDot(AstNode base, String property, boolean lvalue) {
            super(base, property, lvalue);
        }
        @Override
        public void setValue(Bindings bindings, ELContext context, Object value) throws ELException {
            if (!lvalue) {
                throw new ELException(LocalMessages.get("error.value.set.rvalue"));
            }
            Object base = null;
            try {
                base = prefix.eval(bindings, context);
            } catch (Exception e) {
                if (Debug.verboseOn()) {
                    Debug.logVerbose(e, MODULE);
                }
            }
            Object property = getProperty(bindings, context);
            if (property == null && strict) {
                throw new PropertyNotFoundException(LocalMessages.get("error.property.property.notfound", "null", base));
            }
            if (base == null) {
                base = UelUtil.autoVivifyListOrMap(property);
                if (Debug.verboseOn()) {
                    Debug.logVerbose("ExtendedAstDot.setValue auto-vivify base: " + base + ", property = " + property, MODULE);
                }
                prefix.setValue(bindings, context, base);
            }
            context.getELResolver().setValue(context, base, property, value);
            if (!context.isPropertyResolved()) {
                throw new PropertyNotFoundException(LocalMessages.get("error.property.property.notfound", property, base));
            }
        }
    }

    /** Custom <code>Parser</code> class needed to implement auto-vivify. */
    protected static class ExtendedParser extends Parser {
        public ExtendedParser(Builder context, String input) {
            super(context, input);
        }
        @Override
        protected AstEval eval(boolean required, boolean deferred) throws Scanner.ScanException, ParseException {
            AstEval v = null;
            Scanner.Symbol startEval = deferred ? START_EVAL_DEFERRED : START_EVAL_DYNAMIC;
            if (this.getToken().getSymbol() == startEval) {
                consumeToken();
                AstNode node = expr(true);
                try {
                    consumeToken(END_EVAL);
                } catch (ParseException e) {
                    if (this.getToken().getSymbol() == FLOAT && node instanceof AstIdentifier) {
                        // Handle ${someMap.${someId}}
                        String mapKey = this.getToken().getImage().replace(".", "");
                        node = createAstDot(node, mapKey, true);
                        consumeToken();
                        consumeToken(END_EVAL);
                    } else {
                        throw e;
                    }
                }
                v = new AstEval(node, deferred);
            } else if (required) {
                fail(startEval);
            }
            return v;
        }
        @Override
        protected AstBracket createAstBracket(AstNode base, AstNode property, boolean lvalue, boolean strict) {
            return new ExtendedAstBracket(base, property, lvalue, strict);
        }
        @Override
        protected AstDot createAstDot(AstNode base, String property, boolean lvalue) {
            return new ExtendedAstDot(base, property, lvalue);
        }
    }

    /** Custom <code>Builder</code> class needed to implement a custom parser. */
    @SuppressWarnings("serial")
    protected static class ExtendedBuilder extends Builder {
        @Override
        public Tree build(String expression) throws ELException {
            try {
                return new ExtendedParser(this, expression).tree();
            } catch (Scanner.ScanException | Parser.ParseException e) {
                throw new ELException(LocalMessages.get("error.build", expression, e.getMessage()));
            }
        }
    }
}
