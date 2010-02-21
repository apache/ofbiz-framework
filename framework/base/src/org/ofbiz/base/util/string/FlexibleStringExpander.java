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

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.el.PropertyNotFoundException;

import org.ofbiz.base.util.BshUtil;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GroovyUtil;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.cache.UtilCache;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.runtime.InvokerHelper;

import bsh.EvalError;

/** Expands String values that contain Unified Expression Language (JSR 245)
 * syntax. This class also supports the execution of bsh scripts by using the
 * 'bsh:' prefix, and Groovy scripts by using the 'groovy:' prefix.
 * Further it is possible to control the output by specifying the suffix
 * '?currency(XXX)' to format the output according to the supplied locale
 * and specified (XXX) currency.<p>This class extends the UEL by allowing
 * nested expressions.</p>
 */
@SuppressWarnings("serial")
public abstract class FlexibleStringExpander implements Serializable {

    public static final String module = FlexibleStringExpander.class.getName();
    public static final String openBracket = "${";
    public static final String closeBracket = "}";
    protected static final UtilCache<String, FlexibleStringExpander> exprCache = UtilCache.createUtilCache("flexibleStringExpander.ExpressionCache");
    protected static final FlexibleStringExpander nullExpr = new ConstElem("");

    /** Evaluate an expression and return the result as a <code>String</code>.
     * Null expressions return <code>null</code>.
     * A null <code>context</code> argument will return the original expression.
     * <p>Note that the behavior of this method is not the same as using
     * <code>FlexibleStringExpander.getInstance(expression).expandString(context)</code>
     * because it returns <code>null</code> when given a null <code>expression</code>
     * argument, and
     * <code>FlexibleStringExpander.getInstance(expression).expandString(context)</code>
     * returns an empty <code>String</code>.</p>
     * 
     * @param expression The original expression
     * @param context The evaluation context
     * @return The original expression's evaluation result as a <code>String</code>
     */
    public static String expandString(String expression, Map<String, ? extends Object> context) {
        return expandString(expression, context, null, null);
    }

    /** Evaluate an expression and return the result as a <code>String</code>.
     * Null expressions return <code>null</code>.
     * A null <code>context</code> argument will return the original expression.
     * <p>Note that the behavior of this method is not the same as using
     * <code>FlexibleStringExpander.getInstance(expression).expandString(context, locale)</code>
     * because it returns <code>null</code> when given a null <code>expression</code>
     * argument, and
     * <code>FlexibleStringExpander.getInstance(expression).expandString(context, locale)</code>
     * returns an empty <code>String</code>.</p>
     * 
     * @param expression The original expression
     * @param context The evaluation context
     * @param locale The locale to be used for localization
     * @return The original expression's evaluation result as a <code>String</code>
     */
    public static String expandString(String expression, Map<String, ? extends Object> context, Locale locale) {
        return expandString(expression, context, null, locale);
    }

    /** Evaluate an expression and return the result as a <code>String</code>.
     * Null expressions return <code>null</code>.
     * A null <code>context</code> argument will return the original expression.
     * <p>Note that the behavior of this method is not the same as using
     * <code>FlexibleStringExpander.getInstance(expression).expandString(context, timeZone locale)</code>
     * because it returns <code>null</code> when given a null <code>expression</code>
     * argument, and
     * <code>FlexibleStringExpander.getInstance(expression).expandString(context, timeZone, locale)</code>
     * returns an empty <code>String</code>.</p>
     * 
     * @param expression The original expression
     * @param context The evaluation context
     * @param timeZone The time zone to be used for localization
     * @param locale The locale to be used for localization
     * @return The original expression's evaluation result as a <code>String</code>
     */
    public static String expandString(String expression, Map<String, ? extends Object> context, TimeZone timeZone, Locale locale) {
        if (expression == null) {
            return "";
        }
        if (context == null || !expression.contains(openBracket)) {
            return expression;
        }
        FlexibleStringExpander fse = FlexibleStringExpander.getInstance(expression);
        return fse.expandString(context, timeZone, locale);
    }

    /** Returns a <code>FlexibleStringExpander</code> object. <p>A null or
     * empty argument will return a <code>FlexibleStringExpander</code>
     * object that represents an empty expression. That object is a shared
     * singleton, so there is no memory or performance penalty in using it.</p>
     * <p>If the method is passed a <code>String</code> argument that doesn't
     * contain an expression, the <code>FlexibleStringExpander</code> object
     * that is returned does not perform any evaluations on the original
     * <code>String</code> - any methods that return a <code>String</code>
     * will return the original <code>String</code>. The object returned by
     * this method is very compact - taking less memory than the original
     * <code>String</code>.</p>
     * 
     * @param expression The original expression
     * @return A <code>FlexibleStringExpander</code> instance
     */
    public static FlexibleStringExpander getInstance(String expression) {
        if (UtilValidate.isEmpty(expression)) {
            return nullExpr;
        }
        // Remove the next three lines to cache all expressions
        if (!expression.contains(openBracket)) {
            return new ConstElem(expression);
        }
        FlexibleStringExpander fse = exprCache.get(expression);
        if (fse == null) {
            synchronized (exprCache) {
                FlexibleStringExpander[] strElems = getStrElems(expression);
                if (strElems.length == 1) {
                    fse = strElems[0];
                } else {
                    fse = new Elements(expression, strElems);
                }
                exprCache.put(expression, fse);
            }
        }
        return fse;
    }

