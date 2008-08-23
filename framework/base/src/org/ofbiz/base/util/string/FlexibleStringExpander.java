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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.ofbiz.base.util.BshUtil;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;

import bsh.EvalError;

/** Expands string values within a Map context supporting the ${} syntax for
 * variable placeholders and the "." (dot) and "[]" (square-brace) syntax
 * elements for accessing Map entries and List elements in the context.
 * It Also supports the execution of bsh files by using the 'bsh:' prefix.
 * Further it is possible to control the output by specifying the suffix
 * '?currency(XXX)' to format the output according the current locale
 * and specified (XXX) currency
 */
@SuppressWarnings("serial")
public class FlexibleStringExpander implements Serializable {
    
    public static final String module = FlexibleStringExpander.class.getName();
    protected static UtilCache<String, FlexibleStringExpander> exprCache = new UtilCache<String, FlexibleStringExpander>("flexibleStringExpander.ExpressionCache");
    protected static FlexibleStringExpander nullExpr = new FlexibleStringExpander(null);
    protected String orig;
    protected List<StrElem> strElems = null;
    protected int hint = 20;

    /**
     * @deprecated Use getInstance(String original) instead.
     * @param original
     */
    public FlexibleStringExpander(String original) {
        // TODO: Change this to protected, remove @deprecated javadoc comment
        this.orig = original;
        if (original != null && original.contains("${")) {
            this.strElems = getStrElems(original);
            if (original.length() > this.hint) {
                this.hint = original.length();
            }
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
                locale = UtilMisc.ensureLocale(((Map) context.get("autoUserLogin")).get("lastLocale"));
            }
            if (locale == null) {
                locale = Locale.getDefault();
            }
        }
        if (timeZone == null) {
            timeZone = (TimeZone) context.get("timeZone");
            if (timeZone == null && context.containsKey("autoUserLogin")) {
                timeZone = UtilDateTime.toTimeZone((String)((Map) context.get("autoUserLogin")).get("lastTimeZone"));
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
        if (original == null || original.length() == 0) {
            return nullExpr;
        }
        // Remove the next three lines to cache all expressions
        if (!original.contains("${")) {
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
        if (context == null || original == null || !original.contains("${")) {
            return original;
        }
        FlexibleStringExpander fse = FlexibleStringExpander.getInstance(original);
        return fse.expandString(context, timeZone, locale);
    }
    
    /** Protected helper method.
     * @param original
     * @return
     */
    protected static List<StrElem> getStrElems(String original) {
        if (original == null || original.length() == 0) {
            return null;
        }
        int origLen = original.length();
        ArrayList<StrElem> strElems = new ArrayList<StrElem>();
        int start = original.indexOf("${");
        if (start == -1) {
            strElems.add(new ConstElem(original));
            strElems.trimToSize();
            return strElems;
        }
        int currentInd = 0;
        int end = -1;
        while (start != -1) {
            end = original.indexOf("}", start);
            if (end == -1) {
                Debug.logWarning("Found a ${ without a closing } (curly-brace) in the String: " + original, module);
                break;
            } 
            if (start > currentInd) {
                // append everything from the current index to the start of the var
                strElems.add(new ConstElem(original.substring(currentInd, start)));
            }
            // check to see if this starts with a "bsh:", if so treat the rest of the string as a bsh scriptlet
            if (original.indexOf("bsh:", start + 2) == start + 2) {
                strElems.add(new BshElem(original.substring(start + 6, end)));
            } else {
                int ptr = original.indexOf("${", start + 2);
                while (ptr != -1 && end != -1 && ptr < end) {
                    end = original.indexOf("}", end + 1);
                    ptr = original.indexOf("${", ptr + 2);
                }
                if (end == -1) {
                    end = origLen;
                }
                String expression = original.substring(start + 2, end);
                // Evaluation sequence is important - do not change it
                if (expression.contains("?currency(")) {
                    strElems.add(new CurrElem(expression));
                } else if (expression.contains("${")){
                    strElems.add(new NestedVarElem(expression));
                } else {
                    strElems.add(new VarElem(expression));
                }
            }
            // reset the current index to after the var, and the start to the beginning of the next var
            currentInd = end + 1;
            if (currentInd > origLen) {
                currentInd = origLen;
            }
            start = original.indexOf("${", currentInd);
        }
        // append the rest of the original string, ie after the last variable
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
        protected String str;
        protected ConstElem(String value) {
            this.str = value.intern();
        }
        public void append(StringBuilder buffer, Map<String, ? extends Object> context, TimeZone timeZone, Locale locale) {
            buffer.append(this.str); 
        }
    }
    
    protected static class BshElem implements StrElem {
        String str;
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

    protected static class CurrElem implements StrElem {
        String str;
        protected FlexibleStringExpander codeExpr = null;
        protected CurrElem(String original) {
            int currencyPos = original.indexOf("?currency(");
            int closeBracket = original.indexOf(")", currencyPos+10);
            this.codeExpr = FlexibleStringExpander.getInstance(original.substring(currencyPos+10, closeBracket));
            this.str = original.substring(0, currencyPos);
        }
        public void append(StringBuilder buffer, Map<String, ? extends Object> context, TimeZone timeZone, Locale locale) {
            FlexibleMapAccessor<Object> fma = new FlexibleMapAccessor<Object>(this.str);
            Object obj = fma.get(context, locale);
            if (obj != null) {
                String currencyCode = this.codeExpr.expandString(context, timeZone, locale);
                buffer.append(UtilFormatOut.formatCurrency(Double.valueOf(obj.toString()), currencyCode, locale));
            }
        }
    }
        
    protected static class NestedVarElem implements StrElem {
        protected List<StrElem> strElems = null;
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
            if (expr.length() > this.hint) {
                synchronized(this) {
                    this.hint = expr.length();
                }
            }
            FlexibleMapAccessor<Object> fma = new FlexibleMapAccessor<Object>(expr.toString());
            Object obj = fma.get(context, locale);
            if (obj != null) {
                try {
                    buffer.append((String) ObjectType.simpleTypeConvert(obj, "String", null, timeZone, locale, true));
                } catch (Exception e) {
                    buffer.append(obj);
                }
            }
        }
    }

    protected static class VarElem implements StrElem {
        protected FlexibleMapAccessor<Object> fma = null;
        protected VarElem(String original) {
            this.fma = new FlexibleMapAccessor<Object>(original);
        }
        public void append(StringBuilder buffer, Map<String, ? extends Object> context, TimeZone timeZone, Locale locale) {
            Object obj = this.fma.get(context, locale);
            if (obj != null) {
                try {
                    buffer.append((String) ObjectType.simpleTypeConvert(obj, "String", null, timeZone, locale, true));
                } catch (Exception e) {
                    buffer.append(obj);
                }
            }
        }
    }

}
