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

    private static final String MODULE = DebugXaResource.class.getName();
    private Exception ex = null;

    public DebugXaResource(String info) {
        this.ex = new Exception(info);
    }

    public DebugXaResource() {
        this.ex = new Exception();
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        TransactionUtil.DEBUG_RES_MAP.remove(xid);
        if (Debug.verboseOn()) {
            Debug.logVerbose("Xid : " + xid.toString() + " cleared [commit]", MODULE);
        }
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        TransactionUtil.DEBUG_RES_MAP.remove(xid);
        if (Debug.verboseOn()) {
            Debug.logVerbose("Xid : " + xid.toString() + " cleared [rollback]", MODULE);
        }
    }

    @Override
    public void enlist() throws XAException {
        super.enlist();
        TransactionUtil.DEBUG_RES_MAP.put(getXid(), this);
    }

    /**
     * Log.
     */
    public void log() {
        Debug.logInfo("Xid : " + getXid(), MODULE);
        Debug.logInfo(ex, MODULE);
    }
}
