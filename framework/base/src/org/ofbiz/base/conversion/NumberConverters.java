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
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import javolution.util.FastList;
import javolution.util.FastSet;

/** Number Converter classes. */
public class NumberConverters implements ConverterLoader {

    public static final String module = NumberConverters.class.getName();

    public static abstract class AbstractToNumberConverter<S, T> extends AbstractUsesLocaleConverter<S, T> {

        protected Number fromString(String str, Locale locale) throws ConversionException {
            NumberFormat nf = NumberFormat.getNumberInstance(locale);
            try {
                return nf.parse(str);
            } catch (ParseException e) {
                throw new ConversionException(e);
            }
        }

    }

    public static abstract class AbstractUsesLocaleConverter<S, T> extends AbstractLocalizedConverter<S, T> {

        public T convert(S obj) throws ConversionException {
            return convert(obj, Locale.getDefault(), null);
        }

        public T convert(S obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            return convert(obj, Locale.getDefault(), null);
        }


    }

    public static class BigDecimalToDouble extends AbstractConverter<BigDecimal, Double> {

        public Double convert(BigDecimal obj) throws ConversionException {
            return Double.valueOf(obj.doubleValue());
        }

        public Class<BigDecimal> getSourceClass() {
            return BigDecimal.class;
        }

        public Class<Double> getTargetClass() {
            return Double.class;
        }
        
    }

    public static class BigDecimalToFloat extends AbstractConverter<BigDecimal, Float> {

        public Float convert(BigDecimal obj) throws ConversionException {
            return Float.valueOf(obj.floatValue());
        }

        public Class<BigDecimal> getSourceClass() {
            return BigDecimal.class;
        }

        public Class<Float> getTargetClass() {
            return Float.class;
        }
        
    }

    public static class BigDecimalToInteger extends AbstractConverter<BigDecimal, Integer> {

        public Integer convert(BigDecimal obj) throws ConversionException {
            return Integer.valueOf(obj.intValue());
        }

        public Class<BigDecimal> getSourceClass() {
            return BigDecimal.class;
        }

        public Class<Integer> getTargetClass() {
            return Integer.class;
        }
        
    }

    public static class BigDecimalToList extends AbstractConverter<BigDecimal, List<BigDecimal>> {

        public List<BigDecimal> convert(BigDecimal obj) throws ConversionException {
            List<BigDecimal> tempList = FastList.newInstance();
            tempList.add(obj);
            return tempList;
        }

        public Class<BigDecimal> getSourceClass() {
            return BigDecimal.class;
        }

        public Class<?> getTargetClass() {
            return List.class;
        }
        
    }

    public static class BigDecimalToLong extends AbstractConverter<BigDecimal, Long> {

        public Long convert(BigDecimal obj) throws ConversionException {
            return Long.valueOf(obj.longValue());
        }

        public Class<BigDecimal> getSourceClass() {
            return BigDecimal.class;
        }

        public Class<Long> getTargetClass() {
            return Long.class;
        }
        
    }

    public static class BigDecimalToSet extends AbstractConverter<BigDecimal, Set<BigDecimal>> {

        public Set<BigDecimal> convert(BigDecimal obj) throws ConversionException {
            Set<BigDecimal> tempSet = FastSet.newInstance();
            tempSet.add(obj);
            return tempSet;
        }

        public Class<BigDecimal> getSourceClass() {
            return BigDecimal.class;
        }

        public Class<?> getTargetClass() {
            return Set.class;
        }
        
    }

    public static class BigDecimalToString extends AbstractUsesLocaleConverter<BigDecimal, String> {

        public String convert(BigDecimal obj, Locale locale, TimeZone timeZone) throws ConversionException {
            NumberFormat nf = NumberFormat.getNumberInstance(locale);
            return nf.format(obj.doubleValue());
        }

        public Class<BigDecimal> getSourceClass() {
            return BigDecimal.class;
        }

        public Class<String> getTargetClass() {
            return String.class;
        }
        
    }

    public static class DoubleToBigDecimal extends AbstractConverter<Double, BigDecimal> {

        public BigDecimal convert(Double obj) throws ConversionException {
            return BigDecimal.valueOf(obj.doubleValue());
        }

