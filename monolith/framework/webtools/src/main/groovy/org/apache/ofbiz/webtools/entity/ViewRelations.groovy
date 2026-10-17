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

entityName = parameters.entityName
context.entityName = entityName

reader = delegator.getModelReader()
modelEntity = reader.getModelEntity(entityName)

context.plainTableName = modelEntity.getPlainTableName()

boolean hasEntityDataViewPermision = security.hasEntityPermission('ENTITY_DATA', '_VIEW', session)
hasViewPermission = hasEntityDataViewPermision || security.hasEntityPermission(modelEntity.getPlainTableName(), '_VIEW', session)
context.hasViewPermission = hasViewPermission

relations = []
for (rit = modelEntity.getRelationsIterator(); rit.hasNext();) {
    mapRelation = [:]

    modelRelation = rit.next()
    relFields = []
    modelRelation.getKeyMaps().each { keyMap ->
        relFields << [fieldName: keyMap.getFieldName(),
                      relFieldName: keyMap.getRelFieldName()]
    }

    mapRelation.relFields = relFields
    mapRelation.title = modelRelation.getTitle()
    mapRelation.relEntityName = modelRelation.getRelEntityName()
    mapRelation.type = modelRelation.getType()
    mapRelation.fkName = modelRelation.getFkName()

    relations.add(mapRelation)
}
context.relations = relations
