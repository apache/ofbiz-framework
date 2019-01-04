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
package org.apache.ofbiz.entity.condition;

import java.util.Iterator;
import java.util.List;

/**
 * A condition expression corresponding to an ordered collection of conditions
 * that are joined by an operator.
 * <p>
 * The main objective it to express the conjunction or disjunction of a set of
 * conditions which in the case of conjunction corresponds to SQL expression
 * of the form {@code foo=bar AND bar=baz AND ...}.
 */
@SuppressWarnings("serial")
public final class EntityConditionList<T extends EntityCondition> extends EntityConditionListBase<T> {
    /**
     * Constructs an entity condition list.
     *
     * @param conditionList the list of conditions
     * @param operator the operator used to join the list of conditions
     */
    public EntityConditionList(List<? extends T> conditionList, EntityJoinOperator operator) {
        super(conditionList, operator);
    }

    /**
     * Provides the size of the internal list of condition expressions.
     *
     * @return the size of the internal list of condition expressions
     */
    public int getConditionListSize() {
        return conditions.size();
    }

    /**
     * Provides an iterator to iterate on the internal list of condition expressions.
     *
     * @return an iterator iterating on the internal list of condition expressions
     */
    @SuppressWarnings("unchecked")
    public Iterator<T> getConditionIterator() {
        return (Iterator<T>)conditions.iterator();
    }

    @Override
    public void accept(EntityConditionVisitor visitor) {
        visitor.visit(this);
    }
}
