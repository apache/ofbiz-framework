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

import org.apache.ofbiz.entity.jdbc.DatabaseUtil

controlPath = parameters._CONTROL_PATH_

if (security.hasPermission('ENTITY_MAINT', session)) {
    addMissing = parameters.addMissing == 'true'
    checkFkIdx = parameters.checkFkIdx == 'true'
    checkFks = parameters.checkFks == 'true'
    checkPks = parameters.checkPks == 'true'
    repair = parameters.repair == 'true'
    option = parameters.option
    groupName = parameters.groupName
    entityName = parameters.entityName

    if (groupName) {
        helperInfo = delegator.getGroupHelperInfo(groupName)

        messages = []
        //helper = GenericHelperFactory.getHelper(helperName)
        dbUtil = new DatabaseUtil(helperInfo)
        modelEntities = delegator.getModelEntityMapByGroup(groupName)
        modelEntityNames = new TreeSet(modelEntities.keySet())

        switch (option) {
            case 'checkupdatetables':
                fieldsToRepair = null
                if (repair) {
                    fieldsToRepair = []
                }
                dbUtil.checkDb(modelEntities, fieldsToRepair, messages, checkPks, checkFks, checkFkIdx, addMissing)
                if (fieldsToRepair) {
                    dbUtil.repairColumnSizeChanges(modelEntities, fieldsToRepair, messages)
                }
                break
            case 'removetables':
                modelEntityNames.each { modelEntityName ->
                    modelEntity = modelEntities[modelEntityName]
                    dbUtil.deleteTable(modelEntity, messages)
                }
                break
            case 'removetable':
                modelEntity = modelEntities[entityName]
                dbUtil.deleteTable(modelEntity, messages)
                break
            case 'removepks':
                modelEntityNames.each { modelEntityName ->
                    modelEntity = modelEntities[modelEntityName]
                    dbUtil.deletePrimaryKey(modelEntity, messages)
                }
                break
            case 'removepk':
                modelEntity = modelEntities[entityName]
                dbUtil.deletePrimaryKey(modelEntity, messages)
                break
            case 'createpks':
                modelEntityNames.each { modelEntityName ->
                    modelEntity = modelEntities[modelEntityName]
                    dbUtil.createPrimaryKey(modelEntity, messages)
                }
                break
            case 'createpk':
                modelEntity = modelEntities[entityName]
                dbUtil.createPrimaryKey(modelEntity, messages)
                break
            case 'createfkidxs':
                modelEntityNames.each { modelEntityName ->
                    modelEntity = modelEntities[modelEntityName]
                    dbUtil.createForeignKeyIndices(modelEntity, messages)
                }
                break
            case 'removefkidxs':
                modelEntityNames.each { modelEntityName ->
                    modelEntity = modelEntities[modelEntityName]
                    dbUtil.deleteForeignKeyIndices(modelEntity, messages)
                }
                break
            case 'createfks':
                modelEntityNames.each { modelEntityName ->
                    modelEntity = modelEntities[modelEntityName]
                    dbUtil.createForeignKeys(modelEntity, modelEntities, messages)
                }
                break
            case 'removefks':
                modelEntityNames.each { modelEntityName ->
                    modelEntity = modelEntities[modelEntityName]
                    dbUtil.deleteForeignKeys(modelEntity, modelEntities, messages)
                }
                break
            case 'createidx':
                modelEntityNames.each { modelEntityName ->
                    modelEntity = modelEntities[modelEntityName]
                    dbUtil.createDeclaredIndices(modelEntity, messages)
                }
                break
            case 'removeidx':
                modelEntityNames.each { modelEntityName ->
                    modelEntity = modelEntities[modelEntityName]
                    dbUtil.deleteDeclaredIndices(modelEntity, messages)
                }
                break
            case 'updateCharsetCollate':
                modelEntityNames.each { modelEntityName ->
                    modelEntity = modelEntities[modelEntityName]
                    dbUtil.updateCharacterSetAndCollation(modelEntity, messages)
                }
                break
        }
        miter = messages.iterator()
        context.miters = miter
    }
    context.checkDbURL = 'view/checkdb'
    context.groupName = groupName ?: 'org.apache.ofbiz'
    context.entityName = entityName ?: ''
}
