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

if (message) {
    var successMessage = "Got message [" + message + "] and finished fine";
    var result = message;
} else {
    var successMessage = "Got no message but finished fine anyway";
    var result = "[no message received]";
}

if (ofbiz) {
    var partyValue = ofbiz.findOne("PartyNameView");
    if (partyValue) {
        var foundMessage = ofbiz.evalString(" Found Party ${partyValue.groupName}${partyValue.firstName} ${partyValue.lastName}");
        successMessage = successMessage + foundMessage;
        ofbiz.logInfo(successMessage);
    } else {
        ofbiz.logInfo("Party not found with partyId ${parameters.partyId}");
    }
}

function testFunction(context) {
    if (message) {
        var successMessage = "Got message [" + message + "] and finished fine";
        var result = message;
    } else {
        var successMessage = "Got no message but finished fine anyway";
        var result = "[no message received]";
    }
    return result;
}
