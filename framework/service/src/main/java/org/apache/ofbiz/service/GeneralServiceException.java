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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;

/**
 * General Service Exception - base Exception for in-Service Errors
 */
@SuppressWarnings("serial")
public class GeneralServiceException extends org.apache.ofbiz.base.util.GeneralException {

    protected List<Object> errorMsgList = null;
    protected Map<String, ? extends Object> errorMsgMap = null;
    protected Map<String, ? extends Object> nestedServiceResult = null;

    public GeneralServiceException() {
        super();
    }

    public GeneralServiceException(String str) {
        super(str);
    }

    public GeneralServiceException(String str, Throwable nested) {
        super(str, nested);
    }

    public GeneralServiceException(Throwable nested) {
        super(nested);
    }

    public GeneralServiceException(String str, List<? extends Object> errorMsgList, Map<String, ? extends Object> errorMsgMap, Map<String, ? extends Object> nestedServiceResult, Throwable nested) {
        super(str, nested);
        this.errorMsgList = UtilMisc.makeListWritable(errorMsgList);
        this.errorMsgMap = errorMsgMap;
        this.nestedServiceResult = nestedServiceResult;
    }

    public Map<String, Object> returnError(String module) {
        String errMsg = this.getMessage() == null ? "Error in Service" : this.getMessage();
        if (this.getNested() != null) {
            Debug.logError(this.getNested(), errMsg, module);
        }
        return ServiceUtil.returnError(errMsg, this.errorMsgList, this.errorMsgMap, this.nestedServiceResult);
    }

    public void addErrorMessages(List<? extends Object> errMsgs) {
        if (this.errorMsgList == null) {
            this.errorMsgList = new LinkedList<Object>();
        }
        this.errorMsgList.addAll(errMsgs);
    }
}
