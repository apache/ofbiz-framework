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

import static de.odysseus.el.tree.impl.Scanner.Symbol.END_EVAL;
import static de.odysseus.el.tree.impl.Scanner.Symbol.FLOAT;
import static de.odysseus.el.tree.impl.Scanner.Symbol.START_EVAL_DEFERRED;
import static de.odysseus.el.tree.impl.Scanner.Symbol.START_EVAL_DYNAMIC;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.PropertyNotFoundException;

import org.apache.ofbiz.base.util.Debug;

import de.odysseus.el.ExpressionFactoryImpl;
import de.odysseus.el.misc.LocalMessages;
import de.odysseus.el.tree.Bindings;
import de.odysseus.el.tree.Tree;
import de.odysseus.el.tree.TreeStore;
import de.odysseus.el.tree.impl.Builder;
import de.odysseus.el.tree.impl.Cache;
import de.odysseus.el.tree.impl.Parser;
import de.odysseus.el.tree.impl.Parser.ParseException;
import de.odysseus.el.tree.impl.Scanner.ScanException;
import de.odysseus.el.tree.impl.Scanner.Symbol;
import de.odysseus.el.tree.impl.ast.AstBracket;
import de.odysseus.el.tree.impl.ast.AstDot;
import de.odysseus.el.tree.impl.ast.AstEval;
import de.odysseus.el.tree.impl.ast.AstIdentifier;
import de.odysseus.el.tree.impl.ast.AstNode;

/** A facade class used to connect the OFBiz framework to the JUEL library.
 *<p>The Unified Expression Language specification doesn't allow assignment of
 * values to non-existent variables (auto-vivify) - but the OFBiz scripting
 * languages do. This class modifies the JUEL library behavior to enable
 * auto-vivify.</p>
 */
public class JuelConnector {
    protected static final String module = JuelConnector.class.getName();

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
                    Debug.logVerbose(e, module);
                }
            }
            Object property = getProperty(bindings, context);
            if (property == null && strict) {
                throw new PropertyNotFoundException(LocalMessages.get("error.property.property.notfound", "null", base));
            }
            if (base == null) {
                base = UelUtil.autoVivifyListOrMap(property);
                if (Debug.verboseOn()) {
                    Debug.logVerbose("ExtendedAstBracket.setValue auto-vivify base: " + base + ", property = " + property, module);
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
                    Debug.logVerbose(e, module);
                }
            }
            Object property = getProperty(bindings, context);
            if (property == null && strict) {
                throw new PropertyNotFoundException(LocalMessages.get("error.property.property.notfound", "null", base));
            }
            if (base == null) {
                base = UelUtil.autoVivifyListOrMap(property);
                if (Debug.verboseOn()) {
                    Debug.logVerbose("ExtendedAstDot.setValue auto-vivify base: " + base + ", property = " + property, module);
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
        protected AstEval eval(boolean required, boolean deferred) throws ScanException, ParseException {
            AstEval v = null;
            Symbol start_eval = deferred ? START_EVAL_DEFERRED : START_EVAL_DYNAMIC;
            if (this.getToken().getSymbol() == start_eval) {
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
                fail(start_eval);
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
            } catch (ScanException | ParseException e) {
                throw new ELException(LocalMessages.get("error.build", expression, e.getMessage()));
            }
        }
    }
}
