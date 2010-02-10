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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.json.JSON;
import org.ofbiz.base.json.JSONWriter;

import javolution.util.FastList;
import javolution.util.FastSet;

/** Collection Converter classes. */
public class CollectionConverters implements ConverterLoader {
    public static class ArrayToList<T> extends AbstractConverter<T[], List<T>> {
        public ArrayToList() {
            super(Object[].class, List.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return sourceClass.isArray() && ObjectType.instanceOf(targetClass, this.getTargetClass());
        }

        public List<T> convert(T[] obj) throws ConversionException {
            return Arrays.asList(obj);
        }
    }

    public static class ListToString<T> extends AbstractConverter<List<T>, String> {
        public ListToString() {
            super(List.class, String.class);
        }

        public String convert(List<T> obj) throws ConversionException {
            StringWriter sw = new StringWriter();
            try {
                new JSONWriter(sw).write(obj);
            } catch (IOException e) {
                throw new ConversionException(e);
            }
            return sw.toString();
        }
    }

    public static class MapToList<K, V> extends AbstractConverter<Map<K, V>, List<Map<K, V>>> {
        public MapToList() {
            super(Map.class, List.class);
        }

        public List<Map<K, V>> convert(Map<K, V> obj) throws ConversionException {
            List<Map<K, V>> tempList = FastList.newInstance();
            tempList.add(obj);
            return tempList;
        }
    }

    public static class MapToSet<K, V> extends AbstractConverter<Map<K, V>, Set<Map<K, V>>> {
        public MapToSet() {
            super(Map.class, Set.class);
        }

        public Set<Map<K, V>> convert(Map<K, V> obj) throws ConversionException {
            Set<Map<K, V>> tempSet = FastSet.newInstance();
            tempSet.add(obj);
            return tempSet;
        }
    }

    public static class MapToString<K, V> extends AbstractConverter<Map<K, V>, String> {
        public MapToString() {
            super(Map.class, String.class);
        }

        public String convert(Map<K, V> obj) throws ConversionException {
            StringWriter sw = new StringWriter();
            try {
                new JSONWriter(sw).write(obj);
            } catch (IOException e) {
                throw new ConversionException(e);
            }
            return sw.toString();
        }
    }

    public static class StringToList extends AbstractConverter<String, List<Object>> {
        public StringToList() {
            super(String.class, List.class);
        }

        public List<Object> convert(String obj) throws ConversionException {
            try {
                return new JSON(new StringReader(obj)).JSONArray();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class StringToMap extends AbstractConverter<String, Map<String, Object>> {
        public StringToMap() {
            super(String.class, Map.class);
        }

        public Map<String, Object> convert(String obj) throws ConversionException {
            try {
                return new JSON(new StringReader(obj)).JSONObject();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class StringToSet extends AbstractConverter<String, Set<Object>> {
        public StringToSet() {
            super(String.class, Set.class);
        }

        public Set<Object> convert(String obj) throws ConversionException {
            try {
                Set<Object> set = FastSet.newInstance();
                set.addAll(new JSON(new StringReader(obj)).JSONArray());
                return set;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new ConversionException(e);
            }
        }
    }

    public void loadConverters() {
        Converters.loadContainedConverters(CollectionConverters.class);
    }
}
