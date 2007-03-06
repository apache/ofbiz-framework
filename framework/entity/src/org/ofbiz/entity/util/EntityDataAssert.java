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
package org.ofbiz.entity.util;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericPK;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.model.ModelEntity;
import org.xml.sax.SAXException;

/**
 * Some utility routines for loading seed data.
 */
public class EntityDataAssert {

    public static final String module = EntityDataAssert.class.getName();

    public static int assertData(URL dataUrl, GenericDelegator delegator, List errorMessages) throws GenericEntityException, SAXException, ParserConfigurationException, IOException {
        int rowsChecked = 0;

        if (dataUrl == null) {
            String errMsg = "Cannot assert/check data, dataUrl was null";
            errorMessages.add(errMsg);
            Debug.logError(errMsg, module);
            return 0;
        }

        Debug.logVerbose("Loading XML Resource: " + dataUrl.toExternalForm(), module);

        try {
            List checkValueList = delegator.readXmlDocument(dataUrl);
            Iterator checkValueIter = checkValueList.iterator();
            while (checkValueIter.hasNext()) {
                GenericValue checkValue = (GenericValue) checkValueIter.next();

                // to check get the PK, find by that, compare all fields
                GenericPK checkPK = checkValue.getPrimaryKey();
                GenericValue currentValue = delegator.findByPrimaryKey(checkPK);

                ModelEntity modelEntity = currentValue.getModelEntity();
                List nonpkFieldNameList = modelEntity.getNoPkFieldNames();
                Iterator nonpkFieldNameIter = nonpkFieldNameList.iterator();
                while (nonpkFieldNameIter.hasNext()) {
                    String nonpkFieldName = (String) nonpkFieldNameIter.next();
                    Object checkField = checkValue.get(nonpkFieldName);
                    Object currentField = currentValue.get(nonpkFieldName);

                    if (checkField != null && !checkField.equals(currentField)) {
                        StringBuffer matchError = new StringBuffer();
                        matchError.append("Field [" + modelEntity.getEntityName() + "." + nonpkFieldName + "] did not match; file value [" + checkField + "], db value [" + currentField + "] pk [" + checkPK + "]");
                        errorMessages.add(matchError.toString());
                    }
                }

                rowsChecked++;
            }
        } catch (GenericEntityException e) {
            String xmlError = "Error checking/asserting XML Resource: " + dataUrl.toExternalForm() + "; Error was: " + e.getMessage();
            Debug.logError(e, xmlError, module);
            // instead of adding this as a message, throw the real exception; then caller has more control
            //errorMessages.add(xmlError);
            throw e;
        }

        return rowsChecked;
    }
}
