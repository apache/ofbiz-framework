/*
 * $Id: WorkflowEngine.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.workflow;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.transaction.InvalidTransactionException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionFactory;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.service.GenericRequester;
import org.ofbiz.service.GenericResultWaiter;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceDispatcher;
import org.ofbiz.service.engine.AbstractEngine;
import org.ofbiz.service.job.AbstractJob;
import org.ofbiz.service.job.Job;
import org.ofbiz.service.job.JobManagerException;

/**
 * WorkflowEngine - Workflow Service Engine
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
 */
public class WorkflowEngine extends AbstractEngine {

    public static final String module = WorkflowEngine.class.getName();

    public WorkflowEngine(ServiceDispatcher dispatcher) {
        super(dispatcher);
    }
       
    /**
     * @see org.ofbiz.service.engine.GenericEngine#runSync(java.lang.String, org.ofbiz.service.ModelService, java.util.Map)
     */
    public Map runSync(String localName, ModelService modelService, Map context) throws GenericServiceException {
        GenericResultWaiter waiter = new GenericResultWaiter();
        runAsync(localName, modelService, context, waiter, false);
        return waiter.waitForResult();
    }
   
    /**
     * @see org.ofbiz.service.engine.GenericEngine#runSyncIgnore(java.lang.String, org.ofbiz.service.ModelService, java.util.Map)
     */
    public void runSyncIgnore(String localName, ModelService modelService, Map context) throws GenericServiceException {
        runAsync(localName, modelService, context, null, false);
    }
   
    /**
     * @see org.ofbiz.service.engine.GenericEngine#runAsync(java.lang.String, org.ofbiz.service.ModelService, java.util.Map, boolean)
     */
    public void runAsync(String localName, ModelService modelService, Map context, boolean persist) throws GenericServiceException {
        runAsync(localName, modelService, context, null, persist);
    }
   
