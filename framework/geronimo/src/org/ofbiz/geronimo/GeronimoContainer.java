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
package org.ofbiz.geronimo;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.geronimo.transaction.context.TransactionContextManager;
import org.ofbiz.base.container.Container;
import org.ofbiz.base.container.ContainerConfig;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.util.Debug;

/**
 * Geronimo Container
 */
public class GeronimoContainer implements Container {

    public static final String module = GeronimoContainer.class.getName();

    protected String configFile = null;
    protected TransactionContextManager geronimoTcm = null;

    /**
     * @see org.ofbiz.base.container.Container#init(java.lang.String[], java.lang.String)
     */
    public void init(String[] args, String configFile) throws ContainerException {
        this.configFile = configFile;
        this.startGeronimo();
    }

    public boolean start() throws ContainerException {
        return true;
    }

    private void startGeronimo() throws ContainerException {
        // get the container config
        ContainerConfig.Container cc = ContainerConfig.getContainer("geronimo-container", configFile);
        if (cc == null) {
            throw new ContainerException("No geronimo-container configuration found in container config!");
        }

        //String carolPropName = ContainerConfig.getPropertyValue(cc, "jndi-config", "iiop.properties");

        // start Geronimo
        this.geronimoTcm = new TransactionContextManager();

        // bind UserTransaction and TransactionManager to JNDI
        try {
            InitialContext ic = new InitialContext();
            // TODO: for some reason this is not working, throwing an error: java.lang.IllegalArgumentException: RegistryContext: object to bind must be Remote, Reference, or Referenceable
            ic.rebind("java:comp/UserTransaction", this.geronimoTcm.getTransactionManager());
        } catch (NamingException e) {
            throw new ContainerException("Unable to bind UserTransaction/TransactionManager to JNDI", e);
        }

        // check JNDI
        try {
            InitialContext ic = new InitialContext();
            Object o = ic.lookup("java:comp/UserTransaction");
            if (o == null) {
                throw new NamingException("Object came back null");
            }
        } catch (NamingException e) {
            throw new ContainerException("Unable to lookup bound objects", e);
        }
        Debug.logInfo("Geronimo is bound to JNDI - java:comp/UserTransaction", module);
    }

    public void stop() throws ContainerException {        
        // TODO: how to stop the Geronimo transaction manager? is it even needed?
    }

}
