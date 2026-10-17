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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.conversion.ModelConverters.HashMapToDomainModel;
import org.apache.ofbiz.base.model.DomainModel;
import org.apache.ofbiz.base.util.GeneralException;
import org.junit.jupiter.api.Test;

class TestDomainModelConversion {


    public static class TestPerson extends DomainModel {
        private String name;
        private Integer age;
        private List<TestAddress> addresses;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public List<TestAddress> getAddresses() {
            return addresses;
        }

        public void setAddresses(List<TestAddress> addresses) {
            this.addresses = addresses;
        }
    }

    public static class TestAddress extends DomainModel {
        private String city;
        private String zip;

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getZip() {
            return zip;
        }

        public void setZip(String zip) {
            this.zip = zip;
        }
    }

    @Test
    void convertsFlatMapToDomainModel() throws Exception {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("name", "some name");
        map.put("age", 33);

        HashMapToDomainModel converter = new HashMapToDomainModel();
        DomainModel result = converter.convert(map, TestPerson.class.getName());

        assertTrue(result instanceof TestPerson);
        TestPerson person = (TestPerson) result;
        assertEquals("some name", person.getName());
        assertEquals(33, person.getAge());
    }

    @Test
    void convertsNestedListOfDomainModels() throws Exception {
        LinkedHashMap<String, Object> address = new LinkedHashMap<>();
        address.put("city", "Berlin");
        address.put("zip", "10115");

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("name", "some name");
        map.put("addresses", List.of(address));

        HashMapToDomainModel converter = new HashMapToDomainModel();
        TestPerson person = (TestPerson) converter.convert(map, TestPerson.class.getName());

        assertNotNull(person.getAddresses());
        assertEquals(1, person.getAddresses().size());
        assertEquals("Berlin", person.getAddresses().get(0).getCity());
    }

    @Test
    void throwsConversionExceptionForUnknownTargetClass() {
        HashMapToDomainModel converter = new HashMapToDomainModel();
        assertThrows(ConversionException.class, () -> converter.convert(new LinkedHashMap<>(), "non.existing.Class"));
    }

    @Test
    void throwsConversionExceptionForMissingTargetClass() {
        HashMapToDomainModel converter = new HashMapToDomainModel();
        assertThrows(ConversionException.class, () -> converter.convert(new LinkedHashMap<>()));
    }

    @Test
    void throwsConversionExceptionOnUnknownFields() throws Exception {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("name", "some name");
        map.put("age", 42);
        map.put("unknownField", "unknownValue");

        HashMapToDomainModel converter = new HashMapToDomainModel();
        assertThrows(ConversionException.class, () -> converter.convert(map, TestPerson.class.getName()));
    }

    @Test
    void nullValuesInMapMapToNullFields() throws Exception {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("name", null);
        map.put("age", 30);

        HashMapToDomainModel converter = new HashMapToDomainModel();
        TestPerson person = (TestPerson) converter.convert(map, TestPerson.class.getName());

        assertNull(person.getName());
        assertEquals(30, person.getAge());
    }

    // tests for convertRawKustToDomainModel

    private LinkedHashMap<String, Object> personMap(String name, int age) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("age", age);
        return map;
    }

    @Test
    void convertsListOfMapsToListOfDomainModels() throws Exception {
        List<Object> raw = List.of(personMap("Some Name", 42), personMap("Other Name", 30));

        List<DomainModel> result = ModelConverters.convertRawListToDomainModelType(raw, TestPerson.class);

        assertEquals(2, result.size());
        assertEquals("Some Name", ((TestPerson) result.get(0)).getName());
        assertEquals("Other Name", ((TestPerson) result.get(1)).getName());
    }

    @Test
    void returnsEmptyListForNullList() throws Exception {
        List<DomainModel> result = ModelConverters.convertRawListToDomainModelType((List<Object>) null, TestPerson.class);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void returnsEmptyListForEmptyList() throws Exception {
        List<DomainModel> result = ModelConverters.convertRawListToDomainModelType(List.of(), TestPerson.class);
        assertTrue(result.isEmpty());
    }

    @Test
    void throwsGeneralExceptionWhenElementIsNotConvertible() {
        List<Object> raw = List.of("not a map");

        assertThrows(GeneralException.class, () -> ModelConverters.convertRawListToDomainModelType(raw, TestPerson.class));
    }

    @Test
    void contextOverloadExtractsNamedListAndConverts() throws Exception {
        Map<String, Object> context = Map.of("people", List.of(personMap("Some Name", 42)));

        List<DomainModel> result = ModelConverters.convertRawListToDomainModelType(context, "people", TestPerson.class);

        assertEquals(1, result.size());
        assertEquals("Some Name", ((TestPerson) result.get(0)).getName());
    }

    @Test
    void contextOverloadReturnsEmptyListWhenKeyMissing() throws Exception {
        Map<String, Object> context = Map.of("otherKey", "irrelevant");

        List<DomainModel> result = ModelConverters.convertRawListToDomainModelType(context, "people", TestPerson.class);

        assertTrue(result.isEmpty());
    }

    @Test
    void contextOverloadReturnsEmptyListWhenValueForKeyIsNull() throws Exception {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("people", null);

        List<DomainModel> result = ModelConverters.convertRawListToDomainModelType(context, "people", TestPerson.class);

        assertTrue(result.isEmpty());
    }

    @Test
    void contextOverloadThrowsIfNamedValueIsNotAList() {
        Map<String, Object> context = Map.of("people", "not a list at all");

        assertThrows(ClassCastException.class, () -> ModelConverters.convertRawListToDomainModelType(context, "people", TestPerson.class));
    }
}
