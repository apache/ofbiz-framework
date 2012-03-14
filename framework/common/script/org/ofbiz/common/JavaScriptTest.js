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
    var exampleValue = ofbiz.findOne("Example");
    if (exampleValue) {
        var foundMessage = ofbiz.evalString(" Found Example ${exampleValue.exampleName}");
        successMessage = successMessage + foundMessage;
        ofbiz.logInfo(successMessage);
    } else {
        var notFoundMessage = ofbiz.evalString(" Example not found with exampleId ${parameters.exampleId}");
        successMessage = successMessage + notFoundMessage;
        ofbiz.logInfo(successMessage);
    }
}

function testFunction(context) {
    var messageArg = context.get("message");
    if (messageArg) {
        context.put("successMessage", "Function 'testFunction' got message [" + messageArg + "] and finished fine");
        var functionResult = "testFunction: " + messageArg;
    } else {
        context.put("successMessage", "Function 'testFunction' got no message but finished fine anyway");
        var functionResult = "testFunction: no message received";
    }
    // The function's result must be set explicitly in the context Map
    context.put("result", functionResult);
}
