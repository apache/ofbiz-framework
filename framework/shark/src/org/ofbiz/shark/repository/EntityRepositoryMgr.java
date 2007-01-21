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
package org.ofbiz.shark.repository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.enhydra.shark.api.RepositoryTransaction;
import org.enhydra.shark.api.RootException;
import org.enhydra.shark.api.TransactionException;
import org.enhydra.shark.api.internal.repositorypersistence.RepositoryException;
import org.enhydra.shark.api.internal.repositorypersistence.RepositoryPersistenceManager;
import org.enhydra.shark.api.internal.working.CallbackUtilities;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;

import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;

import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.shark.container.SharkContainer;
import org.ofbiz.shark.transaction.JtaTransaction;



public class EntityRepositoryMgr implements RepositoryPersistenceManager {

    public static final String module = EntityRepositoryMgr.class.getName();
    protected CallbackUtilities callBack = null;
    private NextVersions nextVersions;

    public void configure(CallbackUtilities callBack) throws RootException
    {
        this.callBack = callBack;
        nextVersions=new NextVersions();
    }
    public void updateXPDL(RepositoryTransaction t, String xpdlId, String xpdlVersion, byte[] xpdl) throws RepositoryException {
        GenericValue value = this.getXpdlValue(xpdlId, xpdlVersion, false);
        if (value != null) {
            value.setBytes(org.ofbiz.shark.SharkConstants.xpdlData, xpdl);

            try {
               value.store();
            } catch (GenericEntityException e) {
                throw new RepositoryException(e);
            }
        }
    }