        public Class<Double> getSourceClass() {
            return Double.class;
        }

        public Class<BigDecimal> getTargetClass() {
            return BigDecimal.class;
        }
        
    }

    public static class DoubleToFloat extends AbstractConverter<Double, Float> {

        public Float convert(Double obj) throws ConversionException {
            return Float.valueOf(obj.floatValue());
        }

        public Class<Double> getSourceClass() {
            return Double.class;
        }

        public Class<Float> getTargetClass() {
            return Float.class;
        }
        
    }

    public static class DoubleToInteger extends AbstractConverter<Double, Integer> {

        public Integer convert(Double obj) throws ConversionException {
            return Integer.valueOf(obj.intValue());
        }

        public Class<Double> getSourceClass() {
            return Double.class;
        }

        public Class<Integer> getTargetClass() {
            return Integer.class;
        }
        
    }

    public static class DoubleToList extends AbstractConverter<Double, List<Double>> {

        public List<Double> convert(Double obj) throws ConversionException {
            List<Double> tempList = FastList.newInstance();
            tempList.add(obj);
            return tempList;
        }

        public Class<Double> getSourceClass() {
            return Double.class;
        }

        public Class<?> getTargetClass() {
            return List.class;
        }
        
    }

    public static class DoubleToLong extends AbstractConverter<Double, Long> {

        public Long convert(Double obj) throws ConversionException {
            return Long.valueOf(obj.longValue());
        }

        public Class<Double> getSourceClass() {
            return Double.class;
        }

        public Class<Long> getTargetClass() {
            return Long.class;
        }
        
    }

    public static class DoubleToSet extends AbstractConverter<Double, Set<Double>> {

        public Set<Double> convert(Double obj) throws ConversionException {
            Set<Double> tempSet = FastSet.newInstance();
            tempSet.add(obj);
            return tempSet;
        }

        public Class<Double> getSourceClass() {
            return Double.class;
        }

        public Class<?> getTargetClass() {
            return Set.class;
        }
        
    }

    public static class DoubleToString extends AbstractUsesLocaleConverter<Double, String> {

        public String convert(Double obj, Locale locale, TimeZone timeZone) throws ConversionException {
            NumberFormat nf = NumberFormat.getNumberInstance(locale);
            return nf.format(obj.doubleValue());
        }

        public Class<Double> getSourceClass() {
            return Double.class;
        }

        public Class<String> getTargetClass() {
            return String.class;
        }
        
    }

    public static class FloatToBigDecimal extends AbstractConverter<Float, BigDecimal> {

        public BigDecimal convert(Float obj) throws ConversionException {
            return BigDecimal.valueOf(obj.doubleValue());
        }

        public Class<Float> getSourceClass() {
            return Float.class;
        }

        public Class<BigDecimal> getTargetClass() {
            return BigDecimal.class;
        }
        
    }

    public static class FloatToDouble extends AbstractConverter<Float, Double> {

        public Double convert(Float obj) throws ConversionException {
            return Double.valueOf(obj.doubleValue());
        }

        public Class<Float> getSourceClass() {
            return Float.class;
        }

        public Class<Double> getTargetClass() {
            return Double.class;
        }
        
    }

    public static class FloatToInteger extends AbstractConverter<Float, Integer> {

        public Integer convert(Float obj) throws ConversionException {
            return Integer.valueOf(obj.intValue());
        }

        public Class<Float> getSourceClass() {
            return Float.class;
        }

        public Class<Integer> getTargetClass() {
            return Integer.class;
        }
        
    }

    public static class FloatToList extends AbstractConverter<Float, List<Float>> {

        public List<Float> convert(Float obj) throws ConversionException {
            List<Float> tempList = FastList.newInstance();
            tempList.add(obj);
            return tempList;
        }

        public Class<Float> getSourceClass() {
            return Float.class;
        }

        public Class<?> getTargetClass() {
            return List.class;
        }
        
    }

    public static class FloatToLong extends AbstractConverter<Float, Long> {

        public Long convert(Float obj) throws ConversionException {
            return Long.valueOf(obj.longValue());
        }

        public Class<Float> getSourceClass() {
            return Float.class;
        }

