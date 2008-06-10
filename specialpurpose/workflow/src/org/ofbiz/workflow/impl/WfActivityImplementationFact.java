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
package org.ofbiz.workflow.impl;

/**
 * WfActivityImplementationFact.java
 */
public class WfActivityImplementationFact {

    public static final String module = WfActivityImplementationFact.class.getName();

    /**
     * Gets the implementation class to be used.
     * @param type
     * @param wfActivity
     * @return WfActivityAbstractImplementation
     */
    public static WfActivityAbstractImplementation getConcretImplementation(String type, WfActivityImpl wfActivity) {
        if (type.equals("WAT_NO"))
            return new WfActivityNoImplementation(wfActivity); // NO implementation requires MANUAL FinishMode
        else if (type.equals("WAT_ROUTE"))
            return new WfActivityRouteImplementation(wfActivity); // ROUTE goes directly to complete status
        else if (type.equals("WAT_TOOL"))
            return new WfActivityToolImplementation(wfActivity);
        else if (type.equals("WAT_SUBFLOW"))
            return new WfActivitySubFlowImplementation(wfActivity);
        else if (type.equals("WAT_LOOP"))
            return new WfActivityLoopImplementation(wfActivity);
        else
            return null;
    }
}
