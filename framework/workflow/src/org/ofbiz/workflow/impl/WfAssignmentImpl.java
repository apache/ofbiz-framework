/*
 * $Id: WfAssignmentImpl.java 5462 2005-08-05 18:35:48Z jonesde $
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

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.workflow.CannotComplete;
import org.ofbiz.workflow.InvalidResource;
import org.ofbiz.workflow.WfActivity;
import org.ofbiz.workflow.WfAssignment;
import org.ofbiz.workflow.WfException;
import org.ofbiz.workflow.WfResource;

/**
 * WfAssignmentImpl - Workflow Assignment Object implementation
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a> 
 * @version    $Rev$
 * @since      2.0
 */
public class WfAssignmentImpl implements WfAssignment {

    public static final String module = WfAssignmentImpl.class.getName();

    protected WfActivity activity = null;
    protected WfResource resource = null;
    protected Timestamp fromDate = null;
    protected boolean create = false;

    /**
     * Creates new WfAssignment.
     * @param activity Sets the activity object for this assignment.
     * @param resource The WfResource object this is assigned to.
     * @throws WfException
     */
    public WfAssignmentImpl(WfActivity activity, WfResource resource, Timestamp fromDate, boolean create) throws WfException {
        this.activity = activity;
        this.resource = resource;
        this.fromDate = fromDate;
        this.create = create;
        checkAssignment();
    }

    // makes the assignment entity
    private void checkAssignment() throws WfException {
        String workEffortId = activity.runtimeKey();
        String partyId = resource.resourcePartyId();
        String roleTypeId = resource.resourceRoleId();

        if (workEffortId == null)
            throw new WfException("WorkEffort could not be found for assignment");
        if (partyId == null && roleTypeId == null)
            throw new WfException("Both party and role type IDs cannot be null");
        if (fromDate == null)
            throw new WfException("From date cannot be null");

        GenericValue value = null;
        Map fields = new HashMap();

        fields.put("workEffortId", workEffortId);
        fields.put("partyId", partyId);
        fields.put("roleTypeId", roleTypeId);
        fields.put("fromDate", fromDate);
        fields.put("statusId", "CAL_SENT");

        // check if one exists
        try {
            if (valueObject() != null) {
                Debug.logVerbose("[WfAssignment.checkAssignment] : found existing assignment.", module);
                return;
            }
        } catch (WfException e) {
            Debug.logVerbose("[WfAssignment.checkAssignment] : no existing assignment.", module);
        }

        if (create) {
            // none exist; create a new one
            try {
                GenericValue v = activity.getDelegator().makeValue("WorkEffortPartyAssignment", fields);

                value = activity.getDelegator().create(v);
                Debug.logVerbose("[WfAssignment.checkAssignment] : created new party assignment : " + v, module);
            } catch (GenericEntityException e) {
                throw new WfException(e.getMessage(), e);
            }
            if (value == null)
                throw new WfException("Could not create the assignement");
        }
        if (value == null)
            throw new WfException("No existing assignment found or create failed");
    }
   
    /**
     * @see org.ofbiz.workflow.WfAssignment#accept()
     */
    public void accept() throws WfException {
        boolean allDelegated = true;
        boolean acceptAll = activity.getDefinitionObject().get("acceptAllAssignments") != null ?
            activity.getDefinitionObject().getBoolean("acceptAllAssignments").booleanValue() : false;

        if (!acceptAll) {
            // check for existing accepted assignment
            if (!activity.state().equals("open.not_running.not_started")) {
                // activity already running all assignments must be delegated in order to accept
                Iterator ai = activity.getIteratorAssignment();

                while (ai.hasNext() && allDelegated) {
                    WfAssignment a = (WfAssignment) ai.next();
                    if (!a.equals(this) && !a.status().equals("CAL_DELEGATED")) {
                        allDelegated = false;
                    }
                }
                // we cannot accept if the activity is running, with active assignments
                if (!allDelegated) {
                    throw new WfException("Cannot accept; Activity already running with active assignments");
                }
            } else {
                // activity not running, auto change all assignments to delegated status
                Debug.logVerbose("[WfAssignment.accept] : setting other assignments to delegated status.", module);
                Iterator ai = activity.getIteratorAssignment();

                while (ai.hasNext()) {
                    WfAssignment a = (WfAssignment) ai.next();
                    if (!this.isEqual(a)) a.delegate();                    
                }
            }
        }
        // set this assignment as accepted
        changeStatus("CAL_ACCEPTED");
    }
 
    /**
     * @see org.ofbiz.workflow.WfAssignment#setResult(java.util.Map)
     */
    public void setResult(Map results) throws WfException {
        activity.setResult(results);
    }

