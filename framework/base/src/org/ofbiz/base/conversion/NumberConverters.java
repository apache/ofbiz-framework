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

import org.ofbiz.base.util.StringUtil;

import javolution.util.FastList;
import javolution.util.FastSet;

/** Number Converter classes. */
public class NumberConverters implements ConverterLoader {
    public static final String module = NumberConverters.class.getName();

    public static abstract class AbstractToNumberConverter<S, T> extends AbstractUsesLocaleConverter<S, T> {
        protected AbstractToNumberConverter(Class<S> sourceClass, Class<T> targetClass) {
            super(sourceClass, targetClass);
        }

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
        protected AbstractUsesLocaleConverter(Class<S> sourceClass, Class<T> targetClass) {
            super(sourceClass, targetClass);
        }

        public T convert(S obj) throws ConversionException {
            return convert(obj, Locale.getDefault(), null);
        }

        public T convert(S obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            return convert(obj, locale, null);
        }
    }

    public static class BigDecimalToDouble extends AbstractConverter<BigDecimal, Double> {
        public BigDecimalToDouble() {
            super(BigDecimal.class, Double.class);
        }

        public Double convert(BigDecimal obj) throws ConversionException {
            return Double.valueOf(obj.doubleValue());
        }
    }

    public static class BigDecimalToFloat extends AbstractConverter<BigDecimal, Float> {
        public BigDecimalToFloat() {
            super(BigDecimal.class, Float.class);
        }

        public Float convert(BigDecimal obj) throws ConversionException {
            return Float.valueOf(obj.floatValue());
        }
    }

    public static class BigDecimalToInteger extends AbstractConverter<BigDecimal, Integer> {
        public BigDecimalToInteger() {
            super(BigDecimal.class, Integer.class);
        }

        public Integer convert(BigDecimal obj) throws ConversionException {
            return Integer.valueOf(obj.intValue());
        }
    }

    public static class BigDecimalToList extends AbstractConverter<BigDecimal, List<BigDecimal>> {
        public BigDecimalToList() {
            super(BigDecimal.class, List.class);
        }

        public List<BigDecimal> convert(BigDecimal obj) throws ConversionException {
            List<BigDecimal> tempList = FastList.newInstance();
            tempList.add(obj);
            return tempList;
        }
    }

    public static class BigDecimalToLong extends AbstractConverter<BigDecimal, Long> {
        public BigDecimalToLong() {
            super(BigDecimal.class, Long.class);
        }

        public Long convert(BigDecimal obj) throws ConversionException {
            return Long.valueOf(obj.longValue());
        }
    }

    public static class BigDecimalToSet extends AbstractConverter<BigDecimal, Set<BigDecimal>> {
        public BigDecimalToSet() {
            super(BigDecimal.class, Set.class);
        }

        public Set<BigDecimal> convert(BigDecimal obj) throws ConversionException {
            Set<BigDecimal> tempSet = FastSet.newInstance();
            tempSet.add(obj);
            return tempSet;
        }
    }

    public static class BigDecimalToString extends AbstractUsesLocaleConverter<BigDecimal, String> {
        public BigDecimalToString() {
            super(BigDecimal.class, String.class);
        }

        public String convert(BigDecimal obj, Locale locale, TimeZone timeZone) throws ConversionException {
            NumberFormat nf = NumberFormat.getNumberInstance(locale);
            return nf.format(obj.doubleValue());
        }
    }

    public static class DoubleToBigDecimal extends AbstractConverter<Double, BigDecimal> {
        public DoubleToBigDecimal() {
            super(Double.class, BigDecimal.class);
        }

        public BigDecimal convert(Double obj) throws ConversionException {
            return BigDecimal.valueOf(obj.doubleValue());
        }
    }

    public static class DoubleToFloat extends AbstractConverter<Double, Float> {
        public DoubleToFloat() {
            super(Double.class, Float.class);
        }

        public Float convert(Double obj) throws ConversionException {
            return Float.valueOf(obj.floatValue());
        }
    }

    public static class DoubleToInteger extends AbstractConverter<Double, Integer> {
        public DoubleToInteger() {
            super(Double.class, Integer.class);
        }

        public Integer convert(Double obj) throws ConversionException {
            return Integer.valueOf(obj.intValue());
        }
    }

    public static class DoubleToList extends AbstractConverter<Double, List<Double>> {
        public DoubleToList() {
            super(Double.class, List.class);
        }

        public List<Double> convert(Double obj) throws ConversionException {
            List<Double> tempList = FastList.newInstance();
            tempList.add(obj);
            return tempList;
        }
    }

    public static class DoubleToLong extends AbstractConverter<Double, Long> {
        public DoubleToLong() {
            super(Double.class, Long.class);
        }

