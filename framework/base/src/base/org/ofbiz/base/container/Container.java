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
package org.ofbiz.base.container;

/**
 * Container - Interface for containers
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a> 
  *@version    $Rev$
 * @since      3.0
 */
public interface Container {

    /** Initialize the container
     *
     * @param args args from calling class
     * @param configFile Location of master OFBiz configuration file
     * @throws ContainerException
     */
    public void init(String[] args, String configFile) throws ContainerException;
    
    /**
     * Start the container
     *
     * @return true if server started
     * @throws ContainerException
     */
    public boolean start() throws ContainerException;

    /**
     * Stop the container
     *     
     * @throws ContainerException
     */
    public void stop() throws ContainerException;
}
