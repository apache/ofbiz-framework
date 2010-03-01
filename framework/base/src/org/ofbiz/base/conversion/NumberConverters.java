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
import java.math.BigInteger;
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

    protected static Number fromString(String str, Locale locale) throws ConversionException {
        NumberFormat nf = NumberFormat.getNumberInstance(locale);
        try {
            return nf.parse(str);
        } catch (ParseException e) {
            throw new ConversionException(e);
        }
    }

    public static abstract class AbstractStringToNumberConverter<N extends Number> extends AbstractNumberConverter<String, N> {
        public AbstractStringToNumberConverter(Class<N> targetClass) {
            super(String.class, targetClass);
        }

        public N convert(String obj, Locale locale, TimeZone timeZone) throws ConversionException {
            String trimStr = StringUtil.removeSpaces(obj);
            if (trimStr.length() == 0) {
                return null;
            }
            return convert(fromString(trimStr, locale));
        }

        protected abstract N convert(Number number) throws ConversionException;
    }

    public static abstract class AbstractNumberConverter<S, T> extends AbstractLocalizedConverter<S, T> {
        protected AbstractNumberConverter(Class<S> sourceClass, Class<T> targetClass) {
            super(sourceClass, targetClass);
        }

        public T convert(S obj, Locale locale, TimeZone timeZone, String formatString) throws ConversionException {
            return convert(obj, locale, null);
        }
    }

    public static abstract class AbstractNumberToStringConverter<N extends Number> extends AbstractNumberConverter<N, String> {
        public AbstractNumberToStringConverter(Class<N> sourceClass) {
            super(sourceClass, String.class);
        }

        public String convert(N obj) throws ConversionException {
            return obj.toString();
        }

        public String convert(N obj, Locale locale, TimeZone timeZone) throws ConversionException {
            return format(obj, NumberFormat.getNumberInstance(locale));
        }

        protected abstract String format(N obj, NumberFormat nf) throws ConversionException;
    }

    public static class GenericNumberToDouble<N extends Number> extends AbstractConverter<N, Double> {
        public GenericNumberToDouble(Class<N> sourceClass) {
            super(sourceClass, Double.class);
        }

        public Double convert(N obj) throws ConversionException {
            return obj.doubleValue();
        }
    }

    public static class GenericNumberToFloat<N extends Number> extends AbstractConverter<N, Float> {
        public GenericNumberToFloat(Class<N> sourceClass) {
            super(sourceClass, Float.class);
        }

        public Float convert(N obj) throws ConversionException {
            return obj.floatValue();
        }
    }

    public static class GenericNumberToInteger<N extends Number> extends AbstractConverter<N, Integer> {
        public GenericNumberToInteger(Class<N> sourceClass) {
            super(sourceClass, Integer.class);
        }

        public Integer convert(N obj) throws ConversionException {
            return obj.intValue();
        }
    }

    public static class GenericNumberToLong<N extends Number> extends AbstractConverter<N, Long> {
        public GenericNumberToLong(Class<N> sourceClass) {
            super(sourceClass, Long.class);
        }

        public Long convert(N obj) throws ConversionException {
            return obj.longValue();
        }
    }

    public static class GenericNumberToShort<N extends Number> extends AbstractConverter<N, Short> {
        public GenericNumberToShort(Class<N> sourceClass) {
            super(sourceClass, Short.class);
        }

        public Short convert(N obj) throws ConversionException {
            return obj.shortValue();
        }
    }

    public static class BigDecimalToDouble extends GenericNumberToDouble<BigDecimal> {
        public BigDecimalToDouble() {
            super(BigDecimal.class);
        }
    }

    public static class BigDecimalToFloat extends GenericNumberToFloat<BigDecimal> {
        public BigDecimalToFloat() {
            super(BigDecimal.class);
        }
    }

    public static class BigDecimalToInteger extends GenericNumberToInteger<BigDecimal> {
        public BigDecimalToInteger() {
            super(BigDecimal.class);
        }
    }

    public static class BigDecimalToList extends GenericSingletonToList<BigDecimal> {
        public BigDecimalToList() {
            super(BigDecimal.class);
        }
    }

    public static class BigDecimalToLong extends GenericNumberToLong<BigDecimal> {
        public BigDecimalToLong() {
            super(BigDecimal.class);
        }
    }

    public static class BigDecimalToSet extends GenericSingletonToSet<BigDecimal> {
        public BigDecimalToSet() {
            super(BigDecimal.class);
        }
    }

    public static class BigDecimalToString extends AbstractNumberToStringConverter<BigDecimal> {
        public BigDecimalToString() {
            super(BigDecimal.class);
        }

        protected String format(BigDecimal obj, NumberFormat nf) throws ConversionException {
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

    public static class BigIntegerToDouble extends GenericNumberToDouble<BigInteger> {
        public BigIntegerToDouble() {
            super(BigInteger.class);
        }
    }

    public static class BigIntegerToFloat extends GenericNumberToFloat<BigInteger> {
        public BigIntegerToFloat() {
            super(BigInteger.class);
        }
    }

    public static class BigIntegerToInteger extends GenericNumberToInteger<BigInteger> {
        public BigIntegerToInteger() {
            super(BigInteger.class);
        }
    }

    public static class BigIntegerToList extends GenericSingletonToList<BigInteger> {
        public BigIntegerToList() {
            super(BigInteger.class);
        }
    }

    public static class BigIntegerToLong extends GenericNumberToLong<BigInteger> {
        public BigIntegerToLong() {
            super(BigInteger.class);
        }
    }

    public static class BigIntegerToSet extends GenericSingletonToSet<BigInteger> {
        public BigIntegerToSet() {
            super(BigInteger.class);
        }
    }

    public static class BigIntegerToString extends AbstractNumberToStringConverter<BigInteger> {
        public BigIntegerToString() {
            super(BigInteger.class);
        }

        protected String format(BigInteger obj, NumberFormat nf) throws ConversionException {
            return nf.format(obj.doubleValue());
        }
    }

    public static class ByteToDouble extends GenericNumberToDouble<Byte> {
        public ByteToDouble() {
            super(Byte.class);
        }
    }

    public static class ByteToFloat extends GenericNumberToFloat<Byte> {
        public ByteToFloat() {
            super(Byte.class);
        }
    }

    public static class ByteToInteger extends GenericNumberToInteger<Byte> {
        public ByteToInteger() {
            super(Byte.class);
        }
    }

    public static class ByteToList extends GenericSingletonToList<Byte> {
        public ByteToList() {
            super(Byte.class);
        }
    }

    public static class ByteToLong extends GenericNumberToLong<Byte> {
        public ByteToLong() {
            super(Byte.class);
        }
    }

    public static class ByteToSet extends GenericSingletonToSet<Byte> {
        public ByteToSet() {
            super(Byte.class);
        }
    }

    public static class ByteToString extends AbstractNumberToStringConverter<Byte> {
        public ByteToString() {
            super(Byte.class);
        }

        protected String format(Byte obj, NumberFormat nf) throws ConversionException {
            return nf.format(obj.floatValue());
        }
    }

    public static class StringToBigInteger extends AbstractStringToNumberConverter<BigInteger> {
        public StringToBigInteger() {
            super(BigInteger.class);
        }

        public BigInteger convert(String obj) throws ConversionException {
            return new BigInteger(obj);
        }

        protected BigInteger convert(Number number) throws ConversionException {
            return BigInteger.valueOf(number.longValue());
        }
    }

    public static class DoubleToFloat extends GenericNumberToFloat<Double> {
        public DoubleToFloat() {
            super(Double.class);
        }
    }

    public static class DoubleToInteger extends GenericNumberToInteger<Double> {
        public DoubleToInteger() {
            super(Double.class);
        }
    }

    public static class DoubleToList extends GenericSingletonToList<Double> {
        public DoubleToList() {
            super(Double.class);
        }
    }

    public static class DoubleToLong extends GenericNumberToLong<Double> {
        public DoubleToLong() {
            super(Double.class);
        }
    }

    public static class DoubleToSet extends GenericSingletonToSet<Double> {
        public DoubleToSet() {
            super(Double.class);
        }
    }

    public static class DoubleToString extends AbstractNumberToStringConverter<Double> {
        public DoubleToString() {
            super(Double.class);
        }

        protected String format(Double obj, NumberFormat nf) throws ConversionException {
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

    public static class FloatToDouble extends GenericNumberToDouble<Float> {
        public FloatToDouble() {
            super(Float.class);
        }
    }

    public static class FloatToInteger extends GenericNumberToInteger<Float> {
        public FloatToInteger() {
            super(Float.class);
        }
    }

    public static class FloatToList extends GenericSingletonToList<Float> {
        public FloatToList() {
            super(Float.class);
        }
    }

    public static class FloatToLong extends GenericNumberToLong<Float> {
        public FloatToLong() {
            super(Float.class);
        }
    }

    public static class FloatToSet extends GenericSingletonToSet<Float> {
        public FloatToSet() {
            super(Float.class);
        }
    }

    public static class FloatToString extends AbstractNumberToStringConverter<Float> {
        public FloatToString() {
            super(Float.class);
        }

        protected String format(Float obj, NumberFormat nf) throws ConversionException {
            return nf.format(obj.floatValue());
        }
    }

    public static class IntegerToBigDecimal extends AbstractConverter<Integer, BigDecimal> {
        public IntegerToBigDecimal() {
            super(Integer.class, BigDecimal.class);
        }

        public BigDecimal convert(Integer obj) throws ConversionException {
            return BigDecimal.valueOf(obj.intValue());
        }
    }

    public static class IntegerToByte extends AbstractConverter<Integer, Byte> {
        public IntegerToByte() {
            super(Integer.class, Byte.class);
        }

        public Byte convert(Integer obj) throws ConversionException {
            return obj.byteValue();
        }
    }

    public static class IntegerToDouble extends GenericNumberToDouble<Integer> {
        public IntegerToDouble() {
            super(Integer.class);
        }
    }

    public static class IntegerToFloat extends GenericNumberToFloat<Integer> {
        public IntegerToFloat() {
            super(Integer.class);
        }
    }

    public static class IntegerToList extends GenericSingletonToList<Integer> {
        public IntegerToList() {
            super(Integer.class);
        }
    }

    public static class IntegerToLong extends GenericNumberToLong<Integer> {
        public IntegerToLong() {
            super(Integer.class);
        }
    }

    public static class IntegerToSet extends GenericSingletonToSet<Integer> {
        public IntegerToSet() {
            super(Integer.class);
        }
    }

    public static class IntegerToShort extends GenericNumberToShort<Integer> {
        public IntegerToShort() {
            super(Integer.class);
        }
    }

    public static class IntegerToString extends AbstractNumberToStringConverter<Integer> {
        public IntegerToString() {
            super(Integer.class);
        }

        protected String format(Integer obj, NumberFormat nf) throws ConversionException {
            return nf.format(obj.intValue());
        }
    }

    public static class LongToBigDecimal extends AbstractConverter<Long, BigDecimal> {
        public LongToBigDecimal() {
            super(Long.class, BigDecimal.class);
        }

        public BigDecimal convert(Long obj) throws ConversionException {
            return BigDecimal.valueOf(obj.longValue());
        }
    }

    public static class LongToByte extends AbstractConverter<Long, Byte> {
        public LongToByte() {
            super(Long.class, Byte.class);
        }

        public Byte convert(Long obj) throws ConversionException {
            return obj.byteValue();
        }
    }

    public static class LongToDouble extends GenericNumberToDouble<Long> {
        public LongToDouble() {
            super(Long.class);
        }
    }

    public static class LongToFloat extends GenericNumberToFloat<Long> {
        public LongToFloat() {
            super(Long.class);
        }
    }

    public static class LongToInteger extends GenericNumberToInteger<Long> {
        public LongToInteger() {
            super(Long.class);
        }
    }

    public static class LongToList extends GenericSingletonToList<Long> {
        public LongToList() {
            super(Long.class);
        }
    }

    public static class LongToSet extends GenericSingletonToSet<Long> {
        public LongToSet() {
            super(Long.class);
        }
    }

    public static class LongToShort extends GenericNumberToShort<Long> {
        public LongToShort() {
            super(Long.class);
        }
    }

    public static class LongToString extends AbstractNumberToStringConverter<Long> {
        public LongToString() {
            super(Long.class);
        }

        protected String format(Long obj, NumberFormat nf) throws ConversionException {
            return nf.format(obj.longValue());
        }
    }

    public static class ShortToDouble extends GenericNumberToDouble<Short> {
        public ShortToDouble() {
            super(Short.class);
        }
    }

    public static class ShortToFloat extends GenericNumberToFloat<Short> {
        public ShortToFloat() {
            super(Short.class);
        }
    }

    public static class ShortToInteger extends GenericNumberToInteger<Short> {
        public ShortToInteger() {
            super(Short.class);
        }
    }

    public static class ShortToList extends GenericSingletonToList<Short> {
        public ShortToList() {
            super(Short.class);
        }
    }

    public static class ShortToLong extends GenericNumberToLong<Short> {
        public ShortToLong() {
            super(Short.class);
        }
    }

    public static class ShortToSet extends GenericSingletonToSet<Short> {
        public ShortToSet() {
            super(Short.class);
        }
    }

    public static class ShortToString extends AbstractNumberToStringConverter<Short> {
        public ShortToString() {
            super(Short.class);
        }

        protected String format(Short obj, NumberFormat nf) throws ConversionException {
            return nf.format(obj.floatValue());
        }
    }

    public static class StringToBigDecimal extends AbstractStringToNumberConverter<BigDecimal> {
        public StringToBigDecimal() {
            super(BigDecimal.class);
        }

        public BigDecimal convert(String obj) throws ConversionException {
            return BigDecimal.valueOf(Double.valueOf(obj));
        }

        protected BigDecimal convert(Number number) throws ConversionException {
            return BigDecimal.valueOf(number.doubleValue());
        }
    }

    public static class StringToByte extends AbstractConverter<String, Byte> {
        public StringToByte() {
            super(String.class, Byte.class);
        }

        public Byte convert(String obj) throws ConversionException {
            return Byte.valueOf(obj);
        }
    }

    public static class StringToDouble extends AbstractStringToNumberConverter<Double> {
        public StringToDouble() {
            super(Double.class);
        }

        public Double convert(String obj) throws ConversionException {
            return Double.valueOf(obj);
        }

        protected Double convert(Number number) throws ConversionException {
            return number.doubleValue();
        }
    }

    public static class StringToFloat extends AbstractStringToNumberConverter<Float> {
        public StringToFloat() {
            super(Float.class);
        }

        public Float convert(String obj) throws ConversionException {
            return Float.valueOf(obj);
        }

        protected Float convert(Number number) throws ConversionException {
            return number.floatValue();
        }
    }

    public static class StringToInteger extends AbstractStringToNumberConverter<Integer> {
        public StringToInteger() {
            super(Integer.class);
        }

        public Integer convert(String obj) throws ConversionException {
            return Integer.valueOf(obj);
        }

        protected Integer convert(Number number) throws ConversionException {
            return number.intValue();
        }
    }

    public static class StringToLong extends AbstractStringToNumberConverter<Long> {
        public StringToLong() {
            super(Long.class);
        }

        public Long convert(String obj) throws ConversionException {
            return Long.valueOf(obj);
        }

        protected Long convert(Number number) throws ConversionException {
            return number.longValue();
        }
    }

    public static class StringToShort extends AbstractConverter<String, Short> {
        public StringToShort() {
            super(String.class, Short.class);
        }

        public Short convert(String obj) throws ConversionException {
            return Short.valueOf(obj);
        }
    }

    public void loadConverters() {
        Converters.loadContainedConverters(NumberConverters.class);
    }
}
