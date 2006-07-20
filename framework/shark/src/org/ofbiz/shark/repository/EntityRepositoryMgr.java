/*
 * Copyright 2001-2006 The Apache Software Foundation
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
package org.ofbiz.shark.repository;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.ofbiz.shark.transaction.JtaTransaction;
import org.ofbiz.shark.container.SharkContainer;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.Debug;

import org.enhydra.shark.api.internal.repositorypersistence.RepositoryPersistenceManager;
import org.enhydra.shark.api.internal.repositorypersistence.RepositoryException;
import org.enhydra.shark.api.internal.working.CallbackUtilities;
import org.enhydra.shark.api.RootException;
import org.enhydra.shark.api.RepositoryTransaction;
import org.enhydra.shark.api.TransactionException;

/**
 * 
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @since      3.1
 */
public class EntityRepositoryMgr implements RepositoryPersistenceManager {

    public static final String module = EntityRepositoryMgr.class.getName();
    protected CallbackUtilities callBack = null;

    public void configure(CallbackUtilities callBack) throws RootException {
        this.callBack = callBack;
    }

    public void uploadXPDL(RepositoryTransaction t, String xpdlId, byte[] xpdl) throws RepositoryException {
        Debug.log("XPDL Upload : " + xpdlId, module);
        GenericDelegator delegator = SharkContainer.getDelegator();
        try {
            GenericValue v = delegator.makeValue("WfRepository", null);
            v.set("xpdlId", xpdlId);
            v.set("xpdlVersion", UtilDateTime.nowDateString());
            v.set("isHistorical", "N");
            v.setBytes("xpdlData", xpdl);
            delegator.create(v);
            Debug.log("Created Value - " + v, module);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    public void updateXPDL(RepositoryTransaction t, String xpdlId, String xpdlVersion, byte[] xpdl) throws RepositoryException {
        Debug.log("XPDL Update : " + xpdlId + "/" + xpdlVersion + " - " + StringUtil.toHexString(xpdl), module);
        GenericValue value = this.getXpdlValue(xpdlId, xpdlVersion, false);
        if (value != null) {
            value.setBytes("xpdlData", xpdl);
            try {
               value.store();
            } catch (GenericEntityException e) {
                throw new RepositoryException(e);
            }
        }
    }

    public void deleteXPDL(RepositoryTransaction t, String xpdlId, String xpdlVersion) throws RepositoryException {
        Debug.log("XPDL Delete : " + xpdlId + "/" + xpdlVersion, module);
        GenericValue value = this.getXpdlValue(xpdlId, xpdlVersion, false);
        if (value != null) {
            try {
                value.remove();
            } catch (GenericEntityException e) {
                throw new RepositoryException(e);
            }
        }
    }

    public void moveToHistory(RepositoryTransaction t, String xpdlId, String xpdlVersion) throws RepositoryException {
        Debug.log("XPDL Move to History : " + xpdlId + "/" + xpdlVersion, module);
        GenericValue value = this.getXpdlValue(xpdlId, xpdlVersion, false);
        value.set("isHistorical", "Y");
        try {
            value.store();
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    public void deleteFromHistory(RepositoryTransaction t, String xpdlId, String xpdlVersion) throws RepositoryException {
        Debug.log("XPDL Delete from History: " + xpdlId + "/" + xpdlVersion, module);
        GenericValue value = this.getXpdlValue(xpdlId, xpdlVersion, true);
        if (value != null) {
            try {
                value.remove();
            } catch (GenericEntityException e) {
                throw new RepositoryException(e);
            }
        }
    }

    public void clearRepository(RepositoryTransaction t) throws RepositoryException {
        Debug.log("XPDL Clear Repository", module);
        GenericDelegator delegator = SharkContainer.getDelegator();
        try {
            delegator.removeByAnd("WfRepository", null);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    public String getCurrentVersion(RepositoryTransaction t, String xpdlId) throws RepositoryException {
        Debug.log("XPDL get current version : " + xpdlId, module);
        List lookupList = this.getXpdlValues(xpdlId, null, false);
        GenericValue value = EntityUtil.getFirst(lookupList);
        if (value != null) {
            return value.getString("xpdlVersion");
        } else {
            throw new RepositoryException("XPDL not found in repository!");
        }
    }

    public String getNextVersion(RepositoryTransaction t, String xpdlId) throws RepositoryException {
        return UtilDateTime.nowDateString();
    }

    public byte[] getXPDL(RepositoryTransaction t, String xpdlId) throws RepositoryException {
        Debug.log("XPDL Get : " + xpdlId, module);
        List lookupList = this.getXpdlValues(xpdlId, null, false);
        GenericValue value = EntityUtil.getFirst(lookupList);
        if (value != null) {
            return value.getBytes("xpdlData");
        } else {
            throw new RepositoryException("XPDL not found in repository!");
        }
    }

    public byte[] getXPDL(RepositoryTransaction t, String xpdlId, String xpdlVersion) throws RepositoryException {
        Debug.log("XPDL Get : " + xpdlId + "/" + xpdlVersion, module);
        GenericValue value = this.getXpdlValue(xpdlId, xpdlVersion, false);
        if (value != null) {
            return value.getBytes("xpdlData");
        } else {
            throw new RepositoryException("XPDL not found in repository!");
        }
    }

    public List getXPDLVersions(RepositoryTransaction t, String xpdlId) throws RepositoryException {
        Debug.log("XPDL Get Versions : " + xpdlId, module);
        List lookupList = this.getXpdlValues(xpdlId, null, false);
        List versionList = new ArrayList();
        if (!UtilValidate.isEmpty(lookupList)) {
            Iterator i = lookupList.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                versionList.add(v.getString("xpdlVersion"));
            }
        }
        return versionList;
    }

    public boolean doesXPDLExist(RepositoryTransaction t, String xpdlId) throws RepositoryException {
        List xpdls = this.getXpdlValues(xpdlId, null, false);
        Debug.log("Does XPDL [" + xpdlId + "] Exist - " + xpdls + "(" + (xpdls != null && xpdls.size() > 0 ? true : false) + ")", module);
        return (xpdls != null && xpdls.size() > 0 ? true : false);
    }

    public boolean doesXPDLExist(RepositoryTransaction t, String xpdlId, String xpdlVersion) throws RepositoryException {
        GenericValue xpdl = this.getXpdlValue(xpdlId, xpdlVersion, false);
        Debug.log("Does XPDL [" + xpdlId + "/" + xpdlVersion + "] Exist - " + xpdl + "(" + (xpdl != null) + ")", module);
        return (xpdl != null);
    }

    public List getExistingXPDLIds(RepositoryTransaction t) throws RepositoryException {
        Debug.log("Get Existing XPDL IDs", module);
        List lookupList = this.getXpdlValues(null, null, false);
        List idList = new ArrayList();
        if (UtilValidate.isNotEmpty(lookupList)) {
            Iterator i = lookupList.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                Debug.log("Checking - " + v, module);
                String id = v.getString("xpdlId");
                if (!idList.contains(id)) {
                    idList.add(id);
                }
            }
        }
        return idList;
    }

    public void addXPDLReference(RepositoryTransaction t, String referredXPDLId, String referringXPDLId, String referringXPDLVersion) throws RepositoryException {
        Debug.log("Add XPDL Reference", module);
        GenericDelegator delegator = SharkContainer.getDelegator();
        GenericValue ref = delegator.makeValue("WfRepositoryRef", null);
        ref.set("xpdlId", referringXPDLId);
        ref.set("xpdlVersion", referringXPDLVersion);
        ref.set("refXpdlId", referredXPDLId);
        try {
            delegator.create(ref);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    public List getReferringXPDLIds(RepositoryTransaction t, String referredXPDLId) throws RepositoryException {
        Debug.log("Get XPDL Reference IDs", module);
        GenericDelegator delegator = SharkContainer.getDelegator();
        List referringIds = new ArrayList();
        List refs = null;
        try {
            refs = delegator.findByAnd("WfRepositoryRef", UtilMisc.toMap("refXpdlId", referredXPDLId));
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
        if (!UtilValidate.isEmpty(refs)) {
            Iterator i = refs.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                referringIds.add(v.getString("xpdlId"));
            }
        }
        return referringIds;
    }

    public List getReferringXPDLVersions(RepositoryTransaction t, String referredXPDLId, String referringXPDLId) throws RepositoryException {
        Debug.log("Get Referring XPDL Versions", module);
        GenericDelegator delegator = SharkContainer.getDelegator();
        List referringVers = new ArrayList();
        List refs = null;
        try {
            refs = delegator.findByAnd("WfRepositoryRef", UtilMisc.toMap("refXpdlId", referredXPDLId, "xpdlId", referringXPDLId));
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
        if (!UtilValidate.isEmpty(refs)) {
            Iterator i = refs.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                referringVers.add(v.getString("xpdlVersion"));
            }
        }
        return referringVers;
    }

    public List getReferredXPDLIds(RepositoryTransaction t, String referringXPDLId, String referringXPDLVersion) throws RepositoryException {
        Debug.log("Get Referring XPDL IDs", module);
        GenericDelegator delegator = SharkContainer.getDelegator();
        List referringIds = new ArrayList();
        List refs = null;
        try {
            refs = delegator.findByAnd("WfRepositoryRef", UtilMisc.toMap("xpdlId", referringXPDLId, "xpdlVersion", referringXPDLVersion));
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
        if (!UtilValidate.isEmpty(refs)) {
            Iterator i = refs.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                referringIds.add(v.getString("refXpdlId"));
            }
        }
        return referringIds;
    }

    public RepositoryTransaction createTransaction() throws TransactionException {
        return new JtaTransaction();
    }

    private GenericValue getXpdlValue(String xpdlId, String xpdlVersion, boolean includeHistorical) throws RepositoryException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        GenericValue xpdl = null;
        try {
            xpdl = delegator.findByPrimaryKey("WfRepository", UtilMisc.toMap("xpdlId", xpdlId, "xpdlVersion", xpdlVersion));            
            if (!includeHistorical && xpdl.get("isHistorical") != null && xpdl.getString("isHistorical").equalsIgnoreCase("Y")) {
                xpdl = null;

            }
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
        return xpdl;
    }

    private List getXpdlValues(String xpdlId, String xpdlVersion, boolean includeHistory) throws RepositoryException {
        GenericDelegator delegator = SharkContainer.getDelegator();
        List exprList = new ArrayList();
        if (xpdlId != null) {
            exprList.add(new EntityExpr("xpdlId", EntityOperator.EQUALS, xpdlId));
        }
        if (xpdlVersion != null) {
            exprList.add(new EntityExpr("xpdlVersion", EntityOperator.EQUALS, xpdlVersion));
        }
        if (!includeHistory) {
            exprList.add(new EntityExpr("isHistorical", EntityOperator.NOT_EQUAL, "Y"));
        }

        EntityCondition cond = new EntityConditionList(exprList, EntityOperator.AND);
        List lookupList = null;
        try {
            lookupList = delegator.findByCondition("WfRepository", cond, null, UtilMisc.toList("-xpdlVersion"));
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        } finally {
            Debug.set(Debug.VERBOSE, false);
        }        
        return lookupList;
    }
}
