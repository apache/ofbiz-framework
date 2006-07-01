/*
 * $Id: WfRequester.java 5462 2005-08-05 18:35:48Z jonesde $
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
import java.util.List;
import java.util.Map;

import org.ofbiz.service.GenericRequester;

/**
 * WfRequester - Workflow Requester Interface
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
 */
public interface WfRequester {

    /**
     * Gets the number of processes.
     * @throws WfException
     * @return Count of the number of workflow processes
     */
    public int howManyPerformer() throws WfException;

    /** Gets an iterator of processes.
     * @throws WfException
     * @return Iterator of workflow processes.
     */
    public Iterator getIteratorPerformer() throws WfException;

    /**
     * A list of processes
     * @param maxNumber
     * @throws WfException
     * @return List of WfProcess objects.
     */
    public List getSequencePerformer(int maxNumber) throws WfException;

    /**
     * Checks if a WfProcess is associated with this requester object
     * @param member
     * @throws WfException
     * @return true if the process is found.
     */
    public boolean isMemberOfPerformer(WfProcess member) throws WfException;

    /**
     * Registers a process with this requester; starts the process.
     *@param process to register
     *@param context to initialize the process with
     *@param requester associated with the service
     *@throws WfException
     */
    public void registerProcess(WfProcess process, Map context, GenericRequester requester) throws WfException;

    /**
     * Receives notice of event status changes
     * @param event
     * @throws WfException
     * @throws InvalidPerformer
     */
    public void receiveEvent(WfEventAudit event) throws WfException, InvalidPerformer;

} // interface WfRequesterOperations
