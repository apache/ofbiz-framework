/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * 'License'); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
*/
package org.apache.ofbiz.accounting.period

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityConditionBuilder

/**
 * Find a CustomTimePeriod
 */
Map findCustomTimePeriods() {
    List customTimePeriodList = []
    if (parameters.organizationPartyId) {
        // walk up the tree and find all party groups that this is a member of, and include the periods for all of them
        Map serviceResult = run service: 'getParentOrganizations', with: parameters
        List parentOrganizationPartyIdList = serviceResult.parentOrganizationPartyIdList
        EntityCondition condition = new EntityConditionBuilder().AND {
            IN(organizationPartyId: parentOrganizationPartyIdList)
            LESS_THAN(fromDate: parameters.findDate)
            OR {
                GREATER_THAN_EQUAL_TO(thruDate: parameters.findDate)
                EQUALS(thruDate: null)
            }
            if (parameters.onlyIncludePeriodTypeIdList) {
                IN(periodTypeId: parameters.onlyIncludePeriodTypeIdList)
            }
        }
        customTimePeriodList.addAll(from('CustomTimePeriod')
                .where(condition)
                .cache()
                .queryList())
    }
    if (parameters.excludeNoOrganizationPeriods) {
        EntityCondition condition = new EntityConditionBuilder().AND {
            OR {
                EQUALS(organizationPartyId: null)
                EQUALS(organizationPartyId: '_NA_')

            }
            LESS_THAN(fromDate: parameters.findDate)
            OR {
                GREATER_THAN_EQUAL_TO(thruDate: parameters.findDate)
                EQUALS(thruDate: null)
            }
            if (parameters.onlyIncludePeriodTypeIdList) {
                IN(periodTypeId: parameters.onlyIncludePeriodTypeIdList)
            }
        }
        customTimePeriodList.addAll(from('CustomTimePeriod')
                .where(condition)
                .cache()
                .queryList())
    }
    return success([customTimePeriodList: customTimePeriodList])
}

/**
 * Return previous year with respect to the given year and if none found then return current year as previous.
 */
Map getPreviousTimePeriod() {
    GenericValue customTimePeriod = from('CustomTimePeriod').where(parameters).cache().queryOne()
    int periodNum = (customTimePeriod.periodNum ?: 0) - 1
    if (periodNum > -1) {
        GenericValue previousTimePeriod = from('CustomTimePeriod')
                .where(organizationPartyId: customTimePeriod.organizationPartyId,
                        periodTypeId: customTimePeriod.periodTypeId,
                        periodNum: periodNum)
                .cache()
                .queryFirst()
        return success([previousTimePeriod: previousTimePeriod])
    }
    return success()
}