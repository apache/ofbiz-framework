/*******************************************************************************
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
 *******************************************************************************/

// productList.unique() like distinct sql command, return string type.
def productUniqueStr = productList.unique();
def productUniqueStrList = productUniqueStr.toList();
def googleBaseList = from("GoodIdentification").where("goodIdentificationTypeId", "GOOGLE_ID_" + productStore.defaultLocaleString).queryList();
// find product is existed in google base.
def notNeededList = productUniqueStrList - googleBaseList.productId;
def resultList = productUniqueStrList - notNeededList;
def productExportList = [];
// if feed more than 1000 always found an IO error, so should divide to any sections.
def amountPerSection = 1000;
def section = (int)(resultList.size()/amountPerSection);
if (resultList.size() % amountPerSection != 0) {
	section = section+1;
}

for (int i=0; i<section; i++) {
	if (!(i == (section-1))) {
		productExportList.add(resultList.subList((i*amountPerSection), ((i+1)*amountPerSection)));
	} else {
		productExportList.add(resultList.subList((i*amountPerSection), resultList.size()));
	}
}
context.productExportLists = productExportList