        public Long convert(Double obj) throws ConversionException {
            return Long.valueOf(obj.longValue());
        }
    }

    public static class DoubleToSet extends AbstractConverter<Double, Set<Double>> {
        public DoubleToSet() {
            super(Double.class, Set.class);
        }

        public Set<Double> convert(Double obj) throws ConversionException {
            Set<Double> tempSet = FastSet.newInstance();
            tempSet.add(obj);
            return tempSet;
        }
    }

    public static class DoubleToString extends AbstractUsesLocaleConverter<Double, String> {
        public DoubleToString() {
            super(Double.class, String.class);
        }

        public String convert(Double obj, Locale locale, TimeZone timeZone) throws ConversionException {
            NumberFormat nf = NumberFormat.getNumberInstance(locale);
            return nf.format(obj.doubleValue());
        }
    }

    public static class FloatToBigDecimal extends AbstractConverter<Float, BigDecimal> {
        public FloatToBigDecimal() {
            super(Float.class, BigDecimal.class);
        }

        public BigDecimal convert(Float obj) throws ConversionException {
            return BigDecimal.valueOf(obj.doubleValue());
        }
    }

    public static class FloatToDouble extends AbstractConverter<Float, Double> {
        public FloatToDouble() {
            super(Float.class, Double.class);
        }

        public Double convert(Float obj) throws ConversionException {
            return Double.valueOf(obj.doubleValue());
        }
    }

    public static class FloatToInteger extends AbstractConverter<Float, Integer> {
        public FloatToInteger() {
            super(Float.class, Integer.class);
        }

        public Integer convert(Float obj) throws ConversionException {
            return Integer.valueOf(obj.intValue());
        }
    }

    public static class FloatToList extends AbstractConverter<Float, List<Float>> {
        public FloatToList() {
            super(Float.class, List.class);
        }

        public List<Float> convert(Float obj) throws ConversionException {
            List<Float> tempList = FastList.newInstance();
            tempList.add(obj);
            return tempList;
        }
    }

    public static class FloatToLong extends AbstractConverter<Float, Long> {
        public FloatToLong() {
            super(Float.class, Long.class);
        }

        public Long convert(Float obj) throws ConversionException {
            return Long.valueOf(obj.longValue());
        }
    }

    public static class FloatToSet extends AbstractConverter<Float, Set<Float>> {
        public FloatToSet() {
            super(Float.class, Set.class);
        }

        public Set<Float> convert(Float obj) throws ConversionException {
            Set<Float> tempSet = FastSet.newInstance();
            tempSet.add(obj);
            return tempSet;
        }
    }

    public static class FloatToString extends AbstractUsesLocaleConverter<Float, String> {
        public FloatToString() {
            super(Float.class, String.class);
        }

        public String convert(Float obj, Locale locale, TimeZone timeZone) throws ConversionException {
            NumberFormat nf = NumberFormat.getNumberInstance(locale);
            return nf.format(obj.floatValue());
        }
    }

    public static class IntegerToBigDecimal extends AbstractConverter<Integer, BigDecimal> {
        public IntegerToBigDecimal() {
            super(Integer.class, BigDecimal.class);
        }

        public BigDecimal convert(Integer obj) throws ConversionException {
            return BigDecimal.valueOf(obj.doubleValue());
        }
    }

    public static class IntegerToDouble extends AbstractConverter<Integer, Double> {
        public IntegerToDouble() {
            super(Integer.class, Double.class);
        }

        public Double convert(Integer obj) throws ConversionException {
            return Double.valueOf(obj.doubleValue());
        }
    }

    public static class IntegerToFloat extends AbstractConverter<Integer, Float> {
        public IntegerToFloat() {
            super(Integer.class, Float.class);
        }

        public Float convert(Integer obj) throws ConversionException {
            return Float.valueOf(obj.floatValue());
        }
    }

    public static class IntegerToList extends AbstractConverter<Integer, List<Integer>> {
        public IntegerToList() {
            super(Integer.class, List.class);
        }

        public List<Integer> convert(Integer obj) throws ConversionException {
            List<Integer> tempList = FastList.newInstance();
            tempList.add(obj);
            return tempList;
        }
    }

    public static class IntegerToLong extends AbstractConverter<Integer, Long> {
        public IntegerToLong() {
            super(Integer.class, Long.class);
        }

        public Long convert(Integer obj) throws ConversionException {
            return Long.valueOf(obj.longValue());
        }
    }

    public static class IntegerToSet extends AbstractConverter<Integer, Set<Integer>> {
        public IntegerToSet() {
            super(Integer.class, Set.class);
        }

        public Set<Integer> convert(Integer obj) throws ConversionException {
            Set<Integer> tempSet = FastSet.newInstance();
            tempSet.add(obj);
            return tempSet;
        }
    }

