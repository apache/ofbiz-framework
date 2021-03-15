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
package org.apache.ofbiz.ws.rs.spi;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.ofbiz.ws.rs.response.Error;

/*
 *
 */
public class AbstractExceptionMapper {
    /**
     * @param status
     * @param responseEntity
     * @return
     */
    protected Response errorResponse(int status, Error responseEntity) {
        return customizeResponse(status, responseEntity);
    }

    /**
     * @param status
     * @param responseEntity
     * @param t
     * @return
     */
    protected Response errorResponse(int status, Error responseEntity, Throwable t) {
        return customizeResponse(status, responseEntity);
    }

    /**
     * @param ex
     * @return
     */
    protected Response.StatusType getStatusType(Throwable ex) {
        if (ex instanceof WebApplicationException) {
            return ((WebApplicationException) ex).getResponse().getStatusInfo();
        } else {
            return Response.Status.INTERNAL_SERVER_ERROR;
        }
    }

    /**
     * @param status
     * @param responseEntity
     * @return
     */
    private Response customizeResponse(int status, Error responseEntity) {
        return Response.status(status).entity(responseEntity).type(MediaType.APPLICATION_JSON).build();
    }

}
