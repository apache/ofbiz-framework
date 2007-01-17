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
package org.ofbiz.pos.event;

import org.ofbiz.pos.screen.PosScreen;
import org.ofbiz.pos.component.Journal;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.Debug;

public class TestEvents {

    public static final String module = TestEvents.class.getName();

    public static void testAlert(PosScreen pos) {
        pos.showDialog("dialog/error/testerror");
    }

    public static void logSelectedIndex(PosScreen pos) {
        Journal journal = pos.getJournal();
        Debug.log("Selected IDX - " + journal.getSelectedIdx(), module);
    }
    
    public static void testMsr(PosScreen pos) {
        try {
            org.ofbiz.pos.jpos.service.MsrTestService.sendTest();
        } catch (GeneralException e) {
            Debug.logError(e, module);
            pos.showDialog("dialog/error/exception", e.getMessage());
        }
    }
}
