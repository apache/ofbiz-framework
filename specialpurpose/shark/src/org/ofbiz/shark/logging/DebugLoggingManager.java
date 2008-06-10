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
package org.ofbiz.shark.logging;

import org.ofbiz.base.util.Debug;

import org.enhydra.shark.api.internal.logging.LoggingManager;
import org.enhydra.shark.api.internal.working.CallbackUtilities;
import org.enhydra.shark.api.RootException;

public class DebugLoggingManager implements LoggingManager {

    public static final String module = DebugLoggingManager.class.getName();
    protected  CallbackUtilities cus = null;

    public void configure(CallbackUtilities cus) throws RootException {
        this.cus = cus;
    }

    public void error(String msg) throws RootException {
        Debug.logError(msg, module);
    }

    public void error(String msg, RootException ex) throws RootException {
        Debug.logError(ex, msg, module);
    }

    public void error(String channel, String msg) throws RootException {
        Debug.logError(msg, channel);
    }

    public void error(String channel, String msg, RootException ex) throws RootException {
        Debug.logError(ex, msg, channel);
    }

    public void warn(String msg) throws RootException {
        Debug.logWarning(msg, module);
    }

    public void warn(String msg, RootException ex) throws RootException {
        Debug.logWarning(ex, msg, module);
    }

    public void warn(String channel, String msg) throws RootException {
        Debug.logWarning(msg, channel);
    }

    public void warn(String channel, String msg, RootException ex) throws RootException {
        Debug.logWarning(ex, msg, channel);
    }

    public void info(String msg) throws RootException {
        Debug.logInfo(msg, module);
    }

    public void info(String msg, RootException ex) throws RootException {
        Debug.logInfo(ex, msg, module);
    }

    public void info(String channel, String msg) throws RootException {
        Debug.logInfo(msg, channel);
    }

    public void info(String channel, String msg, RootException ex) throws RootException {
        Debug.logInfo(ex, msg, channel);
    }

    public void debug(String msg) throws RootException {
        Debug.logVerbose(msg, module);
    }

    public void debug(String msg, RootException ex) throws RootException {
        Debug.logVerbose(ex, msg, module);
    }

    public void debug(String channel, String msg) throws RootException {
        Debug.logVerbose(msg, channel);
    }

    public void debug(String channel, String msg, RootException ex) throws RootException {
        Debug.logVerbose(ex, msg, channel);
    }
}