    public static class IntegerToString extends AbstractUsesLocaleConverter<Integer, String> {
        public IntegerToString() {
            super(Integer.class, String.class);
        }

        public String convert(Integer obj, Locale locale, TimeZone timeZone) throws ConversionException {
            NumberFormat nf = NumberFormat.getNumberInstance(locale);
            return nf.format(obj.intValue());
        }
    }

    public static class LongToBigDecimal extends AbstractConverter<Long, BigDecimal> {
        public LongToBigDecimal() {
            super(Long.class, BigDecimal.class);
        }

        public BigDecimal convert(Long obj) throws ConversionException {
            return BigDecimal.valueOf(obj.doubleValue());
        }
    }

    public static class LongToDouble extends AbstractConverter<Long, Double> {
        public LongToDouble() {
            super(Long.class, Double.class);
        }

        public Double convert(Long obj) throws ConversionException {
            return Double.valueOf(obj.doubleValue());
        }
    }

    public static class LongToFloat extends AbstractConverter<Long, Float> {
        public LongToFloat() {
            super(Long.class, Float.class);
        }

        public Float convert(Long obj) throws ConversionException {
            return Float.valueOf(obj.floatValue());
        }
    }

    public static class LongToInteger extends AbstractConverter<Long, Integer> {
        public LongToInteger() {
            super(Long.class, Integer.class);
        }

        public Integer convert(Long obj) throws ConversionException {
            return Integer.valueOf(obj.intValue());
        }
    }

    public static class LongToList extends AbstractConverter<Long, List<Long>> {
        public LongToList() {
            super(Long.class, List.class);
        }

        public List<Long> convert(Long obj) throws ConversionException {
            List<Long> tempList = FastList.newInstance();
            tempList.add(obj);
            return tempList;
        }
    }

    public static class LongToSet extends AbstractConverter<Long, Set<Long>> {
        public LongToSet() {
            super(Long.class, Set.class);
        }

        public Set<Long> convert(Long obj) throws ConversionException {
            Set<Long> tempSet = FastSet.newInstance();
            tempSet.add(obj);
            return tempSet;
        }
    }

    public static class LongToString extends AbstractUsesLocaleConverter<Long, String> {
        public LongToString() {
            super(Long.class, String.class);
        }

        public String convert(Long obj, Locale locale, TimeZone timeZone) throws ConversionException {
            NumberFormat nf = NumberFormat.getNumberInstance(locale);
            return nf.format(obj.longValue());
        }
    }

    public static class StringToBigDecimal extends AbstractToNumberConverter<String, BigDecimal> {
        public StringToBigDecimal() {
            super(String.class, BigDecimal.class);
        }

        public BigDecimal convert(String obj, Locale locale, TimeZone timeZone) throws ConversionException {
            String trimStr = StringUtil.removeSpaces(obj);
            if (trimStr.length() == 0) {
                return null;
            }
            return BigDecimal.valueOf(this.fromString(trimStr, locale).doubleValue());
        }
    }

    public static class StringToDouble extends AbstractToNumberConverter<String, Double> {
        public StringToDouble() {
            super(String.class, Double.class);
        }

        public Double convert(String obj, Locale locale, TimeZone timeZone) throws ConversionException {
            String trimStr = StringUtil.removeSpaces(obj);
            if (trimStr.length() == 0) {
                return null;
            }
            return this.fromString(trimStr, locale).doubleValue();
        }
    }

    public static class StringToFloat extends AbstractToNumberConverter<String, Float> {
        public StringToFloat() {
            super(String.class, Float.class);
        }

        public Float convert(String obj, Locale locale, TimeZone timeZone) throws ConversionException {
            String trimStr = StringUtil.removeSpaces(obj);
            if (trimStr.length() == 0) {
                return null;
            }
            return this.fromString(trimStr, locale).floatValue();
        }
    }

    public static class StringToInteger extends AbstractToNumberConverter<String, Integer> {
        public StringToInteger() {
            super(String.class, Integer.class);
        }

        public Integer convert(String obj, Locale locale, TimeZone timeZone) throws ConversionException {
            String trimStr = StringUtil.removeSpaces(obj);
            if (trimStr.length() == 0) {
                return null;
            }
            return this.fromString(trimStr, locale).intValue();
        }
    }

    public static class StringToLong extends AbstractToNumberConverter<String, Long> {
        public StringToLong() {
            super(String.class, Long.class);
        }

        public Long convert(String obj, Locale locale, TimeZone timeZone) throws ConversionException {
            String trimStr = StringUtil.removeSpaces(obj);
            if (trimStr.length() == 0) {
                return null;
            }
            return this.fromString(trimStr, locale).longValue();
        }
    }

    public void loadConverters() {
        Converters.loadContainedConverters(NumberConverters.class);
    }
}
