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
import org.ofbiz.entity.Delegator;
import org.ofbiz.security.Security;
import org.ofbiz.entity.jdbc.DatabaseUtil;
import org.ofbiz.entity.model.ModelEntity;

controlPath = parameters._CONTROL_PATH_;

if (security.hasPermission("ENTITY_MAINT", session)) {
    addMissing = "true".equals(parameters.addMissing);
    checkFkIdx = "true".equals(parameters.checkFkIdx);
    checkFks = "true".equals(parameters.checkFks);
    checkPks = "true".equals(parameters.checkPks);
    repair = "true".equals(parameters.repair);
    option = parameters.option;
    groupName = parameters.groupName;
    entityName = parameters.entityName;

    if (groupName) {
        helperInfo = delegator.getGroupHelperInfo(groupName);

        messages = [];
        //helper = GenericHelperFactory.getHelper(helperName);
        dbUtil = new DatabaseUtil(helperInfo);
        modelEntities = delegator.getModelEntityMapByGroup(groupName);
        modelEntityNames = new TreeSet(modelEntities.keySet());

        if ("checkupdatetables".equals(option)) {
            fieldsToRepair = null;
            if (repair) {
                fieldsToRepair = [];
            }
            dbUtil.checkDb(modelEntities, fieldsToRepair, messages, checkPks, checkFks, checkFkIdx, addMissing);
            if (fieldsToRepair) {
                dbUtil.repairColumnSizeChanges(modelEntities, fieldsToRepair, messages);
            }
        } else if ("removetables".equals(option)) {
            modelEntityNames.each { modelEntityName ->
                modelEntity = modelEntities[modelEntityName];
                dbUtil.deleteTable(modelEntity, messages);
            }
        } else if ("removetable".equals(option)) {
            modelEntity = modelEntities[entityName];
            dbUtil.deleteTable(modelEntity, messages);
        } else if ("removepks".equals(option)) {
            modelEntityNames.each { modelEntityName ->
                modelEntity = modelEntities[modelEntityName];
                dbUtil.deletePrimaryKey(modelEntity, messages);
            }
        } else if ("removepk".equals(option)) {
            modelEntity = modelEntities[entityName];
            dbUtil.deletePrimaryKey(modelEntity, messages);
        } else if ("createpks".equals(option)) {
            modelEntityNames.each { modelEntityName ->
                modelEntity = modelEntities[modelEntityName];
                dbUtil.createPrimaryKey(modelEntity, messages);
            }
        } else if ("createpk".equals(option)) {
            modelEntity = modelEntities[entityName];
            dbUtil.createPrimaryKey(modelEntity, messages);
        } else if ("createfkidxs".equals(option)) {
            modelEntityNames.each { modelEntityName ->
                modelEntity = modelEntities[modelEntityName];
                dbUtil.createForeignKeyIndices(modelEntity, messages);
            }
        } else if ("removefkidxs".equals(option)) {
            modelEntityNames.each { modelEntityName ->
                modelEntity = modelEntities[modelEntityName];
                dbUtil.deleteForeignKeyIndices(modelEntity, messages);
            }
        } else if ("createfks".equals(option)) {
            modelEntityNames.each { modelEntityName ->
                modelEntity = modelEntities[modelEntityName];
                dbUtil.createForeignKeys(modelEntity, modelEntities, messages);
            }
        } else if ("removefks".equals(option)) {
            modelEntityNames.each { modelEntityName ->
                modelEntity = modelEntities[modelEntityName];
                dbUtil.deleteForeignKeys(modelEntity, modelEntities, messages);
            }
        } else if ("createidx".equals(option)) {
            modelEntityNames.each { modelEntityName ->
                modelEntity = modelEntities[modelEntityName];
                dbUtil.createDeclaredIndices(modelEntity, messages);
            }
        } else if ("removeidx".equals(option)) {
            modelEntityNames.each { modelEntityName ->
                modelEntity = modelEntities[modelEntityName];
                dbUtil.deleteDeclaredIndices(modelEntity, messages);
            }
        } else if ("updateCharsetCollate".equals(option)) {
            modelEntityNames.each { modelEntityName ->
                modelEntity = modelEntities[modelEntityName];
                dbUtil.updateCharacterSetAndCollation(modelEntity, messages);
            }
        }
        miter = messages.iterator();
        context.miters = miter;
    }
    context.encodeURLCheckDb = response.encodeURL(controlPath + "/view/checkdb");
    context.groupName = groupName ?: "org.ofbiz";
    context.entityName = entityName ?: "";
}
