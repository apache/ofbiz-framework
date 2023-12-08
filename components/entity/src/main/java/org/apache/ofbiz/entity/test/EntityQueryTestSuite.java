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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.testtools.EntityTestCase;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityFindOptions;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;

public class EntityQueryTestSuite extends EntityTestCase {

    public EntityQueryTestSuite(String name) {
        super(name);
    }

    /**
     * queryCount(): This method returns number of records found for the particular query.
     * assert: Compared count of number of records found by Entity Engine method with count of number of records found by EntityQuery method.
     */
    public void testQueryCount() throws GenericEntityException {
        Delegator delegator = getDelegator();
        List<GenericValue> testingTypes = new LinkedList<>();
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "record-1", "description", "Record One"));
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "record-2", "description", "Record Two"));
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "record-3", "description", "Record Three"));
        delegator.storeAll(testingTypes);

        List<GenericValue> totalRecordsByEntityEngine = delegator.findList("TestingType", null, null, null, null, false);
        int numberOfRecordsByEntityQuery = (int) EntityQuery.use(delegator).from("TestingType").queryCount();

        assertEquals("queryCount(): Total Number of Records matched", totalRecordsByEntityEngine.size(), numberOfRecordsByEntityQuery);
    }

    /**
     * where(): This method is used for setting condition of which records to fetch from entity.
     * assert 1: Compared size of the list returned by Entity Engine method and by EntityQuery method.
     * assert 2: Compared 'testingTypeId' field of first record fetched by Entity Engine method and by EntityQuery method.
     * assert 3: Compared 'description' field of first record fetched by Entity Engine method and by EntityQuery method.
     */
    public void testWhere() throws GenericEntityException {
        Delegator delegator = getDelegator();
        List<GenericValue> testingTypes = new LinkedList<>();
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "where-1", "description", "find me"));
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "where-2", "description", "find me not"));
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "where-3", "description", "find me"));
        delegator.storeAll(testingTypes);

        List<GenericValue> listByEntityEngine = delegator.findList("TestingType", EntityCondition.makeCondition("description",
                EntityOperator.EQUALS, "find me"), null, UtilMisc.toList("description"), null, false);
        List<GenericValue> listByEntityQuery = EntityQuery.use(delegator).from("TestingType").where("description", "find me")
                .orderBy("description").queryList();

        assertEquals("where(): Number of records fetched by Entity Engine and by EntityQuery matched", listByEntityEngine.size(),
                listByEntityQuery.size());
        assertEquals("where(): Record matched = testingTypeId", listByEntityEngine.get(0).getString("testingTypeId"),
                listByEntityQuery.get(0).getString("testingTypeId"));
        assertEquals("where(): Record matched = description", listByEntityEngine.get(0).getString("description"),
                listByEntityQuery.get(0).getString("description"));
    }

    /**
     * queryList(): Returns all records from the given entity.
     * assert 1: Compared size of the list returned by Entity Engine method and by EntityQuery method.
     * assert 2: Compared 'testingTypeId' field of first record fetched by Entity Engine method and by EntityQuery method.
     * assert 3: Compared 'description' field of first record fetched by Entity Engine method and by EntityQuery method.
     */
    public void testQueryList() throws GenericEntityException {
        Delegator delegator = getDelegator();
        List<GenericValue> testingTypes = new LinkedList<>();
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "queryList-1", "description", "queryList record one"));
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "queryList-2", "description", "queryList record two"));
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "queryList-3", "description", "queryList record three"));
        delegator.storeAll(testingTypes);

        List<GenericValue> listByEntityEngine = delegator.findList("TestingType", null, null, UtilMisc.toList("description"), null, false);
        List<GenericValue> listByEntityQuery = EntityQuery.use(delegator).from("TestingType").orderBy("description").queryList();

        assertEquals("queryList(): Number of records fetched by Entity Engine and by EntityQuery matched", listByEntityEngine.size(),
                listByEntityQuery.size());
        assertEquals("queryList(): Record matched = testingTypeId", listByEntityEngine.get(0).getString("testingTypeId"),
                listByEntityQuery.get(0).getString("testingTypeId"));
        assertEquals("queryList(): Record matched = description", listByEntityEngine.get(0).getString("description"),
                listByEntityQuery.get(0).getString("description"));
    }

    /**
     * queryFirst(): Returns first record from result of query.
     * assert 1: Compared 'testingTypeId' field of record fetched by Entity Engine method and by EntityQuery method.
     * assert 2: Compared 'description' field of first record fetched by Entity Engine method and by EntityQuery method.
     */
    public void testQueryFirst() throws GenericEntityException {
        Delegator delegator = getDelegator();
        List<GenericValue> testingTypes = new LinkedList<>();
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "queryFirst-1", "description", "first record"));
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "queryFirst-2", "description", "second record"));
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "queryFirst-3", "description", "third record"));
        delegator.storeAll(testingTypes);

        GenericValue firstRecordByEntityEngine = EntityUtil.getFirst(delegator.findList("TestingType", null, null, null, null, false));
        GenericValue firstRecordByEntityQuery = EntityQuery.use(delegator).from("TestingType").queryFirst();

        assertEquals("queryFirst(): Record matched = testingTypeId", firstRecordByEntityEngine.getString("testingTypeId"),
                firstRecordByEntityQuery.getString("testingTypeId"));
        assertEquals("queryFirst(): Record matched = description", firstRecordByEntityEngine.getString("description"),
                firstRecordByEntityQuery.getString("description"));
    }

    /**
     * queryOne(): This method returns only one record based on the conditions given.
     * assert 1: Compared 'testingTypeId' field of record fetched by Entity Engine method and by EntityQuery method.
     * assert 2: Compared 'description' field of first record fetched by Entity Engine method and by EntityQuery method.
     */
    public void testQueryOne() throws GenericEntityException {
        Delegator delegator = getDelegator();
        List<GenericValue> testingTypes = new LinkedList<>();
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "queryOne-1", "description", "query one"));
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "queryOne-2", "description", "query two"));
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "queryOne-3", "description", "query three"));
        delegator.storeAll(testingTypes);

        GenericValue findOneByEntityEngine = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "queryOne-2").queryOne();
        GenericValue queryOneByEntityQuery = EntityQuery.use(delegator).from("TestingType").where("testingTypeId", "queryOne-2").queryOne();

        assertEquals("queryOne(): Record matched = testingTypeId", findOneByEntityEngine.getString("testingTypeId"),
                queryOneByEntityQuery.getString("testingTypeId"));
        assertEquals("queryOne(): Record matched = description", findOneByEntityEngine.getString("description"),
                queryOneByEntityQuery.getString("description"));
    }

    /**
     * queryOne(): This method returns only one record based on the conditions given, resolve from a context Map.
     * assert 1: Check the TestingType entity queryOneMap-2 has been resolve
     * assert 2: Check the TestingType entity queryOneMap-3 has been resolve with the parameters map present in context
     */
    public void testQueryOneWithContext() throws GenericEntityException {
        Delegator delegator = getDelegator();
        List<GenericValue> testingTypes = new LinkedList<>();
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "queryOneMap-1", "description", "query one by map"));
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "queryOneMap-2", "description", "query two by map"));
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "queryOneMap-3", "description", "query three by map"));
        delegator.storeAll(testingTypes);

        Map<String, Object> context = UtilMisc.toMap("testingTypeId", "queryOneMap-2", "description", "query two by map", "otherField", "otherValue");
        GenericValue queryOneByEntityQueryAndContext = EntityQuery.use(delegator).from("TestingType").where(context).queryOne();
        assertNotNull("queryOne() with context: Record found", queryOneByEntityQueryAndContext);

        context = UtilMisc.toMap("description", "query two by map", "otherField", "otherValue",
                "parameters", UtilMisc.toMap("testingTypeId", "queryOneMap-3", "description", "query three by map", "otherField", "otherValue"));
        GenericValue queryOneByEntityQueryAndParameters = EntityQuery.use(delegator).from("TestingType").where(context).queryOne();
        assertNotNull("queryOne() with parameters: Record found", queryOneByEntityQueryAndParameters);
        assertEquals("queryOne() with parameters: Record is queryOneMap-3 ", "queryOneMap-3",
                queryOneByEntityQueryAndParameters.getString("testingTypeId"));
    }

    /**
     * select(): This method is used to select particular fields only from the entity.
     * assert 1: Compared value of first record of selected 'description' field by both EntityEngine method and EntityQuery method.
     * assert 2: Compared 'testingTypeId' field for null which is fetched by EntityQuery method.
     */
    public void testSelect() throws GenericEntityException {
        Delegator delegator = getDelegator();
        List<GenericValue> testingTypes = new LinkedList<>();
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "select-1", "description", "description one"));
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "select-2", "description", "description two"));
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "select-3", "description", "description three"));
        delegator.storeAll(testingTypes);

        List<GenericValue> selectByEntityEngine = delegator.findList("TestingType", null, UtilMisc.toSet("description"),
                UtilMisc.toList("description"), null, false);
        List<GenericValue> selectByEntityQuery = EntityQuery.use(delegator).select("description").from("TestingType")
                .orderBy("description").queryList();

        assertEquals("select(): Record matched = description", selectByEntityEngine.get(0).getString("description"),
                selectByEntityQuery.get(0).getString("description"));
        assertNull(selectByEntityQuery.get(0).getString("testingTypeId"));
    }

    /**
     * distinct(): This method is used to get distinct values of records from entity field.
     * (Note: Distinct method is generally used with select method)
     * assert 1: Compared size of the list returned by Entity Engine method and by EntityQuery method.
     * assert 2: Compared value of first record of selected 'description' field by both EntityEngine method and EntityQuery method.
     * assert 3: Compared 'testingTypeId' field for null which is fetched by EntityQuery method.
     */
    public void testDistinctAndSelect() throws GenericEntityException {
        Delegator delegator = getDelegator();
        List<GenericValue> testingTypes = new LinkedList<>();
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "distinct-1", "description", "Distinct Record"));
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "distinct-2", "description", "Distinct Record"));
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "distinct-3", "description", "Not a Distinct Record"));
        delegator.storeAll(testingTypes);

        EntityFindOptions findOptions = new EntityFindOptions();
        findOptions.setDistinct(true);
        List<GenericValue> distinctByEntityEngine = delegator.findList("TestingType", null, UtilMisc.toSet("description"),
                UtilMisc.toList("description"), findOptions, false);
        List<GenericValue> distinctByEntityQuery = EntityQuery.use(delegator).select("description").from("TestingType")
                .distinct().orderBy("description").queryList();

        assertEquals("distinct(): Number of records found by EntityEngine method are matching with records found by EntityQuery distinct method",
                distinctByEntityEngine.size(), distinctByEntityQuery.size());
        assertEquals("distinct(): Record matched = description", distinctByEntityEngine.get(0).getString("description"),
                distinctByEntityQuery.get(0).getString("description"));
        assertNull(distinctByEntityQuery.get(0).getString("testingTypeId"));
    }

    /**
     * orderBy(): This method sorts the records found according to the given field or combination of fields.
     * assert 1: Compared number of records returned by Entity Engine method and by EntityQuery method.
     * assert 2: Compared 'testingTypeId' field of first record fetched by Entity Engine method and by EntityQuery method.
     * assert 3: Compared 'description' field of first record fetched by Entity Engine method and by EntityQuery method.
     */
    public void testOrderBy() throws GenericEntityException {
        Delegator delegator = getDelegator();
        List<GenericValue> testingTypes = new LinkedList<>();
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "orderBy-1", "description", "B"));
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "orderBy-2", "description", "C"));
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "orderBy-3", "description", "A"));
        delegator.storeAll(testingTypes);

        List<GenericValue> orderedByEntityEngine = delegator.findList("TestingType", EntityCondition.makeCondition("testingTypeId",
                EntityOperator.LIKE, "orderBy-%"), null, UtilMisc.toList("description"), null, false);
        List<GenericValue> orderedByEntityQuery = EntityQuery.use(delegator).from("TestingType").where(EntityCondition.makeCondition("testingTypeId",
                EntityOperator.LIKE, "orderBy-%")).orderBy("description").queryList();

        assertEquals("orderBy(): Number of records found by both the methods matched", orderedByEntityEngine.size(), orderedByEntityQuery.size());
        assertEquals("orderBy(): Record matched = testingTypeId", orderedByEntityEngine.get(0).getString("testingTypeId"),
                orderedByEntityQuery.get(0).getString("testingTypeId"));
        assertEquals("orderBy(): Record matched = description", orderedByEntityEngine.get(0).getString("description"),
                orderedByEntityQuery.get(0).getString("description"));
    }

    /**
     * filteByDate(): This method return only values that are currently active using from/thruDate fields.
     * assert 1: Compared number of records returned by Entity Engine method and by EntityQuery method.
     * assert 2: Compared 'testingNodeId' field of first record fetched by Entity Engine method and by EntityQuery method.
     * assert 3: Compared 'testingId' field of first record fetched by Entity Engine method and by EntityQuery method.
     * assert 4: Compared 'fromDate' field of first record fetched by Entity Engine method and by EntityQuery method.
     * assert 5: Compared 'thruDate' field of first record fetched by Entity Engine method and by EntityQuery method.
     */
    public void testFilterByDate() throws GenericEntityException {
        Delegator delegator = getDelegator();
        delegator.create("TestingType", "testingTypeId", "filterByDate-1", "description", "Filter BY Date");

        delegator.create("Testing", "testingId", "testing-1", "testingTypeId", "filterByDate-1");
        delegator.create("Testing", "testingId", "testing-2", "testingTypeId", "filterByDate-1");
        delegator.create("Testing", "testingId", "testing-3", "testingTypeId", "filterByDate-1");
        delegator.create("Testing", "testingId", "testing-4", "testingTypeId", "filterByDate-1");

        delegator.create("TestingNode", "testingNodeId", "testingNode-1");
        delegator.create("TestingNode", "testingNodeId", "testingNode-2");
        delegator.create("TestingNode", "testingNodeId", "testingNode-3");
        delegator.create("TestingNode", "testingNodeId", "testingNode-4");

        delegator.create("TestingNodeMember", "testingNodeId", "testingNode-1", "testingId", "testing-1", "fromDate",
                UtilDateTime.nowTimestamp(), "thruDate", UtilDateTime.getNextDayStart(UtilDateTime.nowTimestamp()));
        delegator.create("TestingNodeMember", "testingNodeId", "testingNode-2", "testingId", "testing-2", "fromDate",
                UtilDateTime.nowTimestamp(), "thruDate", UtilDateTime.getNextDayStart(UtilDateTime.nowTimestamp()));
        delegator.create("TestingNodeMember", "testingNodeId", "testingNode-3", "testingId", "testing-3", "fromDate",
                UtilDateTime.getNextDayStart(UtilDateTime.nowTimestamp()), "thruDate", UtilDateTime.getWeekEnd(UtilDateTime.nowTimestamp()));
        delegator.create("TestingNodeMember", "testingNodeId", "testingNode-4", "testingId", "testing-4", "fromDate",
                UtilDateTime.getMonthStart(UtilDateTime.nowTimestamp()), "thruDate", UtilDateTime.getMonthStart(UtilDateTime.nowTimestamp()));

        List<GenericValue> filteredByEntityUtil = EntityUtil.filterByDate(delegator.findList("TestingNodeMember", null, null,
                UtilMisc.toList("testingNodeId"), null, false));
        List<GenericValue> filteredByEntityQuery = EntityQuery.use(delegator).from("TestingNodeMember").filterByDate()
                .orderBy("testingNodeId").queryList();

        assertEquals("filterByDate(): Number of records found by both the methods matched", filteredByEntityUtil.size(),
                filteredByEntityQuery.size());
        assertEquals("filterByDate(): Record matched = testingNodeId", filteredByEntityUtil.get(0).getString("testingNodeId"),
                filteredByEntityQuery.get(0).getString("testingNodeId"));
        assertEquals("filterByDate(): Record matched = testingId", filteredByEntityUtil.get(0).getString("testingId"),
                filteredByEntityQuery.get(0).getString("testingId"));
        assertEquals("filterByDate(): Record matched = fromDate", filteredByEntityUtil.get(0).getString("fromDate"),
                filteredByEntityQuery.get(0).getString("fromDate"));
        assertEquals("filterByDate(): Record matched = thruDate", filteredByEntityUtil.get(0).getString("thruDate"),
                filteredByEntityQuery.get(0).getString("thruDate"));
    }

    /**
     * maxRows(): This method sets the maximum number of records to be fetched by the query.
     * assert 1: Compared number of records returned by Entity Engine method and by EntityQuery method.
     * assert 2: Compared 'testingTypeId' field of first record fetched by Entity Engine method and by EntityQuery method.
     * assert 3: Compared 'description' field of first record fetched by Entity Engine method and by EntityQuery method.
     */
    public void testMaxRows() throws GenericEntityException {
        Delegator delegator = getDelegator();
        List<GenericValue> testingTypes = new LinkedList<>();
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "maxRows-1", "description", "Max Row One"));
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "maxRows-2", "description", "Max Row Two"));
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "maxRows-3", "description", "Max Row Three"));
        delegator.storeAll(testingTypes);

        EntityFindOptions findOptions = new EntityFindOptions();
        findOptions.setMaxRows(2);
        List<GenericValue> maxRowsByEntityEngine = delegator.findList("TestingType", null, null, UtilMisc.toList("description"), findOptions, false);
        List<GenericValue> maxRowsByEntityQuery = EntityQuery.use(delegator).from("TestingType").maxRows(2).orderBy("description").queryList();

        assertEquals("maxRows(): Number of records found by both the methods matched", maxRowsByEntityEngine.size(), maxRowsByEntityQuery.size());
        assertEquals("maxRows(): Record matched = testingTypeId", maxRowsByEntityEngine.get(0).getString("testingTypeId"),
                maxRowsByEntityQuery.get(0).getString("testingTypeId"));
        assertEquals("maxRows(): Record matched = description", maxRowsByEntityEngine.get(0).getString("description"),
                maxRowsByEntityQuery.get(0).getString("description"));
    }

    /**
     * fetchSize(): This method sets the fetch size for the records to be fetched from the entity.
     * assert 1: Compared number of records returned by Entity Engine method and by EntityQuery method.
     * assert 2: Compared 'testingTypeId' field of first record fetched by Entity Engine method and by EntityQuery method.
     * assert 3: Compared 'description' field of first record fetched by Entity Engine method and by EntityQuery method.
     */
    public void testFetchSize() throws GenericEntityException {
        Delegator delegator = getDelegator();
        List<GenericValue> testingTypes = new LinkedList<>();
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "fetchSize-1", "description", "Fetch Size One"));
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "fetchSize-2", "description", "Fetch Size Two"));
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "fetchSize-3", "description", "Fetch Size Three"));
        delegator.storeAll(testingTypes);

        EntityFindOptions findOptions = new EntityFindOptions();
        findOptions.setFetchSize(2);
        List<GenericValue> fetchSizeByEntityEngine = delegator.findList("TestingType", null, null,
                UtilMisc.toList("description"), findOptions, false);
        List<GenericValue> fetchSizeByEntityQuery = EntityQuery.use(delegator).from("TestingType").fetchSize(2).orderBy("description").queryList();

        assertEquals("fetchSize(): Number of records found by both the methods matched", fetchSizeByEntityEngine.size(),
                fetchSizeByEntityQuery.size());
        assertEquals("fetchSize(): Record matched = testingTypeId", fetchSizeByEntityEngine.get(0).getString("testingTypeId"),
                fetchSizeByEntityQuery.get(0).getString("testingTypeId"));
        assertEquals("fetchSize(): Record matched = description", fetchSizeByEntityEngine.get(0).getString("description"),
                fetchSizeByEntityQuery.get(0).getString("description"));
    }

    /**
     * queryIterator(): This method is used to get iterator object over the entity.
     * assert: Compared first record of both the iterator.
     */
    public void testQueryIterator() throws GenericEntityException {
        Delegator delegator = getDelegator();
        List<GenericValue> testingTypes = new LinkedList<>();
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "queryIterator-1", "description", "Value One"));
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "queryIterator-2", "description", "Value Two"));
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "queryIterator-3", "description", "Value Three"));
        delegator.storeAll(testingTypes);

        boolean transactionStarted = false;
        try {
            transactionStarted = TransactionUtil.begin();

            EntityListIterator eliByEntityEngine = null;
            EntityListIterator eliByEntityQuery = null;
            eliByEntityEngine = delegator.find("TestingType", null, null, null, null, null);
            eliByEntityQuery = EntityQuery.use(delegator).from("TestingType").queryIterator();

            GenericValue recordByEntityEngine = eliByEntityEngine.next();
            GenericValue recordByEntityQuery = eliByEntityQuery.next();

            assertEquals("queryIterator(): Value of first record pointed by both iterators matched", recordByEntityEngine, recordByEntityQuery);
            eliByEntityEngine.close();
            eliByEntityQuery.close();

            TransactionUtil.commit(transactionStarted);
        } catch (GenericEntityException e) {
            TransactionUtil.rollback(transactionStarted, "Transaction is Rolled Back", e);
        }
    }

    /**
     * cursorForwardOnly(): Indicate that the ResultSet object's cursor may move only forward
     * assert: Compared first record found by both the iterator.
     */
    public void testCursorForwardOnly() throws GenericEntityException {
        Delegator delegator = getDelegator();
        List<GenericValue> testingTypes = new LinkedList<>();
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "cursorForwardOnly-1", "description", "cursorForwardOnly One"));
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "cursorForwardOnly-2", "description", "cursorForwardOnly Two"));
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "cursorForwardOnly-3", "description", "cursorForwardOnly Three"));
        delegator.storeAll(testingTypes);

        boolean transactionStarted = false;
        try {
            transactionStarted = TransactionUtil.begin();

            EntityListIterator eliByEntityEngine = null;
            EntityListIterator eliByEntityQuery = null;
            EntityFindOptions findOptions = new EntityFindOptions();
            findOptions.setResultSetType(EntityFindOptions.TYPE_FORWARD_ONLY);
            eliByEntityEngine = delegator.find("TestingType", null, null, null, null, findOptions);
            eliByEntityQuery = EntityQuery.use(delegator).from("TestingType").cursorForwardOnly().queryIterator();

            GenericValue nextRecordByEntityEngine = eliByEntityEngine.next();
            GenericValue nextRecordByEntityQuery = eliByEntityQuery.next();

            assertEquals("cursorForwardOnly(): Value of first record pointed by both iterators matched", nextRecordByEntityEngine,
                    nextRecordByEntityQuery);
            eliByEntityEngine.close();
            eliByEntityQuery.close();

            TransactionUtil.commit(transactionStarted);
        } catch (GenericEntityException e) {
            TransactionUtil.rollback(transactionStarted, "Transaction is Rolled Back", e);
        }
    }

    /**
     * cursorScrollSensitive(): ResultSet object's cursor is scrollable but generally sensitive to changes to the data that underlies the ResultSet.
     * assert: Compared first record found by both the iterators.
     */
    public void testCursorScrollSensitive() throws GenericEntityException {
        Delegator delegator = getDelegator();
        List<GenericValue> testingTypes = new LinkedList<>();
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "scrollSensitive-1", "description", "cursorScrollSensitive One"));
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "scrollSensitive-2", "description", "cursorScrollSensitive Two"));
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "scrollSensitive-3", "description", "cursorScrollSensitive Three"));
        delegator.storeAll(testingTypes);

        boolean transactionStarted = false;
        try {
            transactionStarted = TransactionUtil.begin();

            EntityListIterator eliByEntityEngine = null;
            EntityListIterator eliByEntityQuery = null;
            EntityFindOptions findOptions = new EntityFindOptions();
            findOptions.setResultSetType(EntityFindOptions.TYPE_SCROLL_SENSITIVE);
            eliByEntityEngine = delegator.find("TestingType", null, null, null, null, findOptions);
            eliByEntityQuery = EntityQuery.use(delegator).from("TestingType").cursorScrollSensitive().queryIterator();

            GenericValue nextRecordByDelegator = eliByEntityEngine.next();
            GenericValue nextRecordByEntityQuery = eliByEntityQuery.next();

            assertEquals("cursorScrollSensitive(): Records by delegator method and by EntityQuery method matched", nextRecordByDelegator,
                    nextRecordByEntityQuery);
            eliByEntityEngine.close();
            eliByEntityQuery.close();

            TransactionUtil.commit(transactionStarted);
        } catch (GenericEntityException e) {
            TransactionUtil.rollback(transactionStarted, "Transaction is Rolled Back", e);
        }
    }

    /**
     * cursorScrollInSensitive(): ResultSet object's cursor is scrollable but generally not sensitive to changes to the data that
     * underlies the ResultSet.
     * assert: Compared first record found by both the iterators.
     */
    public void testCursorScrollInSensitive() throws GenericEntityException {
        Delegator delegator = getDelegator();
        List<GenericValue> testingTypes = new LinkedList<>();
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "scrollInSensitive-1", "description", "cursorScrollInSensitive One"));
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "scrollInSensitive-2", "description", "cursorScrollInSensitive Two"));
        testingTypes.add(delegator.makeValue("TestingType", "testingTypeId", "scrollInSensitive-3", "description", "cursorScrollInSensitive Three"));
        delegator.storeAll(testingTypes);

        boolean transactionStarted = false;
        try {
            transactionStarted = TransactionUtil.begin();

            EntityListIterator eliByEntityEngine = null;
            EntityListIterator eliByEntityQuery = null;
            EntityFindOptions findOptions = new EntityFindOptions();
            findOptions.setResultSetType(EntityFindOptions.TYPE_SCROLL_INSENSITIVE);
            eliByEntityEngine = delegator.find("TestingType", null, null, null, null, findOptions);
            eliByEntityQuery = EntityQuery.use(delegator).from("TestingType").cursorScrollInsensitive().queryIterator();

            GenericValue nextRecordByDelegator = eliByEntityEngine.next();
            GenericValue nextRecordByEntityQuery = eliByEntityQuery.next();

            assertEquals("cursorScrollInSensitive(): Records by delegator method and by EntityQuery method matched",
                    nextRecordByDelegator, nextRecordByEntityQuery);
            eliByEntityEngine.close();
            eliByEntityQuery.close();

            TransactionUtil.commit(transactionStarted);
        } catch (GenericEntityException e) {
            TransactionUtil.rollback(transactionStarted, "Transaction is Rolled Back", e);
        }
    }
}
