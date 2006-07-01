/*
 * $Id: WfActivity.java 5462 2005-08-05 18:35:48Z jonesde $
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

import java.util.List;
import java.util.Iterator;
import java.util.Map;

/**
 * WfActivity - Workflow Activity Interface
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
 */
public interface WfActivity extends WfExecutionObject {

    /**
     * Retrieve amount of Assignment objects.
     * @throws WfException General workflow exception.
     * @return Amount of current assignments.
     */
    public int howManyAssignment() throws WfException;

    /**
     * Retrieve the Iterator of Assignments objects.
     * @throws WfException General workflow exception.
     * @return Assignment Iterator.
     */
    public Iterator getIteratorAssignment() throws WfException;

    /**
     * Retrieve all assignments of this activity.
     * @param maxNumber the high limit of number of assignment in result set (0 for all).
     * @throws WfException General workflow exception.
     * @return  List of WfAssignment objects.
     */
    public List getSequenceAssignment(int maxNumber) throws WfException;

    /**
     * Check if a specific assignment is a member of this activity.
     * @param member Assignment object.
     * @throws WfException General workflow exception.
     * @return true if the assignment is a member of this activity.
     */
    public boolean isMemberOfAssignment(WfAssignment member) throws WfException;

    /**
     * Getter for the process of this activity.
     * @throws WfException General workflow exception.
     * @return WfProcess Process to which this activity belong.
     */
    public WfProcess container() throws WfException;

    /**
     * Retrieve the Result map of this activity.
     * @throws WfException General workflow exception.
     * @throws ResultNotAvailable No result is available.
     * @return Map of results from this activity
     */
    public Map result() throws WfException, ResultNotAvailable;

    /**
     * Assign Result for this activity.
     * @param newResult New result.
     * @throws WfException General workflow exception.
     * @throws InvalidData Data is invalid
     */
    public void setResult(Map result) throws WfException, InvalidData;

    /**
     * Complete this activity.
     * @throws WfException General workflow exception.
     * @throws CannotComplete Cannot complete the activity
     */
    public void complete() throws WfException, CannotComplete;

    /**
     * Activates this activity.
     * @throws WfException
     * @throws CannotStart
     * @throws AlreadyRunning
     */
    public void activate() throws WfException, CannotStart, AlreadyRunning;

} // interface WfActivity
