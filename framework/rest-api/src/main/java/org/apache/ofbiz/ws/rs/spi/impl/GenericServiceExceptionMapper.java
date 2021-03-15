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
package org.apache.ofbiz.ws.rs.spi.impl;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericEntityNotFoundException;
import org.apache.ofbiz.entity.GenericNoSuchEntityException;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ServiceValidationException;
import org.apache.ofbiz.ws.rs.core.ResponseStatus;
import org.apache.ofbiz.ws.rs.response.Error;
import org.apache.ofbiz.ws.rs.spi.AbstractExceptionMapper;
import org.apache.ofbiz.ws.rs.util.ErrorUtil;
import org.codehaus.groovy.runtime.InvokerInvocationException;

/**
 *
 * Exception Mapper for GenericServiceException. Catches GenericServiceException and handles it and prepares appropriate response.
 *
 */
@Provider
public class GenericServiceExceptionMapper extends AbstractExceptionMapper implements javax.ws.rs.ext.ExceptionMapper<GenericServiceException> {

    /**
     * Module Name Used for debugging
     */
    private static final String MODULE = GenericServiceExceptionMapper.class.getName();

    @Context
    private HttpServletRequest request;

    @Context
    private ContainerRequestContext crc;

    /**
     * To response response.
     * @param gse GenericServiceException
     * @return Response
     */
    @Override
    public Response toResponse(GenericServiceException gse) {
        Debug.logError(gse.getMessage(), MODULE);
        Response.ResponseBuilder builder = null;
        Throwable actualCause = gse.getCause();
        if (actualCause == null) {
            actualCause = gse;
        } else if (actualCause instanceof InvokerInvocationException) {
            actualCause = actualCause.getCause();
        }
        String service = (String) crc.getProperty("requestForService");
        if (actualCause instanceof ServiceValidationException) {
            ServiceValidationException validationException = (ServiceValidationException) actualCause;
            Error error = new Error().type(actualCause.getClass().getSimpleName())
                    .code(Response.Status.BAD_REQUEST.getStatusCode())
                    .description(Response.Status.BAD_REQUEST.getReasonPhrase())
                    .message(ErrorUtil.getErrorMessage(service, "GenericServiceValidationErrorMessage", request.getLocale()))
                    .errorDesc((validationException.getMessage()))
                    .additionalErrors(validationException.getMessageList());
            builder = Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(error);
        } else if (actualCause instanceof GenericNoSuchEntityException
                || actualCause instanceof GenericEntityNotFoundException) {
            Error error = new Error().type(actualCause.getClass().getSimpleName())
                    .code(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                    .description(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())
                    .message(ErrorUtil.getErrorMessage(service, "NoSuchEntityDefaultMessage", request.getLocale()))
                    .errorDesc(ExceptionUtils.getRootCauseMessage(gse));
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
                    .entity(error);
        } else if (actualCause instanceof GenericEntityException) {
            Error error = new Error().type(actualCause.getClass().getSimpleName())
                    .code(ResponseStatus.Custom.UNPROCESSABLE_ENTITY.getStatusCode())
                    .description(ResponseStatus.Custom.UNPROCESSABLE_ENTITY.getReasonPhrase())
                    .message(ErrorUtil.getErrorMessage(service, "GenericServiceExecutionGenericEntityOperationErrorMessage",
                            request.getLocale()))
                    .errorDesc(ExceptionUtils.getRootCauseMessage(gse));
            builder = Response.status(ResponseStatus.Custom.UNPROCESSABLE_ENTITY).type(MediaType.APPLICATION_JSON)
                    .entity(error);
        } else {
            Error error = new Error().type(actualCause.getClass().getSimpleName())
                    .code(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                    .description(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())
                    .message(ErrorUtil.getErrorMessage(service, "GenericServiceExecutionGenericExceptionErrorMessage",
                            request.getLocale()))
                    .errorDesc(ExceptionUtils.getRootCauseMessage(gse));
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
                    .entity(error);
        }
        return builder.build();
    }
}
