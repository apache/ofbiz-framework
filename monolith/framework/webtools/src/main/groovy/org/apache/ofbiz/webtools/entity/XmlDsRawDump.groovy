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
package org.apache.ofbiz.webtools.entity

import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.model.ModelViewEntity
import org.apache.ofbiz.entity.util.EntityQuery

if (!security.hasPermission('ENTITY_MAINT', session)) {
    response.contentType = 'text/plain; charset=UTF-8'
    response.writer.println('ERROR: You do not have permission to use this page (ENTITY_MAINT needed)')
    return 'success'
}

passedEntityNames = session.getAttribute('xmlrawdump_entitylist')
session.removeAttribute('xmlrawdump_entitylist')
EntityCondition entityDateCond = session.getAttribute('entityDateCond')
session.removeAttribute('entityDateCond')

if (!passedEntityNames) {
    response.contentType = 'text/plain; charset=UTF-8'
    response.writer.println('ERROR: No entityName list was found in the session, go back to the export page and try again.')
    return 'success'
}

reader = delegator.getModelReader()
response.contentType = 'text/xml; charset=UTF-8'
writer = response.writer
writer.println('<?xml version="1.0" encoding="UTF-8"?>')
writer.println('<entity-engine-xml>')

numberWritten = 0
try {
    passedEntityNames.each { curEntityName ->
        me = reader.getModelEntity(curEntityName)
        entityQuery = EntityQuery.use(delegator).from(curEntityName).cursorScrollInsensitive()
        if (!me.getNoAutoStamp() && !(me instanceof ModelViewEntity) && entityDateCond) {
            entityQuery.where(entityDateCond)
        }
        entityQuery.queryIterator().withCloseable { values ->
            while ((value = values.next()) != null) {
                value.writeXmlText(writer, '')
                numberWritten++
            }
        }
    }
} catch (Exception e) {
    logError(e, 'Failure in raw XML data source dump')
    throw e
}

writer.println('</entity-engine-xml>')
logInfo("Total records written from all entities: $numberWritten")
return 'success'
