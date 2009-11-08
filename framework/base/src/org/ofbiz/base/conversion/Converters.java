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
package org.ofbiz.base.conversion;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javolution.util.FastMap;
import javolution.util.FastSet;

import org.ofbiz.base.conversion.BooleanConverters.*;
import org.ofbiz.base.conversion.CollectionConverters.*;
import org.ofbiz.base.conversion.DateTimeConverters.*;
import org.ofbiz.base.conversion.NumberConverters.*;
import org.ofbiz.base.conversion.MiscConverters.*;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.TimeDuration;

import com.ibm.icu.util.Calendar;

/** A <code>Converter</code> factory and repository. */
public class Converters {

    protected static final String module = Converters.class.getName(); 
    protected static final String DELIMITER = "->"; 
    protected static final Map<String, Converter<?, ?>> converterMap = FastMap.newInstance(); 
    protected static final Set<String> noConversions = FastSet.newInstance();
    protected static final Converter<Object, Object> nullConverter = new NullConverter();

    // If Arrays aren't converted when using RMI, then comment out the next line
    public static final Converter<Object[], List<?>> ArrayToList = new ArrayToList();
    public static final Converter<BigDecimal, Double> BigDecimalToDouble = new BigDecimalToDouble(); 
    public static final Converter<BigDecimal, Float> BigDecimalToFloat = new BigDecimalToFloat(); 
    public static final Converter<BigDecimal, Integer> BigDecimalToInteger = new BigDecimalToInteger(); 
    public static final Converter<BigDecimal, List<BigDecimal>> BigDecimalToList = new BigDecimalToList(); 
    public static final Converter<BigDecimal, Long> BigDecimalToLong = new BigDecimalToLong(); 
    public static final Converter<BigDecimal, Set<BigDecimal>> BigDecimalToSet = new BigDecimalToSet(); 
    public static final LocalizedConverter<BigDecimal, String> BigDecimalToString = new BigDecimalToString(); 
    public static final Converter<Boolean, Integer> BooleanToInteger = new BooleanToInteger(); 
    public static final Converter<Boolean, String> BooleanToString = new BooleanToString(); 
    public static final Converter<Calendar, Long> CalendarToLong = new CalendarToLong(); 
    public static final Converter<Calendar, String> CalendarToString = new CalendarToString(); 
    public static final Converter<java.util.Date, Long> DateToLong = new DateToLong(); 
    public static final LocalizedConverter<java.util.Date, String> DateToString = new DateToString(); 
    public static final Converter<Double, BigDecimal> DoubleToBigDecimal = new DoubleToBigDecimal(); 
    public static final Converter<Double, Float> DoubleToFloat = new DoubleToFloat(); 
    public static final Converter<Double, Integer> DoubleToInteger = new DoubleToInteger(); 
    public static final Converter<Double, List<Double>> DoubleToList = new DoubleToList(); 
    public static final Converter<Double, Long> DoubleToLong = new DoubleToLong(); 
    public static final Converter<Double, Set<Double>> DoubleToSet = new DoubleToSet(); 
    public static final LocalizedConverter<Double, String> DoubleToString = new DoubleToString(); 
    public static final Converter<TimeDuration, String> DurationToString = new DurationToString(); 
    public static final Converter<Float, BigDecimal> FloatToBigDecimal = new FloatToBigDecimal(); 
    public static final Converter<Float, Double> FloatToDouble = new FloatToDouble(); 
    public static final Converter<Float, Integer> FloatToInteger = new FloatToInteger(); 
    public static final Converter<Float, List<Float>> FloatToList = new FloatToList(); 
    public static final Converter<Float, Long> FloatToLong = new FloatToLong(); 
    public static final Converter<Float, Set<Float>> FloatToSet = new FloatToSet(); 
    public static final LocalizedConverter<Float, String> FloatToString = new FloatToString(); 
    public static final Converter<Integer, BigDecimal> IntegerToBigDecimal = new IntegerToBigDecimal(); 
    public static final Converter<Integer, Boolean> IntegerToBoolean = new IntegerToBoolean(); 
    public static final Converter<Integer, Double> IntegerToDouble = new IntegerToDouble(); 
    public static final Converter<Integer, Float> IntegerToFloat = new IntegerToFloat(); 
    public static final Converter<Integer, List<Integer>> IntegerToList = new IntegerToList(); 
    public static final Converter<Integer, Long> IntegerToLong = new IntegerToLong(); 
    public static final Converter<Integer, Set<Integer>> IntegerToSet = new IntegerToSet(); 
    public static final LocalizedConverter<Integer, String> IntegerToString = new IntegerToString(); 
    public static final Converter<List<?>, String> ListToString = new ListToString(); 
    public static final Converter<Locale, String> LocaleToString = new LocaleToString(); 
    public static final Converter<Long, BigDecimal> LongToBigDecimal = new LongToBigDecimal(); 
    public static final Converter<Long, Calendar> LongToCalendar = new LongToCalendar(); 
    public static final Converter<Long, Double> LongToDouble = new LongToDouble(); 
    public static final Converter<Long, Float> LongToFloat = new LongToFloat(); 
    public static final Converter<Long, Integer> LongToInteger = new LongToInteger(); 
    public static final Converter<Long, List<Long>> LongToList = new LongToList(); 
    public static final Converter<Long, Set<Long>> LongToSet = new LongToSet(); 
    public static final LocalizedConverter<Long, String> LongToString = new LongToString(); 
    public static final Converter<Map<?, ?>, List<Map<?,?>>> MapToList = new MapToList(); 
    public static final Converter<Map<?, ?>, Set<Map<?,?>>> MapToSet = new MapToSet(); 
    public static final Converter<Map<?, ?>, String> MapToString = new MapToString(); 
    public static final Converter<Number, java.util.Date> NumberToDate = new NumberToDate(); 
    public static final Converter<Number, TimeDuration> NumberToDuration = new NumberToDuration(); 
    public static final Converter<Number, java.sql.Date> NumberToSqlDate = new NumberToSqlDate(); 
    public static final Converter<Number, java.sql.Time> NumberToSqlTime = new NumberToSqlTime(); 
    public static final Converter<Number, java.sql.Timestamp> NumberToTimestamp = new NumberToTimestamp(); 
    public static final LocalizedConverter<java.sql.Date, String> SqlDateToString = new SqlDateToString(); 
    public static final LocalizedConverter<java.sql.Time, String> SqlTimeToString = new SqlTimeToString();
    public static final LocalizedConverter<String, BigDecimal> StringToBigDecimal = new StringToBigDecimal();
    public static final Converter<String, Boolean> StringToBoolean = new StringToBoolean();
    public static final LocalizedConverter<String, Calendar> StringToCalendar = new StringToCalendar();
    public static final LocalizedConverter<String, java.util.Date> StringToDate = new StringToDate();
    public static final LocalizedConverter<String, Double> StringToDouble = new StringToDouble();
    public static final Converter<String, TimeDuration> StringToDuration = new StringToDuration();
    public static final LocalizedConverter<String, Float> StringToFloat = new StringToFloat();
    public static final LocalizedConverter<String, Integer> StringToInteger = new StringToInteger();
    public static final Converter<String, List<?>> StringToList = new StringToList();
    public static final Converter<String, Locale> StringToLocale = new StringToLocale();
    public static final LocalizedConverter<String, Long> StringToLong = new StringToLong();
    public static final Converter<String, Map<?, ?>> StringToMap = new StringToMap();
    public static final Converter<String, Set<?>> StringToSet = new StringToSet();
    public static final LocalizedConverter<String, java.sql.Date> StringToSqlDate = new StringToSqlDate();
    public static final LocalizedConverter<String, java.sql.Time> StringToSqlTime = new StringToSqlTime();
    public static final LocalizedConverter<String, java.sql.Timestamp> StringToTimestamp = new StringToTimestamp();
    public static final Converter<String, TimeZone> StringToTimeZone = new StringToTimeZone();
    public static final Converter<TimeZone, String> TimeZoneToString = new TimeZoneToString();

