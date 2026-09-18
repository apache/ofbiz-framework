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
package org.apache.ofbiz.service.engine

import org.apache.ofbiz.entity.Delegator
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityQuery
import org.apache.ofbiz.service.ServiceErrorException

/**
 * Fluent write-side helper backing {@link GroovyBaseScript#update(String)}.
 *
 * Usage: {@code update('SomeEntity').where(pkFields).set(fieldsToChange)} — queries the record,
 * merges the given fields into it, stores it and returns the updated {@link GenericValue}.
 * Throws {@link ServiceErrorException} if no record matches {@code where()}, so a missing record
 * fails fast instead of silently no-op'ing.
 *
 * {@code .ifExists()} suppresses that throw, returning {@code null} instead, for sites that
 * intentionally no-op on a missing record. {@code .first()} uses {@code queryFirst()} instead of
 * {@code queryOne()}, for sites where more than one record can legitimately match; unlike {@code
 * queryOne()}, {@code queryFirst()} does not narrow {@code where()} to PK fields only, so {@code
 * .first()} narrows it to the entity's own field names itself, making it just as safe to pass a raw
 * {@code parameters} map. {@code set(fields, false)} mirrors {@link
 * GenericValue#setNonPKFields(Map, boolean)}'s two-arg form, preserving existing field values
 * instead of nulling them for empty-string input fields.
 */
class EntityUpdateBuilder {

    private final Delegator delegator
    private final String entityName
    private Map<String, Object> whereFields
    private boolean ifExists = false
    private boolean first = false

    EntityUpdateBuilder(Delegator delegator, String entityName) {
        this.delegator = delegator
        this.entityName = entityName
    }

    EntityUpdateBuilder where(Map<String, Object> whereFields) {
        this.whereFields = whereFields
        return this
    }

    /**
     * On no match, {@link #set(Map, boolean)} returns {@code null} instead of throwing
     * {@link ServiceErrorException} -- mirrors {@code queryOne()}'s own null-on-no-match
     * convention. For sites that intentionally no-op when the record doesn't exist.
     */
    @SuppressWarnings('ConfusingMethodName')
    EntityUpdateBuilder ifExists() {
        this.ifExists = true
        return this
    }

    /**
     * Uses {@code queryFirst()} instead of {@code queryOne()} for the internal lookup -- for sites
     * where more than one record can legitimately match {@code where()}. Unlike {@code queryOne()},
     * {@code queryFirst()} does not narrow {@code where()} to PK fields only, so {@link #set(Map,
     * boolean)} narrows {@code where()} to the entity's own field names itself before querying --
     * safe to call with either explicit field names or a raw service {@code parameters} map.
     */
    @SuppressWarnings('ConfusingMethodName')
    EntityUpdateBuilder first() {
        this.first = true
        return this
    }

    GenericValue set(Map<String, Object> fields, boolean setIfEmpty = true) throws ServiceErrorException {
        GenericValue existing
        if (first) {
            // queryOne() narrows where() to PK fields internally (searchPkOnly, the same mechanism
            // setPKFields() uses); queryFirst() does not, so a raw service parameters map (userLogin,
            // locale, timeZone, ...) would otherwise blow up building a condition on a column that
            // isn't one of this entity's own fields. setAllFields(fields, true, null, null) -- null
            // pks means walk every field, not just PK -- is the same "walk the entity's own fields,
            // pull matching values out of the map" primitive set() already uses below via
            // setNonPKFields().
            GenericValue entityWhereFields = delegator.makeValue(entityName)
            entityWhereFields.setAllFields(whereFields, true, null, null)
            existing = EntityQuery.use(delegator).from(entityName).where(entityWhereFields).queryFirst()
        } else {
            existing = EntityQuery.use(delegator).from(entityName).where(whereFields).queryOne()
        }
        if (existing == null) {
            if (ifExists) {
                return null
            }
            throw new ServiceErrorException("No ${entityName} found matching ${whereFields}" as String)
        }
        // Like setNonPKFields(), not a raw Map.putAll(): walks the entity's own non-PK fields and pulls
        // matching values out of the given map, silently ignoring anything else (e.g. userLogin, locale,
        // timeZone commonly present in a raw service parameters map). This is what makes it safe to call
        // as update(entity).where(cond).set(parameters) -- the primary use case this builder exists for.
        existing.setNonPKFields(fields, setIfEmpty)
        existing.store()
        return existing
    }

}
