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

import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.GenericValue


/**
 * Create a KeywordThesaurus
 * @return
 */

def createKeywordThesaurus() {
    if(!(security.hasEntityPermission("CATALOG", "_CREATE", parameters.userLogin))) {
        return error(UtilProperties.getMessage("CommonUiLabels", "CommonGenericPermissionError", parameters.locale))
    }

    GenericValue newEntity = makeValue("KeywordThesaurus", parameters)
    newEntity.enteredKeyword = newEntity.enteredKeyword.toLowerCase()
    newEntity.alternateKeyword = newEntity.alternateKeyword.toLowerCase()
    newEntity.create()
    return success()
}

/**
 * Delete a complete Entry KeywordThesaurus
 * @return
 */

def deleteKeywordThesaurus() {

    if(!(security.hasEntityPermission("CATALOG", "_DELETE", parameters.userLogin))) {
        return error(UtilProperties.getMessage("CommonUiLabels", "CommonGenericPermissionError", parameters.locale))
    }

    GenericValue newEntity = makeValue("KeywordThesaurus")
    newEntity.enteredKeyword = parameters.enteredKeyword
    if (UtilValidate.isNotEmpty(parameters.alternateKeyword)) {
        newEntity.alternateKeyword = parameters.alternateKeyword
    }

    delegator.removeByAnd("KeywordThesaurus", newEntity)

    return success()
}
