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
package org.ofbiz.shark.requester;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericValue;

import org.enhydra.shark.api.SharkTransaction;
import org.enhydra.shark.api.client.wfbase.BaseException;
import org.enhydra.shark.api.client.wfmodel.InvalidPerformer;
import org.enhydra.shark.api.client.wfmodel.WfEventAudit;

/**
 * OFBiz -> Shark Logging Requester
 */
public class LoggingRequester extends AbstractRequester {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    public static final String module = LoggingRequester.class.getName();

    static GenericValue gv = null;
    
    
    public LoggingRequester(){
        super(gv);
    }
    
    // new requester
    public LoggingRequester(GenericValue userLogin) {
        super(userLogin);
        gv = userLogin;
    }

    // -------------------
    // WfRequester methods
    // -------------------

    public void receive_event(WfEventAudit event) throws BaseException, InvalidPerformer {
        Debug.log("Received event - " + event.event_type(), module);
    }

    public void receive_event(SharkTransaction sharkTransaction, WfEventAudit event) throws BaseException, InvalidPerformer {
        this.receive_event(event);
    }
}