        public Class<Long> getTargetClass() {
            return Long.class;
        }
        
    }

    public static class FloatToSet extends AbstractConverter<Float, Set<Float>> {

        public Set<Float> convert(Float obj) throws ConversionException {
            Set<Float> tempSet = FastSet.newInstance();
            tempSet.add(obj);
            return tempSet;
        }

        public Class<Float> getSourceClass() {
            return Float.class;
        }

        public Class<?> getTargetClass() {
            return Set.class;
        }
        
    }

    public static class FloatToString extends AbstractUsesLocaleConverter<Float, String> {

        public String convert(Float obj, Locale locale, TimeZone timeZone) throws ConversionException {
            NumberFormat nf = NumberFormat.getNumberInstance(locale);
            return nf.format(obj.floatValue());
        }

        public Class<Float> getSourceClass() {
            return Float.class;
        }

        public Class<String> getTargetClass() {
            return String.class;
        }
        
    }

    public static class IntegerToBigDecimal extends AbstractConverter<Integer, BigDecimal> {

        public BigDecimal convert(Integer obj) throws ConversionException {
            return BigDecimal.valueOf(obj.doubleValue());
        }

        public Class<Integer> getSourceClass() {
            return Integer.class;
        }

        public Class<BigDecimal> getTargetClass() {
            return BigDecimal.class;
        }
        
    }

    public static class IntegerToDouble extends AbstractConverter<Integer, Double> {

        public Double convert(Integer obj) throws ConversionException {
            return Double.valueOf(obj.doubleValue());
        }

        public Class<Integer> getSourceClass() {
            return Integer.class;
        }

        public Class<Double> getTargetClass() {
            return Double.class;
        }
        
    }

    public static class IntegerToFloat extends AbstractConverter<Integer, Float> {

        public Float convert(Integer obj) throws ConversionException {
            return Float.valueOf(obj.floatValue());
        }

        public Class<Integer> getSourceClass() {
            return Integer.class;
        }

        public Class<Float> getTargetClass() {
            return Float.class;
        }
        
    }

    public static class IntegerToList extends AbstractConverter<Integer, List<Integer>> {

        public List<Integer> convert(Integer obj) throws ConversionException {
            List<Integer> tempList = FastList.newInstance();
            tempList.add(obj);
            return tempList;
        }

        public Class<Integer> getSourceClass() {
            return Integer.class;
        }

        public Class<?> getTargetClass() {
            return List.class;
        }
        
    }

    public static class IntegerToLong extends AbstractConverter<Integer, Long> {

        public Long convert(Integer obj) throws ConversionException {
            return Long.valueOf(obj.longValue());
        }

        public Class<Integer> getSourceClass() {
            return Integer.class;
        }

        public Class<Long> getTargetClass() {
            return Long.class;
        }
        
    }

    public static class IntegerToSet extends AbstractConverter<Integer, Set<Integer>> {

        public Set<Integer> convert(Integer obj) throws ConversionException {
            Set<Integer> tempSet = FastSet.newInstance();
            tempSet.add(obj);
            return tempSet;
        }

        public Class<Integer> getSourceClass() {
            return Integer.class;
        }

        public Class<?> getTargetClass() {
            return Set.class;
        }
        
    }

    public static class IntegerToString extends AbstractUsesLocaleConverter<Integer, String> {

        public String convert(Integer obj, Locale locale, TimeZone timeZone) throws ConversionException {
            NumberFormat nf = NumberFormat.getNumberInstance(locale);
            return nf.format(obj.intValue());
        }

        public Class<Integer> getSourceClass() {
            return Integer.class;
        }

        public Class<String> getTargetClass() {
            return String.class;
        }
        
    }

    public static class LongToBigDecimal extends AbstractConverter<Long, BigDecimal> {

        public BigDecimal convert(Long obj) throws ConversionException {
            return BigDecimal.valueOf(obj.doubleValue());
        }

        public Class<Long> getSourceClass() {
            return Long.class;
        }

        public Class<BigDecimal> getTargetClass() {
            return BigDecimal.class;
        }
        
    }

    public static class LongToDouble extends AbstractConverter<Long, Double> {

