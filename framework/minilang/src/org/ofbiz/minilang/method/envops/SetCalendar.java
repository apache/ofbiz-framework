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
import com.ibm.icu.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Adjust a Timestamp by a specified time.
 */
public class SetCalendar extends MethodOperation {
    public static final class SetCalendarFactory implements Factory<SetCalendar> {
        public SetCalendar createMethodOperation(Element element, SimpleMethod simpleMethod) {
            return new SetCalendar(element, simpleMethod);
        }

        public String getName() {
            return "set-calendar";
        }
    }
    public static final String module = SetCalendar.class.getName();

    protected ContextAccessor<Timestamp> field;
    protected ContextAccessor<Object> fromField;
    protected FlexibleStringExpander valueExdr;
    protected FlexibleStringExpander defaultExdr;
    protected FlexibleStringExpander yearsExdr;
    protected FlexibleStringExpander monthsExdr;
    protected FlexibleStringExpander daysExdr;
    protected FlexibleStringExpander hoursExdr;
    protected FlexibleStringExpander minutesExdr;
    protected FlexibleStringExpander secondsExdr;
    protected FlexibleStringExpander millisExdr;
    protected FlexibleStringExpander periodAlignStart;
    protected FlexibleStringExpander periodAlignEnd;
    protected FlexibleStringExpander localeExdr;
    protected FlexibleStringExpander timeZoneExdr;
    protected boolean setIfNull; // default to false
    protected boolean setIfEmpty; // default to true

