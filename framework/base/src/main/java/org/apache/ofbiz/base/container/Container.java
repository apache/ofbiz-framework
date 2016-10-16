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
package org.apache.ofbiz.base.container;

import java.util.List;

import org.apache.ofbiz.base.start.StartupCommand;

/**
 * An OFBiz container. A container can be thought of as a background process.
 * 
 * <p>
 * When OFBiz starts, the main thread will create the <code>Container</code> instance and
 * then call the container's <code>init</code> method. If the method returns without
 * throwing an exception the container will be added to a list of initialized containers.
 * After all instances have been created and initialized, the main thread will call the
 * <code>start</code> method of each container in the list. When OFBiz shuts down, a
 * separate shutdown thread will call the <code>stop</code> method of each container.
 * Implementations should anticipate asynchronous calls to the methods by different
 * threads.
 * </p>
 * 
 * <p>Containers might be loaded more than once (have more than one instance).<p>
 */
public interface Container {

    /** Initialize the container. This method must not block - implementations
     * should initialize internal structures and then return.
     *
     * @param ofbizCommands Command-line arguments.
     * @param name Unique name of the container's instance.
     * @param configFile Location of the configuration file used to load this container.
     * @throws ContainerException If an error was encountered. Throwing this exception
     * will halt container loading, so it should be thrown only when other containers
     * might depend on this one.
     */
    public void init(List<StartupCommand> ofbizCommands, String name, String configFile) throws ContainerException;

    /**
     * Start the container process. This method must not block - implementations
     * that require thread blocking must create a separate thread and then return.
     *
     * @return <code>true</code> if the process started.
     * @throws ContainerException If an error was encountered.
     */
    public boolean start() throws ContainerException;

    /**
     * Stop the container process. This method must not block.
     *
     * @throws ContainerException If an error was encountered.
     */
    public void stop() throws ContainerException;

    /**
     * Return the container name.
     *
     * @return Name of the container's instance.
     */
    public String getName();
}
