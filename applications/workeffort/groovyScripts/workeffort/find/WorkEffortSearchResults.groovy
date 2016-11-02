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

import org.apache.ofbiz.workeffort.workeffort.WorkEffortSearchEvents
import org.apache.ofbiz.workeffort.workeffort.WorkEffortSearchSession

// note: this can be run multiple times in the same request without causing problems, will check to see on its own if it has run again
WorkEffortSearchSession.processSearchParameters(parameters, request)
Map result = WorkEffortSearchEvents.getWorkEffortSearchResult(request, delegator)

context.put("workEffortIds", result.get("workEffortIds"))
context.put("viewIndex", result.get("viewIndex"))
context.put("viewSize", result.get("viewSize"))
context.put("listSize", result.get("listSize"))
context.put("lowIndex", result.get("lowIndex"))
context.put("highIndex", result.get("highIndex"))
context.put("searchConstraintStrings", result.get("searchConstraintStrings"))
context.put("searchSortOrderString", result.get("searchSortOrderString"))
