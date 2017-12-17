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
package org.apache.ofbiz.base.util;

import java.util.List;
import java.util.Map;

import javax.script.ScriptException;

/**
 * A script helper object. The OFBiz scripting framework will include an
 * instance of this class in the script's bindings.
 * <p>The scripting language will determine how the helper is used. Most
 * languages will access it as a variable:<br>
 * <code>partyValue = ofbiz.findOne("Party");</code><br>
 * while other languages might access it as a native method or function:<br>
 * <code>partyValue = findOne("Party");</code>
 * </p>
 */
public interface ScriptHelper {

    /**
     * Extracts service IN parameters from <code>inputMap</code> and returns them in a new <code>Map</code>.
     *
     * @param serviceName
     * @param inputMap
     * @return The matching service parameters
     * @throws ScriptException
     */
    Map<String, ? extends Object> createServiceMap(String serviceName, Map<String, ? extends Object> inputMap) throws ScriptException;

    /**
     * Sets the event/service status to error status.
     *
     * @param message
     */
    void error(String message);

    /**
     * Evaluates a <code>String</code> and returns the result.
     *
     * @param original
     * @return
     */
    String evalString(String original);

    /**
     * Sets the event/service status to failure status.
     *
     * @param message
     */
    void failure(String message);

    /**
     * Returns a <code>List</code> of <code>GenericValue</code>s.
     * @param entityName
     * @param fields
     * @return
     * @throws ScriptException
     */
    List<Map<String, Object>> findList(String entityName, Map<String, ? extends Object> fields) throws ScriptException;

    /**
     * Finds a <code>GenericValue</code> by primary key. The helper will construct a primary key from existing variables.
     * @param entityName
     * @return
     * @throws ScriptException
     */
    Map<String, Object> findOne(String entityName) throws ScriptException;

    /**
     * Finds a <code>GenericValue</code> by primary key. The helper will construct a primary key from existing variables
     * and/or <code>fields</code>.
     * @param entityName
     * @param fields
     * @param args
     * @return
     * @throws ScriptException
     */
    Map<String, Object> findOne(String entityName, Map<String, ? extends Object> fields, Map<String, ? extends Object> args) throws ScriptException;

    /**
     * Logs an error message.
     *
     * @param message
     */
    void logError(String message);

    /**
     * Logs an info message.
     *
     * @param message
     */
    void logInfo(String message);

    /**
     * Logs a warning message.
     *
     * @param message
     */
    void logWarning(String message);

    /**
     * Creates a new, empty <code>GenericValue</code>.
     * @param entityName
     * @return
     * @throws ScriptException
     */
    Map<String, Object> makeValue(String entityName) throws ScriptException;

    /**
     * Creates a new, empty <code>GenericValue</code>.
     *
     * @param entityName
     * @param fields
     * @return
     * @throws ScriptException
     */
    Map<String, Object> makeValue(String entityName, Map<String, Object> fields) throws ScriptException;

    /**
     * Runs a service synchronously.
     *
     * @param serviceName
     * @param inputMap
     * @return
     * @throws ScriptException
     */
    Map<String, ? extends Object> runService(String serviceName, Map<String, ? extends Object> inputMap) throws ScriptException;

    /**
     * Runs a service synchronously.
     *
     * @param serviceName
     * @param inputMap
     * @param args
     * @return
     * @throws ScriptException
     */
    Map<String, ? extends Object> runService(String serviceName, Map<String, ? extends Object> inputMap, Map<String, ? extends Object> args) throws ScriptException;

    /**
     * Sets the event/service status to success status.
     */
    void success();

    /**
     * Sets the event/service status to success status.
     *
     * @param message
     */
    void success(String message);
}
