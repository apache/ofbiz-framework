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
package org.ofbiz.minilang.method.envops;

import java.sql.Timestamp;
import java.util.Locale;
import java.util.TimeZone;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.Scriptlet;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.MiniLangRuntimeException;
import org.ofbiz.minilang.MiniLangUtil;
import org.ofbiz.minilang.MiniLangValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

import com.ibm.icu.util.Calendar;

/**
 * Implements the &lt;set-calendar&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBADMIN/Mini-language+Reference#Mini-languageReference-{{%3Csetcalendar%3E}}">Mini-language Reference</a>
 */
public final class SetCalendar extends MethodOperation {

    public static final String module = SetCalendar.class.getName();

    // This method is needed only during the v1 to v2 transition
    private static boolean autoCorrect(Element element) {
        boolean elementModified = false;
        // Correct deprecated default-value attribute
        String defaultAttr = element.getAttribute("default-value");
        if (defaultAttr.length() > 0) {
            element.setAttribute("default", defaultAttr);
            element.removeAttribute("default-value");
            elementModified = true;
        }
        // Correct deprecated from-field attribute
        String fromAttr = element.getAttribute("from-field");
        if (fromAttr.length() > 0) {
            element.setAttribute("from", fromAttr);
            element.removeAttribute("from-field");
            elementModified = true;
        }
        // Correct value attribute expression that belongs in from attribute
        String valueAttr = element.getAttribute("value").trim();
        if (valueAttr.startsWith("${") && valueAttr.endsWith("}")) {
            valueAttr = valueAttr.substring(2, valueAttr.length() - 1);
            if (!valueAttr.contains("${")) {
                element.setAttribute("from", valueAttr);
                element.removeAttribute("value");
                elementModified = true;
            }
        }
        return elementModified;
    }

    // Fix for some Java versions that throw an exception when the String includes a "+".
    private static int parseInt(String intStr) {
        return Integer.parseInt(intStr.replace("+", ""));
    }

    private final FlexibleStringExpander daysFse;
    private final FlexibleStringExpander defaultFse;
    private final FlexibleMapAccessor<Object> fieldFma;
    private final FlexibleMapAccessor<Object> fromFma;
    private final FlexibleStringExpander hoursFse;
    private final FlexibleStringExpander localeFse;
    private final FlexibleStringExpander millisFse;
    private final FlexibleStringExpander minutesFse;
    private final FlexibleStringExpander monthsFse;
    private final FlexibleStringExpander periodAlignEnd;
    private final FlexibleStringExpander periodAlignStart;
    private final FlexibleStringExpander secondsFse;
    private final boolean setIfNull;
    private final Scriptlet scriptlet;
    private final FlexibleStringExpander timeZoneFse;
    private final FlexibleStringExpander valueFse;
    private final FlexibleStringExpander yearsFse;

