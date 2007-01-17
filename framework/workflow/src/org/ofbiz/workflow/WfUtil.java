/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.ofbiz.workflow;

import java.util.Map;

import org.ofbiz.base.util.UtilMisc;

/**
 * WorkflowUtil - Workflow Engine Utilities
 */
public final class WfUtil {
    
    private static final Map typeMap = UtilMisc.toMap("WDT_BOOLEAN", "java.lang.Boolean",
        "WDT_STRING", "java.lang.String", "WDT_INTEGER", "java.lang.Long", 
        "WDT_FLOAT", "java.lang.Double", "WDT_DATETIME", "java.sql.Timestamp");
                      
    /**
     * Gets the Java type from a XPDL datatype
     * @param xpdlType XPDL data type to be translated
     * @return Java Class name equivalence to the XPDL data type
     */
    public static final String getJavaType(String xpdlType) {        
        if (typeMap.containsKey(xpdlType))
            return (String) typeMap.get(xpdlType);
        else
            return "java.lang.Object";
    }
    
    /**
     * Returns the OFB status code which refers to the passed OMG status code
     * @param state
     * @return String
     */
    public static String getOFBStatus(String state) {
        String statesArr[] = {"open.running", "open.not_running.not_started", "open.not_running.suspended",
                "closed.completed", "closed.terminated", "closed.aborted"};
        String entityArr[] = {"WF_RUNNING", "WF_NOT_STARTED", "WF_SUSPENDED", "WF_COMPLETED",
                "WF_TERMINATED", "WF_ABORTED"};

        for (int i = 0; i < statesArr.length; i++) {
            if (statesArr[i].equals(state))
                return entityArr[i];
        }
        return null;
    }

    /**
     * Returns the OMG status code which refers to the passed OFB status code
     * @param state
     * @return String
     */
    public static String getOMGStatus(String state) {
        String statesArr[] = {"open.running", "open.not_running.not_started", "open.not_running.suspended",
                "closed.completed", "closed.terminated", "closed.aborted"};
        String entityArr[] = {"WF_RUNNING", "WF_NOT_STARTED", "WF_SUSPENDED", "WF_COMPLETED",
                "WF_TERMINATED", "WF_ABORTED"};

        for (int i = 0; i < entityArr.length; i++) {
            if (entityArr[i].equals(state))
                return statesArr[i];
        }
        return null;
    }
    
    
}
