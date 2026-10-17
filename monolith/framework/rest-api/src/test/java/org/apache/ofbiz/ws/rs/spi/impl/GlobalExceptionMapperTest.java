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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.ofbiz.ws.rs.response.Error;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;

public final class GlobalExceptionMapperTest {

    @Test
    public void mapsIllegalArgumentExceptionToBadRequest() {
        GlobalExceptionMapper mapper = new GlobalExceptionMapper();

        Response response = mapper.toResponse(new IllegalArgumentException("Invalid query contract"));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(Response.Status.BAD_REQUEST.getReasonPhrase(), response.getStatusInfo().getReasonPhrase());
        assertEquals("Invalid query contract", ((Error) response.getEntity()).getErrorMessage());
    }
}
