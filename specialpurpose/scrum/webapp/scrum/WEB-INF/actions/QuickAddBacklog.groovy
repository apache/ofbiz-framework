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

import org.ofbiz.base.util.Debug;

try{
    // for sprint dropdown
    workEffortList = [];
    sprintList = from("WorkEffort").where("workEffortTypeId", "SCRUM_SPRINT","currentStatusId", "SPRINT_ACTIVE").queryList();
    if (sprintList) {
        sprintList.each{ sprintMap ->
            workEffortMap = [:];
            workEffortParentId = sprintMap.workEffortParentId;
            if (workEffortParentId) {
               projectList = from("WorkEffortAndProduct").where("workEffortId", workEffortParentId).queryList();
               projectMap = projectList[0];
               // make sure that project dose not closed
               if (projectMap.currentStatusId != "SPJ_CLOSED") {
                   productMap = from("Product").where("productId", projectMap.productId).queryOne();
                   workEffortMap.productId = productMap.productId;
                   workEffortMap.internalName = returnNameAsString(productMap.internalName,30);
                   workEffortMap.projectId = projectMap.workEffortId;
                   workEffortMap.projectName = returnNameAsString(projectMap.workEffortName,30);
                   workEffortMap.sprintId = sprintMap.workEffortId;
                   workEffortMap.sprintName = returnNameAsString(sprintMap.workEffortName,30);
                   workEffortMap.keyId = productMap.productId+","+projectMap.workEffortId+","+sprintMap.workEffortId;
                   workEffortList.add(workEffortMap);
               }
            }
        }
        context.workEffortList = workEffortList;
        }

    // for backlog category
    productId = null;
    if (parameters.productId) {
        productId = parameters.productId;
    } else {
        if (parameters.keyId) {
            indexList = parameters.keyId.tokenize(",");
            productId = indexList[0].toString().trim();
        }
    }
    categoryList = [];
    if (productId) {
        sprintList = from("CustRequestAndCustRequestItem").where("custRequestTypeId", "RF_PARENT_BACKLOG","productId", productId).queryList();
    } else {
        sprintList = from("CustRequestAndCustRequestItem").where("custRequestTypeId", "RF_PARENT_BACKLOG").queryList();
    }
    if (sprintList) {
        sprintList.each{ categoryMap ->
            inputMap = [:];
            productIdIn = categoryMap.productId;
            if (productIdIn) {
               productMap = from("Product").where("productId", productIdIn).queryOne();
               inputMap.productId = productMap.productId;
               inputMap.internalName = productMap.internalName;
               inputMap.custRequestId = categoryMap.custRequestId;
               inputMap.custRequestName = categoryMap.custRequestName;
               categoryList.add(inputMap);
            }
        }
        context.categoryList = categoryList;
    }
}catch(e){
    Debug.logInfo("catch e ================" + e,"");
}

//subString function
def String returnNameAsString(input,length) {
 if (input.length() > length ) {
     ansValue = input.toString().substring(0,Math.min(input.toString().length(),length));
     return ansValue;
 } else {
     return input;
     }
}
