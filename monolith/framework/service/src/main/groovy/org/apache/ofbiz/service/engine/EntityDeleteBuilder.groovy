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
import org.apache.ofbiz.entity.GenericEntityException
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceErrorException

/**
 * Fluent write-side helper backing {@link GroovyBaseScript#delete(String)}.
 *
 * Usage: {@code delete('SomeEntity').where(fields)} — bulk-removes every record matching the given
 * fields and returns the number of rows removed. The given fields are not required to be the full
 * primary key: deleting by a real, non-PK field (e.g. a foreign key shared by several child rows) is
 * a legitimate, commonly-used bulk-delete pattern. {@code where()} narrows the given fields down to
 * the entity's own field names itself (like {@code set()} does via {@code setNonPKFields()}), so
 * it's safe to pass a raw service {@code parameters} map. Throws {@link ServiceErrorException} if
 * none of the given fields are real fields on the entity at all, rather than silently building a
 * condition-less delete that would remove every record in the entity.
 */
class EntityDeleteBuilder {

    private final Delegator delegator
    private final String entityName

    EntityDeleteBuilder(Delegator delegator, String entityName) {
        this.delegator = delegator
        this.entityName = entityName
    }

    int where(Map<String, Object> fields) throws GenericEntityException, ServiceErrorException {
        GenericValue entityFields = delegator.makeValue(entityName)
        entityFields.setAllFields(fields, true, null, null)
        if (entityFields.isEmpty()) {
            throw new ServiceErrorException(
                    "Cannot delete ${entityName}: given fields include none of its own fields" as String)
        }
        return delegator.removeByAnd(entityName, entityFields)
    }

}
