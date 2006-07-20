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
package org.ofbiz.workflow;

import java.util.Map;

/**
 * WfDataEventAudit - Workflow Data Event Audit Interface
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
 */
public interface WfDataEventAudit {

    /**
     * @throws WfException
     * @return
     */
    public Map oldData() throws WfException;

    /**
     * @throws WfException
     * @return
     */
    public Map newData() throws WfException;

} // interface WfDataEventAuditOperations
