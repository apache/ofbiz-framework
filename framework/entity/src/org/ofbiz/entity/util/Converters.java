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
import org.ofbiz.base.conversion.ConverterLoader;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericValue;

/** Entity Engine <code>Converter</code> classes. */
public class Converters implements ConverterLoader {
    public static class GenericValueToList extends AbstractConverter<GenericValue, List<GenericValue>> {
        public GenericValueToList() {
            super(GenericValue.class, List.class);
        }

        public List<GenericValue> convert(GenericValue obj) throws ConversionException {
            List<GenericValue> tempList = FastList.newInstance();
            tempList.add(obj);
            return tempList;
        }
    }

    public static class GenericValueToSet extends AbstractConverter<GenericValue, Set<GenericValue>> {
        public GenericValueToSet() {
            super(GenericValue.class, Set.class);
        }

        public Set<GenericValue> convert(GenericValue obj) throws ConversionException {
            Set<GenericValue> tempSet = FastSet.newInstance();
            tempSet.add(obj);
            return tempSet;
        }
    }

    public static class GenericValueToString extends AbstractConverter<GenericValue, String> {
        public GenericValueToString() {
            super(GenericValue.class, String.class);
        }

        public String convert(GenericValue obj) throws ConversionException {
            return obj.toString();
        }
    }

    public static class NullFieldToObject extends AbstractConverter<GenericEntity.NullField, Object> {
        public NullFieldToObject() {
            super(GenericEntity.NullField.class, Object.class);
        }

        public Object convert(GenericEntity.NullField obj) throws ConversionException {
            return null;
        }
    }

    public static class ObjectToNullField extends AbstractConverter<Object, GenericEntity.NullField> {
        public ObjectToNullField() {
            super(Object.class, GenericEntity.NullField.class);
        }

        public GenericEntity.NullField convert(Object obj) throws ConversionException {
            return GenericEntity.NULL_FIELD;
        }
    }

    public void loadConverters() {
        org.ofbiz.base.conversion.Converters.loadContainedConverters(Converters.class);
    }
}
