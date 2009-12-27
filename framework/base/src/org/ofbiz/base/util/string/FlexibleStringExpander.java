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
import java.util.List;
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

/** Expands String values that contain Unified Expression Language syntax.
 * Also supports the execution of bsh scripts by using the 'bsh:' prefix.
 * Further it is possible to control the output by specifying the suffix
 * '?currency(XXX)' to format the output according the current locale
 * and specified (XXX) currency.<p>This class extends the UEL by allowing
 * nested expressions.</p>
 */
@SuppressWarnings("serial")
public class FlexibleStringExpander implements Serializable {

    public static final String module = FlexibleStringExpander.class.getName();
    public static final String openBracket = "${";
    public static final String closeBracket = "}";
    protected static final UtilCache<String, FlexibleStringExpander> exprCache = UtilCache.createUtilCache("flexibleStringExpander.ExpressionCache");
    protected static final FlexibleStringExpander nullExpr = new FlexibleStringExpander(null);
    protected final String orig;
    protected final List<StrElem> strElems;
    protected int hint = 20;

    /**
     * @param original
     */
    protected FlexibleStringExpander(String original) {
        this.orig = original;
        if (original != null && original.contains(openBracket)) {
            this.strElems = getStrElems(original);
            if (original.length() > this.hint) {
                this.hint = original.length();
            }
        } else {
            this.strElems = null;
        }
    }

    public boolean isEmpty() {
        return this.orig == null || this.orig.length() == 0;
    }

    public String getOriginal() {
        return this.orig;
    }

    /** This expands the pre-parsed String given the context passed in. A
     * null context argument will return the original String.
     * @param context A context Map containing the variable values
     * @return The original String expanded by replacing varaible place holders.
     */
    public String expandString(Map<String, ? extends Object> context) {
        return this.expandString(context, null, null);
    }

    /** This expands the pre-parsed String given the context passed in. A
     * null context argument will return the original String.
     * @param context A context Map containing the variable values
     * @param locale the current set locale
     * @return The original String expanded by replacing varaible place holders.
     */
    public String expandString(Map<String, ? extends Object> context, Locale locale) {
        return this.expandString(context, null, locale);
    }

