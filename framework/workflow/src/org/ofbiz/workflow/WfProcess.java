/*
 * $Id: WfProcess.java 5462 2005-08-05 18:35:48Z jonesde $
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
package org.ofbiz.workflow;

import java.util.Iterator;
import java.util.Map;
import java.util.List;

/**
 * WfProcess - Workflow Process Interface
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
 */
public interface WfProcess extends WfExecutionObject {

    /**
     * Gets the requester of this process
     * @throws WfException
     * @return requester of this process
     */
    public WfRequester requester() throws WfException;

    /**
     * Sets the requester for this process
     * @param newValue
     * @throws WfException
     * @throws CannotChangeRequester
     */
    public void setRequester(WfRequester newValue) throws WfException, CannotChangeRequester;

    /**
     * @throws WfException
     * @return
     */
    public int howManyStep() throws WfException;

    /**
     * @throws WfException
     * @return
     */
    public Iterator getIteratorStep() throws WfException;

    /**
     * @param maxNumber
     * @throws WfException
     * @return List of WfActivity objects.
     */
    public List getSequenceStep(int maxNumber) throws WfException;

    /**
     * @param member
     * @throws WfException
     * @return
     */
    public boolean isMemberOfStep(WfActivity member) throws WfException;

    /**
     * Gets the manager associated with this process
     * @throws WfException
     * @return
     */
    public WfProcessMgr manager() throws WfException;

    /**
     * Gets the results of this process
     * @throws WfException
     * @throws ResultNotAvailable
     * @return result Map of results from this process
     */
    public Map result() throws WfException, ResultNotAvailable;

    /**
     * Receives activity results.
     * @param activity WfActivity sending the results.
     * @param results Map of the results.
     * @throws WfException
     */
    public void receiveResults(WfActivity activity, Map results) throws WfException;

    /**
     * Receives notification when an activity has completed.
     * @param activity WfActivity which has completed.
     * @throws WfException
     */
    public void activityComplete(WfActivity activity) throws WfException;

    /**
     * Starts the Workflow process
     * @throws WfException
     * @throws CannotStart
     * @throws AlreadyRunning
     */
    public void start() throws WfException, CannotStart, AlreadyRunning;
    
    /**
     * Starts the Workflow process on the defined activity
     * @param activityId The activity to start 
     * @throws WfException
     * @throws CannotStart
     * @throws AlreadyRunning
     */
    public void start(String activityId) throws WfException, CannotStart, AlreadyRunning;    

    /**
     * @param state
     * @throws WfException
     * @throws InvalidState
     * @return
     */
    public Iterator getActivitiesInState(String state) throws WfException, InvalidState;

} // interface WfProcessOperations