    /** Parses an expression and returns an array of <code>FlexibleStringExpander</code>
     * instances.
     * @param expression The expression to be parsed
     * @return An array of <code>FlexibleStringExpander</code>
     * instances
     */
    protected static FlexibleStringExpander[] getStrElems(String expression) {
        if (UtilValidate.isEmpty(expression)) {
            return null;
        }
        int origLen = expression.length();
        ArrayList<FlexibleStringExpander> strElems = new ArrayList<FlexibleStringExpander>();
        int start = expression.indexOf(openBracket);
        if (start == -1) {
            strElems.add(new ConstElem(expression));
            strElems.trimToSize();
            return strElems.toArray(new FlexibleStringExpander[strElems.size()]);
        }
        int currentInd = 0;
        int end = -1;
        while (start != -1) {
            end = expression.indexOf(closeBracket, start);
            if (end == -1) {
                Debug.logWarning("Found a ${ without a closing } (curly-brace) in the String: " + expression, module);
                break;
            }
            // Check for escaped expression
            boolean escapedExpression = (start - 1 >= 0 && expression.charAt(start - 1) == '\\');
            if (start > currentInd) {
                // append everything from the current index to the start of the expression
                strElems.add(new ConstElem(expression.substring(currentInd, escapedExpression ? start -1 : start)));
            }
            if (expression.indexOf("bsh:", start + 2) == start + 2 && !escapedExpression) {
                // checks to see if this starts with a "bsh:", if so treat the rest of the expression as a bsh scriptlet
                strElems.add(new BshElem(expression.substring(start + 6, end)));
            } else if (expression.indexOf("groovy:", start + 2) == start + 2 && !escapedExpression) {
                // checks to see if this starts with a "groovy:", if so treat the rest of the expression as a groovy scriptlet
                strElems.add(new GroovyElem(expression.substring(start + 9, end)));
            } else {
                // Scan for matching closing bracket
                int ptr = expression.indexOf(openBracket, start + 2);
                while (ptr != -1 && end != -1 && ptr < end) {
                    end = expression.indexOf(closeBracket, end + 1);
                    ptr = expression.indexOf(openBracket, ptr + 2);
                }
                if (end == -1) {
                    end = origLen;
                }
                String subExpression = expression.substring(start + 2, end);
                // Evaluation sequence is important - do not change it
                if (escapedExpression) {
                    strElems.add(new ConstElem(expression.substring(start, end + 1)));
                } else if (subExpression.contains("?currency(")) {
                    strElems.add(new CurrElem(subExpression));
                } else if (subExpression.contains(openBracket)) {
                    strElems.add(new NestedVarElem(subExpression));
                } else {
                    strElems.add(new VarElem(subExpression));
                }
            }
            // reset the current index to after the expression, and the start to the beginning of the next expression
            currentInd = end + 1;
            if (currentInd > origLen) {
                currentInd = origLen;
            }
            start = expression.indexOf(openBracket, currentInd);
        }
        // append the rest of the original string, ie after the last expression
        if (currentInd < origLen) {
            strElems.add(new ConstElem(expression.substring(currentInd)));
        }
        return strElems.toArray(new FlexibleStringExpander[strElems.size()]);
    }

    // Note: a character array is used instead of a String to keep the memory footprint small.
    protected final char[] orig;
    protected int hint = 20;

    protected FlexibleStringExpander(String original) {
        this.orig = original.toCharArray();
    }

    /** Appends this object's expression result to <code>buffer</code>.
     * 
     * @param buffer The buffer to append to
     * @param context The evaluation context
     * @param timeZone The time zone to be used for localization
     * @param locale The locale to be used for localization
     */
    protected abstract void append(StringBuilder buffer, Map<String, ? extends Object> context, TimeZone timeZone, Locale locale);

    /** Evaluate this object's expression and return the result as a <code>String</code>.
     * Null or empty expressions return an empty <code>String</code>.
     * A <code>null context</code> argument will return the original expression.
     * 
     * @param context The evaluation context
     * @return This object's expression result as a <code>String</code>
     */
    public String expandString(Map<String, ? extends Object> context) {
        return this.expandString(context, null, null);
    }

