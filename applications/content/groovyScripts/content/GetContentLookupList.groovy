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

import org.apache.ofbiz.entity.GenericEntityException
import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilFormatOut
import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.entity.transaction.TransactionUtil
import org.apache.ofbiz.entity.GenericEntity
import org.apache.ofbiz.entity.model.ModelField
import org.apache.ofbiz.entity.model.ModelEntity
import org.apache.ofbiz.entity.model.ModelReader

module = "GetContentLookupList.groovy"

viewIndex = parameters.VIEW_INDEX ? Integer.valueOf(parameters.VIEW_INDEX) : 0
viewSize = parameters.VIEW_SIZE ? Integer.valueOf(parameters.VIEW_SIZE) : 20

int lowIndex = viewIndex*viewSize+1
int highIndex = (viewIndex+1)*viewSize

context.viewIndexFirst = 0
context.viewIndex = viewIndex
context.viewIndexPrevious = viewIndex-1
context.viewIndexNext = viewIndex+1
String curFindString=""

ModelReader reader = delegator.getModelReader()
ModelEntity modelEntity = reader.getModelEntity("ContentAssocViewTo")
GenericEntity findByEntity = delegator.makeValue("ContentAssocViewTo")
List errMsgList = new ArrayList()
Iterator fieldIterator = modelEntity.getFieldsIterator()
while (fieldIterator.hasNext()) {
    ModelField field = fieldIterator.next()
    String fval = parameters.get(field.getName())
    if (fval != null) {
        if (fval.length() > 0) {
            curFindString = curFindString + "&" + field.getName() + "=" + fval
            try {
                findByEntity.setString(field.getName(), fval)
            } catch (NumberFormatException nfe) {
                Debug.logError(nfe, "Caught an exception : " + nfe.toString(), module)
                errMsgList.add("Entered value is non-numeric for numeric field: " + field.getName())
            }
        }
    }
}
if (errMsgList) {
    request.setAttribute("_ERROR_MESSAGE_LIST_", errMsgList)
}

curFindString = UtilFormatOut.encodeQuery(curFindString)
context.curFindString = curFindString
context.viewSize = viewSize
context.lowIndex = lowIndex
int arraySize = 0
List resultPartialList

if ((highIndex - lowIndex + 1) > 0) {
    // get the results as an entity list iterator
    boolean beganTransaction = false
    try {
        beganTransaction = TransactionUtil.begin()
        listIt = from("ContentAssocViewTo").where("contentIdStart", (String)parameters.get("contentId")).orderBy("contentId ASC").cursorScrollInsensitive().cache(true).queryIterator()
        resultPartialList = listIt.getPartialList(lowIndex, highIndex - lowIndex + 1)
        
        arraySize = listIt.getResultsSizeAfterPartialList()
        if (arraySize < highIndex) {
            highIndex = arraySize
        }
    } catch (GenericEntityException e) {
        Debug.logError(e, "Failure in operation, rolling back transaction", module)
        try {
            // only rollback the transaction if we started one...
            TransactionUtil.rollback(beganTransaction, "Error looking up entity values in WebTools Entity Data Maintenance", e)
        } catch (GenericEntityException e2) {
            Debug.logError(e2, "Could not rollback transaction: " + e2.toString(), module)
        }
        // after rolling back, rethrow the exception
        throw e
    } finally {
        listIt.close()
        // only commit the transaction if we started one... this will throw an exception if it fails
        TransactionUtil.commit(beganTransaction)
    }
}
context.highIndex = highIndex
context.arraySize = arraySize
context.resultPartialList = resultPartialList

viewIndexLast = UtilMisc.getViewLastIndex(arraySize, viewSize)
context.viewIndexLast = viewIndexLast
context.contentAssoc=resultPartialList
