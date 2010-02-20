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
import org.ofbiz.entity.condition.EntityCondition
import org.ofbiz.entity.condition.EntityOperator

def buildCondition
def getValue = { item ->
    if (item instanceof Map) return buildCondition(item)
    return item
}
buildCondition = { item ->
    switch (item.name) {
        case "EntityConditionList":
            def conditions = []
            for (conditionDef in item.list) {
                conditions.add(buildCondition(conditionDef))
            }
            return EntityCondition.makeCondition(conditions, EntityOperator.lookup(item.operator))
        case "EntityExpr":
            return EntityCondition.makeCondition(getValue(item.left), EntityOperator.lookup(item.operator), getValue(item.right))
        case "Include":
            return webslinger.event(item.path)
        default:
            throw new InternalError(item.toString())
    }
}

return buildCondition(webslinger.payload)
