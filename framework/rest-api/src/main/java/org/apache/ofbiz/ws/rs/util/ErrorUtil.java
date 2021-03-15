/**
 *
 */
package org.apache.ofbiz.ws.rs.util;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.ws.rs.core.ResponseStatus;
import org.apache.ofbiz.ws.rs.response.Error;

public final class ErrorUtil {

    private ErrorUtil() {

    }

    private static final String DEFAULT_MSG_UI_LABEL_RESOURCE = "ApiUiLabels";

    @SuppressWarnings("unchecked")
    public static Response buildErrorFromServiceResult(String service, Map<String, Object> result, Locale locale) {
        String errorMessage = null;
        List<String> additionalErrorMessages = new LinkedList<>();
        if (!UtilValidate.isEmpty(result.get(ModelService.ERROR_MESSAGE))) {
            errorMessage = result.get(ModelService.ERROR_MESSAGE).toString();
        }
        if (!UtilValidate.isEmpty(result.get(ModelService.ERROR_MESSAGE_LIST))) {
            List<String> errorMessageList = (List<String>) result.get(ModelService.ERROR_MESSAGE_LIST);
            if (UtilValidate.isEmpty(errorMessage)) {
                errorMessage = errorMessageList.get(0);
                errorMessageList.remove(0);
            }
            for (int i = 0; i < errorMessageList.size(); i++) {
                additionalErrorMessages.add(errorMessageList.get(i));
            }
        }
        Error error = new Error().type("ServiceError").code(ResponseStatus.Custom.UNPROCESSABLE_ENTITY.getStatusCode())
                .description(ResponseStatus.Custom.UNPROCESSABLE_ENTITY.getReasonPhrase())
                .message(getErrorMessage(service, "GenericServiceErrorMessage", locale)).errorDesc(errorMessage);
        return Response.status(ResponseStatus.Custom.UNPROCESSABLE_ENTITY).type(MediaType.APPLICATION_JSON)
                .entity(error).build();
    }

    public static String getErrorMessage(String serviceName, String errorKey, Locale locale) {
        String error = UtilProperties.getMessage(DEFAULT_MSG_UI_LABEL_RESOURCE, errorKey, locale);
        error = error.replace("${service}", serviceName);
        return error;
    }
}
