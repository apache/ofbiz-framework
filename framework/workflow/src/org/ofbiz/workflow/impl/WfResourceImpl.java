/*
 * $Id: WfResourceImpl.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.ofbiz.workflow.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.workflow.NotAssigned;
import org.ofbiz.workflow.WfActivity;
import org.ofbiz.workflow.WfAssignment;
import org.ofbiz.workflow.WfException;
import org.ofbiz.workflow.WfFactory;
import org.ofbiz.workflow.WfResource;

/**
 * WfResourceImpl - Workflow Resource Object implementation
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
 */
public class WfResourceImpl implements WfResource {

    protected GenericDelegator delegator = null;
    protected String resourceKey = null;
    protected String resourceName = null;
    protected String description = null;
    protected String partyId = null;
    protected String roleTypeId = null;
    protected String type = null;

    /**
     * Creates a new WfResource
     * @param resourceKey Uniquely identifies the resource
     * @param resourceName The name of the resource
     * @param partyId The partyID of this resource
     * @param roleTypeId The roleTypeId of this resource
     * @param fromDate The fromDate of this resource
     */
    public WfResourceImpl(GenericDelegator delegator, String resourceKey, String resourceName, String partyId, String roleTypeId) {
        this.delegator = delegator;
        this.resourceKey = resourceKey;
        this.resourceName = resourceName;
        this.description = null;
        this.partyId = partyId;
        this.roleTypeId = roleTypeId;
        this.type = "HUMAN";
    }

    /**
     * Creates a new WfResource
     * @param valueObject The GenericValue object of the WorkflowParticipant
     */
    public WfResourceImpl(GenericValue valueObject) {
        this.delegator = valueObject.getDelegator();
        this.resourceKey = valueObject.getString("participantId");
        this.resourceName = valueObject.getString("participantName");
        this.description = valueObject.getString("description");
        this.partyId = valueObject.getString("partyId");
        this.roleTypeId = valueObject.getString("roleTypeId");
        this.type = valueObject.getString("participantTypeId");
        if (partyId == null)
            partyId = "_NA_";
        if (roleTypeId == null)
            roleTypeId = "_NA_";
    }
   
    /**
     * @see org.ofbiz.workflow.WfResource#howManyWorkItem()
     */
    public int howManyWorkItem() throws WfException {
        return workItems().size();
    }
 
    /**
     * @see org.ofbiz.workflow.WfResource#getIteratorWorkItem()
     */
    public Iterator getIteratorWorkItem() throws WfException {
        return workItems().iterator();
    }
  
    /**
     * @see org.ofbiz.workflow.WfResource#getSequenceWorkItem(int)
     */
    public List getSequenceWorkItem(int maxNumber) throws WfException {
        if (maxNumber > 0)
            return workItems().subList(0, (maxNumber - 1));
        return workItems();
    }

    /**
     * @see org.ofbiz.workflow.WfResource#isMemberOfWorkItems(org.ofbiz.workflow.WfAssignment)
     */
    public boolean isMemberOfWorkItems(WfAssignment member) throws WfException {
        return workItems().contains(member);
    }

    /**
     * @see org.ofbiz.workflow.WfResource#resourceKey()
     */
    public String resourceKey() throws WfException {
        return resourceKey;
    }

    /**
     * @see org.ofbiz.workflow.WfResource#resourceName()
     */
    public String resourceName() throws WfException {
        return resourceName;
    }

    /**
     * @see org.ofbiz.workflow.WfResource#resourceRoleId()
     */
    public String resourceRoleId() throws WfException {
        return roleTypeId;
    }

    /**
     * @see org.ofbiz.workflow.WfResource#resourcePartyId()
     */
    public String resourcePartyId() throws WfException {
        return partyId;
    }

    /**
     * @see org.ofbiz.workflow.WfResource#release(org.ofbiz.workflow.WfAssignment, java.lang.String)
     */
    public void release(WfAssignment fromAssignment,
        String releaseInfo) throws WfException, NotAssigned {
        if (!workItems().contains(fromAssignment))
            throw new NotAssigned();
        // workItems.remove(fromAssignment);
        // log the transaction
    }

    private List workItems() throws WfException {
        List workList = new ArrayList();
        Collection c = null;

        try {
            Map fields = UtilMisc.toMap("partyId", partyId, "roleTypeId", roleTypeId);
            c = delegator.findByAnd("WorkEffortPartyAssignment", fields);
        } catch (GenericEntityException e) {
            throw new WfException(e.getMessage(), e);
        }

        if (c != null) {
            Iterator i = c.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                WfActivity a = null;

                try {
                    a = WfFactory.getWfActivity(delegator, v.getString("workEffortId"));
                } catch (RuntimeException e) {
                    throw new WfException(e.getMessage(), e);
                }
                if (a != null)
                    workList.add(a);
            }
        }
        return workList;
    }
}

