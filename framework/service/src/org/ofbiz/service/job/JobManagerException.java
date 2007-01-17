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
package org.ofbiz.service.job;

/**
 * Job Scheduler Exception
 */
public class JobManagerException extends org.ofbiz.base.util.GeneralException {
    
    /**
     * Creates new <code>JobManagerException</code> without detail message.
     */
    public JobManagerException() {
        super();
    }

    /**
     * Constructs an <code>JobManagerException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public JobManagerException(String msg) {
        super(msg);
    }

    /**
     * Constructs an <code>JobManagerException</code> with the specified detail message and nested Exception.
     * @param nested the nested exception.
     */
    public JobManagerException(Throwable nested) {
        super(nested);
    }

    /**
     * Constructs an <code>JobManagerException</code> with the specified detail message and nested Exception.
     * @param msg the detail message.
     * @param nested the nested exception.
     */
    public JobManagerException(String msg, Throwable nested) {
        super(msg, nested);
    }
}

