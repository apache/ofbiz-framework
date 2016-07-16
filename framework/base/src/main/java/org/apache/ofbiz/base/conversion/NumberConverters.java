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
package org.apache.ofbiz.base.conversion;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.ofbiz.base.util.StringUtil;

/** Number Converter classes. */
public class NumberConverters implements ConverterLoader {

    protected static Number fromString(String str, Locale locale) throws ConversionException {
        NumberFormat nf = NumberFormat.getNumberInstance(locale);
        if (nf instanceof DecimalFormat) {
            ((DecimalFormat) nf).setParseBigDecimal(true);
        }
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

    public static class BigDecimalToString extends AbstractNumberToStringConverter<BigDecimal> {
        public BigDecimalToString() {
            super(BigDecimal.class);
        }

        @Override
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

    public static class BigIntegerToString extends AbstractNumberToStringConverter<BigInteger> {
        public BigIntegerToString() {
            super(BigInteger.class);
        }

        @Override
        protected String format(BigInteger obj, NumberFormat nf) throws ConversionException {
            return nf.format(obj.doubleValue());
        }
    }

    public static class ByteToString extends AbstractNumberToStringConverter<Byte> {
        public ByteToString() {
            super(Byte.class);
        }

        @Override
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

        @Override
        protected BigInteger convert(Number number) throws ConversionException {
            return BigInteger.valueOf(number.longValue());
        }
    }

    public static class DoubleToString extends AbstractNumberToStringConverter<Double> {
        public DoubleToString() {
            super(Double.class);
        }

        @Override
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

    public static class FloatToString extends AbstractNumberToStringConverter<Float> {
        public FloatToString() {
            super(Float.class);
        }

        @Override
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

    public static class IntegerToString extends AbstractNumberToStringConverter<Integer> {
        public IntegerToString() {
            super(Integer.class);
        }

        @Override
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

    public static class LongToString extends AbstractNumberToStringConverter<Long> {
        public LongToString() {
            super(Long.class);
        }

        @Override
        protected String format(Long obj, NumberFormat nf) throws ConversionException {
            return nf.format(obj.longValue());
        }
    }

    public static class ShortToString extends AbstractNumberToStringConverter<Short> {
        public ShortToString() {
            super(Short.class);
        }

        @Override
        protected String format(Short obj, NumberFormat nf) throws ConversionException {
            return nf.format(obj.floatValue());
        }
    }

    public static class StringToBigDecimal extends AbstractStringToNumberConverter<BigDecimal> {
        public StringToBigDecimal() {
            super(BigDecimal.class);
        }

        public BigDecimal convert(String obj) throws ConversionException {
            return new BigDecimal(obj);
        }

        @Override
        protected BigDecimal convert(Number number) throws ConversionException {
            if (number instanceof BigDecimal) {
                return (BigDecimal) number;
            }
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

        @Override
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

        @Override
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

        @Override
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

        @Override
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

        Converters.registerConverter(new GenericNumberToDouble<BigDecimal>(BigDecimal.class));
        Converters.registerConverter(new GenericNumberToDouble<BigInteger>(BigInteger.class));
        Converters.registerConverter(new GenericNumberToDouble<Byte>(Byte.class));
        Converters.registerConverter(new GenericNumberToDouble<Float>(Float.class));
        Converters.registerConverter(new GenericNumberToDouble<Integer>(Integer.class));
        Converters.registerConverter(new GenericNumberToDouble<Long>(Long.class));
        Converters.registerConverter(new GenericNumberToDouble<Short>(Short.class));

        Converters.registerConverter(new GenericNumberToFloat<BigDecimal>(BigDecimal.class));
        Converters.registerConverter(new GenericNumberToFloat<BigInteger>(BigInteger.class));
        Converters.registerConverter(new GenericNumberToFloat<Byte>(Byte.class));
        Converters.registerConverter(new GenericNumberToFloat<Double>(Double.class));
        Converters.registerConverter(new GenericNumberToFloat<Integer>(Integer.class));
        Converters.registerConverter(new GenericNumberToFloat<Long>(Long.class));
        Converters.registerConverter(new GenericNumberToFloat<Short>(Short.class));

        Converters.registerConverter(new GenericNumberToInteger<BigDecimal>(BigDecimal.class));
        Converters.registerConverter(new GenericNumberToInteger<BigInteger>(BigInteger.class));
        Converters.registerConverter(new GenericNumberToInteger<Byte>(Byte.class));
        Converters.registerConverter(new GenericNumberToInteger<Double>(Double.class));
        Converters.registerConverter(new GenericNumberToInteger<Float>(Float.class));
        Converters.registerConverter(new GenericNumberToInteger<Long>(Long.class));
        Converters.registerConverter(new GenericNumberToInteger<Short>(Short.class));

        Converters.registerConverter(new GenericSingletonToList<BigDecimal>(BigDecimal.class));
        Converters.registerConverter(new GenericSingletonToList<BigInteger>(BigInteger.class));
        Converters.registerConverter(new GenericSingletonToList<Byte>(Byte.class));
        Converters.registerConverter(new GenericSingletonToList<Double>(Double.class));
        Converters.registerConverter(new GenericSingletonToList<Float>(Float.class));
        Converters.registerConverter(new GenericSingletonToList<Integer>(Integer.class));
        Converters.registerConverter(new GenericSingletonToList<Long>(Long.class));
        Converters.registerConverter(new GenericSingletonToList<Short>(Short.class));

        Converters.registerConverter(new GenericNumberToLong<BigDecimal>(BigDecimal.class));
        Converters.registerConverter(new GenericNumberToLong<BigInteger>(BigInteger.class));
        Converters.registerConverter(new GenericNumberToLong<Byte>(Byte.class));
        Converters.registerConverter(new GenericNumberToLong<Double>(Double.class));
        Converters.registerConverter(new GenericNumberToLong<Float>(Float.class));
        Converters.registerConverter(new GenericNumberToLong<Integer>(Integer.class));
        Converters.registerConverter(new GenericNumberToLong<Short>(Short.class));

        Converters.registerConverter(new GenericSingletonToSet<BigDecimal>(BigDecimal.class));
        Converters.registerConverter(new GenericSingletonToSet<BigInteger>(BigInteger.class));
        Converters.registerConverter(new GenericSingletonToSet<Byte>(Byte.class));
        Converters.registerConverter(new GenericSingletonToSet<Double>(Double.class));
        Converters.registerConverter(new GenericSingletonToSet<Float>(Float.class));
        Converters.registerConverter(new GenericSingletonToSet<Integer>(Integer.class));
        Converters.registerConverter(new GenericSingletonToSet<Long>(Long.class));
        Converters.registerConverter(new GenericSingletonToSet<Short>(Short.class));

        Converters.registerConverter(new GenericNumberToShort<Integer>(Integer.class));
        Converters.registerConverter(new GenericNumberToShort<Long>(Long.class));
    }
}