    /**
     * @see org.ofbiz.service.engine.GenericEngine#runAsync(java.lang.String, org.ofbiz.service.ModelService, java.util.Map, org.ofbiz.service.GenericRequester, boolean)
     */
    public void runAsync(String localName, ModelService modelService, Map context, GenericRequester requester, boolean persist) throws GenericServiceException {       
        // Suspend the current transaction
        TransactionManager tm = TransactionFactory.getTransactionManager();
        if (tm == null) {
            throw new GenericServiceException("Cannot get the transaction manager; cannot run persisted services.");
        }

        Transaction parentTrans = null;
        boolean beganTransaction = false;
        try {
            try {
                parentTrans = tm.suspend();                
                beganTransaction = TransactionUtil.begin();
                //Debug.logInfo("Suspended transaction; began new: " + beganTransaction, module);
            } catch (SystemException se) {
                Debug.logError(se, "Cannot suspend transaction: " + se.getMessage(), module);
            } catch (GenericTransactionException e) {
                Debug.logError(e, "Cannot begin nested transaction: " + e.getMessage(), module);
            }
            
            // Build the requester
            WfRequester req = null;
            try {
                req = WfFactory.getWfRequester();
            } catch (WfException e) {
                try {
                    TransactionUtil.rollback(beganTransaction, "Error getting Workflow Requester", e);
                } catch (GenericTransactionException gte) {
                    Debug.logError(gte, "Unable to rollback nested exception.", module);
                }
                throw new GenericServiceException(e.getMessage(), e);
            }

            // Get the package and process ID::VERSION
            String location = this.getLocation(modelService);
            String invoke = modelService.invoke;
            String packageId = this.getSplitPosition(location, 0);
            String packageVersion = this.getSplitPosition(location, 1);
            String processId = this.getSplitPosition(invoke, 0);
            String processVersion = this.getSplitPosition(invoke, 1);

            // Build the process manager
            WfProcessMgr mgr = null;
            try {
                mgr = WfFactory.getWfProcessMgr(dispatcher.getDelegator(), packageId, packageVersion, processId, processVersion);
            } catch (WfException e) {
                String errMsg = "Process manager error";
                Debug.logError(e, errMsg, module);
                try {
                    TransactionUtil.rollback(beganTransaction, errMsg, e);
                } catch (GenericTransactionException gte) {
                    Debug.logError(gte, "Unable to rollback nested exception.", module);
                }
                throw new GenericServiceException(e.getMessage(), e);
            } catch (Exception e) {
                Debug.logError(e, "Un-handled process manager error", module);
                throw new GenericServiceException(e.getMessage(), e);
            }

            // Create the process
            WfProcess process = null;
            try {
                process = mgr.createProcess(req);
            } catch (NotEnabled ne) {
                try {
                    TransactionUtil.rollback(beganTransaction, "Error in create workflow process: Not Enabled", ne);
                } catch (GenericTransactionException gte) {
                    Debug.logError(gte, "Unable to rollback nested exception.", module);
                }
                throw new GenericServiceException(ne.getMessage(), ne);
            } catch (InvalidRequester ir) {
                try {
                    TransactionUtil.rollback(beganTransaction, "Error in create workflow process: Invalid Requester", ir);
                } catch (GenericTransactionException gte) {
                    Debug.logError(gte, "Unable to rollback nested exception.", module);
                }
                throw new GenericServiceException(ir.getMessage(), ir);
            } catch (RequesterRequired rr) {
                try {
                    TransactionUtil.rollback(beganTransaction, "Error in create workflow process: Requester Required", rr);
                } catch (GenericTransactionException gte) {
                    Debug.logError(gte, "Unable to rollback nested exception.", module);
                }
                throw new GenericServiceException(rr.getMessage(), rr);
            } catch (WfException wfe) {
                try {
                    TransactionUtil.rollback(beganTransaction, "Error in create workflow process: general workflow error error", wfe);
                } catch (GenericTransactionException gte) {
                    Debug.logError(gte, "Unable to rollback nested exception.", module);
                }
                throw new GenericServiceException(wfe.getMessage(), wfe);
            } catch (Exception e) {
                Debug.logError(e, "Un-handled process exception", module);
                throw new GenericServiceException(e.getMessage(), e);
            }
            
            // Assign the owner of the process
            GenericValue userLogin = null;
            if (context.containsKey("userLogin")) {
                userLogin = (GenericValue) context.remove("userLogin");
                try {
                    Map fields = UtilMisc.toMap("partyId", userLogin.getString("partyId"),
                            "roleTypeId", "WF_OWNER", "workEffortId", process.runtimeKey(),
                            "fromDate", UtilDateTime.nowTimestamp());

                    try {
                        GenericValue wepa = dispatcher.getDelegator().makeValue("WorkEffortPartyAssignment", fields);
                        dispatcher.getDelegator().create(wepa);
                    } catch (GenericEntityException e) {
                        String errMsg = "Cannot set ownership of workflow";
                        try {
                            TransactionUtil.rollback(beganTransaction, errMsg, e);
                        } catch (GenericTransactionException gte) {
                            Debug.logError(gte, "Unable to rollback nested exception.", module);
                        }
                        throw new GenericServiceException(errMsg, e);
                    }
                } catch (WfException we) {
                    String errMsg = "Cannot get the workflow process runtime key";
                    try {
                        TransactionUtil.rollback(beganTransaction, errMsg, we);
                    } catch (GenericTransactionException gte) {
                        Debug.logError(gte, "Unable to rollback nested exception.", module);
                    }
                    throw new GenericServiceException(errMsg);
                }
            }
        
            // Grab the locale from the context
            Locale locale = (Locale) context.remove("locale");
        
            // Grab the starting activityId from the context
            String startActivityId = (String) context.remove("startWithActivityId");

            // Register the process and set the workflow owner
            try {
                req.registerProcess(process, context, requester);
                if (userLogin != null) {
                    Map pContext = process.processContext();
                    pContext.put("workflowOwnerId", userLogin.getString("userLoginId"));
                    process.setProcessContext(pContext);
                }
            } catch (WfException wfe) {
                try {
                    TransactionUtil.rollback(beganTransaction, wfe.getMessage(), wfe);
                } catch (GenericTransactionException gte) {
                    Debug.logError(gte, "Unable to rollback nested exception.", module);
                }
                throw new GenericServiceException(wfe.getMessage(), wfe);
            }
        
            // Set the initial locale - (in context)
            if (locale != null) {
                try {
                    Map pContext = process.processContext();
                    pContext.put("initialLocale", locale);
                    process.setProcessContext(pContext);
                } catch (WfException wfe) {
                    try {
                        TransactionUtil.rollback(beganTransaction, wfe.getMessage(), wfe);
                    } catch (GenericTransactionException gte) {
                        Debug.logError(gte, "Unable to rollback nested exception.", module);
                    }
                    throw new GenericServiceException(wfe.getMessage(), wfe);
                }
            }
        
            // Use the WorkflowRunner to start the workflow in a new thread                        
            try {
                Job job = new WorkflowRunner(process, requester, startActivityId);
                if (Debug.verboseOn()) Debug.logVerbose("Created WorkflowRunner: " + job, module);
                dispatcher.getJobManager().runJob(job);
            } catch (JobManagerException je) {
                try {
                    TransactionUtil.rollback(beganTransaction, je.getMessage(), je);
                } catch (GenericTransactionException gte) {
                    Debug.logError(gte, "Unable to rollback nested exception.", module);
                }
                throw new GenericServiceException(je.getMessage(), je);
            }
            
            try {
                TransactionUtil.commit(beganTransaction);
            } catch (GenericTransactionException e) {
                Debug.logError(e, "Cannot commit nested transaction: " + e.getMessage(), module);
            }
        } finally {
            // Resume the parent transaction
            if (parentTrans != null) {
                try {
                    tm.resume(parentTrans);
                    //Debug.logInfo("Resumed the parent transaction.", module);
                } catch (InvalidTransactionException ite) {
                    throw new GenericServiceException("Cannot resume transaction", ite);
                } catch (SystemException se) {
                    throw new GenericServiceException("Unexpected transaction error", se);
                }
            }
        }
    }

    private String getSplitPosition(String splitString, int position) {
        if (splitString.indexOf("::") == -1) {
            if (position == 0)
                return splitString;
            if (position == 1)
                return null;
        }
        List splitList = StringUtil.split(splitString, "::");
        return (String) splitList.get(position);
    }
}

/** Workflow Runner class runs inside its own thread using the Scheduler API */
class WorkflowRunner extends AbstractJob {

    GenericRequester requester;
    WfProcess process;
    String startActivityId;

    WorkflowRunner(WfProcess process, GenericRequester requester, String startActivityId) {
        super(process.toString() + "." + System.currentTimeMillis(), process.toString());
        this.process = process;
        this.requester = requester;
        this.startActivityId = startActivityId;
        runtime = new Date().getTime();
    }

    protected void finish() {
        runtime = -1;
    }

    public void exec() {
        try {
            if (startActivityId != null)
                process.start(startActivityId);
            else
                process.start();
        } catch (Exception e) {            
            Debug.logError(e, module);
            if (requester != null)
                requester.receiveResult(null);
        }
        finish();
    }
}

