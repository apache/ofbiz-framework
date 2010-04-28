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

package org.ofbiz.workeffort.workeffort;

import java.util.List;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;


/** WorkEffortWorker - Work Effort worker class. */
public class WorkEffortWorker {

    public static final String module = WorkEffortWorker.class.getName();

    public static List<GenericValue> getLowestLevelWorkEfforts(Delegator delegator, String workEffortId, String workEffortAssocTypeId) {
        return getLowestLevelWorkEfforts(delegator, workEffortId, workEffortAssocTypeId, "workEffortIdFrom", "workEffortIdTo");
    }

    public static List<GenericValue> getLowestLevelWorkEfforts(Delegator delegator, String workEffortId, String workEffortAssocTypeId, String left, String right) {
        if (left == null) {
            left = "workEffortIdFrom";
        }
        if (right == null) {
            right = "workEffortIdTo";
        }

        List<GenericValue> workEfforts = FastList.newInstance();
        try {
            EntityConditionList<EntityExpr> exprsLevelFirst = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition(left, workEffortId),
                    EntityCondition.makeCondition("workEffortAssocTypeId", workEffortAssocTypeId)), EntityOperator.AND);
            List<GenericValue> childWEAssocsLevelFirst = delegator.findList("WorkEffortAssoc", exprsLevelFirst, null, null, null, true);
            for (GenericValue childWEAssocLevelFirst : childWEAssocsLevelFirst) {
                EntityConditionList<EntityExpr> exprsLevelNext = EntityCondition.makeCondition(UtilMisc.toList(
                        EntityCondition.makeCondition(left, childWEAssocLevelFirst.get(right)),
                        EntityCondition.makeCondition("workEffortAssocTypeId", workEffortAssocTypeId)), EntityOperator.AND);
                List<GenericValue> childWEAssocsLevelNext = delegator.findList("WorkEffortAssoc", exprsLevelNext, null, null, null, true);
                while (UtilValidate.isNotEmpty(childWEAssocsLevelNext)) {
                    List<GenericValue> tempWorkEffortList = FastList.newInstance();
                    for (GenericValue childWEAssocLevelNext : childWEAssocsLevelNext) {
                        EntityConditionList<EntityExpr> exprsLevelNth = EntityCondition.makeCondition(UtilMisc.toList(
                                EntityCondition.makeCondition(left, childWEAssocLevelNext.get(right)),
                                EntityCondition.makeCondition("workEffortAssocTypeId", workEffortAssocTypeId)), EntityOperator.AND);
                        List<GenericValue> childWEAssocsLevelNth = delegator.findList("WorkEffortAssoc", exprsLevelNth, null, null, null, true);
                        if (UtilValidate.isNotEmpty(childWEAssocsLevelNth)) {
                            tempWorkEffortList.addAll(childWEAssocsLevelNth);
                        }
                        workEfforts.add(childWEAssocLevelNext);
                    }
                    childWEAssocsLevelNext = tempWorkEffortList;
                }
                workEfforts.add(childWEAssocLevelFirst);
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, module);
        }
        return workEfforts;
    }

    public static List<GenericValue> removeDuplicateWorkEfforts(List<GenericValue> workEfforts) {
        Set<String> keys = FastSet.newInstance();
        Set<GenericValue> exclusions = FastSet.newInstance();
        for (GenericValue workEffort : workEfforts) {
            String workEffortId = workEffort.getString("workEffortId");
            if (keys.contains(workEffortId)) {
                exclusions.add(workEffort);
            } else {
                keys.add(workEffortId);
            }
        }
        workEfforts.removeAll(exclusions);
        return workEfforts;
    }
}