        public Double convert(Long obj) throws ConversionException {
            return Double.valueOf(obj.doubleValue());
        }

        public Class<Long> getSourceClass() {
            return Long.class;
        }

        public Class<Double> getTargetClass() {
            return Double.class;
        }
        
    }

    public static class LongToFloat extends AbstractConverter<Long, Float> {

        public Float convert(Long obj) throws ConversionException {
            return Float.valueOf(obj.floatValue());
        }

        public Class<Long> getSourceClass() {
            return Long.class;
        }

        public Class<Float> getTargetClass() {
            return Float.class;
        }
        
    }

    public static class LongToInteger extends AbstractConverter<Long, Integer> {

        public Integer convert(Long obj) throws ConversionException {
            return Integer.valueOf(obj.intValue());
        }

        public Class<Long> getSourceClass() {
            return Long.class;
        }

        public Class<Integer> getTargetClass() {
            return Integer.class;
        }
        
    }

    public static class LongToList extends AbstractConverter<Long, List<Long>> {

        public List<Long> convert(Long obj) throws ConversionException {
            List<Long> tempList = FastList.newInstance();
            tempList.add(obj);
            return tempList;
        }

        public Class<Long> getSourceClass() {
            return Long.class;
        }

        public Class<?> getTargetClass() {
            return List.class;
        }
        
    }

    public static class LongToSet extends AbstractConverter<Long, Set<Long>> {

        public Set<Long> convert(Long obj) throws ConversionException {
            Set<Long> tempSet = FastSet.newInstance();
            tempSet.add(obj);
            return tempSet;
        }

        public Class<Long> getSourceClass() {
            return Long.class;
        }

        public Class<?> getTargetClass() {
            return Set.class;
        }
        
    }

    public static class LongToString extends AbstractUsesLocaleConverter<Long, String> {

        public String convert(Long obj, Locale locale, TimeZone timeZone) throws ConversionException {
            NumberFormat nf = NumberFormat.getNumberInstance(locale);
            return nf.format(obj.longValue());
        }

        public Class<Long> getSourceClass() {
            return Long.class;
        }

        public Class<String> getTargetClass() {
            return String.class;
        }
        
    }

    public static class StringToBigDecimal extends AbstractToNumberConverter<String, BigDecimal> {

        public BigDecimal convert(String obj, Locale locale, TimeZone timeZone) throws ConversionException {
            return BigDecimal.valueOf(this.fromString(obj, locale).doubleValue());
        }

        public Class<String> getSourceClass() {
            return String.class;
        }

        public Class<BigDecimal> getTargetClass() {
            return BigDecimal.class;
        }
        
    }

    public static class StringToDouble extends AbstractToNumberConverter<String, Double> {

        public Double convert(String obj, Locale locale, TimeZone timeZone) throws ConversionException {
            return this.fromString(obj, locale).doubleValue();
        }

        public Class<String> getSourceClass() {
            return String.class;
        }

        public Class<Double> getTargetClass() {
            return Double.class;
        }
        
    }

    public static class StringToFloat extends AbstractToNumberConverter<String, Float> {

        public Float convert(String obj, Locale locale, TimeZone timeZone) throws ConversionException {
            return this.fromString(obj, locale).floatValue();
        }

        public Class<String> getSourceClass() {
            return String.class;
        }

        public Class<Float> getTargetClass() {
            return Float.class;
        }
        
    }

    public static class StringToInteger extends AbstractToNumberConverter<String, Integer> {

        public Integer convert(String obj, Locale locale, TimeZone timeZone) throws ConversionException {
            return this.fromString(obj, locale).intValue();
        }

        public Class<String> getSourceClass() {
            return String.class;
        }

        public Class<Integer> getTargetClass() {
            return Integer.class;
        }
        
    }

    public static class StringToLong extends AbstractToNumberConverter<String, Long> {

        public Long convert(String obj, Locale locale, TimeZone timeZone) throws ConversionException {
            return this.fromString(obj, locale).longValue();
        }

        public Class<String> getSourceClass() {
            return String.class;
        }

        public Class<Long> getTargetClass() {
            return Long.class;
        }
        
    }

    public void loadConverters() {
        Converters.loadContainedConverters(NumberConverters.class);
    }

}