    /** Evaluate this object's expression and return the result as a <code>String</code>.
     * Null or empty expressions return an empty <code>String</code>.
     * A <code>null context</code> argument will return the original expression.
     * 
     * @param context The evaluation context
     * @param locale The locale to be used for localization
     * @return This object's expression result as a <code>String</code>
     */
    public String expandString(Map<String, ? extends Object> context, Locale locale) {
        return this.expandString(context, null, locale);
    }

    /** Evaluate this object's expression and return the result as a <code>String</code>.
     * Null or empty expressions return an empty <code>String</code>.
     * A <code>null context</code> argument will return the original expression.
     * 
     * @param context The evaluation context
     * @param timeZone The time zone to be used for localization
     * @param locale The locale to be used for localization
     * @return This object's expression result as a <code>String</code>
     */
    public String expandString(Map<String, ? extends Object> context, TimeZone timeZone, Locale locale) {
        if (context == null) {
            return this.toString();
        }
        if (locale == null) {
            locale = (Locale) context.get("locale");
            if (locale == null && context.containsKey("autoUserLogin")) {
                Map<String, Object> autoUserLogin = UtilGenerics.cast(context.get("autoUserLogin"));
                locale = UtilMisc.ensureLocale(autoUserLogin.get("lastLocale"));
            }
            if (locale == null) {
                locale = Locale.getDefault();
            }
        }
        if (timeZone == null) {
            timeZone = (TimeZone) context.get("timeZone");
            if (timeZone == null && context.containsKey("autoUserLogin")) {
                Map<String, String> autoUserLogin = UtilGenerics.cast(context.get("autoUserLogin"));
                timeZone = UtilDateTime.toTimeZone(autoUserLogin.get("lastTimeZone"));
            }
            if (timeZone == null) {
                timeZone = TimeZone.getDefault();
            }
        }
        StringBuilder buffer = new StringBuilder(this.hint);
        this.append(buffer, context, timeZone, locale);
        if (buffer.length() > this.hint) {
            synchronized(this) {
                this.hint = buffer.length();
            }
        }
        return buffer.toString();
    }

    /** Returns a copy of the original expression.
     * 
     * @return The original expression
     */
    public String getOriginal() {
        return new String(this.orig);
    }

    /** Returns <code>true</code> if the original expression is empty
     * or <code>null</code>.
     * 
     * @return <code>true</code> if the original expression is empty
     * or <code>null</code>
     */
    public boolean isEmpty() {
        return this.orig == null || this.orig.length == 0;
    }

    /** Returns a copy of the original expression.
     * 
     * @return The original expression
     */
    @Override
    public String toString() {
        return new String(this.orig);
    }

