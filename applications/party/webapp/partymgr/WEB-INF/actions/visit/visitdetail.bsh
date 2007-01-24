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

import org.ofbiz.base.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.entity.util.*;
import org.ofbiz.entity.condition.*;

partyId = parameters.get("partyId");
visitId = parameters.get("visitId");

visit = null;
serverHits = null;
if (visitId != null) {
    visit = delegator.findByPrimaryKey("Visit", UtilMisc.toMap("visitId", visitId));
    if (visit != null) {
        serverHits = delegator.findByAnd("ServerHit", UtilMisc.toMap("visitId", visitId), UtilMisc.toList("-hitStartDateTime"));
    }
}

viewIndex = 0;
try {
    viewIndex = Integer.valueOf((String) parameters.get("VIEW_INDEX")).intValue();
} catch (Exception e) {
    viewIndex = 0;
}

viewSize = 20;
try {
    viewSize = Integer.valueOf((String) parameters.get("VIEW_SIZE")).intValue();
} catch (Exception e) {
    viewSize = 20;
}

listSize = 0;
if (serverHits != null) {
    listSize = serverHits.size();
}
lowIndex = viewIndex * viewSize;
highIndex = (viewIndex + 1) * viewSize;
if (listSize < highIndex) {
    highIndex = listSize;
}

context.put("partyId", partyId);
context.put("visitId", visitId);
context.put("visit", visit);
context.put("serverHits", serverHits);

context.put("viewIndex", viewIndex);
context.put("viewSize", viewSize);
context.put("listSize", listSize);
context.put("lowIndex", lowIndex);
context.put("highIndex", highIndex);
