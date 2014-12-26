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

import org.ofbiz.entity.*
import org.ofbiz.base.util.*

allSequenceNums = from("ProductContent").where("productId", parameters.productId, "productContentTypeId", "IMAGE").queryList();
nullSequenceNums = from("ProductContent").where("productId", parameters.productId, "productContentTypeId", "IMAGE", "sequenceNum", null).queryList();
productContents = allSequenceNums - nullSequenceNums
def duplicate = 0
if(parameters.sequenceNum){
    findExisted = from("ProductContent").where("productId", parameters.productId, "productContentTypeId", "IMAGE", "sequenceNum", (Long)parameters.sequenceNum).queryList();
    duplicate = findExisted.size()
}
if(duplicate > 1){
    context.parameters.sequenceNum = (Long)productContents.sequenceNum[productContents.size()-1] + 1
}
