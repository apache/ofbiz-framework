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
package org.apache.ofbiz.service;

/**
 * Thrown by the Groovy DSL's {@code fail()}/{@code require()} control-flow helpers. Extends
 * {@link ExecutionServiceException} so it is recognized, with no engine changes, by the same
 * {@code instanceof ExecutionServiceException} check {@code GroovyEngine} already uses to convert
 * a nested {@code runService()} failure into a service error result, and is caught generically by
 * {@code GroovyEventHandler}'s existing catch-all exception handling for events.
 */
@SuppressWarnings("serial")
public class ServiceErrorException extends ExecutionServiceException {

    public ServiceErrorException(String str) {
        super(str);
    }
}
