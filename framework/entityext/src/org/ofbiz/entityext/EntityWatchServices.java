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
package org.ofbiz.entityext;

import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.security.Security;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.jdbc.DatabaseUtil;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilURL;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;

import java.util.Map;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;

public class EntityWatchServices {

    public static final String module = EntityWatchServices.class.getName();

    /**
     * This service is meant to be called through an Entity ECA (EECA) to watch an entity
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map watchEntity(DispatchContext dctx, Map context) {
        GenericValue newValue = (GenericValue) context.get("newValue");
        String fieldName = (String) context.get("fieldName");

        if (newValue == null) {
            return ServiceUtil.returnSuccess();
        }

        GenericValue currentValue = null;
        try {
            currentValue = dctx.getDelegator().findOne(newValue.getEntityName(), newValue.getPrimaryKey(), false);
        } catch (GenericEntityException e) {
            String errMsg = "Error finding currentValue for primary key [" + newValue.getPrimaryKey() + "]: " + e.toString();
            Debug.logError(e, errMsg, module);
        }

        if (currentValue != null) {
            if (UtilValidate.isNotEmpty(fieldName)) {
                // just watch the field
                Object currentFieldValue = currentValue.get(fieldName);
                Object newFieldValue = newValue.get(fieldName);
                boolean changed = false;
                if (currentFieldValue != null) {
                    if (!currentFieldValue.equals(newFieldValue)) {
                        changed = true;
                    }
                } else {
                    if (newFieldValue != null) {
                        changed = true;
                    }
                }

                if (changed) {
                    String errMsg = "Watching entity [" + currentValue.getEntityName() + "] field [" + fieldName + "] value changed from [" + currentFieldValue + "] to [" + newFieldValue + "] for pk [" + newValue.getPrimaryKey() + "]";
                    Debug.log(new Exception(errMsg), errMsg, module);
                }
            } else {
                // watch the whole entity
                if (!currentValue.equals(newValue)) {
                    String errMsg = "Watching entity [" + currentValue.getEntityName() + "] values changed from [" + currentValue + "] to [" + newValue + "] for pk [" + newValue.getPrimaryKey() + "]";
                    Debug.log(new Exception(errMsg), errMsg, module);
                }
            }
        } else {
            if (UtilValidate.isNotEmpty(fieldName)) {
                // just watch the field
                Object newFieldValue = newValue.get(fieldName);
                String errMsg = "Watching entity [" + newValue.getEntityName() + "] field [" + fieldName + "] value changed from [null] to [" + newFieldValue + "] for pk [" + newValue.getPrimaryKey() + "]";
                Debug.log(new Exception(errMsg), errMsg, module);
            } else {
                // watch the whole entity
                String errMsg = "Watching entity [" + newValue.getEntityName() + "] values changed from [null] to [" + newValue + "] for pk [" + newValue.getPrimaryKey() + "]";
                Debug.log(new Exception(errMsg), errMsg, module);
            }
        }

        return ServiceUtil.returnSuccess();
    }
}
