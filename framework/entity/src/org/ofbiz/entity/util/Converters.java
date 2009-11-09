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
package org.ofbiz.entity.util;

import java.util.List;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastSet;

import org.ofbiz.base.conversion.AbstractConverter;
import org.ofbiz.base.conversion.ConversionException;
import org.ofbiz.entity.GenericValue;

/** Entity Engine <code>Converter</code> classes. */
public class Converters {

    public static class GenericValueToList extends AbstractConverter<GenericValue, List<GenericValue>> {

        public List<GenericValue> convert(GenericValue obj) throws ConversionException {
            List<GenericValue> tempList = FastList.newInstance();
            tempList.add(obj);
            return tempList;
        }

        public Class<GenericValue> getSourceClass() {
            return GenericValue.class;
        }

        @SuppressWarnings("unchecked")
        public Class<List> getTargetClass() {
            return List.class;
        }

    } 

    public static class GenericValueToSet extends AbstractConverter<GenericValue, Set<GenericValue>> {

        public Set<GenericValue> convert(GenericValue obj) throws ConversionException {
            Set<GenericValue> tempSet = FastSet.newInstance();
            tempSet.add(obj);
            return tempSet;
        }

        public Class<GenericValue> getSourceClass() {
            return GenericValue.class;
        }

        @SuppressWarnings("unchecked")
        public Class<Set> getTargetClass() {
            return Set.class;
        }

    }

    public static class GenericValueToString extends AbstractConverter<GenericValue, String> {

        public String convert(GenericValue obj) throws ConversionException {
            return obj.toString();
        }

        public Class<GenericValue> getSourceClass() {
            return GenericValue.class;
        }

        public Class<String> getTargetClass() {
            return String.class;
        }

    }

}
