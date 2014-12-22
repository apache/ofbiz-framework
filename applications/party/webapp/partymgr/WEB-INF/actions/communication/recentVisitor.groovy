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

import org.ofbiz.entity.condition.*;
import org.ofbiz.entity.util.*
import org.ofbiz.base.util.*;

lastDate = UtilDateTime.addDaysToTimestamp(UtilDateTime.nowTimestamp(), -21); // should be there the last 3 weeks.
visits = select('partyId')
             .from('Visit')
             .where(EntityCondition.makeCondition("fromDate", EntityOperator.GREATER_THAN, lastDate))
             .distinct()
             .queryList()
partyIds = EntityUtil.getFieldListFromEntityList(visits, 'partyId', false)
context.recentParties = select("partyId", "firstName", "middleName", "lastName", "groupName")
                            .from("PartyNameView")
                            .where(EntityCondition.makeCondition('partyId', EntityOperator.IN, partyIds))
                            .distinct()
                            .queryList();
