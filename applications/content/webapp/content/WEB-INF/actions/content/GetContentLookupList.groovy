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

 import org.ofbiz.entity.condition.*;
 import org.ofbiz.entity.util.*;
 import org.ofbiz.entity.*;
 import org.ofbiz.base.util.*;
 import javolution.util.FastList;
 import javolution.util.FastSet;
 import javolution.util.FastMap;
 import org.ofbiz.entity.transaction.TransactionUtil;
 import org.ofbiz.entity.util.EntityListIterator;
 import org.ofbiz.entity.GenericEntity;
 import org.ofbiz.entity.model.ModelField;
 import org.ofbiz.base.util.UtilValidate;
 import org.ofbiz.entity.model.ModelEntity;
 import org.ofbiz.entity.model.ModelReader;

try {
    viewIndex = Integer.valueOf((String)parameters.get("VIEW_INDEX")).intValue();
} catch (NumberFormatException nfe) {
        viewIndex = 0;
}

context.viewIndexFirst = 0;
context.viewIndex = viewIndex;
context.viewIndexPrevious = viewIndex-1;
context.viewIndexNext = viewIndex+1;
String curFindString="";

ModelReader reader = delegator.getModelReader();
ModelEntity modelEntity = reader.getModelEntity("ContentAssocViewTo");
GenericEntity findByEntity = delegator.makeValue("ContentAssocViewTo");
List errMsgList = FastList.newInstance();
Iterator fieldIterator = modelEntity.getFieldsIterator();
while (fieldIterator.hasNext()) {
    ModelField field = fieldIterator.next();
    String fval = parameters.get(field.getName());
    if (fval != null) {
        if (fval.length() > 0) {
            curFindString = curFindString + "&" + field.getName() + "=" + fval;
            try {
                findByEntity.setString(field.getName(), fval);
            } catch (NumberFormatException nfe) {
                Debug.logError(nfe, "Caught an exception : " + nfe.toString(), "GetContentLookupList.groovy");
                errMsgList.add("Entered value is non-numeric for numeric field: " + field.getName());
            }
        }
    }
}
if (errMsgList) {
    request.setAttribute("_ERROR_MESSAGE_LIST_", errMsgList);
}

curFindString = UtilFormatOut.encodeQuery(curFindString);
context.curFindString = curFindString;
try {
    viewSize = Integer.valueOf((String)parameters.get("VIEW_SIZE")).intValue();
} catch (NumberFormatException nfe) {
    
}

context.viewSize = viewSize;

int lowIndex = viewIndex*viewSize+1;
int highIndex = (viewIndex+1)*viewSize;

context.lowIndex = lowIndex;
int arraySize = 0;
List resultPartialList = null;
    conditions = [EntityCondition.makeCondition("contentIdStart", EntityOperator.EQUALS,(String)parameters.get("contentId"))];

if ((highIndex - lowIndex + 1) > 0) {
    // get the results as an entity list iterator
    boolean beganTransaction = false;
    if(resultPartialList==null){
    try {
        beganTransaction = TransactionUtil.begin();
        allConditions = EntityCondition.makeCondition( conditions, EntityOperator.AND );
        fieldsToSelect = FastSet.newInstance();
        //fieldsToSelect=["contentId", "contentName", "mimeTypeId"] as Set;
        findOptions = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true);
        EntityListIterator listIt=null;
        listIt = delegator.find("ContentAssocViewTo", allConditions, null, null, ["contentId ASC"], findOptions);
        resultPartialList = listIt.getPartialList(lowIndex, highIndex - lowIndex + 1);
        
        arraySize = listIt.getResultsSizeAfterPartialList();
        if (arraySize < highIndex) {
            highIndex = arraySize;
        }
        listIt.close();
    } catch (GenericEntityException e) {
        Debug.logError(e, "Failure in operation, rolling back transaction", "GetContentLookupList.groovy");
        try {
            // only rollback the transaction if we started one...
            TransactionUtil.rollback(beganTransaction, "Error looking up entity values in WebTools Entity Data Maintenance", e);
        } catch (GenericEntityException e2) {
            Debug.logError(e2, "Could not rollback transaction: " + e2.toString(), "GetContentLookupList.groovy");
        }
        // after rolling back, rethrow the exception
        throw e;
    } finally {
        // only commit the transaction if we started one... this will throw an exception if it fails
        TransactionUtil.commit(beganTransaction);
    }
    }
}
context.highIndex = highIndex;
context.arraySize = arraySize;
context.resultPartialList = resultPartialList;

viewIndexLast = (int) (arraySize/viewSize);
context.viewIndexLast = viewIndexLast;
contentAssoc = FastList.newInstance();
context.contentAssoc=resultPartialList;