    /** An object that represents a <code>${bsh:}</code> expression. */
    protected static class BshElem extends FlexibleStringExpander {
        protected BshElem(String original) {
            super(original);
        }
        public void append(StringBuilder buffer, Map<String, ? extends Object> context, TimeZone timeZone, Locale locale) {
            try {
                Object obj = BshUtil.eval(new String(this.orig), UtilMisc.makeMapWritable(context));
                if (obj != null) {
                    try {
                        buffer.append(ObjectType.simpleTypeConvert(obj, "String", null, timeZone, locale, true));
                    } catch (Exception e) {
                        buffer.append(obj);
                    }
                } else {
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("BSH scriptlet evaluated to null [" + this + "], got no return so inserting nothing.", module);
                    }
                }
            } catch (EvalError e) {
                Debug.logWarning(e, "Error evaluating BSH scriptlet [" + this + "], inserting nothing; error was: " + e, module);
            }
        }
    }

    /** An object that represents a <code>String</code> constant portion of an expression. */
    protected static class ConstElem extends FlexibleStringExpander {
        protected ConstElem(String original) {
            super(original);
        }
        @Override
        public void append(StringBuilder buffer, Map<String, ? extends Object> context, TimeZone timeZone, Locale locale) {
            buffer.append(this.orig);
        }
        @Override
        public String expandString(Map<String, ? extends Object> context, TimeZone timeZone, Locale locale) {
            return new String(this.orig);
        }
    }

    /** An object that represents a currency portion of an expression. */
    protected static class CurrElem extends FlexibleStringExpander {
        protected final char[] valueStr;
        protected final FlexibleStringExpander codeExpr;
        protected CurrElem(String original) {
            super(original);
            int currencyPos = original.indexOf("?currency(");
            int closeParen = original.indexOf(")", currencyPos + 10);
            this.codeExpr = FlexibleStringExpander.getInstance(original.substring(currencyPos + 10, closeParen));
            this.valueStr = openBracket.concat(original.substring(0, currencyPos)).concat(closeBracket).toCharArray();
        }
        public void append(StringBuilder buffer, Map<String, ? extends Object> context, TimeZone timeZone, Locale locale) {
            try {
                Object obj = UelUtil.evaluate(context, new String(this.valueStr));
                if (obj != null) {
                    String currencyCode = this.codeExpr.expandString(context, timeZone, locale);
                    buffer.append(UtilFormatOut.formatCurrency(new BigDecimal(obj.toString()), currencyCode, locale));
                }
            } catch (PropertyNotFoundException e) {
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Error evaluating expression: " + e, module);
                }
            } catch (Exception e) {
                Debug.logError("Error evaluating expression: " + e, module);
            }
        }
    }

    /** A container object that contains expression fragments. */
    protected static class Elements extends FlexibleStringExpander {
        protected final FlexibleStringExpander[] childElems;
        protected Elements(String original, FlexibleStringExpander[] childElems) {
            super(original);
            this.childElems = childElems;
        }
        public void append(StringBuilder buffer, Map<String, ? extends Object> context, TimeZone timeZone, Locale locale) {
            for (FlexibleStringExpander child : this.childElems) {
                child.append(buffer, context, timeZone, locale);
            }
        }
    }

    /** An object that represents a <code>${groovy:}</code> expression. */
    protected static class GroovyElem extends FlexibleStringExpander {
        protected final Class<?> parsedScript;
        protected GroovyElem(String script) {
            super(script);
            this.parsedScript = GroovyUtil.parseClass(script);
        }
        public void append(StringBuilder buffer, Map<String, ? extends Object> context, TimeZone timeZone, Locale locale) {
            try {
                Object obj = InvokerHelper.createScript(this.parsedScript, GroovyUtil.getBinding(context)).run();
                if (obj != null) {
                    try {
                        buffer.append(ObjectType.simpleTypeConvert(obj, "String", null, timeZone, locale, true));
                    } catch (Exception e) {
                        buffer.append(obj);
                    }
                } else {
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("Groovy scriptlet evaluated to null [" + this + "], got no return so inserting nothing.", module);
                    }
                }
            } catch (CompilationFailedException e) {
                Debug.logWarning(e, "Error evaluating Groovy scriptlet [" + this + "], inserting nothing; error was: " + e, module);
            } catch (Exception e) {
                // handle other things, like the groovy.lang.MissingPropertyException
                Debug.logWarning(e, "Error evaluating Groovy scriptlet [" + this + "], inserting nothing; error was: " + e, module);
            }
        }
    }

    /** An object that represents a nested expression. */
    protected static class NestedVarElem extends FlexibleStringExpander {
        protected final FlexibleStringExpander[] childElems;
        protected NestedVarElem(String original) {
            super(original);
            this.childElems = getStrElems(original);
            if (original.length() > this.hint) {
                this.hint = original.length();
            }
        }
        public void append(StringBuilder buffer, Map<String, ? extends Object> context, TimeZone timeZone, Locale locale) {
            StringBuilder expr = new StringBuilder(this.hint);
            for (FlexibleStringExpander child : this.childElems) {
                child.append(expr, context, timeZone, locale);
            }
            if (expr.length() == 0) {
                return;
            }
            try {
                Object obj = UelUtil.evaluate(context, openBracket.concat(expr.toString()).concat(closeBracket));
                if (obj != null) {
                    buffer.append((String) ObjectType.simpleTypeConvert(obj, "String", null, timeZone, locale, false));
                }
            } catch (PropertyNotFoundException e) {
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Error evaluating expression: " + e, module);
                }
            } catch (Exception e) {
                Debug.logError("Error evaluating expression: " + e, module);
            }
        }
    }

    /** An object that represents a simple, non-nested expression. */
    protected static class VarElem extends FlexibleStringExpander {
        protected final char[] bracketedOriginal;
        protected VarElem(String original) {
            super(original);
            this.bracketedOriginal = openBracket.concat(UelUtil.prepareExpression(original)).concat(closeBracket).toCharArray();
        }
        public void append(StringBuilder buffer, Map<String, ? extends Object> context, TimeZone timeZone, Locale locale) {
            Object obj = null;
            try {
                obj = UelUtil.evaluate(context, new String(this.bracketedOriginal));
            } catch (PropertyNotFoundException e) {
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Error evaluating expression " + this + ": " + e, module);
                }
            } catch (Exception e) {
                Debug.logError("Error evaluating expression " + this + ": " + e, module);
            }
            if (obj != null) {
                try {
                    // Check for runtime nesting
                    String str = (String) obj;
                    if (str.contains(openBracket)) {
                        FlexibleStringExpander fse = FlexibleStringExpander.getInstance(str);
                        fse.append(buffer, context, timeZone, locale);
                        return;
                    }
                } catch (ClassCastException e) {}
                try {
                    buffer.append((String) ObjectType.simpleTypeConvert(obj, "String", null, timeZone, locale, false));
                } catch (Exception e) {}
            }
        }
    }
}
