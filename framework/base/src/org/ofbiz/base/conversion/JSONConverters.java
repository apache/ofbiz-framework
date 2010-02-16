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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastSet;

import org.ofbiz.base.json.JSON;
import org.ofbiz.base.util.UtilGenerics;

/** JSON Converter classes. */
public class JSONConverters implements ConverterLoader {
    public static class ObjectToJSONResultCreator<R extends JSONResult> implements ConverterCreator, ConverterLoader {
        public void loadConverters() {
            Converters.registerCreator(this);
        }

        public <S, T> Converter<S, T> createConverter(Class<S> sourceClass, Class<T> targetClass) {
            if (!JSONResult.class.isAssignableFrom(targetClass)) {
                return null;
            }
            if (Collection.class.isAssignableFrom(sourceClass)) {
            } else if (Map.class.isAssignableFrom(sourceClass)) {
            } else if (Byte.class == sourceClass) {
            } else if (Character.class == sourceClass) {
            } else if (Double.class == sourceClass) {
            } else if (Float.class == sourceClass) {
            } else if (Integer.class == sourceClass) {
            } else if (Long.class == sourceClass) {
            } else if (Short.class == sourceClass) {
            } else {
                return null;
            }
            return UtilGenerics.cast(new ObjectToJSONWriterResult<S, JSONResult>(sourceClass, UtilGenerics.<Class<JSONResult>>cast(targetClass)));
        }
    }

    private static class ObjectToJSONWriterResult<S, T extends JSONResult> extends AbstractConverter<S, T> {
        public ObjectToJSONWriterResult(Class<S> sourceClass, Class<T> targetClass) {
            super(sourceClass, targetClass);
        }

        public T convert(S obj) throws ConversionException {
            try {
                T result = UtilGenerics.<T>cast(getTargetClass().newInstance());
                result.getWriter().write(obj);
                return result;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class JSONToByte extends AbstractConverter<JSON, Byte> {
        public JSONToByte() {
            super(JSON.class, Byte.class);
        }

        public Byte convert(JSON obj) throws ConversionException {
            try {
                return obj.JSONLong().byteValue();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class JSONToDouble extends AbstractConverter<JSON, Double> {
        public JSONToDouble() {
            super(JSON.class, Double.class);
        }

        public Double convert(JSON obj) throws ConversionException {
            try {
                return obj.JSONFloat();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class JSONToFloat extends AbstractConverter<JSON, Float> {
        public JSONToFloat() {
            super(JSON.class, Float.class);
        }

        public Float convert(JSON obj) throws ConversionException {
            try {
                return obj.JSONFloat().floatValue();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class JSONToInteger extends AbstractConverter<JSON, Integer> {
        public JSONToInteger() {
            super(JSON.class, Integer.class);
        }

        public Integer convert(JSON obj) throws ConversionException {
            try {
                return obj.JSONLong().intValue();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class JSONToList extends AbstractConverter<JSON, List<Object>> {
        public JSONToList() {
            super(JSON.class, List.class);
        }

        public List<Object> convert(JSON obj) throws ConversionException {
            try {
                return obj.JSONArray();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class JSONToLong extends AbstractConverter<JSON, Long> {
        public JSONToLong() {
            super(JSON.class, Long.class);
        }

        public Long convert(JSON obj) throws ConversionException {
            try {
                return obj.JSONLong();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class JSONToMap extends AbstractConverter<JSON, Map<String, Object>> {
        public JSONToMap() {
            super(JSON.class, Map.class);
        }

        public Map<String, Object> convert(JSON obj) throws ConversionException {
            try {
                return obj.JSONObject();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class JSONToShort extends AbstractConverter<JSON, Short> {
        public JSONToShort() {
            super(JSON.class, Short.class);
        }

        public Short convert(JSON obj) throws ConversionException {
            try {
                return obj.JSONLong().shortValue();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class JSONToSet extends AbstractConverter<JSON, Set<Object>> {
        public JSONToSet() {
            super(JSON.class, Set.class);
        }

        public Set<Object> convert(JSON obj) throws ConversionException {
            try {
                Set<Object> set = FastSet.newInstance();
                set.addAll(obj.JSONArray());
                return set;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new ConversionException(e);
            }
        }
    }

    public void loadConverters() {
        Converters.loadContainedConverters(JSONConverters.class);
    }
}