    private Converters() {}

    /** Returns an appropriate <code>Converter</code> instance for
     * <code>sourceClass</code> and <code>targetClass</code>. If no matching
     * <code>Converter</code> is found, the method throws
     * <code>ClassNotFoundException</code>.
     * 
     * <p>This method is intended to be used when the source or
     * target <code>Object</code> types are unknown at compile time.
     * If the source and target <code>Object</code> types are known
     * at compile time, then one of the "ready made" converters should be used.</p>
     * 
     * @param sourceClass The object class to convert from
     * @param targetClass The object class to convert to
     * @return A matching <code>Converter</code> instance
     * @throws ClassNotFoundException 
     */
    @SuppressWarnings("unchecked")
    public static <S, T> Converter<S, T> getConverter(Class<S> sourceClass, Class<T> targetClass) throws ClassNotFoundException {
        String key = sourceClass.getName().concat(DELIMITER).concat(targetClass.getName());
        Converter<?, ?> result = converterMap.get(key);
        if (result == null) {
            if (!noConversions.contains(key)) {
                synchronized (converterMap) {
                    Collection<Converter<?, ?>> values = converterMap.values();
                    for (Converter<?, ?> value : values) {
                        if (value.canConvert(sourceClass, targetClass)) {
                            converterMap.put(key, value);
                            return (Converter<S, T>) value;
                        }
                    }
                    noConversions.add(key);
                    Debug.logWarning("*** No converter found, converting from " +
                            sourceClass.getName() + " to " + targetClass.getName() +
                            ". Please report this message to the developer community so " +
                            "a suitable converter can be created. ***", module);
                }
            }
            throw new ClassNotFoundException("No converter found for " + key);
        }
        return (Converter<S, T>) result;
    }

