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
package org.apache.ofbiz.entity.transaction;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.GenericEntityConfException;
import org.apache.ofbiz.entity.config.model.EntityConfig;

/**
 * TransactionFactoryLoader - utility class that loads the transaction manager and provides to client code a reference to it (TransactionFactory)
 */
public class TransactionFactoryLoader {

    private static final String MODULE = TransactionFactoryLoader.class.getName();
    private static final TransactionFactory TX_FACTORY = createTransactionFactory();

    private static TransactionFactory createTransactionFactory() {
        TransactionFactory instance = null;
        try {
            String className = EntityConfig.getInstance().getTransactionFactory().getClassName();
            if (className == null) {
                throw new IllegalStateException("Could not find transaction factory class name definition");
            }
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Class<?> tfClass = loader.loadClass(className);
            instance = (TransactionFactory) tfClass.getDeclaredConstructor().newInstance();
        } catch (GenericEntityConfException gece) {
            Debug.logError(gece, "Could not find transaction factory class name definition", MODULE);
        } catch (ClassNotFoundException cnfe) {
            Debug.logError(cnfe, "Could not find transaction factory class", MODULE);
        } catch (Exception e) {
            Debug.logError(e, "Unable to instantiate the transaction factory", MODULE);
        }
        return instance;
    }

    public static TransactionFactory getInstance() {
        if (TX_FACTORY == null) {
            throw new IllegalStateException("The Transaction Factory is not initialized.");
        }
        return TX_FACTORY;
    }
}
