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

import org.apache.ofbiz.service.ModelService

def createTextAndUploadedContent(){
    Map result = success()

    Map serviceResult = run service: 'createContent', with: parameters
    parameters.parentContentId = serviceResult.contentId

    if (parameters.uploadedFile) {
        logInfo('Uploaded file found; processing sub-content')
        Map uploadContext = dispatcher.getDispatchContext()
                .makeValidContext('createContentFromUploadedFile', ModelService.IN_PARAM, parameters)
        uploadContext.ownerContentId = parameters.parentContentId
        uploadContext.contentIdFrom = parameters.parentContentId
        uploadContext.contentAssocTypeId = 'SUB_CONTENT'
        uploadContext.contentPurposeTypeId = 'SECTION'
        run service: 'createContentFromUploadedFile', with: uploadContext
    }

    result.contentId = parameters.parentContentId
    return result
}