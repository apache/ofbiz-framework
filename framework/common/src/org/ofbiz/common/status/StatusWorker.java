/*
 * $Id: StatusWorker.java 5462 2005-08-05 18:35:48Z jonesde $
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
package org.ofbiz.common.status;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.jsp.PageContext;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;

/**
 * StatusWorker
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class StatusWorker {
    
    public static final String module = StatusWorker.class.getName();
    
    public static void getStatusItems(PageContext pageContext, String attributeName, String statusTypeId) {
        GenericDelegator delegator = (GenericDelegator) pageContext.getRequest().getAttribute("delegator");

        try {
            Collection statusItems = delegator.findByAndCache("StatusItem", UtilMisc.toMap("statusTypeId", statusTypeId), UtilMisc.toList("sequenceId"));

            if (statusItems != null)
                pageContext.setAttribute(attributeName, statusItems);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
    }

    public static void getStatusItems(PageContext pageContext, String attributeName, String statusTypeIdOne, String statusTypeIdTwo) {
        GenericDelegator delegator = (GenericDelegator) pageContext.getRequest().getAttribute("delegator");
        List statusItems = new LinkedList();

        try {
            Collection calItems = delegator.findByAndCache("StatusItem", UtilMisc.toMap("statusTypeId", statusTypeIdOne), UtilMisc.toList("sequenceId"));

            if (calItems != null)
                statusItems.addAll(calItems);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        try {
            Collection taskItems = delegator.findByAndCache("StatusItem", UtilMisc.toMap("statusTypeId", statusTypeIdTwo), UtilMisc.toList("sequenceId"));

            if (taskItems != null)
                statusItems.addAll(taskItems);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        if (statusItems.size() > 0)
            pageContext.setAttribute(attributeName, statusItems);
    }

    public static void getStatusValidChangeToDetails(PageContext pageContext, String attributeName, String statusId) {
        GenericDelegator delegator = (GenericDelegator) pageContext.getRequest().getAttribute("delegator");
        Collection statusValidChangeToDetails = null;

        try {
            statusValidChangeToDetails = delegator.findByAndCache("StatusValidChangeToDetail", UtilMisc.toMap("statusId", statusId), UtilMisc.toList("sequenceId"));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        if (statusValidChangeToDetails != null)
            pageContext.setAttribute(attributeName, statusValidChangeToDetails);
    }
}
