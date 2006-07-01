/*
 * $Id: StartActivityJob.java 5462 2005-08-05 18:35:48Z jonesde $
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
package org.ofbiz.workflow.client;

import java.util.Date;
import java.util.HashMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.service.GenericRequester;
import org.ofbiz.service.job.AbstractJob;
import org.ofbiz.workflow.WfActivity;

/**
 * Workflow Client API - Start Activity Async-Job
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a> 
 * @version    $Rev$
 * @since      2.0
 */
public class StartActivityJob extends AbstractJob {
    
    public static final String module = StartActivityJob.class.getName();

    protected WfActivity activity = null;
    protected GenericRequester requester = null;

    public StartActivityJob(WfActivity activity) {
        this(activity, null);
    }
    
    public StartActivityJob(WfActivity activity, GenericRequester requester) {
        super(activity.toString() + "." + System.currentTimeMillis(), activity.toString());        
        this.activity = activity;
        this.requester = requester;
        runtime = new Date().getTime();
        if (Debug.verboseOn()) Debug.logVerbose("Created new StartActivityJob : " + activity, module);
    }

    protected void finish() {
        runtime = -1;
    }

    /**
     * @see org.ofbiz.service.job.Job#exec()
     */
    public void exec() {        
        String activityIds = null;
        try {
            Debug.logVerbose("Executing job now : " + activity, module);                                      
            activity.activate();
            if (requester != null)
                requester.receiveResult(new HashMap());
        } catch (Exception e) {            
            Debug.logError(e, "Start Activity [" + activity + "] Failed", module);
            if (requester != null)
                requester.receiveThrowable(e);
        }       
        finish();
    }
}
