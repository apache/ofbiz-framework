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
package org.ofbiz.webapp.view;

import org.ofbiz.webapp.view.ApacheFopWorker;

import org.apache.fop.apps.FopFactory;

/**
 * Apache FOP Factory used to provide a singleton instance of the FopFactory.  Best pratices recommended
 * the reuse of the factory because of the startup time.
 *
 */

public class ApacheFopFactory {

    public static final String module = ApacheFopFactory.class.getName();

    /** @deprecated use ApacheFopWorker.getFactoryInstance() */
    @Deprecated
    public static FopFactory instance() {
        return ApacheFopWorker.getFactoryInstance();
    }
}
