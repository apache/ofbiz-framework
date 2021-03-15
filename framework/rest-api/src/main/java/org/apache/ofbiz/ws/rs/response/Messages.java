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
package org.apache.ofbiz.ws.rs.response;

public class Messages {

    private String successMsgKey;
    private String errorMsgKey;

    public Messages(String successMessageVal, String errorMessageVal) {
        successMsgKey = successMessageVal;
        errorMsgKey = errorMessageVal;
    }

    /**
     * @param successKey
     * @return
     */
    public Messages successKey(String successKey) {
        successMsgKey = successKey;
        return this;
    }

    /**
     * @param errorKey
     * @return
     */
    public Messages errorKey(String errorKey) {
        errorMsgKey = errorKey;
        return this;
    }

    /**
     * @return the successMsgKey
     */
    public String getSuccessMsgKey() {
        return successMsgKey;
    }

    /**
     * @return the errorMsgKey
     */
    public String getErrorMsgKey() {
        return errorMsgKey;
    }
}
