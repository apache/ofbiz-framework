/*
 * $Id: DataEventAudit.java 7426 2006-04-26 23:35:58Z jonesde $
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
package org.ofbiz.shark.audit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

import org.enhydra.shark.api.internal.eventaudit.DataEventAuditPersistenceInterface;

/**
 * Persistance Object
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @since      3.1
 */
public class DataEventAudit extends EventAudit implements DataEventAuditPersistenceInterface {

    public static final String module = AssignmentEventAudit.class.getName();
    protected GenericValue dataEventAudit = null;
    private boolean newValue = false;

    public DataEventAudit(EntityAuditMgr mgr, GenericDelegator delegator, String eventAuditId) {
        super(mgr, delegator, eventAuditId);
        if (this.delegator != null) {
            try {
                this.dataEventAudit = delegator.findByPrimaryKey("WfDataEventAudit", UtilMisc.toMap("eventAuditId", eventAuditId));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        } else {
            Debug.logError("Invalid delegator object passed", module);
        }
    }

    public DataEventAudit(EntityAuditMgr mgr, GenericDelegator delegator) {
        super(mgr, delegator);
        this.newValue = true;
        this.dataEventAudit = delegator.makeValue("WfDataEventAudit", UtilMisc.toMap("eventAuditId", this.eventAuditId));
    }

    public DataEventAudit(EntityAuditMgr mgr, GenericValue dataEventAudit) {
        super(mgr, dataEventAudit.getDelegator(), dataEventAudit.getString("eventAuditId"));
        this.dataEventAudit = dataEventAudit;
    }
        
    public void setOldData(Map od) {
        byte[] value = serialize(od);
        dataEventAudit.setBytes("oldData", (value != null ? value : null));
    }

    public Map getOldData() {
        byte[] value = dataEventAudit.getBytes("oldData");
        if (value != null) {
            return deserialize(value);
        }
        return null;
    }

    public void setNewData(Map nd) {
        byte[] value = serialize(nd);
        dataEventAudit.setBytes("newData", (value != null ? value : null));
    }

    public Map getNewData() {
        byte[] value = dataEventAudit.getBytes("newData");
        if (value != null) {
            return deserialize(value);
        }
        return null;
    }

    public void store() throws GenericEntityException {
        super.store();
        if (newValue) {
            newValue = false;
            delegator.createOrStore(dataEventAudit);
        } else {
            delegator.store(dataEventAudit);
        }
    }

    public void reload() throws GenericEntityException {
        super.reload();
        if (!newValue) {
            dataEventAudit.refresh();
        }
    }

    public void remove() throws GenericEntityException {
        super.remove();
        if (!newValue) {
            delegator.removeValue(dataEventAudit);
        }
    }

    private Map deserialize(byte[] bytes) {
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;
        Map map = null;

        try {
            bis = new ByteArrayInputStream(bytes);
            ois = new ObjectInputStream(bis);
            map = (Map) ois.readObject();
        } catch (IOException e) {
            Debug.logError(e, module);
        } catch (ClassCastException e) {
            Debug.logError(e, module);
        } catch (ClassNotFoundException e) {
            Debug.logError(e, module);
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    Debug.logError(e, module);
                }
            }
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    Debug.logError(e, module);
                }
            }
        }
        return map;
    }

    private byte[] serialize(Map map) {
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;
        byte[] bytes = null;

        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(map);
            oos.flush();
            bytes = bos.toByteArray();
        } catch (IOException e) {
            Debug.logError(e, module);
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    Debug.logError(e, module);
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    Debug.logError(e, module);
                }
            }
        }
        return bytes;
    }
}
