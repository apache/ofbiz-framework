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




import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.util.EntityUtilProperties

if (context.noConditionFind == null) {
    context.noConditionFind = parameters.noConditionFind
}
if (context.noConditionFind == null) {
    context.noConditionFind = EntityUtilProperties.getPropertyValue("widget", "widget.defaultNoConditionFind", delegator)
}
if (context.filterByDate == null) {
    context.filterByDate = parameters.filterByDate
}
prepareResult = runService('prepareFind', [entityName : context.entityName,
                                           orderBy : context.orderBy,
                                           inputFields : parameters,
                                           filterByDate : context.filterByDate,
                                           filterByDateValue : context.filterByDateValue,
                                           userLogin : context.userLogin])

exprList = [EntityCondition.makeCondition("statusId", null),
            EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PARTY_DISABLED")]
statusPartyDisable = EntityCondition.makeCondition(exprList, EntityOperator.OR)
entityConditionList = null
if (prepareResult.entityConditionList != null) {
    ConditionList = [prepareResult.entityConditionList, statusPartyDisable]
    entityConditionList = EntityCondition.makeCondition(ConditionList)
} else if (context.noConditionFind == "Y") {
    entityConditionList = statusPartyDisable
}

executeResult = runService('executeFind', [entityName : context.entityName,
                                           orderByList : prepareResult.orderByList,
                                           entityConditionList : entityConditionList,
                                           noConditionFind : context.noConditionFind])
if (executeResult.listIt == null) {
    Debug.logWarning("No list found for query string + [" + prepareResult.queryString + "]", "FindLookUp.groovy")
}
context.listIt = executeResult.listIt
context.queryString = prepareResult.queryString
context.queryStringMap = prepareResult.queryStringMap
