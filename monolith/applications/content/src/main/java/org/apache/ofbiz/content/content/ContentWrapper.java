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
package org.apache.ofbiz.content.content;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilCodec;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelUtil;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;

/**
 * ContentWrapper Interface
 */

public interface ContentWrapper {

    String MODULE = ContentWrapper.class.getName();
    String CACHE_KEY_SEPARATOR = "::";

    StringUtil.StringWrapper get(String contentTypeId, String encoderType);

    /**
     * Get the configured default for content mimeTypeId.
     * @param delegator
     * @return
     */
    static String getDefaultMimeTypeId(Delegator delegator) {
        return EntityUtilProperties.getPropertyValue("content", "defaultMimeType", "text/html; charset=utf-8", delegator);
    }

    /**
     * Check modelObject for existance of a field named like given contentTypeId and
     * return its value as String.
     * @param modelObject
     * @param contentTypeId
     * @return
     */
    static String getCandidateFieldValue(GenericValue modelObject, String contentTypeId) {
        if (modelObject != null) {
            String candidateFieldName = ModelUtil.dbNameToVarName(contentTypeId);
            if (modelObject.getModelEntity().isField(candidateFieldName)) {
                return modelObject.getString(candidateFieldName);
            }
        }
        return null;
    }

    /**
     * Check if modelEntityName is an existing entity and has a field named like
     * given contentTypeId and get the unique modelObject entry by modelObjectPk and
     * return the candidate field value as String.
     * @param delegator
     * @param modelEntityName
     * @param modelObjectPk
     * @param contentTypeId
     * @param useCache
     * @return
     * @throws GenericEntityException
     */
    static String getCandidateFieldValue(Delegator delegator, String modelEntityName, EntityCondition modelObjectPk,
            String contentTypeId, boolean useCache) throws GenericEntityException {

        ModelEntity modelEntity = delegator.getModelEntity(modelEntityName);
        if (modelEntity != null) {
            String candidateFieldName = ModelUtil.dbNameToVarName(contentTypeId);

            if (modelEntity.isField(candidateFieldName)) {
                GenericValue modelObject = EntityQuery.use(delegator).from(modelEntityName).where(modelObjectPk).cache(useCache).queryOne();
                if (modelObject != null) {
                    return modelObject.getString(candidateFieldName);
                }
            }
        }
        return null;
    }

    /**
     * Encode given content string via given encoderType.
     * @param value
     * @param encoderType
     * @return
     */
    static String encodeContentValue(String value, String encoderType) {
        if (UtilValidate.isNotEmpty(value)) {
            UtilCodec.SimpleEncoder encoder = UtilCodec.getEncoder(encoderType);
            if (encoder != null) {
                value = encoder.sanitize(value, null);
            } else {
                Debug.logWarning("Unknown encoderType %s for encoding content value!", MODULE, encoderType);
            }
        }
        return value;
    }
}
