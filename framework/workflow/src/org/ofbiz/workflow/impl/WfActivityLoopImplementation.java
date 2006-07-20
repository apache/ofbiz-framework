/*
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.ofbiz.workflow.impl;

/**
 * WfActivityLoopImplementation.java
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a> 
 * @author     Oswin Ondarza and Manuel Soto
 * @version    $Rev$
 * @since      2.0
 */
public class WfActivityLoopImplementation extends WfActivityAbstractImplementation {

    public static final String module = WfActivityLoopImplementation.class.getName();

    /**     
     * @see org.ofbiz.workflow.impl.WfActivityAbstractImplementation#WfActivityAbstractImplementation(WfActivityImpl)
     */
    public WfActivityLoopImplementation(WfActivityImpl wfActivity) {
        super(wfActivity);
    }

    /**
     * To be implemented.
     * @see org.ofbiz.workflow.impl.WfActivityAbstractImplementation#run()
     */
    public void run() {}
}