    /**
     * @see org.ofbiz.workflow.WfAssignment#complete()
     */
    public void complete() throws WfException {
        changeStatus("CAL_COMPLETED");
        try {
            activity.complete();
        } catch (CannotComplete e) {
            Debug.logWarning("Activity not complete : " + e.getMessage(), module);            
        }
    }

    /**
     * @see org.ofbiz.workflow.WfAssignment#delegate()
     */
    public void delegate() throws WfException {
        // check and make sure we are not already delegated
        if (status().equals("CAL_DELEGATED"))
            throw new WfException("Assignment has already been delegated");
        
        // set the thru-date
        GenericValue valueObject = valueObject();
        try {
            valueObject.set("thruDate", UtilDateTime.nowTimestamp());
            valueObject.store();
            if (Debug.verboseOn()) Debug.logVerbose("[WfAssignment.delegated()] : set the thru-date.", module);
        } catch (GenericEntityException e) {
            e.printStackTrace();            
            throw new WfException(e.getMessage(), e);
        }  
        
        // change the status      
        changeStatus("CAL_DELEGATED");     
    }

    /**
     * @see org.ofbiz.workflow.WfAssignment#changeStatus(java.lang.String)
     */
    public void changeStatus(String status) throws WfException {    
        // change the status
        GenericValue valueObject = valueObject();
        try {
            valueObject.set("statusId", status);
            valueObject.store();
            if (Debug.verboseOn()) Debug.logVerbose("[WfAssignment.changeStatus] : changed status to " + status, module);
        } catch (GenericEntityException e) {
            e.printStackTrace();
            throw new WfException(e.getMessage(), e);
        }
    }

    /**
     * @see org.ofbiz.workflow.WfAssignment#activity()
     */
    public WfActivity activity() throws WfException {
        return activity;
    }

    /**
     * @see org.ofbiz.workflow.WfAssignment#assignee()
     */
    public WfResource assignee() throws WfException {
        return resource;
    }

    /**
     * @see org.ofbiz.workflow.WfAssignment#setAssignee(org.ofbiz.workflow.WfResource)
     */
    public void setAssignee(WfResource newValue) throws WfException, InvalidResource {
        remove();
        this.resource = newValue;
        this.fromDate = new Timestamp(new Date().getTime());
        checkAssignment();
    }

    /**
     * @see org.ofbiz.workflow.WfAssignment#remove()
     */
    public void remove() throws WfException {
        try {
            valueObject().remove();
        } catch (GenericEntityException e) {
            throw new WfException(e.getMessage(), e);
        }
    }

    /**
     * @see org.ofbiz.workflow.WfAssignment#status()
     */
    public String status() throws WfException {
        return valueObject().getString("statusId");
    }

    /**
     * @see org.ofbiz.workflow.WfAssignment#fromDate()
     */
    public Timestamp fromDate() throws WfException {
        return fromDate;
    }

    private GenericValue valueObject() throws WfException {
        GenericValue value = null;
        Map fields = new HashMap();

        fields.put("workEffortId", activity.runtimeKey());
        fields.put("partyId", resource.resourcePartyId());
        fields.put("roleTypeId", resource.resourceRoleId());
        fields.put("fromDate", fromDate);
        try {
            value = activity.getDelegator().findByPrimaryKey("WorkEffortPartyAssignment", fields);
        } catch (GenericEntityException e) {
            throw new WfException(e.getMessage(), e);
        }
        if (value == null)
            throw new WfException("Invalid assignment; no runtime entity");
        return value;
    }
    
    private boolean isEqual(WfAssignment asgn) throws WfException {
        // compare this to null = different assignment
        if (asgn == null) {
            return false;
        }
        
        // if status is different; we must be different
        if (!this.status().equals(asgn.status())) {
            return false;
        }
        
        // different activity = different assignment
        WfActivity thisActivity = this.activity();
        WfActivity compActivity = asgn.activity();               
        if ((thisActivity == null && compActivity != null) || (thisActivity != null && compActivity == null)) {
            return false;              
        } else {
            String thisKey = thisActivity.runtimeKey();
            String compKey = compActivity.runtimeKey();
            if ((thisKey != null && compKey == null) || (thisKey == null && compKey != null)) {
                return false;
            } else if (thisKey != null && compKey != null && !thisKey.equals(compKey)) {
                return false;
            }                                
        }
        
        // different participantId = different assignment - the rest doesn't matter
        WfResource thisResource = this.assignee();
        WfResource compResource = asgn.assignee();       
        if ((thisResource == null && compResource != null) || (thisResource != null && compResource == null)) {
            return false;              
        } else {
            String thisKey = thisResource.resourceKey();
            String compKey = compResource.resourceKey();
            if ((thisKey != null && compKey == null) || (thisKey == null && compKey != null)) {
                return false;
            } else if (thisKey != null && compKey != null && !thisKey.equals(compKey)) {
                return false;
            }                                
        }               
        
        // same status, same activity, same participantId = same assignement
        return true;
    }
}

