/*
 * $Id: EntityDataAssert.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2004 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.ofbiz.entity.util;

import java.net.URL;
import java.util.Iterator;
import java.util.List;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericPK;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.model.ModelEntity;

/**
 * Some utility routines for loading seed data.
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      3.3
 */
public class EntityDataAssert {

    public static final String module = EntityDataAssert.class.getName();

    public static int assertData(URL dataUrl, GenericDelegator delegator, List errorMessages) {
        int rowsChecked = 0;
        
        if (dataUrl == null) {
            String errMsg = "Cannot assert/check data, dataUrl was null";
            errorMessages.add(errMsg);
            Debug.logError(errMsg, module);
            return 0;
        }

        Debug.logVerbose("[install.loadData] Loading XML Resource: \"" + dataUrl.toExternalForm() + "\"", module);

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
                    
                    boolean matches = false;
                    if (checkField == null) {
                        if (currentField == null) {
                            matches = true;
                        }
                    } else {
                        if (checkField.equals(currentField)) {
                            matches = true;
                        }
                    }
                    
                    if (!matches) {
                        StringBuffer matchError = new StringBuffer();
                        matchError.append("Field [" + modelEntity.getEntityName() + "." + nonpkFieldName + "] did not match; file value [" + checkField  + "], db value [" + currentField + "] pk [" + checkPK + "]");
                        errorMessages.add(matchError.toString());
                    }
                }
                
                rowsChecked++;
            }
        } catch (Exception e) {
            String xmlError = "Error checking/asserting XML Resource \"" + dataUrl.toExternalForm() + "\"; Error was: " + e.getMessage();
            errorMessages.add(xmlError);
            Debug.logError(e, xmlError, module);
        }

        return rowsChecked;
    }
}
