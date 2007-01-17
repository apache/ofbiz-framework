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
package org.ofbiz.entity.finder;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityJoinOperator;
import org.ofbiz.entity.finder.EntityFinderUtil.Condition;
import org.ofbiz.entity.finder.EntityFinderUtil.ConditionExpr;
import org.ofbiz.entity.finder.EntityFinderUtil.ConditionList;
import org.ofbiz.entity.finder.EntityFinderUtil.ConditionObject;
import org.ofbiz.entity.finder.EntityFinderUtil.GetAll;
import org.ofbiz.entity.finder.EntityFinderUtil.LimitRange;
import org.ofbiz.entity.finder.EntityFinderUtil.LimitView;
import org.ofbiz.entity.finder.EntityFinderUtil.OutputHandler;
import org.ofbiz.entity.finder.EntityFinderUtil.UseIterator;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.w3c.dom.Element;

/**
 * Uses the delegator to find entity values by a condition
 *
 */
public class ByConditionFinder implements Serializable {
    public static final String module = ByConditionFinder.class.getName();         
    
    protected FlexibleStringExpander entityNameExdr;
    protected FlexibleStringExpander useCacheStrExdr;
    protected FlexibleStringExpander filterByDateStrExdr;
    protected FlexibleStringExpander distinctStrExdr;
    protected FlexibleStringExpander delegatorNameExdr;
    protected FlexibleMapAccessor listAcsr;
    protected FlexibleStringExpander resultSetTypeExdr;
    
    protected Condition whereCondition;
    protected Condition havingCondition;
    protected List selectFieldExpanderList;
    protected List orderByExpanderList;
    protected OutputHandler outputHandler;

    public ByConditionFinder(Element element) {
        this.entityNameExdr = new FlexibleStringExpander(element.getAttribute("entity-name"));
        this.useCacheStrExdr = new FlexibleStringExpander(element.getAttribute("use-cache"));
        this.filterByDateStrExdr = new FlexibleStringExpander(element.getAttribute("filter-by-date"));
        this.distinctStrExdr = new FlexibleStringExpander(element.getAttribute("distinct"));
        this.delegatorNameExdr = new FlexibleStringExpander(element.getAttribute("delegator-name"));
        this.listAcsr = new FlexibleMapAccessor(element.getAttribute("list-name"));
        this.resultSetTypeExdr = new FlexibleStringExpander(element.getAttribute("result-set-type"));

        // NOTE: the whereCondition can be null, ie (condition-expr | condition-list) is optional; if left out, means find all, or with no condition in essense  
        // process condition-expr | condition-list
        Element conditionExprElement = UtilXml.firstChildElement(element, "condition-expr");
        Element conditionListElement = UtilXml.firstChildElement(element, "condition-list");
        Element conditionObjectElement = UtilXml.firstChildElement(element, "condition-object");
        if (conditionExprElement != null) {
            this.whereCondition = new ConditionExpr(conditionExprElement);
        } else if (conditionListElement != null) {
            this.whereCondition = new ConditionList(conditionListElement);
        } else if (conditionObjectElement != null) {
            this.whereCondition = new ConditionObject(conditionObjectElement);
        }
        
        // process having-condition-list
        Element havingConditionListElement = UtilXml.firstChildElement(element, "having-condition-list");
        if (havingConditionListElement != null) {
            this.havingCondition = new ConditionList(havingConditionListElement);
        }

        // process select-field
        selectFieldExpanderList = EntityFinderUtil.makeSelectFieldExpanderList(element);
        
        // process order-by
        List orderByElementList = UtilXml.childElementList(element, "order-by");
        if (orderByElementList.size() > 0) {
            orderByExpanderList = new LinkedList();
            Iterator orderByElementIter = orderByElementList.iterator();
            while (orderByElementIter.hasNext()) {
                Element orderByElement = (Element) orderByElementIter.next();
                orderByExpanderList.add(new FlexibleStringExpander(orderByElement.getAttribute("field-name")));
            }
        }

        // process limit-range | limit-view | use-iterator
        Element limitRangeElement = UtilXml.firstChildElement(element, "limit-range");
        Element limitViewElement = UtilXml.firstChildElement(element, "limit-view");
        Element useIteratorElement = UtilXml.firstChildElement(element, "use-iterator");
        if ((limitRangeElement != null && limitViewElement != null) || (limitRangeElement != null && useIteratorElement != null) || (limitViewElement != null && useIteratorElement != null)) {
            throw new IllegalArgumentException("In entity find by condition element, cannot have more than one of the following: limit-range, limit-view, and use-iterator");
        }
        if (limitRangeElement != null) {
            outputHandler = new LimitRange(limitRangeElement);
        } else if (limitViewElement != null) {
            outputHandler = new LimitView(limitViewElement);
        } else if (useIteratorElement != null) {
            outputHandler = new UseIterator(useIteratorElement);
        } else {
            // default to get all
            outputHandler = new GetAll();
        }
    }

