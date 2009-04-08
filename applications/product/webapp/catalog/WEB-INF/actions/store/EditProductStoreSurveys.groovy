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

import org.ofbiz.base.util.*
import org.ofbiz.entity.condition.*
import org.ofbiz.entity.util.*

context.hasPermission = security.hasEntityPermission("CATALOG", "_VIEW", session);

productStoreId = request.getParameter("productStoreId");
if (productStoreId) {
    productStore = delegator.findOne("ProductStore", [productStoreId : productStoreId], false);
    context.productStoreId = productStoreId;
    context.productStore = productStore;
}

context.productStoreSurveys = EntityUtil.filterByDate(delegator.findList("ProductStoreSurveyAppl", EntityCondition.makeCondition([productStoreId : productStoreId]), null, null, null, false));

context.surveys = delegator.findList("Survey", null, null, ['description'], null, false);

context.surveyApplTypes = delegator.findList("SurveyApplType", null, null, ['description'], null, false);

context.productCategories = delegator.findList("ProductCategory", null, null, ['description'], null, false);

context.nowTimestampString = UtilDateTime.nowTimestamp().toString();