    public SetCalendar(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.deprecatedAttribute(simpleMethod, element, "from-field", "replace with \"from\"");
            MiniLangValidate.deprecatedAttribute(simpleMethod, element, "default-value", "replace with \"default\"");
            MiniLangValidate.attributeNames(simpleMethod, element, "field", "from-field", "from", "value", "default-value", "default", "set-if-null",
                    "years", "months", "days", "hours", "minutes", "seconds", "millis", "period-align-start", "period-align-end", "locale", "time-zone");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "field");
            MiniLangValidate.requireAnyAttribute(simpleMethod, element, "from", "value");
            MiniLangValidate.constantPlusExpressionAttributes(simpleMethod, element, "value");
            MiniLangValidate.constantAttributes(simpleMethod, element, "set-if-null");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "field", "from", "from-field");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        boolean elementModified = autoCorrect(element);
        if (elementModified && MiniLangUtil.autoCorrectOn()) {
            MiniLangUtil.flagDocumentAsCorrected(element);
        }
        this.fieldFma = FlexibleMapAccessor.getInstance(element.getAttribute("field"));
        String fromAttribute = element.getAttribute("from");
        if (MiniLangUtil.containsScript(fromAttribute)) {
            this.scriptlet = new Scriptlet(StringUtil.convertOperatorSubstitutions(fromAttribute));
            this.fromFma = FlexibleMapAccessor.getInstance(null);
        } else {
            this.scriptlet = null;
            this.fromFma = FlexibleMapAccessor.getInstance(fromAttribute);
        }
        this.valueFse = FlexibleStringExpander.getInstance(element.getAttribute("value"));
        if (!fromAttribute.isEmpty() && !this.valueFse.isEmpty()) {
            throw new IllegalArgumentException("Cannot include both a from attribute and a value attribute in a <set-calendar> element.");
        }
        this.defaultFse = FlexibleStringExpander.getInstance(element.getAttribute("default"));
        this.setIfNull = "true".equals(element.getAttribute("set-if-null"));
        this.yearsFse = FlexibleStringExpander.getInstance(element.getAttribute("years"));
        this.monthsFse = FlexibleStringExpander.getInstance(element.getAttribute("months"));
        this.daysFse = FlexibleStringExpander.getInstance(element.getAttribute("days"));
        this.hoursFse = FlexibleStringExpander.getInstance(element.getAttribute("hours"));
        this.minutesFse = FlexibleStringExpander.getInstance(element.getAttribute("minutes"));
        this.secondsFse = FlexibleStringExpander.getInstance(element.getAttribute("seconds"));
        this.millisFse = FlexibleStringExpander.getInstance(element.getAttribute("millis"));
        this.periodAlignStart = FlexibleStringExpander.getInstance(element.getAttribute("period-align-start"));
        this.periodAlignEnd = FlexibleStringExpander.getInstance(element.getAttribute("period-align-end"));
        this.localeFse = FlexibleStringExpander.getInstance(element.getAttribute("locale"));
        this.timeZoneFse = FlexibleStringExpander.getInstance(element.getAttribute("time-zone"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        Object newValue = null;
        if (this.scriptlet != null) {
            try {
                newValue = this.scriptlet.executeScript(methodContext.getEnvMap());
            } catch (Exception exc) {
                Debug.logWarning(exc, "Error evaluating scriptlet [" + this.scriptlet + "]: " + exc, module);
            }
        } else if (!this.fromFma.isEmpty()) {
            newValue = this.fromFma.get(methodContext.getEnvMap());
        } else if (!this.valueFse.isEmpty()) {
            newValue = this.valueFse.expand(methodContext.getEnvMap());
        }
        if (ObjectType.isEmpty(newValue) && !this.defaultFse.isEmpty()) {
            newValue = this.defaultFse.expand(methodContext.getEnvMap());
        }
        if (!setIfNull && newValue == null) {
            return true;
        }
        Locale locale = null;
        TimeZone timeZone = null;
        Timestamp fromStamp = null;
        int years = 0;
        int months = 0;
        int days = 0;
        int hours = 0;
        int minutes = 0;
        int seconds = 0;
        int millis = 0;
        try {
            if (!this.localeFse.isEmpty()) {
                locale = (Locale) ObjectType.simpleTypeConvert(this.localeFse.expand(methodContext.getEnvMap()), "Locale", null, null);
            }
            if (locale == null) {
                locale = methodContext.getLocale();
            }
            if (locale == null) {
                locale = Locale.getDefault();
            }
            if (!this.timeZoneFse.isEmpty()) {
                timeZone = (TimeZone) ObjectType.simpleTypeConvert(this.timeZoneFse.expand(methodContext.getEnvMap()), "TimeZone", null, null);
            }
            if (timeZone == null) {
                timeZone = methodContext.getTimeZone();
            }
            if (timeZone == null) {
                timeZone = TimeZone.getDefault();
            }
            fromStamp = (Timestamp) MiniLangUtil.convertType(newValue, java.sql.Timestamp.class, locale, timeZone, UtilDateTime.getDateTimeFormat());
            if (!this.yearsFse.isEmpty()) {
                years= parseInt(this.yearsFse.expandString(methodContext.getEnvMap()));
            }
            if (!this.monthsFse.isEmpty()) {
                months = parseInt(this.monthsFse.expandString(methodContext.getEnvMap()));
            }
            if (!this.daysFse.isEmpty()) {
                days = parseInt(this.daysFse.expandString(methodContext.getEnvMap()));
            }
            if (!this.hoursFse.isEmpty()) {
                hours = parseInt(this.hoursFse.expandString(methodContext.getEnvMap()));
            }
            if (!this.minutesFse.isEmpty()) {
                minutes = parseInt(this.minutesFse.expandString(methodContext.getEnvMap()));
            }
            if (!this.secondsFse.isEmpty()) {
                seconds = parseInt(this.secondsFse.expandString(methodContext.getEnvMap()));
            }
            if (!this.millisFse.isEmpty()) {
                millis = parseInt(this.millisFse.expandString(methodContext.getEnvMap()));
            }
        } catch (Exception e) {
            throw new MiniLangRuntimeException("Exception thrown while parsing attributes: " + e.getMessage(), this);
        }
        Calendar cal = UtilDateTime.toCalendar(fromStamp, timeZone, locale);
        cal.add(Calendar.MILLISECOND, millis);
        cal.add(Calendar.SECOND, seconds);
        cal.add(Calendar.MINUTE, minutes);
        cal.add(Calendar.HOUR, hours);
        cal.add(Calendar.DAY_OF_MONTH, days);
        cal.add(Calendar.MONTH, months);
        cal.add(Calendar.YEAR, years);
        Timestamp toStamp = new Timestamp(cal.getTimeInMillis());
        if (!periodAlignStart.isEmpty()) {
            String period = periodAlignStart.expandString(methodContext.getEnvMap());
            if ("day".equals(period)) {
                toStamp = UtilDateTime.getDayStart(toStamp, 0, timeZone, locale);
            } else if ("week".equals(period)) {
                toStamp = UtilDateTime.getWeekStart(toStamp, 0, timeZone, locale);
            } else if ("month".equals(period)) {
                toStamp = UtilDateTime.getMonthStart(toStamp, 0, timeZone, locale);
            } else if ("year".equals(period)) {
                toStamp = UtilDateTime.getYearStart(toStamp, 0, timeZone, locale);
            } else {
                throw new MiniLangRuntimeException("Invalid period-align-start attribute value: " + period, this);
            }
        } else if (!periodAlignEnd.isEmpty()) {
            String period = periodAlignEnd.expandString(methodContext.getEnvMap());
            if ("day".equals(period)) {
                toStamp = UtilDateTime.getDayEnd(toStamp, timeZone, locale);
            } else if ("week".equals(period)) {
                toStamp = UtilDateTime.getWeekEnd(toStamp, timeZone, locale);
            } else if ("month".equals(period)) {
                toStamp = UtilDateTime.getMonthEnd(toStamp, timeZone, locale);
            } else if ("year".equals(period)) {
                toStamp = UtilDateTime.getYearEnd(toStamp, timeZone, locale);
            } else {
                throw new MiniLangRuntimeException("Invalid period-align-end attribute value: " + period, this);
            }
        }
        this.fieldFma.put(methodContext.getEnvMap(), toStamp);
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<set-calendar ");
        sb.append("field=\"").append(this.fieldFma).append("\" ");
        if (!this.fromFma.isEmpty()) {
            sb.append("from=\"").append(this.fromFma).append("\" ");
        }
        if (this.scriptlet != null) {
            sb.append("from=\"").append(this.scriptlet).append("\" ");
        }
        if (!this.valueFse.isEmpty()) {
            sb.append("value=\"").append(this.valueFse).append("\" ");
        }
        if (!this.defaultFse.isEmpty()) {
            sb.append("default=\"").append(this.defaultFse).append("\" ");
        }
        if (!this.yearsFse.isEmpty()) {
            sb.append("years=\"").append(this.yearsFse).append("\" ");
        }
        if (!this.monthsFse.isEmpty()) {
            sb.append("months=\"").append(this.monthsFse).append("\" ");
        }
        if (!this.daysFse.isEmpty()) {
            sb.append("days=\"").append(this.daysFse).append("\" ");
        }
        if (!this.hoursFse.isEmpty()) {
            sb.append("hours=\"").append(this.hoursFse).append("\" ");
        }
        if (!this.minutesFse.isEmpty()) {
            sb.append("minutes=\"").append(this.minutesFse).append("\" ");
        }
        if (!this.secondsFse.isEmpty()) {
            sb.append("seconds=\"").append(this.secondsFse).append("\" ");
        }
        if (!this.millisFse.isEmpty()) {
            sb.append("millis=\"").append(this.millisFse).append("\" ");
        }
        if (!this.periodAlignStart.isEmpty()) {
            sb.append("period-align-start=\"").append(this.localeFse).append("\" ");
        }
        if (!this.periodAlignEnd.isEmpty()) {
            sb.append("period-align-end=\"").append(this.localeFse).append("\" ");
        }
        if (!this.localeFse.isEmpty()) {
            sb.append("locale=\"").append(this.localeFse).append("\" ");
        }
        if (!this.timeZoneFse.isEmpty()) {
            sb.append("time-zone=\"").append(this.timeZoneFse).append("\" ");
        }
        if (this.setIfNull) {
            sb.append("set-if-null=\"true\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;set-calendar&gt; element.
     */
    public static final class SetCalendarFactory implements Factory<SetCalendar> {
        @Override
        public SetCalendar createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new SetCalendar(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "set-calendar";
        }
    }
}
