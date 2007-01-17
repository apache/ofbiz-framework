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
package org.ofbiz.pos.adaptor;

import java.util.Map;
import java.sql.Timestamp;

import org.ofbiz.service.GenericServiceCallback;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.pos.screen.PosScreen;
import org.ofbiz.pos.event.SecurityEvents;

public class SyncCallbackAdaptor implements GenericServiceCallback {

    public static final String module = SyncCallbackAdaptor.class.getName();

    protected PosScreen screen = null;
    protected Timestamp txStamp = null;
    protected String entitySyncId = null;
    protected boolean enabled = true;

    public SyncCallbackAdaptor(PosScreen pos, String entitySyncId, Timestamp txStamp) {
        this.screen = pos;
        this.entitySyncId = entitySyncId;
        this.txStamp = txStamp;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    protected void internalReceiveEvent(Map context, Object obj) {
        String ctxSyncId = (String) context.get("entitySyncId");
        if (ctxSyncId != null && entitySyncId.equals(ctxSyncId)) {
            GenericValue entitySync = null;
            try {
                entitySync = screen.getSession().getDelegator().findByPrimaryKey("EntitySync", UtilMisc.toMap("entitySyncId", entitySyncId));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (entitySync != null) {
                Timestamp lastSync = entitySync.getTimestamp("lastSuccessfulSynchTime");
                if (lastSync.after(txStamp)) {
                    this.setEnabled(false);
                    screen.showDialog("dialog/error/terminalclosed");
                    screen.refresh();
                    SecurityEvents.logout(screen);
                }
            }
        }
    }

    public void receiveEvent(Map context) {
        this.internalReceiveEvent(context, null);
    }

    public void receiveEvent(Map context, Map result) {
        this.internalReceiveEvent(context, result);
    }

    public void receiveEvent(Map context, Throwable error) {
        this.internalReceiveEvent(context, error);
    }

    public boolean isEnabled() {
        return this.enabled;
    }
}
