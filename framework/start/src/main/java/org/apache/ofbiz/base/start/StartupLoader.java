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
package org.apache.ofbiz.base.start;

import java.util.List;

/**
 * An object that loads server startup classes.
 * <p>
 * When OFBiz starts, the main thread will create the <code>StartupLoader</code> instance and
 * then call the loader's <code>load</code> method. If the method returns without
 * throwing an exception the loader will be added to a list of initialized loaders.
 * After all instances have been created and initialized, the main thread will call the
 * <code>start</code> method of each loader in the list. When OFBiz shuts down, a
 * separate shutdown thread will call the <code>unload</code> method of each loader.
 * Implementations should anticipate asynchronous calls to the methods by different
 * threads.
 * </p>
 * 
 */
public interface StartupLoader {

    /**
     * Start a startup class.
     *
     * @param config Startup config.
     * @param ofbizCommands Command-line arguments.
     * @throws StartupException If an error was encountered. Throwing this exception
     * will halt loader loading, so it should be thrown only when OFBiz can't
     * operate without it.
     */
    public void load(Config config, List<StartupCommand> ofbizCommands) throws StartupException;

    /**
     * Stop the startup class. This method must not block.
     *
     * @throws StartupException If an error was encountered.
     */
    public void unload() throws StartupException;
}
