/*
 * $Id: WfAssignment.java 5462 2005-08-05 18:35:48Z jonesde $
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

/**
 * WfAssignment - Workflow Assignment Interface
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
 */
public interface WfAssignment {

    /**
     * Gets the activity object of this assignment.
     * @return WfActivity The activity object of this assignment.
     * @throws WfException
     */
    public WfActivity activity() throws WfException;

    /**
     * Gets the assignee (resource) of this assignment.
     * @return WfResource The assigned resource.
     * @throws WfException
     */
    public WfResource assignee() throws WfException;

    /**
     * Sets the assignee of this assignment.
     * @param newValue The new assigned resource.
     * @throws WfException
     * @throws InvalidResource
     */
    public void setAssignee(WfResource newValue) throws WfException, InvalidResource;

    /**
     * Gets the from date of this assignment.
     * @return Timestamp when this assignment first began.
     * @throws WfException
     */
    public java.sql.Timestamp fromDate() throws WfException;

    /**
     * Mark this assignment as accepted.
     * @throws WfException
     */
    public void accept() throws WfException;

    /**
     * Set the results of this assignment.
     * @param Map The results of the assignement.
     * @throws WfException
     */
    public void setResult(java.util.Map results) throws WfException;

    /**
     * Mark this assignment as complete.
     * @throws WfException
     */
    public void complete() throws WfException;

    /**
     * Mark this assignment as delegated.
     * @throws WfException
     */
    public void delegate() throws WfException;

    /**
     * Change the status of this assignment.
     * @param status The new status.
     * @throws WfException
     */
    public void changeStatus(String status) throws WfException;

    /**
     * Gets the status of this assignment.
     * @return String status code for this assignment.
     * @throws WfException
     */
    public String status() throws WfException;

    /**
     * Removes the stored data for this object.
     * @throws WfException
     */
    public void remove() throws WfException;

} // interface WfAssignmentOperations
