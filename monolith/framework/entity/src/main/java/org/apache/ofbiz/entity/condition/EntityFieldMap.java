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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.entity.util.EntityUtil;

/**
 * A condition expression corresponding to an unordered collection of
 * conditions containing two values compared with a comparison operator
 * and that are joined by an operator.
 * <p>
 * The main objective it to express the conjunction or disjunction of a set of
 * conditions which in the case of conjunction corresponds to SQL expression
 * of the form {@code foo=bar AND bar=baz AND ...} and where the comparison
 * operator is {@code =} and the join operator is {@code AND}.
 */
@SuppressWarnings("serial")
public final class EntityFieldMap extends EntityConditionListBase<EntityExpr> {

    /** The map whose entries correspond to the set of equality checks conditions. */
    private final Map<String, ?> fieldMap;

    /**
     * Converts a map of condition fields into a list of condition expression.
     * @param fieldMap the condition fields
     * @param op the operator used to compared each entry in the condition field map.
     * @return a list of condition expression
     */
    private static <V> List<EntityExpr> makeConditionList(Map<String, V> fieldMap, EntityComparisonOperator<?, V> op) {
        return (fieldMap == null)
            ? Collections.emptyList()
            : fieldMap.entrySet().stream()
                      .map(entry -> EntityCondition.makeCondition(entry.getKey(), op, entry.getValue()))
                      .collect(Collectors.toList());
    }

    /**
     * Constructs a map of fields.
     * @param compOp the operator used to compare fields
     * @param joinOp the operator used to join field comparisons
     * @param keysValues a list of values that the field map will contain.
     *        This list must be of even length and each successive pair will
     *        be associated in the field map.
     * @param <V> The type of values that are compared.
     */
    @SafeVarargs
    public <V> EntityFieldMap(EntityComparisonOperator<?, ?> compOp, EntityJoinOperator joinOp, V... keysValues) {
        this(EntityUtil.makeFields(keysValues), UtilGenerics.cast(compOp), joinOp);
    }

    /**
     * Constructs a map of fields.
     * @param fieldMap the map containing the fields to compare
     * @param compOp the operator to compare fields
     * @param joinOp the operator to join entries in the field map
     * @param <V> the type of values contained in {@code fieldMap}
     */
    public <V> EntityFieldMap(Map<String, V> fieldMap, EntityComparisonOperator<?, ?> compOp,
                              EntityJoinOperator joinOp) {
        super(makeConditionList(fieldMap, UtilGenerics.cast(compOp)), joinOp);
        this.fieldMap = (fieldMap == null) ? Collections.emptyMap() : fieldMap;
    }

    /**
     * Gets the value associated with field {@code name}.
     * @param name the name of the field
     * @return the value associated with field {@code name}
     * @throws NullPointerException if the specified name is {@code null}
     *         and the field map does not permit null keys
     */
    public Object getField(String name) {
        return fieldMap.get(name);
    }

    /**
     * Checks if the field map contains the field {@code name}.
     * @param name the name of the field to search
     * @return {@code true} if field is defined in the field map
     * @throws NullPointerException if the specified name is {@code null}
     *         and the field map does not permit null keys
     */
    public boolean containsField(String name) {
        return fieldMap.containsKey(name);
    }

    /**
     * Provides an iterator on the fields contained in the field map.
     * @return an iterator of fields
     */
    public Iterator<String> getFieldKeyIterator() {
        return Collections.unmodifiableSet(fieldMap.keySet()).iterator();
    }

    /**
     * Provides an iterator on the entries contained in the field map.
     * @return an iterator of field entries
     */
    public Iterator<Map.Entry<String, ? extends Object>> getFieldEntryIterator() {
        return Collections.<Map.Entry<String, ? extends Object>>unmodifiableSet(fieldMap.entrySet()).iterator();
    }

    @Override
    public void accept(EntityConditionVisitor visitor) {
        visitor.visit(this);
    }
}