    /** This expands the pre-parsed String given the context passed in. A
     * null context argument will return the original String.
     * @param context A context Map containing the variable values
     * @param timeZone the current set time zone
     * @param locale the current set locale
     * @return The original String expanded by replacing varaible place holders.
     */
    public String expandString(Map<String, ? extends Object> context, TimeZone timeZone, Locale locale) {
        if (this.strElems == null || context == null) {
            return this.orig == null ? "" : this.orig;
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
        for (StrElem elem : this.strElems) {
            elem.append(buffer, context, timeZone, locale);
        }
        if (buffer.length() > this.hint) {
            synchronized(this) {
                this.hint = buffer.length();
            }
        }
        return buffer.toString();
    }

    /** Returns a FlexibleStringExpander instance.
     * @param original The original String expression
     * @return A FlexibleStringExpander instance
     */
    public static FlexibleStringExpander getInstance(String original) {
        if (UtilValidate.isEmpty(original)) {
            return nullExpr;
        }
        // Remove the next three lines to cache all expressions
        if (!original.contains(openBracket)) {
            return new FlexibleStringExpander(original);
        }
        FlexibleStringExpander fse = exprCache.get(original);
        if (fse == null) {
            synchronized (exprCache) {
                fse = exprCache.get(original);
                if (fse == null) {
                    fse = new FlexibleStringExpander(original);
                    exprCache.put(original, fse);
                }
            }
        }
        return fse;
    }

    /** Does on-the-fly parsing and expansion of the original String using
     * variable values from the passed context. A null context argument will
     * return the original String.
     * @param original The original String that will be expanded
     * @param context A context Map containing the variable values
     * @return The original String expanded by replacing varaible place holders.
     */
    public static String expandString(String original, Map<String, ? extends Object> context) {
        return expandString(original, context, null, null);
    }

    /** Does on-the-fly parsing and expansion of the original String using
     * variable values from the passed context. A null context argument will
     * return the original String.
     * @param original The original String that will be expanded
     * @param context A context Map containing the variable values
     * @return The original String expanded by replacing varaible place holders.
     */
    public static String expandString(String original, Map<String, ? extends Object> context, Locale locale) {
        return expandString(original, context, null, locale);
    }

    /** Does on-the-fly parsing and expansion of the original String using
     * variable values from the passed context. A null context argument will
     * return the original String.
     * @param original The original String that will be expanded
     * @param context A context Map containing the variable values
     * @return The original String expanded by replacing varaible place holders.
     */
    public static String expandString(String original, Map<String, ? extends Object> context, TimeZone timeZone, Locale locale) {
        if (context == null || original == null || !original.contains(openBracket)) {
            return original;
        }
        FlexibleStringExpander fse = FlexibleStringExpander.getInstance(original);
        return fse.expandString(context, timeZone, locale);
    }

    /** Protected helper method.
     * @param original
     * @return a list of parsed string elements
     */
    protected static List<StrElem> getStrElems(String original) {
        if (UtilValidate.isEmpty(original)) {
            return null;
        }
        int origLen = original.length();
        ArrayList<StrElem> strElems = new ArrayList<StrElem>();
        int start = original.indexOf(openBracket);
        if (start == -1) {
            strElems.add(new ConstElem(original));
            strElems.trimToSize();
            return strElems;
        }
        int currentInd = 0;
        int end = -1;
        while (start != -1) {
            end = original.indexOf(closeBracket, start);
            if (end == -1) {
                Debug.logWarning("Found a ${ without a closing } (curly-brace) in the String: " + original, module);
                break;
            }
            // Check for escaped expression
            boolean escapedExpression = (start - 1 >= 0 && original.charAt(start - 1) == '\\');
            if (start > currentInd) {
                // append everything from the current index to the start of the expression
                strElems.add(new ConstElem(original.substring(currentInd, escapedExpression ? start -1 : start)));
            }
            if (original.indexOf("bsh:", start + 2) == start + 2 && !escapedExpression) {
                // checks to see if this starts with a "bsh:", if so treat the rest of the expression as a bsh scriptlet
                strElems.add(new BshElem(original.substring(start + 6, end)));
            } else if (original.indexOf("groovy:", start + 2) == start + 2 && !escapedExpression) {
                // checks to see if this starts with a "groovy:", if so treat the rest of the expression as a groovy scriptlet
                strElems.add(new GroovyElem(original.substring(start + 9, end)));
            } else {
                // Scan for matching closing bracket
                int ptr = original.indexOf(openBracket, start + 2);
                while (ptr != -1 && end != -1 && ptr < end) {
                    end = original.indexOf(closeBracket, end + 1);
                    ptr = original.indexOf(openBracket, ptr + 2);
                }
                if (end == -1) {
                    end = origLen;
                }
                String expression = original.substring(start + 2, end);
                // Evaluation sequence is important - do not change it
                if (escapedExpression) {
                    strElems.add(new ConstElem(original.substring(start, end + 1)));
                } else if (expression.contains("?currency(")) {
                    strElems.add(new CurrElem(expression));
                } else if (expression.contains(openBracket)) {
                    strElems.add(new NestedVarElem(expression));
                } else {
                    strElems.add(new VarElem(expression));
                }
            }
            // reset the current index to after the expression, and the start to the beginning of the next expression
            currentInd = end + 1;
            if (currentInd > origLen) {
                currentInd = origLen;
            }
            start = original.indexOf(openBracket, currentInd);
        }
        // append the rest of the original string, ie after the last expression
        if (currentInd < origLen) {
            strElems.add(new ConstElem(original.substring(currentInd)));
        }
        strElems.trimToSize();
        return strElems;
    }

    protected static interface StrElem extends Serializable {
        public void append(StringBuilder buffer, Map<String, ? extends Object> context, TimeZone timeZone, Locale locale);
    }

    protected static class ConstElem implements StrElem {
        protected final String str;
        protected ConstElem(String value) {
            this.str = value.intern();
        }
        public void append(StringBuilder buffer, Map<String, ? extends Object> context, TimeZone timeZone, Locale locale) {
            buffer.append(this.str);
        }
    }

    protected static class BshElem implements StrElem {
        protected final String str;
        protected BshElem(String scriptlet) {
            this.str = scriptlet;
        }
        public void append(StringBuilder buffer, Map<String, ? extends Object> context, TimeZone timeZone, Locale locale) {
            try {
                Object obj = BshUtil.eval(this.str, UtilMisc.makeMapWritable(context));
                if (obj != null) {
                    try {
                        buffer.append(ObjectType.simpleTypeConvert(obj, "String", null, timeZone, locale, true));
                    } catch (Exception e) {
                        buffer.append(obj);
                    }
                } else {
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("BSH scriptlet evaluated to null [" + this.str + "], got no return so inserting nothing.", module);
                    }
                }
            } catch (EvalError e) {
                Debug.logWarning(e, "Error evaluating BSH scriptlet [" + this.str + "], inserting nothing; error was: " + e, module);
            }
        }
    }