    public void runFind(Map context, GenericDelegator delegator) throws GeneralException {
        String entityName = this.entityNameExdr.expandString(context);
        String useCacheStr = this.useCacheStrExdr.expandString(context);
        String filterByDateStr = this.filterByDateStrExdr.expandString(context);
        String distinctStr = this.distinctStrExdr.expandString(context);
        String delegatorName = this.delegatorNameExdr.expandString(context);
        String resultSetTypeString = this.resultSetTypeExdr.expandString(context);
        
        boolean useCache = "true".equals(useCacheStr);
        boolean filterByDate = "true".equals(filterByDateStr);
        boolean distinct = "true".equals(distinctStr);
        int resultSetType = ResultSet.TYPE_SCROLL_INSENSITIVE;
        if ("forward".equals(resultSetTypeString))
            resultSetType = ResultSet.TYPE_FORWARD_ONLY;
        
        if (delegatorName != null && delegatorName.length() > 0) {
            delegator = GenericDelegator.getGenericDelegator(delegatorName);
        }

        // create whereEntityCondition from whereCondition
        EntityCondition whereEntityCondition = null;
        if (this.whereCondition != null) {
            whereEntityCondition = this.whereCondition.createCondition(context, entityName, delegator);
        }

        // create havingEntityCondition from havingCondition
        EntityCondition havingEntityCondition = null;
        if (this.havingCondition != null) {
            havingEntityCondition = this.havingCondition.createCondition(context, entityName, delegator);
        }

        if (useCache) {
            // if useCache == true && outputHandler instanceof UseIterator, throw exception; not a valid combination
            if (outputHandler instanceof UseIterator) {
                throw new IllegalArgumentException("In find entity by condition cannot have use-cache set to true and select use-iterator for the output type.");
            }
            if (distinct) {
                throw new IllegalArgumentException("In find entity by condition cannot have use-cache set to true and set distinct to true.");
            }
            if (havingEntityCondition != null) {
                throw new IllegalArgumentException("In find entity by condition cannot have use-cache set to true and specify a having-condition-list (can only use a where condition with condition-expr or condition-list).");
            }
        }
        
        // get the list of fieldsToSelect from selectFieldExpanderList
        Set fieldsToSelect = EntityFinderUtil.makeFieldsToSelect(selectFieldExpanderList, context);

        //if fieldsToSelect != null and useCacheBool is true, throw an error
        if (fieldsToSelect != null && useCache) {
            throw new IllegalArgumentException("Error in entity query by condition definition, cannot specify select-field elements when use-cache is set to true");
        }
        
        // get the list of orderByFields from orderByExpanderList
        List orderByFields = EntityFinderUtil.makeOrderByFieldList(this.orderByExpanderList, context);
        
        try {
            // if filterByDate, do a date filter on the results based on the now-timestamp
            if (filterByDate) {
                EntityCondition filterByDateCondition = EntityUtil.getFilterByDateExpr();
                if (whereEntityCondition != null) {
                    whereEntityCondition = new EntityConditionList(UtilMisc.toList(whereEntityCondition, filterByDateCondition), EntityJoinOperator.AND);
                } else {
                    whereEntityCondition = filterByDateCondition;
                }
            }
            
            if (useCache) {
                List results = delegator.findByConditionCache(entityName, whereEntityCondition, fieldsToSelect, orderByFields);
                this.outputHandler.handleOutput(results, context, listAcsr);
            } else {
                boolean useTransaction = true;
                if (this.outputHandler instanceof UseIterator && !TransactionUtil.isTransactionInPlace()) {
                    Exception newE = new Exception("Stack Trace");
                    Debug.logError(newE, "ERROR: Cannot do a by condition find that returns an EntityListIterator with no transaction in place. Wrap this call in a transaction.", module);
                    useTransaction = false;
                }
                
                EntityFindOptions options = new EntityFindOptions();
                options.setDistinct(distinct);
                options.setResultSetType(resultSetType);
                boolean beganTransaction = false;
                try {
                    if (useTransaction) {
                        beganTransaction = TransactionUtil.begin();
                    }

                    EntityListIterator eli = delegator.findListIteratorByCondition(entityName, whereEntityCondition, havingEntityCondition, fieldsToSelect, orderByFields, options);
                    this.outputHandler.handleOutput(eli, context, listAcsr);
                } catch (GenericEntityException e) {
                    String errMsg = "Failure in by condition find operation, rolling back transaction";
                    Debug.logError(e, errMsg, module);
                    try {
                        // only rollback the transaction if we started one...
                        TransactionUtil.rollback(beganTransaction, errMsg, e);
                    } catch (GenericEntityException e2) {
                        Debug.logError(e2, "Could not rollback transaction: " + e2.toString(), module);
                    }
                    // after rolling back, rethrow the exception
                    throw e;
                } finally {
                    // only commit the transaction if we started one... this will throw an exception if it fails
                    TransactionUtil.commit(beganTransaction);
                }
            }
        } catch (GenericEntityException e) {
            String errMsg = "Error doing find by condition: " + e.toString();
            Debug.logError(e, module);
            throw new GeneralException(errMsg, e);
        }
    }
}

