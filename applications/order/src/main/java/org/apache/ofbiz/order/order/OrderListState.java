/*
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
 */
package org.apache.ofbiz.order.order;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;

/**
 * Session object for keeping track of the list of orders.
 * The state of the list is preserved here instead of
 * via url parameters, which can get messy.  There
 * are three types of state:  Order State, Order Type,
 * and pagination position.
 *
 * Also provides convenience methods for retrieving
 * the right set of data for a particular state.
 *
 * TODO: this can be generalized to use a set of State
 * objects, including Pagination. Think about design
 * patterns in Fowler.
 */
@SuppressWarnings("serial")
public class OrderListState implements Serializable {

    public static final String module = OrderListState.class.getName();
    public static final String SESSION_KEY = "__ORDER_LIST_STATUS__";
    public static final String VIEW_SIZE_PARAM = "viewSize";
    public static final String VIEW_INDEX_PARAM = "viewIndex";

    // state variables
    protected int viewSize;
    protected int viewIndex;
    protected Map<String, String> orderStatusState;
    protected Map<String, String> orderTypeState;
    protected Map<String, String> orderFilterState;
    protected int orderListSize;

    // parameter to ID maps
    protected static final Map<String, String> parameterToOrderStatusId;
    protected static final Map<String, String> parameterToOrderTypeId;
    protected static final Map<String, String> parameterToFilterId;
    static {
        Map<String, String> map = new HashMap<String, String>();
        map.put("viewcompleted", "ORDER_COMPLETED");
        map.put("viewcancelled", "ORDER_CANCELLED");
        map.put("viewrejected", "ORDER_REJECTED");
        map.put("viewapproved", "ORDER_APPROVED");
        map.put("viewcreated", "ORDER_CREATED");
        map.put("viewprocessing", "ORDER_PROCESSING");
        map.put("viewhold", "ORDER_HOLD");
        parameterToOrderStatusId = map;

        map = new HashMap<String, String>();
        map.put("view_SALES_ORDER", "SALES_ORDER");
        map.put("view_PURCHASE_ORDER", "PURCHASE_ORDER");
        parameterToOrderTypeId = map;

        map = new HashMap<String, String>();
        map.put("filterInventoryProblems", "filterInventoryProblems");
        map.put("filterAuthProblems", "filterAuthProblems");
        map.put("filterPartiallyReceivedPOs", "filterPartiallyReceivedPOs");
        map.put("filterPOsOpenPastTheirETA", "filterPOsOpenPastTheirETA");
        map.put("filterPOsWithRejectedItems", "filterPOsWithRejectedItems");
        parameterToFilterId = map;
    }

    //=============   Initialization and Request methods   ===================//

    /**
     * Initializes the order list state with default values. Do not use directly,
     * instead use getInstance().
     */
    protected OrderListState() {
        viewSize = 10;
        viewIndex = 0;
        orderStatusState = new HashMap<String, String>();
        orderTypeState = new HashMap<String, String>();
        orderFilterState = new HashMap<String, String>();

        // defaults (TODO: configuration)
        orderStatusState.put("viewcreated", "Y");
        orderStatusState.put("viewprocessing", "Y");
        orderStatusState.put("viewapproved", "Y");
        orderStatusState.put("viewhold", "N");
        orderStatusState.put("viewcompleted", "N");
        orderStatusState.put("viewsent", "N");
        orderStatusState.put("viewrejected", "N");
        orderStatusState.put("viewcancelled", "N");
        orderTypeState.put("view_SALES_ORDER", "Y");
    }

    /**
     * Retrieves the current user's OrderListState from the session
     * or creates a new one with defaults.
     */
    public static OrderListState getInstance(HttpServletRequest request) {
        HttpSession session = request.getSession();
        OrderListState status = (OrderListState) session.getAttribute(SESSION_KEY);
        if (status == null) {
            status = new OrderListState();
            session.setAttribute(SESSION_KEY, status);
        }
        return status;
    }

    /**
     * Given a request, decides what state to change.  If a parameter changeStatusAndTypeState
     * is present with value "Y", the status and type state will be updated.  Otherwise, if the
     * viewIndex and viewSize parameters are present, the pagination changes.
     */
    public void update(HttpServletRequest request) {
        if ("Y".equals(request.getParameter("changeStatusAndTypeState"))) {
            changeOrderListStates(request);
        } else {
            String viewSizeParam = request.getParameter(VIEW_SIZE_PARAM);
            String viewIndexParam = request.getParameter(VIEW_INDEX_PARAM);
            if (UtilValidate.isNotEmpty(viewSizeParam) && UtilValidate.isNotEmpty(viewIndexParam))
                changePaginationState(viewSizeParam, viewIndexParam);
        }
    }

    private void changePaginationState(String viewSizeParam, String viewIndexParam) {
        try {
            viewSize = Integer.parseInt(viewSizeParam);
            viewIndex = Integer.parseInt(viewIndexParam);
        } catch (NumberFormatException e) {
            Debug.logWarning("Values of " + VIEW_SIZE_PARAM + " ["+viewSizeParam+"] and " + VIEW_INDEX_PARAM + " ["+viewIndexParam+"] must both be Integers. Not paginating order list.", module);
        }
    }

