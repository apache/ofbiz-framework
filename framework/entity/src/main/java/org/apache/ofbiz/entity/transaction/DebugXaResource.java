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
package org.apache.ofbiz.entity.transaction;

import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import org.apache.ofbiz.base.util.Debug;

public class DebugXaResource extends GenericXaResource {

    public static final String module = DebugXaResource.class.getName();
    public Exception ex = null;

    public DebugXaResource(String info) {
        this.ex = new Exception(info);
    }

    public DebugXaResource() {
        this.ex = new Exception();
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        TransactionUtil.debugResMap.remove(xid);
        if (Debug.verboseOn()) {
            Debug.logVerbose("Xid : " + xid.toString() + " cleared [commit]", module);
        }
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        TransactionUtil.debugResMap.remove(xid);
        if (Debug.verboseOn()) {
            Debug.logVerbose("Xid : " + xid.toString() + " cleared [rollback]", module);
        }
    }

    @Override
    public void enlist() throws XAException {
        super.enlist();
        TransactionUtil.debugResMap.put(xid, this);
    }

    public void log() {
        Debug.logInfo("Xid : " + xid, module);
        Debug.logInfo(ex, module);
    }
}
