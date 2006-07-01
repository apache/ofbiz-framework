/*
 * $Id: SimpleRequester.java 7426 2006-04-26 23:35:58Z jonesde $
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

import java.util.Map;

import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.GenericRequester;
import org.ofbiz.service.ModelService;

import org.enhydra.shark.api.SharkTransaction;
import org.enhydra.shark.api.client.wfbase.BaseException;
import org.enhydra.shark.api.client.wfmodel.InvalidPerformer;
import org.enhydra.shark.api.client.wfmodel.WfEventAudit;

/**
 * 
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @since      3.3
 */
public class SimpleRequester extends AbstractRequester {

    public static final String module = SimpleRequester.class.getName();
    protected GenericRequester req = null;
    protected ModelService model = null;

    // new requester
    public SimpleRequester(GenericValue userLogin, ModelService model, GenericRequester req) {
        super(userLogin);
        this.model = model;
        this.setServiceRequester(req);

    }

    public SimpleRequester(GenericValue userLogin, ModelService model) {
        this(userLogin, model, null);
    }

    // -------------------
    // WfRequester methods
    // -------------------

    public void receive_event(WfEventAudit event) throws BaseException, InvalidPerformer {
        if (this.req != null) {
            Map out = model.makeValid(this.getWRD(event, null), ModelService.OUT_PARAM);
            req.receiveResult(out);
        }
    }

    public void receive_event(SharkTransaction sharkTransaction, WfEventAudit event) throws BaseException, InvalidPerformer {
        this.receive_event(event);
    }

    // -------------
    // local methods
    // -------------

    public void setServiceRequester(GenericRequester req) {
        this.req = req;
    }

    public GenericRequester getServiceRequester() {
        return this.req;
    }
}
