/*
 * $Id: LoggingRequester.java 7426 2006-04-26 23:35:58Z jonesde $
 *
 * Copyright 2004-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.ofbiz.shark.requester;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericValue;

import org.enhydra.shark.api.SharkTransaction;
import org.enhydra.shark.api.client.wfbase.BaseException;
import org.enhydra.shark.api.client.wfmodel.InvalidPerformer;
import org.enhydra.shark.api.client.wfmodel.WfEventAudit;

/**
 * OFBiz -> Shark Logging Requester
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @since      3.1
 */
public class LoggingRequester extends AbstractRequester {

    public static final String module = LoggingRequester.class.getName();

    // new requester
    public LoggingRequester(GenericValue userLogin) {
        super(userLogin);
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