    public void deleteXPDL(RepositoryTransaction t, String xpdlId, String xpdlVersion) throws RepositoryException {
        Debug.log("XPDL Delete : " + xpdlId + "/" + xpdlVersion, module);
        GenericDelegator delegator = SharkContainer.getDelegator();
        GenericValue value = this.getXpdlValue(xpdlId, xpdlVersion, false);
        if (value != null) {
            try {
                value.remove();
                delegator.removeByAnd(org.ofbiz.shark.SharkConstants.WfRepositoryRef, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.xpdlId, xpdlId));
            } catch (GenericEntityException e) {
                throw new RepositoryException(e);
            }
        }
    }

    public void moveToHistory(RepositoryTransaction t, String xpdlId, String xpdlVersion) throws RepositoryException {
        Debug.log("XPDL Move to History : " + xpdlId + "/" + xpdlVersion, module);
        GenericDelegator delegator = SharkContainer.getDelegator();
        GenericValue value = this.getXpdlValue(xpdlId, xpdlVersion, false);
        value.set(org.ofbiz.shark.SharkConstants.isHistorical, "Y");
        try {
            value.store();
            delegator.removeByAnd(org.ofbiz.shark.SharkConstants.WfRepositoryRef, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.xpdlId, xpdlId));
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
            delegator.removeByAnd(org.ofbiz.shark.SharkConstants.WfRepository, null);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    public String getCurrentVersion(RepositoryTransaction t, String xpdlId) throws RepositoryException
    {
        Debug.log("XPDL get current version : " + xpdlId, module);
        List lookupList = this.getXpdlValues(xpdlId, null, false);
        GenericValue value = EntityUtil.getFirst(lookupList);
        if (value != null) {
            return value.getString(org.ofbiz.shark.SharkConstants.xpdlVersion);
        } else {
            throw new RepositoryException("XPDL not found in repository!");
        }
    }

    public String getNextVersion(RepositoryTransaction t, String xpdlId) throws RepositoryException {
        try {
            return nextVersions.getNextVersion(xpdlId);
         } catch (Exception ex) {
            throw new RepositoryException(ex);
         }
    }

    public byte[] getXPDL(RepositoryTransaction t, String xpdlId) throws RepositoryException {
        Debug.log("XPDL Get : " + xpdlId, module);
        List lookupList = this.getXpdlValues(xpdlId, null, false);
        GenericValue value = EntityUtil.getFirst(lookupList);
        if (value != null) {
            return value.getBytes(org.ofbiz.shark.SharkConstants.serializedPkg);
        } else {
            throw new RepositoryException("XPDL not found in repository!");
        }
    }

    public byte[] getXPDL(RepositoryTransaction t, String xpdlId, String xpdlVersion) throws RepositoryException {
        Debug.log("XPDL Get : " + xpdlId + "/" + xpdlVersion, module);
        GenericValue value = this.getXpdlValue(xpdlId, xpdlVersion, false);
        if (value != null) {
            return value.getBytes(org.ofbiz.shark.SharkConstants.xpdlData);
        } else {
            throw new RepositoryException("XPDL not found in repository!");
        }
    }

    public List getXPDLVersions(RepositoryTransaction t, String xpdlId) throws RepositoryException {
        List lookupList = this.getXpdlValues(xpdlId, null, false);
        List versionList = new ArrayList();
        if (!UtilValidate.isEmpty(lookupList)) {
            Iterator i = lookupList.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                versionList.add(v.getString(org.ofbiz.shark.SharkConstants.xpdlVersion));
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
                String id = v.getString(org.ofbiz.shark.SharkConstants.xpdlId);
                if (!idList.contains(id)) {
                    idList.add(id);
                }
            }
        }
        return idList;
    }

    public List getReferringXPDLIds(RepositoryTransaction t, String referredXPDLId) throws RepositoryException {
        Debug.log("Get XPDL Reference IDs", module);
        GenericDelegator delegator = SharkContainer.getDelegator();
        List referringIds = new ArrayList();
        List refs = null;
        try {
            refs = delegator.findByAnd(org.ofbiz.shark.SharkConstants.WfRepositoryRef, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.refXpdlId, referredXPDLId));
            
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
        if (!UtilValidate.isEmpty(refs)) {
            Iterator i = refs.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                referringIds.add(v.getString(org.ofbiz.shark.SharkConstants.xpdlId));
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
            refs = delegator.findByAnd(org.ofbiz.shark.SharkConstants.WfRepositoryRef, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.refXpdlId, referredXPDLId, org.ofbiz.shark.SharkConstants.xpdlId, referringXPDLId));
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
        if (!UtilValidate.isEmpty(refs)) {
            Iterator i = refs.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                referringVers.add(v.getString(org.ofbiz.shark.SharkConstants.xpdlVersion));
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
            refs = delegator.findByAnd(org.ofbiz.shark.SharkConstants.WfRepositoryRef, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.xpdlId, referringXPDLId, org.ofbiz.shark.SharkConstants.xpdlVersion, referringXPDLVersion));
        } catch (GenericEntityException e){
            throw new RepositoryException(e);
        }
        try
        {
            if (!UtilValidate.isEmpty(refs)) {
                Iterator i = refs.iterator();
                while (i.hasNext()) {
                    GenericValue v = (GenericValue) i.next();
                    referringIds.add(v.getString(org.ofbiz.shark.SharkConstants.refXpdlId));
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
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
            xpdl = delegator.findByPrimaryKey(org.ofbiz.shark.SharkConstants.WfRepository, UtilMisc.toMap(org.ofbiz.shark.SharkConstants.xpdlId, xpdlId, org.ofbiz.shark.SharkConstants.xpdlVersion, xpdlVersion));
            if (!includeHistorical && xpdl.get(org.ofbiz.shark.SharkConstants.isHistorical) != null && xpdl.getString(org.ofbiz.shark.SharkConstants.isHistorical).equalsIgnoreCase("Y")) {
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
            exprList.add(new EntityExpr(org.ofbiz.shark.SharkConstants.xpdlId, EntityOperator.EQUALS, xpdlId));
        }
        if (xpdlVersion != null) {
            exprList.add(new EntityExpr(org.ofbiz.shark.SharkConstants.xpdlVersion, EntityOperator.EQUALS, xpdlVersion));
        }
        if (!includeHistory) {
            exprList.add(new EntityExpr(org.ofbiz.shark.SharkConstants.isHistorical, EntityOperator.NOT_EQUAL, "Y"));
        }

        EntityCondition cond = new EntityConditionList(exprList, EntityOperator.AND);
        List lookupList = null;
        try {
            lookupList = delegator.findByCondition(org.ofbiz.shark.SharkConstants.WfRepository, cond, null, UtilMisc.toList("-xpdlVersion"));
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        } finally {
            Debug.set(Debug.VERBOSE, false);
        }
        return lookupList;
    }

    public void uploadXPDL(RepositoryTransaction t,
                            String xpdlId,
                            byte[] xpdl,
                            byte[] serializedPkg,
                            long xpdlClassVer) throws RepositoryException
    {
        Debug.log("XPDL Upload : " + xpdlId, module);
        //try{throw new Exception ("XPDL Upload");}catch(Exception e){e.printStackTrace();};
        String newVersion = null;
        try {
            newVersion = nextVersions.updateNextVersion(xpdlId);
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        GenericDelegator delegator = SharkContainer.getDelegator();
        try {
            GenericValue v = delegator.makeValue(org.ofbiz.shark.SharkConstants.WfRepository, null);
            v.set(org.ofbiz.shark.SharkConstants.xpdlId, xpdlId);
            v.set(org.ofbiz.shark.SharkConstants.xpdlVersion, newVersion);
            v.set(org.ofbiz.shark.SharkConstants.isHistorical, "N");
            v.set(org.ofbiz.shark.SharkConstants.XPDLClassVersion, (new Long (xpdlClassVer)).toString());
            v.setBytes(org.ofbiz.shark.SharkConstants.serializedPkg,serializedPkg);
            v.setBytes(org.ofbiz.shark.SharkConstants.xpdlData, xpdl);
            delegator.create(v);
            Debug.log("Created Value - " + v, module);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }

    }
    public void updateXPDL(RepositoryTransaction t,String xpdlId,String xpdlVersion,byte[] xpdl,byte[] serializedPkg,long xpdlClassVer) throws RepositoryException {
        GenericValue value = this.getXpdlValue(xpdlId, xpdlVersion, false);
        if (value != null) {
            value.setBytes(org.ofbiz.shark.SharkConstants.xpdlData, xpdl);
            value.setBytes(org.ofbiz.shark.SharkConstants.serializedPkg,serializedPkg);
            try {
               value.store();
            } catch (GenericEntityException e) {
                throw new RepositoryException(e);
            }
        }
    }

    public byte[] getSerializedXPDLObject(RepositoryTransaction arg0, String arg1, String arg2) throws RepositoryException
    {
        Debug.log("XPDL Get : " + arg1, module);
        List lookupList = this.getXpdlValues(arg1, null, false);
        GenericValue value = EntityUtil.getFirst(lookupList);
        if (value != null) {
            return value.getBytes(org.ofbiz.shark.SharkConstants.serializedPkg);
        } else {
            throw new RepositoryException("XPDL not found in repository!");
        }
    }
    public void addXPDLReference(RepositoryTransaction t,
            String referredXPDLId,
            String referringXPDLId,
            String referringXPDLVersion,
            int referredXPDLNumber) throws RepositoryException 
    {
            Debug.log("Add XPDL Reference", module);
            GenericDelegator delegator = SharkContainer.getDelegator();
            GenericValue ref = delegator.makeValue(org.ofbiz.shark.SharkConstants.WfRepositoryRef, null);
            ref.set(org.ofbiz.shark.SharkConstants.xpdlId, referringXPDLId);
            ref.set(org.ofbiz.shark.SharkConstants.xpdlVersion, referringXPDLVersion);
            ref.set(org.ofbiz.shark.SharkConstants.refXpdlId, referredXPDLId);
            ref.set(org.ofbiz.shark.SharkConstants.refNumber, (new Long(referredXPDLNumber)).toString());
            try {
                delegator.create(ref);
            } catch (GenericEntityException e) {
                throw new RepositoryException(e);
            }

    }

    public long getSerializedXPDLObjectVersion(RepositoryTransaction arg0, String xpdlId, String xpdlVersion) throws RepositoryException
    {
        try
        {
            return (new Long(getCurrentVersion(null, xpdlId))).longValue();
        }
        catch (Exception ex)
        {
            throw new RepositoryException(ex);
        }
    }

    public byte[] getSerializedXPDLObject(RepositoryTransaction arg0, String xpdlId) throws RepositoryException
    {
        try
        {
            byte [] b = getXPDL(null, xpdlId);
            if(b == null)
                return null;
            return b;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }
} 
class NextVersions extends HashMap implements Serializable 
{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final String INITIAL_VERSION = "1";

    public synchronized String getNextVersion (String xpdlId) 
    {
        if (containsKey(xpdlId)) 
        {
            return (String)get(xpdlId);
        }
        return INITIAL_VERSION;
    }

    public synchronized String updateNextVersion (String xpdlId) throws Exception 
    {
        String curVersion=INITIAL_VERSION;
        String nextVersion=INITIAL_VERSION;
        if (containsKey(xpdlId)) 
        {
            curVersion=(String)get(xpdlId);
        }
        int nver=Integer.parseInt(curVersion)+1;
        nextVersion=String.valueOf(nver);

        put(xpdlId,nextVersion);
        return curVersion;
    }
}
