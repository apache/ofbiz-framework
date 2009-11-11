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
import org.ofbiz.base.util.UtilGenerics;

import javolution.util.FastList;
import javolution.util.FastSet;

/** Collection Converter classes. */
public class CollectionConverters implements ConverterLoader {

    public static class ArrayToList extends AbstractConverter<Object[], List> {

        public ArrayToList() {
            super(Object[].class, List.class);
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return sourceClass.isArray() && ObjectType.instanceOf(targetClass, this.getTargetClass());
        }

        public List<?> convert(Object[] obj) throws ConversionException {
            return Arrays.asList(obj);
        }

    }

    public static class ListToString extends AbstractConverter<List, String> {

        public ListToString() {
            super(List.class, String.class);
        }

        public String convert(List obj) throws ConversionException {
            return obj.toString();
        }

    }

    public static class MapToList extends AbstractCollectionConverter<Map, List<Map>> {

        public MapToList() {
            super(Map.class, List.class);
        }

        public List<Map> convert(Map obj) throws ConversionException {
            List<Map> tempList = FastList.newInstance();
            tempList.add(obj);
            return tempList;
        }

    }

    public static class MapToSet extends AbstractCollectionConverter<Map, Set<Map>> {

        public MapToSet() {
            super(Map.class, Set.class);
        }

        public Set<Map> convert(Map obj) throws ConversionException {
            Set<Map> tempSet = FastSet.newInstance();
            tempSet.add(obj);
            return tempSet;
        }

    }

    public static class MapToString extends AbstractConverter<Map, String> {

        public MapToString() {
            super(Map.class, String.class);
        }

        public String convert(Map obj) throws ConversionException {
            return obj.toString();
        }

    }

    public static class StringToList extends AbstractCollectionConverter<String, List<String>> {

        public StringToList() {
            super(String.class, List.class);
        }

        public List<String> convert(String obj) throws ConversionException {
            if (obj.startsWith("[") && obj.endsWith("]")) {
                return StringUtil.toList(obj);
            } else {
                List<String> tempList = FastList.newInstance();
                tempList.add(obj);
                return tempList;
            }
        }

    }

    public static class StringToMap extends AbstractConverter<String, Map> {

        public StringToMap() {
            super(String.class, Map.class);
        }

        public Map convert(String obj) throws ConversionException {
            if (obj.startsWith("{") && obj.endsWith("}")) {
                return StringUtil.toMap(obj);
            }
            throw new ConversionException("Could not convert " + obj + " to Map: ");
        }

    }

    public static class StringToSet extends AbstractCollectionConverter<String, Set<String>> {

        public StringToSet() {
            super(String.class, Set.class);
        }

        public Set<String> convert(String obj) throws ConversionException {
            if (obj.startsWith("[") && obj.endsWith("]")) {
                return StringUtil.toSet(obj);
            } else {
                Set<String> tempSet = FastSet.newInstance();
                tempSet.add(obj);
                return tempSet;
            }
        }

    }

    public void loadConverters() {
        Converters.loadContainedConverters(CollectionConverters.class);
    }

}