    public SetCalendar(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        this.field = new ContextAccessor<Timestamp>(element.getAttribute("field"));
        this.fromField = new ContextAccessor<Object>(element.getAttribute("from-field"));
        this.valueExdr = FlexibleStringExpander.getInstance(element.getAttribute("value"));
        this.defaultExdr = FlexibleStringExpander.getInstance(element.getAttribute("default-value"));
        this.yearsExdr = FlexibleStringExpander.getInstance(element.getAttribute("years"));
        this.monthsExdr = FlexibleStringExpander.getInstance(element.getAttribute("months"));
        this.daysExdr = FlexibleStringExpander.getInstance(element.getAttribute("days"));
        this.hoursExdr = FlexibleStringExpander.getInstance(element.getAttribute("hours"));
        this.minutesExdr = FlexibleStringExpander.getInstance(element.getAttribute("minutes"));
        this.secondsExdr = FlexibleStringExpander.getInstance(element.getAttribute("seconds"));
        this.millisExdr = FlexibleStringExpander.getInstance(element.getAttribute("millis"));
        this.periodAlignStart = FlexibleStringExpander.getInstance(element.getAttribute("period-align-start"));
        this.periodAlignEnd = FlexibleStringExpander.getInstance(element.getAttribute("period-align-end"));
        this.localeExdr = FlexibleStringExpander.getInstance(element.getAttribute("locale"));
        this.timeZoneExdr = FlexibleStringExpander.getInstance(element.getAttribute("time-zone"));
        // default to false, anything but true is false
        this.setIfNull = "true".equals(element.getAttribute("set-if-null"));
        // default to true, anything but false is true
        this.setIfEmpty = !"false".equals(element.getAttribute("set-if-empty"));

        if (!this.fromField.isEmpty() && !this.valueExdr.isEmpty()) {
            throw new IllegalArgumentException("Cannot specify a from-field [" + element.getAttribute("from-field") + "] and a value [" + element.getAttribute("value") + "] on the set-calendar action in a screen widget");
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) {
        Object newValue = null;
        if (!this.fromField.isEmpty()) {
            newValue = this.fromField.get(methodContext);
            if (Debug.verboseOn()) Debug.logVerbose("In screen getting value for field from [" + this.fromField.toString() + "]: " + newValue, module);
        } else if (!this.valueExdr.isEmpty()) {
            newValue = methodContext.expandString(this.valueExdr);
        }

        // If newValue is still empty, use the default value
        if (ObjectType.isEmpty(newValue) && !this.defaultExdr.isEmpty()) {
            newValue = methodContext.expandString(this.defaultExdr);
        }

        if (!setIfNull && newValue == null) {
            if (Debug.verboseOn()) Debug.logVerbose("Field value not found (null) with name [" + fromField + "] and value [" + valueExdr + "], and there was not default value, not setting field", module);
            return true;
        }
        if (!setIfEmpty && ObjectType.isEmpty(newValue)) {
            if (Debug.verboseOn()) Debug.logVerbose("Field value not found (empty) with name [" + fromField + "] and value [" + valueExdr + "], and there was not default value, not setting field", module);
            return true;
        }

        // Convert attributes to the corresponding data types
        Locale locale = null;
        TimeZone timeZone = null;
        Timestamp fromStamp = null;
        try {
            if (!this.localeExdr.isEmpty()) {
                locale = (Locale) ObjectType.simpleTypeConvert(methodContext.expandString(this.localeExdr), "Locale", null, null);
            }
            if (!this.timeZoneExdr.isEmpty()) {
                timeZone = (TimeZone) ObjectType.simpleTypeConvert(methodContext.expandString(this.timeZoneExdr), "TimeZone", null, null);
            }
            if (locale == null) {
                locale = methodContext.getLocale();
            }
            if (locale == null) {
                locale = Locale.getDefault();
            }
            if (timeZone == null) {
                timeZone = methodContext.getTimeZone();
            }
            if (timeZone == null) {
                timeZone = TimeZone.getDefault();
            }
            fromStamp = (Timestamp) ObjectType.simpleTypeConvert(newValue, "Timestamp", UtilDateTime.DATE_TIME_FORMAT, timeZone, locale, true);
        } catch (Exception e) {
            // Catching all exceptions - even potential ClassCastException
            if (Debug.verboseOn()) Debug.logVerbose("Error converting attributes to objects: " + e.getMessage(), module);
            return true;
        }

        // Convert Strings to ints
        int years = this.yearsExdr.isEmpty() ? 0 : Integer.parseInt(methodContext.expandString(this.yearsExdr));
        int months = this.monthsExdr.isEmpty() ? 0 : Integer.parseInt(methodContext.expandString(this.monthsExdr));
        int days = this.daysExdr.isEmpty() ? 0 : Integer.parseInt(methodContext.expandString(this.daysExdr));
        int hours = this.hoursExdr.isEmpty() ? 0 : Integer.parseInt(methodContext.expandString(this.hoursExdr));
        int minutes = this.minutesExdr.isEmpty() ? 0 : Integer.parseInt(methodContext.expandString(this.minutesExdr));
        int seconds = this.secondsExdr.isEmpty() ? 0 : Integer.parseInt(methodContext.expandString(this.secondsExdr));
        int millis = this.millisExdr.isEmpty() ? 0 : Integer.parseInt(methodContext.expandString(this.millisExdr));

        // Adjust calendar
        Calendar cal = UtilDateTime.toCalendar(fromStamp, timeZone, locale);
        cal.add(Calendar.MILLISECOND, millis);
        cal.add(Calendar.SECOND, seconds);
        cal.add(Calendar.MINUTE, minutes);
        cal.add(Calendar.HOUR, hours);
        cal.add(Calendar.DAY_OF_MONTH, days);
        cal.add(Calendar.MONTH, months);
        cal.add(Calendar.YEAR, years);

        Timestamp toStamp = new Timestamp(cal.getTimeInMillis());

        // Align period start/end
        if (!periodAlignStart.isEmpty()) {
            String period = methodContext.expandString(periodAlignStart);
            if ("day".equals(period)) {
                toStamp = UtilDateTime.getDayStart(toStamp, 0, timeZone, locale);
            } else if ("week".equals(period)) {
                toStamp = UtilDateTime.getWeekStart(toStamp, 0, timeZone, locale);
            } else if ("month".equals(period)) {
                toStamp = UtilDateTime.getMonthStart(toStamp, 0, timeZone, locale);
            } else if ("year".equals(period)) {
                toStamp = UtilDateTime.getYearStart(toStamp, 0, timeZone, locale);
            }
        } else if (!periodAlignEnd.isEmpty()) {
            String period = methodContext.expandString(periodAlignEnd);
            if ("day".equals(period)) {
                toStamp = UtilDateTime.getDayEnd(toStamp, timeZone, locale);
            } else if ("week".equals(period)) {
                toStamp = UtilDateTime.getWeekEnd(toStamp, timeZone, locale);
            } else if ("month".equals(period)) {
                toStamp = UtilDateTime.getMonthEnd(toStamp, timeZone, locale);
            } else if ("year".equals(period)) {
                toStamp = UtilDateTime.getYearEnd(toStamp, timeZone, locale);
            }
        }

        if (Debug.verboseOn())
            Debug.logVerbose("In screen setting calendar [" + this.field.toString(), module);
        this.field.put(methodContext, toStamp);
        return true;
    }

    @Override
    public String rawString() {
        return "<set-calendar field=\"" + this.field
                + (this.valueExdr.isEmpty() ? "" : "\" value=\"" + this.valueExdr.getOriginal())
                + (this.fromField.isEmpty() ? "" : "\" from-field=\"" + this.fromField)
                + (this.defaultExdr.isEmpty() ? "" : "\" default-value=\"" + this.defaultExdr.getOriginal())
                + "\"/>";
    }
    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
