/*
 * $Id: DebugXaResource.java 6778 2006-02-20 05:13:55Z jonesde $
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
package org.ofbiz.entity.transaction;

import javax.transaction.xa.Xid;
import javax.transaction.xa.XAException;

import org.ofbiz.base.util.Debug;

/**
 * 
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      3.1
 */
public class DebugXaResource extends GenericXaResource {

    public static final String module = DebugXaResource.class.getName();
    public Exception ex = null;

    public DebugXaResource(String info) {
        this.ex = new Exception(info);
    }
    
    public DebugXaResource() {
        this.ex = new Exception();
    }

    public void commit(Xid xid, boolean onePhase) throws XAException {
        TransactionUtil.debugResMap.remove(xid);
        if (Debug.verboseOn()) Debug.logVerbose("Xid : " + xid.toString() + " cleared [commit]", module);
    }

    public void rollback(Xid xid) throws XAException {
        TransactionUtil.debugResMap.remove(xid);
        if (Debug.verboseOn()) Debug.logVerbose("Xid : " + xid.toString() + " cleared [rollback]", module);
    }

    public void enlist() throws XAException {
        super.enlist();
        TransactionUtil.debugResMap.put(xid, this);
    }

    public void log() {
        Debug.log("Xid : " + xid, module);
        Debug.log(ex, module);
    }    
}
