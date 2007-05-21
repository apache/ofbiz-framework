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
package org.ofbiz.order.order;

import java.util.*;
import javax.servlet.http.*;
import javolution.util.*;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.entity.condition.*;
import org.ofbiz.entity.util.*;

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
public class OrderListState {

    public static final String module = OrderListState.class.getName();
    public static final String SESSION_KEY = "__ORDER_LIST_STATUS__";
    public static final String VIEW_SIZE_PARAM = "viewSize";
    public static final String VIEW_INDEX_PARAM = "viewIndex";

    // state variables
    protected int viewSize;
    protected int viewIndex;
    protected Map orderStatusState;
    protected Map orderTypeState;
    protected Map orderFilterState;
    protected int orderListSize;

    // parameter to ID maps
    protected static final Map parameterToOrderStatusId;
    protected static final Map parameterToOrderTypeId;
    protected static final Map parameterToFilterId;
    static {
        Map map = FastMap.newInstance();
        map.put("viewcompleted", "ORDER_COMPLETED");
        map.put("viewcancelled", "ORDER_CANCELLED");
        map.put("viewrejected", "ORDER_REJECTED");
        map.put("viewapproved", "ORDER_APPROVED");
        map.put("viewcreated", "ORDER_CREATED");
        map.put("viewprocessing", "ORDER_PROCESSING");
        map.put("viewsent", "ORDER_SENT");
        map.put("viewhold", "ORDER_HOLD");
        parameterToOrderStatusId = map;

        map = FastMap.newInstance();
        map.put("view_SALES_ORDER", "SALES_ORDER");
        map.put("view_PURCHASE_ORDER", "PURCHASE_ORDER");
        parameterToOrderTypeId = map;

        map = FastMap.newInstance();
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
        orderStatusState = FastMap.newInstance();
        orderTypeState = FastMap.newInstance();
        orderFilterState = FastMap.newInstance();        

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
            if (!UtilValidate.isEmpty(viewSizeParam) && !UtilValidate.isEmpty(viewIndexParam))
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
        for (Iterator iter = parameterToOrderStatusId.keySet().iterator(); iter.hasNext(); ) {
            String param = (String) iter.next();
            String value = request.getParameter(param);
            if ("Y".equals(value)) {
                orderStatusState.put(param, "Y");
            } else {
                orderStatusState.put(param, "N");
            }
        }
        for (Iterator iter = parameterToOrderTypeId.keySet().iterator(); iter.hasNext(); ) {
            String param = (String) iter.next();
            String value = request.getParameter(param);
            if ("Y".equals(value)) {
                orderTypeState.put(param, "Y");
            } else {
                orderTypeState.put(param, "N");
            }
        }
        for (Iterator iter = parameterToFilterId.keySet().iterator(); iter.hasNext(); ) {
            String param = (String) iter.next();
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


    public Map getOrderStatusState() { return orderStatusState; };
    public Map getOrderTypeState() { return orderTypeState; }
    public Map getorderFilterState() { return orderFilterState; }
    
    public boolean hasStatus(String param) { return ("Y".equals(orderStatusState.get(param))); }
    public boolean hasType(String param) { return ("Y".equals(orderTypeState.get(param))); }
    public boolean hasFilter(String param) { return ("Y".equals(orderFilterState.get(param))); }
    
    public boolean hasAllStatus() {
        for (Iterator iter = orderStatusState.values().iterator(); iter.hasNext(); ) {
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
    public List getOrders(String facilityId, GenericDelegator delegator) throws GenericEntityException {
        List allConditions = new ArrayList();

        if (facilityId != null) {
            allConditions.add(new EntityExpr("originFacilityId", EntityOperator.EQUALS, facilityId));
        }

        List statusConditions = new ArrayList();
        for (Iterator iter = orderStatusState.keySet().iterator(); iter.hasNext(); ) {
            String status = (String) iter.next();
            if (!hasStatus(status)) continue;
            statusConditions.add( new EntityExpr("statusId", EntityOperator.EQUALS, parameterToOrderStatusId.get(status)) );
        }
        List typeConditions = new ArrayList();
        for (Iterator iter = orderTypeState.keySet().iterator(); iter.hasNext(); ) {
            String type = (String) iter.next();
            if (!hasType(type)) continue;
            typeConditions.add( new EntityExpr("orderTypeId", EntityOperator.EQUALS, parameterToOrderTypeId.get(type)) );
        }
                
        EntityCondition statusConditionsList = new EntityConditionList(statusConditions,  EntityOperator.OR);
        EntityCondition typeConditionsList = new EntityConditionList(typeConditions, EntityOperator.OR);
        if ((typeConditions.size() > 0) && (statusConditions.size() > 0)) {
            allConditions.add(statusConditionsList);
            allConditions.add(typeConditionsList);
        }

        EntityCondition queryConditionsList = new EntityConditionList(allConditions, EntityOperator.AND);
        EntityFindOptions options = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true);
        EntityListIterator iterator = delegator.findListIteratorByCondition("OrderHeader", queryConditionsList, null, null, UtilMisc.toList("orderDate DESC"), options);

        // get subset corresponding to pagination state
        List orders = iterator.getPartialList(viewSize * viewIndex, viewSize);
        iterator.last();
        orderListSize = iterator.currentIndex();
        iterator.close();
        //Debug.logInfo("### size of list: " + orderListSize, module);
        return orders;
    }

    public String toString() {
        StringBuffer buff = new StringBuffer("OrderListState:\n\t");
        buff.append("viewIndex=").append(viewIndex).append(", viewSize=").append(viewSize).append("\n\t");
        buff.append(getOrderStatusState().toString()).append("\n\t");
        buff.append(getOrderTypeState().toString()).append("\n\t");
        buff.append(getorderFilterState().toString()).append("\n\t");
        return buff.toString();
    }
}
