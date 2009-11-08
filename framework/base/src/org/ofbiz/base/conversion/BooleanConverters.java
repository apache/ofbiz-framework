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

/** Boolean Converter classes. */
public class BooleanConverters {

    public static class BooleanToInteger extends AbstractConverter<Boolean, Integer> {

        public Integer convert(Boolean obj) throws ConversionException {
             return obj.booleanValue() ? 1 : 0;
        }

        public Class<Boolean> getSourceClass() {
            return Boolean.class;
        }

        public Class<Integer> getTargetClass() {
            return Integer.class;
        }

    }

    public static class BooleanToString extends AbstractConverter<Boolean, String> {

        public String convert(Boolean obj) throws ConversionException {
            return obj.booleanValue() ? "true" : "false";
        }

        public Class<Boolean> getSourceClass() {
            return Boolean.class;
        }

        public Class<String> getTargetClass() {
            return String.class;
        }

    }

    public static class IntegerToBoolean extends AbstractConverter<Integer, Boolean> {

        public Boolean convert(Integer obj) throws ConversionException {
             return obj.intValue() == 0 ? false : true;
        }

        public Class<Integer> getSourceClass() {
            return Integer.class;
        }

        public Class<Boolean> getTargetClass() {
            return Boolean.class;
        }

    }

    public static class StringToBoolean extends AbstractConverter<String, Boolean> {

        public Boolean convert(String obj) throws ConversionException {
            return "TRUE".equals(obj.toUpperCase());
        }

        public Class<String> getSourceClass() {
            return String.class;
        }

        public Class<Boolean> getTargetClass() {
            return Boolean.class;
        }
        
    }
}
