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
 */
class EntityUpdateBuilder {

    private final Delegator delegator
    private final String entityName
    private Map<String, Object> whereFields

    EntityUpdateBuilder(Delegator delegator, String entityName) {
        this.delegator = delegator
        this.entityName = entityName
    }

    EntityUpdateBuilder where(Map<String, Object> whereFields) {
        this.whereFields = whereFields
        return this
    }

    GenericValue set(Map<String, Object> fields) throws ServiceErrorException {
        GenericValue existing = EntityQuery.use(delegator).from(entityName).where(whereFields).queryOne()
        if (existing == null) {
            throw new ServiceErrorException("No ${entityName} found matching ${whereFields}" as String)
        }
        // Like setNonPKFields(), not a raw Map.putAll(): walks the entity's own non-PK fields and pulls
        // matching values out of the given map, silently ignoring anything else (e.g. userLogin, locale,
        // timeZone commonly present in a raw service parameters map). This is what makes it safe to call
        // as update(entity).where(cond).set(parameters) -- the primary use case this builder exists for.
        existing.setNonPKFields(fields)
        existing.store()
        return existing
    }

}
