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

def module = "AcctgTransEntriesSearchResultsParameters.groovy";

try {
    def birtParameters = [:];
    birtParameters.organizationPartyId = parameters.organizationPartyId;
    birtParameters.productId = parameters.productId;
    birtParameters.isPosted = parameters.isPosted;
    birtParameters.invoiceId = parameters.invoiceId;
    birtParameters.acctgTransId = parameters.acctgTransId;
    birtParameters.glFiscalTypeId = parameters.glFiscalTypeId;
    birtParameters.glAccountId = parameters.glAccountId;
    birtParameters.shipmentId = parameters.shipmentId;
    birtParameters.acctgTransTypeId = parameters.acctgTransTypeId;
    birtParameters.workEffortId = parameters.workEffortId;
    birtParameters.glJournalId = parameters.glJournalId;
    birtParameters.partyId = parameters.partyId;
    birtParameters.paymentId = parameters.paymentId;
    if (parameters.fromDate) {
        birtParameters.fromDate = Timestamp.valueOf(parameters.fromDate);
    }
    if (parameters.thruDate) {
        birtParameters.thruDate = Timestamp.valueOf(parameters.thruDate);
    }
    birtParameters.userLoginId = userLogin.userLoginId;
    request.setAttribute("birtParameters", birtParameters);
} catch (e) {
    Debug.logError(e, module);
}
return "success";