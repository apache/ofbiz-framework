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
import org.apache.ofbiz.entity.Delegator
import org.apache.ofbiz.security.Security
import org.apache.ofbiz.entity.model.ModelReader
import org.apache.ofbiz.entity.model.ModelEntity

controlPath = parameters._CONTROL_PATH_
context.controlPath = controlPath

if (security.hasPermission("ENTITY_MAINT", session)) {
    forstatic = "true".equals(parameters.forstatic)
    context.forstatic = forstatic

    reader = delegator.getModelReader()
    ec = reader.getEntityNames()
    entities = new TreeSet(ec)
    search = parameters.search

    packageNames = new TreeSet()
    ec.each { eName ->
        ent = reader.getModelEntity(eName)
        packageNames.add(ent.getPackageName())
    }
    context.packageNames = packageNames

    entitiesList = []
    entities.each { entityName ->
        entityMap = [:]
        if (!search || entityName.toLowerCase().indexOf(search.toLowerCase()) != -1 ) {
            url = search ? "?search=$search" : ""
            entityMap.url = url
        }
        entityMap.entityName = entityName

        entitiesList.add(entityMap)
    }
    context.entitiesList = entitiesList
}