    private void changeOrderListStates(HttpServletRequest request) {
        for (String param : parameterToOrderStatusId.keySet()) {
            String value = request.getParameter(param);
            if ("Y".equals(value)) {
                orderStatusState.put(param, "Y");
            } else {
                orderStatusState.put(param, "N");
            }
        }
        for (String param : parameterToOrderTypeId.keySet()) {
            String value = request.getParameter(param);
            if ("Y".equals(value)) {
                orderTypeState.put(param, "Y");
            } else {
                orderTypeState.put(param, "N");
            }
        }
        for (String param : parameterToFilterId.keySet()) {
            String value = request.getParameter(param);
            if ("Y".equals(value)) {
                orderFilterState.put(param, "Y");
            } else {
                orderFilterState.put(param, "N");
            }
        }
        viewIndex = 0;
    }


    //==============   Get and Set methods   =================//


    public Map<String, String> getOrderStatusState() { return orderStatusState; }
    public Map<String, String> getOrderTypeState() { return orderTypeState; }
    public Map<String, String> getorderFilterState() { return orderFilterState; }

    public void setStatus(String param, boolean b) { orderStatusState.put(param, (b ? "Y" : "N")); }
    public void setType(String param, boolean b) { orderTypeState.put(param, (b ? "Y" : "N")); }
    
    public boolean hasStatus(String param) { return ("Y".equals(orderStatusState.get(param))); }
    public boolean hasType(String param) { return ("Y".equals(orderTypeState.get(param))); }
    public boolean hasFilter(String param) { return ("Y".equals(orderFilterState.get(param))); }

    public boolean hasAllStatus() {
        for (Iterator<String> iter = orderStatusState.values().iterator(); iter.hasNext();) {
            if (!"Y".equals(iter.next())) return false;
        }
        return true;
    }

    public int getViewSize() { return viewSize; }
    public int getViewIndex() { return viewIndex; }
    public int getSize() { return orderListSize; }

    public boolean hasPrevious() { return (viewIndex > 0); }
    public boolean hasNext() { return (viewIndex < getSize() / viewSize); }

    /**
     * Get the OrderHeaders corresponding to the state.
     */
    public List<GenericValue> getOrders(String facilityId, Timestamp filterDate, Delegator delegator) throws GenericEntityException {
        List<EntityCondition> allConditions = new LinkedList<EntityCondition>();

        if (facilityId != null) {
            allConditions.add(EntityCondition.makeCondition("originFacilityId", EntityOperator.EQUALS, facilityId));
        }

        if (filterDate != null) {
            List<EntityCondition> andExprs = new LinkedList<EntityCondition>();
            andExprs.add(EntityCondition.makeCondition("orderDate", EntityOperator.GREATER_THAN_EQUAL_TO, UtilDateTime.getDayStart(filterDate)));
            andExprs.add(EntityCondition.makeCondition("orderDate", EntityOperator.LESS_THAN_EQUAL_TO, UtilDateTime.getDayEnd(filterDate)));
            allConditions.add(EntityCondition.makeCondition(andExprs, EntityOperator.AND));
        }

        List<EntityCondition> statusConditions = new LinkedList<EntityCondition>();
        for (String status : orderStatusState.keySet()) {
            if (!hasStatus(status)) continue;
            statusConditions.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, parameterToOrderStatusId.get(status)));
        }
        List<EntityCondition> typeConditions = new LinkedList<EntityCondition>();
        for (String type : orderTypeState.keySet()) {
            if (!hasType(type)) continue;
            typeConditions.add(EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, parameterToOrderTypeId.get(type)));
        }

        EntityCondition statusConditionsList = EntityCondition.makeCondition(statusConditions,  EntityOperator.OR);
        EntityCondition typeConditionsList = EntityCondition.makeCondition(typeConditions, EntityOperator.OR);
        if (statusConditions.size() > 0) {
            allConditions.add(statusConditionsList);
        }
        if (typeConditions.size() > 0) {
            allConditions.add(typeConditionsList);
        }

        EntityListIterator iterator = EntityQuery.use(delegator).from("OrderHeader")
                .where(allConditions)
                .orderBy("orderDate DESC")
                .maxRows(viewSize * (viewIndex + 1))
                .cursorScrollInsensitive()
                .queryIterator();

        // get subset corresponding to pagination state
        List<GenericValue> orders = iterator.getPartialList(viewSize * viewIndex, viewSize);
        orderListSize = iterator.getResultsSizeAfterPartialList();
        iterator.close();
        return orders;
    }

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder("OrderListState:\n\t");
        buff.append("viewIndex=").append(viewIndex).append(", viewSize=").append(viewSize).append("\n\t");
        buff.append(getOrderStatusState().toString()).append("\n\t");
        buff.append(getOrderTypeState().toString()).append("\n\t");
        buff.append(getorderFilterState().toString()).append("\n\t");
        return buff.toString();
    }
}