    /** Registers a <code>Converter</code> instance to be used by the
     * {@link org.ofbiz.base.conversion.Converters#getConverter(Class, Class)}
     * method.
     * 
     * @param <S> The source object type
     * @param <T> The target object type
     * @param converter The <code>Converter</code> instance to register
     */
    public static <S, T> void registerConverter(Converter<S, T> converter) {
        String key = converter.getSourceClass().getName().concat(DELIMITER).concat(converter.getTargetClass().getName());
        synchronized (converterMap) {
            converterMap.put(key, converter);
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("Registered converter " + converter.getClass().getName(), module);
        }
    }

    protected static class NullConverter implements Converter<Object, Object> {

        public NullConverter() {
            Converters.registerConverter(this);
        }

        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            if (sourceClass.getName().equals(targetClass.getName()) || "java.lang.Object".equals(targetClass.getName())) {
                return true;
            }
            return ObjectType.instanceOf(sourceClass, targetClass);
        }

        public Object convert(Object obj) throws ConversionException {
            return obj;
        }

        public Class<?> getSourceClass() {
            return Object.class;
        }

        public Class<?> getTargetClass() {
            return Object.class;
        }
        
    }

/*
    public static String mapSize() {
        return "Map size = " + converterMap.size();
    }

    @SuppressWarnings("unchecked")
    public static String runPerformanceTest() {
        Object[] intArray = {1,2,3,4};
        List obj = null;
        try {
            Converter<Object[],  List> converter = Converters.getConverter(Object[].class, List.class);
            obj = converter.convert(intArray);
        } catch (Exception e) {}
        System.gc();
        long newStart = System.currentTimeMillis();
        try {
            for (int i = 0; i < 1000000; i++) {
                Converter<Object[],  List> converter = Converters.getConverter(Object[].class, List.class);
                obj = converter.convert(intArray);
            }
        } catch (Exception e) {
            Debug.logError(e, module);
        }
        long newStop = System.currentTimeMillis();
        Debug.logInfo("Elapsed time = " + (newStop - newStart), module);
        return "Elapsed time = " + (newStop - newStart) + ", List size = " + obj.size();
    }
*/

}
