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

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.ws.rs.response.Error;

@Provider
public class GlobalExceptionMapper implements jakarta.ws.rs.ext.ExceptionMapper<Throwable> {

    /**
     * Module Name Used for debugging
     */
    private static final String MODULE = GlobalExceptionMapper.class.getName();

    /**
     * To response response.
     * @param throwable the throwable
     * @return the response
     */
    @Override
    public Response toResponse(Throwable throwable) {
        Debug.logError(throwable.getMessage(), MODULE);
        if (Debug.verboseOn()) {
            throwable.printStackTrace();
        }
        Response.StatusType type = (throwable instanceof WebApplicationException
                    ? ((WebApplicationException) throwable).getResponse().getStatusInfo()
                    : Response.Status.INTERNAL_SERVER_ERROR);

        Error error = new Error(type.getStatusCode(), type.getReasonPhrase(), throwable.getMessage());
        return Response.status(type.getStatusCode()).entity(error).type(MediaType.APPLICATION_JSON).build();
    }

}
