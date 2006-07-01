/*
 * $Id: TaskWorker.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.order.task;

import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

/**
 * Order Processing Task Worker
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
 */
public class TaskWorker {
    
    public static final String module = TaskWorker.class.getName();
    
    public static String getCustomerName(GenericValue orderTaskList) {
        String lastName = orderTaskList.getString("customerLastName");
        String firstName = orderTaskList.getString("customerFirstName");
        //String groupName = orderTaskList.getString("customerGroupName");
        String groupName = null; // this is only until the entity gets fixed
        if (groupName != null) {
            return groupName;
        } else if (lastName != null) {
            String name = lastName;
            if (firstName != null)
                name = name + ", " + firstName;
            return name;
        } else {
            return "";
        }
    } 
    
    static Map statusMapping = UtilMisc.toMap("WF_NOT_STARTED", "Waiting", "WF_RUNNING", "Active", "WF_COMPLETE", "Complete", "WF_SUSPENDED", "Hold");
    
    public static String getPrettyStatus(GenericValue orderTaskList) {
        String statusId = orderTaskList.getString("currentStatusId");        
        String prettyStatus = (String) statusMapping.get(statusId);
        if (prettyStatus == null)
            prettyStatus = "?";
        return prettyStatus;
    }
        
     
    public static String getRoleDescription(GenericValue orderTaskList) {        
        GenericValue role = null;
        try {
            Map pkFields = UtilMisc.toMap("roleTypeId", orderTaskList.getString("roleTypeId"));
            role = orderTaskList.getDelegator().findByPrimaryKey("RoleType", pkFields);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot get RoleType entity value", module);
            return orderTaskList.getString("roleTypeId");
        }
        return role.getString("description");
    }   
    
}
