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

partyId = parameters.partyId
visitId = parameters.visitId

visit = null
serverHits = null
if (visitId) {
    visit = from("Visit").where("visitId", visitId).queryOne()
    if (visit) {
        serverHits = from("ServerHit").where("visitId", visitId).orderBy("-hitStartDateTime").queryList()
    }
}

viewIndex = 0
try {
    viewIndex = Integer.valueOf((String) parameters.VIEW_INDEX).intValue()
} catch (Exception e) {
    viewIndex = 0
}

viewSize = 20
try {
    viewSize = Integer.valueOf((String) parameters.VIEW_SIZE).intValue()
} catch (Exception e) {
    viewSize = 20
}

listSize = 0
if (serverHits) {
    listSize = serverHits.size()
}
lowIndex = viewIndex * viewSize
highIndex = (viewIndex + 1) * viewSize
if (listSize < highIndex) {
    highIndex = listSize
}

context.partyId = partyId
context.visitId = visitId
context.visit = visit
context.serverHits = serverHits

context.viewIndex = viewIndex
context.viewSize = viewSize
context.listSize = listSize
context.lowIndex = lowIndex
context.highIndex = highIndex
