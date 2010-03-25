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
import javolution.util.FastList;

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.model.ModelGroupReader;
import org.ofbiz.entity.model.ModelReader;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelViewEntity;

mgr = delegator.getModelGroupReader();
entityGroups = mgr.getGroupNames(delegator.getDelegatorBaseName()).iterator();

filterByGroupName = parameters.filterByGroupName;
context.filterByGroupName = filterByGroupName;

filterByEntityName = parameters.filterByEntityName;
context.filterByEntityName = filterByEntityName;

reader = delegator.getModelReader();
entities = new TreeSet(reader.getEntityNames());

int colSize = entities.size()/3 + 1;
int kIdx = 0;
entitiesList = [];
entities.each { entityName ->
    entity = reader.getModelEntity(entityName);

    if (filterByGroupName && !filterByGroupName.equals(delegator.getEntityGroupName(entity.getEntityName()))) {
        return;
    }
    if (filterByEntityName && !((String)entity.getEntityName()).toUpperCase().contains(filterByEntityName.toUpperCase())) {
        return;
    }

    viewEntity = "N";
    if (entity instanceof ModelViewEntity) {
        viewEntity = "Y";
    }

    entityPermissionView = "N";
    if (security.hasEntityPermission("ENTITY_DATA", "_VIEW", session) || security.hasEntityPermission(entity.getPlainTableName(), "_VIEW", session)) {
        entityPermissionView = "Y";
    }

    entityPermissionCreate = "N";
    if (security.hasEntityPermission("ENTITY_DATA", "_CREATE", session) || security.hasEntityPermission(entity.getPlainTableName(), "_CREATE", session)) {
        entityPermissionCreate = "Y";
    }

    changeColumn = "N";
    kIdx++;
    if (kIdx >= colSize) {
        colSize += colSize;
        changeColumn = "Y";
    }

    entityMap = [:];
    entityMap.entityName = entity.getEntityName();
    entityMap.entityPermissionView = entityPermissionView;
    entityMap.entityPermissionCreate = entityPermissionCreate;
    entityMap.viewEntity = viewEntity;
    entityMap.changeColumn = changeColumn;

    entitiesList.add(entityMap);
}
context.entityGroups = entityGroups;
context.entitiesList = entitiesList;