    protected static class GroovyElem implements StrElem {
        protected final String originalString;
        protected final Class<?> parsedScript;
        protected GroovyElem(String script) {
            this.originalString = script;
            this.parsedScript = GroovyUtil.parseClass(script);
        }
        public void append(StringBuilder buffer, Map<String, ? extends Object> context, TimeZone timeZone, Locale locale) {
            try {
                // this approach will re-parse the script each time: Object obj = GroovyUtil.eval(this.str, UtilMisc.makeMapWritable(context));
                Object obj = InvokerHelper.createScript(this.parsedScript, GroovyUtil.getBinding(context)).run();
                if (obj != null) {
                    try {
                        buffer.append(ObjectType.simpleTypeConvert(obj, "String", null, timeZone, locale, true));
                    } catch (Exception e) {
                        buffer.append(obj);
                    }
                } else {
                    if (Debug.verboseOn()) {
                        Debug.logVerbose("Groovy scriptlet evaluated to null [" + this.originalString + "], got no return so inserting nothing.", module);
                    }
                }
            } catch (CompilationFailedException e) {
                Debug.logWarning(e, "Error evaluating Groovy scriptlet [" + this.originalString + "], inserting nothing; error was: " + e, module);
            } catch (Exception e) {
                // handle other things, like the groovy.lang.MissingPropertyException
                Debug.logWarning(e, "Error evaluating Groovy scriptlet [" + this.originalString + "], inserting nothing; error was: " + e, module);
            }
        }
    }

    protected static class CurrElem implements StrElem {
        protected final String valueStr;
        protected final FlexibleStringExpander codeExpr;
        protected CurrElem(String original) {
            int currencyPos = original.indexOf("?currency(");
            int closeParen = original.indexOf(")", currencyPos + 10);
            this.codeExpr = FlexibleStringExpander.getInstance(original.substring(currencyPos + 10, closeParen));
            this.valueStr = openBracket + original.substring(0, currencyPos) + closeBracket;
        }
        public void append(StringBuilder buffer, Map<String, ? extends Object> context, TimeZone timeZone, Locale locale) {
            try {
                Object obj = UelUtil.evaluate(context, this.valueStr);
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

    protected static class NestedVarElem implements StrElem {
        protected final List<StrElem> strElems;
        protected int hint = 20;
        protected NestedVarElem(String original) {
            this.strElems = getStrElems(original);
            if (original.length() > this.hint) {
                this.hint = original.length();
            }
        }
        public void append(StringBuilder buffer, Map<String, ? extends Object> context, TimeZone timeZone, Locale locale) {
            if (strElems == null) {
                return;
            }
            StringBuilder expr = new StringBuilder(this.hint);
            for (StrElem elem : this.strElems) {
                elem.append(expr, context, timeZone, locale);
            }
            if (expr.length() == 0) {
                return;
            }
            if (expr.length() > this.hint) {
                synchronized(this) {
                    this.hint = expr.length();
                }
            }
            try {
                Object obj = UelUtil.evaluate(context, openBracket + expr.toString() + closeBracket);
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

    protected static class VarElem implements StrElem {
        protected final String original;
        protected final String bracketedOriginal;
        protected VarElem(String original) {
            this.original = original;
            this.bracketedOriginal = openBracket + UelUtil.prepareExpression(original) + closeBracket;
        }
        public void append(StringBuilder buffer, Map<String, ? extends Object> context, TimeZone timeZone, Locale locale) {
            Object obj = null;
            try {
                obj = UelUtil.evaluate(context, this.bracketedOriginal);
            } catch (PropertyNotFoundException e) {
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Error evaluating expression " + this.original + ": " + e, module);
                }
            } catch (Exception e) {
                Debug.logError("Error evaluating expression " + this.original + ": " + e, module);
            }
            if (obj == null) {
                if (this.original.startsWith("env.")) {
                    Debug.logWarning("${env...} expression syntax deprecated, use ${sys:getProperty(String)} instead", module);
                    obj = System.getProperty(this.original.substring(4));
                }
            }
            if (obj != null) {
                try {
                    buffer.append((String) ObjectType.simpleTypeConvert(obj, "String", null, timeZone, locale, false));
                } catch (Exception e) {}
            }
        }
    }

}
