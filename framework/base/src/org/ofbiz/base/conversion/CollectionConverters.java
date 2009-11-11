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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.StringUtil;

import javolution.util.FastList;
import javolution.util.FastSet;

/** Collection Converter classes. */
public class CollectionConverters implements ConverterLoader {

    public static class ArrayToList extends AbstractConverter<Object[], List<?>> {

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return sourceClass.isArray() && ObjectType.instanceOf(targetClass, this.getTargetClass());
        }

        public List<?> convert(Object[] obj) throws ConversionException {
            return Arrays.asList(obj);
        }

        public Class<Object[]> getSourceClass() {
            return Object[].class;
        }

        @SuppressWarnings("unchecked")
        public Class<List> getTargetClass() {
            return List.class;
        }

    }

    public static class ListToString extends AbstractConverter<List<?>, String> {

        public String convert(List<?> obj) throws ConversionException {
            return obj.toString();
        }

        @SuppressWarnings("unchecked")
        public Class<List> getSourceClass() {
            return List.class;
        }

        public Class<String> getTargetClass() {
            return String.class;
        }

    }

    public static class MapToList extends AbstractConverter<Map<?, ?>, List<Map<?,?>>> {

        public List<Map<?,?>> convert(Map<?, ?> obj) throws ConversionException {
            List<Map<?,?>> tempList = FastList.newInstance();
            tempList.add(obj);
            return tempList;
        }

        @SuppressWarnings("unchecked")
        public Class<Map> getSourceClass() {
            return Map.class;
        }

        @SuppressWarnings("unchecked")
        public Class<List> getTargetClass() {
            return List.class;
        }

    }

    public static class MapToSet extends AbstractConverter<Map<?, ?>, Set<Map<?,?>>> {

        public Set<Map<?,?>> convert(Map<?, ?> obj) throws ConversionException {
            Set<Map<?,?>> tempSet = FastSet.newInstance();
            tempSet.add(obj);
            return tempSet;
        }

        @SuppressWarnings("unchecked")
        public Class<Map> getSourceClass() {
            return Map.class;
        }

        @SuppressWarnings("unchecked")
        public Class<Set> getTargetClass() {
            return Set.class;
        }

    }

    public static class MapToString extends AbstractConverter<Map<?, ?>, String> {

        public String convert(Map<?, ?> obj) throws ConversionException {
            return obj.toString();
        }

        @SuppressWarnings("unchecked")
        public Class<Map> getSourceClass() {
            return Map.class;
        }

        public Class<String> getTargetClass() {
            return String.class;
        }

    }

    public static class StringToList extends AbstractConverter<String, List<?>> {

        public List<?> convert(String obj) throws ConversionException {
            if (obj.startsWith("[") && obj.endsWith("]")) {
                return StringUtil.toList(obj);
            } else {
                List<String> tempList = FastList.newInstance();
                tempList.add(obj);
                return tempList;
            }
        }

        public Class<String> getSourceClass() {
            return String.class;
        }

        @SuppressWarnings("unchecked")
        public Class<List> getTargetClass() {
            return List.class;
        }

    }

    public static class StringToMap extends AbstractConverter<String, Map<?, ?>> {

        public Map<?, ?> convert(String obj) throws ConversionException {
            if (obj.startsWith("{") && obj.endsWith("}")) {
                return StringUtil.toMap(obj);
            }
            throw new ConversionException("Could not convert " + obj + " to Map: ");
        }

        public Class<String> getSourceClass() {
            return String.class;
        }

        @SuppressWarnings("unchecked")
        public Class<Map> getTargetClass() {
            return Map.class;
        }

    }

    public static class StringToSet extends AbstractConverter<String, Set<?>> {

        public Set<?> convert(String obj) throws ConversionException {
            if (obj.startsWith("[") && obj.endsWith("]")) {
                return StringUtil.toSet(obj);
            } else {
                Set<String> tempSet = FastSet.newInstance();
                tempSet.add(obj);
                return tempSet;
            }
        }

        public Class<String> getSourceClass() {
            return String.class;
        }

        @SuppressWarnings("unchecked")
        public Class<Set> getTargetClass() {
            return Set.class;
        }

    }

    public void loadConverters() {
        Converters.loadContainedConverters(CollectionConverters.class);
    }

}
