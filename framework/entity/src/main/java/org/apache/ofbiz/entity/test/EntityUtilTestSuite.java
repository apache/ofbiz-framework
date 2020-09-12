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
package org.apache.ofbiz.entity.test;

import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityExpr;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.testtools.EntityTestCase;
import org.apache.ofbiz.entity.util.EntityUtil;

import java.util.ArrayList;
import java.util.List;

public class EntityUtilTestSuite extends EntityTestCase {

    private static final String MODULE = EntityUtilTestSuite.class.getName();

    public static final long TEST_COUNT = 1000;

    public EntityUtilTestSuite(String name) {
        super(name);
    }

    private List<GenericValue> prepareGenericValueList() {
        List<GenericValue> newValues = new ArrayList<>();
        for (int i = 0; i < TEST_COUNT; i++) {
            newValues.add(getDelegator().makeValue("Testing", "testingId", StringUtil.padNumberString(String.valueOf(i), 5),
                    "description", "Description " + i % 10));
        }
        return newValues;
    }

    /**
     * Test get field list from entity list.
     */
    public void testGetFieldListFromEntityList() {
        List<GenericValue> newValues = prepareGenericValueList();
        List<String> descriptionList = EntityUtil.getFieldListFromEntityList(newValues, "description", false);
        assertEquals("Get not distinct field list from " + TEST_COUNT + " entity", TEST_COUNT, descriptionList.size());
        assertEquals("Get first description value", "Description 0", descriptionList.get(0));
        assertEquals("Get tens description value", "Description 0", descriptionList.get(10));

        descriptionList = EntityUtil.getFieldListFromEntityList(newValues, "description", true);
        assertEquals("Get distinct field list from " + TEST_COUNT + " entity, modulo 10 values", 10, descriptionList.size());
        assertEquals("Get first description value", "Description 0", descriptionList.get(0));
    }

    /**
     * Test filter by condition.
     */
    public void testFilterByCondition() {
        List<GenericValue> newValues = prepareGenericValueList();
        EntityExpr condition = EntityCondition.makeCondition("description", "Description 0");
        List<GenericValue> filteredValues = EntityUtil.filterByCondition(newValues, condition);
        assertEquals("Filter on 10% description condition " + TEST_COUNT + " entity", TEST_COUNT / 10, filteredValues.size());
        assertEquals("Get first description value", "Description 0", filteredValues.get(0).get("description"));

        filteredValues = EntityUtil.filterOutByCondition(newValues, condition);
        assertEquals("Filter out on 10% description condition " + TEST_COUNT + " entity", TEST_COUNT - TEST_COUNT / 10, filteredValues.size());
        assertEquals("Get first description value", "Description 1", filteredValues.get(0).get("description"));
    }

    /**
     * Test filter by and.
     */
    public void testFilterByAnd() {
        List<GenericValue> newValues = prepareGenericValueList();
        List<EntityExpr> condition = UtilMisc.toList(EntityCondition.makeCondition("description", "Description 0"),
                EntityCondition.makeCondition("testingId", "00010"));
        List<GenericValue> filteredWithMap = EntityUtil.filterByAnd(newValues, UtilMisc.toMap("description", "Description 0", "testingId", "00010"));
        List<GenericValue> filteredWithCondition = EntityUtil.filterByAnd(newValues, condition);
        assertEquals("Filter with same condition using Map and List<EntityExpr> ", filteredWithCondition, filteredWithMap);

        condition = UtilMisc.toList(EntityCondition.makeCondition("description", "Description 0"),
                EntityCondition.makeCondition("testingId", EntityOperator.LIKE, "000%"));
        filteredWithCondition = EntityUtil.filterByAnd(newValues, condition);
        assertEquals("Filter condition using List<EntityExpr> must have 10 results", 10, filteredWithCondition.size());
        filteredWithCondition.forEach(genericValue ->
                assertEquals("Filter condition using List<EntityExpr> must get simple description",
                        "Description 0", genericValue.get("description")));
    }

    /**
     * Test filter by or.
     */
    public void testFilterByOr() {
        List<GenericValue> newValues = prepareGenericValueList();
        List<EntityExpr> condition = UtilMisc.toList(EntityCondition.makeCondition("description", "Description 0"),
                EntityCondition.makeCondition("testingId", "00001"));
        List<GenericValue> filteredWithCondition = EntityUtil.filterByOr(newValues, condition);

        assertEquals("Filter condition using List<EntityExpr> must have " + (TEST_COUNT / 10 + 1) + " results",
                TEST_COUNT / 10 + 1, filteredWithCondition.size());
    }
}
